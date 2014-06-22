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

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

public class RdioAuthenticatorUtils extends AuthenticatorUtils implements RdioListener {

    // Used for debug logging
    private final static String TAG = RdioAuthenticatorUtils.class.getSimpleName();

    public static final String ACCOUNT_NAME = "Rdio-Account";

    public static final String RDIO_APPKEY = "Z3FiN3oyejhoNmU3ZTc2emNicTl3ZHlw";

    public static final String RDIO_APPKEYSECRET = "YlhyRndVZUY5cQ==";

    private Rdio mRdio;

    public RdioAuthenticatorUtils(Context context) {
        mContext = context;
        onInit();
    }

    @Override
    public void onInit() {
        loginWithToken();
    }

    @Override
    public void onLogin(String username) {
        if (TextUtils.isEmpty(username)) {
            Log.d(TAG, "TomahawkService: Rdio user was already logged in :)");
        } else {
            Log.d(TAG,
                    "TomahawkService: Rdio user '" + username + "' logged in successfully :)");
        }
    }

    @Override
    public void onLoginFailed(final String message) {
        Log.d(TAG, "TomahawkService: Rdio login failed :(, Error: " + message);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
            }
        });
        AuthenticatorManager.getInstance()
                .onLoggedInOut(AuthenticatorManager.AUTHENTICATOR_ID_RDIO, false,
                        PipeLine.PLUGINNAME_RDIO);
        mIsAuthenticating = false;
    }

    @Override
    public void onLogout() {
        Log.d(TAG, "TomahawkService: Rdio user logged out");
        AuthenticatorManager.getInstance()
                .onLoggedInOut(AuthenticatorManager.AUTHENTICATOR_ID_RDIO, false,
                        PipeLine.PLUGINNAME_RDIO);
        mIsAuthenticating = false;
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
            am.setUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN, refreshToken);
            am.setUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN_SECRET,
                    accessToken);
        }
        mRdio.prepareForPlayback();
        AuthenticatorManager.getInstance()
                .onLoggedInOut(AuthenticatorManager.AUTHENTICATOR_ID_RDIO, true,
                        PipeLine.PLUGINNAME_RDIO);
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
        return AUTHENTICATOR_NAME_RDIO;
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
                    new String(
                            Base64.decode(RdioAuthenticatorUtils.RDIO_APPKEYSECRET,
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
        Account account = new Account(ACCOUNT_NAME,
                TomahawkApp.getContext().getString(R.string.accounttype_string));
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        String accessToken = null;
        String accessTokenSecret = null;
        if (am != null) {
            accessToken = am.getUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN);
            accessTokenSecret = am.getUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN_SECRET);
        }
        try {
            mRdio = new Rdio(new String(Base64.decode(RDIO_APPKEY, Base64.DEFAULT), "UTF-8"),
                    new String(Base64.decode(RDIO_APPKEYSECRET, Base64.DEFAULT), "UTF-8"),
                    accessToken, accessTokenSecret, TomahawkApp.getContext(), this);
            if (accessToken == null || accessTokenSecret == null) {
                // If either one is null, reset both of them
                onRdioAuthorised(null, null);
            } else {
                Log.d(TAG, "Found cached credentials:");
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
        final AccountManager am = AccountManager.get(mContext);
        Account account = TomahawkUtils.getAccountByName(mContext, getAuthenticatorUtilsName());
        if (am != null && account != null) {
            am.removeAccount(account, null, null);
        }
        onLogout();
    }

    @Override
    public boolean isLoggedIn() {
        Account account = new Account(ACCOUNT_NAME,
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
        Log.d(TAG, "Tell the user that playback is stopping.");
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

        onAuthTokenProvided(ACCOUNT_NAME, accessToken, 0, accessTokenSecret, 0);
    }

    public Rdio getRdio() {
        return mRdio;
    }
}
