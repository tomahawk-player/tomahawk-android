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
import org.tomahawk.libtomahawk.collection.StationPlaylist;
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
import org.tomahawk.tomahawk_android.utils.IdGenerator;
import org.tomahawk.tomahawk_android.utils.MediaNotification;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;
import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;

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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.widget.Toast;

import java.util.ArrayList;
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
public class PlaybackService extends MediaBrowserServiceCompat {

    private static final String TAG = PlaybackService.class.getSimpleName();

    public static final String ACTION_PLAY = "org.tomahawk.tomahawk_android.ACTION_PLAY";

    public static final int NOT_REPEATING = 0;

    public static final int REPEAT_ALL = 1;

    public static final int REPEAT_ONE = 2;

    private static final int MAX_ALBUM_ART_CACHE_SIZE = 5 * 1024 * 1024;

    private int mPlayState = PlaybackStateCompat.STATE_NONE;

    private static final int DELAY_SCROBBLE = 15000;

    private static final int DELAY_UNBIND_PLUGINSERVICES = 1800000;

    private static final int DELAY_SUICIDE = 1800000;

    public static final String MEDIA_ID_ROOT = "__ROOT__";

    public static final String MEDIA_ID_ALBUMS = "__ALBUMS__";

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

    public static class HeadsetPluggedInEvent {

    }

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

    private MediaNotification mNotification;

    private PowerManager.WakeLock mWakeLock;

    private int mRepeatingMode = NOT_REPEATING;

    private MediaSessionCompat mMediaSession;

    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {

        /**
         * Override to handle requests to begin playback.
         */
        @Override
        public void onPlay() {
            play();
        }

        /**
         * Override to handle requests to pause playback.
         */
        @Override
        public void onPause() {
            pause();
        }

        /**
         * Override to handle requests to skip to the next media item.
         */
        @Override
        public void onSkipToNext() {
            next();
        }

        /**
         * Override to handle requests to skip to the previous media item.
         */
        @Override
        public void onSkipToPrevious() {
            previous();
        }

        /**
         * Override to handle requests to fast forward.
         */
        @Override
        public void onFastForward() {
            long duration = getCurrentQuery().getPreferredTrack().getDuration();
            int newPos = (int) Math.min(duration, Math.max(0, getPosition() + 10000));
            seekTo(newPos);
        }

        /**
         * Override to handle requests to rewind.
         */
        @Override
        public void onRewind() {
            long duration = getCurrentQuery().getPreferredTrack().getDuration();
            int newPos = (int) Math.min(duration, Math.max(0, getPosition() - 10000));
            seekTo(newPos);
        }

        /**
         * Override to handle requests to stop playback.
         */
        @Override
        public void onStop() {
            pause();
        }

        /**
         * Override to handle requests to seek to a specific position in ms.
         *
         * @param pos New position to move to, in milliseconds.
         */
        @Override
        public void onSeekTo(long pos) {
            seekTo((int) pos);
        }

        /**
         * Override to handle the item being rated.
         *
         * @param rating
         */
        @Override
        public void onSetRating(RatingCompat rating) {
            if (rating.getRatingStyle() == RatingCompat.RATING_HEART) {
                CollectionManager.get().setLovedItem(getCurrentQuery(), rating.hasHeart());
                updateMediaMetadata();
            }
        }

    };

    private boolean isNoisyReceiverRegistered = false;

