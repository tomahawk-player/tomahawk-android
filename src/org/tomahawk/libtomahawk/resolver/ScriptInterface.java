package org.tomahawk.libtomahawk.resolver;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;
import org.tomahawk.libtomahawk.infosystem.InfoSystemUtils;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains all methods that are being exposed to the javascript script inside a {@link
 * ScriptResolver} object.
 */
public class ScriptInterface {

    private final static String TAG = ScriptInterface.class.getSimpleName();

    private ScriptResolver mScriptResolver;

    private ObjectMapper mObjectMapper;

    /**
     * Class to make a callback on the javascript side of this ScriptInterface. The callback is
     * stored in a map on the js side and can be identified by its callback-id, which is given to
     * this JsCallback in its constructor.
     */
    public class JsCallback {

        private int mCallbackId;

        public JsCallback(int callbackId) {
            mCallbackId = callbackId;
        }

        public void call(String responseText, Map<String, List<String>> responseHeaders, int status,
                String statusText) {
            mScriptResolver
                    .callback(mCallbackId, responseText, responseHeaders, status, statusText);
        }
    }

    ScriptInterface(ScriptResolver scriptResolver) {
        mScriptResolver = scriptResolver;
    }

    /**
     * This method should be called whenever a javascript function should call back to Java after it
     * is finished. Returned {@link Result}s are also handed over to the {@link ScriptResolver}
     * through this method.
     *
     * @param id                 used to identify who is calling back
     * @param in                 the raw result {@link String}
     * @param shouldReturnResult whether or not the javascript function will return a result
     */
    @JavascriptInterface
    public void callbackToJava(int id, String in, boolean shouldReturnResult) {
        if (shouldReturnResult) {
            mScriptResolver.handleCallbackToJava(id, in);
        } else {
            mScriptResolver.handleCallbackToJava(id);
        }
    }

