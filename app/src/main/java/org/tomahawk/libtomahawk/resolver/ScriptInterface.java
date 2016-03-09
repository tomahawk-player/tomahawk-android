package org.tomahawk.libtomahawk.resolver;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverData;
import org.tomahawk.libtomahawk.utils.GsonHelper;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.util.Log;
import android.webkit.JavascriptInterface;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * This class contains all methods that are being exposed to the javascript script inside a {@link
 * ScriptResolver} object.
 */
public class ScriptInterface {

    private final static String TAG = ScriptInterface.class.getSimpleName();

    private final ScriptAccount mScriptAccount;

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
        // NOOP until Tomahawk Desktop drops compat for pre 0.9 resolvers
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
    public void localStorageSetItem(String key, String value) {
        String dirPath = TomahawkApp.getContext().getFilesDir().getAbsolutePath()
                + File.separator + "TomahawkWebViewStorage";
        boolean success = new File(dirPath).mkdirs();
        if (!success) {
            Log.e(TAG, "localStorageSetItem - Wasn't able to create directory: " + dirPath);
        }
        try {
            FileUtils.writeStringToFile(new File(dirPath + File.separator + key), value,
                    Charsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "setItem: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    @JavascriptInterface
    public String localStorageGetItem(String key) {
        String dirPath = TomahawkApp.getContext().getFilesDir().getAbsolutePath()
                + File.separator + "TomahawkWebViewStorage";
        boolean success = new File(dirPath).mkdirs();
        if (!success) {
            Log.e(TAG, "localStorageGetItem - Wasn't able to create directory: " + dirPath);
        }
        try {
            return FileUtils
                    .readFileToString(new File(dirPath + File.separator + key), Charsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "getItem: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return null;
    }

    @JavascriptInterface
    public void localStorageRemoveItem(String key) {
        String path = TomahawkApp.getContext().getFilesDir().getAbsolutePath()
                + File.separator + "TomahawkWebViewStorage" + File.separator + key;
        boolean success = new File(path).delete();
        if (!success) {
            Log.e(TAG, "localStorageRemoveItem - Wasn't able to delete file: " + path);
        }
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
    public void reportScriptJobResults(String resultsString) {
        JsonElement node = GsonHelper.get().fromJson(resultsString, JsonElement.class);
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

    @JavascriptInterface
    public void invokeNativeScriptJob(int requestId, String methodName, String paramsString) {
        mScriptAccount.invokeNativeScriptJob(requestId, methodName, paramsString);
    }
}
