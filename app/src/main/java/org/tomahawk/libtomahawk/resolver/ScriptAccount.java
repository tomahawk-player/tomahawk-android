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
import com.google.gson.reflect.TypeToken;

import com.squareup.okhttp.Response;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.tomahawk.libtomahawk.database.CollectionDb;
import org.tomahawk.libtomahawk.database.CollectionDbManager;
import org.tomahawk.libtomahawk.resolver.models.ScriptInterfaceRequestOptions;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverMetaData;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverTrack;
import org.tomahawk.libtomahawk.resolver.plugins.ScriptChartProviderPluginFactory;
import org.tomahawk.libtomahawk.resolver.plugins.ScriptCollectionPluginFactory;
import org.tomahawk.libtomahawk.resolver.plugins.ScriptInfoPluginFactory;
import org.tomahawk.libtomahawk.resolver.plugins.ScriptPlaylistGeneratorFactory;
import org.tomahawk.libtomahawk.resolver.plugins.ScriptResolverPluginFactory;
import org.tomahawk.libtomahawk.utils.GsonHelper;
import org.tomahawk.libtomahawk.utils.ImageUtils;
import org.tomahawk.libtomahawk.utils.NetworkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.utils.IdGenerator;
import org.tomahawk.tomahawk_android.utils.PreferenceUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

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

    private ScriptChartProviderPluginFactory mChartsProviderPluginFactory =
            new ScriptChartProviderPluginFactory();

    private ScriptPlaylistGeneratorFactory mPlaylistGeneratorFactory =
            new ScriptPlaylistGeneratorFactory();

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

        CookieManager.setAcceptFileSchemeCookies(true);

        mWebView = new WebView(TomahawkApp.getContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView, true);
        }
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
        //TODO: Remove this hack once we can get rid of Tomahawk.resolver.instance completely
        evaluateJavaScript("Tomahawk.resolver.instance = Tomahawk.resolver.instance "
                + "|| Tomahawk.extend(Tomahawk.Resolver, {});"
                + "Tomahawk.PluginManager.registerPlugin('" + ScriptObject.TYPE_RESOLVER
                + "', Tomahawk.resolver.instance);");
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
        PreferenceUtils.edit().putString(buildPreferenceKey(), rawJsonString).commit();
        mScriptResolver.saveUserConfig();
    }

    /**
     * @return the Map<String, String> containing the Config information of this resolver
     */
    public Map<String, Object> getConfig() {
        String rawJsonString = PreferenceUtils.getString(buildPreferenceKey());
        Map<String, Object> result = null;
        if (rawJsonString != null) {
            result = GsonHelper.get().fromJson(rawJsonString, Map.class);
        }
        if (result == null) {
            result = new HashMap<>();
        }
        return result;
    }

    public void loadIcon(ImageView imageView, boolean grayOut) {
        ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                mPath + "/content/" + mMetaData.manifest.icon,
                grayOut ? R.color.disabled_resolver : 0);
    }

    public void loadIconWhite(ImageView imageView, int tintColorResId) {
        ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                mPath + "/content/" + mMetaData.manifest.iconWhite, tintColorResId);
    }

    public String getIconBackgroundPath() {
        return mPath + "/content/" + mMetaData.manifest.iconBackground;
    }

    public void loadIconBackground(ImageView imageView, boolean grayOut) {
        ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                mPath + "/content/" + mMetaData.manifest.iconBackground,
                grayOut ? R.color.disabled_resolver : 0);
    }

    public boolean isManuallyInstalled() {
        return mManuallyInstalled;
    }

    private String buildPreferenceKey() {
        return mName + "_" + CONFIG;
    }

    public void unregisterAllPlugins() {
        //TODO: Uncomment this once we can get rid of Tomahawk.resolver.instance completely
        /*
        for (String objectId : mResolverPluginFactory.getScriptPlugins().keySet()) {
            String json = mObjects.get(objectId).toJson();
            evaluateJavaScript("Tomahawk.PluginManager.unregisterPlugin('"
                    + ScriptObject.TYPE_RESOLVER + "', " + json + ");");
        }
        */
        for (String objectId : mCollectionPluginFactory.getScriptPlugins().keySet()) {
            String json = mObjects.get(objectId).toJson();
            evaluateJavaScript("Tomahawk.PluginManager.unregisterPlugin('"
                    + ScriptObject.TYPE_COLLECTION + "', " + json + ");");
        }
        for (String objectId : mInfoPluginFactory.getScriptPlugins().keySet()) {
            String json = mObjects.get(objectId).toJson();
            evaluateJavaScript("Tomahawk.PluginManager.unregisterPlugin('"
                    + ScriptObject.TYPE_INFOPLUGIN + "', " + json + ");");
        }
    }

    public void startJob(final ScriptJob job) {
        final String requestId = IdGenerator.getSessionUniqueStringId();
        mJobs.put(requestId, job);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                evaluateJavaScript("Tomahawk.PluginManager.invoke("
                        + "'" + requestId + "',"
                        + "'" + job.getScriptObject().getId() + "',"
                        + "'" + job.getMethodName() + "',"
                        + GsonHelper.get().toJson(job.getArguments()) + ")");
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
                PipeLine.get().onPluginLoaded(this);
                break;
            case ScriptObject.TYPE_COLLECTION:
                mCollectionPluginFactory.registerPlugin(object, this);
                break;
            case ScriptObject.TYPE_INFOPLUGIN:
                mInfoPluginFactory.registerPlugin(object, this);
                break;
            case ScriptObject.TYPE_CHARTSPROVIDER:
                mChartsProviderPluginFactory.registerPlugin(object, this);
                break;
            case ScriptObject.TYPE_PLAYLISTGENERATOR:
                mPlaylistGeneratorFactory.registerPlugin(object, this);
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
        } else {
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
                case ScriptObject.TYPE_CHARTSPROVIDER:
                    mChartsProviderPluginFactory.unregisterPlugin(object);
                    break;
                case ScriptObject.TYPE_PLAYLISTGENERATOR:
                    mPlaylistGeneratorFactory.unregisterPlugin(object);
                    break;
                default:
                    Log.e(TAG, "unregisterScriptPlugin - ScriptAccount:" + mName
                            + ", ScriptPlugin type not supported!");
            }
        }
    }

    public void invokeNativeScriptJob(int requestId, String methodName, String paramsString) {
        JsonObject params = GsonHelper.get().fromJson(paramsString, JsonObject.class);
        if (methodName.equals("collectionAddTracks")) {
            String id = params.get("id").getAsString();
            List<ScriptResolverTrack> tracks = GsonHelper.get().fromJson(
                    params.getAsJsonArray("tracks"),
                    new TypeToken<List<ScriptResolverTrack>>() {
                    }.getType());

            CollectionDb collectionDb = CollectionDbManager.get().getCollectionDb(id);
            collectionDb.addTracks(tracks);

            reportNativeScriptJobResult(requestId, "'" + collectionDb.getRevision() + "'");
        } else if (methodName.equals("collectionWipe")) {
            String id = params.get("id").getAsString();

            CollectionDbManager.get().getCollectionDb(id).wipe();

            reportNativeScriptJobResult(requestId, null);
        } else if (methodName.equals("collectionRevision")) {
            String id = params.get("id").getAsString();

            CollectionDb collectionDb = CollectionDbManager.get().getCollectionDb(id);

            reportNativeScriptJobResult(requestId, "'" + collectionDb.getRevision() + "'");
        } else if (methodName.equals("collectionInitialized")) {
            String id = params.get("id").getAsString();

            CollectionDbManager.get().getCollectionDb(id).wipe();

            reportNativeScriptJobResult(requestId, null);
        } else if (methodName.equals("httpRequest")) {
            ScriptInterfaceRequestOptions options =
                    GsonHelper.get().fromJson(paramsString, ScriptInterfaceRequestOptions.class);

            reportNativeScriptJobResult(requestId, GsonHelper.get().toJson(jsHttpRequest(options)));
        } else if (methodName.equals("showWebView")) {
            String url = params.get("url").getAsString();

            // This will open up a WebViewActivity, which will call onShowWebViewFinished when
            // finished
            TomahawkMainActivity.ShowWebViewEvent event
                    = new TomahawkMainActivity.ShowWebViewEvent();
            event.mRequestid = requestId;
            event.mUrl = url;
            EventBus.getDefault().post(event);
        }
    }

    private void reportNativeScriptJobResult(int requestId, String result) {
        if (result == null) {
            evaluateJavaScript("Tomahawk.NativeScriptJobManager.reportNativeScriptJobResult( "
                    + requestId + " );");
        } else {
            evaluateJavaScript("Tomahawk.NativeScriptJobManager.reportNativeScriptJobResult( "
                    + requestId + ", " + result + " );");
        }
    }

    public void onShowWebViewFinished(int requestId, String url) {
        if (url != null) {
            HashMap<String, Object> args = new HashMap<>();
            args.put("url", url);
            reportNativeScriptJobResult(requestId, GsonHelper.get().toJson(args));
        }
    }

    private JsonObject jsHttpRequest(ScriptInterfaceRequestOptions options) {
        Response response = null;
        try {
            String url = null;
            Map<String, String> headers = null;
            String method = null;
            String username = null;
            String password = null;
            String data = null;
            boolean isTestingConfig = false;
            if (options != null) {
                url = options.url;
                headers = options.headers;
                method = options.method;
                username = options.username;
                password = options.password;
                data = options.data;
                isTestingConfig = options.isTestingConfig;
            }
            java.net.CookieManager cookieManager = getCookieManager(isTestingConfig);
            response = NetworkUtils.httpRequest(method, url, headers, username, password, data,
                    true, cookieManager);
            // We have to encode the %-chars because the Android WebView automatically decodes
            // percentage-escaped chars ... for whatever reason. Seems likely that this is a bug.
            String responseText = response.body().string().replace("%", "%25");
            JsonObject responseHeaders = new JsonObject();
            for (String headerName : response.headers().names()) {
                String concatenatedValues = "";
                for (int i = 0; i < response.headers(headerName).size(); i++) {
                    if (i > 0) {
                        concatenatedValues += "\n";
                    }
                    concatenatedValues += response.headers(headerName).get(i);
                }
                String escapedKey = headerName.toLowerCase().replace("%", "%25");
                String escapedValue = concatenatedValues.replace("%", "%25");
                responseHeaders.addProperty(escapedKey, escapedValue);
            }
            int status = response.code();
            String statusText = response.message().replace("%", "%25");

            JsonObject result = new JsonObject();
            result.addProperty("responseText", responseText);
            result.add("responseHeaders", responseHeaders);
            result.addProperty("status", status);
            result.addProperty("statusText", statusText);
            return result;
        } catch (IOException e) {
            Log.e(TAG, "jsHttpRequest: " + e.getClass() + ": " + e.getLocalizedMessage());
            return null;
        } finally {
            if (response != null) {
                try {
                    response.body().close();
                } catch (IOException e) {
                    Log.e(TAG, "jsHttpRequest: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
            }
        }
    }

    public java.net.CookieManager getCookieManager(boolean isTestingConfig) {
        String cookieContextId;
        if (isTestingConfig) {
            cookieContextId = mName + "_testConfig";
        } else {
            cookieContextId = mName;
        }
        return NetworkUtils.getCookieManager(cookieContextId);
    }

}