    /**
     * This method is needed because the javascript script is expecting an exposed method which will
     * return the scriptPath and config. This method is being called in tomahawk_android_pre.js
     *
     * @return a {@link JSONObject} containing the scriptPath and config.
     */
    @JavascriptInterface
    public String resolverDataString() {
        if (mObjectMapper == null) {
            mObjectMapper = InfoSystemUtils.constructObjectMapper();
        }
        Map<String, Object> config = mScriptResolver.getConfig();
        ScriptResolverData data = new ScriptResolverData();
        data.scriptPath = mScriptResolver.getScriptFilePath();
        data.config = config;
        String jsonString = "";
        try {
            jsonString = mObjectMapper.writeValueAsString(data);
        } catch (JsonMappingException e) {
            Log.e(TAG, "resolverDataString: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (JsonGenerationException e) {
            Log.e(TAG, "resolverDataString: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG, "resolverDataString: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return jsonString;
    }

    /**
     * A straightforward log method to write something into the Debug log.
     */
    @JavascriptInterface
    public void log(String message) {
        Log.d(TAG, "log: " + mScriptResolver.getId() + ": " + message);
    }

    /**
     * This method is needed because the javascript script is expecting an exposed method which it
     * can call to return the resolved {@link Result}s. This method is being called in
     * tomahawk_android_pre.js
     *
     * @param in the JSONObject {@link String} containing the resolved {@link Result}s
     */
    @JavascriptInterface
    public void addTrackResultsString(String in) {
        mScriptResolver.handleCallbackToJava(R.id.scriptresolver_add_track_results_string, in);
    }

    /**
     * This method is needed because the javascript script is expecting an exposed method which it
     * can call to report its capabilities. This method is being called in tomahawk_android_pre.js
     *
     * @param in the int pointing to the script's capabilities
     */
    @JavascriptInterface
    public void reportCapabilities(int in) {
        mScriptResolver.reportCapabilities(in);
    }

    @JavascriptInterface
    public void reportStreamUrlString(String qid, String url, String stringifiedHeaders) {
        if (stringifiedHeaders != null) {
            mScriptResolver.handleCallbackToJava(R.id.scriptresolver_report_stream_url, qid, url,
                    stringifiedHeaders);
        } else {
            mScriptResolver.handleCallbackToJava(R.id.scriptresolver_report_stream_url, qid, url);
        }
    }

    @JavascriptInterface
    public void addCustomUrlHandler(String protocol, String callbackFuncName, boolean isAsync) {
        PipeLine.getInstance().addCustomUrlHandler(protocol, mScriptResolver, callbackFuncName);
    }

    @JavascriptInterface
    public String readBase64(String fileName) {
        // We return an empty string because we don't want the base64 string containing png image
        // data or stuff from config.ui.
        return "";
    }

    @JavascriptInterface
    public void javaAsyncRequest(final String url, final int callbackId,
            final String stringifiedExtraHeaders, final String stringifiedOptions,
            final int errorHandlerId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, String> extraHeaders = new HashMap<String, String>();
                    if (!TextUtils.isEmpty(stringifiedExtraHeaders)) {
                        extraHeaders = mObjectMapper.readValue(stringifiedExtraHeaders, Map.class);
                    }
                    ScriptInterfaceRequestOptions options = null;
                    if (!TextUtils.isEmpty(stringifiedOptions)) {
                        options = mObjectMapper.readValue(stringifiedOptions,
                                ScriptInterfaceRequestOptions.class);
                    }
                    JsCallback callback = null;
                    if (callbackId >= 0) {
                        callback = new JsCallback(callbackId);
                    }
                    String method = null;
                    String username = null;
                    String password = null;
                    String data = null;
                    JsCallback errorHandler = null;
                    if (options != null) {
                        method = options.method;
                        username = options.username;
                        password = options.password;
                        data = options.data;
                        if (errorHandlerId >= 0) {
                            errorHandler = new JsCallback(errorHandlerId);
                        }
                    }
                    TomahawkUtils.httpRequest(method, url, extraHeaders, username, password, data,
                            callback, errorHandler);
                } catch (NoSuchAlgorithmException e) {
                    Log.e(TAG,
                            "javaAsyncRequest: " + e.getClass() + ": " + e.getLocalizedMessage());
                } catch (KeyManagementException e) {
                    Log.e(TAG,
                            "javaAsyncRequest: " + e.getClass() + ": " + e.getLocalizedMessage());
                } catch (IOException e) {
                    Log.e(TAG,
                            "javaAsyncRequest: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
            }
        }).start();
    }

    @JavascriptInterface
    public void createFuzzyIndexString(String stringifiedIndexList) {
        try {
            String filePath = TomahawkApp.getContext().getFilesDir().getAbsolutePath()
                    + File.separator + mScriptResolver.getId() + ".lucene";
            mScriptResolver.setFuzzyIndex(new FuzzyIndex(filePath, true));
            ScriptResolverFuzzyIndex[] indexList = mObjectMapper
                    .readValue(stringifiedIndexList, ScriptResolverFuzzyIndex[].class);
            mScriptResolver.getFuzzyIndex().addScriptResolverFuzzyIndexList(indexList);
        } catch (IOException e) {
            Log.e(TAG, "createFuzzyIndexString: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    @JavascriptInterface
    public String searchFuzzyIndexString(String query) {
        double[][] results = mScriptResolver.getFuzzyIndex().search(Query.get(query, false));
        try {
            return mObjectMapper.writeValueAsString(results);
        } catch (IOException e) {
            Log.e(TAG, "searchFuzzyIndexString: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return null;
    }

    @JavascriptInterface
    public String resolveFromFuzzyIndexString(String artist, String album, String title) {
        double[][] results = mScriptResolver.getFuzzyIndex().search(
                Query.get(title, album, artist, false));
        try {
            return mObjectMapper.writeValueAsString(results);
        } catch (IOException e) {
            Log.e(TAG, "resolveFromFuzzyIndexString: " + e.getClass() + ": " + e
                    .getLocalizedMessage());
        }
        return null;
    }

    @JavascriptInterface
    public void deleteFuzzyIndex() {
        if (mScriptResolver.getFuzzyIndex() != null) {
            mScriptResolver.getFuzzyIndex().deleteIndex();
        }
    }
}
