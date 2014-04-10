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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;

public class AuthenticatorManager {

    public static final int AUTHENTICATOR_ID_SPOTIFY = 0;

    public static final int AUTHENTICATOR_ID_HATCHET = 1;

    private static AuthenticatorManager instance;

    private SparseArray<AuthenticatorUtils> mAuthenticatorUtils
            = new SparseArray<AuthenticatorUtils>();

    private OnAuthenticatedListener mOnAuthenticatedListener;

    public interface OnAuthenticatedListener {

        void onLoggedInOut(int authenticatorId, boolean loggedIn);

    }

    private AuthenticatorManager() {
        mAuthenticatorUtils.put(AUTHENTICATOR_ID_SPOTIFY,
                new SpotifyAuthenticatorUtils(TomahawkApp.getContext()));
        mAuthenticatorUtils.put(AUTHENTICATOR_ID_HATCHET,
                new HatchetAuthenticatorUtils(TomahawkApp.getContext()));
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

    /**
     * Authenticators should callback here, if they logged in or out
     */
    public void onLoggedInOut(int authenticatorId, boolean loggedIn) {
        if (mOnAuthenticatedListener != null) {
            mOnAuthenticatedListener.onLoggedInOut(authenticatorId, loggedIn);
        }
    }

    public AuthenticatorUtils getAuthenticatorUtils(int authenticatorId) {
        return mAuthenticatorUtils.get(authenticatorId);
    }

    public boolean isAuthenticating() {
        for (int i = 0; i < mAuthenticatorUtils.size(); i++) {
            if (mAuthenticatorUtils.valueAt(i).isAuthenticating()) {
                return true;
            }
        }
        return false;
    }

    public void setOnAuthenticatedListener(OnAuthenticatedListener onAuthenticatedListener) {
        mOnAuthenticatedListener = onAuthenticatedListener;
    }

    public void updateBitrate() {
        ConnectivityManager conMan = (ConnectivityManager) TomahawkApp.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Log.d("WifiReceiver", "Have Wifi Connection");
            ((SpotifyAuthenticatorUtils) mAuthenticatorUtils.get(AUTHENTICATOR_ID_SPOTIFY))
                    .setBitrate(SpotifyAuthenticatorUtils.SPOTIFY_PREF_BITRATE_MODE_HIGH);
        } else {
            Log.d("WifiReceiver", "Don't have Wifi Connection");
            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(TomahawkApp.getContext());
            int prefbitrate = preferences.getInt(
                    SpotifyAuthenticatorUtils.SPOTIFY_PREF_BITRATE,
                    SpotifyAuthenticatorUtils.SPOTIFY_PREF_BITRATE_MODE_MEDIUM);
            ((SpotifyAuthenticatorUtils) mAuthenticatorUtils.get(AUTHENTICATOR_ID_SPOTIFY))
                    .setBitrate(prefbitrate);
        }
    }
}
