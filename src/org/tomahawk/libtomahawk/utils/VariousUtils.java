package org.tomahawk.libtomahawk.utils;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

public class VariousUtils {

    public static final String TAG = VariousUtils.class.getSimpleName();

    public static boolean containsIgnoreCase(String str1, String str2) {
        return str1.toLowerCase().contains(str2.toLowerCase());
    }

    /**
     * By default File#delete fails for non-empty directories, it works like "rm". We need something
     * a little more brutal - this does the equivalent of "rm -r"
     *
     * @param path Root File Path
     * @return true if the file and all sub files/directories have been removed
     */
    public static boolean deleteRecursive(File path) throws FileNotFoundException {
        if (!path.exists()) {
            throw new FileNotFoundException(path.getAbsolutePath());
        }
        boolean ret = true;
        if (path.isDirectory()) {
            for (File f : path.listFiles()) {
                ret = ret && deleteRecursive(f);
            }
        }
        return ret && path.delete();
    }

    public static float[] getFloatArray(SharedPreferences pref, String key) {
        float[] array = null;
        String s = pref.getString(key, null);
        if (s != null) {
            try {
                JSONArray json = new JSONArray(s);
                array = new float[json.length()];
                for (int i = 0; i < array.length; i++) {
                    array[i] = (float) json.getDouble(i);
                }
            } catch (JSONException e) {
                Log.e(TAG, "getFloatArray: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
        return array;
    }

    public static void putFloatArray(SharedPreferences.Editor editor, String key, float[] array) {
        try {
            JSONArray json = new JSONArray();
            for (float f : array) {
                json.put(f);
            }
            editor.putString(key, json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "putFloatArray: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }
}
