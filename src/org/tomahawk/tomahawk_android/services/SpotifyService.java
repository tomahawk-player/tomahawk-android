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

import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.playback.Config;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.PlaybackBitrate;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.spotify.sdk.android.playback.PlayerState;
import com.spotify.sdk.android.playback.PlayerStateCallback;

import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.RejectedExecutionException;

public class SpotifyService extends Service implements
        PlayerNotificationCallback, ConnectionStateCallback {

    // Used for debug logging
    private static final String TAG = SpotifyService.class.getSimpleName();

    public static final String CLIENT_ID = "d3e9c989687c496ab2f92c9e84f779dc";

    public static final String REQUEST_SPOTIFYSERVICE
            = "org.tomahawk.tomahawk_android.request_spotifyservice";

    public static final String STRING_KEY = "org.tomahawk.tomahawk_android.string_key";

    public static final String STRING_KEY2 = "org.tomahawk.tomahawk_android.string_key2";

    private static final int MSG_UPDATE_PROGRESS = 0x1;

    private static final long MSG_UPDATE_PROGRESS_INTERVAL = 1000;

    /**
     * Message ids for messages _to_ libspotify
     */
    public static final int MSG_REGISTERCLIENT = 0;

    public static final int MSG_PREPARE = 3;

    public static final int MSG_PLAY = 4;

    public static final int MSG_PAUSE = 5;

    public static final int MSG_SEEK = 6;

    public static final int MSG_SETBITRATE = 7;

    /**
     * Message ids for messages _from_ libspotify
     */
    public static final int MSG_ONPREPARED = 100;

    public static final int MSG_ONPLAYERENDOFTRACK = 104;

    public static final int MSG_ONPLAYERPOSITIONCHANGED = 105;

    private WifiManager.WifiLock mWifiLock;

    private Player mPlayer;

    private String mAccessToken;

    private String mPreparedUri;

    private ReportPositionHandler mReportPositionHandler = new ReportPositionHandler(this);

    private static class ReportPositionHandler extends WeakReferenceHandler<SpotifyService> {

        public ReportPositionHandler(SpotifyService spotifyService) {
            super(spotifyService);
        }

        @Override
        public void handleMessage(Message msg) {
            SpotifyService service = getReferencedObject();
            if (service != null) {
                switch (msg.what) {
                    case MSG_UPDATE_PROGRESS:
                        try {
                            service.mPlayer.getPlayerState(new PlayerStateCallback() {
                                @Override
                                public void onPlayerState(PlayerState playerState) {
                                    SpotifyService service = getReferencedObject();
                                    if (service != null) {
                                        service.sendMsg(MSG_ONPLAYERPOSITIONCHANGED,
                                                playerState.positionInMs);
                                    }
                                    sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS,
                                            MSG_UPDATE_PROGRESS_INTERVAL);
                                }
                            });
                        } catch (RejectedExecutionException e) {
                            Log.e(TAG, "handleMessage - " + e.getLocalizedMessage());
                        }
                        break;
                }
            }
        }
    }

    /**
     * Handler of incoming messages from clients.
     */
    private ToSpotifyHandler mToSpotifyHandler = new ToSpotifyHandler(this);

    private static class ToSpotifyHandler extends WeakReferenceHandler<SpotifyService> {

        public ToSpotifyHandler(SpotifyService spotifyService) {
            super(spotifyService);
        }

        @Override
        public void handleMessage(Message msg) {
            final SpotifyService service = getReferencedObject();
            if (service != null) {
                switch (msg.what) {
                    case MSG_REGISTERCLIENT:
                        service.mFromSpotifyMessengers.add(msg.replyTo);
                        break;
                    case MSG_PREPARE:
                        String newToken = msg.getData().getString(STRING_KEY2);
                        if (newToken != null && !newToken.equals(service.mAccessToken)) {
                            service.mAccessToken = newToken;
                            service.initializePlayer();
                        }
                        service.prepare(msg.getData().getString(SpotifyService.STRING_KEY));
                        break;
                    case MSG_PLAY:
                        service.play();
                        break;
                    case MSG_PAUSE:
                        service.pause();
                        break;
                    case MSG_SEEK:
                        service.seek(msg.arg1);
                        break;
                    case MSG_SETBITRATE:
                        service.setBitrate(msg.arg1);
                        break;
                }
            }
        }
    }

    private ArrayList<Messenger> mFromSpotifyMessengers = new ArrayList<>();

    private final Messenger mToSpotifyMessenger = new Messenger(mToSpotifyHandler);

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

        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
        mWifiLock.acquire();
        Log.d(TAG, "SpotifyService has been created");
    }

    @Override
    public void onDestroy() {
        Spotify.destroyPlayer(this);
        mWifiLock.release();
        Log.d(TAG, "SpotifyService has been destroyed");

        super.onDestroy();
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
            try {
                mFromSpotifyMessengers.get(i).send(Message.obtain(null, msg));
            } catch (RemoteException e) {
                Log.e(TAG, "sendMsg: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
    }

    private void sendMsg(int msg, String value) {
        for (int i = mFromSpotifyMessengers.size() - 1; i >= 0; i--) {
            try {
                Bundle bundle = new Bundle();
                bundle.putString(SpotifyService.STRING_KEY, value);
                Message message = Message.obtain(null, msg);
                message.setData(bundle);
                mFromSpotifyMessengers.get(i).send(message);
            } catch (RemoteException e) {
                Log.e(TAG, "sendMsg: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
    }

    private void sendMsg(int msg, int value) {
        for (int i = mFromSpotifyMessengers.size() - 1; i >= 0; i--) {
            try {
                mFromSpotifyMessengers.get(i).send(Message.obtain(null, msg, value, 0));
            } catch (RemoteException e) {
                Log.e(TAG, "sendMsg: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
    }

    private void initializePlayer() {
        Config playerConfig = new Config(this, mAccessToken, CLIENT_ID);
        mPlayer = Spotify.getPlayer(playerConfig, this,
                new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        mPlayer.addConnectionStateCallback(SpotifyService.this);
                        mPlayer.addPlayerNotificationCallback(SpotifyService.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e(TAG, "Could not initialize player: " + throwable
                                .getMessage());
                    }
                });
    }

    /**
     * Prepare a track via our native OpenSLES layer
     *
     * @param uri {@link String} containing the previously resolved Spotify URI
     */
    private void prepare(String uri) {
        mPreparedUri = uri;
        sendMsg(MSG_ONPREPARED);
    }

    /**
     * Start playing the previously prepared track via our native OpenSLES layer
     */
    public void play() {
        if (mPlayer != null) {
            try {
                mPlayer.getPlayerState(new PlayerStateCallback() {
                    @Override
                    public void onPlayerState(PlayerState playerState) {
                        mReportPositionHandler.sendEmptyMessage(MSG_UPDATE_PROGRESS);
                        if (!playerState.trackUri.equals(mPreparedUri)) {
                            mPlayer.play(mPreparedUri);
                        } else if (!playerState.playing) {
                            mPlayer.resume();
                        }
                    }
                });
            } catch (RejectedExecutionException e) {
                Log.e(TAG, "play - " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Pause playing the previously prepared track via our native OpenSLES layer
     */
    public void pause() {
        if (mPlayer != null) {
            try {
                mPlayer.getPlayerState(new PlayerStateCallback() {
                    @Override
                    public void onPlayerState(PlayerState playerState) {
                        mReportPositionHandler.removeCallbacksAndMessages(null);
                        if (playerState.playing) {
                            mPlayer.pause();
                        }
                    }
                });
            } catch (RejectedExecutionException e) {
                Log.e(TAG, "pause - " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Seek to position (in ms) in the currently prepared track via our native OpenSLES layer
     *
     * @param position position to be seeked to (in ms)
     */
    public void seek(int position) {
        if (mPlayer != null) {
            mPlayer.seekToPosition(position);
        }
    }

    /**
     * Set the preferred bitrate mode
     *
     * @param bitratemode int containing values '0'(96 kbit/s), '1'(160 kbit/s) or '2'(320 kbit/s)
     */
    public void setBitrate(int bitratemode) {
        if (mPlayer != null) {
            PlaybackBitrate bitrate = null;
            switch (bitratemode) {
                case 0:
                    bitrate = PlaybackBitrate.BITRATE_LOW;
                    break;
                case 1:
                    bitrate = PlaybackBitrate.BITRATE_NORMAL;
                    break;
                case 2:
                    bitrate = PlaybackBitrate.BITRATE_HIGH;
                    break;
            }
            if (bitrate != null) {
                mPlayer.setPlaybackBitrate(bitrate);
            } else {
                Log.d(TAG, "Invalid bitratemode given");
            }
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d(TAG, "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d(TAG, "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d(TAG, "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d(TAG, "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d(TAG, "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d(TAG, "Playback event received: " + eventType.name());
        if (playerState.trackUri.equals(mPreparedUri)
                && EventType.TRACK_END.equals(eventType)) {
            sendMsg(MSG_ONPLAYERENDOFTRACK);
        }
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d(TAG, "Playback error received: " + errorType.name());
    }
}
