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

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.SpotifyAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyResolver;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyServiceUtils;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment;
import org.tomahawk.tomahawk_android.utils.SpotifyMediaPlayer;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkMediaPlayer;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This {@link Service} handles all playback related processes.
 */
public class PlaybackService extends Service
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private static String TAG = PlaybackService.class.getName();

    public static final String BROADCAST_CURRENTTRACKCHANGED
            = "org.tomahawk.tomahawk_android.BROADCAST_CURRENTTRACKCHANGED";

    public static final String BROADCAST_PLAYLISTCHANGED
            = "org.tomahawk.tomahawk_android.BROADCAST_PLAYLISTCHANGED";

    public static final String BROADCAST_PLAYSTATECHANGED
            = "org.tomahawk.tomahawk_android.BROADCAST_PLAYSTATECHANGED";

    public static final String BROADCAST_NOTIFICATIONINTENT_PREVIOUS
            = "org.tomahawk.tomahawk_android.BROADCAST_NOTIFICATIONINTENT_PREVIOUS";

    public static final String BROADCAST_NOTIFICATIONINTENT_PLAYPAUSE
            = "org.tomahawk.tomahawk_android.BROADCAST_NOTIFICATIONINTENT_PLAYPAUSE";

    public static final String BROADCAST_NOTIFICATIONINTENT_NEXT
            = "org.tomahawk.tomahawk_android.BROADCAST_NOTIFICATIONINTENT_NEXT";

    public static final String BROADCAST_NOTIFICATIONINTENT_EXIT
            = "org.tomahawk.tomahawk_android.BROADCAST_NOTIFICATIONINTENT_EXIT";

    private static final int PLAYBACKSERVICE_PLAYSTATE_PLAYING = 0;

    private static final int PLAYBACKSERVICE_PLAYSTATE_PAUSED = 1;

    private int mPlayState = PLAYBACKSERVICE_PLAYSTATE_PLAYING;

    private static final int PLAYBACKSERVICE_NOTIFICATION_ID = 1;

    private static final int DELAY_TO_KILL = 300000;

    private boolean mIsRunningInForeground;

    protected HashSet<String> mCorrespondingQueryKeys = new HashSet<String>();

    protected ConcurrentHashMap<String, String> mCorrespondingInfoDataIds
            = new ConcurrentHashMap<String, String>();

    private Playlist mCurrentPlaylist;

    private PowerManager.WakeLock mWakeLock;

    private PlaybackServiceBroadcastReceiver mPlaybackServiceBroadcastReceiver;

    private Bitmap mNotificationBitmap = null;

    private Image mNotificationBitmapImage = null;

    private boolean mIsBindingToSpotifyService;

    private SpotifyService.SpotifyServiceConnection mSpotifyServiceConnection
            = new SpotifyService.SpotifyServiceConnection(new SpotifyServiceConnectionListener());

    private Target mTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            mNotificationBitmap = resizeNotificationBitmap(getResources(), bitmap);
            if (mIsRunningInForeground) {
                updatePlayingNotification();
            }
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {

        }

        @Override
        public void onPrepareLoad(Drawable drawable) {

        }
    };

    /**
     * The static {@link ServiceConnection} which calls methods in {@link
     * PlaybackServiceConnectionListener} to let every depending object know, if the {@link
     * PlaybackService} connects or disconnects.
     */
    public static class PlaybackServiceConnection implements ServiceConnection {

        private PlaybackServiceConnectionListener mPlaybackServiceConnectionListener;

        public interface PlaybackServiceConnectionListener {

            public void setPlaybackService(PlaybackService ps);

            public void onPlaybackServiceReady();
        }

        public PlaybackServiceConnection(
                PlaybackServiceConnectionListener playbackServiceConnectedListener) {
            mPlaybackServiceConnectionListener = playbackServiceConnectedListener;
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            PlaybackServiceBinder binder = (PlaybackServiceBinder) service;
            mPlaybackServiceConnectionListener.setPlaybackService(binder.getService());
            mPlaybackServiceConnectionListener.onPlaybackServiceReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mPlaybackServiceConnectionListener.setPlaybackService(null);
        }
    }

    private PhoneCallListener mPhoneCallListener = new PhoneCallListener();

    /**
     * Listens for incoming phone calls and handles playback.
     */
    private class PhoneCallListener extends PhoneStateListener {

        private long mStartCallTime = 0L;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (isPlaying()) {
                        mStartCallTime = System.currentTimeMillis();
                        pause();
                    }
                    break;

                case TelephonyManager.CALL_STATE_IDLE:
                    if (mStartCallTime > 0 && (System.currentTimeMillis() - mStartCallTime
                            < 30000)) {
                        start();
                    }

                    mStartCallTime = 0L;
                    break;
            }
        }
    }

    private class SpotifyServiceConnectionListener
            implements SpotifyService.SpotifyServiceConnection.SpotifyServiceConnectionListener {

        @Override
        public void setToSpotifyMessenger(Messenger messenger) {
            SpotifyResolver spotifyResolver = (SpotifyResolver) PipeLine.getInstance()
                    .getResolver(PipeLine.RESOLVER_ID_SPOTIFY);
            spotifyResolver.setToSpotifyMessenger(messenger);
            SpotifyAuthenticatorUtils authUtils = (SpotifyAuthenticatorUtils)
                    AuthenticatorManager.getInstance()
                            .getAuthenticatorUtils(AuthenticatorManager.AUTHENTICATOR_ID_SPOTIFY);
            authUtils.setToSpotifyMessenger(messenger);
            SpotifyMediaPlayer spotifyMediaPlayer = SpotifyMediaPlayer.getInstance();
            spotifyMediaPlayer.setToSpotifyMessenger(messenger);
            //Now that every client has its messenger reference and registered itself to the
            //SpotifyService's messenger, we can initialize libspotify through the wrapper
            if (messenger != null) {
                SpotifyServiceUtils.sendMsg(messenger, SpotifyService.MSG_INIT);
            }
            mIsBindingToSpotifyService = false;
        }
    }

    /**
     * Handles incoming broadcasts
     */
    private class PlaybackServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // AudioManager tells us that the sound will be played through the speaker
                if (isPlaying()) {
                    // So we stop playback, if needed
                    pause();
                }
            } else if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction()) && intent
                    .hasExtra("state") && intent.getIntExtra("state", 0) == 1) {
                // Headset has been plugged in
                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                boolean playbackOnHeadsetInsert = prefs
                        .getBoolean(FakePreferenceFragment.FAKEPREFERENCEFRAGMENT_KEY_PLUGINTOPLAY,
                                false);

                if (!isPlaying() && playbackOnHeadsetInsert) {
                    //resume playback, if user has set the "resume on headset plugin" preference
                    start();
                }
            } else if (PipeLine.PIPELINE_RESULTSREPORTED.equals(intent.getAction())) {
                String qid = intent.getStringExtra(PipeLine.PIPELINE_RESULTSREPORTED_QUERYKEY);
                onPipeLineResultsReported(qid);
            } else if (InfoSystem.INFOSYSTEM_RESULTSREPORTED.equals(intent.getAction())) {
                String requestId = intent
                        .getStringExtra(InfoSystem.INFOSYSTEM_RESULTSREPORTED_REQUESTID);
                onInfoSystemResultsReported(requestId);
            } else if (SpotifyService.REQUEST_SPOTIFYSERVICE.equals(intent.getAction())) {
                if (!mIsBindingToSpotifyService) {
                    Log.d(TAG, "SpotifyService has been requested, I'm trying to bind to it ...");
                    mIsBindingToSpotifyService = true;
                    bindService(new Intent(PlaybackService.this, SpotifyService.class),
                            mSpotifyServiceConnection, Context.BIND_AUTO_CREATE);
                }
            }
        }
    }

    public class PlaybackServiceBinder extends Binder {

        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    // Stops this service if it doesn't have any bound services
    private Handler mKillTimerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (isPlaying()) {
                mKillTimerHandler.removeCallbacksAndMessages(null);
                Message msgx = mKillTimerHandler.obtainMessage();
                mKillTimerHandler.sendMessageDelayed(msgx, DELAY_TO_KILL);
                Log.d(TAG, "Killtimer checked if I should die, but I survived *cheer*");
            } else {
                Log.d(TAG, "Killtimer called stopSelf() on me");
                stopSelf();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        DatabaseHelper.getInstance().ensureInit();
        PipeLine.getInstance().ensureInit();
        InfoSystem.getInstance().ensureInit();
        AuthenticatorManager.getInstance().ensureInit();
        UserCollection.getInstance().ensureInit();

        bindService(new Intent(this, SpotifyService.class), mSpotifyServiceConnection,
                Context.BIND_AUTO_CREATE);

        // Initialize PhoneCallListener
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(
                Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneCallListener, PhoneStateListener.LISTEN_CALL_STATE);
        telephonyManager
                .listen(mPhoneCallListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

        // Initialize WakeLock
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        // Initialize and register PlaybackServiceBroadcastReceiver
        mPlaybackServiceBroadcastReceiver = new PlaybackServiceBroadcastReceiver();
        registerReceiver(mPlaybackServiceBroadcastReceiver,
                new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        registerReceiver(mPlaybackServiceBroadcastReceiver,
                new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        registerReceiver(mPlaybackServiceBroadcastReceiver,
                new IntentFilter(PipeLine.PIPELINE_RESULTSREPORTED));
        registerReceiver(mPlaybackServiceBroadcastReceiver,
                new IntentFilter(InfoSystem.INFOSYSTEM_RESULTSREPORTED));
        registerReceiver(mPlaybackServiceBroadcastReceiver,
                new IntentFilter(SpotifyService.REQUEST_SPOTIFYSERVICE));

        // Initialize killtime handler (watchdog style)
        mKillTimerHandler.removeCallbacksAndMessages(null);
        Message msg = mKillTimerHandler.obtainMessage();
        mKillTimerHandler.sendMessageDelayed(msg, DELAY_TO_KILL);

        restoreState();
        Log.d(TAG, "PlaybackService has been created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(BROADCAST_NOTIFICATIONINTENT_PREVIOUS)) {
                previous();
            } else if (intent.getAction().equals(BROADCAST_NOTIFICATIONINTENT_PLAYPAUSE)) {
                playPause();
            } else if (intent.getAction().equals(BROADCAST_NOTIFICATIONINTENT_NEXT)) {
                next();
            } else if (intent.getAction().equals(BROADCAST_NOTIFICATIONINTENT_EXIT)) {
                pause(true);
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Client has been bound to PlaybackService");
        return new PlaybackServiceBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Client has been unbound from PlaybackService");
        return false;
    }

    @Override
    public void onDestroy() {
        pause(true);
        saveState();
        unregisterReceiver(mPlaybackServiceBroadcastReceiver);
        mPlaybackServiceBroadcastReceiver = null;
        unbindService(mSpotifyServiceConnection);
        mSpotifyServiceConnection = null;
        SpotifyMediaPlayer.getInstance().release();
        TomahawkMediaPlayer.getInstance().release();
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        mWakeLock = null;
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(
                Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneCallListener, PhoneStateListener.LISTEN_NONE);
        mPhoneCallListener = null;
        mTarget = null;
        mKillTimerHandler.removeCallbacksAndMessages(null);
        mKillTimerHandler = null;

        super.onDestroy();
        Log.d(TAG, "PlaybackService has been destroyed");
    }

    /**
     * Called if given {@link TomahawkMediaPlayer} has been prepared for playback
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "MediaPlayer successfully prepared the track '"
                + getCurrentQuery().getName() + "' by '"
                + getCurrentQuery().getArtist().getName()
                + "' resolved by Resolver " + getCurrentQuery()
                .getPreferredTrackResult().getResolvedBy());
        if (isPlaying()) {
            InfoSystem.getInstance().sendNowPlayingPostStruct(
                    AuthenticatorManager.getInstance()
                            .getAuthenticatorUtils(
                                    AuthenticatorManager.AUTHENTICATOR_ID_HATCHET),
                    getCurrentQuery()
            );
        }
        handlePlayState();
    }

    /**
     * Called if an error has occurred while trying to prepare {@link TomahawkMediaPlayer}
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        String whatString = "CODE UNSPECIFIED";
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                whatString = "MEDIA_ERROR_UNKNOWN";
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                whatString = "MEDIA_ERROR_SERVER_DIED";
        }

        Log.e(TAG, "onError - " + whatString);
        return false;
    }

    /**
     * Called if given {@link org.tomahawk.tomahawk_android.utils.TomahawkMediaPlayer} has finished
     * playing a song. Prepare the next track if possible.
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mCurrentPlaylist != null && mCurrentPlaylist.peekNextQuery() != null) {
            next();
        }
    }

    /**
     * Save the current playlist in the UserPlaylists Database
     */
    private void saveState() {
        if (getCurrentPlaylist() != null) {
            long startTime = System.currentTimeMillis();
            UserCollection userCollection = UserCollection.getInstance();
            userCollection.setCachedUserPlaylist(UserPlaylist
                    .fromQueryList(DatabaseHelper.CACHED_PLAYLIST_ID,
                            DatabaseHelper.CACHED_PLAYLIST_NAME,
                            getCurrentPlaylist().getQueries(),
                            getCurrentPlaylist().getCurrentQueryIndex()));
            Log.d(TAG, "Playlist stored in " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

    /**
     * Restore the current playlist from the UserPlaylists Database. Do this by storing it in the
     * {@link org.tomahawk.libtomahawk.collection.UserCollection} first, and then retrieving the
     * playlist from there.
     */
    private void restoreState() {
        long startTime = System.currentTimeMillis();
        UserCollection userCollection = UserCollection.getInstance();
        setCurrentPlaylist(userCollection.getCachedUserPlaylist());
        Log.d(TAG, "Playlist loaded in " + (System.currentTimeMillis() - startTime) + "ms");
        if (getCurrentPlaylist() != null && isPlaying()) {
            pause(true);
        }
    }

    /**
     * Start or pause playback (Doesn't dismiss notification on pause)
     */
    public void playPause() {
        playPause(false);
    }

    /**
     * Start or pause playback.
     *
     * @param dismissNotificationOnPause if true, dismiss notification on pause, otherwise don't
     */
    public void playPause(boolean dismissNotificationOnPause) {
        if (mPlayState == PLAYBACKSERVICE_PLAYSTATE_PLAYING) {
            pause(dismissNotificationOnPause);
        } else if (mPlayState == PLAYBACKSERVICE_PLAYSTATE_PAUSED) {
            start();
        }
    }

    /**
     * Initial start of playback. Acquires wakelock and creates a notification
     */
    public void start() {
        mPlayState = PLAYBACKSERVICE_PLAYSTATE_PLAYING;
        sendBroadcast(new Intent(BROADCAST_PLAYSTATECHANGED));
        handlePlayState();
        updatePlayingNotification();
    }

    /**
     * Pause playback. (Doesn't dismiss notification on pause)
     */
    public void pause() {
        pause(false);
    }

    /**
     * Pause playback.
     *
     * @param dismissNotificationOnPause if true, dismiss notification on pause, otherwise don't
     */
    public void pause(boolean dismissNotificationOnPause) {
        mPlayState = PLAYBACKSERVICE_PLAYSTATE_PAUSED;
        sendBroadcast(new Intent(BROADCAST_PLAYSTATECHANGED));
        handlePlayState();
        if (dismissNotificationOnPause) {
            mIsRunningInForeground = false;
            stopForeground(true);
        } else {
            updatePlayingNotification();
        }
    }

    /**
     * Update the TomahawkMediaPlayer so that it reflects the current playState
     */
    public void handlePlayState() {
        if (!isPreparing() && getCurrentQuery() != null
                && getCurrentQuery().getMediaPlayerInterface() != null) {
            try {
                switch (mPlayState) {
                    case PLAYBACKSERVICE_PLAYSTATE_PLAYING:
                        if (!mWakeLock.isHeld()) {
                            mWakeLock.acquire();
                        }
                        if (getCurrentQuery().getMediaPlayerInterface()
                                .isPrepared(getCurrentQuery())) {
                            if (!getCurrentQuery().getMediaPlayerInterface().isPlaying()) {
                                getCurrentQuery().getMediaPlayerInterface().start();
                            }
                        } else if (!isPreparing()) {
                            prepareCurrentQuery();
                        }
                        break;
                    case PLAYBACKSERVICE_PLAYSTATE_PAUSED:
                        if (getCurrentQuery().getMediaPlayerInterface().isPlaying()
                                && getCurrentQuery().getMediaPlayerInterface()
                                .isPrepared(getCurrentQuery())) {
                            InfoSystem.getInstance().sendPlaybackEntryPostStruct(
                                    AuthenticatorManager.getInstance().getAuthenticatorUtils(
                                            AuthenticatorManager.AUTHENTICATOR_ID_HATCHET)
                            );
                            getCurrentQuery().getMediaPlayerInterface().pause();
                        }
                        if (mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                        break;
                }
            } catch (IllegalStateException e1) {
                Log.e(TAG,
                        "handlePlayState() IllegalStateException, msg:" + e1.getLocalizedMessage()
                                + " , preparing=" + isPreparing()
                );
            }
            mKillTimerHandler.removeCallbacksAndMessages(null);
            Message msg = mKillTimerHandler.obtainMessage();
            mKillTimerHandler.sendMessageDelayed(msg, DELAY_TO_KILL);
        }
    }

    /**
     * Start playing the next Track.
     */
    public void next() {
        if (mCurrentPlaylist != null) {
            TomahawkMediaPlayer.getInstance().release();
            SpotifyMediaPlayer.getInstance().release();
            Query query = null;
            int maxCount = mCurrentPlaylist.getCount();
            int counter = 0;
            while (mCurrentPlaylist.hasNextQuery() && counter++ < maxCount && (query == null
                    || !query.isPlayable())) {
                query = mCurrentPlaylist.getNextQuery();
                sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
                onTrackChanged();
            }
            handlePlayState();
        }
    }

    /**
     * Play the previous track.
     */
    public void previous() {
        if (mCurrentPlaylist != null) {
            TomahawkMediaPlayer.getInstance().release();
            SpotifyMediaPlayer.getInstance().release();
            Query query = null;
            int maxCount = mCurrentPlaylist.getCount();
            int counter = 0;
            while (mCurrentPlaylist.hasPreviousQuery() && counter++ < maxCount && (query == null
                    || !query.isPlayable())) {
                query = mCurrentPlaylist.getPreviousQuery();
                sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
                onTrackChanged();
            }
            handlePlayState();
        }
    }

    /**
     * Set whether or not to enable shuffle mode on the current playlist.
     */
    public void setShuffled(boolean shuffled) {
        mCurrentPlaylist.setShuffled(shuffled);
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    /**
     * Set whether or not to enable repeat mode on the current playlist.
     */
    public void setRepeating(boolean repeating) {
        mCurrentPlaylist.setRepeating(repeating);
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    /**
     * Returns whether this PlaybackService is currently playing media.
     */
    public boolean isPlaying() {
        return mPlayState == PLAYBACKSERVICE_PLAYSTATE_PLAYING;
    }

    /**
     * @return Whether or not the mediaPlayer currently prepares a track
     */
    public boolean isPreparing() {
        return getCurrentQuery() != null && getCurrentQuery().getMediaPlayerInterface() != null
                && getCurrentQuery().getMediaPlayerInterface().isPreparing();
    }

    /**
     * Get the current Query
     */
    public Query getCurrentQuery() {
        if (mCurrentPlaylist == null) {
            return null;
        }
        return mCurrentPlaylist.getCurrentQuery();
    }

    /**
     * @return the currently playing Track item (contains all necessary meta-data)
     */
    public Track getCurrentTrack() {
        if (getCurrentQuery() != null) {
            return getCurrentQuery().getPreferredTrack();
        }
        return null;
    }

    /**
     * This method sets the current track and prepares it for playback.
     */
    private void prepareCurrentQuery() {
        if (getCurrentQuery() != null) {
            if (getCurrentQuery().isPlayable()) {
                mKillTimerHandler.removeCallbacksAndMessages(null);
                Message msg = mKillTimerHandler.obtainMessage();
                mKillTimerHandler.sendMessageDelayed(msg, DELAY_TO_KILL);

                if (mIsRunningInForeground) {
                    updatePlayingNotification();
                }
                sendBroadcast(new Intent(BROADCAST_CURRENTTRACKCHANGED));

                if (getCurrentQuery().getImage() == null) {
                    ArrayList<String> requestIds = InfoSystem.getInstance().resolve(
                            getCurrentQuery().getArtist(), true);
                    for (String requestId : requestIds) {
                        mCorrespondingInfoDataIds.put(requestId, getCurrentQuery().getCacheKey());
                    }
                    String requestId = InfoSystem.getInstance()
                            .resolve(getCurrentQuery().getAlbum());
                    if (requestId != null) {
                        mCorrespondingInfoDataIds.put(requestId, getCurrentQuery().getCacheKey());
                    }
                }

                TomahawkRunnable r = new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_PLAYBACK) {
                    @Override
                    public void run() {
                        if (isPlaying() && getCurrentQuery().getMediaPlayerInterface() != null) {
                            if (getCurrentQuery().getMediaPlayerInterface().prepare(
                                    PlaybackService.this, getCurrentQuery(), PlaybackService.this,
                                    PlaybackService.this, PlaybackService.this) == null) {
                                boolean isNetworkAvailable = isNetworkAvailable();
                                if (isNetworkAvailable
                                        && getCurrentQuery().getPreferredTrackResult() != null) {
                                    getCurrentQuery().blacklistTrackResult(
                                            getCurrentQuery().getPreferredTrackResult());
                                    sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
                                }
                                if (!isNetworkAvailable
                                        || getCurrentQuery().getPreferredTrackResult() == null) {
                                    Log.e(TAG,
                                            "MediaPlayer was unable to prepare the track, jumping to next track");
                                    next();
                                } else {
                                    Log.d(TAG,
                                            "MediaPlayer blacklisted a result and tries to prepare again");
                                    prepareCurrentQuery();
                                }
                            }
                        }
                    }
                };
                ThreadManager.getInstance().executePlayback(r);
            } else {
                next();
            }
        }
    }

    public void setCurrentQueryIndex(int queryIndex) {
        TomahawkMediaPlayer.getInstance().release();
        SpotifyMediaPlayer.getInstance().release();
        getCurrentPlaylist().setCurrentQueryIndex(queryIndex);
        handlePlayState();
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
        onTrackChanged();
    }

    private void onTrackChanged() {
        if (getCurrentQuery() != null) {
            resolveQueriesFromTo(getCurrentPlaylist().getCurrentQueryIndex(),
                    getCurrentPlaylist().getCurrentQueryIndex() + 10);
            if (mIsRunningInForeground) {
                updatePlayingNotification();
            }
        }
    }

    /**
     * @return whether or not wi-fi is available
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    /**
     * Get the current Playlist
     */
    public Playlist getCurrentPlaylist() {
        return mCurrentPlaylist;
    }

    /**
     * Set the current Playlist to playlist and set the current Track to the Playlist's current
     * Track.
     */
    public void setCurrentPlaylist(Playlist playlist) {
        TomahawkMediaPlayer.getInstance().release();
        SpotifyMediaPlayer.getInstance().release();
        mCurrentPlaylist = playlist;
        handlePlayState();
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
        onTrackChanged();
    }

    /**
     * Add given {@link ArrayList} of {@link org.tomahawk.libtomahawk.resolver.Query}s to the
     * current {@link Playlist}
     */
    public void addQueriesToCurrentPlaylist(ArrayList<Query> queries) {
        if (mCurrentPlaylist == null) {
            mCurrentPlaylist = UserPlaylist
                    .fromQueryList(DatabaseHelper.CACHED_PLAYLIST_ID,
                            DatabaseHelper.CACHED_PLAYLIST_NAME,
                            new ArrayList<Query>());
        }
        mCurrentPlaylist.addQueries(queries);
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    /**
     * Add given {@link ArrayList} of {@link Track}s to the current {@link Playlist} at the given
     * position
     */
    public void addQueriesToCurrentPlaylist(int position, ArrayList<Query> queries) {
        if (mCurrentPlaylist == null) {
            mCurrentPlaylist = UserPlaylist
                    .fromQueryList(DatabaseHelper.CACHED_PLAYLIST_ID,
                            DatabaseHelper.CACHED_PLAYLIST_NAME,
                            new ArrayList<Query>());
        }
        if (position < mCurrentPlaylist.getCount()) {
            mCurrentPlaylist.addQueries(position, queries);
        } else {
            mCurrentPlaylist.addQueries(queries);
        }
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    /**
     * Remove query at given position from current playlist
     */
    public void deleteQueryAtPos(int position) {
        mCurrentPlaylist.deleteQueryAtPos(position);
        if (mCurrentPlaylist.getCount() == 0) {
            pause(true);
        }
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    /**
     * Remove query at given position from current playlist
     */
    public void deleteQuery(Query query) {
        mCurrentPlaylist.deleteQuery(query);
        if (mCurrentPlaylist.getCount() == 0) {
            pause(true);
        }
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    /**
     * Returns the position of playback in the current Track.
     */
    public int getPosition() {
        int position = 0;
        if (getCurrentQuery() != null && getCurrentQuery().getMediaPlayerInterface() != null) {
            try {
                position = getCurrentQuery().getMediaPlayerInterface().getPosition();
            } catch (IllegalStateException e) {
                Log.e(TAG, "getPosition: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
        return position;
    }

    /**
     * Seeks to position msec
     */
    public void seekTo(int msec) {
        if (getCurrentQuery() != null && getCurrentQuery().getMediaPlayerInterface() != null
                && getCurrentQuery().getMediaPlayerInterface().isPrepared(getCurrentQuery())) {
            getCurrentQuery().getMediaPlayerInterface().seekTo(msec);
        }
    }

    /**
     * Create or update an ongoing notification
     */
    public void updatePlayingNotification() {
        Query query = getCurrentQuery();
        if (query == null) {
            return;
        }

        String albumName = "";
        String artistName = "";
        Image image = query.getImage();
        if (query.getAlbum() != null) {
            albumName = query.getAlbum().getName();
        }
        if (query.getArtist() != null) {
            artistName = query.getArtist().getName();
        }

        Intent intent = new Intent(BROADCAST_NOTIFICATIONINTENT_PREVIOUS, null, this,
                PlaybackService.class);
        PendingIntent previousPendingIntent = PendingIntent
                .getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        intent = new Intent(BROADCAST_NOTIFICATIONINTENT_PLAYPAUSE, null, this,
                PlaybackService.class);
        PendingIntent playPausePendingIntent = PendingIntent
                .getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        intent = new Intent(BROADCAST_NOTIFICATIONINTENT_NEXT, null, this, PlaybackService.class);
        PendingIntent nextPendingIntent = PendingIntent
                .getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        intent = new Intent(BROADCAST_NOTIFICATIONINTENT_EXIT, null, this, PlaybackService.class);
        PendingIntent exitPendingIntent = PendingIntent
                .getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteViews smallNotificationView;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            smallNotificationView = new RemoteViews(getPackageName(), R.layout.notification_small);
            if (mNotificationBitmap != null) {
                smallNotificationView.setImageViewBitmap(R.id.notification_small_imageview_albumart,
                        mNotificationBitmap);
            }
        } else {
            smallNotificationView = new RemoteViews(getPackageName(),
                    R.layout.notification_small_compat);
        }
        smallNotificationView.setTextViewText(R.id.notification_small_textview, query.getName());
        if (TextUtils.isEmpty(albumName)) {
            smallNotificationView.setTextViewText(R.id.notification_small_textview2, artistName);
        } else {
            smallNotificationView.setTextViewText(R.id.notification_small_textview2,
                    artistName + " - " + albumName);
        }
        if (isPlaying()) {
            smallNotificationView.setImageViewResource(R.id.notification_small_imageview_playpause,
                    R.drawable.ic_player_pause_light);
        } else {
            smallNotificationView.setImageViewResource(R.id.notification_small_imageview_playpause,
                    R.drawable.ic_player_play_light);
        }
        smallNotificationView.setOnClickPendingIntent(R.id.notification_small_imageview_playpause,
                playPausePendingIntent);
        smallNotificationView
                .setOnClickPendingIntent(R.id.notification_small_imageview_next, nextPendingIntent);
        smallNotificationView
                .setOnClickPendingIntent(R.id.notification_small_imageview_exit, exitPendingIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher).setContentTitle(artistName)
                .setContentText(query.getName()).setOngoing(true).setPriority(
                        NotificationCompat.PRIORITY_MAX).setContent(smallNotificationView);
        if (mNotificationBitmap != null) {
            builder.setLargeIcon(mNotificationBitmap);
        }
        if (mNotificationBitmap == null || image != mNotificationBitmapImage) {
            mNotificationBitmapImage = image;
            TomahawkUtils.loadImageIntoBitmap(this, image, mTarget, Image.IMAGE_SIZE_SMALL);
        }

        Intent notificationIntent = new Intent(this, TomahawkMainActivity.class);
        intent.setAction(TomahawkMainActivity.SHOW_PLAYBACKFRAGMENT_ON_STARTUP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent
                .getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        Notification notification = builder.build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            RemoteViews largeNotificationView = new RemoteViews(getPackageName(),
                    R.layout.notification_large);
            if (mNotificationBitmap != null) {
                largeNotificationView.setImageViewBitmap(R.id.notification_large_imageview_albumart,
                        mNotificationBitmap);
            }
            largeNotificationView.setTextViewText(R.id.notification_large_textview,
                    query.getName());
            largeNotificationView.setTextViewText(R.id.notification_large_textview2, artistName);
            largeNotificationView.setTextViewText(R.id.notification_large_textview3, albumName);
            if (isPlaying()) {
                largeNotificationView
                        .setImageViewResource(R.id.notification_large_imageview_playpause,
                                R.drawable.ic_player_pause_light);
            } else {
                largeNotificationView
                        .setImageViewResource(R.id.notification_large_imageview_playpause,
                                R.drawable.ic_player_play_light);
            }
            largeNotificationView
                    .setOnClickPendingIntent(R.id.notification_large_imageview_previous,
                            previousPendingIntent);
            largeNotificationView
                    .setOnClickPendingIntent(R.id.notification_large_imageview_playpause,
                            playPausePendingIntent);
            largeNotificationView.setOnClickPendingIntent(R.id.notification_large_imageview_next,
                    nextPendingIntent);
            largeNotificationView.setOnClickPendingIntent(R.id.notification_large_imageview_exit,
                    exitPendingIntent);
            notification.bigContentView = largeNotificationView;
        }
        mIsRunningInForeground = true;
        startForeground(PLAYBACKSERVICE_NOTIFICATION_ID, notification);
    }

    private static Bitmap resizeNotificationBitmap(Resources resources, Bitmap bitmap) {
        if (resources != null && bitmap != null) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float goalHeight = resources
                    .getDimension(android.R.dimen.notification_large_icon_height);
            float goalWidth = resources.getDimension(android.R.dimen.notification_large_icon_width);
            float scaleHeight = (float) height / goalHeight;
            float scaleWidth = (float) width / goalWidth;
            float scale;
            if (scaleWidth < scaleHeight) {
                scale = scaleHeight;
            } else {
                scale = scaleWidth;
            }
            return Bitmap.createScaledBitmap(bitmap, (int) (width / scale), (int) (height / scale),
                    true);
        }
        return null;
    }

    private void resolveQueriesFromTo(int start, int end) {
        ArrayList<Query> qs = new ArrayList<Query>();
        for (int i = start; i < end; i++) {
            if (i >= 0 && i < mCurrentPlaylist.getQueries().size()) {
                Query q = mCurrentPlaylist.peekQueryAtPos(i);
                if (!mCorrespondingQueryKeys.contains(q.getCacheKey())) {
                    qs.add(q);
                }
            }
        }
        if (!qs.isEmpty()) {
            HashSet<String> queryKeys = PipeLine.getInstance().resolve(qs);
            mCorrespondingQueryKeys.addAll(queryKeys);
        }
    }

    private void onPipeLineResultsReported(String queryKey) {
        if (mCurrentPlaylist != null
                && getCurrentQuery().getCacheKey().equals(queryKey)) {
            if (mIsRunningInForeground) {
                updatePlayingNotification();
            }
            sendBroadcast(new Intent(BROADCAST_CURRENTTRACKCHANGED));
        }
    }

    private void onInfoSystemResultsReported(String requestId) {
        if (mCurrentPlaylist != null && getCurrentQuery().getCacheKey()
                .equals(mCorrespondingInfoDataIds.get(requestId))) {
            if (mIsRunningInForeground) {
                updatePlayingNotification();
            }
            sendBroadcast(new Intent(BROADCAST_CURRENTTRACKCHANGED));
        }
    }
}
