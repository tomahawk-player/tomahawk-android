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

import org.tomahawk.libtomahawk.authentication.Authenticator;
import org.tomahawk.libtomahawk.authentication.SpotifyAuthenticator;
import org.tomahawk.libtomahawk.resolver.spotify.LibSpotifyWrapper;
import org.tomahawk.tomahawk_android.TomahawkApp;

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
import android.util.SparseArray;

/**
 * This service wraps all non-playback service functionality. Like auth stuff.
 */
public class TomahawkService extends Service {

    // Used for debug logging
    private static final String TAG = TomahawkService.class.getName();

    public static final int AUTHENTICATOR_ID_SPOTIFY = 0;

    public static final int AUTHENTICATOR_ID_HATCHET = 1;

    // After this time we will check if this service can be killed
    private static final int DELAY_TO_KILL = 300000;

    private boolean mHasBoundServices;

    private final IBinder mBinder = new TomahawkServiceBinder();

    private TomahawkServiceBroadcastReceiver mTomahawkServiceBroadcastReceiver;

    private WifiManager.WifiLock mWifiLock;

    private SharedPreferences mSharedPreferences;

    private SparseArray<Authenticator> mAuthenticators = new SparseArray<Authenticator>();

    private OnLoggedInOutListener mOnLoggedInOutListener;

    public interface OnLoggedInOutListener {

        void onLoggedInOut(int authenticatorId, boolean loggedIn);

    }

    public interface AuthenticatorListener {

        void onInit();

        void onLogin(String username);

        void onLoginFailed(String message);

        void onLogout();

        void onCredBlobUpdated(String blob);
    }

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

    private class TomahawkServiceBroadcastReceiver extends BroadcastReceiver {

        /**
         * Set the spotify preferred bitrate to high if we're connected to wifi, otherwise set
         * bitrate to user-preferred setting, if none set yet, default to medium
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())
                    && mAuthenticators.get(AUTHENTICATOR_ID_SPOTIFY) != null) {
                ConnectivityManager conMan = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = conMan.getActiveNetworkInfo();
                if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    Log.d("WifiReceiver", "Have Wifi Connection");
                    ((SpotifyAuthenticator) mAuthenticators.get(AUTHENTICATOR_ID_SPOTIFY))
                            .setBitrate(SpotifyAuthenticator.SPOTIFY_PREF_BITRATE_MODE_HIGH);
                } else {
                    Log.d("WifiReceiver", "Don't have Wifi Connection");
                    int prefbitrate = mSharedPreferences.getInt(
                            SpotifyAuthenticator.SPOTIFY_PREF_BITRATE,
                            SpotifyAuthenticator.SPOTIFY_PREF_BITRATE_MODE_MEDIUM);
                    ((SpotifyAuthenticator) mAuthenticators.get(AUTHENTICATOR_ID_SPOTIFY))
                            .setBitrate(prefbitrate);
                }
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());

        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
        mWifiLock.acquire();

        mAuthenticators.put(AUTHENTICATOR_ID_SPOTIFY,
                new SpotifyAuthenticator((TomahawkApp) getApplication(), this));

        // Start our killtimer (watchdog-style)
        mKillTimerHandler.removeCallbacksAndMessages(null);
        Message msg = mKillTimerHandler.obtainMessage();
        mKillTimerHandler.sendMessageDelayed(msg, DELAY_TO_KILL);

        mTomahawkServiceBroadcastReceiver = new TomahawkServiceBroadcastReceiver();
        registerReceiver(mTomahawkServiceBroadcastReceiver,
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
        unregisterReceiver(mTomahawkServiceBroadcastReceiver);
    }

    /**
     * Authenticators should callback here, if they logged in or out
     */
    public void onLoggedInOut(int authenticatorId, boolean loggedIn) {
        if (mOnLoggedInOutListener != null) {
            mOnLoggedInOutListener.onLoggedInOut(authenticatorId, loggedIn);
        }
    }

    public Authenticator getAuthenticator(int authenticatorId) {
        return mAuthenticators.get(authenticatorId);
    }

    public boolean isAuthenticating() {
        for (int i = 0; i < mAuthenticators.size(); i++) {
            if (mAuthenticators.valueAt(i).isAuthenticating()) {
                return true;
            }
        }
        return false;
    }

    public void setOnLoggedInOutListener(OnLoggedInOutListener onLoggedInOutListener) {
        mOnLoggedInOutListener = onLoggedInOutListener;
    }

}
