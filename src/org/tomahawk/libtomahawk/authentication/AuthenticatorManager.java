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

import java.util.ArrayList;
import java.util.HashMap;

public class AuthenticatorManager {

    public static final String AUTHENTICATOR_ID_SPOTIFY = "spotify_auth";

    public static final String AUTHENTICATOR_ID_HATCHET = "hatchet_auth";

    public static final String AUTHENTICATOR_ID_RDIO = "rdio_auth";

    private static AuthenticatorManager instance;

    private boolean mInitialized;

    private HashMap<String, AuthenticatorUtils> mAuthenticatorUtils
            = new HashMap<String, AuthenticatorUtils>();

    private ArrayList<OnAuthenticatedListener> mOnAuthenticatedListeners
            = new ArrayList<OnAuthenticatedListener>();

    public interface OnAuthenticatedListener {

        void onLoggedInOut(String authenticatorId, boolean loggedIn);

    }

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
            mAuthenticatorUtils.put(AUTHENTICATOR_ID_SPOTIFY,
                    new SpotifyAuthenticatorUtils(TomahawkApp.getContext()));
            mAuthenticatorUtils.put(AUTHENTICATOR_ID_HATCHET,
                    new HatchetAuthenticatorUtils(TomahawkApp.getContext()));
            mAuthenticatorUtils.put(AUTHENTICATOR_ID_RDIO,
                    new RdioAuthenticatorUtils(TomahawkApp.getContext()));
        }
    }

    /**
     * Authenticators should callback here, if they logged in or out
     */
    public void onLoggedInOut(String authenticatorId, boolean loggedIn) {
        for (OnAuthenticatedListener listener : mOnAuthenticatedListeners) {
            if (listener != null) {
                listener.onLoggedInOut(authenticatorId, loggedIn);
            }
        }
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

    public void addOnAuthenticatedListener(OnAuthenticatedListener onAuthenticatedListener) {
        mOnAuthenticatedListeners.add(onAuthenticatedListener);
    }
}
