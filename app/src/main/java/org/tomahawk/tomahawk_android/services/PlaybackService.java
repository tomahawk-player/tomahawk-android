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
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.ImageUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.mediaplayers.DeezerMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.PluginMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.SpotifyMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.TomahawkMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.TomahawkMediaPlayerCallback;
import org.tomahawk.tomahawk_android.mediaplayers.VLCMediaPlayer;
import org.tomahawk.tomahawk_android.utils.AudioFocusHelper;
import org.tomahawk.tomahawk_android.utils.AudioFocusable;
import org.tomahawk.tomahawk_android.utils.MediaButtonReceiver;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;
import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;

/**
 * This {@link Service} handles all playback related processes.
 */
public class PlaybackService extends Service {

    private static final String TAG = PlaybackService.class.getSimpleName();

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

    public static final String ACTION_FAVORITE
            = "org.tomahawk.tomahawk_android.ACTION_FAVORITE";

    public static final int NOT_REPEATING = 0;

    public static final int REPEAT_ALL = 1;

    public static final int REPEAT_ONE = 2;

    private static final int PLAYBACKSERVICE_PLAYSTATE_PLAYING = 0;

    private static final int PLAYBACKSERVICE_PLAYSTATE_PAUSED = 1;

    private int mPlayState = PLAYBACKSERVICE_PLAYSTATE_PLAYING;

    private static final int PLAYBACKSERVICE_NOTIFICATION_ID = 1;

    private static final int DELAY_TO_KILL = 300000;

    public static class PlayingTrackChangedEvent {

    }

    public static class PlayingPlaylistChangedEvent {

    }

    public static class PlayStateChangedEvent {

    }

    public static class PlayPositionChangedEvent {

        public long duration;

        public int currentPosition;

    }

    public static class ReadyEvent {

    }

    public static class RequestServiceBindingEvent {

        private ServiceConnection mConnection;

        private String mServicePackageName;

        public RequestServiceBindingEvent(ServiceConnection connection, String servicePackageName) {
            mConnection = connection;
            mServicePackageName = servicePackageName;
        }

    }

    public static class SetBitrateEvent {

        public int mode;

    }

    private boolean mShowingNotification;

    protected final Set<Query> mCorrespondingQueries
            = Collections.newSetFromMap(new ConcurrentHashMap<Query, Boolean>());

    protected final ConcurrentHashMap<String, String> mCorrespondingRequestIds
            = new ConcurrentHashMap<>();

    private Playlist mPlaylist;

    private Playlist mQueue;

    private int mQueueStartPos = -1;

    private PlaylistEntry mCurrentEntry;

    private int mCurrentIndex = -1;

    private TomahawkMediaPlayer mCurrentMediaPlayer;

    private Notification mNotification;

    private RemoteViews mLargeNotificationView;

    private RemoteViews mSmallNotificationView;

    private PowerManager.WakeLock mWakeLock;

    private int mRepeatingMode = NOT_REPEATING;

    private MediaControllerCompat.TransportControls mTransportControls;