    private BroadcastReceiver mAudioBecomingNoisyReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // AudioManager tells us that the sound will be played through the speaker
                Log.d(TAG, "Action audio becoming noisy, pausing ...");
                // So we stop playback, if needed
                pause();
            }
        }
    };

    // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    private AudioFocusHelper mAudioFocusHelper = null;

    // do we have audio focus?
    private enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    private AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    private final Map<Class, TomahawkMediaPlayer> mMediaPlayers = new HashMap<>();

    private final LruCache<Image, Bitmap> mMediaImageCache =
            new LruCache<Image, Bitmap>(MAX_ALBUM_ART_CACHE_SIZE) {
                @Override
                protected int sizeOf(Image key, Bitmap value) {
                    return value.getByteCount();
                }
            };

    private MediaImageTarget mMediaImageTarget;

    private class MediaImageTarget implements Target {

        private Image mImageToLoad;

        public MediaImageTarget(Image imageToLoad) {
            mImageToLoad = imageToLoad;
        }

        @Override
        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            new Runnable() {
                @Override
                public void run() {
                    if (mMediaSession == null) {
                        Log.e(TAG, "updateAlbumArt failed - mMediaSession == null!");
                        return;
                    }
                    mMediaImageCache.put(mImageToLoad, bitmap.copy(bitmap.getConfig(), false));
                    mMediaSession.setMetadata(buildMetadata());
                    Log.d(TAG, "Setting lockscreen bitmap");
                }
            }.run();
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
        }

        @Override
        public void onPrepareLoad(Drawable drawable) {
        }

    }

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
                        play();
                    }

                    mStartCallTime = 0L;
                    break;
            }
        }
    };

    private SuicideHandler mSuicideHandler = new SuicideHandler(this);

    // Stops this service if it doesn't have any bound services
    private static class SuicideHandler extends DelayedHandler {

        public SuicideHandler(PlaybackService service) {
            super(service, DELAY_SUICIDE);
        }

        @Override
        public void handleMessage(Message msg) {
            if (getReferencedObject() != null) {
                Log.d(TAG, "Killtimer called stopSelf() on me");
                getReferencedObject().stopSelf();
            }
        }
    }

    private PluginServiceKillHandler mPluginServiceKillHandler = new PluginServiceKillHandler(this);

    private static class PluginServiceKillHandler extends DelayedHandler {

        public PluginServiceKillHandler(PlaybackService service) {
            super(service, DELAY_UNBIND_PLUGINSERVICES);
        }

        @Override
        public void handleMessage(Message msg) {
            if (getReferencedObject() != null) {
                Log.d(TAG, "Unbinding from every PluginService...");
                for (TomahawkMediaPlayer mp : getReferencedObject().mMediaPlayers.values()) {
                    if (mp instanceof PluginMediaPlayer) {
                        PluginMediaPlayer pmp = (PluginMediaPlayer) mp;
                        if (pmp.isBound()) {
                            pmp.setService(null);
                            getReferencedObject().unbindService(pmp.getServiceConnection());
                        }
                    }
                }
            }
        }
    }

    private ScrobbleHandler mScrobbleHandler = new ScrobbleHandler(this);

    private static class ScrobbleHandler extends DelayedHandler {

        public ScrobbleHandler(PlaybackService service) {
            super(service, DELAY_SCROBBLE);
        }

        @Override
        public void handleMessage(Message msg) {
            if (getReferencedObject() != null) {
                Log.d(TAG, "Scrobbling delay has passed. Scrobbling...");
                InfoSystem.get().sendNowPlayingPostStruct(
                        AuthenticatorManager.get().getAuthenticatorUtils(
                                TomahawkApp.PLUGINNAME_HATCHET),
                        getReferencedObject().getCurrentQuery()
                );
            }
        }
    }

    private static abstract class DelayedHandler extends WeakReferenceHandler<PlaybackService> {

        private int mDelay;

        private long mStartingTime = 0L;

        private int mDelayReduction = 0;

        public DelayedHandler(PlaybackService referencedObject, int delay) {
            super(referencedObject);

            mDelay = delay;
        }

        public abstract void handleMessage(Message msg);

        public void reset() {
            mDelayReduction = 0;
        }

        public void start() {
            if (mDelay - mDelayReduction > 0) {
                mStartingTime = System.currentTimeMillis();
                removeCallbacksAndMessages(null);
                Message message = obtainMessage();
                sendMessageDelayed(message, mDelay - mDelayReduction);
            }
        }

        public void stop() {
            int delayReduction = (int) (System.currentTimeMillis() - mStartingTime);
            if (delayReduction > 0) {
                mDelayReduction += delayReduction;
            }
            removeCallbacksAndMessages(null);
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
                }
                mScrobbleHandler.reset();
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
            updateMediaMetadata();
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
            updateMediaMetadata();
            EventBus.getDefault().post(new PlayingTrackChangedEvent());
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(CollectionManager.UpdatedEvent event) {
        if (event.mUpdatedItemIds != null && getCurrentQuery() != null
                && event.mUpdatedItemIds.contains(getCurrentQuery().getCacheKey())) {
            updateMediaMetadata();
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

    @SuppressWarnings("unused")
    public void onEvent(StationPlaylist.StationPlayableEvent event) {
        if (mPlaylist == event.mStationPlaylist) {
            setCurrentEntry(mPlaylist.getEntryAtPos(0));
            play();
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(HeadsetPluggedInEvent event) {
        play();
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
            }

            @Override
            public void onLostAudioFocus(boolean canDuck) {
                if (!canDuck) {
                    pause();
                }
            }
        });

        mPlaylist = Playlist.fromEmptyList(IdGenerator.getLifetimeUniqueStringId(), false, "");
        mQueue = Playlist.fromEmptyList(IdGenerator.getLifetimeUniqueStringId(), false, "");

        initMediaSession();

        try {
            mNotification = new MediaNotification(this);
        } catch (RemoteException e) {
            Log.e(TAG, "Could not connect to media controller: ", e);
        }

        Log.d(TAG, "PlaybackService has been created");
    }

    private void initMediaSession() {
        ComponentName componentName = new ComponentName(this, MediaButtonReceiver.class);
        mMediaSession = new MediaSessionCompat(
                getApplicationContext(), "Tomahawk", componentName, null);
        setSessionToken(mMediaSession.getSessionToken());
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        Intent intent = new Intent(PlaybackService.this, TomahawkMainActivity.class);
        intent.setAction(TomahawkMainActivity.SHOW_PLAYBACKFRAGMENT_ON_STARTUP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(PlaybackService.this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mMediaSession.setSessionActivity(pendingIntent);
        mMediaSession.setCallback(mMediaSessionCallback);
        mMediaSession.setRatingType(RatingCompat.RATING_HEART);
        updateMediaPlayState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSession, intent);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Client has been bound to PlaybackService");
        return new PlaybackServiceBinder();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName,
            int clientUid, @Nullable Bundle rootHints) {
        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId,
            @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
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
        mSuicideHandler.stop();
        mSuicideHandler = null;
        mPluginServiceKillHandler.stop();
        mPluginServiceKillHandler = null;
        if (mMediaSession != null) {
            mMediaSession.setCallback(null);
            mMediaSession.release();
            mMediaSession = null;
        }

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
        if (mPlayState == PlaybackStateCompat.STATE_PLAYING) {
            pause(dismissNotificationOnPause);
        } else if (mPlayState == PlaybackStateCompat.STATE_PAUSED) {
            play();
        }
    }

    /**
     * Initial start of playback. Acquires wakelock and creates a notification
     */
    public void play() {
        Log.d(TAG, "play");
        if (getCurrentQuery() != null) {
            if (!isNoisyReceiverRegistered) {
                registerReceiver(mAudioBecomingNoisyReceiver,
                        new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
                isNoisyReceiverRegistered = true;
            }
            mSuicideHandler.stop();
            mPluginServiceKillHandler.stop();
            mScrobbleHandler.start();
            mPlayState = PlaybackStateCompat.STATE_PLAYING;
            EventBus.getDefault().post(new PlayStateChangedEvent());
            handlePlayState();

            mNotification.startNotification();
            tryToGetAudioFocus();
            updateMediaPlayState();
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
        if (isNoisyReceiverRegistered) {
            unregisterReceiver(mAudioBecomingNoisyReceiver);
            isNoisyReceiverRegistered = false;
        }
        mSuicideHandler.start();
        mPluginServiceKillHandler.start();
        mScrobbleHandler.stop();
        mPlayState = PlaybackStateCompat.STATE_PAUSED;
        EventBus.getDefault().post(new PlayStateChangedEvent());
        handlePlayState();
        if (dismissNotificationOnPause) {
            mNotification.stopNotification();
        }
        updateMediaPlayState();
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
                    case PlaybackStateCompat.STATE_PLAYING:
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
                    case PlaybackStateCompat.STATE_PAUSED:
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
        return mPlayState == PlaybackStateCompat.STATE_PLAYING;
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
        if (mPlaylist instanceof StationPlaylist && !hasNextEntry(getNextEntry())) {
            ((StationPlaylist) mPlaylist).fillPlaylist(false);
        }
    }

    private void onTrackChanged() {
        Log.d(TAG, "onTrackChanged");
        EventBus.getDefault().post(new PlayingTrackChangedEvent());
        if (getCurrentEntry() != null) {
            resolveQueriesFromTo(mCurrentIndex, mCurrentIndex - 2 + 10);
            updateMediaMetadata();
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
    public void setPlaylist(StationPlaylist stationPlaylist) {
        Log.d(TAG, "setPlaylist - StationPlaylist");
        stationPlaylist.setPlayedTimeStamp(System.currentTimeMillis());
        DatabaseHelper.get().storeStation(stationPlaylist);
        releaseAllPlayers();
        mRepeatingMode = NOT_REPEATING;
        mPlaylist = stationPlaylist;
        int size = stationPlaylist.size();
        if (size > 0) {
            setCurrentEntry(stationPlaylist.getEntryAtPos(size - 1));
        } else {
            stationPlaylist.fillPlaylist(true);
        }
        if (mQueue.size() > 0) {
            mQueueStartPos = mCurrentIndex + 1;
        } else {
            mQueueStartPos = -1;
        }
    }

    /**
     * Set the current Playlist to playlist and set the current Track to the Playlist's current
     * Track.
     */
    public void setPlaylist(Playlist playlist, PlaylistEntry currentEntry) {
        Log.d(TAG, "setPlaylist - Playlist");
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
     * Update the playback controls/views which are being shown on the lockscreen
     */
    private void updateMediaMetadata() {
        Log.d(TAG, "updateMediaMetadata()");

        if (mMediaSession == null) {
            Log.e(TAG, "updateMediaMetadata failed - mMediaSession == null!");
            return;
        }
        updateMediaPlayState();
        mMediaSession.setActive(true);

        mMediaSession.setMetadata(buildMetadata());
        Log.d(TAG, "Setting lockscreen metadata to: "
                + getCurrentQuery().getArtist().getPrettyName() + ", "
                + getCurrentQuery().getPrettyName());

        mMediaSession.setQueue(buildQueue());
        if (mPlaylist != null) {
            mMediaSession.setQueueTitle(mPlaylist.getName());
        }
    }

    public void updateMediaPlayState() {
        if (mMediaSession == null) {
            Log.e(TAG, "updateMediaPlayState failed - mMediaSession == null!");
            return;
        }
        long actions = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
        if (getCurrentQuery() != null) {
            actions |= PlaybackStateCompat.ACTION_SET_RATING |
                    PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM;
        }
        if (isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE |
                    PlaybackStateCompat.ACTION_SEEK_TO |
                    PlaybackStateCompat.ACTION_FAST_FORWARD |
                    PlaybackStateCompat.ACTION_REWIND;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }
        if (hasNextEntry()) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        }
        if (hasPreviousEntry()) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        }
        Log.d(TAG, "updateMediaPlayState()");
        PlaybackStateCompat playbackStateCompat = new PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(mPlayState, getPosition(), 1f, SystemClock.elapsedRealtime())
                .build();
        mMediaSession.setPlaybackState(playbackStateCompat);
    }

    private MediaMetadataCompat buildMetadata() {
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                        getCurrentEntry().getCacheKey())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST,
                        getCurrentQuery().getArtist().getPrettyName())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                        getCurrentQuery().getArtist().getPrettyName())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                        getCurrentQuery().getPrettyName())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                        getCurrentQuery().getPreferredTrack().getDuration())
                .putRating(MediaMetadataCompat.METADATA_KEY_USER_RATING,
                        RatingCompat.newHeartRating(
                                DatabaseHelper.get().isItemLoved(getCurrentQuery())));
        Bitmap bitmap;
        if (getCurrentQuery().getImage() == null) {
            bitmap =
                    BitmapFactory.decodeResource(getResources(), R.drawable.album_placeholder_grid);
        } else {
            bitmap = mMediaImageCache.get(getCurrentQuery().getImage());
        }
        if (bitmap == null) {
            bitmap =
                    BitmapFactory.decodeResource(getResources(), R.drawable.album_placeholder_grid);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (mMediaImageTarget == null
                            || mMediaImageTarget.mImageToLoad != getCurrentQuery().getImage()) {
                        mMediaImageTarget = new MediaImageTarget(getCurrentQuery().getImage());
                        ImageUtils.loadImageIntoBitmap(TomahawkApp.getContext(),
                                getCurrentQuery().getImage(), mMediaImageTarget,
                                Image.getLargeImageSize(), getCurrentQuery().hasArtistImage());
                    }
                }
            });
        }
        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap);
        if (!TextUtils.isEmpty(getCurrentQuery().getAlbum().getPrettyName())) {
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                    getCurrentQuery().getAlbum().getPrettyName());
        }
        return builder.build();
    }

    private List<MediaSessionCompat.QueueItem> buildQueue() {
        List<MediaSessionCompat.QueueItem> queue = null;
        if (mPlaylist != null) {
            queue = new ArrayList<>();
            int currentIndex = getPlaybackListIndex(getCurrentEntry());
            for (int i = Math.max(0, currentIndex - 10);
                    i < Math.min(getPlaybackListSize(), currentIndex + 40); i++) {
                PlaylistEntry entry = getPlaybackListEntry(i);
                MediaDescriptionCompat.Builder descBuilder = new MediaDescriptionCompat.Builder();
                descBuilder.setMediaId(entry.getCacheKey());
                descBuilder.setTitle(entry.getQuery().getPrettyName());
                descBuilder.setSubtitle(entry.getArtist().getPrettyName());
                MediaSessionCompat.QueueItem item =
                        new MediaSessionCompat.QueueItem(descBuilder.build(), i);
                queue.add(item);
            }
        }
        return queue;
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
