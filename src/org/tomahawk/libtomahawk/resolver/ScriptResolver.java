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
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.ScriptResolverCollection;
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

    private final static String TAG = ScriptResolver.class.getSimpleName();

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

    private ScriptResolverCollectionMetaData mCollectionMetaData;

    private String mIconPath;

    private int mWeight;

    private int mTimeout;

    private ScriptResolverConfigUi mConfigUi;

    private boolean mEnabled;

    private boolean mReady;

    private boolean mStopped;

    private ObjectMapper mObjectMapper;

    private SharedPreferences mSharedPreferences;

    private boolean mBrowsable;

    private boolean mPlaylistSync;

    private boolean mAccountFactory;

    private boolean mUrlLookup;

    private FuzzyIndex mFuzzyIndex;

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
            if (TomahawkApp.PLUGINNAME_RDIO.equals(mId)
                    || TomahawkApp.PLUGINNAME_BEATSMUSIC.equals(mId)
                    || TomahawkApp.PLUGINNAME_BEETS.equals(mId)
                    || TomahawkApp.PLUGINNAME_GMUSIC.equals(mId)
                    || TomahawkApp.PLUGINNAME_DEEZER.equals(mId)) {
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
        loadUrl("javascript:" + makeJSFunctionCallbackJava(
                R.id.scriptresolver_resolver_init, "Tomahawk.resolver.instance.init()", false));
    }

    /**
     * This method tries to get the {@link Resolver}'s settings.
     */
    private void resolverSettings() {
        loadUrl("javascript:"
                + makeJSFunctionCallbackJava(R.id.scriptresolver_resolver_settings,
                "Tomahawk.resolver.instance.settings", true));
    }

    /**
     * This method tries to save the {@link Resolver}'s UserConfig.
     */
    private void resolverSaveUserConfig() {
        loadUrl("javascript: Tomahawk.resolver.instance.saveUserConfig()");
    }

    /**
     * This method tries to get the {@link Resolver}'s UserConfig.
     */
    private void resolverGetConfigUi() {
        loadUrl("javascript:"
                + makeJSFunctionCallbackJava(R.id.scriptresolver_resolver_get_config_ui,
                "Tomahawk.resolver.instance.getConfigUi()", true));
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
            loadUrl("javascript: Tomahawk.callback(" + callbackId + ","
                    + "'" + StringEscapeUtils.escapeJavaScript(responseText) + "',"
                    + "'" + StringEscapeUtils.escapeJavaScript(headersString) + "',"
                    + status + ","
                    + "'" + StringEscapeUtils.escapeJavaScript(statusText) + "');");
        } catch (IOException e) {
            Log.e(TAG, "callback: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    /**
     * Sometimes we need the String returned by a certain javascript function. Therefore we wrap the
     * function call in a call to "callbackToJava", which is being received by the ScriptInterface.
     * The ScriptInterface redirects the call to this method, where we can access the returned
     * String. Throughout this whole process we are passing an id along, which enables us to
     * identify which call wants to return its result.
     *
     * @param id         used to identify which function did the callback
     * @param jsonString the json-string which is the result of the called function. Can be null.
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
            } else if (id == R.id.scriptresolver_resolver_collection && jsonString != null
                    && jsonString.length == 1) {
                mCollectionMetaData = mObjectMapper
                        .readValue(jsonString[0], ScriptResolverCollectionMetaData.class);
                CollectionManager.getInstance().addCollection(new ScriptResolverCollection(this));
            }
        } catch (IOException e) {
            Log.e(TAG, "handleCallbackToJava: " + e.getClass() + ": " + e
                    .getLocalizedMessage());
        }
    }

    public void addTrackResultsString(final String results) {
        ThreadManager.getInstance().execute(
                new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_REPORTING) {
                    @Override
                    public void run() {
                        ScriptResolverResult result = null;
                        try {
                            result = mObjectMapper
                                    .readValue(results, ScriptResolverResult.class);
                        } catch (IOException e) {
                            Log.e(TAG, "addTrackResultsString: " + e.getClass() + ": " + e
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
    }

    public void addAlbumResultsString(final String results) {
        ThreadManager.getInstance().execute(
                new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_REPORTING) {
                    @Override
                    public void run() {
                        ScriptResolverAlbumResult result = null;
                        try {
                            result = mObjectMapper
                                    .readValue(results, ScriptResolverAlbumResult.class);
                        } catch (IOException e) {
                            Log.e(TAG, "addAlbumResultsString: " + e.getClass() + ": " + e
                                    .getLocalizedMessage());
                        }
                        if (result != null) {
                            ScriptResolverCollection collection = (ScriptResolverCollection)
                                    CollectionManager.getInstance().getCollection(result.qid);
                            if (collection != null) {
                                Artist artist = Artist.get(result.artist);
                                ArrayList<Album> albums = new ArrayList<Album>();
                                for (String albumName : result.albums) {
                                    albums.add(Album.get(albumName, artist));
                                }
                                collection.addAlbumResults(albums);
                            }
                        }
                        mTimeOutHandler.removeCallbacksAndMessages(null);
                        mStopped = true;
                    }
                }
        );
    }

    public void addArtistResultsString(final String results) {
        ThreadManager.getInstance().execute(
                new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_REPORTING) {
                    @Override
                    public void run() {
                        ScriptResolverArtistResult result = null;
                        try {
                            result = mObjectMapper.readValue(results,
                                    ScriptResolverArtistResult.class);
                        } catch (IOException e) {
                            Log.e(TAG, "addArtistResultsString: " + e.getClass() + ": " + e
                                    .getLocalizedMessage());
                        }
                        if (result != null) {
                            ScriptResolverCollection collection = (ScriptResolverCollection)
                                    CollectionManager.getInstance().getCollection(result.qid);
                            if (collection != null) {
                                ArrayList<Artist> artists = new ArrayList<Artist>();
                                for (String artistName : result.artists) {
                                    artists.add(Artist.get(artistName));
                                }
                                collection.addArtistResults(artists);
                            }
                        }
                        mTimeOutHandler.removeCallbacksAndMessages(null);
                        mStopped = true;
                    }
                }
        );
    }

    public void addAlbumTrackResultsString(final String results) {
        ThreadManager.getInstance().execute(
                new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_REPORTING) {
                    @Override
                    public void run() {
                        ScriptResolverAlbumTrackResult result = null;
                        try {
                            result = mObjectMapper.readValue(results,
                                    ScriptResolverAlbumTrackResult.class);
                        } catch (IOException e) {
                            Log.e(TAG, "addAlbumTrackResultsString: " + e.getClass() + ": " + e
                                    .getLocalizedMessage());
                        }
                        if (result != null) {
                            ScriptResolverCollection collection = (ScriptResolverCollection)
                                    CollectionManager.getInstance().getCollection(result.qid);
                            ArrayList<Result> parsedResults = parseResultList(
                                    result.results, result.qid);
                            Artist artist = Artist.get(result.artist);
                            Album album = Album.get(result.album, artist);
                            collection.addAlbumTrackResults(album, parsedResults);
                        }
                        mTimeOutHandler.removeCallbacksAndMessages(null);
                        mStopped = true;
                    }
                }
        );
    }

    public void reportStreamUrl(String qid, String url, String stringifiedHeaders) {
        try {
            Map<String, String> headers = null;
            if (stringifiedHeaders != null) {
                headers = mObjectMapper.readValue(stringifiedHeaders, Map.class);
            }
            String resultKey = mQueryKeys.get(qid);
            PipeLine.getInstance().sendStreamUrlReportBroadcast(resultKey, url, headers);
        } catch (IOException e) {
            Log.e(TAG, "reportStreamUrl: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public void collection() {
        loadUrl("javascript:"
                + makeJSFunctionCallbackJava(R.id.scriptresolver_resolver_collection,
                "Tomahawk.resolver.instance.collection()", true));
    }

    public void tracks(String qid, String artistName, String albumName) {
        String escapedQid = StringEscapeUtils.escapeJavaScript(qid);
        String escapedArtistName = StringEscapeUtils.escapeJavaScript(artistName);
        String escapedAlbumName = StringEscapeUtils.escapeJavaScript(albumName);
        loadUrl("javascript: Tomahawk.resolver.instance.tracks( '" + escapedQid + "', '"
                + escapedArtistName + "', '" + escapedAlbumName + "' )");
    }

    public void artists(String qid) {
        String escapedQid = StringEscapeUtils.escapeJavaScript(qid);
        loadUrl("javascript: Tomahawk.resolver.instance.artists( '" + escapedQid + "' )");
    }

    public void albums(String qid, String artistName) {
        String escapedQid = StringEscapeUtils.escapeJavaScript(qid);
        String escapedArtistName = StringEscapeUtils.escapeJavaScript(artistName);
        loadUrl("javascript: Tomahawk.resolver.instance.albums( '" + escapedQid + "', '"
                + escapedArtistName + "' )");
    }

    public void loadUrl(final String url) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mScriptEngine.loadUrl(url);
            }
        });
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
                url = "javascript: Tomahawk.resolver.instance.search( '" + escapedQid + "', '"
                        + fullTextQuery + "' )";
            } else {
                String artistName = StringEscapeUtils.escapeJavaScript(query.getArtist().getName());
                String albumName = StringEscapeUtils.escapeJavaScript(query.getAlbum().getName());
                String trackName = StringEscapeUtils.escapeJavaScript(query.getName());
                url = "javascript: Tomahawk.resolver.instance.resolve( '" + escapedQid + "', '"
                        + artistName
                        + "', '" + albumName + "', '" + trackName + "' )";
            }

            // call it
            loadUrl(url);
        }
        return mReady;
    }

    public void getStreamUrl(final Result result, String callbackFuncName) {
        if (result != null) {
            String resultId = TomahawkMainActivity.getSessionUniqueStringId();
            // we are using the same map as we do when resolving queries
            mQueryKeys.put(resultId, result.getCacheKey());
            loadUrl("javascript: Tomahawk.resolver.instance." + callbackFuncName + "( '"
                    + StringEscapeUtils.escapeJavaScript(resultId)
                    + "', '" + StringEscapeUtils.escapeJavaScript(result.getPath()) + "' )");
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

    @Override
    public String getCollectionName() {
        return mCollectionMetaData.prettyname;
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
        AuthenticatorUtils utils = AuthenticatorManager.getInstance().getAuthenticatorUtils(mId);
        if (utils != null) {
            return utils.isLoggedIn();
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

    public void reportCapabilities(int in) {
        switch (in) {
            case 1:
                mBrowsable = true;
                collection();
                break;
            case 2:
                mPlaylistSync = true;
                break;
            case 4:
                mAccountFactory = true;
                break;
            case 8:
                mUrlLookup = true;
                break;
        }
    }

    public boolean isBrowsable() {
        return mBrowsable;
    }

    public boolean isPlaylistSync() {
        return mPlaylistSync;
    }

    public boolean isAccountFactory() {
        return mAccountFactory;
    }

    public boolean isUrlLookup() {
        return mUrlLookup;
    }

    public FuzzyIndex getFuzzyIndex() {
        return mFuzzyIndex;
    }

    public void setFuzzyIndex(FuzzyIndex fuzzyIndex) {
        if (mFuzzyIndex != null) {
            mFuzzyIndex.close();
        }
        mFuzzyIndex = fuzzyIndex;
    }
}
