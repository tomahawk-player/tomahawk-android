/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.libtomahawk.authentication;

import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.Intent;

import java.util.HashMap;

public class AuthenticatorManager {

    public static final String AUTHENTICATOR_LOGGED_IN
            = "org.tomahawk.tomahawk_android.authenticator_logged_in";

    public static final String AUTHENTICATOR_LOGGED_IN_ID
            = "org.tomahawk.tomahawk_android.authenticator_logged_in_id";

    public static final String AUTHENTICATOR_LOGGED_IN_STATE
            = "org.tomahawk.tomahawk_android.authenticator_logged_in_state";

    public static final String AUTHENTICATOR_LOGGED_IN_RESOLVERID
            = "org.tomahawk.tomahawk_android.authenticator_logged_in_resolverid";

    private static AuthenticatorManager instance;

    private boolean mInitialized;

    private HashMap<String, AuthenticatorUtils> mAuthenticatorUtils
            = new HashMap<String, AuthenticatorUtils>();

    private AuthenticatorManager() {
    }

    public static AuthenticatorManager getInstance() {
        if (instance == null) {
            synchronized (AuthenticatorManager.class) {
                if (instance == null) {
                    instance = new AuthenticatorManager();
                }
            }
        }
        return instance;
    }

    public void ensureInit() {
        if (!mInitialized) {
            mInitialized = true;
            mAuthenticatorUtils.put(TomahawkApp.PLUGINNAME_SPOTIFY,
                    new SpotifyAuthenticatorUtils(TomahawkApp.getContext()));
            mAuthenticatorUtils.put(TomahawkApp.PLUGINNAME_HATCHET,
                    new HatchetAuthenticatorUtils(TomahawkApp.getContext()));
            mAuthenticatorUtils.put(TomahawkApp.PLUGINNAME_RDIO,
                    new RdioAuthenticatorUtils(TomahawkApp.getContext()));
            mAuthenticatorUtils.put(TomahawkApp.PLUGINNAME_DEEZER,
                    new DeezerAuthenticatorUtils(TomahawkApp.getContext()));
        }
    }

    /**
     * Authenticators should callback here, if they logged in or out
     */
    public void onLoggedInOut(String id, boolean loggedIn) {
        Intent i = new Intent(AUTHENTICATOR_LOGGED_IN)
                .putExtra(AUTHENTICATOR_LOGGED_IN_STATE, loggedIn)
                .putExtra(AUTHENTICATOR_LOGGED_IN_ID, id);
        if (id != null) {
            i.putExtra(AUTHENTICATOR_LOGGED_IN_RESOLVERID, id);
        }
        TomahawkApp.getContext().sendBroadcast(i);
    }

    public AuthenticatorUtils getAuthenticatorUtils(String authenticatorId) {
        return mAuthenticatorUtils.get(authenticatorId);
    }

    public boolean isAuthenticating() {
        for (AuthenticatorUtils authUtils : mAuthenticatorUtils.values()) {
            if (authUtils.isAuthenticating()) {
                return true;
            }
        }
        return false;
    }
}
