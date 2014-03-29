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

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.spotify.LibSpotifyWrapper;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyResolver;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class SpotifyAuthenticatorUtils extends AuthenticatorUtils {

    // Used for debug logging
    private final static String TAG = SpotifyAuthenticatorUtils.class.getName();

    // String tags used to store Spotify's preferred bitrate

    public static final String SPOTIFY_PREF_BITRATE
            = "org.tomahawk.tomahawk_android.spotify_pref_bitrate";

    public static final int SPOTIFY_PREF_BITRATE_MODE_LOW = 0;

    public static final int SPOTIFY_PREF_BITRATE_MODE_MEDIUM = 1;

    public static final int SPOTIFY_PREF_BITRATE_MODE_HIGH = 2;

    private static boolean mIsSpotifyLoggedIn = false;

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
        public void onLoginFailed(final String error, final String errorDescription) {
            Log.d(TAG,
                    "TomahawkService: Spotify login failed :(, Error: " + error + ", Description: "
                            + errorDescription
            );
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext,
                            TextUtils.isEmpty(errorDescription) ? error : errorDescription,
                            Toast.LENGTH_LONG).show();
                }
            });
            mIsAuthenticating = false;
        }

        @Override
        public void onLogout() {
            Log.d(TAG, "TomahawkService: Spotify user logged out");
            setAuthenticated(false);
        }

        private void setAuthenticated(boolean isAuthenticated) {
            mIsSpotifyLoggedIn = isAuthenticated;
            SpotifyResolver spotifyResolver = (SpotifyResolver) PipeLine.getInstance()
                    .getResolver(PipeLine.RESOLVER_ID_SPOTIFY);
            spotifyResolver.setAuthenticated(isAuthenticated);
            AuthenticatorManager.getInstance()
                    .onLoggedInOut(AuthenticatorUtils.AUTHENTICATOR_ID_SPOTIFY, isAuthenticated);
            mIsAuthenticating = false;
        }

        /**
         * Store the given blob-string, so we can relogin in a later session
         */
        @Override
        public void onAuthTokenProvided(String username, String refreshToken,
                int refreshTokenExpiresIn, String accessToken, int accessTokenExpiresIn) {
            if (username != null && !TextUtils.isEmpty(username) && refreshToken != null
                    && !TextUtils.isEmpty(refreshToken)) {
                Log.d(TAG, "TomahawkService: Spotify blob is served and yummy");
                Account account = new Account(username,
                        mContext.getString(R.string.accounttype_string));
                AccountManager am = AccountManager.get(mContext);
                if (am != null) {
                    am.addAccountExplicitly(account, null, new Bundle());
                    am.setUserData(account, AuthenticatorUtils.AUTHENTICATOR_NAME,
                            getAuthenticatorUtilsName());
                    am.setAuthToken(account, getAuthenticatorUtilsTokenType(), refreshToken);
                }
            }
            setAuthenticated(true);
        }
    };

    public SpotifyAuthenticatorUtils(Context context) {
        mContext = context;
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

    @Override
    public String getAuthenticatorUtilsName() {
        return AuthenticatorUtils.AUTHENTICATOR_NAME_SPOTIFY;
    }

    @Override
    public String getAuthenticatorUtilsTokenType() {
        return AuthenticatorUtils.AUTH_TOKEN_TYPE_SPOTIFY;
    }

    @Override
    public int getUserIdEditTextHintResId() {
        return R.string.logindialog_emailorusername_label_string;
    }

    /**
     * Try to login to spotify with given credentials
     *
     * @return null, because we can't generate the auth token synchronously. It'll be provided in
     * the onAuthTokenProvided callback
     */
    @Override
    public void login(String email, String password) {
        if (email != null && password != null) {
            mIsAuthenticating = true;
            LibSpotifyWrapper.loginUser(email, password, "");
        }
    }

    /**
     * Try to login to spotify with stored credentials
     */
    public void loginWithToken() {
        Account account = TomahawkUtils.getAccountByName(mContext, getAuthenticatorUtilsName());
        if (account != null) {
            String blob = TomahawkUtils.peekAuthTokenForAccount(mContext,
                    getAuthenticatorUtilsName(),
                    getAuthenticatorUtilsTokenType());
            String email = account.name;
            if (email != null && blob != null) {
                mIsAuthenticating = true;
                LibSpotifyWrapper.loginUser(email, "", blob);
            }
        }
    }

    /**
     * Logout spotify
     */
    @Override
    public void logout() {
        mIsAuthenticating = true;
        final AccountManager am = AccountManager.get(mContext);
        Account account = TomahawkUtils.getAccountByName(mContext, getAuthenticatorUtilsName());
        if (am != null && account != null) {
            am.removeAccount(
                    TomahawkUtils.getAccountByName(mContext, getAuthenticatorUtilsName()), null,
                    null);
        }
        LibSpotifyWrapper.logoutUser();
    }

    public static boolean isSpotifyLoggedIn() {
        return mIsSpotifyLoggedIn;
    }
}
