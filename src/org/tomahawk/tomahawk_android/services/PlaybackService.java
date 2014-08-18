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
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyResolver;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyServiceUtils;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment;
import org.tomahawk.tomahawk_android.utils.AudioFocusHelper;
import org.tomahawk.tomahawk_android.utils.DeezerMediaPlayer;
import org.tomahawk.tomahawk_android.utils.MediaButtonHelper;
import org.tomahawk.tomahawk_android.utils.MediaButtonReceiver;
import org.tomahawk.tomahawk_android.utils.MusicFocusable;
import org.tomahawk.tomahawk_android.utils.RdioMediaPlayer;
import org.tomahawk.tomahawk_android.utils.RemoteControlClientCompat;
import org.tomahawk.tomahawk_android.utils.RemoteControlHelper;
import org.tomahawk.tomahawk_android.utils.SpotifyMediaPlayer;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;
import org.tomahawk.tomahawk_android.utils.VLCMediaPlayer;
import org.videolan.libvlc.EventHandler;

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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This {@link Service} handles all playback related processes.
 */
public class PlaybackService extends Service
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MusicFocusable {

    private static String TAG = PlaybackService.class.getSimpleName();

    public static final String BROADCAST_CURRENTTRACKCHANGED
            = "org.tomahawk.tomahawk_android.BROADCAST_CURRENTTRACKCHANGED";

    public static final String BROADCAST_PLAYLISTCHANGED
            = "org.tomahawk.tomahawk_android.BROADCAST_PLAYLISTCHANGED";

    public static final String BROADCAST_PLAYSTATECHANGED
            = "org.tomahawk.tomahawk_android.BROADCAST_PLAYSTATECHANGED";

    public static final String BROADCAST_VLCMEDIAPLAYER_PREPARED
            = "org.tomahawk.tomahawk_android.BROADCAST_VLCMEDIAPLAYER_PREPARED";

    public static final String BROADCAST_VLCMEDIAPLAYER_RELEASED
            = "org.tomahawk.tomahawk_android.BROADCAST_VLCMEDIAPLAYER_RELEASED";

    public static final String ACTION_PLAYPAUSE
            = "org.tomahawk.tomahawk_android.ACTION_PLAYPAUSE";

    public static final String ACTION_PLAY
            = "org.tomahawk.tomahawk_android.ACTION_PLAY";

    public static final String ACTION_PAUSE
            = "org.tomahawk.tomahawk_android.ACTION_PAUSE";

    public static final String ACTION_NEXT
            = "org.tomahawk.tomahawk_android.ACTION_NEXT";

    public static final String ACTION_PREVIOUS
            = "org.tomahawk.tomahawk_android.ACTION_PREVIOUS";

    public static final String ACTION_EXIT
            = "org.tomahawk.tomahawk_android.ACTION_EXIT";

    public static final String MERGED_PLAYLIST_ID = "merged_playlist_id";

    public static final String SHUFFLED_PLAYLIST_ID = "shuffled_playlist_id";

    private static Bitmap sPlaceHolder = null;

    // The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
    public static final float DUCK_VOLUME = 0.1f;

    private static final int PLAYBACKSERVICE_PLAYSTATE_PLAYING = 0;

    private static final int PLAYBACKSERVICE_PLAYSTATE_PAUSED = 1;

    private int mPlayState = PLAYBACKSERVICE_PLAYSTATE_PLAYING;

    private static final int PLAYBACKSERVICE_NOTIFICATION_ID = 1;

    private static final int DELAY_TO_KILL = 300000;

    private boolean mIsRunningInForeground;

    protected HashSet<String> mCorrespondingQueryKeys = new HashSet<String>();

    protected ConcurrentHashMap<String, String> mCorrespondingInfoDataIds
            = new ConcurrentHashMap<String, String>();

    private Playlist mPlaylist;

    private Playlist mShuffledPlaylist;

    private Playlist mQueue;

    private Playlist mMergedPlaylist;

    private int mQueueStartPos = -1;

    private PlaylistEntry mCurrentEntry;

    private PowerManager.WakeLock mWakeLock;

    private PlaybackServiceBroadcastReceiver mPlaybackServiceBroadcastReceiver;

    private Bitmap mNotificationBitmap = null;

    private Bitmap mLockscreenBitmap = null;

    private Image mLoadingNotificationImage = null;

    private Image mLoadedNotificationImage = null;

    private Image mLoadingLockscreenImage = null;

    private Image mLoadedLockscreenImage = null;

    private boolean mIsBindingToSpotifyService;

    private boolean mShuffled;

    private boolean mRepeating;

    private Class mReturnFragmentClass;

    private Bundle mReturnFragmentArgs;

    // our RemoteControlClient object, which will use remote control APIs available in
    // SDK level >= 14, if they're available.
    RemoteControlClientCompat mRemoteControlClientCompat;

    AudioManager mAudioManager;

    // The component name of PlaybackServiceBroadcastReceiver, for use with media button and
    // remote control APIs
    ComponentName mMediaButtonReceiverComponent;

    // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    AudioFocusHelper mAudioFocusHelper = null;

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    private SpotifyService.SpotifyServiceConnection mSpotifyServiceConnection
            = new SpotifyService.SpotifyServiceConnection(new SpotifyServiceConnectionListener());

    private Handler mVlcHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Bundle data = msg.getData();
            if (data != null) {
                switch (data.getInt("event")) {
                    case EventHandler.MediaPlayerEncounteredError:
                        VLCMediaPlayer.getInstance().onError(null, MediaPlayer.MEDIA_ERROR_UNKNOWN,
                                0);
                        break;
                    case EventHandler.MediaPlayerEndReached:
                        VLCMediaPlayer.getInstance().onCompletion(null);
                        break;
                    default:
                        return false;
                }
                return true;
            }
            return false;
        }
    });

    private Target mNotificationTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            mLoadedNotificationImage = mLoadingNotificationImage;
            mLoadingNotificationImage = null;
            mNotificationBitmap = bitmap;
            if (mIsRunningInForeground) {
                updatePlayingNotification();
            }
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
            mLoadingNotificationImage = null;
            mLoadedNotificationImage = null;
        }

        @Override
        public void onPrepareLoad(Drawable drawable) {

        }
    };

    private Target mLockscreenTarget = new Target() {
        @Override
        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            mLoadedLockscreenImage = mLoadingLockscreenImage;
            mLoadingLockscreenImage = null;
            mLockscreenBitmap = bitmap;
            updateLockscreenControls();
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
            mLoadingLockscreenImage = null;
            mLoadedLockscreenImage = null;
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
                    .getResolver(TomahawkApp.PLUGINNAME_SPOTIFY);
            spotifyResolver.setToSpotifyMessenger(messenger);
            SpotifyAuthenticatorUtils authUtils = (SpotifyAuthenticatorUtils)
                    AuthenticatorManager.getInstance()
                            .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_SPOTIFY);
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
                    Log.d(TAG, "Action audio becoming noisy, pausing ...");
                    // So we stop playback, if needed
                    pause();
                }
            } else if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction()) && intent
                    .hasExtra("state") && intent.getIntExtra("state", 0) == 1) {
                Log.d(TAG, "Headset has been plugged in");
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
            } else if (BROADCAST_VLCMEDIAPLAYER_PREPARED.equals(intent.getAction())) {
                EventHandler.getInstance().addHandler(mVlcHandler);
            } else if (BROADCAST_VLCMEDIAPLAYER_RELEASED.equals(intent.getAction())) {
                EventHandler.getInstance().removeHandler(mVlcHandler);
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
        CollectionManager.getInstance().ensureInit();

        bindService(new Intent(this, SpotifyService.class), mSpotifyServiceConnection,
                Context.BIND_AUTO_CREATE);

        startService(new Intent(this, MicroService.class));

        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            bindService(new Intent(this, RemoteControllerService.class), connection,
                    Context.BIND_AUTO_CREATE);
        }

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

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
        registerReceiver(mPlaybackServiceBroadcastReceiver,
                new IntentFilter(BROADCAST_VLCMEDIAPLAYER_PREPARED));
        registerReceiver(mPlaybackServiceBroadcastReceiver,
                new IntentFilter(BROADCAST_VLCMEDIAPLAYER_RELEASED));

        mMediaButtonReceiverComponent = new ComponentName(this, MediaButtonReceiver.class);
        mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);

        // Initialize killtime handler (watchdog style)
        mKillTimerHandler.removeCallbacksAndMessages(null);
        Message msg = mKillTimerHandler.obtainMessage();
        mKillTimerHandler.sendMessageDelayed(msg, DELAY_TO_KILL);

        sPlaceHolder =
                BitmapFactory.decodeResource(getResources(), R.drawable.no_album_art_placeholder);

        mPlaylist = Playlist.fromEntriesList(DatabaseHelper.CACHED_PLAYLIST_NAME, "",
                new ArrayList<PlaylistEntry>());
        mPlaylist.setId(DatabaseHelper.CACHED_PLAYLIST_ID);
        mQueue = Playlist.fromEntriesList(DatabaseHelper.QUEUE_NAME, "",
                new ArrayList<PlaylistEntry>());
        mQueue.setId(DatabaseHelper.QUEUE_ID);
        mShuffledPlaylist = Playlist.fromEntriesList("", "", new ArrayList<PlaylistEntry>());
        mShuffledPlaylist.setId(SHUFFLED_PLAYLIST_ID);
        mMergedPlaylist = Playlist.fromEntriesList("", "", new ArrayList<PlaylistEntry>());
        mMergedPlaylist.setId(MERGED_PLAYLIST_ID);
        restoreState();
        Log.d(TAG, "PlaybackService has been created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(ACTION_PREVIOUS)) {
                previous();
            } else if (intent.getAction().equals(ACTION_PLAYPAUSE)) {
                playPause();
            } else if (intent.getAction().equals(ACTION_PLAY)) {
                start();
            } else if (intent.getAction().equals(ACTION_PAUSE)) {
                pause();
            } else if (intent.getAction().equals(ACTION_NEXT)) {
                next();
            } else if (intent.getAction().equals(ACTION_EXIT)) {
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
        giveUpAudioFocus();
        pause(true);
        saveState();
        unregisterReceiver(mPlaybackServiceBroadcastReceiver);
        mPlaybackServiceBroadcastReceiver = null;
        unbindService(mSpotifyServiceConnection);
        mSpotifyServiceConnection = null;
        releaseAllPlayers();
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        mWakeLock = null;
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(
                Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneCallListener, PhoneStateListener.LISTEN_NONE);
        mPhoneCallListener = null;
        mNotificationTarget = null;
        mKillTimerHandler.removeCallbacksAndMessages(null);
        mKillTimerHandler = null;

        super.onDestroy();
        Log.d(TAG, "PlaybackService has been destroyed");
    }

    /**
     * Called if given {@link VLCMediaPlayer} has been prepared for playback
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
                    AuthenticatorManager.getInstance().getAuthenticatorUtils(
                            TomahawkApp.PLUGINNAME_HATCHET),
                    getCurrentQuery()
            );
        }
        handlePlayState();
    }

    /**
     * Called if an error has occurred while trying to prepare {@link VLCMediaPlayer}
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
        giveUpAudioFocus();
        if (hasNextEntry()) {
            next();
        } else {
            pause();
        }
        return false;
    }

    /**
     * Called if given {@link org.tomahawk.tomahawk_android.utils.VLCMediaPlayer} has finished
     * playing a song. Prepare the next track if possible.
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion");
        if (hasNextEntry()) {
            next();
        } else {
            pause();
        }
    }

    /**
     * Save the current playlist in the Playlists Database
     */
    private void saveState() {
        /*
        if (getPlaylist() != null) {
            long startTime = System.currentTimeMillis();
            CollectionManager.getInstance().setCachedPlaylist(Playlist
                    .fromQueryList(DatabaseHelper.CACHED_PLAYLIST_ID,
                            DatabaseHelper.CACHED_PLAYLIST_NAME,
                            getPlaylist().getQueries()));
            Log.d(TAG, "Playlist stored in " + (System.currentTimeMillis() - startTime) + "ms");
        }*/
    }

    /**
     * Restore the current playlist from the Playlists Database. Do this by storing it in the {@link
     * org.tomahawk.libtomahawk.collection.UserCollection} first, and then retrieving the playlist
     * from there.
     */
    private void restoreState() {
        /*
        long startTime = System.currentTimeMillis();
        setPlaylist(CollectionManager.getInstance().getCachedPlaylist());
        Log.d(TAG, "Playlist loaded in " + (System.currentTimeMillis() - startTime) + "ms");
        if (getPlaylist() != null && isPlaying()) {
            pause(true);
        }
        */
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
        Log.d(TAG, "start");
        mPlayState = PLAYBACKSERVICE_PLAYSTATE_PLAYING;
        sendBroadcast(new Intent(BROADCAST_PLAYSTATECHANGED));
        handlePlayState();
        updatePlayingNotification();

        tryToGetAudioFocus();
        updateLockscreenControls();
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
        Log.d(TAG, "pause, dismissing Notification:" + dismissNotificationOnPause);
        mPlayState = PLAYBACKSERVICE_PLAYSTATE_PAUSED;
        sendBroadcast(new Intent(BROADCAST_PLAYSTATECHANGED));
        handlePlayState();
        if (dismissNotificationOnPause) {
            mIsRunningInForeground = false;
            stopForeground(true);
            giveUpAudioFocus();
        } else {
            updatePlayingNotification();
        }
        updateLockscreenControls();
    }

    /**
     * Update the TomahawkMediaPlayer so that it reflects the current playState
     */
    public void handlePlayState() {
        Log.d(TAG, "handlePlayState");
        if (!isPreparing() && getCurrentQuery() != null
                && getCurrentQuery().getMediaPlayerInterface() != null) {
            try {
                switch (mPlayState) {
                    case PLAYBACKSERVICE_PLAYSTATE_PLAYING:
                        if (mWakeLock != null && mWakeLock.isHeld()) {
                            mWakeLock.acquire();
                        }
                        if (getCurrentQuery().getMediaPlayerInterface()
                                .isPrepared(getCurrentQuery())) {
                            if (!getCurrentQuery().getMediaPlayerInterface()
                                    .isPlaying(getCurrentQuery())) {
                                getCurrentQuery().getMediaPlayerInterface().start();
                            }
                        } else if (!isPreparing()) {
                            prepareCurrentQuery();
                        }
                        break;
                    case PLAYBACKSERVICE_PLAYSTATE_PAUSED:
                        if (getCurrentQuery().getMediaPlayerInterface().isPlaying(getCurrentQuery())
                                && getCurrentQuery().getMediaPlayerInterface()
                                .isPrepared(getCurrentQuery())) {
                            InfoSystem.getInstance().sendPlaybackEntryPostStruct(
                                    AuthenticatorManager.getInstance().getAuthenticatorUtils(
                                            TomahawkApp.PLUGINNAME_HATCHET)
                            );
                            getCurrentQuery().getMediaPlayerInterface().pause();
                        }
                        if (mWakeLock != null && mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                        break;
                }
            } catch (IllegalStateException e1) {
                Log.e(TAG,
                        "handlePlayState IllegalStateException, msg:" + e1.getLocalizedMessage()
                                + " , preparing=" + isPreparing()
                );
            }
            if (mKillTimerHandler != null) {
                mKillTimerHandler.removeCallbacksAndMessages(null);
                Message msg = mKillTimerHandler.obtainMessage();
                mKillTimerHandler.sendMessageDelayed(msg, DELAY_TO_KILL);
            }
        } else {
            Log.d(TAG, "handlePlayState couldn't do anything, isPreparing" + isPreparing());
        }
    }

    /**
     * Start playing the next Track.
     */
    public void next() {
        Log.d(TAG, "next");
        releaseAllPlayers();
        int counter = 0;
        while (hasNextEntry() && counter++ < mMergedPlaylist.size()) {
            PlaylistEntry entry = getNextEntry();
            deleteQueryInQueue(mCurrentEntry);
            mCurrentEntry = entry;
            if (mCurrentEntry.getQuery().isPlayable()) {
                sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
                onTrackChanged();
                break;
            }
        }
        handlePlayState();
    }

    /**
     * Play the previous track.
     */
    public void previous() {
        Log.d(TAG, "previous");
        releaseAllPlayers();
        int counter = 0;
        while (hasPreviousEntry() && counter++ < mMergedPlaylist.size()) {
            PlaylistEntry entry = getPreviousEntry();
            deleteQueryInQueue(entry);
            mCurrentEntry = entry;
            if (mCurrentEntry.getQuery().isPlayable()) {
                sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
                onTrackChanged();
                break;
            }
        }
        handlePlayState();
    }

    public boolean isShuffled() {
        return mShuffled;
    }

    /**
     * Set whether or not to enable shuffle mode on the current playlist.
     */
    public void setShuffled(boolean shuffled) {
        Log.d(TAG, "setShuffled to " + shuffled);
        mShuffled = shuffled;
        if (shuffled) {
            mShuffledPlaylist.setEntries(getShuffledPlaylistEntries());
            mMergedPlaylist.setEntries(getMergedPlaylistEntries());
        }

        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    private ArrayList<PlaylistEntry> getShuffledPlaylistEntries() {
        ArrayList<PlaylistEntry> entries = mPlaylist.getEntries();
        Map<Artist, List<PlaylistEntry>> artistMap = new HashMap<Artist, List<PlaylistEntry>>();
        for (PlaylistEntry entry : entries) {
            if (artistMap.get(entry.getArtist()) == null) {
                artistMap.put(entry.getArtist(), new ArrayList<PlaylistEntry>());
            }
            artistMap.get(entry.getArtist()).add(entry);
        }
        ArrayList<PlaylistEntry> shuffledEntries = new ArrayList<PlaylistEntry>();
        for (int i = entries.size(); i >= 0; i--) {
            int pos = (int) (Math.random() * i);
            for (Artist key : artistMap.keySet()) {
                List<PlaylistEntry> artistEntries = artistMap.get(key);
                if (artistEntries.size() > pos) {
                    shuffledEntries.add(artistEntries.remove(pos));
                }
            }
        }
        return shuffledEntries;
    }

    public boolean isRepeating() {
        return mRepeating;
    }

    /**
     * Set whether or not to enable repeat mode on the current playlist.
     */
    public void setRepeating(boolean repeating) {
        Log.d(TAG, "setRepeating to " + repeating);
        mRepeating = repeating;
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
                && getCurrentQuery().getMediaPlayerInterface().isPreparing(getCurrentQuery());
    }

    /**
     * Get the current PlaylistEntry
     */
    public PlaylistEntry getCurrentEntry() {
        return mCurrentEntry;
    }

    public PlaylistEntry getNextEntry() {
        PlaylistEntry entry = mMergedPlaylist.getNextEntry(mCurrentEntry);
        if (entry == null && mRepeating) {
            entry = mMergedPlaylist.getFirstEntry();
        }
        return entry;
    }

    public boolean hasNextEntry() {
        boolean result = mMergedPlaylist.hasNextEntry(mCurrentEntry);
        if (!result && mRepeating) {
            result = mMergedPlaylist.size() > 0;
        }
        return result;
    }

    public PlaylistEntry getPreviousEntry() {
        PlaylistEntry entry = mMergedPlaylist.getPreviousEntry(mCurrentEntry);
        if (entry == null && mRepeating) {
            entry = mMergedPlaylist.getLastEntry();
        }
        return entry;
    }

    public boolean hasPreviousEntry() {
        boolean result = mMergedPlaylist.hasPreviousEntry(mCurrentEntry);
        if (!result && mRepeating) {
            result = mMergedPlaylist.size() > 0;
        }
        return result;
    }

    /**
     * Get the current Query
     */
    public Query getCurrentQuery() {
        if (mCurrentEntry != null) {
            return mCurrentEntry.getQuery();
        }
        return null;
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
        Log.d(TAG, "prepareCurrentQuery");
        if (getCurrentQuery() != null) {
            if (getCurrentQuery().isPlayable()) {
                mKillTimerHandler.removeCallbacksAndMessages(null);
                Message msg = mKillTimerHandler.obtainMessage();
                mKillTimerHandler.sendMessageDelayed(msg, DELAY_TO_KILL);

                if (mIsRunningInForeground) {
                    updatePlayingNotification();
                }
                updateLockscreenControls();
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
                                    getApplication(), getCurrentQuery(), PlaybackService.this,
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

    public void setCurrentEntry(PlaylistEntry entry) {
        Log.d(TAG, "setCurrentEntry to " + entry.getId());
        releaseAllPlayers();
        deleteQueryInQueue(mCurrentEntry);
        mCurrentEntry = entry;
        handlePlayState();
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
        onTrackChanged();
    }

    private void onTrackChanged() {
        Log.d(TAG, "onTrackChanged");
        sendBroadcast(new Intent(BROADCAST_CURRENTTRACKCHANGED));
        if (getCurrentQuery() != null) {
            int index = mMergedPlaylist.getIndexOfEntry(mCurrentEntry);
            resolveQueriesFromTo(mMergedPlaylist.getEntries(), index, index + 10);
            resolveQueriesFromTo(mQueue.getEntries(), index, index + 10);
            if (mIsRunningInForeground) {
                updatePlayingNotification();
            }
            updateLockscreenControls();
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
    public Playlist getPlaylist() {
        return mMergedPlaylist;
    }

    /**
     * Get the current Queue
     */
    public Playlist getQueue() {
        return mQueue;
    }

    /**
     * Set the current Playlist to playlist and set the current Track to the Playlist's current
     * Track.
     */
    public void setPlaylist(Playlist playlist, PlaylistEntry currentEntry) {
        Log.d(TAG, "setPlaylist");
        releaseAllPlayers();
        mPlaylist = playlist;
        mQueueStartPos = -1;
        deleteQueryInQueue(mCurrentEntry);
        mCurrentEntry = currentEntry;
        mMergedPlaylist.setEntries(getMergedPlaylistEntries());

        handlePlayState();
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
        onTrackChanged();
    }

    public void setReturnFragment(Class clss, Bundle args) {
        mReturnFragmentClass = clss;
        mReturnFragmentArgs = args;
    }

    public Class getReturnFragmentClass() {
        return mReturnFragmentClass;
    }

    public Bundle getReturnFragmentArgs() {
        return mReturnFragmentArgs;
    }

    private ArrayList<PlaylistEntry> getMergedPlaylistEntries() {
        ArrayList<PlaylistEntry> entries = new ArrayList<PlaylistEntry>();
        entries.addAll(mShuffled ? mShuffledPlaylist.getEntries() : mPlaylist.getEntries());
        int insertPos = entries.indexOf(mCurrentEntry);
        if (insertPos > 0) {
            entries.addAll(insertPos + 1, mQueue.getEntries());
            mQueueStartPos = insertPos;
        } else if (mQueueStartPos > 0) {
            entries.addAll(mQueueStartPos + 1, mQueue.getEntries());
        }
        return entries;
    }

    /**
     * Add given {@link ArrayList} of {@link org.tomahawk.libtomahawk.resolver.Query}s to the Queue
     */
    public void addQueriesToQueue(ArrayList<Query> queries) {
        Log.d(TAG, "addQueriesToQueue count: " + queries.size());
        mQueue.addQueries(queries);
        mMergedPlaylist.setEntries(getMergedPlaylistEntries());
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
        onTrackChanged();
    }

    /**
     * Add given {@link ArrayList} of {@link org.tomahawk.libtomahawk.resolver.Query}s to the Queue
     */
    public void deleteQueryInQueue(PlaylistEntry entry) {
        Log.d(TAG, "deleteQueryInQueue");
        mQueue.deleteEntry(entry);
        mMergedPlaylist.setEntries(getMergedPlaylistEntries());
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
        onTrackChanged();
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
        Log.d(TAG, "seekTo " + msec);
        if (getCurrentQuery() != null && getCurrentQuery().getMediaPlayerInterface() != null
                && getCurrentQuery().getMediaPlayerInterface().isPrepared(getCurrentQuery())) {
            getCurrentQuery().getMediaPlayerInterface().seekTo(msec);
        }
    }

    /**
     * Create or update an ongoing notification
     */
    public void updatePlayingNotification() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "updatePlayingNotification");
                Query query = getCurrentQuery();
                if (query == null) {
                    return;
                }

                String albumName = "";
                String artistName = "";
                if (query.getAlbum() != null) {
                    albumName = query.getAlbum().getName();
                }
                if (query.getArtist() != null) {
                    artistName = query.getArtist().getName();
                }

                Intent intent = new Intent(ACTION_PREVIOUS, null, PlaybackService.this,
                        PlaybackService.class);
                PendingIntent previousPendingIntent = PendingIntent
                        .getService(PlaybackService.this, 0, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT);
                intent = new Intent(ACTION_PLAYPAUSE, null, PlaybackService.this,
                        PlaybackService.class);
                PendingIntent playPausePendingIntent = PendingIntent
                        .getService(PlaybackService.this, 0, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT);
                intent = new Intent(ACTION_NEXT, null, PlaybackService.this, PlaybackService.class);
                PendingIntent nextPendingIntent = PendingIntent
                        .getService(PlaybackService.this, 0, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT);
                intent = new Intent(ACTION_EXIT, null, PlaybackService.this, PlaybackService.class);
                PendingIntent exitPendingIntent = PendingIntent
                        .getService(PlaybackService.this, 0, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT);

                RemoteViews smallNotificationView;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    smallNotificationView = new RemoteViews(getPackageName(),
                            R.layout.notification_small);
                    if (mNotificationBitmap != null && !mNotificationBitmap.isRecycled()
                            && mLoadedNotificationImage != null
                            && mLoadedNotificationImage == getCurrentQuery().getImage()) {
                        smallNotificationView
                                .setImageViewBitmap(R.id.notification_small_imageview_albumart,
                                        mNotificationBitmap);
                    } else {
                        smallNotificationView
                                .setImageViewBitmap(R.id.notification_small_imageview_albumart,
                                        sPlaceHolder.copy(sPlaceHolder.getConfig(), false));
                    }
                } else {
                    smallNotificationView = new RemoteViews(getPackageName(),
                            R.layout.notification_small_compat);
                }
                smallNotificationView
                        .setTextViewText(R.id.notification_small_textview, query.getName());
                if (TextUtils.isEmpty(albumName)) {
                    smallNotificationView
                            .setTextViewText(R.id.notification_small_textview2, artistName);
                } else {
                    smallNotificationView.setTextViewText(R.id.notification_small_textview2,
                            artistName + " - " + albumName);
                }
                if (isPlaying()) {
                    smallNotificationView
                            .setImageViewResource(R.id.notification_small_imageview_playpause,
                                    R.drawable.ic_player_pause_light);
                } else {
                    smallNotificationView
                            .setImageViewResource(R.id.notification_small_imageview_playpause,
                                    R.drawable.ic_player_play_light);
                }
                smallNotificationView
                        .setOnClickPendingIntent(R.id.notification_small_imageview_playpause,
                                playPausePendingIntent);
                smallNotificationView
                        .setOnClickPendingIntent(R.id.notification_small_imageview_next,
                                nextPendingIntent);
                smallNotificationView
                        .setOnClickPendingIntent(R.id.notification_small_imageview_exit,
                                exitPendingIntent);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(
                        PlaybackService.this)
                        .setSmallIcon(R.drawable.ic_notification).setContentTitle(artistName)
                        .setContentText(query.getName()).setOngoing(true).setPriority(
                                NotificationCompat.PRIORITY_MAX).setContent(smallNotificationView);
                if (mNotificationBitmap != null && !mNotificationBitmap.isRecycled()
                        && mLoadedNotificationImage != null
                        && mLoadedNotificationImage == getCurrentQuery().getImage()) {
                    builder.setLargeIcon(mNotificationBitmap);
                } else if (mLoadingNotificationImage != getCurrentQuery().getImage()) {
                    // A bitmap has been loaded or is loading, but it's not the one we want
                    builder.setLargeIcon(sPlaceHolder.copy(sPlaceHolder.getConfig(), false));
                    Picasso.with(TomahawkApp.getContext())
                            .cancelRequest(mNotificationTarget);
                    mLoadingNotificationImage = getCurrentQuery().getImage();
                    TomahawkUtils.loadImageIntoBitmap(TomahawkApp.getContext(),
                            getCurrentQuery().getImage(), mNotificationTarget,
                            Image.getLargeImageSize());
                }

                Intent notificationIntent = new Intent(PlaybackService.this,
                        TomahawkMainActivity.class);
                intent.setAction(TomahawkMainActivity.SHOW_PLAYBACKFRAGMENT_ON_STARTUP);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent resultPendingIntent = PendingIntent
                        .getActivity(PlaybackService.this, 0, notificationIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(resultPendingIntent);

                Notification notification = builder.build();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    RemoteViews largeNotificationView = new RemoteViews(getPackageName(),
                            R.layout.notification_large);
                    if (mNotificationBitmap != null && !mNotificationBitmap.isRecycled()
                            && mLoadedNotificationImage != null
                            && mLoadedNotificationImage == getCurrentQuery().getImage()) {
                        largeNotificationView
                                .setImageViewBitmap(R.id.notification_large_imageview_albumart,
                                        mNotificationBitmap);
                    } else {
                        largeNotificationView
                                .setImageViewBitmap(R.id.notification_large_imageview_albumart,
                                        sPlaceHolder.copy(sPlaceHolder.getConfig(), false));
                    }
                    largeNotificationView.setTextViewText(R.id.notification_large_textview,
                            query.getName());
                    largeNotificationView
                            .setTextViewText(R.id.notification_large_textview2, artistName);
                    largeNotificationView
                            .setTextViewText(R.id.notification_large_textview3, albumName);
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
                    largeNotificationView
                            .setOnClickPendingIntent(R.id.notification_large_imageview_next,
                                    nextPendingIntent);
                    largeNotificationView
                            .setOnClickPendingIntent(R.id.notification_large_imageview_exit,
                                    exitPendingIntent);
                    notification.bigContentView = largeNotificationView;
                }
                mIsRunningInForeground = true;
                startForeground(PLAYBACKSERVICE_NOTIFICATION_ID, notification);
            }
        }).start();
    }

    /**
     * Update the playback controls/views which are being shown on the lockscreen
     */
    private void updateLockscreenControls() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
                        && getCurrentQuery() != null) {
                    Log.d(TAG, "updateLockscreenControls()");
                    // Use the media button APIs (if available) to register ourselves for media button
                    // events
                    MediaButtonHelper.registerMediaButtonEventReceiverCompat(
                            mAudioManager, mMediaButtonReceiverComponent);

                    // Use the remote control APIs (if available) to set the playback state
                    if (mRemoteControlClientCompat == null) {
                        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                        intent.setComponent(mMediaButtonReceiverComponent);
                        mRemoteControlClientCompat = new RemoteControlClientCompat(
                                PendingIntent.getBroadcast(PlaybackService.this /*context*/,
                                        0 /*requestCode, ignored*/, intent /*intent*/, 0 /*flags*/)
                        );
                        RemoteControlHelper.registerRemoteControlClient(mAudioManager,
                                mRemoteControlClientCompat);
                    }

                    if (isPlaying()) {
                        mRemoteControlClientCompat
                                .setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
                    } else {
                        mRemoteControlClientCompat
                                .setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
                    }

                    int flags = RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                            RemoteControlClient.FLAG_KEY_MEDIA_PAUSE;
                    if (hasNextEntry()) {
                        flags |= RemoteControlClient.FLAG_KEY_MEDIA_NEXT;
                    }
                    if (hasPreviousEntry()) {
                        flags |= RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS;
                    }
                    mRemoteControlClientCompat.setTransportControlFlags(flags);

                    // Update the remote controls
                    RemoteControlClientCompat.MetadataEditorCompat editor =
                            mRemoteControlClientCompat.editMetadata(true);
                    editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST,
                            getCurrentQuery().getArtist().getName())
                            .putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
                                    getCurrentQuery().getName())
                            .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                                    getCurrentQuery().getPreferredTrack().getDuration());
                    if (mLockscreenBitmap != null && !mLockscreenBitmap.isRecycled()
                            && mLoadedLockscreenImage != null
                            && mLoadedLockscreenImage == getCurrentQuery().getImage()) {
                        // Bitmap is already loaded, we'll use that
                        editor.putBitmap(
                                RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
                                mLockscreenBitmap);
                    } else if (mLoadingLockscreenImage != getCurrentQuery().getImage()) {
                        // A bitmap has been loaded or is loading, but it's not the one we want
                        editor.putBitmap(
                                RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
                                sPlaceHolder.copy(sPlaceHolder.getConfig(), false));
                        Picasso.with(TomahawkApp.getContext()).cancelRequest(mLockscreenTarget);
                        mLoadingLockscreenImage = getCurrentQuery().getImage();
                        TomahawkUtils.loadImageIntoBitmap(TomahawkApp.getContext(),
                                getCurrentQuery().getImage(), mLockscreenTarget,
                                Image.getLargeImageSize());
                    }
                    editor.apply();
                }
            }
        }).start();
    }

    @Override
    public void onGainedAudioFocus() {

    }

    @Override
    public void onLostAudioFocus(boolean canDuck) {

    }

    void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.requestFocus()) {
            mAudioFocus = AudioFocus.Focused;
        }
    }

    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.abandonFocus()) {
            mAudioFocus = AudioFocus.NoFocusNoDuck;
        }
    }

    private void resolveQueriesFromTo(ArrayList<PlaylistEntry> entries, int start, int end) {
        ArrayList<Query> qs = new ArrayList<Query>();
        for (int i = start; i < end; i++) {
            if (i >= 0 && i < entries.size()) {
                Query q = entries.get(i).getQuery();
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
        if (getCurrentQuery() != null
                && getCurrentQuery().getCacheKey().equals(queryKey)) {
            if (mIsRunningInForeground) {
                updatePlayingNotification();
            }
            updateLockscreenControls();
            sendBroadcast(new Intent(BROADCAST_CURRENTTRACKCHANGED));
        }
    }

    private void onInfoSystemResultsReported(String requestId) {
        if (getCurrentEntry() != null && getCurrentQuery().getCacheKey()
                .equals(mCorrespondingInfoDataIds.get(requestId))) {
            if (mIsRunningInForeground) {
                updatePlayingNotification();
            }
            updateLockscreenControls();
            sendBroadcast(new Intent(BROADCAST_CURRENTTRACKCHANGED));
        }
    }

    private void releaseAllPlayers() {
        VLCMediaPlayer.getInstance().release();
        SpotifyMediaPlayer.getInstance().release();
        RdioMediaPlayer.getInstance().release();
        DeezerMediaPlayer.getInstance().release();
    }
}
