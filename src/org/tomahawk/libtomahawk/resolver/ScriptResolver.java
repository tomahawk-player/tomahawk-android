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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.IOException;
import java.util.ArrayList;

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

    private final static String BASEURL_EXFM = "http://ex.fm";

    private final static String BASEURL_JAMENDO = "http://api.jamendo.com";

    private final static String BASEURL_SOUNDCLOUD = "http://developer.echonest.com";
    //TEMPORARY WORKAROUND END

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

    private Handler UiThreadHandler;

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
        String baseurl = "http://fake.bla.blu";
        if (getScriptFilePath().contains("officialfm.js")) {
            baseurl = BASEURL_OFFICIALFM;
        } else if (getScriptFilePath().contains("exfm.js")) {
            baseurl = BASEURL_EXFM;
        } else if (getScriptFilePath().contains("jamendo-resolver.js")) {
            baseurl = BASEURL_JAMENDO;
        } else if (getScriptFilePath().contains("soundcloud.js")) {
            baseurl = BASEURL_SOUNDCLOUD;
        }

        mScriptEngine.loadDataWithBaseURL(baseurl, "<!DOCTYPE html>" + "<html>" + "<body>"
                + "<script src=\"file:///android_asset/js/tomahawk_android.js\" type=\"text/javascript\"></script>"
                + "<script src=\"file:///android_asset/js/tomahawk.js        \" type=\"text/javascript\"></script>"
                + "<script src=\"file:///android_asset/" + mScriptFilePath
                + "\" type=\"text/javascript\"></script>" + "</body>" + "</html>", "text/html",
                null, null);
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
        UiThreadHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                mScriptEngine.loadUrl(
                        "javascript:" + RESOLVER_LEGACY_CODE + makeJSFunctionCallbackJava(
                                R.id.scriptresolver_resolver_init, "resolver.init()", false));
            }
        };
        Message message = UiThreadHandler.obtainMessage();
        message.sendToTarget();
    }

    /**
     * This method tries to get the {@link Resolver}'s settings.
     */
    private void resolverSettings() {
        UiThreadHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                mScriptEngine.loadUrl(
                        "javascript:" + RESOLVER_LEGACY_CODE + makeJSFunctionCallbackJava(
                                R.id.scriptresolver_resolver_settings,
                                "resolver.settings ? resolver.settings : getSettings() ", true));
            }
        };
        Message message = UiThreadHandler.obtainMessage();
        message.sendToTarget();
    }

    /**
     * This method tries to get the {@link Resolver}'s UserConfig.
     */
    private void resolverUserConfig() {
        UiThreadHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                mScriptEngine.loadUrl(
                        "javascript:" + RESOLVER_LEGACY_CODE + makeJSFunctionCallbackJava(
                                R.id.scriptresolver_resolver_userconfig, "resolver.getUserConfig()",
                                true));
            }
        };
        Message message = UiThreadHandler.obtainMessage();
        message.sendToTarget();
    }

    /**
     * Every callback from a function inside the javascript should first call the method
     * callbackToJava, which is exposed to javascript within the {@link ScriptInterface}. And after
     * that this callback will be handled here.
     *
     * @param id  used to identify which function did the callback
     * @param obj the {@link JSONObject} containing the {@link Result} information. Can be null
     */
    public void handleCallbackToJava(final int id, final JSONObject obj) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                //Log.d(TAG, "handleCallbackToJava: id='" + mTomahawkApp.getResources()
                //        .getResourceEntryName(id) + "(" + id + ")" + "', result='" + s + "'");
                try {
                    if (id == R.id.scriptresolver_resolver_settings && obj != null) {
                        mName = obj.getString("name");
                        mWeight = obj.getInt("weight");
                        mTimeout = obj.getInt("timeout") * 1000;
                        String[] tokens = getScriptFilePath().split("/");
                        String basepath = "";
                        for (int i = 0; i < tokens.length - 1; i++) {
                            basepath += tokens[i];
                            basepath += "/";
                        }
                        mIcon = Drawable.createFromStream(
                                mTomahawkApp.getAssets().open(basepath + obj.getString("icon")),
                                null);
                    } else if (id == R.id.scriptresolver_resolver_userconfig) {
                    } else if (id == R.id.scriptresolver_resolver_init) {
                        resolverSettings();
                    } else if (id == R.id.scriptresolver_add_track_results_string && obj != null) {
                        String qid = obj.get("qid").toString();
                        JSONArray resultList = obj.getJSONArray("results");
                        mTomahawkApp.getPipeLine().reportResults(qid, parseResultList(resultList));
                        mStopped = true;
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "handleCallbackToJava: " + e.getClass() + ": " + e
                            .getLocalizedMessage());
                } catch (IOException e) {
                    Log.e(TAG, "handleCallbackToJava: " + e.getClass() + ": " + e
                            .getLocalizedMessage());
                }
            }
        };
        Thread t = new Thread(r);
        t.start();
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
            UiThreadHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message inputMessage) {
                    if (!query.isFullTextQuery()) {
                        mScriptEngine.loadUrl(
                                "javascript:" + RESOLVER_LEGACY_CODE2 + makeJSFunctionCallbackJava(
                                        R.id.scriptresolver_resolve,
                                        "resolver.resolve( '" + query.getQid() + "', '" + query
                                                .getArtist().getName() + "', '" + query.getAlbum()
                                                .getName() + "', '" + query.getName() + "' )",
                                        false));
                    } else {
                        mScriptEngine.loadUrl(
                                "javascript:" + RESOLVER_LEGACY_CODE + makeJSFunctionCallbackJava(
                                        R.id.scriptresolver_resolve,
                                        "(Tomahawk.resolver.instance !== undefined) ?resolver.search( '"
                                                + query.getQid() + "', '" + query.getFullTextQuery()
                                                + "' ):resolve( '" + query.getQid() + "', '', '', '"
                                                + query.getFullTextQuery() + "' )", false));
                    }
                }
            };
            Message message = UiThreadHandler.obtainMessage();
            message.sendToTarget();
        }
        return mReady;
    }

    /**
     * Parses the given {@link JSONArray} into a {@link ArrayList} of {@link Result}s.
     *
     * @param resList {@link JSONArray} containing the raw result information
     * @return a {@link ArrayList} of {@link Result}s containing the parsed data
     */
    private ArrayList<Result> parseResultList(final JSONArray resList) {
        ArrayList<Result> resultList = new ArrayList<Result>();
        for (int i = 0; i < resList.length(); i++) {
            if (!resList.isNull(i)) {
                try {
                    JSONObject obj = resList.getJSONObject(i);
                    if (obj.has("url")) {
                        Artist artist;
                        Album album;
                        Track track;
                        if (obj.has("artist")) {
                            artist = Artist.get(obj.get("artist").toString());
                        } else {
                            artist = Artist.get("");
                        }
                        if (obj.has("album")) {
                            album = Album.get(obj.get("album").toString(), artist);
                        } else {
                            album = Album.get("", artist);
                        }
                        if (obj.has("track")) {
                            track = Track.get(obj.get("track").toString(), album, artist);
                        } else {
                            track = Track.get("", album, artist);
                        }
                        if (obj.has("albumpos")) {
                            track.setAlbumPos(Integer.valueOf(obj.get("albumpos").toString()));
                        }
                        if (obj.has("discnumber")) {
                            track.setAlbumPos(Integer.valueOf(obj.get("discnumber").toString()));
                        }
                        if (obj.has("year")) {
                            String yearString = obj.get("year").toString();
                            if (yearString.matches("-?\\d+")) {
                                track.setYear(Integer.valueOf(yearString));
                            }
                        }
                        if (obj.has("duration")) {
                            track.setDuration(
                                    Math.round(
                                            Float.valueOf(obj.get("duration").toString()) * 1000));
                        }
                        artist.addAlbum(album);
                        Result result = new Result(obj.get("url").toString(), track);
                        if (obj.has("bitrate")) {
                            result.setBitrate(Integer.valueOf(obj.get("bitrate").toString()));
                        }
                        if (obj.has("size")) {
                            result.setSize(Integer.valueOf(obj.get("size").toString()));
                        }
                        if (obj.has("purchaseUrl")) {
                            result.setPurchaseUrl(obj.get("purchaseUrl").toString());
                        }
                        if (obj.has("linkUrl")) {
                            result.setLinkUrl(obj.get("linkUrl").toString());
                        }
                        if (obj.has("score")) {
                            result.setTrackScore(Float.valueOf(obj.get("score").toString()));
                        }
                        result.setResolvedBy(this);
                        result.setArtist(artist);
                        result.setAlbum(album);
                        result.setTrack(track);
                        album.addQuery(new Query(result, false));
                        artist.addQuery(new Query(result, false));
                        resultList.add(result);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "parseResultList: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
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
