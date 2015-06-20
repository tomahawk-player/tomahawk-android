package org.tomahawk.libtomahawk.resolver;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.tomahawk.libtomahawk.resolver.models.ScriptInterfaceRequestOptions;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverData;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverFuzzyIndex;
import org.tomahawk.libtomahawk.utils.GsonHelper;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;

import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * This class contains all methods that are being exposed to the javascript script inside a {@link
 * ScriptResolver} object.
 */
public class ScriptInterface {

    private final static String TAG = ScriptInterface.class.getSimpleName();

    private final ScriptAccount mScriptAccount;

    /**
     * Class to make a callback on the javascript side of this ScriptInterface. The callback is
     * stored in a map on the js side and can be identified by its callback-id, which is given to
     * this JsCallback in its constructor.
     */
    public class JsCallback {

        private final int mReqId;

        public JsCallback(int reqId) {
            mReqId = reqId;
        }

        public void call(TomahawkUtils.HttpResponse response) {
            mScriptAccount.nativeAsyncRequestDone(mReqId, response.mResponseText,
                    response.mResponseHeaders, response.mStatus, response.mStatusText);
        }
    }

    ScriptInterface(ScriptAccount scriptAccount) {
        mScriptAccount = scriptAccount;
    }

    /**
     * This method is needed because the javascript script is expecting an exposed method which will
     * return the scriptPath and config. This method is being called in tomahawk_android_pre.js
     *
     * @return a serialized JSON-{@link String} containing the scriptPath and config.
     */
    @JavascriptInterface
    public String resolverDataString() {
        Map<String, Object> config = mScriptAccount.getConfig();
        ScriptResolverData data = new ScriptResolverData();
        data.scriptPath = mScriptAccount.getPath() + "/content/" + mScriptAccount
                .getMetaData().manifest.main;
        data.config = config;
        return GsonHelper.get().toJson(data);
    }

    /**
     * A straightforward log method to write something into the Debug log.
     */
    @JavascriptInterface
    public void log(String message) {
        Log.d(TAG, "log: " + mScriptAccount.getName() + ": " + message);
    }

    /**
     * This method is needed because the javascript script is expecting an exposed method which it
     * can call to report its capabilities. This method is being called in tomahawk_android_pre.js
     *
     * @param in the int pointing to the script's capabilities
     */
    @JavascriptInterface
    public void nativeReportCapabilities(int in) {
        if (mScriptAccount.getScriptResolver() != null) {
            mScriptAccount.getScriptResolver().reportCapabilities(in);
        } else {
            Log.e(TAG, "nativeReportCapabilities - ScriptResolver not set in ScriptAccount: "
                    + mScriptAccount.getName());
        }
    }

    @JavascriptInterface
    public void addCustomUrlHandler(String protocol, String callbackFuncName, boolean isAsync) {
        // NOOP until Tomahawk Desktop recognizes Resolvers as URL-Handlers without this call
    }

    @JavascriptInterface
    public String readBase64(String fileName) {
        // We return an empty string because we don't want the base64 string containing png image
        // data or stuff from config.ui.
        return "";
    }

