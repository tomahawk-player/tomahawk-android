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

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class DeezerAuthenticatorUtils extends AuthenticatorUtils {

    // Used for debug logging
    private final static String TAG = DeezerAuthenticatorUtils.class.getName();

    public static final String ACCOUNT_NAME = "Deezer-Account";

    private final static String APP_ID = "138751";

    private final static String[] PERMISSIONS = new String[]{};

    private DeezerConnect mDeezerConnect;

    public DeezerAuthenticatorUtils(Context context) {
        mContext = context;
        mDeezerConnect = new DeezerConnect(APP_ID);
    }

    @Override
    public void onInit() {
        loginWithToken();
    }

    @Override
    public void onLogin(String username) {
        if (TextUtils.isEmpty(username)) {
            Log.d(TAG, "TomahawkService: Deezer user was already logged in :)");
        } else {
            Log.d(TAG,
                    "TomahawkService: Deezer user '" + username + "' logged in successfully :)");
        }
    }

    @Override
    public void onLoginFailed(final String message) {
        Log.d(TAG, "TomahawkService: Deezer login failed :(, Error: " + message);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
            }
        });
        AuthenticatorManager.getInstance()
                .onLoggedInOut(AuthenticatorManager.AUTHENTICATOR_ID_DEEZER, false,
                        PipeLine.PLUGINNAME_DEEZER);
        mIsAuthenticating = false;
    }

    @Override
    public void onLogout() {
        Log.d(TAG, "TomahawkService: Deezer user logged out");
        AuthenticatorManager.getInstance()
                .onLoggedInOut(AuthenticatorManager.AUTHENTICATOR_ID_DEEZER, false,
                        PipeLine.PLUGINNAME_DEEZER);
        mIsAuthenticating = false;
    }

    @Override
    public boolean isLoggedIn() {
        Account account = new Account(ACCOUNT_NAME,
                TomahawkApp.getContext().getString(R.string.accounttype_string));
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        return am != null && am.getUserData(account, ACCESS_TOKEN_DEEZER) != null
                && am.getUserData(account, ACCESS_TOKEN_EXPIRES_IN_DEEZER) != null;
    }

    /**
     * Store the given blob-string, so we can relogin in a later session
     */
    @Override
    public void onAuthTokenProvided(String username, String refreshToken,
            int refreshTokenExpiresIn, String accessToken, int accessTokenExpiresIn) {
        Account account = new Account(ACCOUNT_NAME,
                TomahawkApp.getContext().getString(R.string.accounttype_string));
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        if (am != null) {
            am.addAccountExplicitly(account, null, new Bundle());
            am.setUserData(account, AuthenticatorUtils.AUTHENTICATOR_NAME,
                    getAuthenticatorUtilsName());
            am.setUserData(account, ACCESS_TOKEN_DEEZER, accessToken);
            am.setUserData(account, ACCESS_TOKEN_EXPIRES_IN_DEEZER,
                    String.valueOf(accessTokenExpiresIn));
        }
        AuthenticatorManager.getInstance()
                .onLoggedInOut(AuthenticatorManager.AUTHENTICATOR_ID_DEEZER, true,
                        PipeLine.PLUGINNAME_DEEZER);
    }

    @Override
    public int getTitleResourceId() {
        return 0;
    }

    @Override
    public int getIconResourceId() {
        return 0;
    }

    @Override
    public String getAuthenticatorUtilsName() {
        return AUTHENTICATOR_NAME_DEEZER;
    }

    @Override
    public String getAuthenticatorUtilsTokenType() {
        return null;
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
        Account account = new Account(ACCOUNT_NAME,
                TomahawkApp.getContext().getString(R.string.accounttype_string));
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        if (am != null) {
            am.addAccountExplicitly(account, null, new Bundle());
            String accessToken = am.getUserData(account, ACCESS_TOKEN_DEEZER);
            long accessTokenExpiresIn = Long.valueOf(
                    am.getUserData(account, ACCESS_TOKEN_EXPIRES_IN_DEEZER));
            mDeezerConnect.setAccessToken(mContext, accessToken);
            mDeezerConnect.setAccessExpires(accessTokenExpiresIn);
        }
    }

    /**
     * Logout deezer
     */
    @Override
    public void logout(Activity activity) {
        mDeezerConnect.logout(activity);
        final AccountManager am = AccountManager.get(mContext);
        Account account = TomahawkUtils.getAccountByName(mContext, getAuthenticatorUtilsName());
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
            onLogin(values.getString("username"));
            onAuthTokenProvided(values.getString("username"), null, 0,
                    mDeezerConnect.getAccessToken(), (int) mDeezerConnect.getAccessExpires());
        }

        @Override
        public void onDeezerError(final DeezerError deezerError) {
            Log.e(TAG, "DialogError error during login", deezerError);
            onLoginFailed(deezerError.getLocalizedMessage());
        }

        @Override
        public void onError(final DialogError dialogError) {
            Log.e(TAG, "DialogError error during login", dialogError);
            onLoginFailed(dialogError.getLocalizedMessage());
        }

        @Override
        public void onCancel() {
            Log.e(TAG, "Deezer authentication cancelled");
        }

        @Override
        public void onOAuthException(OAuthException oAuthException) {
            Log.e(TAG, "DialogError error during login", oAuthException);
            onLoginFailed(oAuthException.getLocalizedMessage());
        }
    }
}
