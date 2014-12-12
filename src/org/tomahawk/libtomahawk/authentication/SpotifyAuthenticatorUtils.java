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

import org.tomahawk.libtomahawk.infosystem.InfoSystemUtils;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyServiceUtils;
import org.tomahawk.libtomahawk.resolver.spotify.models.SpotifyLogin;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.SpotifyService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

public class SpotifyAuthenticatorUtils extends AuthenticatorUtils {

    // Used for debug logging
    private final static String TAG = SpotifyAuthenticatorUtils.class.getSimpleName();

    public static final String SPOTIFY_PRETTY_NAME = "Spotify";

    private static final String STORAGE_KEY_USERNAME = "spotify_username";

    private static final String STORAGE_KEY_BLOB = "spotify_blob";

    // String tags used to store Spotify's preferred bitrate
    private static final String SPOTIFY_PREF_BITRATE
            = "org.tomahawk.tomahawk_android.spotify_pref_bitrate";

    public static final int SPOTIFY_PREF_BITRATE_MODE_LOW = 0;

    public static final int SPOTIFY_PREF_BITRATE_MODE_MEDIUM = 1;

    public static final int SPOTIFY_PREF_BITRATE_MODE_HIGH = 2;

    private Messenger mToSpotifyMessenger = null;

    private final Messenger mFromSpotifyMessenger =
            new Messenger(new FromSpotifyHandler(Looper.getMainLooper()));

    private boolean mInitialized;

    /**
     * The following field only contain valid data, if the SpotifyService wasn't available. In that
     * case we cache the action and its params here and retry the action as soon as the
     * SpotifyService is available again (takes usually just a few ms).
     */
    private boolean mIsCached;

    private int mCachedAction;

    private SpotifyLogin mCachedLoginData;

    private int mCachedBitrate;

    /**
     * Handler of incoming messages from the SpotifyService's messenger.
     */
    private class FromSpotifyHandler extends Handler {

