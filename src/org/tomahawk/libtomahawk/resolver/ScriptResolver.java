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

import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.infosystem.InfoSystemUtils;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.IOException;
import java.util.ArrayList;
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

    //TEMPORARY WORKAROUND
    private final static String BASEURL_OFFICIALFM = "http://api.official.fm";

    private final static String BASEURL_EXFM = "https://ex.fm";

    private final static String BASEURL_JAMENDO = "http://api.jamendo.com";

    private final static String BASEURL_SOUNDCLOUD = "http://developer.echonest.com";
    //TEMPORARY WORKAROUND END

    // We have to map the original cache keys to an id string, because a string containing "\t\t"
    // delimiters does come out without the delimiters, after it has been processed in the js
    // resolver script
    private ConcurrentHashMap<String, String> mQueryKeys = new ConcurrentHashMap<String, String>();

    private TomahawkApp mTomahawkApp;

    private int mId;

    private WebView mScriptEngine;

    private String mScriptFilePath;

    private String mName;

    private Drawable mIcon;

    private int mWeight;

    private int mTimeout;

    private JSONObject mConfig;

    private boolean mReady;

    private boolean mStopped;

    private ObjectMapper mObjectMapper;

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
     * @param id          the id of this {@link ScriptResolver}
     * @param tomahawkApp referenced {@link TomahawkApp}, needed to report our results in the {@link
     *                    PipeLine}
     * @param scriptPath  {@link String} containing the path to our javascript file
     */
    public ScriptResolver(int id, TomahawkApp tomahawkApp, String scriptPath) {
        mReady = false;
        mStopped = true;
        mId = id;
        mTomahawkApp = tomahawkApp;
        mScriptEngine = new WebView(mTomahawkApp);
        WebSettings settings = mScriptEngine.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        mScriptEngine.setWebChromeClient(new TomahawkWebChromeClient());
        mScriptEngine.setWebViewClient(new ScriptEngine(this));
        final ScriptInterface scriptInterface = new ScriptInterface(this);
        mScriptEngine.addJavascriptInterface(scriptInterface, SCRIPT_INTERFACE_NAME);
        String[] tokens = scriptPath.split("/");
        mName = tokens[tokens.length - 1];
        mIcon = mTomahawkApp.getResources().getDrawable(R.drawable.ic_resolver_default);
        mScriptFilePath = scriptPath;

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

    /**
     * Initialize this {@link ScriptResolver}. Loads the .js script from the given path and sets the
     * appropriate base URL.
     */
    private void init() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                String baseurl = "http://fake.bla.blu";
                if (mScriptFilePath.contains("officialfm.js")) {
                    baseurl = BASEURL_OFFICIALFM;
                } else if (mScriptFilePath.contains("exfm.js")) {
                    baseurl = BASEURL_EXFM;
                } else if (mScriptFilePath.contains("jamendo-resolver.js")) {
                    baseurl = BASEURL_JAMENDO;
                } else if (mScriptFilePath.contains("soundcloud.js")) {
                    baseurl = BASEURL_SOUNDCLOUD;
                }
                mScriptEngine.loadDataWithBaseURL(baseurl, "<!DOCTYPE html>"
                        + "<html><body>"
                        + "<script src=\"file:///android_asset/js/tomahawk_android.js"
                        + "\" type=\"text/javascript\"></script>"
                        + "<script src=\"file:///android_asset/js/tomahawk.js"
                        + "\" type=\"text/javascript\"></script>"
                        + "<script src=\"file:///android_asset/" + mScriptFilePath
                        + "\" type=\"text/javascript\"></script>"
                        + "</body></html>", "text/html", null, null);
            }
        });
    }

    /**
     * This method is being called, when the {@link ScriptEngine} has completely loaded the given
     * .js script.
     */

    public void onScriptEngineReady() {
        resolverInit();
        resolverUserConfig();
        mReady = true;
        mTomahawkApp.getPipeLine().onResolverReady();
    }

    /**
     * This method calls the js function resolver.init().
     */
    private void resolverInit() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mScriptEngine.loadUrl(
                        "javascript:" + RESOLVER_LEGACY_CODE + makeJSFunctionCallbackJava(
                                R.id.scriptresolver_resolver_init, "resolver.init()", false)
                );
            }
        });
    }

    /**
     * This method tries to get the {@link Resolver}'s settings.
     */
    private void resolverSettings() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mScriptEngine.loadUrl(
                        "javascript:" + RESOLVER_LEGACY_CODE + makeJSFunctionCallbackJava(
                                R.id.scriptresolver_resolver_settings,
                                "resolver.settings ? resolver.settings : getSettings() ", true)
                );
            }
        });
    }

    /**
     * This method tries to get the {@link Resolver}'s UserConfig.
     */
    private void resolverUserConfig() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mScriptEngine.loadUrl(
                        "javascript:" + RESOLVER_LEGACY_CODE + makeJSFunctionCallbackJava(
                                R.id.scriptresolver_resolver_userconfig, "resolver.getUserConfig()",
                                true)
                );
            }
        });
    }

    /**
     * Every callback from a function inside the javascript should first call the method
     * callbackToJava, which is exposed to javascript within the {@link ScriptInterface}. And after
     * that this callback will be handled here.
     *
     * @param id         used to identify which function did the callback
     * @param jsonString the json-string containing the {@link Result} information. Can be null
     */
    public void handleCallbackToJava(final int id, final String jsonString) {
        if (mObjectMapper == null) {
            mObjectMapper = InfoSystemUtils.constructObjectMapper();
        }
        try {
            if (id == R.id.scriptresolver_resolver_settings && jsonString != null) {
                ScriptResolverSettings settings = mObjectMapper
                        .readValue(jsonString, ScriptResolverSettings.class);
                mName = settings.name;
                mWeight = settings.weight;
                mTimeout = settings.timeout * 1000;
                String[] tokens = mScriptFilePath.split("/");
                String basepath = "";
                for (int i = 0; i < tokens.length - 1; i++) {
                    basepath += tokens[i];
                    basepath += "/";
                }
                mIcon = Drawable
                        .createFromStream(mTomahawkApp.getAssets().open(basepath + settings.icon),
                                null);
            } else if (id == R.id.scriptresolver_resolver_userconfig) {
            } else if (id == R.id.scriptresolver_resolver_init) {
                resolverSettings();
            } else if (id == R.id.scriptresolver_add_track_results_string && jsonString != null) {
                mTomahawkApp.getThreadManager()
                        .execute(new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_REPORTING) {
                            @Override
                            public void run() {
                                ScriptResolverResult result = null;
                                try {
                                    result = mObjectMapper
                                            .readValue(jsonString, ScriptResolverResult.class);
                                } catch (IOException e) {
                                    Log.e(TAG, "handleCallbackToJava: " + e.getClass() + ": " + e
                                            .getLocalizedMessage());
                                }
                                if (result != null) {
                                    ArrayList<Result> parsedResults = parseResultList(
                                            result.results);
                                    mTomahawkApp.getPipeLine()
                                            .reportResults(mQueryKeys.get(result.qid),
                                                    parsedResults, mId);
                                }
                                mTimeOutHandler.removeCallbacksAndMessages(null);
                                mStopped = true;
                            }
                        });
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
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    String qid = TomahawkApp.getSessionUniqueStringId();
                    mQueryKeys.put(qid, TomahawkUtils.getCacheKey(query));
                    if (!query.isFullTextQuery()) {
                        mScriptEngine.loadUrl(
                                "javascript:" + RESOLVER_LEGACY_CODE2 + makeJSFunctionCallbackJava(
                                        R.id.scriptresolver_resolve,
                                        "resolver.resolve( '"
                                                + qid.replace("'", "\\'")
                                                + "', '"
                                                + query.getArtist().getName().replace("'", "\\'")
                                                + "', '"
                                                + query.getAlbum().getName().replace("'", "\\'")
                                                + "', '"
                                                + query.getName().replace("'", "\\'")
                                                + "' )",
                                        false
                                )
                        );
                    } else {
                        mScriptEngine.loadUrl(
                                "javascript:" + RESOLVER_LEGACY_CODE + makeJSFunctionCallbackJava(
                                        R.id.scriptresolver_resolve,
                                        "(Tomahawk.resolver.instance !== undefined) ?resolver.search( '"
                                                + qid.replace("'", "\\'")
                                                + "', '"
                                                + query.getFullTextQuery().replace("'", "\\'")
                                                + "' ):resolve( '"
                                                + qid.replace("'", "\\'")
                                                + "', '', '', '"
                                                + query.getFullTextQuery().replace("'", "\\'")
                                                + "' )", false
                                )
                        );
                    }
                }
            });
        }
        return mReady;
    }

    /**
     * Parses the given {@link JSONArray} into a {@link ArrayList} of {@link Result}s.
     *
     * @param resultEntries ArrayList of ScriptResolverResultEntries containing the raw result
     *                      information
     * @return a {@link ArrayList} of {@link Result}s containing the parsed data
     */
    private ArrayList<Result> parseResultList(ArrayList<ScriptResolverResultEntry> resultEntries) {
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

                Result result = new Result(resultEntry.url, track);
                result.setBitrate(resultEntry.bitrate);
                result.setSize(resultEntry.size);
                result.setPurchaseUrl(resultEntry.purchaseUrl);
                result.setLinkUrl(resultEntry.linkUrl);
                result.setResolvedBy(this);
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
    public int getId() {
        return mId;
    }

    /**
     * @return the absolute filepath (without file://android_asset) of the corresponding script
     */
    public String getScriptFilePath() {
        return mScriptFilePath;
    }

    /**
     * @return the {@link JSONObject} containing the Config information, which was returned by the
     * corresponding script
     */
    public JSONObject getConfig() {
        return mConfig;
    }

    /**
     * @return the {@link Drawable} which has been created by loading the image the js function
     * attribute "icon" pointed at
     */
    @Override
    public Drawable getIcon() {
        return mIcon;
    }

    /**
     * @return this {@link ScriptResolver}'s weight
     */
    @Override
    public int getWeight() {
        return mWeight;
    }

}
