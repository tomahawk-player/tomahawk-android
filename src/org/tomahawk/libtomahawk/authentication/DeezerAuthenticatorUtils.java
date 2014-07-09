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

import com.deezer.sdk.network.connect.DeezerConnect;
import com.deezer.sdk.network.connect.SessionStore;
import com.deezer.sdk.network.connect.event.DialogError;
import com.deezer.sdk.network.connect.event.DialogListener;
import com.deezer.sdk.network.request.event.DeezerError;
import com.deezer.sdk.network.request.event.OAuthException;

import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class DeezerAuthenticatorUtils extends AuthenticatorUtils {

    // Used for debug logging
    private final static String TAG = DeezerAuthenticatorUtils.class.getSimpleName();

    public static final String ACCOUNT_PRETTY_NAME = "Deezer-Account";

    private final static String APP_ID = "138751";

    private final static String[] PERMISSIONS = new String[]{"offline_access"};

    private DeezerConnect mDeezerConnect;

    public DeezerAuthenticatorUtils(String id, String prettyName) {
        super(id, prettyName);

        mDeezerConnect = new DeezerConnect(APP_ID);
        onInit();
    }

    @Override
    public void onInit() {
        loginWithToken();
    }

    @Override
    public void onLogin(String username, String refreshToken,
            long refreshTokenExpiresIn, String accessToken, long accessTokenExpiresIn) {
        if (TextUtils.isEmpty(username)) {
            Log.d(TAG, "Deezer user was already logged in :)");
        } else {
            Log.d(TAG, "Deezer user '" + username + "' logged in successfully :)");
        }
        Account account = new Account(ACCOUNT_PRETTY_NAME,
                TomahawkApp.getContext().getString(R.string.accounttype_string));
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        if (am != null) {
            am.addAccountExplicitly(account, null, new Bundle());
            am.setUserData(account, AuthenticatorUtils.ACCOUNT_NAME,
                    getAccountName());
            am.setUserData(account, PACKAGE_PREFIX + getId() + ACCESS_TOKEN_SUFFIX, accessToken);
            am.setUserData(account, getAccessTokenExpiresInName(),
                    String.valueOf(accessTokenExpiresIn));
        }
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS,
                AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS);
    }

    @Override
    public void onLoginFailed(int type, String message) {
        Log.d(TAG, "Deezer login failed :(, Type:" + type + ", Error: " + message);
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS, type,
                message);
    }

    @Override
    public void onLogout() {
        Log.d(TAG, "Deezer user logged out");
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS,
                AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_LOGOUT);
    }

    @Override
    public boolean isLoggedIn() {
        Account account = new Account(ACCOUNT_PRETTY_NAME,
                TomahawkApp.getContext().getString(R.string.accounttype_string));
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        return am != null && am.getUserData(account, getAccessTokenName()) != null
                && am.getUserData(account, getAccessTokenExpiresInName()) != null;
    }

    @Override
    public int getIconResourceId() {
        return 0;
    }

    @Override
    public int getUserIdEditTextHintResId() {
        return 0;
    }

    /**
     * Try to login to deezer
     */
    @Override
    public void login(Activity activity, String email, String password) {
        mDeezerConnect.authorize(activity, PERMISSIONS, new DialogHandler());
    }

    /**
     * Try to login to deezer with stored tokens
     */
    public void loginWithToken() {
        Log.d(TAG, "loginWithToken");
        Account account = new Account(ACCOUNT_PRETTY_NAME,
                TomahawkApp.getContext().getString(R.string.accounttype_string));
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        if (am != null) {
            am.addAccountExplicitly(account, null, new Bundle());
            String accessToken = am.getUserData(account, getAccessTokenName());
            String accessTokenExpiresIn = am.getUserData(account, getAccessTokenExpiresInName());
            if (accessToken != null && accessTokenExpiresIn != null) {
                mDeezerConnect.setAccessToken(TomahawkApp.getContext(), accessToken);
                mDeezerConnect.setAccessExpires(Long.valueOf(accessTokenExpiresIn));
            }
        }
    }

    /**
     * Logout deezer
     */
    @Override
    public void logout(Activity activity) {
        mDeezerConnect.logout(activity);
        final AccountManager am = AccountManager.get(TomahawkApp.getContext());
        Account account = TomahawkUtils.getAccountByName(getAccountName());
        if (am != null && account != null) {
            am.removeAccount(account, null, null);
        }
        onLogout();
    }

    public DeezerConnect getDeezerConnect() {
        return mDeezerConnect;
    }

    /**
     * Handle DeezerConnect callbacks.
     */
    private class DialogHandler implements DialogListener {

        @Override
        public void onComplete(final Bundle values) {
            SessionStore sessionStore = new SessionStore();
            sessionStore.save(mDeezerConnect, TomahawkApp.getContext());
            onLogin(values.getString("username"), null, 0,
                    mDeezerConnect.getAccessToken(), (int) mDeezerConnect.getAccessExpires());
        }

        @Override
        public void onDeezerError(final DeezerError deezerError) {
            Log.e(TAG, "DialogError error during login", deezerError);
            onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                    deezerError.getLocalizedMessage());
        }

        @Override
        public void onError(final DialogError dialogError) {
            Log.e(TAG, "DialogError error during login", dialogError);
            onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                    dialogError.getLocalizedMessage());
        }

        @Override
        public void onCancel() {
            Log.d(TAG, "Deezer authentication cancelled");
            onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                    "Deezer authentication cancelled.");
        }

        @Override
        public void onOAuthException(OAuthException oAuthException) {
            Log.e(TAG, "DialogError error during login", oAuthException);
            onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                    oAuthException.getLocalizedMessage());
        }
    }
}
