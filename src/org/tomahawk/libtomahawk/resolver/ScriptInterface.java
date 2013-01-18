package org.tomahawk.libtomahawk.resolver;

import org.json.JSONException;
import org.json.JSONObject;
import org.tomahawk.tomahawk_android.R;

import android.util.Log;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com>
 * Date: 17.01.13
 * This class contains all methods that are being exposed to the javascript script inside a ScriptResolver object.
 */
public class ScriptInterface {
    private final static String TAG = ScriptInterface.class.getName();

    ScriptResolver mScriptResolver;

    ScriptInterface(ScriptResolver scriptResolver) {
        mScriptResolver = scriptResolver;
    }

    /**
     * This method should be called whenever a javascript function should call back to Java after it is finished.
     * Returned results are also handed over to the ScriptResolver through this method.
     * @param id used to identify who is calling back
     * @param in the raw result String
     * @param shouldReturnResult wether or not the javascript function will return a result
     */
    public void callbackToJava(int id, String in, boolean shouldReturnResult) {
        Log.d(TAG, "callbackToJava: id='" + id + "', in='" + in + "'");
        if (shouldReturnResult) {
            JSONObject out = new JSONObject();
            try {
                out = new JSONObject(in);
            } catch (JSONException e) {
                Log.e(TAG, "callbackToJava: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
            mScriptResolver.handleCallbackToJava(id, out);
        } else
            mScriptResolver.handleCallbackToJava(id, null);
    }

    /**
     * This method is needed for TomahawkAndroid because the javascript script is expecting an exposed method
     * which will return the scriptPath and config. This method is being called in tomahawk_android.js
     * @return a JSONObject containing the scriptPath and config.
     */
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
     * @param message
     */
    public void log(String message) {
        Log.d(TAG, "log: " + mScriptResolver.getScriptFilePath() + ":" + message);
    }

    /**
     * This method is needed for TomahawkAndroid because the javascript script is expecting an exposed method
     * which it can call to return the resolved results. This method is being called in tomahawk_android.js
     * @param in the JSONObject string containing the resolved results
     */
    public void addTrackResultsString(String in) {
        Log.d(TAG, "addTrackResultsString: in='" + in + "'");
        try {
            JSONObject out = new JSONObject(in);
            mScriptResolver.handleCallbackToJava(R.id.scriptresolver_add_track_results_string, out);
        } catch (JSONException e) {
            Log.e(TAG, "addTrackResultsString: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }
}
