package org.tomahawk.libtomahawk.resolver;

import org.json.JSONException;
import org.json.JSONObject;
import org.tomahawk.tomahawk_android.R;

import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * This class contains all methods that are being exposed to the javascript script inside a {@link
 * ScriptResolver} object.
 */
public class ScriptInterface {

    private final static String TAG = ScriptInterface.class.getName();

    private ScriptResolver mScriptResolver;

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
            mScriptResolver.handleCallbackToJava(id, null);
        }
    }

    /**
     * This method is needed because the javascript script is expecting an exposed method which will
     * return the scriptPath and config. This method is being called in tomahawk_android.js
     *
     * @return a {@link JSONObject} containing the scriptPath and config.
     */
    @JavascriptInterface
    public String resolverDataString() {
        JSONObject result = new JSONObject();
        try {
            result.put("scriptPath", mScriptResolver.getScriptFilePath());
            result.put("config", mScriptResolver.getScriptFilePath());
        } catch (JSONException e) {
            Log.e(TAG, "resolverDataString: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return result.toString();
    }

    /**
     * A straightforward log method to write something into the Debug log.
     */
    @JavascriptInterface
    public void log(String message) {
        //Log.d(TAG, "log: " + mScriptResolver.getScriptFilePath() + ":" + message);
    }

    /**
     * This method is needed because the javascript script is expecting an exposed method which it
     * can call to return the resolved {@link Result}s. This method is being called in
     * tomahawk_android.js
     *
     * @param in the JSONObject {@link String} containing the resolved {@link Result}s
     */
    @JavascriptInterface
    public void addTrackResultsString(String in) {
        mScriptResolver.handleCallbackToJava(R.id.scriptresolver_add_track_results_string, in);
    }

    /**
     * This method is needed because the javascript script is expecting an exposed method which it
     * can call to report its capabilities. This method is being called in tomahawk_android.js
     *
     * @param in the int pointing to the script's capabilities
     */
    @JavascriptInterface
    public void reportCapabilities(int in) {
    }

}
