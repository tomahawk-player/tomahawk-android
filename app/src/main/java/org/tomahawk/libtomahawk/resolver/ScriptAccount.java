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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.tomahawk.libtomahawk.database.CollectionDb;
import org.tomahawk.libtomahawk.database.CollectionDbManager;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverMetaData;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverTrack;
import org.tomahawk.libtomahawk.resolver.plugins.ScriptCollectionPluginFactory;
import org.tomahawk.libtomahawk.resolver.plugins.ScriptInfoPluginFactory;
import org.tomahawk.libtomahawk.resolver.plugins.ScriptResolverPluginFactory;
import org.tomahawk.libtomahawk.utils.GsonHelper;
import org.tomahawk.libtomahawk.utils.StringUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptAccount implements ScriptWebViewClient.WebViewClientReadyListener {

    private final static String TAG = ScriptAccount.class.getSimpleName();

    public final static String SCRIPT_INTERFACE_NAME = "Tomahawk";

    public final static String CONFIG = "config";

    public final static String ENABLED_KEY = "_enabled_";

    private String mPath;

    private boolean mManuallyInstalled;

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

    private ScriptResolver mScriptResolver;

    private ScriptResolverMetaData mMetaData;

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    public ScriptAccount(String path, boolean manuallyInstalled) {
        String prefix = manuallyInstalled ? "file://" : "file:///android_asset";
        mPath = prefix + path;
        mManuallyInstalled = manuallyInstalled;
        String[] parts = mPath.split("/");
        mName = parts[parts.length - 1];
        InputStream inputStream = null;
        try {
            if (mManuallyInstalled) {
                File metadataFile = new File(
                        path + File.separator + "content" + File.separator + "metadata.json");
                inputStream = new FileInputStream(metadataFile);
            } else {
                inputStream = TomahawkApp.getContext().getAssets()
                        .open(path.substring(1) + "/content/metadata.json");
            }
            String metadataString = IOUtils.toString(inputStream, Charsets.UTF_8);
            mMetaData = GsonHelper.get().fromJson(metadataString, ScriptResolverMetaData.class);
            if (mMetaData == null) {
                Log.e(TAG, "Couldn't read metadata.json. Cannot instantiate ScriptAccount.");
                return;
            }
        } catch (IOException e) {
            Log.e(TAG, "ScriptAccount: " + e.getClass() + ": " + e.getLocalizedMessage());
            Log.e(TAG, "Couldn't read metadata.json. Cannot instantiate ScriptAccount.");
            return;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "ScriptAccount: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
            }
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
                if (mMetaData.manifest.scripts != null) {
                    for (String scriptPath : mMetaData.manifest.scripts) {
                        data += "<script src=\"" + mPath + "/content/" + scriptPath
                                + "\" type=\"text/javascript\"></script>";
                    }
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
                        + "<script src=\"" + mPath + "/content/" + mMetaData.manifest.main
                        + "\" type=\"text/javascript\"></script>"
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
        PipeLine.get().onPluginLoaded(this);
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
        String rawJsonString = GsonHelper.get().toJson(config);
        PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext())
                .edit().putString(buildPreferenceKey(), rawJsonString).commit();
        mScriptResolver.saveUserConfig();
    }

    /**
     * @return the Map<String, String> containing the Config information of this resolver
     */
    public Map<String, Object> getConfig() {
        String rawJsonString = PreferenceManager.getDefaultSharedPreferences(
                TomahawkApp.getContext()).getString(buildPreferenceKey(), "");
        Map<String, Object> result = GsonHelper.get().fromJson(rawJsonString, Map.class);
        if (result == null) {
            result = new HashMap<>();
        }
        return result;
    }

    public boolean isManuallyInstalled() {
        return mManuallyInstalled;
    }

    private String buildPreferenceKey() {
        return mMetaData.pluginName + "_" + CONFIG;
    }

    public void registerPlugin(String type) {
        evaluateJavaScript("Tomahawk.PluginManager.registerPlugin('" + type
                + "', Tomahawk.resolver.instance);");
    }

    public void unregisterAllPlugins() {
        for (String objectId : mResolverPluginFactory.getScriptPlugins().keySet()) {
            evaluateJavaScript("Tomahawk.PluginManager.unregisterPlugin('"
                    + ScriptObject.TYPE_RESOLVER + "', " + objectId + ");");
        }
        for (String objectId : mCollectionPluginFactory.getScriptPlugins().keySet()) {
            evaluateJavaScript("Tomahawk.PluginManager.unregisterPlugin('"
                    + ScriptObject.TYPE_COLLECTION + "', " + objectId + ");");
        }
        for (String objectId : mInfoPluginFactory.getScriptPlugins().keySet()) {
            evaluateJavaScript("Tomahawk.PluginManager.unregisterPlugin('"
                    + ScriptObject.TYPE_INFOPLUGIN + "', " + objectId + ");");
        }
    }

    public void startJob(final ScriptJob job) {
        final String requestId = TomahawkMainActivity.getSessionUniqueStringId();
        mJobs.put(requestId, job);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                String serializedArgs = GsonHelper.get().toJson(job.getArguments());
                serializedArgs = "JSON.parse('" + StringUtils
                        .escapeJavaScript(serializedArgs) + "')";
                evaluateJavaScript("Tomahawk.PluginManager.invoke("
                        + "'" + requestId + "',"
                        + "'" + job.getScriptObject().getId() + "',"
                        + "'" + job.getMethodName() + "',"
                        + serializedArgs + ")");
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

    public void reportScriptJobResult(JsonObject result) {
        JsonElement requestIdNode = result.get("requestId");
        String requestId = null;
        if (requestIdNode != null && requestIdNode.isJsonPrimitive()) {
            requestId = result.get("requestId").getAsString();
        }
        if (requestId != null && !requestId.isEmpty()) {
            ScriptJob job = mJobs.get(requestId);
            if (job != null) {
                JsonElement errorNode = result.get("error");
                if (errorNode == null) {
                    job.reportResults(result.get("data"));
                } else if (errorNode.isJsonPrimitive()) {
                    job.reportFailure(result.get("error").getAsString());
                } else {
                    job.reportFailure("no error message provided");
                }
            } else {
                Log.e(TAG, "reportScriptJobResult - ScriptAccount:" + mName
                        + ", couldn't find ScriptJob with given requestId");
            }
        } else {
            Log.e(TAG, "reportScriptJobResult - ScriptAccount:" + mName
                    + ", requestId is null or empty");
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
        String headersString = GsonHelper.get().toJson(headers);
        // We have to encode the %-chars because the Android WebView automatically decodes
        // percentage-escaped chars ... for whatever reason. Seems likely that this is a bug.
        String escapedResponseText =
                StringUtils.escapeJavaScript(responseText).replace("%", "%25");
        String escapedHeadersString =
                StringUtils.escapeJavaScript(headersString).replace("%", "%25");
        String escapedStatusText =
                StringUtils.escapeJavaScript(statusText).replace("%", "%25");
        evaluateJavaScript("Tomahawk._nativeAsyncRequestDone(" + requestId + ","
                + "'" + escapedResponseText + "',"
                + "'" + escapedHeadersString + "',"
                + status + ","
                + "'" + escapedStatusText + "');");
    }

    public class NativeScriptJobParams {

        String id;

        ScriptResolverTrack[] tracks;
    }

    public void invokeNativeScriptJob(int requestId, String methodName, String paramsString) {
        String result = null;
        NativeScriptJobParams params =
                GsonHelper.get().fromJson(paramsString, NativeScriptJobParams.class);
        switch (methodName) {
            case "collectionAddTracks": {
                CollectionDb collectionDb = CollectionDbManager.get().getCollectionDb(params.id);
                collectionDb.addTracks(params.tracks);
                result = collectionDb.getRevision();
                break;
            }
            case "collectionWipe":
                CollectionDbManager.get().getCollectionDb(params.id).wipe();
                break;
            case "collectionRevision": {
                CollectionDb collectionDb = CollectionDbManager.get().getCollectionDb(params.id);
                result = collectionDb.getRevision();
                break;
            }
            case "collectionInitialized":
                CollectionDbManager.get().getCollectionDb(params.id).wipe();
                break;
        }
        if (result == null) {
            evaluateJavaScript("Tomahawk.NativeScriptJobManager.reportNativeScriptJobResult("
                    + requestId + ");");
        } else {
            evaluateJavaScript("Tomahawk.NativeScriptJobManager.reportNativeScriptJobResult("
                    + requestId + ", '" + result + "');");
        }
    }

}