        private FromSpotifyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case SpotifyService.MSG_ONINIT:
                        mInitialized = true;
                        loginWithToken();
                        break;
                    case SpotifyService.MSG_ONLOGIN:
                        onLogin(msg.getData().getString(SpotifyService.STRING_KEY), null);
                        break;
                    case SpotifyService.MSG_ONLOGINFAILED:
                        onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                                msg.getData().getString(SpotifyService.STRING_KEY));
                        break;
                    case SpotifyService.MSG_ONLOGOUT:
                        onLogout();
                        break;
                    case SpotifyService.MSG_ONCREDBLOBUPDATED:
                        SpotifyLogin spotifyLogin = InfoSystemUtils.getObjectMapper()
                                .readValue(msg.getData().getString(SpotifyService.STRING_KEY),
                                        SpotifyLogin.class);
                        onLogin(spotifyLogin.username, spotifyLogin.blob);
                        break;
                    default:
                        super.handleMessage(msg);
                }
            } catch (IOException e) {
                Log.e(TAG, "handleMessage: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
    }

    public SpotifyAuthenticatorUtils() {
        super(TomahawkApp.PLUGINNAME_SPOTIFY, SPOTIFY_PRETTY_NAME);
    }

    public void setToSpotifyMessenger(Messenger toSpotifyMessenger) {
        mToSpotifyMessenger = toSpotifyMessenger;
        if (mToSpotifyMessenger != null) {
            SpotifyServiceUtils.registerMsg(mToSpotifyMessenger, mFromSpotifyMessenger);
            if (mIsCached) {
                switch (mCachedAction) {
                    case SpotifyService.MSG_SETBITRATE:
                        setBitrate(mCachedBitrate);
                        break;
                    case SpotifyService.MSG_LOGIN:
                        if (mCachedLoginData == null) {
                            loginWithToken();
                        } else {
                            login(mCachedLoginData.username, mCachedLoginData.password);
                        }
                        break;
                    case SpotifyService.MSG_LOGOUT:
                        logout();
                        break;
                }
                mIsCached = false;
                mCachedLoginData = null;
            }
        } else {
            mInitialized = false;
        }
    }

    public void onLogin(String username, String blob) {
        if (TextUtils.isEmpty(username)) {
            Log.d(TAG, "Spotify user was already logged in :)");
        } else {
            Log.d(TAG, "Spotify user '" + username + "' logged in successfully :)");
        }
        if (username != null && !TextUtils.isEmpty(username) && blob != null
                && !TextUtils.isEmpty(blob)) {
            Log.d(TAG, "Spotify blob is served and yummy");
            SharedPreferences.Editor editor = PreferenceManager
                    .getDefaultSharedPreferences(TomahawkApp.getContext()).edit();
            editor.putString(STORAGE_KEY_USERNAME, username);
            editor.putString(STORAGE_KEY_BLOB, blob);
            editor.commit();
            updateBitrate();
        }
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS,
                AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS);
    }

    public void onLoginFailed(int type, String message) {
        Log.d(TAG,
                "Spotify login failed :(, Type:" + type + ", Error: " + message);
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS, type,
                message);
    }

    public void onLogout() {
        Log.d(TAG, "Spotify user logged out");
        AuthenticatorManager.broadcastConfigTestResult(getId(),
                AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINTYPE_AUTHUTILS,
                AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_LOGOUT);
    }

    @Override
    public boolean isLoggedIn() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());
        return preferences.contains(STORAGE_KEY_USERNAME)
                && preferences.contains(STORAGE_KEY_BLOB);
    }

    public void updateBitrate() {
        ConnectivityManager conMan = (ConnectivityManager) TomahawkApp.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Log.d(TAG, "Updating bitrate to HIGH, because we have a Wifi connection");
            setBitrate(SpotifyAuthenticatorUtils.SPOTIFY_PREF_BITRATE_MODE_HIGH);
        } else {
            Log.d(TAG, "Updating bitrate to user setting, because we don't have a Wifi connection");
            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(TomahawkApp.getContext());
            int prefbitrate = preferences.getInt(
                    SpotifyAuthenticatorUtils.SPOTIFY_PREF_BITRATE,
                    SpotifyAuthenticatorUtils.SPOTIFY_PREF_BITRATE_MODE_MEDIUM);
            setBitrate(prefbitrate);
        }
    }

    public void setBitrate(int bitrate) {
        if (mToSpotifyMessenger != null) {
            if (mInitialized) {
                SpotifyServiceUtils
                        .sendMsg(mToSpotifyMessenger, SpotifyService.MSG_SETBITRATE, bitrate);
            }
        } else {
            mIsCached = true;
            mCachedAction = SpotifyService.MSG_SETBITRATE;
            mCachedBitrate = bitrate;
            TomahawkApp.getContext().sendBroadcast(
                    new Intent(SpotifyService.REQUEST_SPOTIFYSERVICE));
        }
    }

    @Override
    public int getIconResourceId() {
        return R.drawable.ic_spotify;
    }

    @Override
    public int getUserIdEditTextHintResId() {
        return R.string.login_name_label;
    }

    @Override
    public void register(final String name, final String password, final String email) {
    }

    /**
     * Try to login to spotify with given credentials
     */
    @Override
    public void login(Activity activity, String email, String password) {
        SpotifyLogin spotifyLogin = new SpotifyLogin();
        spotifyLogin.username = email;
        spotifyLogin.password = password;
        if (mToSpotifyMessenger != null) {
            if (mInitialized && spotifyLogin.username != null && spotifyLogin.password != null) {
                try {
                    String jsonString =
                            InfoSystemUtils.getObjectMapper().writeValueAsString(spotifyLogin);
                    SpotifyServiceUtils
                            .sendMsg(mToSpotifyMessenger, SpotifyService.MSG_LOGIN, jsonString);
                } catch (IOException e) {
                    Log.e(TAG, "login: " + e.getClass() + ": " + e.getLocalizedMessage());
                    onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                            e.getClass() + ": " + e.getLocalizedMessage());
                }
            }
        } else {
            mIsCached = true;
            mCachedAction = SpotifyService.MSG_LOGIN;
            mCachedLoginData = spotifyLogin;
            TomahawkApp.getContext().sendBroadcast(
                    new Intent(SpotifyService.REQUEST_SPOTIFYSERVICE));
        }
    }

    /**
     * Try to login to spotify with stored credentials
     */
    public void loginWithToken() {
        if (mToSpotifyMessenger != null) {
            if (mInitialized) {
                SharedPreferences preferences = PreferenceManager
                        .getDefaultSharedPreferences(TomahawkApp.getContext());
                String username = preferences.getString(STORAGE_KEY_USERNAME, null);
                String blob = preferences.getString(STORAGE_KEY_BLOB, null);
                if (username != null && blob != null) {
                    SpotifyLogin spotifyLogin = new SpotifyLogin();
                    spotifyLogin.username = username;
                    spotifyLogin.blob = blob;
                    try {
                        String jsonString =
                                InfoSystemUtils.getObjectMapper().writeValueAsString(spotifyLogin);
                        SpotifyServiceUtils.sendMsg(mToSpotifyMessenger,
                                SpotifyService.MSG_LOGIN, jsonString);
                    } catch (IOException e) {
                        Log.e(TAG, "loginWithToken: " + e.getClass() + ": "
                                + e.getLocalizedMessage());
                        onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                                e.getClass() + ": " + e.getLocalizedMessage());
                    }
                }
            }
        } else {
            mIsCached = true;
            mCachedAction = SpotifyService.MSG_LOGIN;
            mCachedLoginData = null;
            TomahawkApp.getContext().sendBroadcast(
                    new Intent(SpotifyService.REQUEST_SPOTIFYSERVICE));
        }
    }

    /**
     * Logout spotify
     */
    @Override
    public void logout(Activity activity) {
        if (mToSpotifyMessenger != null) {
            if (mInitialized) {
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(TomahawkApp.getContext()).edit();
                editor.remove(STORAGE_KEY_USERNAME);
                editor.remove(STORAGE_KEY_BLOB);
                editor.commit();
                SpotifyServiceUtils.sendMsg(mToSpotifyMessenger, SpotifyService.MSG_LOGOUT);
            }
        } else {
            mIsCached = true;
            mCachedAction = SpotifyService.MSG_LOGOUT;
            TomahawkApp.getContext().sendBroadcast(
                    new Intent(SpotifyService.REQUEST_SPOTIFYSERVICE));
        }
    }

    @Override
    public String getUserName() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());
        return preferences.getString(STORAGE_KEY_USERNAME, "");
    }
}
