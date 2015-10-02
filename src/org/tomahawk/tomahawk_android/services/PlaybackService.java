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

import org.tomahawk.aidl.IPluginService;
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
import org.tomahawk.tomahawk_android.mediaplayers.RdioMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.SpotifyMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.TomahawkMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.TomahawkMediaPlayerCallback;
import org.tomahawk.tomahawk_android.mediaplayers.VLCMediaPlayer;
import org.tomahawk.tomahawk_android.utils.AudioFocusHelper;
import org.tomahawk.tomahawk_android.utils.MediaButtonHelper;
import org.tomahawk.tomahawk_android.utils.MediaButtonReceiver;
import org.tomahawk.tomahawk_android.utils.MusicFocusable;
import org.tomahawk.tomahawk_android.utils.RemoteControlClientCompat;
import org.tomahawk.tomahawk_android.utils.RemoteControlHelper;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;
import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;

import android.annotation.TargetApi;
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
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

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
public class PlaybackService extends Service implements MusicFocusable {

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

    public static final String MERGED_PLAYLIST_ID = "merged_playlist_id";

    public static final String SHUFFLED_PLAYLIST_ID = "shuffled_playlist_id";

    // The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
    public static final float DUCK_VOLUME = 0.1f;

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

        private PluginMediaPlayer mRequestingPlayer;

        private String mServicePackageName;

