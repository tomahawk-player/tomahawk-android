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

import com.google.common.collect.Sets;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.SpotifyAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Artist;
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
import org.tomahawk.tomahawk_android.events.PipeLineResultsEvent;
import org.tomahawk.tomahawk_android.mediaplayers.DeezerMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.RdioMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.SpotifyMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.VLCMediaPlayer;
import org.tomahawk.tomahawk_android.utils.AudioFocusHelper;
import org.tomahawk.tomahawk_android.utils.MediaButtonHelper;
import org.tomahawk.tomahawk_android.utils.MediaButtonReceiver;
import org.tomahawk.tomahawk_android.utils.MediaPlayerInterface;
import org.tomahawk.tomahawk_android.utils.MusicFocusable;
import org.tomahawk.tomahawk_android.utils.RemoteControlClientCompat;
import org.tomahawk.tomahawk_android.utils.RemoteControlHelper;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;
import org.videolan.libvlc.EventHandler;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
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
import java.util.Set;
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

    // The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
    public static final float DUCK_VOLUME = 0.1f;

    private static final int PLAYBACKSERVICE_PLAYSTATE_PLAYING = 0;

    private static final int PLAYBACKSERVICE_PLAYSTATE_PAUSED = 1;

    private int mPlayState = PLAYBACKSERVICE_PLAYSTATE_PLAYING;

    private static final int PLAYBACKSERVICE_NOTIFICATION_ID = 1;

    private static final int DELAY_TO_KILL = 300000;

    private boolean mShowingNotification;

    protected Set<Query> mCorrespondingQueries
            = Sets.newSetFromMap(new ConcurrentHashMap<Query, Boolean>());

    protected ConcurrentHashMap<String, String> mCorrespondingInfoDataIds
            = new ConcurrentHashMap<String, String>();

    private Playlist mPlaylist;

    private Playlist mShuffledPlaylist;

    private Playlist mQueue;

    private Playlist mMergedPlaylist;

    private int mQueueStartPos = -1;

    private PlaylistEntry mCurrentEntry;

    private MediaPlayerInterface mCurrentMediaPlayer;

    private Notification mNotification;

    private RemoteViews mLargeNotificationView;

    private RemoteViews mSmallNotificationView;

    private PowerManager.WakeLock mWakeLock;

    private PlaybackServiceBroadcastReceiver mPlaybackServiceBroadcastReceiver;

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

    private List<MediaPlayerInterface> mMediaPlayers = new ArrayList<MediaPlayerInterface>();

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

    private Target mLockscreenTarget = new Target() {
        @Override
        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            updateAlbumArt(bitmap);
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
            updateAlbumArt(BitmapFactory
                    .decodeResource(getResources(), R.drawable.album_placeholder_grid));
        }

        @Override
        public void onPrepareLoad(Drawable drawable) {
            updateAlbumArt(BitmapFactory
                    .decodeResource(getResources(), R.drawable.album_placeholder_grid));
        }

        private void updateAlbumArt(final Bitmap bitmap) {
            new Runnable() {
                @Override
                public void run() {
                    synchronized (PlaybackService.this) {
                        RemoteControlClientCompat.MetadataEditorCompat editor =
                                mRemoteControlClientCompat.editMetadata(false);
                        editor.putBitmap(
                                RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
                                bitmap.copy(bitmap.getConfig(), false));
                        editor.apply();
                        Log.d(TAG, "Setting lockscreen bitmap");
                    }
                }
            }.run();
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
            if (InfoSystem.INFOSYSTEM_RESULTSREPORTED.equals(intent.getAction())) {
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

    @SuppressWarnings("unused")
    public void onEvent(PipeLineResultsEvent event) {
        onPipeLineResultsReported(event.mQuery);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mMediaPlayers.add(VLCMediaPlayer.getInstance());
        mMediaPlayers.add(DeezerMediaPlayer.getInstance());
        mMediaPlayers.add(SpotifyMediaPlayer.getInstance());
        mMediaPlayers.add(RdioMediaPlayer.getInstance());

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
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneCallListener, PhoneStateListener.LISTEN_CALL_STATE);

        // Initialize WakeLock
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        // Initialize and register PlaybackServiceBroadcastReceiver
        mPlaybackServiceBroadcastReceiver = new PlaybackServiceBroadcastReceiver();
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
        super.onDestroy();

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
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneCallListener, PhoneStateListener.LISTEN_NONE);
        mPhoneCallListener = null;
        mKillTimerHandler.removeCallbacksAndMessages(null);
        mKillTimerHandler = null;

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
                .getPreferredTrackResult().getResolvedBy().getId());
        boolean allPlayersReleased = true;
        for (MediaPlayerInterface mediaPlayer : mMediaPlayers) {
            if (!mediaPlayer.isPrepared(getCurrentQuery())) {
                mediaPlayer.release();
            } else {
                allPlayersReleased = false;
            }
        }
        if (allPlayersReleased) {
            prepareCurrentQuery();
        } else if (isPlaying()) {
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
     * Called if given {@link org.tomahawk.tomahawk_android.mediaplayers.VLCMediaPlayer} has
     * finished playing a song. Prepare the next track if possible.
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
        if (getCurrentQuery() != null) {
            mPlayState = PLAYBACKSERVICE_PLAYSTATE_PLAYING;
            sendBroadcast(new Intent(BROADCAST_PLAYSTATECHANGED));
            handlePlayState();

            mShowingNotification = true;
            updateNotification();
            tryToGetAudioFocus();
            updateLockscreenPlayState();
        }
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
            mShowingNotification = false;
            stopForeground(true);
            giveUpAudioFocus();
            NotificationManager notificationManager = (NotificationManager) TomahawkApp.getContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(PLAYBACKSERVICE_NOTIFICATION_ID);
        } else {
            updateNotificationPlayState();
        }
        updateLockscreenPlayState();
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
        Log.d(TAG, "setShuffled from " + mShuffled + " to " + shuffled);
        if (mShuffled != shuffled) {
            mShuffled = shuffled;
            if (shuffled) {
                mShuffledPlaylist.setEntries(getShuffledPlaylistEntries());
            }
            mMergedPlaylist.setEntries(getMergedPlaylistEntries());
            if (getCurrentEntry() != null) {
                int index = mMergedPlaylist.getIndexOfEntry(mCurrentEntry);
                resolveQueriesFromTo(mMergedPlaylist.getEntries(), index, index + 10);
                resolveQueriesFromTo(mQueue.getEntries(), index, index + 10);
            }

            sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
        }
    }

    private ArrayList<PlaylistEntry> getShuffledPlaylistEntries() {
        ArrayList<PlaylistEntry> entries = mPlaylist.getEntries();
        Map<Artist, List<PlaylistEntry>> artistMap = new HashMap<Artist, List<PlaylistEntry>>();
        for (PlaylistEntry entry : entries) {
            if (entry != mCurrentEntry) {
                if (artistMap.get(entry.getArtist()) == null) {
                    artistMap.put(entry.getArtist(), new ArrayList<PlaylistEntry>());
                }
                artistMap.get(entry.getArtist()).add(entry);
            }
        }
        ArrayList<PlaylistEntry> shuffledEntries = new ArrayList<PlaylistEntry>();
        shuffledEntries.add(mCurrentEntry);
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
                            } else {
                                mCurrentMediaPlayer = getCurrentQuery().getMediaPlayerInterface();
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
        if (getCurrentEntry() != null) {
            int index = mMergedPlaylist.getIndexOfEntry(mCurrentEntry);
            resolveQueriesFromTo(mMergedPlaylist.getEntries(), index - 2, index + 10);
            resolveQueriesFromTo(mQueue.getEntries(), index, index - 2 + 10);
            updateNotification();
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
        mShuffled = false;
        mRepeating = false;
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
        if (mQueue.deleteEntry(entry)) {
            mMergedPlaylist.setEntries(getMergedPlaylistEntries());
            sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
            onTrackChanged();
        }
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
    public void updateNotification() {
        if (mShowingNotification) {
            Log.d(TAG, "updateNotification");

            String albumName = "";
            String artistName = "";
            if (getCurrentQuery().getAlbum() != null) {
                albumName = getCurrentQuery().getAlbum().getName();
            }
            if (getCurrentQuery().getArtist() != null) {
                artistName = getCurrentQuery().getArtist().getName();
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mSmallNotificationView = new RemoteViews(getPackageName(),
                        R.layout.notification_small);
            } else {
                mSmallNotificationView = new RemoteViews(getPackageName(),
                        R.layout.notification_small_compat);
            }
            mSmallNotificationView
                    .setTextViewText(R.id.notification_small_textview, getCurrentQuery().getName());
            if (TextUtils.isEmpty(albumName)) {
                mSmallNotificationView
                        .setTextViewText(R.id.notification_small_textview2, artistName);
            } else {
                mSmallNotificationView.setTextViewText(R.id.notification_small_textview2,
                        artistName + " - " + albumName);
            }
            if (isPlaying()) {
                mSmallNotificationView
                        .setImageViewResource(R.id.notification_small_imageview_playpause,
                                R.drawable.ic_player_pause_light);
            } else {
                mSmallNotificationView
                        .setImageViewResource(R.id.notification_small_imageview_playpause,
                                R.drawable.ic_player_play_light);
            }
            mSmallNotificationView
                    .setOnClickPendingIntent(R.id.notification_small_imageview_playpause,
                            playPausePendingIntent);
            mSmallNotificationView
                    .setOnClickPendingIntent(R.id.notification_small_imageview_next,
                            nextPendingIntent);
            mSmallNotificationView
                    .setOnClickPendingIntent(R.id.notification_small_imageview_exit,
                            exitPendingIntent);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(
                    PlaybackService.this)
                    .setSmallIcon(R.drawable.ic_notification).setContentTitle(artistName)
                    .setContentText(getCurrentQuery().getName()).setOngoing(true).setPriority(
                            NotificationCompat.PRIORITY_MAX).setContent(mSmallNotificationView);

            Intent notificationIntent = new Intent(PlaybackService.this,
                    TomahawkMainActivity.class);
            intent.setAction(TomahawkMainActivity.SHOW_PLAYBACKFRAGMENT_ON_STARTUP);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent resultPendingIntent = PendingIntent
                    .getActivity(PlaybackService.this, 0, notificationIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(resultPendingIntent);

            mNotification = builder.build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mLargeNotificationView = new RemoteViews(getPackageName(),
                        R.layout.notification_large);
                mLargeNotificationView.setTextViewText(R.id.notification_large_textview,
                        getCurrentQuery().getName());
                mLargeNotificationView
                        .setTextViewText(R.id.notification_large_textview2, artistName);
                mLargeNotificationView
                        .setTextViewText(R.id.notification_large_textview3, albumName);
                if (isPlaying()) {
                    mLargeNotificationView
                            .setImageViewResource(R.id.notification_large_imageview_playpause,
                                    R.drawable.ic_player_pause_light);
                } else {
                    mLargeNotificationView
                            .setImageViewResource(R.id.notification_large_imageview_playpause,
                                    R.drawable.ic_player_play_light);
                }
                mLargeNotificationView
                        .setOnClickPendingIntent(R.id.notification_large_imageview_previous,
                                previousPendingIntent);
                mLargeNotificationView
                        .setOnClickPendingIntent(R.id.notification_large_imageview_playpause,
                                playPausePendingIntent);
                mLargeNotificationView
                        .setOnClickPendingIntent(R.id.notification_large_imageview_next,
                                nextPendingIntent);
                mLargeNotificationView
                        .setOnClickPendingIntent(R.id.notification_large_imageview_exit,
                                exitPendingIntent);
                mNotification.bigContentView = mLargeNotificationView;

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        TomahawkUtils.loadImageIntoNotification(TomahawkApp.getContext(),
                                getCurrentQuery().getImage(), mSmallNotificationView,
                                R.id.notification_small_imageview_albumart,
                                PLAYBACKSERVICE_NOTIFICATION_ID,
                                mNotification, Image.getSmallImageSize(),
                                getCurrentQuery().hasArtistImage());
                        TomahawkUtils.loadImageIntoNotification(TomahawkApp.getContext(),
                                getCurrentQuery().getImage(), mLargeNotificationView,
                                R.id.notification_large_imageview_albumart,
                                PLAYBACKSERVICE_NOTIFICATION_ID,
                                mNotification, Image.getSmallImageSize(),
                                getCurrentQuery().hasArtistImage());
                    }
                });
            }
            startForeground(PLAYBACKSERVICE_NOTIFICATION_ID, mNotification);
        }
    }

    /**
     * Create or update an ongoing notification
     */
    public void updateNotificationPlayState() {
        if (mShowingNotification) {
            Log.d(TAG, "updateNotificationPlayState()");
            int resId;
            if (isPlaying()) {
                resId = R.drawable.ic_player_pause_light;
            } else {
                resId = R.drawable.ic_player_play_light;
            }
            if (mLargeNotificationView != null) {
                TomahawkUtils.loadDrawableIntoNotification(TomahawkApp.getContext(), resId,
                        mLargeNotificationView, R.id.notification_large_imageview_playpause,
                        PLAYBACKSERVICE_NOTIFICATION_ID, mNotification);
            }
            if (mSmallNotificationView != null) {
                TomahawkUtils.loadDrawableIntoNotification(TomahawkApp.getContext(), resId,
                        mSmallNotificationView, R.id.notification_small_imageview_playpause,
                        PLAYBACKSERVICE_NOTIFICATION_ID, mNotification);
            }
        }
    }

    /**
     * Update the playback controls/views which are being shown on the lockscreen
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void updateLockscreenControls() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Log.d(TAG, "updateLockscreenControls()");
            // Use the media button APIs (if available) to register ourselves for media button
            // events
            MediaButtonHelper.registerMediaButtonEventReceiverCompat(
                    mAudioManager, mMediaButtonReceiverComponent);

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

            // Use the remote control APIs (if available) to set the playback state
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
            synchronized (this) {
                RemoteControlClientCompat.MetadataEditorCompat editor =
                        mRemoteControlClientCompat.editMetadata(true);
                editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST,
                        getCurrentQuery().getArtist().getName())
                        .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST,
                                getCurrentQuery().getArtist().getName())
                        .putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
                                getCurrentQuery().getName())
                        .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                                getCurrentQuery().getPreferredTrack().getDuration());
                if (!TextUtils.isEmpty(getCurrentQuery().getAlbum().getName())) {
                    editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,
                            getCurrentQuery().getAlbum().getName());
                }
                editor.apply();
                Log.d(TAG, "Setting lockscreen metadata to: "
                        + getCurrentQuery().getArtist().getName() + ", "
                        + getCurrentQuery().getName());
            }

            Picasso.with(TomahawkApp.getContext()).cancelRequest(mLockscreenTarget);
            TomahawkUtils.loadImageIntoBitmap(TomahawkApp.getContext(),
                    getCurrentQuery().getImage(), mLockscreenTarget,
                    Image.getLargeImageSize(), getCurrentQuery().hasArtistImage());
        }
    }

    /**
     * Create or update an ongoing notification
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void updateLockscreenPlayState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
                && getCurrentQuery() != null) {
            Log.d(TAG, "updateLockscreenPlayState()");
            if (isPlaying()) {
                mRemoteControlClientCompat
                        .setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
            } else {
                mRemoteControlClientCompat
                        .setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            }
        }
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
        Set<Query> qs = new HashSet<>();
        for (int i = start; i < end; i++) {
            if (i >= 0 && i < entries.size()) {
                Query q = entries.get(i).getQuery();
                if (!mCorrespondingQueries.contains(q)) {
                    qs.add(q);
                }
            }
        }
        if (!qs.isEmpty()) {
            HashSet<Query> queries = PipeLine.getInstance().resolve(qs);
            mCorrespondingQueries.addAll(queries);
        }
    }

    private void onPipeLineResultsReported(Query query) {
        if (getCurrentQuery() != null && getCurrentQuery() == query) {
            updateNotification();
            updateLockscreenControls();
            sendBroadcast(new Intent(BROADCAST_CURRENTTRACKCHANGED));
            if (mCurrentMediaPlayer == null
                    || !(mCurrentMediaPlayer.isPrepared(getCurrentQuery())
                    || mCurrentMediaPlayer.isPreparing(getCurrentQuery()))) {
                prepareCurrentQuery();
            }
        }
    }

    private void onInfoSystemResultsReported(String requestId) {
        if (getCurrentEntry() != null && getCurrentQuery().getCacheKey()
                .equals(mCorrespondingInfoDataIds.get(requestId))) {
            updateNotification();
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
