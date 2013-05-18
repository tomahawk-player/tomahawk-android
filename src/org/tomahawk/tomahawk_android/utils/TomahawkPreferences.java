/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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

import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class TomahawkPreferences {

    public static final String PREFERENCE_FILENAME = "TomahawkPreferences";

    public static final String PREFERENCE_USERNAME = "username";

    public static final String PREFERENCE_PASSWORD = "password";

    public static final String PREFERENCE_GO_ONLINE = "go_online";

    private TomahawkPreferences() {
    }

    ;

    public static void setUsernamePassword(String username, String passwd) {

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREFERENCE_USERNAME, username);

        // TODO: encrypt?
        editor.putString(PREFERENCE_PASSWORD, passwd);
        editor.commit();
    }

    public static final String getUsername() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());
        return prefs.getString(PREFERENCE_USERNAME, "username");
    }

    /**
     * Primarily user for testing purposes before we start encrypting passwords.
     */
    public static final String getPassword() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());
        return prefs.getString(PREFERENCE_PASSWORD, "");
    }

    public static boolean goOnline() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());
        return prefs.getBoolean(PREFERENCE_GO_ONLINE, false);
    }
}