        public RequestServiceBindingEvent(PluginMediaPlayer requestingPlayer,
                String servicePackageName) {
            mRequestingPlayer = requestingPlayer;
            mServicePackageName = servicePackageName;
        }

    }

    private boolean mShowingNotification;

    protected final Set<Query> mCorrespondingQueries
            = Collections.newSetFromMap(new ConcurrentHashMap<Query, Boolean>());

    protected final ConcurrentHashMap<String, String> mCorrespondingRequestIds
            = new ConcurrentHashMap<>();

    private Playlist mPlaylist;

    private Playlist mQueue;

    private List<Integer> mShuffledIndex = new ArrayList<>();

    private int mQueueStartPos = -1;

    private PlaylistEntry mCurrentEntry;

    private int mCurrentIndex;

    private TomahawkMediaPlayer mCurrentMediaPlayer;

    private Notification mNotification;

    private RemoteViews mLargeNotificationView;

    private RemoteViews mSmallNotificationView;

    private PowerManager.WakeLock mWakeLock;

    private boolean mShuffled;

    private int mRepeatingMode = NOT_REPEATING;

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

    private final List<TomahawkMediaPlayer> mMediaPlayers = new ArrayList<>();

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

        private final PlaybackServiceConnectionListener mPlaybackServiceConnectionListener;

        public interface PlaybackServiceConnectionListener {

            void setPlaybackService(PlaybackService ps);

            void onPlaybackServiceReady();
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

    private class PluginServiceConnection implements ServiceConnection {

        private PluginMediaPlayer mPluginMediaPlayer;

        public PluginServiceConnection(PluginMediaPlayer pluginMediaPlayer) {
            mPluginMediaPlayer = pluginMediaPlayer;
        }

        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            IPluginService pluginService = IPluginService.Stub.asInterface(service);
            mPluginMediaPlayer.setService(pluginService);

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                pluginService.registerCallback(mPluginMediaPlayer);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mPluginMediaPlayer.setService(null);
        }
    }

    ;

    public class PlaybackServiceBinder extends Binder {

        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

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
                for (TomahawkMediaPlayer mediaPlayer : mMediaPlayers) {
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
                Log.e(TAG, "onPrepared received for an unexpected Query: "
                        + query.getName() + "' by '" + query.getArtist().getName()
                        + "' resolved by Resolver "
                        + query.getPreferredTrackResult().getResolvedBy().getId());
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
        public void onError(String message) {
            Log.e(TAG, "onError - " + message);
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
        Intent intent = new Intent(IPluginService.class.getName());
        intent.setPackage(event.mServicePackageName);
        bindService(intent, new PluginServiceConnection(event.mRequestingPlayer),
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        EventBus.getDefault().register(this);

        mMediaPlayers.add(VLCMediaPlayer.get());
        mMediaPlayers.add(DeezerMediaPlayer.get());
        mMediaPlayers.add(SpotifyMediaPlayer.get());
        mMediaPlayers.add(RdioMediaPlayer.get());

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

        mMediaButtonReceiverComponent = new ComponentName(this, MediaButtonReceiver.class);
        mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);

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
                            InfoSystem.get().sendPlaybackEntryPostStruct(
                                    AuthenticatorManager.get().getAuthenticatorUtils(
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
                fillShuffledIndex();
            }
            if (getCurrentEntry() != null) {
                resolveQueriesFromTo(mCurrentIndex, mCurrentIndex + 10);
            }

            EventBus.getDefault().post(new PlayingPlaylistChangedEvent());
        }
    }

    /**
     * Shuffles the list of tracks in the current playlist and fills the shuffled index accordingly.
     * The shuffle method ensures that the shuffled list does only contain a minimum amount of
     * tracks by the same artist in sequence.
     */
    private void fillShuffledIndex() {
        mShuffledIndex.clear();
        Map<String, List<Integer>> artistMap = new HashMap<>();
        List<String> artistNames = new ArrayList<>();
        int shuffledTracksCount = 0;
        boolean isPlayingFromQueue = mQueue.getIndexOfEntry(mCurrentEntry) >= 0;
        int currentIndex = -1;
        if (isPlayingFromQueue) {
            currentIndex = mPlaylist.getIndexOfEntry(mCurrentEntry);
        }
        for (int i = 0; i < mPlaylist.size(); i++) {
            if (isPlayingFromQueue || i != currentIndex) {
                String artistName = mPlaylist.getArtistName(i);
                if (artistMap.get(artistName) == null) {
                    artistMap.put(artistName, new ArrayList<Integer>());
                    artistNames.add(artistName);
                }
                artistMap.get(artistName).add(i);
                shuffledTracksCount++;
            }
        }
        if (!isPlayingFromQueue) {
            mShuffledIndex.add(mCurrentIndex);
        }
        String lastArtistName = null;
        while (shuffledTracksCount >= 0) {
            // Get a random artistName out of all available ones
            String artistName = null;
            int tryCount = 0;
            while (tryCount++ < 3 && (artistName == null || artistName.equals(lastArtistName))) {
                // We try 3 times to get an artistName that is different from the one we picked
                // previously
                int randomPos = (int) (Math.random() * artistNames.size());
                artistName = artistNames.get(randomPos);
            }
            // Now we can get the list of track indexes
            List<Integer> indexes = artistMap.get(artistName);
            int randomPos = (int) (Math.random() * indexes.size());
            // Add the randomly picked track index to our shuffled index
            mShuffledIndex.add(indexes.get(randomPos));
            shuffledTracksCount--;
            lastArtistName = artistName;
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
                        if (isPlaying() && getCurrentQuery().getMediaPlayerInterface() != null) {
                            if (getCurrentQuery().getMediaPlayerInterface().prepare(
                                    getApplication(), getCurrentQuery(), mMediaPlayerCallback)
                                    == null) {
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
                                mCurrentMediaPlayer = getCurrentQuery().getMediaPlayerInterface();
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
        mShuffled = false;
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
            return getPlaylistEntry(position);
        } else {
            if (position < mQueueStartPos + mQueue.size()) {
                // Getting the entry from the queue
                return mQueue.getEntryAtPos(position - mQueueStartPos);
            } else {
                // The requested entry is positioned after the queue
                return getPlaylistEntry(position - mQueue.size());
            }
        }
    }

    /**
     * Private helper method that makes sure that the shuffled playlist is being used if needed.
     */
    private PlaylistEntry getPlaylistEntry(int position) {
        if (mShuffled) {
            int newPos = mShuffledIndex.get(position);
            return mPlaylist.getEntryAtPos(newPos);
        } else {
            return mPlaylist.getEntryAtPos(position);
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
        EventBus.getDefault().post(new PlayingPlaylistChangedEvent());
        onTrackChanged();
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
                        getCurrentQuery().getArtist().getPrettyName())
                        .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST,
                                getCurrentQuery().getArtist().getPrettyName())
                        .putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
                                getCurrentQuery().getPrettyName())
                        .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                                getCurrentQuery().getPreferredTrack().getDuration());
                if (!TextUtils.isEmpty(getCurrentQuery().getAlbum().getPrettyName())) {
                    editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,
                            getCurrentQuery().getAlbum().getPrettyName());
                }
                editor.apply();
                Log.d(TAG, "Setting lockscreen metadata to: "
                        + getCurrentQuery().getArtist().getPrettyName() + ", "
                        + getCurrentQuery().getPrettyName());
            }

            Picasso.with(TomahawkApp.getContext()).cancelRequest(mLockscreenTarget);
            ImageUtils.loadImageIntoBitmap(TomahawkApp.getContext(),
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
        VLCMediaPlayer.get().release();
        SpotifyMediaPlayer.get().release();
        RdioMediaPlayer.get().release();
        DeezerMediaPlayer.get().release();
    }
}
