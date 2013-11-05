/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
package org.tomahawk.tomahawk_android.services;

import org.tomahawk.libtomahawk.resolver.spotify.LibSpotifyWrapper;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyResolver;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.OnLoggedInOutListener;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This service wraps all non-playback service functionality. Like auth stuff.
 */
public class TomahawkService extends Service {

    // Used for debug logging
    private final static String TAG = TomahawkService.class.getName();

    // String tags used to store spotify credentials and preferred bitrate
    public static final String SPOTIFY_CREDS_BLOB = "spotify_creds_blob";

    public static final String SPOTIFY_CREDS_EMAIL = "spotify_creds_email";

    public static final String SPOTIFY_PREF_BITRATE = "spotify_pref_bitrate";

    public static final int SPOTIFY_PREF_BITRATE_MODE_LOW = 0;

    public static final int SPOTIFY_PREF_BITRATE_MODE_MEDIUM = 1;

    public static final int SPOTIFY_PREF_BITRATE_MODE_HIGH = 2;

    // After this time we will check if this service can be killed
    private static final int DELAY_TO_KILL = 300000;

    private boolean mHasBoundServices;

    private final IBinder mBinder = new TomahawkServiceBinder();

    private ServiceBroadcastReceiver mServiceBroadcastReceiver;

    private WifiManager.WifiLock mWifiLock;

    private SharedPreferences mSharedPreferences;

    private String mSpotifyUserId;

    private boolean mIsAttemptingLogInOut;

    private OnLoggedInOutListener mOnLoggedInOutListener;

    public static interface OnLoginListener {

        void onLogin(String username);

        void onLoginFailed(String message);

        void onLogout();

    }

    // This listener handles every event regarding the login/logout methods
    private OnLoginListener mOnLoginListener = new OnLoginListener() {
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

        private void logInOut(boolean loggedIn) {
            mIsAttemptingLogInOut = false;
            if (mOnLoggedInOutListener != null) {
                mOnLoggedInOutListener.onLoggedInOut(TomahawkApp.RESOLVER_ID_SPOTIFY, loggedIn);
            }
            if (!loggedIn) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.remove(SPOTIFY_CREDS_BLOB);
                editor.remove(SPOTIFY_CREDS_EMAIL);
                editor.commit();
            }
            SpotifyResolver spotifyResolver = (SpotifyResolver) ((TomahawkApp) getApplication())
                    .getPipeLine().getResolver(TomahawkApp.RESOLVER_ID_SPOTIFY);
            spotifyResolver.setReady(loggedIn);
        }
    };

    public static interface OnCredBlobUpdatedListener {

        void onCredBlobUpdated(String blob);
    }

    // This listener fires, if libspotify calls back with a blob
    private OnCredBlobUpdatedListener mOnCredBlobUpdatedListener = new OnCredBlobUpdatedListener() {

        /**
         * Store the given blob-string, so we can relogin in a later session
         * @param blob the given blob-string
         */
        @Override
        public void onCredBlobUpdated(String blob) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(SPOTIFY_CREDS_BLOB, blob);
            if (mSpotifyUserId != null) {
                editor.putString(SPOTIFY_CREDS_EMAIL, mSpotifyUserId);
            }
            editor.commit();
        }
    };

    public static class TomahawkServiceConnection implements ServiceConnection {

        private TomahawkServiceConnectionListener mTomahawkServiceConnectionListener;

        public interface TomahawkServiceConnectionListener {

            public void setTomahawkService(TomahawkService ps);

            public void onTomahawkServiceReady();
        }

        public TomahawkServiceConnection(
                TomahawkServiceConnectionListener tomahawkServiceConnectedListener) {
            mTomahawkServiceConnectionListener = tomahawkServiceConnectedListener;
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            TomahawkServiceBinder binder = (TomahawkServiceBinder) service;
            mTomahawkServiceConnectionListener.setTomahawkService(binder.getService());
            mTomahawkServiceConnectionListener.onTomahawkServiceReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mTomahawkServiceConnectionListener.setTomahawkService(null);
        }
    }

    public class TomahawkServiceBinder extends Binder {

        public TomahawkService getService() {
            return TomahawkService.this;
        }
    }

    // Stops this service if it doesn't have any bound services
    private final Handler mKillTimerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!mHasBoundServices) {
                stopSelf();
            }
        }
    };

    private class ServiceBroadcastReceiver extends BroadcastReceiver {

        /**
         * Set the spotify preferred bitrate to high if we're connected to wifi, otherwise set
         * bitrate to user-preferred setting, if none set yet, default to medium
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conMan = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.d("WifiReceiver", "Have Wifi Connection");
                LibSpotifyWrapper.setbitrate(SPOTIFY_PREF_BITRATE_MODE_HIGH);
            } else {
                Log.d("WifiReceiver", "Don't have Wifi Connection");
                int prefbitrate = mSharedPreferences
                        .getInt(SPOTIFY_PREF_BITRATE, SPOTIFY_PREF_BITRATE_MODE_MEDIUM);
                LibSpotifyWrapper.setbitrate(prefbitrate);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());

        // Load our libspotify wrapper classes
        System.loadLibrary("spotify");
        System.loadLibrary("spotifywrapper");

        // Initialize LibspotifyWrapper
        LibSpotifyWrapper
                .init(LibSpotifyWrapper.class.getClassLoader(), getFilesDir() + "/Spotify");

        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
        mWifiLock.acquire();

        // Try to login spotify
        loginSpotifyWithStoredCreds();

        // Start our killtimer (watchdog-style)
        mKillTimerHandler.removeCallbacksAndMessages(null);
        Message msg = mKillTimerHandler.obtainMessage();
        mKillTimerHandler.sendMessageDelayed(msg, DELAY_TO_KILL);

        mServiceBroadcastReceiver = new ServiceBroadcastReceiver();
        registerReceiver(mServiceBroadcastReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public int onStartCommand(Intent i, int j, int k) {
        super.onStartCommand(i, j, k);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        mHasBoundServices = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mHasBoundServices = false;
        stopSelf();
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mWifiLock.release();
        LibSpotifyWrapper.destroy();
        unregisterReceiver(mServiceBroadcastReceiver);
    }

    /**
     * Try to login to spotify with stored credentials
     */
    public void loginSpotifyWithStoredCreds() {
        mIsAttemptingLogInOut = true;
        String email = mSharedPreferences.getString(SPOTIFY_CREDS_EMAIL, null);
        String blob = mSharedPreferences.getString(SPOTIFY_CREDS_BLOB, null);
        if (email != null && blob != null) {
            LibSpotifyWrapper
                    .loginUser(email, "", blob, mOnLoginListener, mOnCredBlobUpdatedListener);
        }
    }

    /**
     * Try to login to spotify with given credentials
     */
    public void loginSpotify(String email, String password) {
        mIsAttemptingLogInOut = true;
        if (email != null && password != null) {
            LibSpotifyWrapper
                    .loginUser(email, password, "", mOnLoginListener, mOnCredBlobUpdatedListener);
        }
    }

    /**
     * Logout spotify
     */
    public void logoutSpotify() {
        mIsAttemptingLogInOut = true;
        LibSpotifyWrapper.logoutUser(mOnLoginListener);
    }

    public boolean isAttemptingLogInOut() {
        return mIsAttemptingLogInOut;
    }

    public String getSpotifyUserId() {
        return mSpotifyUserId;
    }

    public void setOnLoggedInOutListener(OnLoggedInOutListener onLoggedInOutListener) {
        mOnLoggedInOutListener = onLoggedInOutListener;
    }

}