    private MediaSessionCompat mMediaSessionCompat;

    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public boolean onMediaButtonEvent(Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // AudioManager tells us that the sound will be played through the speaker
                Log.d(TAG, "Action audio becoming noisy, pausing ...");
                // So we stop playback, if needed
                pause();
            } else if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
                if (keyEvent == null) {
                    return false;
                }
                Log.d(TAG, "Mediabutton pressed ... keyCode: " + keyEvent.getKeyCode());
                if (keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }

                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        if (isPlaying()) {
                            mTransportControls.pause();
                        } else {
                            mTransportControls.play();
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        mTransportControls.play();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        mTransportControls.pause();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_STOP:
                        mTransportControls.stop();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        mTransportControls.skipToNext();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        mTransportControls.skipToPrevious();
                        break;
                }
            }
            return super.onMediaButtonEvent(intent);
        }

        @Override
        public void onPlay() {
            super.onPlay();
            start();
        }

        @Override
        public void onPause() {
            super.onPause();
            pause();
        }
    };

    AudioManager mAudioManager;

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

    private final Map<Class, TomahawkMediaPlayer> mMediaPlayers = new HashMap<>();

    private final Target mLockscreenTarget = new Target() {
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
                    MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST,
                                    getCurrentQuery().getArtist().getPrettyName())
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                                    getCurrentQuery().getArtist().getPrettyName())
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                                    getCurrentQuery().getPrettyName())
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                                    getCurrentQuery().getPreferredTrack().getDuration())
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap);
                    if (!TextUtils.isEmpty(getCurrentQuery().getAlbum().getPrettyName())) {
                        builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                                getCurrentQuery().getAlbum().getPrettyName());
                    }
                    mMediaSessionCompat.setMetadata(builder.build());
                    Log.d(TAG, "Setting lockscreen bitmap");
                }
            }.run();
        }
    };

    public class PlaybackServiceBinder extends Binder {

        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    private RemoteControllerConnection mRemoteControllerConnection;

    private static class RemoteControllerConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to RemoteControllerService!");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "RemoteControllerService has crashed :(");
        }
    }

    /**
     * The static {@link ServiceConnection} which calls methods in {@link
     * PlaybackServiceConnectionListener} to let every depending object know, if the {@link
     * PlaybackService} connects or disconnects.
     */
    public static class PlaybackServiceConnection implements ServiceConnection {

        private final PlaybackServiceConnectionListener mPlaybackServiceConnectionListener;

        public interface PlaybackServiceConnectionListener {

            void setPlaybackService(PlaybackService ps);
        }

        public PlaybackServiceConnection(
                PlaybackServiceConnectionListener playbackServiceConnectedListener) {
            mPlaybackServiceConnectionListener = playbackServiceConnectedListener;
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            PlaybackServiceBinder binder = (PlaybackServiceBinder) service;
            mPlaybackServiceConnectionListener.setPlaybackService(binder.getService());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mPlaybackServiceConnectionListener.setPlaybackService(null);
        }
    }

    /**
     * Listens for incoming phone calls and handles playback.
     */
    private PhoneStateListener mPhoneCallListener = new PhoneStateListener() {

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
    };

    // Stops this service if it doesn't have any bound services
    private KillTimerHandler mKillTimerHandler = new KillTimerHandler(this);

    private static class KillTimerHandler extends WeakReferenceHandler<PlaybackService> {

        public KillTimerHandler(PlaybackService referencedObject) {
            super(referencedObject);
        }

        @Override
        public void handleMessage(Message msg) {
            PlaybackService service = getReferencedObject();
            if (service != null) {
                if (service.isPlaying()) {
                    removeCallbacksAndMessages(null);
                    Message msgx = obtainMessage();
                    sendMessageDelayed(msgx, DELAY_TO_KILL);
                    Log.d(TAG, "Killtimer checked if I should die, but I survived *cheer*");
                } else {
                    Log.d(TAG, "Killtimer called stopSelf() on me");
                    service.stopSelf();
                }
            }
        }
    }

    private TomahawkMediaPlayerCallback mMediaPlayerCallback = new TomahawkMediaPlayerCallback() {
        @Override
        public void onPrepared(Query query) {
            if (query == getCurrentQuery()) {
                Log.d(TAG, "MediaPlayer successfully prepared the track '"
                        + getCurrentQuery().getName() + "' by '"
                        + getCurrentQuery().getArtist().getName()
                        + "' resolved by Resolver " + getCurrentQuery()
                        .getPreferredTrackResult().getResolvedBy().getId());
                boolean allPlayersReleased = true;
                for (TomahawkMediaPlayer mediaPlayer : mMediaPlayers.values()) {
                    if (!mediaPlayer.isPrepared(getCurrentQuery())) {
                        mediaPlayer.release();
                    } else {
                        allPlayersReleased = false;
                    }
                }
                if (allPlayersReleased) {
                    prepareCurrentQuery();
                } else if (isPlaying()) {
                    InfoSystem.get().sendNowPlayingPostStruct(
                            AuthenticatorManager.get().getAuthenticatorUtils(
                                    TomahawkApp.PLUGINNAME_HATCHET),
                            getCurrentQuery()
                    );
                }
                handlePlayState();
            } else {
                String queryInfo;
                if (query != null) {
                    queryInfo = query.getName() + "' by '" + query.getArtist().getName()
                            + "' resolved by Resolver "
                            + query.getPreferredTrackResult().getResolvedBy().getId();
                } else {
                    queryInfo = "null";
                }
                Log.e(TAG, "onPrepared received for an unexpected Query: " + queryInfo);
            }
        }

        @Override
        public void onCompletion(Query query) {
            if (query == getCurrentQuery()) {
                Log.d(TAG, "onCompletion");
                if (hasNextEntry()) {
                    next();
                } else {
                    pause();
                }
            }
        }

        @Override
        public void onError(final String message) {
            Log.e(TAG, "onError - " + message);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(TomahawkApp.getContext(), message, Toast.LENGTH_LONG).show();
                }
            });
            giveUpAudioFocus();
            if (hasNextEntry()) {
                next();
            } else {
                pause();
            }
        }
    };

    @SuppressWarnings("unused")
    public void onEventMainThread(PipeLine.ResultsEvent event) {
        if (getCurrentQuery() != null && getCurrentQuery() == event.mQuery) {
            updateNotification();
            updateLockscreenControls();
            EventBus.getDefault().post(new PlayingTrackChangedEvent());
            if (mCurrentMediaPlayer == null
                    || !(mCurrentMediaPlayer.isPrepared(getCurrentQuery())
                    || mCurrentMediaPlayer.isPreparing(getCurrentQuery()))) {
                prepareCurrentQuery();
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(InfoSystem.ResultsEvent event) {
        if (getCurrentEntry() != null && getCurrentQuery().getCacheKey()
                .equals(mCorrespondingRequestIds.get(event.mInfoRequestData.getRequestId()))) {
            updateNotification();
            updateLockscreenControls();
            EventBus.getDefault().post(new PlayingTrackChangedEvent());
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(CollectionManager.UpdatedEvent event) {
        if (event.mUpdatedItemIds != null && getCurrentQuery() != null
                && event.mUpdatedItemIds.contains(getCurrentQuery().getCacheKey())) {
            updateNotification();
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(RequestServiceBindingEvent event) {
        Intent intent = new Intent(event.mServicePackageName + ".BindToService");
        intent.setPackage(event.mServicePackageName);
        bindService(intent, event.mConnection, Context.BIND_AUTO_CREATE);
    }

    @SuppressWarnings("unused")
    public void onEvent(SetBitrateEvent event) {
        setBitrate(event.mode);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        EventBus.getDefault().register(this);

        mMediaPlayers.put(VLCMediaPlayer.class, new VLCMediaPlayer());
        mMediaPlayers.put(DeezerMediaPlayer.class, new DeezerMediaPlayer());
        mMediaPlayers.put(SpotifyMediaPlayer.class, new SpotifyMediaPlayer());

        startService(new Intent(this, MicroService.class));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mRemoteControllerConnection = new RemoteControllerConnection();
            bindService(new Intent(this, RemoteControllerService.class),
                    mRemoteControllerConnection, Context.BIND_AUTO_CREATE);
        }

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // Initialize PhoneCallListener
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneCallListener, PhoneStateListener.LISTEN_CALL_STATE);

        // Initialize WakeLock
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), new AudioFocusable() {
            @Override
            public void onGainedAudioFocus() {
                //TODO
            }

            @Override
            public void onLostAudioFocus(boolean canDuck) {
                //TODO
            }
        });

        // Initialize killtime handler (watchdog style)
        mKillTimerHandler.removeCallbacksAndMessages(null);
        Message msg = mKillTimerHandler.obtainMessage();
        mKillTimerHandler.sendMessageDelayed(msg, DELAY_TO_KILL);

        mPlaylist =
                Playlist.fromEmptyList(TomahawkMainActivity.getLifetimeUniqueStringId(), false, "");
        mQueue =
                Playlist.fromEmptyList(TomahawkMainActivity.getLifetimeUniqueStringId(), false, "");
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
            } else if (intent.getAction().equals(ACTION_FAVORITE)) {
                CollectionManager.get().toggleLovedItem(getCurrentQuery());
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

        EventBus.getDefault().unregister(this);

        pause(true);
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
        mMediaSessionCompat.setCallback(null);
        mMediaSessionCompat.release();
        mMediaSessionCompat = null;

        if (mRemoteControllerConnection != null) {
            unbindService(mRemoteControllerConnection);
        }

        for (TomahawkMediaPlayer mp : mMediaPlayers.values()) {
            if (mp instanceof PluginMediaPlayer) {
                PluginMediaPlayer pmp = (PluginMediaPlayer) mp;
                if (pmp.isBound()) {
                    pmp.setService(null);
                    unbindService(pmp.getServiceConnection());
                }
            }
        }

        Log.d(TAG, "PlaybackService has been destroyed");
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
            EventBus.getDefault().post(new PlayStateChangedEvent());
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
        EventBus.getDefault().post(new PlayStateChangedEvent());
        handlePlayState();
        if (dismissNotificationOnPause) {
            mShowingNotification = false;
            stopForeground(true);
            giveUpAudioFocus();
            NotificationManager notificationManager = (NotificationManager) TomahawkApp.getContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(PLAYBACKSERVICE_NOTIFICATION_ID);
        } else {
            updateNotification();
        }
        updateLockscreenPlayState();
    }

    /**
     * Update the TomahawkMediaPlayer so that it reflects the current playState
     */
    public void handlePlayState() {
        Log.d(TAG, "handlePlayState");
        if (!isPreparing() && getCurrentQuery() != null
                && getCurrentQuery().getMediaPlayerClass() != null) {
            try {
                TomahawkMediaPlayer mp = mMediaPlayers.get(getCurrentQuery().getMediaPlayerClass());
                switch (mPlayState) {
                    case PLAYBACKSERVICE_PLAYSTATE_PLAYING:
                        if (mWakeLock != null && mWakeLock.isHeld()) {
                            mWakeLock.acquire();
                        }
                        if (mp.isPrepared(getCurrentQuery())) {
                            if (!mp.isPlaying(getCurrentQuery())) {
                                mp.start();
                            }
                        } else if (!isPreparing()) {
                            prepareCurrentQuery();
                        }
                        break;
                    case PLAYBACKSERVICE_PLAYSTATE_PAUSED:
                        if (mp.isPlaying(getCurrentQuery()) && mp.isPrepared(getCurrentQuery())) {
                            InfoSystem.get().sendPlaybackEntryPostStruct(
                                    AuthenticatorManager.get().getAuthenticatorUtils(
                                            TomahawkApp.PLUGINNAME_HATCHET)
                            );
                            mp.pause();
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
        int counter = 0;
        PlaylistEntry entry = getNextEntry();
        while (entry != null && counter++ < getPlaybackListSize()) {
            setCurrentEntry(entry);
            if (entry.getQuery().isPlayable()) {
                break;
            }
            entry = getNextEntry(entry);
        }
    }

    /**
     * Play the previous track.
     */
    public void previous() {
        Log.d(TAG, "previous");
        int counter = 0;
        PlaylistEntry entry = getPreviousEntry();
        while (entry != null && counter++ < getPlaybackListSize()) {
            if (entry.getQuery().isPlayable()) {
                setCurrentEntry(entry);
                break;
            }
            entry = getPreviousEntry(entry);
        }
    }

    public boolean isShuffled() {
        return mPlaylist.isShuffled();
    }

    /**
     * Set whether or not to enable shuffle mode on the current playlist.
     */
    public void setShuffled(boolean shuffled) {
        Log.d(TAG, "setShuffled from " + mPlaylist.isShuffled() + " to " + shuffled);
        if (mPlaylist.isShuffled() != shuffled) {
            boolean isPlayingFromQueue = mQueue.getIndexOfEntry(mCurrentEntry) >= 0;
            int currentIndex = -1;
            if (!isPlayingFromQueue) {
                currentIndex = mPlaylist.getIndexOfEntry(mCurrentEntry);
            }
            int newCurrentIndex;
            if (shuffled) {
                newCurrentIndex = mPlaylist.setShuffled(true, currentIndex);
            } else {
                newCurrentIndex = mPlaylist.setShuffled(false, currentIndex);
            }
            mCurrentIndex = newCurrentIndex;
            if (mQueue.size() > 0) {
                mQueueStartPos = mCurrentIndex + 1;
            } else {
                mQueueStartPos = -1;
            }
            if (getCurrentEntry() != null) {
                resolveQueriesFromTo(mCurrentIndex, mCurrentIndex + 10);
            }

            EventBus.getDefault().post(new PlayingPlaylistChangedEvent());
        }
    }

    public int getRepeatingMode() {
        return mRepeatingMode;
    }

    /**
     * Set the repeat mode on the current playlist.
     */
    public void setRepeatingMode(int repeatingMode) {
        Log.d(TAG, "setRepeatingMode to " + repeatingMode);
        mRepeatingMode = repeatingMode;
        EventBus.getDefault().post(new PlayingPlaylistChangedEvent());
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
        if (getCurrentQuery() != null && getCurrentQuery().getMediaPlayerClass() != null) {
            TomahawkMediaPlayer mp = mMediaPlayers.get(getCurrentQuery().getMediaPlayerClass());
            return mp.isPreparing(getCurrentQuery());
        }
        return false;
    }

    /**
     * Get the current PlaylistEntry
     */
    public PlaylistEntry getCurrentEntry() {
        return mCurrentEntry;
    }

    public PlaylistEntry getNextEntry() {
        return getNextEntry(mCurrentEntry);
    }

    public PlaylistEntry getNextEntry(PlaylistEntry entry) {
        if (mRepeatingMode == REPEAT_ONE) {
            return entry;
        }
        int index = getPlaybackListIndex(entry);
        PlaylistEntry nextEntry = getPlaybackListEntry(index + 1);
        if (nextEntry == null && mRepeatingMode == REPEAT_ALL) {
            nextEntry = getPlaybackListEntry(0);
        }
        return nextEntry;
    }

    public boolean hasNextEntry() {
        return hasNextEntry(mCurrentEntry);
    }

    public boolean hasNextEntry(PlaylistEntry entry) {
        return mRepeatingMode == REPEAT_ONE || getNextEntry(entry) != null;
    }

    public PlaylistEntry getPreviousEntry() {
        return getPreviousEntry(mCurrentEntry);
    }

    public PlaylistEntry getPreviousEntry(PlaylistEntry entry) {
        if (mRepeatingMode == REPEAT_ONE) {
            return entry;
        }
        int index = getPlaybackListIndex(entry);
        PlaylistEntry previousEntry = getPlaybackListEntry(index - 1);
        if (previousEntry == null && mRepeatingMode == REPEAT_ALL) {
            previousEntry = getPlaybackListEntry(getPlaybackListSize() - 1);
        }
        return previousEntry;
    }

    public boolean hasPreviousEntry() {
        return hasPreviousEntry(mCurrentEntry);
    }

    public boolean hasPreviousEntry(PlaylistEntry entry) {
        return mRepeatingMode == REPEAT_ONE || getPreviousEntry(entry) != null;
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
                    String requestId = InfoSystem.get().resolve(
                            getCurrentQuery().getArtist(), false);
                    if (requestId != null) {
                        mCorrespondingRequestIds.put(requestId, getCurrentQuery().getCacheKey());
                    }
                    requestId = InfoSystem.get().resolve(getCurrentQuery().getAlbum());
                    if (requestId != null) {
                        mCorrespondingRequestIds.put(requestId, getCurrentQuery().getCacheKey());
                    }
                }

                TomahawkRunnable r = new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_PLAYBACK) {
                    @Override
                    public void run() {
                        if (isPlaying() && getCurrentQuery().getMediaPlayerClass() != null) {
                            TomahawkMediaPlayer mp =
                                    mMediaPlayers.get(getCurrentQuery().getMediaPlayerClass());
                            if (mp.prepare(getCurrentQuery(), mMediaPlayerCallback) == null) {
                                boolean isNetworkAvailable = isNetworkAvailable();
                                if (isNetworkAvailable
                                        && getCurrentQuery().getPreferredTrackResult() != null) {
                                    getCurrentQuery().blacklistTrackResult(
                                            getCurrentQuery().getPreferredTrackResult());
                                    EventBus.getDefault().post(new PlayingPlaylistChangedEvent());
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
                                mCurrentMediaPlayer = mp;
                            }
                        }
                    }
                };
                ThreadManager.get().executePlayback(r);
            } else {
                next();
            }
        }
    }

    public void setCurrentEntry(PlaylistEntry entry) {
        Log.d(TAG, "setCurrentEntry to " + entry.getId());
        releaseAllPlayers();
        if (mCurrentEntry != null) {
            deleteQueryInQueue(mCurrentEntry);
        }
        mCurrentEntry = entry;
        mCurrentIndex = getPlaybackListIndex(mCurrentEntry);
        handlePlayState();
        EventBus.getDefault().post(new PlayingPlaylistChangedEvent());
        onTrackChanged();
    }

    private void onTrackChanged() {
        Log.d(TAG, "onTrackChanged");
        EventBus.getDefault().post(new PlayingTrackChangedEvent());
        if (getCurrentEntry() != null) {
            resolveQueriesFromTo(mCurrentIndex, mCurrentIndex - 2 + 10);
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
        return mPlaylist;
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
        mRepeatingMode = NOT_REPEATING;
        mPlaylist = playlist;
        setCurrentEntry(currentEntry);
        if (mQueue.size() > 0) {
            mQueueStartPos = mCurrentIndex + 1;
        } else {
            mQueueStartPos = -1;
        }
    }

    public int getQueueStartPos() {
        return mQueueStartPos;
    }

    /**
     * @param position int containing the position in the current playback list
     * @return the {@link PlaylistEntry} which has been found at the given position
     */
    public PlaylistEntry getPlaybackListEntry(int position) {
        if (position < mQueueStartPos) {
            // The requested entry is positioned before the queue
            return mPlaylist.getEntryAtPos(position);
        } else {
            if (position < mQueueStartPos + mQueue.size()) {
                // Getting the entry from the queue
                return mQueue.getEntryAtPos(position - mQueueStartPos);
            } else {
                // The requested entry is positioned after the queue
                return mPlaylist.getEntryAtPos(position - mQueue.size());
            }
        }
    }

    /**
     * @param entry The {@link PlaylistEntry} to get the index for
     * @return an int containing the index of the given {@link PlaylistEntry} inside the current
     * playback list
     */
    public int getPlaybackListIndex(PlaylistEntry entry) {
        int index = mQueue.getIndexOfEntry(entry);
        if (index >= 0) {
            // Found entry in queue
            return index + mQueueStartPos;
        } else {
            index = mPlaylist.getIndexOfEntry(entry);
            if (index < 0) {
                Log.e(TAG,
                        "getPlaybackListIndex - Couldn't find given entry in mQueue or mPlaylist.");
                return -1;
            }
            if (index < mQueueStartPos) {
                // Found entry and its positioned before the queue
                return index;
            } else {
                // Found entry and its positioned after the queue
                return index + mQueue.size();
            }
        }
    }

    public int getPlaybackListSize() {
        return mQueue.size() + mPlaylist.size();
    }

    /**
     * Add given {@link org.tomahawk.libtomahawk.resolver.Query} to the Queue
     */
    public void addQueryToQueue(Query query) {
        Log.d(TAG, "addQueryToQueue " + query.getName());
        if (mQueue.size() == 0) {
            mQueueStartPos = mCurrentIndex + 1;
        }
        mQueue.addQuery(0, query);
        if (getCurrentEntry() == null) {
            setCurrentEntry(getPlaybackListEntry(0));
        } else {
            EventBus.getDefault().post(new PlayingPlaylistChangedEvent());
            onTrackChanged();
        }
    }

    /**
     * Add given {@link List} of {@link org.tomahawk.libtomahawk.resolver.Query}s to the Queue
     */
    public void addQueriesToQueue(List<Query> queries) {
        Log.d(TAG, "addQueriesToQueue count: " + queries.size());
        for (Query query : queries) {
            addQueryToQueue(query);
        }
    }

    public void deleteQueryInQueue(PlaylistEntry entry) {
        Log.d(TAG, "deleteQueryInQueue");
        if (mQueue.deleteEntry(entry)) {
            EventBus.getDefault().post(new PlayingPlaylistChangedEvent());
            onTrackChanged();
        }
    }

    /**
     * Returns the position of playback in the current Track.
     */
    public int getPosition() {
        int position = 0;
        if (getCurrentQuery() != null && getCurrentQuery().getMediaPlayerClass() != null) {
            try {
                TomahawkMediaPlayer mp =
                        mMediaPlayers.get(getCurrentQuery().getMediaPlayerClass());
                position = mp.getPosition();
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
        if (getCurrentQuery() != null && getCurrentQuery().getMediaPlayerClass() != null) {
            TomahawkMediaPlayer mp =
                    mMediaPlayers.get(getCurrentQuery().getMediaPlayerClass());
            if (mp.isPrepared(getCurrentQuery())) {
                mp.seekTo(msec);
            }
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
            intent = new Intent(ACTION_FAVORITE, null, PlaybackService.this, PlaybackService.class);
            PendingIntent favoritePendingIntent = PendingIntent
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
                                R.drawable.ic_av_pause);
            } else {
                mSmallNotificationView
                        .setImageViewResource(R.id.notification_small_imageview_playpause,
                                R.drawable.ic_av_play_arrow);
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
            notificationIntent.setAction(TomahawkMainActivity.SHOW_PLAYBACKFRAGMENT_ON_STARTUP);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
                                    R.drawable.ic_av_pause);
                } else {
                    mLargeNotificationView
                            .setImageViewResource(R.id.notification_large_imageview_playpause,
                                    R.drawable.ic_av_play_arrow);
                }
                if (DatabaseHelper.get().isItemLoved(getCurrentQuery())) {
                    mLargeNotificationView
                            .setImageViewResource(R.id.notification_large_imageview_favorite,
                                    R.drawable.ic_action_favorites_underlined);
                } else {
                    mLargeNotificationView
                            .setImageViewResource(R.id.notification_large_imageview_favorite,
                                    R.drawable.ic_action_favorites);
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
                        .setOnClickPendingIntent(R.id.notification_large_imageview_favorite,
                                favoritePendingIntent);
                mLargeNotificationView
                        .setOnClickPendingIntent(R.id.notification_large_imageview_exit,
                                exitPendingIntent);
                mNotification.bigContentView = mLargeNotificationView;

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.loadImageIntoNotification(TomahawkApp.getContext(),
                                getCurrentQuery().getImage(), mSmallNotificationView,
                                R.id.notification_small_imageview_albumart,
                                PLAYBACKSERVICE_NOTIFICATION_ID,
                                mNotification, Image.getSmallImageSize(),
                                getCurrentQuery().hasArtistImage());
                        ImageUtils.loadImageIntoNotification(TomahawkApp.getContext(),
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
     * Update the playback controls/views which are being shown on the lockscreen
     */
    private void updateLockscreenControls() {
        Log.d(TAG, "updateLockscreenControls()");

        if (mMediaSessionCompat == null) {
            ComponentName componentName = new ComponentName(this, MediaButtonReceiver.class);
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            intent.setComponent(componentName);
            mMediaSessionCompat = new MediaSessionCompat(
                    getApplicationContext(), "Tomahawk", componentName, null);
            mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
            intent = new Intent(PlaybackService.this, TomahawkMainActivity.class);
            intent.setAction(TomahawkMainActivity.SHOW_PLAYBACKFRAGMENT_ON_STARTUP);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(PlaybackService.this, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);
            mMediaSessionCompat.setSessionActivity(pendingIntent);
            mMediaSessionCompat.setCallback(mMediaSessionCallback);
            mTransportControls = mMediaSessionCompat.getController().getTransportControls();
        }

        updateLockscreenPlayState();
        mMediaSessionCompat.setActive(true);

        // Update the remote controls
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST,
                        getCurrentQuery().getArtist().getPrettyName())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                        getCurrentQuery().getArtist().getPrettyName())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                        getCurrentQuery().getPrettyName())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                        getCurrentQuery().getPreferredTrack().getDuration());
        if (!TextUtils.isEmpty(getCurrentQuery().getAlbum().getPrettyName())) {
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                    getCurrentQuery().getAlbum().getPrettyName());
        }
        mMediaSessionCompat.setMetadata(builder.build());
        Log.d(TAG, "Setting lockscreen metadata to: "
                + getCurrentQuery().getArtist().getPrettyName() + ", "
                + getCurrentQuery().getPrettyName());

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Picasso.with(TomahawkApp.getContext()).cancelRequest(mLockscreenTarget);
                ImageUtils.loadImageIntoBitmap(TomahawkApp.getContext(),
                        getCurrentQuery().getImage(), mLockscreenTarget,
                        Image.getLargeImageSize(), getCurrentQuery().hasArtistImage());
            }
        });
    }

    /**
     * Create or update an ongoing notification
     */
    public void updateLockscreenPlayState() {
        if (getCurrentQuery() != null) {
            Log.d(TAG, "updateLockscreenPlayState()");
            long actions = PlaybackStateCompat.ACTION_PLAY |
                    PlaybackStateCompat.ACTION_PAUSE |
                    PlaybackStateCompat.ACTION_STOP |
                    PlaybackStateCompat.ACTION_SEEK_TO;
            if (hasNextEntry()) {
                actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
            }
            if (hasPreviousEntry()) {
                actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
            }
            PlaybackStateCompat playbackStateCompat = new PlaybackStateCompat.Builder()
                    .setActions(actions)
                    .setState(isPlaying() ? PlaybackStateCompat.STATE_PLAYING
                            : PlaybackStateCompat.STATE_PAUSED, getPosition(), 1f)
                    .build();
            mMediaSessionCompat.setPlaybackState(playbackStateCompat);
        }
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

    private void resolveQueriesFromTo(int start, int end) {
        Set<Query> qs = new HashSet<>();
        for (int i = start; i < end; i++) {
            if (i >= 0 && i < getPlaybackListSize()) {
                Query q = getPlaybackListEntry(i).getQuery();
                if (!mCorrespondingQueries.contains(q)) {
                    qs.add(q);
                }
            }
        }
        if (!qs.isEmpty()) {
            HashSet<Query> queries = PipeLine.get().resolve(qs);
            mCorrespondingQueries.addAll(queries);
        }
    }

    private void releaseAllPlayers() {
        for (TomahawkMediaPlayer mp : mMediaPlayers.values()) {
            mp.release();
        }
    }

    public void setBitrate(int mode) {
        for (TomahawkMediaPlayer mp : mMediaPlayers.values()) {
            mp.setBitrate(mode);
        }
    }
}
