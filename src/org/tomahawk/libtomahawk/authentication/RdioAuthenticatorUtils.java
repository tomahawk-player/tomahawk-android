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

import com.rdio.android.api.OAuth1WebViewActivity;
import com.rdio.android.api.Rdio;
import com.rdio.android.api.RdioListener;

import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;

public class RdioAuthenticatorUtils extends AuthenticatorUtils implements RdioListener {

    // Used for debug logging
    private final static String TAG = RdioAuthenticatorUtils.class.getSimpleName();

    public static final String ACCOUNT_PRETTY_NAME = "Rdio-Account";

    public static final String RDIO_APPKEY = "Z3FiN3oyejhoNmU3ZTc2emNicTl3ZHlw";

    public static final String RDIO_APPKEYSECRET = "YlhyRndVZUY5cQ==";

    private Rdio mRdio;

    public RdioAuthenticatorUtils(String id, String prettyName) {
        super(id, prettyName);

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
            Log.d(TAG, "Rdio user was already logged in :)");
        } else {
            Log.d(TAG, "Rdio user '" + username + "' logged in successfully :)");
        }
        Account account = new Account(ACCOUNT_PRETTY_NAME,
                TomahawkApp.getContext().getString(R.string.accounttype_string));
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        if (am != null) {
            am.addAccountExplicitly(account, null, new Bundle());
            am.setUserData(account, AuthenticatorUtils.ACCOUNT_NAME,
                    getAccountName());
            am.setUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN, refreshToken);
            am.setUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN_SECRET,
                    accessToken);
        }
        try {
            mRdio = new Rdio(new String(Base64.decode(RDIO_APPKEY, Base64.DEFAULT), "UTF-8"),
                    new String(Base64.decode(RDIO_APPKEYSECRET, Base64.DEFAULT), "UTF-8"),
                    refreshToken, accessToken, TomahawkApp.getContext(), this);
            mRdio.prepareForPlayback();
            AuthenticatorManager.broadcastConfigTestResult(getId(),
                    AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS,
                    AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "onAuthTokenProvided: " + e.getClass() + ": " + e.getLocalizedMessage());
            onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                    e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onLoginFailed(int type, String message) {
        Log.d(TAG, "Rdio login failed :(, Type:" + type + ", Error: " + message);
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS, type,
                message);
    }

    @Override
    public void onLogout() {
        Log.d(TAG, "Rdio user logged out");
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS,
                AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_LOGOUT);
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
     * NOP, because we can only log into rdio via its OAuth1WebViewActivity.
     */
    @Override
    public void login(Activity activity, String email, String password) {
        try {
            Intent myIntent = new Intent(TomahawkApp.getContext(),
                    OAuth1WebViewActivity.class);
            myIntent.putExtra(OAuth1WebViewActivity.EXTRA_CONSUMER_KEY,
                    new String(Base64.decode(RdioAuthenticatorUtils.RDIO_APPKEY,
                            Base64.DEFAULT), "UTF-8")
            );
            myIntent.putExtra(OAuth1WebViewActivity.EXTRA_CONSUMER_SECRET,
                    new String(Base64.decode(RdioAuthenticatorUtils.RDIO_APPKEYSECRET,
                            Base64.DEFAULT), "UTF-8")
            );
            activity.startActivityForResult(myIntent, 1);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "onCreateDialog: " + e.getClass() + ": "
                    + e.getLocalizedMessage());
        }
    }

    /**
     * Try to login to rdio with stored credentials
     */
    public void loginWithToken() {
        Account account = new Account(ACCOUNT_PRETTY_NAME,
                TomahawkApp.getContext().getString(R.string.accounttype_string));
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        String accessToken = null;
        String accessTokenSecret = null;
        if (am != null) {
            accessToken = am.getUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN);
            accessTokenSecret = am.getUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN_SECRET);
        }
        try {
            if (accessToken == null || accessTokenSecret == null) {
                // If either one is null, reset both of them
                if (am != null) {
                    am.removeAccount(account, null, null);
                }
            } else {
                Log.d(TAG, "Found cached credentials:");
                mRdio = new Rdio(new String(Base64.decode(RDIO_APPKEY, Base64.DEFAULT), "UTF-8"),
                        new String(Base64.decode(RDIO_APPKEYSECRET, Base64.DEFAULT), "UTF-8"),
                        accessToken, accessTokenSecret, TomahawkApp.getContext(), this);
                mRdio.prepareForPlayback();
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "<init>: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    /**
     * Logout rdio
     */
    @Override
    public void logout(Activity activity) {
        final AccountManager am = AccountManager.get(TomahawkApp.getContext());
        Account account = TomahawkUtils.getAccountByName(getAccountName());
        if (am != null && account != null) {
            am.removeAccount(account, null, null);
        }
        onLogout();
    }

    @Override
    public boolean isLoggedIn() {
        Account account = new Account(ACCOUNT_PRETTY_NAME,
                TomahawkApp.getContext().getString(R.string.accounttype_string));
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        return am != null && am.getUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN) != null
                && am.getUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN_SECRET) != null;
    }

    @Override
    public void onRdioReadyForPlayback() {
        Log.d(TAG, "Rdio SDK is ready for playback");
    }

    @Override
    public void onRdioUserPlayingElsewhere() {
        Log.d(TAG, "onRdioUserPlayingElsewhere()");
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS,
                AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_PLAYINGELSEWHERE);
    }

    /*
     * Dispatched by the Rdio object once the setTokenAndSecret call has finished, and the credentials are
     * ready to be used to make API calls.  The token & token secret are passed in so that you can
     * save/cache them for future re-use.
     * @see com.rdio.android.api.RdioListener#onRdioAuthorised(java.lang.String, java.lang.String)
     */
    @Override
    public void onRdioAuthorised(String accessToken, String accessTokenSecret) {
        Log.d(TAG, "Rdio Application authorised, saving access token & secret.");

        onLogin(ACCOUNT_PRETTY_NAME, accessToken, 0, accessTokenSecret, 0);
    }

    public Rdio getRdio() {
        return mRdio;
    }
}
