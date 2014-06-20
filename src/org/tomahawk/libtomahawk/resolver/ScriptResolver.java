/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.libtomahawk.resolver;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.infosystem.InfoSystemUtils;
import org.tomahawk.libtomahawk.utils.StringEscapeUtils;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a javascript resolver.
 */
public class ScriptResolver implements Resolver {

    private final static String TAG = ScriptResolver.class.getName();

    private final static String RESOLVER_LEGACY_CODE
            = "var resolver = Tomahawk.resolver.instance ? Tomahawk.resolver.instance : TomahawkResolver;";

    private final static String RESOLVER_LEGACY_CODE2
            = "var resolver = Tomahawk.resolver.instance ? Tomahawk.resolver.instance : window;";

    private final static String SCRIPT_INTERFACE_NAME = "Tomahawk";

    public final static String CONFIG = "config";

    public final static String ENABLED_KEY = "_enabled_";

    // We have to map the original cache keys to an id string, because a string containing "\t\t"
    // delimiters does come out without the delimiters, after it has been processed in the js
    // resolver script
    private ConcurrentHashMap<String, String> mQueryKeys = new ConcurrentHashMap<String, String>();

    private String mId;

    private WebView mScriptEngine;

    private String mPath;

    private ScriptResolverMetaData mMetaData;

    private String mIconPath;

    private int mWeight;

    private int mTimeout;

    private ScriptResolverConfigUi mConfigUi;

    private boolean mEnabled;

    private boolean mReady;

    private boolean mStopped;

    private ObjectMapper mObjectMapper;

    private SharedPreferences mSharedPreferences;

    private static final int TIMEOUT_HANDLER_MSG = 1337;

