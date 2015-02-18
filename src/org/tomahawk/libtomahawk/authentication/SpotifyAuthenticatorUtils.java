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
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
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
import android.util.Log;

import de.greenrobot.event.EventBus;

public class SpotifyAuthenticatorUtils extends AuthenticatorUtils {

    // Used for debug logging
    private final static String TAG = SpotifyAuthenticatorUtils.class.getSimpleName();

    public static final String SPOTIFY_PRETTY_NAME = "Spotify";

    private static final String SPOTIFY_IS_LOGGED_IN = "spotify_is_logged_in";

    // String tags used to store Spotify's preferred bitrate
    private static final String SPOTIFY_PREF_BITRATE
            = "org.tomahawk.tomahawk_android.spotify_pref_bitrate";

    public static final int SPOTIFY_PREF_BITRATE_MODE_LOW = 0;

    public static final int SPOTIFY_PREF_BITRATE_MODE_MEDIUM = 1;

    public static final int SPOTIFY_PREF_BITRATE_MODE_HIGH = 2;

    private Messenger mToSpotifyMessenger = null;

    private final Messenger mFromSpotifyMessenger =
            new Messenger(new FromSpotifyHandler(Looper.getMainLooper()));

    /**
     * The following field only contain valid data, if the SpotifyService wasn't available. In that
     * case we cache the action and its params here and retry the action as soon as the
     * SpotifyService is available again (takes usually just a few ms).
     */
    private boolean mIsCached;

    private int mCachedAction;

    private int mCachedBitrate;

    private boolean mIsLoggedIn;

    /**
     * Handler of incoming messages from the SpotifyService's messenger.
     */
    private class FromSpotifyHandler extends Handler {

        private FromSpotifyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SpotifyService.MSG_ONLOGIN:
                    onLogin(msg.getData().getString(SpotifyService.STRING_KEY));
                    break;
                case SpotifyService.MSG_ONLOGINFAILED:
                    onLoginFailed(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_OTHER,
                            msg.getData().getString(SpotifyService.STRING_KEY));
                    break;
                case SpotifyService.MSG_ONLOGOUT:
                    onLogout();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    public SpotifyAuthenticatorUtils() {
        super(TomahawkApp.PLUGINNAME_SPOTIFY, SPOTIFY_PRETTY_NAME);

        if (PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext())
                .getBoolean(SPOTIFY_IS_LOGGED_IN, false)) {
            login(null, null, null);
        }
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
                        login(null, null);
                        break;
                    case SpotifyService.MSG_LOGOUT:
                        logout();
                        break;
                }
                mIsCached = false;
            }
        }
    }

    public void onLogin(String accessToken) {
        Log.d(TAG, "Spotify user logged in successfully :)");
        mIsLoggedIn = true;
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext()).edit();
        editor.putBoolean(SPOTIFY_IS_LOGGED_IN, true);
        editor.apply();

        updateBitrate();

        ScriptResolver spotifyResolver = (ScriptResolver) PipeLine.getInstance()
                .getResolver(TomahawkApp.PLUGINNAME_SPOTIFY);
        spotifyResolver.setAccessToken(accessToken);

        AuthenticatorManager.ConfigTestResultEvent event
                = new AuthenticatorManager.ConfigTestResultEvent();
        event.mComponent = this;
        event.mType = AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS;
        EventBus.getDefault().post(event);
        AuthenticatorManager.showToast(getPrettyName(), event);
    }

    public void onLoginFailed(int type, String message) {
        Log.d(TAG, "Spotify login failed :(, Type:" + type + ", Error: " + message);
        mIsLoggedIn = false;
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext()).edit();
        editor.putBoolean(SPOTIFY_IS_LOGGED_IN, false);
        editor.apply();

        AuthenticatorManager.ConfigTestResultEvent event
                = new AuthenticatorManager.ConfigTestResultEvent();
        event.mComponent = this;
        event.mType = type;
        event.mMessage = message;
        EventBus.getDefault().post(event);
        AuthenticatorManager.showToast(getPrettyName(), event);
    }

    public void onLogout() {
        Log.d(TAG, "Spotify user logged out");
        mIsLoggedIn = false;
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext()).edit();
        editor.putBoolean(SPOTIFY_IS_LOGGED_IN, false);
        editor.apply();

        AuthenticatorManager.ConfigTestResultEvent event
                = new AuthenticatorManager.ConfigTestResultEvent();
        event.mComponent = this;
        event.mType = AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_LOGOUT;
        EventBus.getDefault().post(event);
        AuthenticatorManager.showToast(getPrettyName(), event);
    }

    @Override
    public String getDescription() {
        return TomahawkApp.getContext().getString(R.string.preferences_spotify_text,
                SPOTIFY_PRETTY_NAME);
    }

    @Override
    public boolean isLoggedIn() {
        return mIsLoggedIn;
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
            SpotifyServiceUtils
                    .sendMsg(mToSpotifyMessenger, SpotifyService.MSG_SETBITRATE, bitrate);
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
        if (mToSpotifyMessenger != null) {
            SpotifyServiceUtils.sendMsg(mToSpotifyMessenger, SpotifyService.MSG_LOGIN);
        } else {
            mIsCached = true;
            mCachedAction = SpotifyService.MSG_LOGIN;
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
            SpotifyServiceUtils.sendMsg(mToSpotifyMessenger, SpotifyService.MSG_LOGOUT);
        } else {
            mIsCached = true;
            mCachedAction = SpotifyService.MSG_LOGOUT;
            TomahawkApp.getContext().sendBroadcast(
                    new Intent(SpotifyService.REQUEST_SPOTIFYSERVICE));
        }
    }

    @Override
    public String getUserName() {
        return null;
    }

    @Override
    public boolean doesAllowRegistration() {
        return false;
    }
}
