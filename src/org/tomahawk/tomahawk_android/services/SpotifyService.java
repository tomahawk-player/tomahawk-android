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
package org.tomahawk.tomahawk_android.services;

import org.codehaus.jackson.map.ObjectMapper;
import org.tomahawk.libtomahawk.infosystem.InfoSystemUtils;
import org.tomahawk.libtomahawk.resolver.spotify.LibSpotifyWrapper;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyLogin;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyQuery;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyResult;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyResults;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyServiceUtils;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This service wraps all non-playback service functionality. Like auth stuff.
 */
public class SpotifyService extends Service {

    // Used for debug logging
    private static final String TAG = SpotifyService.class.getName();

    public static final String REQUEST_SPOTIFYSERVICE
            = "org.tomahawk.tomahawk_android.request_spotifyservice";

    public static final String STRING_KEY = "org.tomahawk.tomahawk_android.string_key";

    /**
     * Message ids for messages _to_ libspotify
     */
    public static final int MSG_REGISTERCLIENT = 0;

    public static final int MSG_INIT = 1;

    public static final int MSG_DESTROY = 2;

    public static final int MSG_LOGIN = 3;

    public static final int MSG_LOGOUT = 4;

    public static final int MSG_RESOLVE = 5;

    public static final int MSG_PREPARE = 6;

    public static final int MSG_PLAY = 7;

    public static final int MSG_PAUSE = 8;

    public static final int MSG_SEEK = 9;

    public static final int MSG_SETBITRATE = 10;

    /**
     * Message ids for messages _from_ libspotify
     */
    public static final int MSG_ONRESOLVED = 100;

    public static final int MSG_ONPREPARED = 101;

    public static final int MSG_ONINIT = 102;

    public static final int MSG_ONLOGIN = 103;

    public static final int MSG_ONLOGINFAILED = 104;

    public static final int MSG_ONLOGOUT = 105;

    public static final int MSG_ONCREDBLOBUPDATED = 106;

    public static final int MSG_ONPLAYERENDOFTRACK = 107;

    public static final int MSG_ONPLAYERPOSITIONCHANGED = 108;

    private ArrayList<Messenger> mFromSpotifyMessengers = new ArrayList<Messenger>();

    private final Messenger mToSpotifyMessenger = new Messenger(new ToSpotifyHandler());

    private WifiManager.WifiLock mWifiLock;

    private ObjectMapper mObjectMapper = InfoSystemUtils.constructObjectMapper();

