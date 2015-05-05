/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.tomahawk.libtomahawk.infosystem.InfoSystemUtils;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverCollectionMetaData;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverMetaData;
import org.tomahawk.libtomahawk.resolver.plugins.ScriptCollectionPluginFactory;
import org.tomahawk.libtomahawk.resolver.plugins.ScriptInfoPluginFactory;
import org.tomahawk.libtomahawk.resolver.plugins.ScriptResolverPluginFactory;
import org.tomahawk.libtomahawk.utils.StringEscapeUtils;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptAccount implements ScriptWebViewClient.WebViewClientReadyListener {

    private final static String TAG = ScriptAccount.class.getSimpleName();

    public final static String SCRIPT_INTERFACE_NAME = "Tomahawk";

    public final static String CONFIG = "config";

    public final static String ENABLED_KEY = "_enabled_";

    private String mPath;

    private String mName;

    private WebView mWebView;

    private HashMap<String, ScriptJob> mJobs = new HashMap<>();

    private HashMap<String, ScriptObject> mObjects = new HashMap<>();

    private ScriptResolverPluginFactory mResolverPluginFactory =
            new ScriptResolverPluginFactory();

    private ScriptCollectionPluginFactory mCollectionPluginFactory =
            new ScriptCollectionPluginFactory();

    private ScriptInfoPluginFactory mInfoPluginFactory =
            new ScriptInfoPluginFactory();

    private boolean mStopped;

    private ScriptResolver mScriptResolver;

    private ScriptResolverMetaData mMetaData;

    public ScriptResolverCollectionMetaData mCollectionMetaData;

    public String mCollectionIconPath;

    public ScriptAccount(String path) {
        mPath = path;
        String[] parts = mPath.split("/");
        mName = parts[parts.length - 1];
        try {
            String rawJsonString = TomahawkUtils.inputStreamToString(TomahawkApp.getContext()
                    .getAssets().open(mPath + "/content/metadata.json"));
            mMetaData = InfoSystemUtils.getObjectMapper().readValue(rawJsonString,
                    ScriptResolverMetaData.class);
            if (mMetaData == null) {
                Log.e(TAG, "Couldn't read metadata.json. Cannot instantiate ScriptResolver.");
                return;
            }
        } catch (IOException e) {
            Log.e(TAG, "ScriptResolver: " + e.getClass() + ": " + e.getLocalizedMessage());
        }

        mWebView = new WebView(TomahawkApp.getContext());
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDatabaseEnabled(true);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //noinspection deprecation
            settings.setDatabasePath(
                    TomahawkApp.getContext().getDir("databases", Context.MODE_PRIVATE)
                            .getPath());
        }
        settings.setDomStorageEnabled(true);
        mWebView.setWebChromeClient(new TomahawkWebChromeClient());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                //initalize WebView
                String data = "<!DOCTYPE html>" + "<html>"
                        + "<head><title>" + mName + "</title></head>"
                        + "<body>"
                        + "<script src=\"file:///android_asset/js/rsvp-latest.min.js"
                        + "\" type=\"text/javascript\"></script>"
                        + "<script src=\"file:///android_asset/js/cryptojs-core.js"
                        + "\" type=\"text/javascript\"></script>";
                for (String scriptPath : mMetaData.manifest.scripts) {
                    data += "<script src=\"file:///android_asset/" + mPath + "/content/"
                            + scriptPath + "\" type=\"text/javascript\"></script>";
                }
                try {
                    String[] cryptoJsScripts =
                            TomahawkApp.getContext().getAssets().list("js/cryptojs");
                    for (String scriptPath : cryptoJsScripts) {
                        data += "<script src=\"file:///android_asset/js/cryptojs/"
                                + scriptPath
                                + "\" type=\"text/javascript\"></script>";
                    }
                } catch (IOException e) {
                    Log.e(TAG,
                            "ScriptResolver: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
                data += "<script src=\"file:///android_asset/js/tomahawk_android_pre.js"
                        + "\" type=\"text/javascript\"></script>"
                        + "<script src=\"file:///android_asset/js/tomahawk.js"
                        + "\" type=\"text/javascript\"></script>"
                        + "<script src=\"file:///android_asset/js/tomahawk-infosystem.js"
                        + "\" type=\"text/javascript\"></script>"
                        + "<script src=\"file:///android_asset/js/tomahawk_android_post.js"
                        + "\" type=\"text/javascript\"></script>"
                        + "<script src=\"file:///android_asset/" + mPath + "/content/"
                        + mMetaData.manifest.main + "\" type=\"text/javascript\"></script>"
                        + "</body></html>";
                mWebView.setWebViewClient(new ScriptWebViewClient(ScriptAccount.this));
                mWebView.addJavascriptInterface(new ScriptInterface(ScriptAccount.this),
                        SCRIPT_INTERFACE_NAME);
                mWebView.loadDataWithBaseURL("file:///android_asset/test.html", data,
                        "text/html", null, null);
            }
        });
    }

    /**
     * This method is being called, when the {@link ScriptWebViewClient} has completely loaded the
     * given .js script.
     */
    @Override
    public void onWebViewClientReady() {
        registerPlugin(ScriptObject.TYPE_RESOLVER);
    }

    public ScriptResolver getScriptResolver() {
        return mScriptResolver;
    }

    public void setScriptResolver(ScriptResolver scriptResolver) {
        mScriptResolver = scriptResolver;
    }

    public ScriptResolverMetaData getMetaData() {
        return mMetaData;
    }

    public String getPath() {
        return mPath;
    }

    public String getName() {
        return mName;
    }

    public void setConfig(Map<String, Object> config) {
        try {
            String rawJsonString = InfoSystemUtils.getObjectMapper().writeValueAsString(config);
            PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext())
                    .edit().putString(buildPreferenceKey(), rawJsonString).commit();
            mScriptResolver.resolverSaveUserConfig();
        } catch (IOException e) {
            Log.e(TAG, "setConfig: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    /**
     * @return the Map<String, String> containing the Config information of this resolver
     */
    public Map<String, Object> getConfig() {
        String rawJsonString = PreferenceManager.getDefaultSharedPreferences(
                TomahawkApp.getContext()).getString(buildPreferenceKey(), "");
        try {
            return InfoSystemUtils.getObjectMapper().readValue(rawJsonString, Map.class);
        } catch (IOException e) {
            Log.e(TAG, "getConfig: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return new HashMap<>();
    }

    private String buildPreferenceKey() {
        return mMetaData.pluginName + "_" + CONFIG;
    }

    public void registerPlugin(String type) {
        evaluateJavaScript("Tomahawk.PluginManager.registerPlugin('" + type
                + "', Tomahawk.resolver.instance);");
    }

    public void unregisterPlugin(String type) {
        evaluateJavaScript("Tomahawk.PluginManager.unregisterPlugin('" + type
                + "', Tomahawk.resolver.instance);");
    }

    public void startJob(final ScriptJob job) {
        final String requestId = TomahawkMainActivity.getSessionUniqueStringId();
        mJobs.put(requestId, job);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    String serializedArgs = InfoSystemUtils.getObjectMapper()
                            .writeValueAsString(job.getArguments());
                    serializedArgs = "JSON.parse('" + StringEscapeUtils
                            .escapeJavaScript(serializedArgs) + "')";
                    evaluateJavaScript("Tomahawk.PluginManager.invoke("
                            + "'" + requestId + "',"
                            + "'" + job.getScriptObject().getId() + "',"
                            + "'" + job.getMethodName() + "',"
                            + serializedArgs + ")");
                } catch (JsonProcessingException e) {
                    Log.e(TAG, "startJob: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
            }
        });
    }

    private void evaluateJavaScript(final String code) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl("javascript: " + code);
            }
        });
    }

    public void reportScriptJobResult(JsonNode result) {
        String requestId = result.get("requestId").asText();
        if (requestId.isEmpty()) {
            Log.e(TAG, "reportScriptJobResult - ScriptAccount:" + mName + ", requestId is empty");
        }
        ScriptJob job = mJobs.get(requestId);
        if (result.get("error") == null) {
            JsonNode data = result.get("data");
            job.reportResults(data);
        } else {
            job.reportFailure(result.get("error").asText());
        }
    }

    public void registerScriptPlugin(String type, String objectId) {
        ScriptObject object = mObjects.get(objectId);
        if (object == null) {
            object = new ScriptObject(objectId, this);
            mObjects.put(objectId, object);
        }
        switch (type) {
            case ScriptObject.TYPE_RESOLVER:
                mResolverPluginFactory.registerPlugin(object, this);
                break;
            case ScriptObject.TYPE_COLLECTION:
                mCollectionPluginFactory.registerPlugin(object, this);
                break;
            case ScriptObject.TYPE_INFOPLUGIN:
                mInfoPluginFactory.registerPlugin(object, this);
                break;
            default:
                Log.e(TAG, "registerScriptPlugin - ScriptAccount:" + mName
                        + ", ScriptPlugin type not supported!");
        }
    }

    public void unregisterScriptPlugin(String type, String objectId) {
        ScriptObject object = mObjects.get(objectId);
        if (object == null) {
            Log.e(TAG, "unregisterScriptPlugin - ScriptAccount:" + mName
                    + ", tried to unregister a plugin that was not registered!");
        }
        switch (type) {
            case ScriptObject.TYPE_RESOLVER:
                mResolverPluginFactory.unregisterPlugin(object);
                break;
            case ScriptObject.TYPE_COLLECTION:
                mCollectionPluginFactory.unregisterPlugin(object);
                break;
            case ScriptObject.TYPE_INFOPLUGIN:
                mInfoPluginFactory.unregisterPlugin(object);
                break;
            default:
                Log.e(TAG, "unregisterScriptPlugin - ScriptAccount:" + mName
                        + ", ScriptPlugin type not supported!");
        }
    }

    public boolean isStopped() {
        return mStopped;
    }

    public void nativeAsyncRequestDone(final int requestId, final String responseText,
            final Map<String, List<String>> responseHeaders, final int status,
            final String statusText) {
        final Map<String, String> headers = new HashMap<>();
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
            String headersString = InfoSystemUtils.getObjectMapper().writeValueAsString(headers);
            evaluateJavaScript("Tomahawk._nativeAsyncRequestDone(" + requestId + ","
                    + "'" + StringEscapeUtils.escapeJavaScript(responseText) + "',"
                    + "'" + StringEscapeUtils.escapeJavaScript(headersString) + "',"
                    + status + ","
                    + "'" + StringEscapeUtils.escapeJavaScript(statusText) + "');");
        } catch (IOException e) {
            Log.e(TAG, "nativeAsyncRequestDone: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

}