    @JavascriptInterface
    public void nativeAsyncRequestString(final int reqId, final String url,
            final String stringifiedExtraHeaders, final String stringifiedOptions) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, String> extraHeaders = new HashMap<>();
                    if (!TextUtils.isEmpty(stringifiedExtraHeaders)) {
                        extraHeaders = GsonHelper.get()
                                .fromJson(stringifiedExtraHeaders, Map.class);
                    }
                    ScriptInterfaceRequestOptions options = null;
                    if (!TextUtils.isEmpty(stringifiedOptions)) {
                        options = GsonHelper.get().fromJson(stringifiedOptions,
                                ScriptInterfaceRequestOptions.class);
                    }
                    JsCallback callback = null;
                    if (reqId >= 0) {
                        callback = new JsCallback(reqId);
                    }
                    String method = null;
                    String username = null;
                    String password = null;
                    String data = null;
                    if (options != null) {
                        method = options.method;
                        username = options.username;
                        password = options.password;
                        data = options.data;
                    }
                    TomahawkUtils
                            .httpRequest(method, url, extraHeaders, username, password, data,
                                    callback);
                } catch (NoSuchAlgorithmException | IOException | KeyManagementException e) {
                    Log.e(TAG, "nativeAsyncRequestString: " + e.getClass() + ": "
                            + e.getLocalizedMessage());
                }
            }
        }).start();
    }

    @JavascriptInterface
    public boolean hasFuzzyIndex(String id) {
        return mScriptAccount.hasFuzzyIndex(id);
    }

    @JavascriptInterface
    public void addToFuzzyIndexString(String id, String stringifiedIndexList) {
        if (mScriptAccount.hasFuzzyIndex(id)) {
            ScriptResolverFuzzyIndex[] indexList = GsonHelper.get().fromJson(stringifiedIndexList,
                    ScriptResolverFuzzyIndex[].class);
            mScriptAccount.getFuzzyIndex(id).addScriptResolverFuzzyIndexList(indexList);
        } else {
            Log.e(TAG, "addToFuzzyIndexString: Couldn't add indexList to fuzzy index, no fuzzy "
                    + "index available");
        }
    }

    @JavascriptInterface
    public void createFuzzyIndexString(String id, String stringifiedIndexList) {
        mScriptAccount.createFuzzyIndex(id);
        ScriptResolverFuzzyIndex[] indexList = GsonHelper.get().fromJson(stringifiedIndexList,
                ScriptResolverFuzzyIndex[].class);
        mScriptAccount.getFuzzyIndex(id).addScriptResolverFuzzyIndexList(indexList);
    }

    @JavascriptInterface
    public String searchFuzzyIndexString(String id, String query) {
        if (mScriptAccount.hasFuzzyIndex(id)) {
            double[][] results = mScriptAccount.getFuzzyIndex(id).search(Query.get(query, false));
            return GsonHelper.get().toJson(results);
        } else {
            Log.e(TAG, "searchFuzzyIndexString - ScriptResolver not set in ScriptAccount: "
                    + mScriptAccount.getName());
        }
        return null;
    }

    @JavascriptInterface
    public String resolveFromFuzzyIndexString(String id, String artist, String album,
            String title) {
        if (mScriptAccount.hasFuzzyIndex(id)) {
            double[][] results = mScriptAccount.getFuzzyIndex(id).search(
                    Query.get(title, album, artist, false));
            return GsonHelper.get().toJson(results);
        } else {
            Log.e(TAG, "resolveFromFuzzyIndexString - ScriptResolver not set in ScriptAccount: "
                    + mScriptAccount.getName());
        }
        return null;
    }

    @JavascriptInterface
    public void deleteFuzzyIndex(String id) {
        if (mScriptAccount.hasFuzzyIndex(id)) {
            mScriptAccount.getFuzzyIndex(id).deleteIndex();
        } else {
            Log.e(TAG, "deleteFuzzyIndex - ScriptResolver not set in ScriptAccount: "
                    + mScriptAccount.getName());
        }
    }

    @JavascriptInterface
    public void localStorageSetItem(String key, String value) {
        String dirPath = TomahawkApp.getContext().getFilesDir().getAbsolutePath()
                + File.separator + "TomahawkWebViewStorage";
        new File(dirPath).mkdirs();
        try {
            Files.write(value, new File(dirPath + File.separator + key), Charsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "setItem: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    @JavascriptInterface
    public String localStorageGetItem(String key) {
        String dirPath = TomahawkApp.getContext().getFilesDir().getAbsolutePath()
                + File.separator + "TomahawkWebViewStorage";
        new File(dirPath).mkdirs();
        try {
            return Files.toString(new File(dirPath + File.separator + key), Charsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "getItem: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return null;
    }

    @JavascriptInterface
    public void localStorageRemoveItem(String key) {
        String path = TomahawkApp.getContext().getFilesDir().getAbsolutePath()
                + File.separator + "TomahawkWebViewStorage" + File.separator + key;
        new File(path).delete();
    }

    @JavascriptInterface
    public String[] keys() {
        String path = TomahawkApp.getContext().getFilesDir().getAbsolutePath()
                + File.separator + "TomahawkWebViewStorage";
        String[] keys = new File(path).list();
        if (keys == null) {
            keys = new String[]{};
        }
        return keys;
    }

    @JavascriptInterface
    public String[] values() {
        String[] keys = keys();
        String[] values = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            values[i] = localStorageGetItem(keys[i]);
        }
        return values;
    }

    @JavascriptInterface
    public void onConfigTestResult(int type) {
        if (mScriptAccount.getScriptResolver() != null) {
            mScriptAccount.getScriptResolver().onConfigTestResult(type, "");
        } else {
            Log.e(TAG, "onConfigTestResult - ScriptResolver not set in ScriptAccount: "
                    + mScriptAccount.getName());
        }
    }

    @JavascriptInterface
    public void onConfigTestResult(int type, String message) {
        if (mScriptAccount.getScriptResolver() != null) {
            mScriptAccount.getScriptResolver().onConfigTestResult(type, message);
        } else {
            Log.e(TAG, "onConfigTestResult - ScriptResolver not set in ScriptAccount: "
                    + mScriptAccount.getName());
        }
    }

    @JavascriptInterface
    public void showWebView(String url) {
        TomahawkMainActivity.ShowWebViewEvent event
                = new TomahawkMainActivity.ShowWebViewEvent();
        event.mUrl = url;
        EventBus.getDefault().post(event);
    }

    @JavascriptInterface
    public void reportScriptJobResultsString(String result) {
        JsonElement node = GsonHelper.get().fromJson(result, JsonElement.class);
        if (node.isJsonObject()) {
            mScriptAccount.reportScriptJobResult((JsonObject) node);
        }
    }

    @JavascriptInterface
    public void registerScriptPlugin(String type, String objectId) {
        mScriptAccount.registerScriptPlugin(type, objectId);
    }

    @JavascriptInterface
    public void unregisterScriptPlugin(String type, String objectId) {
        mScriptAccount.unregisterScriptPlugin(type, objectId);
    }
}