    // Handler which sets the mStopped bool to true after the timeout has occured.
    // Meaning this resolver is no longer being shown as resolving.
    private final Handler mTimeOutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            removeMessages(msg.what);
            mStopped = true;
        }
    };

    /**
     * Construct a new {@link ScriptResolver}
     *
     * @param path {@link String} containing the path to this js resolver's "content"-folder
     */
    public ScriptResolver(String path) {
        mObjectMapper = InfoSystemUtils.constructObjectMapper();
        mSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());
        mPath = path;
        mReady = false;
        mStopped = true;
        mScriptEngine = new WebView(TomahawkApp.getContext());
        WebSettings settings = mScriptEngine.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        mScriptEngine.setWebChromeClient(new TomahawkWebChromeClient());
        mScriptEngine.setWebViewClient(new ScriptEngine(this));
        final ScriptInterface scriptInterface = new ScriptInterface(this);
        mScriptEngine.addJavascriptInterface(scriptInterface, SCRIPT_INTERFACE_NAME);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mScriptEngine.getSettings().setAllowUniversalAccessFromFileURLs(true);
        }
        try {
            String rawJsonString = TomahawkUtils.inputStreamToString(TomahawkApp.getContext()
                    .getAssets().open(path + "/metadata.json"));
            mMetaData = mObjectMapper.readValue(rawJsonString, ScriptResolverMetaData.class);
            mId = mMetaData.pluginName;
            mIconPath = "file:///android_asset/" + path + "/" + mMetaData.manifest.icon;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "ScriptResolver: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (JsonMappingException e) {
            Log.e(TAG, "ScriptResolver: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (JsonParseException e) {
            Log.e(TAG, "ScriptResolver: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG, "ScriptResolver: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        if (getConfig().get(ENABLED_KEY) != null) {
            mEnabled = (Boolean) getConfig().get(ENABLED_KEY);
        } else {
            if (PipeLine.PLUGINNAME_RDIO.equals(mId)
                    || PipeLine.PLUGINNAME_BEATSMUSIC.equals(mId)
                    || PipeLine.PLUGINNAME_BEETS.equals(mId)
                    || PipeLine.PLUGINNAME_GMUSIC.equals(mId)
                    || PipeLine.PLUGINNAME_DEEZER.equals(mId)) {
                setEnabled(false);
            } else {
                setEnabled(true);
            }
        }

        init();
    }

    /**
     * @return whether or not this {@link Resolver} is ready
     */
    @Override
    public boolean isReady() {
        return mReady;
    }

    /**
     * @return whether or not this {@link ScriptResolver} is currently resolving
     */
    @Override
    public boolean isResolving() {
        return mReady && !mStopped;
    }

    @Override
    public String getIconPath() {
        return mIconPath;
    }

    @Override
    public int getIconResId() {
        return 0;
    }

    /**
     * Initialize this {@link ScriptResolver}. Loads the .js script from the given path and sets the
     * appropriate base URL.
     */
    private void init() {
        final String baseurl = "file://fake.bla.blu";
        String data = "<!DOCTYPE html>" + "<html><body>"
                + "<script src=\"file:///android_asset/js/cryptojs-core.js"
                + "\" type=\"text/javascript\"></script>";
        for (String scriptPath : mMetaData.manifest.scripts) {
            data += "<script src=\"file:///android_asset/" + mPath + "/" + scriptPath
                    + "\" type=\"text/javascript\"></script>";
        }
        try {
            String[] cryptoJsScripts =
                    TomahawkApp.getContext().getAssets().list("js/cryptojs");
            for (String scriptPath : cryptoJsScripts) {
                data += "<script src=\"file:///android_asset/js/cryptojs/" + scriptPath
                        + "\" type=\"text/javascript\"></script>";
            }
        } catch (IOException e) {
            Log.e(TAG, "ScriptResolver: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        data += "<script src=\"file:///android_asset/js/tomahawk_android_pre.js"
                + "\" type=\"text/javascript\"></script>"
                + "<script src=\"file:///android_asset/js/tomahawk.js"
                + "\" type=\"text/javascript\"></script>"
                + "<script src=\"file:///android_asset/js/tomahawk_android_post.js"
                + "\" type=\"text/javascript\"></script>"
                + "<script src=\"file:///android_asset/" + mPath + "/"
                + mMetaData.manifest.main + "\" type=\"text/javascript\"></script>"
                + "</body></html>";
        final String finalData = data;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mScriptEngine.loadDataWithBaseURL(baseurl, finalData, "text/html", null, null);
            }
        });
    }

    /**
     * This method is being called, when the {@link ScriptEngine} has completely loaded the given
     * .js script.
     */

    public void onScriptEngineReady() {
        resolverInit();
        mReady = true;
        PipeLine.getInstance().onResolverReady();
    }

    /**
     * This method calls the js function resolver.init().
     */
    private void resolverInit() {
        final String url = "javascript:" + RESOLVER_LEGACY_CODE + makeJSFunctionCallbackJava(
                R.id.scriptresolver_resolver_init, "resolver.init()", false);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mScriptEngine.loadUrl(url);
            }
        });
    }

    /**
     * This method tries to get the {@link Resolver}'s settings.
     */
    private void resolverSettings() {
        final String url = "javascript:" + RESOLVER_LEGACY_CODE
                + makeJSFunctionCallbackJava(R.id.scriptresolver_resolver_settings,
                "resolver.settings ? resolver.settings : getSettings() ", true);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mScriptEngine.loadUrl(url);
            }
        });
    }

    /**
     * This method tries to save the {@link Resolver}'s UserConfig.
     */
    private void resolverSaveUserConfig() {
        final String url = "javascript:" + RESOLVER_LEGACY_CODE
                + makeJSFunctionCallbackJava(R.id.scriptresolver_resolver_save_userconfig,
                "resolver.saveUserConfig()", false);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mScriptEngine.loadUrl(url);
            }
        });
    }

    /**
     * This method tries to get the {@link Resolver}'s UserConfig.
     */
    private void resolverGetConfigUi() {
        final String url = "javascript:" + RESOLVER_LEGACY_CODE
                + makeJSFunctionCallbackJava(R.id.scriptresolver_resolver_get_config_ui,
                "resolver.getConfigUi()", true);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mScriptEngine.loadUrl(url);
            }
        });
    }

    public void callback(final int callbackId, final String responseText,
            final Map<String, List<String>> responseHeaders, final int status,
            final String statusText) {
        final Map<String, String> headers = new HashMap<String, String>();
        for (String key : responseHeaders.keySet()) {
            if (key != null) {
                String concatenatedValues = "";
                for (int i = 0; i < responseHeaders.get(key).size(); i++) {
                    if (i > 0) {
                        concatenatedValues += "\n";
                    }
                    concatenatedValues += responseHeaders.get(key).get(i);
                }
                headers.put(key, concatenatedValues);
            }
        }
        try {
            String headersString = mObjectMapper.writeValueAsString(headers);
            final String url = "javascript: Tomahawk.callback(" + callbackId + ","
                    + "'" + StringEscapeUtils.escapeJavaScript(responseText) + "',"
                    + "'" + StringEscapeUtils.escapeJavaScript(headersString) + "',"
                    + status + ","
                    + "'" + StringEscapeUtils.escapeJavaScript(statusText) + "');";
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mScriptEngine.loadUrl(url);
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "callback: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    /**
     * Every callback from a function inside the javascript should first call the method
     * callbackToJava, which is exposed to javascript within the {@link ScriptInterface}. And after
     * that this callback will be handled here.
     *
     * @param id         used to identify which function did the callback
     * @param jsonString the json-string containing the {@link Result} information. Can be null
     */
    public void handleCallbackToJava(final int id, final String... jsonString) {
        try {
            if (id == R.id.scriptresolver_resolver_settings && jsonString != null
                    && jsonString.length == 1) {
                ScriptResolverSettings settings = mObjectMapper
                        .readValue(jsonString[0], ScriptResolverSettings.class);
                mWeight = settings.weight;
                mTimeout = settings.timeout * 1000;
                resolverGetConfigUi();
            } else if (id == R.id.scriptresolver_resolver_get_config_ui && jsonString != null
                    && jsonString.length == 1) {
                mConfigUi = mObjectMapper.readValue(jsonString[0], ScriptResolverConfigUi.class);
            } else if (id == R.id.scriptresolver_resolver_init) {
                resolverSettings();
            } else if (id == R.id.scriptresolver_add_track_results_string && jsonString != null) {
                ThreadManager.getInstance().execute(
                        new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_REPORTING) {
                            @Override
                            public void run() {
                                ScriptResolverResult result = null;
                                try {
                                    result = mObjectMapper
                                            .readValue(jsonString[0], ScriptResolverResult.class);
                                } catch (IOException e) {
                                    Log.e(TAG, "handleCallbackToJava: " + e.getClass() + ": " + e
                                            .getLocalizedMessage());
                                }
                                if (result != null) {
                                    ArrayList<Result> parsedResults = parseResultList(
                                            result.results, result.qid);
                                    PipeLine.getInstance().reportResults(mQueryKeys.get(result.qid),
                                            parsedResults, mId);
                                }
                                mTimeOutHandler.removeCallbacksAndMessages(null);
                                mStopped = true;
                            }
                        }
                );
            } else if (id == R.id.scriptresolver_report_stream_url && jsonString != null
                    && jsonString.length >= 2) {
                Map<String, String> headers = null;
                if (jsonString.length > 2) {
                    headers = mObjectMapper.readValue(jsonString[2], Map.class);
                }
                String resultKey = mQueryKeys.get(jsonString[0]);
                PipeLine.getInstance().sendStreamUrlReportBroadcast(resultKey, jsonString[1],
                        headers);
            }
        } catch (IOException e) {
            Log.e(TAG, "handleCallbackToJava: " + e.getClass() + ": " + e
                    .getLocalizedMessage());
        }
    }

    /**
     * Invoke the javascript to resolve the given {@link Query}.
     *
     * @param query the {@link Query} which should be resolved
     * @return whether or not the Resolver is ready to resolve
     */
    @Override
    public boolean resolve(final Query query) {
        if (mReady) {
            mStopped = false;
            mTimeOutHandler.removeCallbacksAndMessages(null);
            mTimeOutHandler.sendEmptyMessageDelayed(TIMEOUT_HANDLER_MSG, mTimeout);
            String qid = TomahawkMainActivity.getSessionUniqueStringId();
            mQueryKeys.put(qid, query.getCacheKey());

            // construct javascript call url
            final String url;
            String escapedQid = StringEscapeUtils.escapeJavaScript(qid);
            if (query.isFullTextQuery()) {
                String fullTextQuery = StringEscapeUtils.escapeJavaScript(query.getFullTextQuery());
                url = "javascript:" + RESOLVER_LEGACY_CODE
                        + makeJSFunctionCallbackJava(R.id.scriptresolver_resolve,
                        "(Tomahawk.resolver.instance !== undefined) ?resolver.search( '"
                                + escapedQid + "', '" + fullTextQuery
                                + "' ):resolve( '" + escapedQid + "', '', '', '"
                                + fullTextQuery + "' )",
                        false);
            } else {
                String artistName = StringEscapeUtils.escapeJavaScript(query.getArtist().getName());
                String albumName = StringEscapeUtils.escapeJavaScript(query.getAlbum().getName());
                String trackName = StringEscapeUtils.escapeJavaScript(query.getName());
                url = "javascript:" + RESOLVER_LEGACY_CODE2
                        + makeJSFunctionCallbackJava(R.id.scriptresolver_resolve,
                        "resolver.resolve( '" + escapedQid + "', '" + artistName
                                + "', '" + albumName + "', '" + trackName + "' )",
                        false);
            }

            // call it
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mScriptEngine.loadUrl(url);
                }
            });
        }
        return mReady;
    }

    public void getStreamUrl(final Result result, String callbackFuncName) {
        if (result != null) {
            String resultId = TomahawkMainActivity.getSessionUniqueStringId();
            // we are using the same map as we do when resolving queries
            mQueryKeys.put(resultId, result.getCacheKey());
            final String url = "javascript:" + RESOLVER_LEGACY_CODE2
                    + makeJSFunctionCallbackJava(R.id.scriptresolver_report_stream_url,
                    "resolver." + callbackFuncName + "( '"
                            + StringEscapeUtils.escapeJavaScript(resultId)
                            + "', '" + StringEscapeUtils.escapeJavaScript(result.getPath()) + "' )",
                    true);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mScriptEngine.loadUrl(url);
                }
            });
        }
    }

    /**
     * Parses the given {@link JSONArray} into a {@link ArrayList} of {@link Result}s.
     *
     * @param resultEntries ArrayList of ScriptResolverResultEntries containing the raw result
     *                      information
     * @return a {@link ArrayList} of {@link Result}s containing the parsed data
     */
    private ArrayList<Result> parseResultList(ArrayList<ScriptResolverResultEntry> resultEntries,
            String queryKey) {
        ArrayList<Result> resultList = new ArrayList<Result>();
        for (ScriptResolverResultEntry resultEntry : resultEntries) {
            if (resultEntry != null && !TextUtils.isEmpty(resultEntry.url)
                    && !TextUtils.isEmpty(resultEntry.track)) {
                Artist artist;
                if (resultEntry.artist != null) {
                    artist = Artist.get(resultEntry.artist);
                } else {
                    artist = Artist.get("");
                }

                Album album;
                if (resultEntry.album != null) {
                    album = Album.get(resultEntry.album, artist);
                } else {
                    album = Album.get("", artist);
                }
                artist.addAlbum(album);

                Track track = Track.get(resultEntry.track, album, artist);
                track.setAlbumPos(resultEntry.albumpos);
                track.setDiscNumber(resultEntry.discnumber);
                if (resultEntry.year != null && resultEntry.year.matches("-?\\d+")) {
                    track.setYear(Integer.valueOf(resultEntry.year));
                }
                track.setDuration(resultEntry.duration * 1000);

                Result result = Result.get(resultEntry.url, track, this, queryKey);
                result.setBitrate(resultEntry.bitrate);
                result.setSize(resultEntry.size);
                result.setPurchaseUrl(resultEntry.purchaseUrl);
                result.setLinkUrl(resultEntry.linkUrl);
                result.setArtist(artist);
                result.setAlbum(album);
                result.setTrack(track);

                album.addQuery(Query.get(result, false));
                artist.addQuery(Query.get(result, false));
                resultList.add(result);
            }
        }
        return resultList;
    }

    /**
     * Wraps the given js call into the necessary functions to make sure, that the javascript
     * function will callback the exposed java method callbackToJava in the {@link ScriptInterface}
     *
     * @param id                 used to later identify the callback
     * @param string             the {@link String} which should be surrounded. Usually a simple js
     *                           function call.
     * @param shouldReturnResult whether or not this js function call will return with a {@link
     *                           JSONObject} as a result
     * @return the computed {@link String}
     */
    private String makeJSFunctionCallbackJava(int id, String string, boolean shouldReturnResult) {
        return SCRIPT_INTERFACE_NAME + ".callbackToJava(" + id + ",JSON.stringify(" + string + "),"
                + shouldReturnResult + ");";
    }

    /**
     * @return this {@link ScriptResolver}'s id
     */
    @Override
    public String getId() {
        return mId;
    }

    public String getName() {
        return mMetaData.name;
    }

    /**
     * @return the absolute filepath (without file://android_asset) of the corresponding script
     */
    public String getScriptFilePath() {
        return mPath + "/" + mMetaData.manifest.main;
    }

    public void setConfig(Map<String, Object> config) {
        try {
            String rawJsonString = mObjectMapper.writeValueAsString(config);
            mSharedPreferences.edit().putString(buildPreferenceKey(), rawJsonString).commit();
            resolverSaveUserConfig();
        } catch (IOException e) {
            Log.e(TAG, "setConfig: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    /**
     * @return the Map<String, String> containing the Config information of this resolver
     */
    public Map<String, Object> getConfig() {
        String rawJsonString = mSharedPreferences.getString(buildPreferenceKey(), "");
        try {
            return mObjectMapper.readValue(rawJsonString, Map.class);
        } catch (IOException e) {
            Log.e(TAG, "getConfig: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return new HashMap<String, Object>();
    }

    /**
     * @return this {@link ScriptResolver}'s weight
     */
    @Override
    public int getWeight() {
        return mWeight;
    }

    public String getDescription() {
        return mMetaData.description;
    }

    private String buildPreferenceKey() {
        return mMetaData.pluginName + "_" + CONFIG;
    }

    public ScriptResolverConfigUi getConfigUi() {
        return mConfigUi;
    }

    public boolean isEnabled() {
        if (getCorrespondingAuthUtilId() != null) {
            return AuthenticatorManager.getInstance().getAuthenticatorUtils(
                    getCorrespondingAuthUtilId()).isLoggedIn();
        }
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        Log.d(TAG, this.mId + " has been " + (enabled ? "enabled" : "disabled"));
        mEnabled = enabled;
        Map<String, Object> config = getConfig();
        config.put(ENABLED_KEY, enabled);
        setConfig(config);
    }

    public String getCorrespondingAuthUtilId() {
        if (mId.equals(PipeLine.PLUGINNAME_RDIO)) {
            return AuthenticatorManager.AUTHENTICATOR_ID_RDIO;
        } else if (mId.equals(PipeLine.PLUGINNAME_DEEZER)) {
            return AuthenticatorManager.AUTHENTICATOR_ID_DEEZER;
        }
        return null;
    }
}
