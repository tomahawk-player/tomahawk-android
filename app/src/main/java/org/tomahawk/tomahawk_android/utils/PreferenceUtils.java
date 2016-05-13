/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.dialogs.AskAccessConfigDialog;

import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import java.util.Set;

public class PreferenceUtils {

    public static final String TAG = PreferenceUtils.class.getSimpleName();

    /**
     * USER PREFERENCES
     */
    public static final String SCROBBLE_EVERYTHING
            = "org.tomahawk.tomahawk_android.scrobbleeverything";

    public static final String PLUG_IN_TO_PLAY
            = "org.tomahawk.tomahawk_android.plugintoplay";

    public static final String ASKED_FOR_ACCESS
            = "org.tomahawk.tomahawk_android.asked_for_access";

    public static final String PREF_BITRATE
            = "org.tomahawk.tomahawk_android.prefbitrate";

    /**
     * See {@link org.tomahawk.tomahawk_android.R.array#fake_preferences_items_bitrate}
     */
    public static final int PREF_BITRATE_LOW = 0;

    public static final int PREF_BITRATE_MEDIUM = 1;

    public static final int PREF_BITRATE_HIGH = 2;

    /**
     * COACHMARK PREFERENCES
     */
    public static final String COACHMARK_SEEK_DISABLED = "coachmark_seek_disabled";

    public static final String COACHMARK_SEEK_TIMESTAMP = "coachmark_seek_timestamp";

    public static final String COACHMARK_PLAYBACKFRAGMENT_NAVIGATION_DISABLED
            = "coachmark_playbackfragment_navigation_disabled";

    public static final String COACHMARK_WELCOMEFRAGMENT_DISABLED
            = "coachmark_welcomefragment_disabled";

    public static final String COACHMARK_SWIPELAYOUT_ENQUEUE_DISABLED
            = "coachmark_swipelayout_enqueue_disabled";


    /**
     * CHARTS PREFERENCES
     */
    public static final String CHARTS_COUNTRY_CODE
            = "org.tomahawk.tomahawk_android.charts_country_code";

    public static final String LAST_DISPLAYED_PROVIDER_ID =
            "org.tomahawk.tomahawk_android.last_displayed_provider_id";


    /**
     * EQUALIZER PREFERENCES
     */
    public final static String EQUALIZER_VALUES = "equalizer_values";

    public final static String EQUALIZER_ENABLED = "equalizer_enabled";

    public final static String EQUALIZER_PRESET = "equalizer_preset";

    /**
     * USERPAGE PREFERENCES
     */
    public static final String USERPAGER_SELECTOR_POSITION
            = "org.tomahawk.tomahawk_android.userpager_selector_position";


    private static final SharedPreferences mPreferences =
            PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext());

    public static SharedPreferences.Editor edit() {
        return mPreferences.edit();
    }

    private static boolean getBooleanDefault(String prefKey) {
        if (prefKey.equals(SCROBBLE_EVERYTHING)) {
            return true;
        }
        return false;
    }

    private static int getIntDefault(String prefKey) {
        if (prefKey.equals(PREF_BITRATE)) {
            return PREF_BITRATE_MEDIUM;
        } else if (prefKey.equals(EQUALIZER_PRESET)) {
            return 0;
        } else if (prefKey.equals(USERPAGER_SELECTOR_POSITION)) {
            return 0;
        }
        return -1;
    }

    private static String getStringDefault(String prefKey) {
        return null;
    }

    private static int getLongDefault(String prefKey) {
        return -1;
    }

    private static Set<String> getStringSetDefault(String prefKey) {
        return null;
    }

    public static boolean getBoolean(String key) {
        return mPreferences.getBoolean(key, getBooleanDefault(key));
    }

    public static String getString(String key) {
        return mPreferences.getString(key, getStringDefault(key));
    }

    public static int getInt(String key) {
        return getInt(key, getIntDefault(key));
    }

    public static int getInt(String key, int defaultValue) {
        return mPreferences.getInt(key, defaultValue);
    }

    public static long getLong(String key) {
        return getLong(key, getLongDefault(key));
    }

    public static long getLong(String key, long defaultValue) {
        return mPreferences.getLong(key, defaultValue);
    }

    public static Set<String> getStringSet(String key) {
        return mPreferences.getStringSet(key, getStringSetDefault(key));
    }

    public static float[] getFloatArray(String key) {
        float[] array = null;
        String s = mPreferences.getString(key, null);
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

    /**
     * Starts the AskAccessActivity in order to ask the user for permission to the notification
     * listener, if the user hasn't been asked before and is logged into hatchet
     */
    public static void attemptAskAccess(TomahawkMainActivity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (!getBoolean(ASKED_FOR_ACCESS)) {
                askAccess(activity);
            }
        }
    }

    /**
     * Starts the AskAccessActivity in order to ask the user for permission to the notification
     * listener, if the user is logged into Hatchet and we don't already have access
     */
    public static void askAccess(FragmentActivity activity) {
        if (AuthenticatorManager.get()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET).isLoggedIn()) {
            mPreferences.edit().putBoolean(ASKED_FOR_ACCESS, true).apply();
            new AskAccessConfigDialog().show(activity.getSupportFragmentManager(), null);
        }
    }

}
