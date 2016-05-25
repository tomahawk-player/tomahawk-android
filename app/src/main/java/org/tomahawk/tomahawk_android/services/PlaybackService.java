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

import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;
import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.StationPlaylist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.ImageUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.mediaplayers.DeezerMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.PluginMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.SpotifyMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.TomahawkMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.TomahawkMediaPlayerCallback;
import org.tomahawk.tomahawk_android.mediaplayers.VLCMediaPlayer;
import org.tomahawk.tomahawk_android.utils.AudioFocusHelper;
import org.tomahawk.tomahawk_android.utils.AudioFocusable;
import org.tomahawk.tomahawk_android.utils.DelayedHandler;
import org.tomahawk.tomahawk_android.utils.IdGenerator;
import org.tomahawk.tomahawk_android.utils.MediaBrowserHelper;
import org.tomahawk.tomahawk_android.utils.MediaNotification;
import org.tomahawk.tomahawk_android.utils.PlaybackManager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
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
import android.support.v4.util.SparseArrayCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
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

    public static final String ACTION_PLAY
            = "org.tomahawk.tomahawk_android.ACTION_PLAY";

    public static final String ACTION_STOP_NOTIFICATION
            = "org.tomahawk.tomahawk_android.STOP_NOTIFICATION";

    public static final String ACTION_DELETE_ENTRY_IN_QUEUE
            = "org.tomahawk.tomahawk_android.DELETE_ENTRY_IN_QUEUE";

    public static final String ACTION_ADD_QUERY_TO_QUEUE
            = "org.tomahawk.tomahawk_android.ADD_QUERY_TO_QUEUE";

    public static final String ACTION_ADD_QUERIES_TO_QUEUE
            = "org.tomahawk.tomahawk_android.ADD_QUERIES_TO_QUEUE";

    public static final String ACTION_SET_SHUFFLE_MODE
            = "org.tomahawk.tomahawk_android.SET_SHUFFLE_MODE";

    public static final String ACTION_SET_REPEAT_MODE
            = "org.tomahawk.tomahawk_android.SET_REPEAT_MODE";

    public static final String EXTRAS_KEY_PLAYBACKMANAGER
            = "org.tomahawk.tomahawk_android.PLAYBACKMANAGER";

    public static final String EXTRAS_KEY_REPEAT_MODE
            = "org.tomahawk.tomahawk_android.REPEAT_MODE";

    public static final String EXTRAS_KEY_SHUFFLE_MODE
            = "org.tomahawk.tomahawk_android.SHUFFLE_MODE";

    private static final int MAX_ALBUM_ART_CACHE_SIZE = 5 * 1024 * 1024;

    private int mPlayState = PlaybackStateCompat.STATE_NONE;

    private boolean mIsPreparing = false;

    private static final int DELAY_SCROBBLE = 15000;

    private static final int DELAY_UNBIND_PLUGINSERVICES = 1800000;

    private static final int DELAY_SUICIDE = 1800000;

    private final Set<Query> mCorrespondingQueries
            = Collections.newSetFromMap(new ConcurrentHashMap<Query, Boolean>());

    private final ConcurrentHashMap<String, String> mCorrespondingRequestIds
            = new ConcurrentHashMap<>();

    private final Map<StationPlaylist, Set<Query>> mStationQueries = new ConcurrentHashMap<>();

    private PlaybackManager mPlaybackManager;

    private TomahawkMediaPlayer mCurrentMediaPlayer;

    private final Map<Class, TomahawkMediaPlayer> mMediaPlayers = new HashMap<>();

    private MediaNotification mNotification;

    private MediaSessionCompat mMediaSession;

    private Handler mCallbackHandler;

    private MediaBrowserHelper mMediaBrowserHelper;

    private SparseArrayCompat<PlaylistEntry> mQueueMap = new SparseArrayCompat<>();

    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {

        /**
         * Override to handle requests to begin playback.
         */
        @Override
        public void onPlay() {
            Log.d(TAG, "play");
            if (mPlaybackManager.getCurrentQuery() != null) {
                if (mAudioBecomingNoisyReceiver == null) {
                    mAudioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver();
                    registerReceiver(mAudioBecomingNoisyReceiver,
                            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
                }
                mSuicideHandler.stop();
                mSuicideHandler.reset();
                mPluginServiceKillHandler.stop();
                mPluginServiceKillHandler.reset();
                mScrobbleHandler.start();
                mPlayState = PlaybackStateCompat.STATE_PLAYING;
                handlePlayState();

                mAudioFocusHelper.tryToGetAudioFocus();
                updateMediaPlayState();
                mNotification.startNotification();
            }
        }

        /**
         * Override to handle requests to pause playback.
         */
        @Override
        public void onPause() {
            Log.d(TAG, "pause");
            if (mAudioBecomingNoisyReceiver != null) {
                unregisterReceiver(mAudioBecomingNoisyReceiver);
                mAudioBecomingNoisyReceiver = null;
            }
            mSuicideHandler.start();
            mPluginServiceKillHandler.start();
            mScrobbleHandler.stop();
            mPlayState = PlaybackStateCompat.STATE_PAUSED;
            handlePlayState();
            updateMediaPlayState();
        }

        /**
         * Override to handle requests to play a specific mediaId that was
         * provided by your app.
         */
        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            if (mMediaSession == null) {
                Log.e(TAG, "onPlayFromMediaId failed - mMediaSession == null!");
                return;
            }
            mMediaBrowserHelper.onPlayFromMediaId(mMediaSession, mPlaybackManager, mediaId, extras);
        }

        /**
         * Override to handle requests to play an item with a given id from the
         * play queue.
         */
        public void onSkipToQueueItem(long id) {
            Log.d(TAG, "Skipping to queue item with id " + id);
            PlaylistEntry entry = mQueueMap.get((int) id);
            mPlaybackManager.setCurrentEntry(entry);
        }

        /**
         * Override to handle requests to skip to the next media item.
         */
        @Override
        public void onSkipToNext() {
            Log.d(TAG, "next");
            int counter = 0;
            PlaylistEntry entry = mPlaybackManager.getCurrentEntry();
            while ((entry = mPlaybackManager.getNextEntry(entry)) != null
                    && counter++ < mPlaybackManager.getPlaybackListSize()) {
                if (entry.getQuery().isPlayable()) {
                    mPlaybackManager.setCurrentEntry(entry);
                    break;
                }
            }
        }

        /**
         * Override to handle requests to skip to the previous media item.
         */
        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "previous");
            int counter = 0;
            PlaylistEntry entry = mPlaybackManager.getCurrentEntry();
            while ((entry = mPlaybackManager.getPreviousEntry(entry)) != null
                    && counter++ < mPlaybackManager.getPlaybackListSize()) {
                if (entry.getQuery().isPlayable()) {
                    mPlaybackManager.setCurrentEntry(entry);
                    break;
                }
            }
        }

        /**
         * Override to handle requests to fast forward.
         */
        @Override
        public void onFastForward() {
            Log.d(TAG, "fastForward");
            long duration = mPlaybackManager.getCurrentTrack().getDuration();
            long newPos = Math.min(duration, Math.max(0, getPlaybackPosition() + 10000));
            onSeekTo(newPos);
        }

        /**
         * Override to handle requests to rewind.
         */
        @Override
        public void onRewind() {
            Log.d(TAG, "rewind");
            long duration = mPlaybackManager.getCurrentTrack().getDuration();
            long newPos = Math.min(duration, Math.max(0, getPlaybackPosition() - 10000));
            onSeekTo(newPos);
        }

        /**
         * Override to handle requests to stop playback.
         */
        @Override
        public void onStop() {
            onPause();
        }

        /**
         * Override to handle requests to seek to a specific position in ms.
         *
         * @param pos New position to move to, in milliseconds.
         */
        @Override
        public void onSeekTo(long pos) {
            Log.d(TAG, "seekTo " + pos);
            final Query currentQuery = mPlaybackManager.getCurrentQuery();
            if (currentQuery != null && currentQuery.getMediaPlayerClass() != null) {
                TomahawkMediaPlayer mp =
                        mMediaPlayers.get(currentQuery.getMediaPlayerClass());
                if (mp.isPrepared(currentQuery)) {
                    mp.seekTo(pos);
                    updateMediaPlayState();
                }
            }
        }

        /**
         * Override to handle the item being rated.
         */
        @Override
        public void onSetRating(RatingCompat rating) {
            if (rating.getRatingStyle() == RatingCompat.RATING_HEART
                    && mPlaybackManager.getCurrentQuery() != null) {
                CollectionManager.get().setLovedItem(
                        mPlaybackManager.getCurrentQuery(), rating.hasHeart());
                mPlaybackManagerCallback.onCurrentEntryChanged();
            }
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            if (ACTION_STOP_NOTIFICATION.equals(action)) {
                mNotification.stopNotification();
            } else if (ACTION_DELETE_ENTRY_IN_QUEUE.equals(action)) {
                PlaylistEntry entry =
                        PlaylistEntry.getByKey(extras.getString(TomahawkFragment.PLAYLISTENTRY));
                mPlaybackManager.deleteFromQueue(entry);
            } else if (ACTION_ADD_QUERY_TO_QUEUE.equals(action)) {
                Query query = Query.getByKey(extras.getString(TomahawkFragment.QUERY));
                mPlaybackManager.addToQueue(query);
            } else if (ACTION_ADD_QUERIES_TO_QUEUE.equals(action)) {
                List<String> queryKeys = extras.getStringArrayList(TomahawkFragment.QUERYARRAY);
                List<Query> queries = new ArrayList<>();
                for (String queryKey : queryKeys) {
                    queries.add(Query.getByKey(queryKey));
                }
                mPlaybackManager.addToQueue(queries);
            } else if (ACTION_SET_SHUFFLE_MODE.equals(action)) {
                int shuffleMode = extras.getInt(EXTRAS_KEY_SHUFFLE_MODE);
                Log.d(TAG, "setShuffleMode to " + shuffleMode);
                mPlaybackManager.setShuffleMode(shuffleMode);
            } else if (ACTION_SET_REPEAT_MODE.equals(action)) {
                int repeatMode = extras.getInt(EXTRAS_KEY_REPEAT_MODE);
                Log.d(TAG, "setRepeatMode to " + repeatMode);
                mPlaybackManager.setRepeatMode(repeatMode);
            }
        }
    };

    private PlaybackManager.Callback mPlaybackManagerCallback = new PlaybackManager.Callback() {
        @Override
        public void onPlaylistChanged() {
            Playlist playlist = mPlaybackManager.getPlaylist();
            Log.d(TAG, "Playlist has changed to: " + playlist);
            if (playlist instanceof StationPlaylist) {
                StationPlaylist stationPlaylist = (StationPlaylist) playlist;
                stationPlaylist.setPlayedTimeStamp(System.currentTimeMillis());
                if (stationPlaylist.getPlaylist() == null) {
                    DatabaseHelper.get().storeStation(stationPlaylist);
                }
                if (stationPlaylist.size() == 0) {
                    mIsPreparing = true;
                    updateMediaPlayState();
                    // station is empty, so we should fill it with some new tracks
                    fillStation(stationPlaylist);
                }
            }
            onCurrentEntryChanged();
        }

        @Override
        public void onCurrentEntryChanged() {
            Log.d(TAG, "Current entry has changed to: " + mPlaybackManager.getCurrentEntry());
            handlePlayState();
            Playlist playlist = mPlaybackManager.getPlaylist();
            if (playlist instanceof StationPlaylist) {
                if (!mPlaybackManager.hasNextEntry(mPlaybackManager.getNextEntry())) {
                    // there's no track after the next one,
                    // so we should fill the station with some new tracks
                    fillStation((StationPlaylist) playlist);
                }
            }
            resolveProximalQueries();
            updateMediaMetadata();
        }

        @Override
        public void onShuffleModeChanged() {
            updateMediaQueue();
        }

        @Override
        public void onRepeatModeChanged() {
            updateMediaQueue();
        }
    };

    private void fillStation(final StationPlaylist stationPlaylist) {
        Promise<List<Query>, Throwable, Void> promise = stationPlaylist.fillPlaylist(10);
        if (promise != null) {
            Log.d(TAG, "filling " + stationPlaylist);
            promise.done(new DoneCallback<List<Query>>() {
                @Override
                public void onDone(List<Query> result) {
                    Log.d(TAG, "found " + result.size() + " candidates to fill " + stationPlaylist);
                    for (Query query : result) {
                        mCorrespondingQueries.add(query);
                        if (!mStationQueries.containsKey(stationPlaylist)) {
                            Set<Query> querySet = Collections.newSetFromMap(
                                    new ConcurrentHashMap<Query, Boolean>());
                            mStationQueries.put(stationPlaylist, querySet);
                        }
                        mStationQueries.get(stationPlaylist).add(query);
                        PipeLine.get().resolve(query);
                    }
                }
            });
        }
    }

    private AudioBecomingNoisyReceiver mAudioBecomingNoisyReceiver;

    private class AudioBecomingNoisyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // AudioManager tells us that the sound will be played through the speaker
                Log.d(TAG, "Action audio becoming noisy, pausing ...");
                // So we stop playback, if needed
                mMediaSession.getController().getTransportControls().pause();
            }
        }
    }

    private PowerManager.WakeLock mWakeLock;

    private AudioFocusHelper mAudioFocusHelper = null;

    private Bitmap mCachedPlaceHolder;

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
                    Bitmap copy = bitmap.copy(bitmap.getConfig(), false);
                    if (mImageToLoad == null) {
                        // Has to the placeHolder bitmap then
                        mCachedPlaceHolder = copy;
                    } else {
                        mMediaImageCache.put(mImageToLoad, copy);
                    }
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
     * Listens for incoming phone calls and handles playback.
     */
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

        private boolean mShouldResume = false;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            Log.d(TAG, "PhoneStateListener onCallStateChanged state: " + state);
            if (mMediaSession == null) {
                Log.e(TAG, "onCallStateChanged failed - mMediaSession == null!");
                return;
            }
            switch (state) {
                case TelephonyManager.CALL_STATE_OFFHOOK:
                case TelephonyManager.CALL_STATE_RINGING:
                    if (mPlayState != PlaybackStateCompat.STATE_PAUSED) {
                        Log.d(TAG, "PhoneStateListener pausing playback");
                        mShouldResume = true;
                        mMediaSession.getController().getTransportControls().pause();
                    }
                    break;

                case TelephonyManager.CALL_STATE_IDLE:
                    if (mShouldResume) {
                        Log.d(TAG, "PhoneStateListener resuming playback");
                        mShouldResume = false;
                        mMediaSession.getController().getTransportControls().play();
                    }
                    break;
            }
        }
    };

    private SuicideHandler mSuicideHandler = new SuicideHandler(this);

    // Stops this service if it doesn't have any bound services
    private static class SuicideHandler extends DelayedHandler<PlaybackService> {

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

    private static class PluginServiceKillHandler extends DelayedHandler<PlaybackService> {

        public PluginServiceKillHandler(PlaybackService service) {
            super(service, DELAY_UNBIND_PLUGINSERVICES);
        }

        @Override
        public void handleMessage(Message msg) {
            if (getReferencedObject() != null) {
                getReferencedObject().unbindPluginServices();
            }
        }
    }

    private ScrobbleHandler mScrobbleHandler = new ScrobbleHandler(this);

    private static class ScrobbleHandler extends DelayedHandler<PlaybackService> {

        public ScrobbleHandler(PlaybackService service) {
            super(service, DELAY_SCROBBLE);
        }

        @Override
        public void handleMessage(Message msg) {
            if (getReferencedObject() != null) {
                Log.d(TAG, "Scrobbling delay has passed. Scrobbling...");
                if (getReferencedObject().mPlaybackManager.getCurrentQuery() != null) {
                    InfoSystem.get().sendNowPlayingPostStruct(
                            AuthenticatorManager.get().getAuthenticatorUtils(
                                    TomahawkApp.PLUGINNAME_HATCHET),
                            getReferencedObject().mPlaybackManager.getCurrentQuery());
                }
            }
        }
    }

    private TomahawkMediaPlayerCallback mMediaPlayerCallback = new TomahawkMediaPlayerCallback() {
        @Override
        public void onPrepared(TomahawkMediaPlayer mediaPlayer, Query query) {
            if (query != null && query == mPlaybackManager.getCurrentQuery()) {
                Log.d(TAG, mediaPlayer + " successfully prepared the track "
                        + mPlaybackManager.getCurrentQuery() + " resolved by "
                        + mPlaybackManager.getCurrentQuery()
                        .getPreferredTrackResult().getResolvedBy().getId());
                mIsPreparing = false;
                updateMediaPlayState();
                mScrobbleHandler.reset();
                handlePlayState();
            } else {
                String queryInfo;
                if (query != null) {
                    queryInfo = mPlaybackManager.getCurrentQuery() + " resolved by "
                            + mPlaybackManager.getCurrentQuery()
                            .getPreferredTrackResult().getResolvedBy().getId();
                } else {
                    queryInfo = "null";
                }
                Log.e(TAG, "onPrepared received for an unexpected Query: " + queryInfo);
            }
        }

        @Override
        public void onCompletion(TomahawkMediaPlayer mediaPlayer, Query query) {
            if (mMediaSession == null) {
                Log.e(TAG, "onCompletion failed - mMediaSession == null!");
                return;
            }
            if (query != null && query == mPlaybackManager.getCurrentQuery()) {
                Log.d(TAG, "onCompletion - mediaPlayer: " + mediaPlayer + ", query: " + query);
                if (mPlaybackManager.hasNextEntry()) {
                    mMediaSession.getController().getTransportControls().skipToNext();
                } else {
                    mMediaSession.getController().getTransportControls().pause();
                }
            }
        }

        @Override
        public void onError(TomahawkMediaPlayer mediaPlayer, final String message) {
            Log.d(TAG, "onError - mediaPlayer: " + mediaPlayer + ", message: " + message);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(TomahawkApp.getContext(), message, Toast.LENGTH_LONG).show();
                }
            });
            mAudioFocusHelper.giveUpAudioFocus();
            if (mMediaSession == null) {
                Log.e(TAG, "onError failed - mMediaSession == null!");
                return;
            }
            if (mPlaybackManager.hasNextEntry()) {
                mMediaSession.getController().getTransportControls().skipToNext();
            } else {
                mMediaSession.getController().getTransportControls().pause();
            }
        }
    };

    @SuppressWarnings("unused")
    public void onEventAsync(PipeLine.ResultsEvent event) {
        Playlist playlist = mPlaybackManager.getPlaylist();
        if (playlist instanceof StationPlaylist && event.mQuery.isPlayable()
                && mStationQueries.containsKey(playlist)
                && mStationQueries.get(playlist).remove(event.mQuery)) {
            boolean wasNull = mPlaybackManager.getCurrentEntry() == null;
            mPlaybackManager.addToPlaylist(event.mQuery);
            if (wasNull) {
                if (mMediaSession == null) {
                    Log.e(TAG,
                            "onEventAsync(PipeLine.ResultsEvent event) failed - mMediaSession == null!");
                } else {
                    mMediaSession.getController().getTransportControls().play();
                }
            }
        }
        Query currentQuery = mPlaybackManager.getCurrentQuery();
        if (currentQuery != null && currentQuery == event.mQuery) {
            mPlaybackManagerCallback.onCurrentEntryChanged();
            if (mCurrentMediaPlayer == null
                    || !(mCurrentMediaPlayer.isPrepared(currentQuery)
                    || mCurrentMediaPlayer.isPreparing(currentQuery))) {
                handlePlayState();
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventAsync(InfoSystem.ResultsEvent event) {
        Query currentQuery = mPlaybackManager.getCurrentQuery();
        if (currentQuery != null && currentQuery.getCacheKey()
                .equals(mCorrespondingRequestIds.get(event.mInfoRequestData.getRequestId()))) {
            mPlaybackManagerCallback.onCurrentEntryChanged();
        }
    }

    @SuppressWarnings("unused")
    public void onEventAsync(CollectionManager.UpdatedEvent event) {
        Query currentQuery = mPlaybackManager.getCurrentQuery();
        if (event.mUpdatedItemIds != null && currentQuery != null
                && event.mUpdatedItemIds.contains(currentQuery.getCacheKey())) {
            mPlaybackManagerCallback.onCurrentEntryChanged();
        }
    }

    public static class RequestServiceBindingEvent {

        private ServiceConnection mConnection;

        private String mServicePackageName;

        public RequestServiceBindingEvent(ServiceConnection connection, String servicePackageName) {
            mConnection = connection;
            mServicePackageName = servicePackageName;
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(RequestServiceBindingEvent event) {
        Intent intent = new Intent(event.mServicePackageName + ".BindToService");
        intent.setPackage(event.mServicePackageName);
        bindService(intent, event.mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        EventBus.getDefault().register(this);

        mMediaBrowserHelper = new MediaBrowserHelper(this);

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
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        // Initialize WakeLock
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), new AudioFocusable() {
            @Override
            public void onGainedAudioFocus() {
            }

            @Override
            public void onLostAudioFocus(boolean canDuck) {
                if (!canDuck && mMediaSession != null && mMediaSession.getController() != null) {
                    mMediaSession.getController().getTransportControls().pause();
                }
            }
        });

        mPlaybackManager = PlaybackManager.get(IdGenerator.getSessionUniqueStringId());
        mPlaybackManager.setCallback(mPlaybackManagerCallback);

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
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        Intent intent = new Intent(PlaybackService.this, TomahawkMainActivity.class);
        intent.setAction(TomahawkMainActivity.SHOW_PLAYBACKFRAGMENT_ON_STARTUP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(PlaybackService.this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mMediaSession.setSessionActivity(pendingIntent);
        HandlerThread thread = new HandlerThread("playbackservice_callback");
        thread.start();
        mCallbackHandler = new Handler(thread.getLooper());
        mMediaSession.setCallback(mMediaSessionCallback, mCallbackHandler);
        mMediaSession.setRatingType(RatingCompat.RATING_HEART);
        Bundle extras = new Bundle();
        extras.putString(EXTRAS_KEY_PLAYBACKMANAGER, mPlaybackManager.getId());
        mMediaSession.setExtras(extras);
        updateMediaPlayState();
        setSessionToken(mMediaSession.getSessionToken());
    }

    public Handler getCallbackHandler() {
        return mCallbackHandler;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSession, intent);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Client has been bound to PlaybackService");
        return super.onBind(intent);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
            @Nullable Bundle rootHints) {
        return mMediaBrowserHelper.onGetRoot(clientPackageName, clientUid, rootHints);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId,
            @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        mMediaBrowserHelper.onLoadChildren(parentId, result);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Client has been unbound from PlaybackService");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);

        mAudioFocusHelper.abandonFocus();

        mPlaybackManager.setCallback(null);

        if (mAudioBecomingNoisyReceiver != null) {
            unregisterReceiver(mAudioBecomingNoisyReceiver);
            mAudioBecomingNoisyReceiver = null;
        }
        mScrobbleHandler.stop();

        mPlayState = PlaybackStateCompat.STATE_PAUSED;
        handlePlayState();
        mNotification.stopNotification();

        for (TomahawkMediaPlayer mp : mMediaPlayers.values()) {
            mp.release();
        }
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        mWakeLock = null;
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        mPhoneStateListener = null;
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
        unbindPluginServices();

        Log.d(TAG, "PlaybackService has been destroyed");
    }

    private void unbindPluginServices() {
        Log.d(TAG, "Unbinding all PluginServices...");
        for (TomahawkMediaPlayer mp : mMediaPlayers.values()) {
            if (mp instanceof PluginMediaPlayer) {
                PluginMediaPlayer pmp = (PluginMediaPlayer) mp;
                if (pmp.isBound()) {
                    pmp.setService(null);
                    unbindService(pmp.getServiceConnection());
                }
            }
        }
    }

    /**
     * Update the TomahawkMediaPlayer so that it reflects the current playState
     */
    private void handlePlayState() {
        Log.d(TAG, "handlePlayState");
        Query currentQuery = mPlaybackManager.getCurrentQuery();
        if (currentQuery != null && currentQuery.getMediaPlayerClass() != null) {
            try {
                TomahawkMediaPlayer mp = mMediaPlayers.get(currentQuery.getMediaPlayerClass());
                switch (mPlayState) {
                    case PlaybackStateCompat.STATE_PLAYING:
                        // The service needs to continue running even after the bound client
                        // (usually a MediaController) disconnects, otherwise the music playback
                        // will stop. Calling startService(Intent) will keep the service running
                        // until it is explicitly killed.
                        startService(new Intent(getApplicationContext(), PlaybackService.class));
                        if (mWakeLock != null && mWakeLock.isHeld()) {
                            mWakeLock.acquire();
                        }
                        if (mp.isPreparing(currentQuery) || mp.isPrepared(currentQuery)) {
                            if (!mp.isPlaying(currentQuery)) {
                                mp.play();
                            }
                        } else {
                            prepareCurrentQuery();
                        }
                        break;
                    case PlaybackStateCompat.STATE_PAUSED:
                        if (mp.isPlaying(currentQuery)
                                && (mp.isPreparing(currentQuery) || mp.isPrepared(currentQuery))) {
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
                Log.e(TAG, "handlePlayState IllegalStateException, msg: "
                        + e1.getLocalizedMessage() + " , isPreparing: " + mIsPreparing);
            }
        } else {
            for (TomahawkMediaPlayer mp : mMediaPlayers.values()) {
                mp.release();
            }
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            Log.d(TAG, "handlePlayState couldn't do anything, isPreparing: " + mIsPreparing);
        }
    }

    /**
     * This method sets the current track and prepares it for playback.
     */
    private void prepareCurrentQuery() {
        if (mMediaSession == null) {
            Log.e(TAG, "prepareCurrentQuery failed - mMediaSession == null!");
            return;
        }
        Log.d(TAG, "prepareCurrentQuery");
        final Query currentQuery = mPlaybackManager.getCurrentQuery();
        if (currentQuery != null) {
            if (!currentQuery.isPlayable() || currentQuery.getMediaPlayerClass() == null) {
                Log.e(TAG, currentQuery + " isn't playable. Skipping to next track");
                mMediaSession.getController().getTransportControls().skipToNext();
            } else {
                // Resolve images for current query
                if (currentQuery.getImage() == null) {
                    String requestId = InfoSystem.get().resolve(
                            currentQuery.getArtist(), false);
                    if (requestId != null) {
                        mCorrespondingRequestIds.put(requestId, currentQuery.getCacheKey());
                    }
                    requestId = InfoSystem.get().resolve(currentQuery.getAlbum());
                    if (requestId != null) {
                        mCorrespondingRequestIds.put(requestId, currentQuery.getCacheKey());
                    }
                }

                mIsPreparing = true;
                updateMediaPlayState();

                TomahawkMediaPlayer mp = mMediaPlayers.get(currentQuery.getMediaPlayerClass());
                mp.prepare(currentQuery, mMediaPlayerCallback);
                if (mCurrentMediaPlayer != null && mCurrentMediaPlayer != mp) {
                    mCurrentMediaPlayer.release();
                }
                mCurrentMediaPlayer = mp;
            }
        }
    }

    /**
     * Returns the position of playback in the current Track.
     */
    private long getPlaybackPosition() {
        long position = 0;
        final Query currentQuery = mPlaybackManager.getCurrentQuery();
        if (currentQuery != null && currentQuery.getMediaPlayerClass() != null) {
            try {
                position = mMediaPlayers.get(currentQuery.getMediaPlayerClass()).getPosition();
            } catch (IllegalStateException e) {
                Log.e(TAG, "getPlaybackPosition: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
        return position;
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
        if (mPlaybackManager.getCurrentQuery() != null) {
            Log.d(TAG, "Setting media metadata to: " + mPlaybackManager.getCurrentQuery());
        } else if (mPlaybackManager.getPlaylist() instanceof StationPlaylist) {
            Log.d(TAG, "Setting media metadata to: " + getString(R.string.loading_station) + " "
                    + mPlaybackManager.getPlaylist().getName());
        } else {
            Log.e(TAG, "Wasn't able to set media metadata");
        }
    }

    private MediaMetadataCompat buildMetadata() {
        final Query currentQuery = mPlaybackManager.getCurrentQuery();
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();

        if (currentQuery != null) {
            builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                    mPlaybackManager.getCurrentEntry().getCacheKey())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST,
                            currentQuery.getArtist().getPrettyName())
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                            currentQuery.getArtist().getPrettyName())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                            currentQuery.getPrettyName())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                            currentQuery.getPreferredTrack().getDuration())
                    .putRating(MediaMetadataCompat.METADATA_KEY_USER_RATING,
                            RatingCompat.newHeartRating(
                                    DatabaseHelper.get().isItemLoved(currentQuery)));
            if (!currentQuery.getAlbum().getName().isEmpty()) {
                builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                        currentQuery.getAlbum().getPrettyName());
            }
            Bitmap bitmap;
            if (currentQuery.getImage() != null) {
                bitmap = mMediaImageCache.get(currentQuery.getImage());
            } else {
                bitmap = mCachedPlaceHolder;
            }
            if (bitmap == null) {
                // Image is not in cache yet. We have to fetch it...
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (mMediaImageTarget == null
                                || mMediaImageTarget.mImageToLoad != currentQuery.getImage()) {
                            mMediaImageTarget = new MediaImageTarget(currentQuery.getImage());
                            ImageUtils.loadImageIntoBitmap(TomahawkApp.getContext(),
                                    currentQuery.getImage(), mMediaImageTarget,
                                    Image.getLargeImageSize(), false);
                        }
                    }
                });
            }
            if (bitmap != null) {
                builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap);
            }
        } else if (mPlaybackManager.getPlaylist() instanceof StationPlaylist) {
            builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                    getString(R.string.loading_station) + " "
                            + mPlaybackManager.getPlaylist().getName());
        }
        return builder.build();
    }

    private void updateMediaPlayState() {
        if (mMediaSession == null) {
            Log.e(TAG, "updateMediaPlayState failed - mMediaSession == null!");
            return;
        }
        long actions = 0L;
        if (mPlaybackManager.getCurrentQuery() != null) {
            actions |= PlaybackStateCompat.ACTION_SET_RATING;
        }
        if (mPlayState == PlaybackStateCompat.STATE_PLAYING) {
            actions |= PlaybackStateCompat.ACTION_PAUSE |
                    PlaybackStateCompat.ACTION_SEEK_TO |
                    PlaybackStateCompat.ACTION_FAST_FORWARD |
                    PlaybackStateCompat.ACTION_REWIND;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }
        if (mPlaybackManager.hasNextEntry()) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        }
        if (mPlaybackManager.hasPreviousEntry()) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        }
        Log.d(TAG, "updateMediaPlayState()");
        Bundle extras = new Bundle();
        extras.putInt(EXTRAS_KEY_REPEAT_MODE, mPlaybackManager.getRepeatMode());
        extras.putInt(EXTRAS_KEY_SHUFFLE_MODE, mPlaybackManager.getShuffleMode());
        int playState = mIsPreparing ? PlaybackStateCompat.STATE_BUFFERING : mPlayState;
        PlaybackStateCompat playbackStateCompat = new PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(playState, getPlaybackPosition(), 1f, SystemClock.elapsedRealtime())
                .setExtras(extras)
                .build();
        mMediaSession.setPlaybackState(playbackStateCompat);
    }

    private void updateMediaQueue() {
        if (mMediaSession == null) {
            Log.e(TAG, "updateMediaQueue failed - mMediaSession == null!");
            return;
        }
        updateMediaMetadata();
        mMediaSession.setQueue(buildQueue());
        mMediaSession.setQueueTitle(getString(R.string.mediabrowser_queue_title));
    }

    private List<MediaSessionCompat.QueueItem> buildQueue() {
        List<MediaSessionCompat.QueueItem> queue = null;
        if (mPlaybackManager.getPlaylist() != null) {
            queue = new ArrayList<>();
            int currentIndex = mPlaybackManager.getCurrentIndex();
            for (int i = Math.max(0, currentIndex - 10);
                    i < Math.min(mPlaybackManager.getPlaybackListSize(), currentIndex + 40); i++) {
                PlaylistEntry entry = mPlaybackManager.getPlaybackListEntry(i);
                MediaDescriptionCompat.Builder descBuilder = new MediaDescriptionCompat.Builder();
                descBuilder.setMediaId(entry.getCacheKey());
                descBuilder.setTitle(entry.getQuery().getPrettyName());
                descBuilder.setSubtitle(entry.getArtist().getPrettyName());
                MediaSessionCompat.QueueItem item =
                        new MediaSessionCompat.QueueItem(descBuilder.build(), i);
                queue.add(item);
                mQueueMap.put(i, entry);
            }
        }
        return queue;
    }

    private void resolveProximalQueries() {
        Set<Query> qs = new HashSet<>();
        int start = Math.max(0, mPlaybackManager.getCurrentIndex() - 2);
        int end = Math.min(mPlaybackManager.getPlaybackListSize(),
                mPlaybackManager.getCurrentIndex() + 10);
        for (int i = start; i < end; i++) {
            Query q = mPlaybackManager.getPlaybackListEntry(i).getQuery();
            if (!mCorrespondingQueries.contains(q)) {
                qs.add(q);
            }
        }
        if (!qs.isEmpty()) {
            HashSet<Query> queries = PipeLine.get().resolve(qs);
            mCorrespondingQueries.addAll(queries);
        }
    }

    private void setBitrate(int mode) {
        for (TomahawkMediaPlayer mp : mMediaPlayers.values()) {
            mp.setBitrate(mode);
        }
    }
}
