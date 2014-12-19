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

import org.tomahawk.tomahawk_android.TomahawkApp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;

public class RdioAuthenticatorUtils extends AuthenticatorUtils implements RdioListener {

    // Used for debug logging
    private final static String TAG = RdioAuthenticatorUtils.class.getSimpleName();

    public static final String RDIO_PRETTY_NAME = "Rdio";

    private static final String STORAGE_KEY_EXTRA_TOKEN = "rdio_extratoken";

    private static final String STORAGE_KEY_EXTRA_TOKEN_SECRET = "rdio_extratokensecret";

    private static final String RDIO_APPKEY = "Z3FiN3oyejhoNmU3ZTc2emNicTl3ZHlw";

    private static final String RDIO_APPKEYSECRET = "YlhyRndVZUY5cQ==";

    private Rdio mRdio;

    public RdioAuthenticatorUtils() {
        super(TomahawkApp.PLUGINNAME_RDIO, RDIO_PRETTY_NAME);

        loginWithToken();
    }

    public void onLogin(String extraToken, String extraTokenSecret) {
        Log.d(TAG, "Rdio user logged in successfully :)");
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext()).edit();
        editor.putString(STORAGE_KEY_EXTRA_TOKEN, extraToken);
        editor.putString(STORAGE_KEY_EXTRA_TOKEN_SECRET, extraTokenSecret);
        editor.commit();
        try {
            mRdio = new Rdio(new String(Base64.decode(RDIO_APPKEY, Base64.DEFAULT), "UTF-8"),
                    new String(Base64.decode(RDIO_APPKEYSECRET, Base64.DEFAULT), "UTF-8"),
                    extraToken, extraTokenSecret, TomahawkApp.getContext(), this);
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

    public void onLoginFailed(int type, String message) {
        Log.d(TAG, "Rdio login failed :(, Type:" + type + ", Error: " + message);
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS, type,
                message);
    }

    public void onLogout() {
        Log.d(TAG, "Rdio user logged out");
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS,
                AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_LOGOUT);
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public int getIconResourceId() {
        return 0;
    }

    @Override
    public int getUserIdEditTextHintResId() {
        return 0;
    }

    @Override
    public void register(final String name, final String password, final String email) {
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
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());
        String extraToken = preferences.getString(STORAGE_KEY_EXTRA_TOKEN, null);
        String extraTokenSecret = preferences.getString(STORAGE_KEY_EXTRA_TOKEN_SECRET, null);
        try {
            if (extraToken == null || extraTokenSecret == null) {
                // If either one is null, reset both of them
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(TomahawkApp.getContext()).edit();
                editor.remove(STORAGE_KEY_EXTRA_TOKEN);
                editor.remove(STORAGE_KEY_EXTRA_TOKEN_SECRET);
                editor.commit();
            } else {
                Log.d(TAG, "Found cached credentials:");
                mRdio = new Rdio(new String(Base64.decode(RDIO_APPKEY, Base64.DEFAULT), "UTF-8"),
                        new String(Base64.decode(RDIO_APPKEYSECRET, Base64.DEFAULT), "UTF-8"),
                        extraToken, extraTokenSecret, TomahawkApp.getContext(), this);
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
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext()).edit();
        editor.remove(STORAGE_KEY_EXTRA_TOKEN);
        editor.remove(STORAGE_KEY_EXTRA_TOKEN_SECRET);
        editor.commit();
        onLogout();
    }

    @Override
    public boolean isLoggedIn() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());
        return preferences.contains(STORAGE_KEY_EXTRA_TOKEN)
                && preferences.contains(STORAGE_KEY_EXTRA_TOKEN_SECRET);
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
    public void onRdioAuthorised(String extraToken, String extraTokenSecret) {
        Log.d(TAG, "Rdio Application authorised, saving extra token & secret.");

        onLogin(extraToken, extraTokenSecret);
    }

    public Rdio getRdio() {
        return mRdio;
    }

    @Override
    public String getUserName() {
        return null;
    }
}
