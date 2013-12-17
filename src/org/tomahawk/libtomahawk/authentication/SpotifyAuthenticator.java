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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class SpotifyAuthenticator implements Authenticator {

    // Used for debug logging
    private final static String TAG = SpotifyAuthenticator.class.getName();

    // String tags used to store spotify credentials and preferred bitrate
    public static final String SPOTIFY_CREDS_BLOB
            = "org.tomahawk.tomahawk_android.spotify_creds_blob";

    public static final String SPOTIFY_CREDS_EMAIL
            = "org.tomahawk.tomahawk_android.spotify_creds_email";

    public static final String SPOTIFY_PREF_BITRATE
            = "org.tomahawk.tomahawk_android.spotify_pref_bitrate";

    public static final int SPOTIFY_PREF_BITRATE_MODE_LOW = 0;

    public static final int SPOTIFY_PREF_BITRATE_MODE_MEDIUM = 1;

    public static final int SPOTIFY_PREF_BITRATE_MODE_HIGH = 2;

    private TomahawkApp mTomahawkApp;

    private TomahawkService mTomahawkService;

    private SharedPreferences mSharedPreferences;

    private String mSpotifyUserId;

    private boolean mIsAuthenticating;

    // This listener handles every event regarding the login/logout methods
    private TomahawkService.AuthenticatorListener mAuthenticatorListener
            = new TomahawkService.AuthenticatorListener() {

        @Override
        public void onInit() {
            loginWithToken();
        }

        @Override
        public void onLogin(String username) {
            Log.d(TAG,
                    "TomahawkService: spotify user '" + username + "' logged in successfully :)");
            mSpotifyUserId = username;
            logInOut(true);
        }

        @Override
        public void onLoginFailed(String message) {
            Log.d(TAG, "TomahawkService: spotify loginSpotify failed :( message: " + message);
            mSpotifyUserId = null;
            logInOut(false);
        }

        @Override
        public void onLogout() {
            Log.d(TAG, "TomahawkService: spotify user logged out");
            mSpotifyUserId = null;
            logInOut(false);
        }

        /**
         * Store the given blob-string, so we can relogin in a later session
         * @param blob the given blob-string
         */
        @Override
        public void onCredBlobUpdated(String blob) {
            SharedPreferences.Editor editor = PreferenceManager
                    .getDefaultSharedPreferences(TomahawkApp.getContext()).edit();
            editor.putString(SPOTIFY_CREDS_BLOB, blob);
            if (mSpotifyUserId != null) {
                editor.putString(SPOTIFY_CREDS_EMAIL, mSpotifyUserId);
            }
            editor.commit();
        }

        private void logInOut(boolean loggedIn) {
            mTomahawkService.onLoggedInOut(TomahawkService.AUTHENTICATOR_ID_SPOTIFY, loggedIn);
            SpotifyResolver spotifyResolver = (SpotifyResolver) mTomahawkApp.getPipeLine()
                    .getResolver(TomahawkApp.RESOLVER_ID_SPOTIFY);
            spotifyResolver.setAuthenticated(loggedIn);
            mIsAuthenticating = false;
        }
    };

    public SpotifyAuthenticator(TomahawkApp tomahawkApp, TomahawkService tomahawkService) {
        mTomahawkApp = tomahawkApp;
        mTomahawkService = tomahawkService;

        mSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());

        // Load the LibSpotifyWrapper libaries
        System.loadLibrary("spotify");
        System.loadLibrary("spotifywrapper");

        // Initialize LibSpotifyWrapper
        LibSpotifyWrapper.init(LibSpotifyWrapper.class.getClassLoader(),
                mTomahawkApp.getFilesDir() + "/Spotify", mAuthenticatorListener);
    }

    public void setBitrate(int bitrate) {
        LibSpotifyWrapper.setbitrate(bitrate);
    }

    @Override
    public int getTitleResourceId() {
        return R.string.authenticator_title_spotify;
    }

    /**
     * Try to login to spotify with given credentials
     *
     * @return null, because we can't generate the auth token synchronously. It'll be provided in
     * the onCredBlobUpdated callback
     */
    @Override
    public String login(String email, String password) {
        mIsAuthenticating = true;
        if (email != null && password != null) {
            LibSpotifyWrapper.loginUser(email, password, "");
        }
        return null;
    }

    /**
     * Try to login to spotify with stored credentials
     */
    @Override
    public void loginWithToken() {
        mIsAuthenticating = true;
        String blob = mSharedPreferences.getString(SpotifyAuthenticator.SPOTIFY_CREDS_BLOB, "");
        String email = mSharedPreferences.getString(SpotifyAuthenticator.SPOTIFY_CREDS_EMAIL, "");
        if (email != null && blob != null) {
            LibSpotifyWrapper.loginUser(email, "", blob);
        }
    }

    /**
     * Logout spotify
     */
    @Override
    public void logout() {
        mIsAuthenticating = true;
        LibSpotifyWrapper.logoutUser();
    }

    @Override
    public String getUserId() {
        return mSpotifyUserId;
    }

    @Override
    public boolean isLoggedIn() {
        return mSpotifyUserId != null;
    }

    @Override
    public boolean isAuthenticating() {
        return mIsAuthenticating;
    }
}