    /**
     * Handler of incoming messages from clients.
     */
    private class ToSpotifyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case MSG_REGISTERCLIENT:
                        mFromSpotifyMessengers.add(msg.replyTo);
                        break;
                    case MSG_INIT:
                        init(LibSpotifyWrapper.class.getClassLoader(), getFilesDir() + "/Spotify");
                        break;
                    case MSG_DESTROY:
                        destroy();
                        break;
                    case MSG_LOGIN:
                        SpotifyLogin spotifyLogin = mObjectMapper
                                .readValue(msg.getData().getString(SpotifyService.STRING_KEY),
                                        SpotifyLogin.class);
                        login(spotifyLogin.username, spotifyLogin.password, spotifyLogin.blob);
                        break;
                    case MSG_LOGOUT:
                        logout();
                        break;
                    case MSG_RESOLVE:
                        SpotifyQuery spotifyQuery = mObjectMapper
                                .readValue(msg.getData().getString(SpotifyService.STRING_KEY),
                                        SpotifyQuery.class);
                        resolve(spotifyQuery.queryKey, spotifyQuery.queryString);
                        break;
                    case MSG_PREPARE:
                        prepare(msg.getData().getString(SpotifyService.STRING_KEY));
                        break;
                    case MSG_PLAY:
                        play();
                        break;
                    case MSG_PAUSE:
                        pause();
                        break;
                    case MSG_SEEK:
                        seek(msg.arg1);
                        break;
                    case MSG_SETBITRATE:
                        seek(msg.arg1);
                        break;
                    default:
                        super.handleMessage(msg);
                }
            } catch (IOException e) {
                Log.e(TAG, "handleMessage: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
    }

    public static class SpotifyServiceConnection implements ServiceConnection {

        private SpotifyServiceConnectionListener mSpotifyServiceConnectionListener;

        public interface SpotifyServiceConnectionListener {

            public void setToSpotifyMessenger(Messenger messenger);
        }

        public SpotifyServiceConnection(SpotifyServiceConnectionListener listener) {
            mSpotifyServiceConnectionListener = listener;
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mSpotifyServiceConnectionListener.setToSpotifyMessenger(new Messenger(service));
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mSpotifyServiceConnectionListener.setToSpotifyMessenger(null);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Load the LibSpotifyWrapper libaries
        System.loadLibrary("spotify");
        System.loadLibrary("spotifywrapper");

        LibSpotifyWrapper.setSpotifyService(this);

        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
        mWifiLock.acquire();
        Log.d(TAG, "SpotifyService has been created");
    }

    @Override
    public int onStartCommand(Intent i, int j, int k) {
        super.onStartCommand(i, j, k);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mWifiLock.release();
        Log.d(TAG, "SpotifyService has been destroyed");
        LibSpotifyWrapper.nativedestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Client has been bound to SpotifyService");
        return mToSpotifyMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Client has been unbound from SpotifyService");
        stopSelf();
        return false;
    }

    private void sendMsg(int msg) {
        for (int i = mFromSpotifyMessengers.size() - 1; i >= 0; i--) {
            SpotifyServiceUtils.sendMsg(mFromSpotifyMessengers.get(i), msg);
        }
    }

    private void sendMsg(int msg, String value) {
        for (int i = mFromSpotifyMessengers.size() - 1; i >= 0; i--) {
            SpotifyServiceUtils.sendMsg(mFromSpotifyMessengers.get(i), msg, value);
        }
    }

    private void sendMsg(int msg, int value) {
        for (int i = mFromSpotifyMessengers.size() - 1; i >= 0; i--) {
            SpotifyServiceUtils.sendMsg(mFromSpotifyMessengers.get(i), msg, value);
        }
    }

    /**
     * Initialize libspotify
     *
     * @param loader      {@link ClassLoader} needed to initialize libspotify
     * @param storagePath {@link String} containing the path to where libspotify stores its stuff
     */
    public void init(ClassLoader loader, String storagePath) {
        LibSpotifyWrapper.nativeinit(loader, storagePath);
    }

    /**
     * Destroy libspotify session
     */
    public void destroy() {
        LibSpotifyWrapper.nativedestroy();
    }

    /**
     * Use loginUser(...) instead, if you want a proper callback. Login Spotify account. Does only
     * need blob OR password. Not both
     *
     * @param username {@link String} containing the username
     * @param password {@link String} containing the password
     * @param blob     {@link String} containing the blob
     */
    private void login(String username, String password, String blob) {
        if (password == null) {
            password = "";
        }
        if (blob == null) {
            blob = "";
        }
        LibSpotifyWrapper.nativelogin(username, password, blob);
    }

    /**
     * Use reloginUser(...) instead, if you want a proper callback. Relogin, in case session
     * expired
     */
    private void relogin() {
        LibSpotifyWrapper.nativerelogin();
    }

    /**
     * Use logoutUser(...) instead, if you want a proper callback. Logout Spotify account
     */
    private void logout() {
        LibSpotifyWrapper.nativelogout();
    }

    /**
     * Resolve a {@link org.tomahawk.libtomahawk.resolver.Query} via libspotify
     *
     * @param queryKey    the key of the given query
     * @param queryString {@link String} used to fetch results
     */
    private void resolve(String queryKey, String queryString) {
        LibSpotifyWrapper.nativeresolve(queryKey, queryString);
    }

    /**
     * Prepare a track via our native OpenSLES layer
     *
     * @param uri {@link String} containing the previously resolved Spotify URI
     */
    private void prepare(String uri) {
        LibSpotifyWrapper.nativeprepare(uri);
    }

    /**
     * Start playing the previously prepared track via our native OpenSLES layer
     */
    public void play() {
        LibSpotifyWrapper.nativeplay();

    }

    /**
     * Pause playing the previously prepared track via our native OpenSLES layer
     */
    public void pause() {
        LibSpotifyWrapper.nativepause();
    }

    /**
     * Seek to position (in ms) in the currently prepared track via our native OpenSLES layer
     *
     * @param position position to be seeked to (in ms)
     */
    public void seek(int position) {
        LibSpotifyWrapper.nativeseek(position);
    }

    /**
     * Star the current track within Spotify
     */
    public void star() {
        LibSpotifyWrapper.nativestar();
    }

    /**
     * Unstar the current track within Spotify
     */
    public void unstar() {
        LibSpotifyWrapper.nativeunstar();
    }

    /**
     * Set the preferred bitrate mode
     *
     * @param bitratemode int containing values '1'(96 kbit/s), '2'(160 kbit/s) or '3'(320 kbit/s)
     */
    public void setbitrate(int bitratemode) {
        LibSpotifyWrapper.nativesetbitrate(bitratemode);
    }

    /**
     * Called by libspotify, when a track has been prepared
     */
    public void onPrepared() {
        sendMsg(MSG_ONPREPARED);
    }

    /**
     * Called by libspotify, when a {@link org.tomahawk.libtomahawk.resolver.Query} has been
     * resolved
     *
     * @param qid        {@link String} containing the {@link org.tomahawk.libtomahawk.resolver.Query}'s
     *                   id
     * @param success    boolean signaling whether or not the {@link org.tomahawk.libtomahawk.resolver.Query}
     *                   has been successfully resolved
     * @param message    {@link String} containing libspotify's message
     * @param didYouMean {@link String} containing libspotify's "Did you mean?" suggestion
     */
    public void onResolved(final String qid, final boolean success, final String message,
            final String didYouMean, final int count, final int[] trackDurations,
            final int[] trackDiscnumbers, final int[] trackIndexes, final int[] albumYears,
            final String[] trackNames, final String[] trackUris, final String[] albumNames,
            final String[] artistNames) {
        ArrayList<SpotifyResult> results = new ArrayList<SpotifyResult>();
        for (int i = 0; i < count; i++) {
            SpotifyResult result = new SpotifyResult();
            result.trackDuration = trackDurations[i];
            result.trackDiscnumber = trackDiscnumbers[i];
            result.trackIndex = trackIndexes[i];
            result.albumYear = albumYears[i];
            result.trackName = trackNames[i];
            result.trackUri = trackUris[i];
            result.albumName = albumNames[i];
            result.artistName = artistNames[i];
            results.add(result);
        }
        SpotifyResults spotifyResults = new SpotifyResults();
        spotifyResults.qid = qid;
        spotifyResults.success = success;
        spotifyResults.message = message;
        spotifyResults.didYouMean = didYouMean;
        spotifyResults.count = count;
        spotifyResults.results = results;
        try {
            String jsonString = mObjectMapper.writeValueAsString(spotifyResults);
            sendMsg(MSG_ONRESOLVED, jsonString);
        } catch (IOException e) {
            Log.e(TAG, "onResolved: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    /**
     * Called by libspotify when initialized
     */
    public void onInit() {
        sendMsg(MSG_ONINIT);
    }

    /**
     * Called by libspotify on login
     *
     * @param success  boolean signaling whether or not the login process was successful
     * @param message  {@link String} containing libspotify's message
     * @param username {@link String} containing the username, which has been used to login to
     *                 Spotify
     */
    public void onLogin(final boolean success, final String message, final String username) {
        if (success) {
            sendMsg(MSG_ONLOGIN, username);
        } else {
            sendMsg(MSG_ONLOGINFAILED, message);
        }
    }

    /**
     * Called by libspotify on logout
     */
    public void onLogout() {
        sendMsg(MSG_ONLOGOUT);
    }

    /**
     * Called by libspotify, when the credential blob has been updated
     *
     * @param username {@link String} containing the username
     * @param blob     {@link String} containing the blob
     */
    public void onCredentialsBlobUpdated(final String username, final String blob) {
        SpotifyLogin spotifyLogin = new SpotifyLogin();
        spotifyLogin.username = username;
        spotifyLogin.blob = blob;
        try {
            String jsonString = mObjectMapper.writeValueAsString(spotifyLogin);
            sendMsg(MSG_ONCREDBLOBUPDATED, jsonString);
        } catch (IOException e) {
            Log.e(TAG,
                    "onCredentialsBlobUpdated: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    /**
     * Called by libspotify, when the OpenSLES player has finished playing a track
     */
    public void onPlayerEndOfTrack() {
        sendMsg(MSG_ONPLAYERENDOFTRACK);
    }

    /**
     * Called by libspotify, when the OpenSLES player signals that the current position has changed
     */
    public void onPlayerPositionChanged(final int position) {
        sendMsg(MSG_ONPLAYERPOSITIONCHANGED, position);
    }
}
