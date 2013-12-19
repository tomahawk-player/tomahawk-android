/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.tomahawk.libtomahawk.resolver.spotify.LibSpotifyWrapper;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyResolver;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.TomahawkService;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class SpotifyAuthenticatorUtils extends AuthenticatorUtils {

    // Used for debug logging
    private final static String TAG = SpotifyAuthenticatorUtils.class.getName();

    // String tags used to store Spotify's preferred bitrate

    public static final String SPOTIFY_PREF_BITRATE
            = "org.tomahawk.tomahawk_android.spotify_pref_bitrate";

    public static final int SPOTIFY_PREF_BITRATE_MODE_LOW = 0;

    public static final int SPOTIFY_PREF_BITRATE_MODE_MEDIUM = 1;

    public static final int SPOTIFY_PREF_BITRATE_MODE_HIGH = 2;

    // This listener handles every event regarding the login/logout methods
    private AuthenticatorListener mAuthenticatorListener = new AuthenticatorListener() {

        @Override
        public void onInit() {
            loginWithToken();
        }

        @Override
        public void onLogin(String username) {
            Log.d(TAG,
                    "TomahawkService: Spotify user '" + username + "' logged in successfully :)");
        }

        @Override
        public void onLoginFailed(String message) {
            Log.d(TAG, "TomahawkService: Spotify login failed :( message: " + message);
            mIsAuthenticating = false;
            mTomahawkService.onLoggedInOut(TomahawkService.AUTHENTICATOR_ID_SPOTIFY, false);
        }

        @Override
        public void onLogout() {
            Log.d(TAG, "TomahawkService: Spotify user logged out");
            SpotifyResolver spotifyResolver = (SpotifyResolver) mTomahawkApp.getPipeLine()
                    .getResolver(TomahawkApp.RESOLVER_ID_SPOTIFY);
            spotifyResolver.setAuthenticated(false);
            mIsAuthenticating = false;
            mTomahawkService.onLoggedInOut(TomahawkService.AUTHENTICATOR_ID_SPOTIFY, false);
        }

        /**
         * Store the given blob-string, so we can relogin in a later session
         * @param authToken the given blob-string
         */
        @Override
        public void onAuthTokenProvided(String username, String authToken) {
            if (username != null && !TextUtils.isEmpty(username) && authToken != null && !TextUtils
                    .isEmpty(authToken)) {
                Log.d(TAG, "TomahawkService: Spotify blob is served and yummy");
                Account account = new Account(username,
                        mTomahawkApp.getString(R.string.accounttype_string));
                AccountManager am = AccountManager.get(mTomahawkApp);
                if (am != null) {
                    am.addAccountExplicitly(account, null, new Bundle());
                    am.setUserData(account, TomahawkService.AUTHENTICATOR_NAME, mName);
                    am.setAuthToken(account, mAuthTokenType, authToken);
                }
            }
            SpotifyResolver spotifyResolver = (SpotifyResolver) mTomahawkApp.getPipeLine()
                    .getResolver(TomahawkApp.RESOLVER_ID_SPOTIFY);
            spotifyResolver.setAuthenticated(true);
            mIsAuthenticating = false;
            mTomahawkService.onLoggedInOut(TomahawkService.AUTHENTICATOR_ID_SPOTIFY, true);
        }
    };

    public SpotifyAuthenticatorUtils(TomahawkApp tomahawkApp, TomahawkService tomahawkService) {
        mTomahawkApp = tomahawkApp;
        mTomahawkService = tomahawkService;
        mName = TomahawkService.AUTHENTICATOR_NAME_SPOTIFY;
        mAuthTokenType = TomahawkService.AUTH_TOKEN_TYPE_SPOTIFY;

        LibSpotifyWrapper.setsAuthenticatorListener(mAuthenticatorListener);
        if (LibSpotifyWrapper.isInitialized()) {
            loginWithToken();
        }
    }

    public void setBitrate(int bitrate) {
        LibSpotifyWrapper.setbitrate(bitrate);
    }

    @Override
    public int getTitleResourceId() {
        return R.string.authenticator_title_spotify;
    }

    @Override
    public int getIconResourceId() {
        return R.drawable.spotify_icon;
    }

    /**
     * Try to login to spotify with given credentials
     *
     * @return null, because we can't generate the auth token synchronously. It'll be provided in
     * the onAuthTokenProvided callback
     */
    @Override
    public void login(String email, String password) {
        mIsAuthenticating = true;
        if (email != null && password != null) {
            LibSpotifyWrapper.loginUser(email, password, "");
        }
    }

    /**
     * Try to login to spotify with stored credentials
     */
    @Override
    public void loginWithToken() {
        mIsAuthenticating = true;
        final AccountManager am = AccountManager.get(mTomahawkApp);
        if (am != null) {
            Account[] accounts = am
                    .getAccountsByType(mTomahawkApp.getString(R.string.accounttype_string));
            if (accounts != null) {
                for (Account account : accounts) {
                    if (mName.equals(am.getUserData(account, TomahawkService.AUTHENTICATOR_NAME))) {
                        String blob = am.peekAuthToken(account, mAuthTokenType);
                        String email = account.name;
                        if (email != null && blob != null) {
                            LibSpotifyWrapper.loginUser(email, "", blob);
                        }
                    }
                }
            }
        }
    }

    /**
     * Logout spotify
     */
    @Override
    public void logout() {
        mIsAuthenticating = true;
        final AccountManager am = AccountManager.get(mTomahawkApp);
        if (am != null) {
            Account[] accounts = am
                    .getAccountsByType(mTomahawkApp.getString(R.string.accounttype_string));
            if (accounts != null) {
                for (Account account : accounts) {
                    if (mName.equals(am.getUserData(account, TomahawkService.AUTHENTICATOR_NAME))) {
                        am.removeAccount(account, null, null);
                    }
                }
            }
        }
        LibSpotifyWrapper.logoutUser();
    }
}
