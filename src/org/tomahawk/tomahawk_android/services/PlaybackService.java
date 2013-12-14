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

import org.tomahawk.libtomahawk.collection.BitmapItem;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.database.UserPlaylistsDataSource;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment;
import org.tomahawk.tomahawk_android.utils.OnCompletionListener;
import org.tomahawk.tomahawk_android.utils.OnErrorListener;
import org.tomahawk.tomahawk_android.utils.OnPreparedListener;
import org.tomahawk.tomahawk_android.utils.TomahawkMediaPlayer;

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
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * This {@link Service} handles all playback related processes.
 */
public class PlaybackService extends Service
        implements OnCompletionListener, OnErrorListener, OnPreparedListener {

    private static String TAG = PlaybackService.class.getName();

    private final IBinder mBinder = new PlaybackServiceBinder();

    private boolean mHasBoundServices;

    public static final String BROADCAST_NEWTRACK
            = "org.tomahawk.tomahawk_android..BROADCAST_NEWTRACK";

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

    private static final int PLAYBACKSERVICE_PLAYSTATE_STOPPED = 2;

    private int mPlayState = PLAYBACKSERVICE_PLAYSTATE_PLAYING;

    private static final int PLAYBACKSERVICE_NOTIFICATION_ID = 1;

    private static final int DELAY_TO_KILL = 300000;

    private TomahawkApp mTomahawkApp;

    private PipeLine mPipeLine;

    protected HashSet<String> mCorrespondingQueryIds = new HashSet<String>();

    private Playlist mCurrentPlaylist;

    private TomahawkMediaPlayer mTomahawkMediaPlayer;

    private PowerManager.WakeLock mWakeLock;

    private PlaybackServiceBroadcastReceiver mPlaybackServiceBroadcastReceiver;

    private Handler mHandler;

    private BitmapItem.AsyncBitmap mNotificationAsyncBitmap = new BitmapItem.AsyncBitmap(null);

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
                        mVolumeIncreaseFader.run();
                        start();
                    }

                    mStartCallTime = 0L;
                    break;
            }
        }

        @Override
        public void onDataConnectionStateChanged(int state) {
            super.onDataConnectionStateChanged(state);
            switch (state) {
                case TelephonyManager.DATA_CONNECTED:
                    if (mTomahawkMediaPlayer != null
                            && mTomahawkMediaPlayer.getCurrentPosition() == 0) {
                        setCurrentQuery(getCurrentQuery());
                    }
            }
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
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(TomahawkApp.getContext());
                boolean playbackOnHeadsetInsert = prefs
                        .getBoolean(FakePreferenceFragment.FAKEPREFERENCEFRAGMENT_KEY_PLUGINTOPLAY,
                                false);

                if (!isPlaying() && playbackOnHeadsetInsert) {
                    //resume playback, if user has set the "resume on headset plugin" preference
                    start();
                }
            } else if (BitmapItem.BITMAPITEM_BITMAPLOADED.equals(intent.getAction())) {
                // a bitmap has been loaded
                if (mCurrentPlaylist.getCurrentQuery() != null
                        && mCurrentPlaylist.getCurrentQuery().getAlbums() != null
                        && !mCurrentPlaylist.getCurrentQuery().getAlbums().isEmpty()
                        && intent.getStringExtra(BitmapItem.BITMAPITEM_BITMAPLOADED_PATH).equals(
                        mCurrentPlaylist.getCurrentQuery().getAlbums().get(0)
                                .getAlbumArtPath())) {
                    // the loaded bitmap is relevant to us, so we update the playing notification
                    updatePlayingNotification();
                }
            } else if (PipeLine.PIPELINE_RESULTSREPORTED.equals(intent.getAction())) {
                String qid = intent.getStringExtra(PipeLine.PIPELINE_RESULTSREPORTED_QID);
                onPipeLineResultsReported(qid);
            }
        }
    }

    public class PlaybackServiceBinder extends Binder {

        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    // This Runnable is used to increase the volume gently.
    private Runnable mVolumeIncreaseFader = new Runnable() {

        private float mVolume = 0f;

        @Override
        public void run() {

            mTomahawkMediaPlayer.setVolume(mVolume, mVolume);

            if (mVolume < 1.0f) {
                mVolume += .05f;
                mHandler.postDelayed(mVolumeIncreaseFader, 250);
            } else {
                mVolume = 0;
            }
        }
    };

    // Stops this service if it doesn't have any bound services
    private final Handler mKillTimerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (isPlaying()) {
                mKillTimerHandler.removeCallbacksAndMessages(null);
                Message msgx = mKillTimerHandler.obtainMessage();
                mKillTimerHandler.sendMessageDelayed(msgx, DELAY_TO_KILL);
            } else if (!mHasBoundServices) {
                stopSelf();
            }
        }
    };

    @Override
    public void onCreate() {
        mTomahawkApp = (TomahawkApp) getApplication();
        mPipeLine = mTomahawkApp.getPipeLine();

        mHandler = new Handler();

        // Initialize PhoneCallListener
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(
                Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneCallListener(), PhoneStateListener.LISTEN_CALL_STATE);
        telephonyManager
                .listen(new PhoneCallListener(), PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

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
                new IntentFilter(BitmapItem.BITMAPITEM_BITMAPLOADED));
        registerReceiver(mPlaybackServiceBroadcastReceiver,
                new IntentFilter(PipeLine.PIPELINE_RESULTSREPORTED));

        // Initialize killtime handler (watchdog style)
        mKillTimerHandler.removeCallbacksAndMessages(null);
        Message msg = mKillTimerHandler.obtainMessage();
        mKillTimerHandler.sendMessageDelayed(msg, DELAY_TO_KILL);

        // Finally initialize the heart of this PlaybackService, the TomahawkMediaPlayer object
        initMediaPlayer();
        restoreState();
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
                pause();
                stopForeground(true);
            }
        }
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
        if (!isPlaying()) {
            stopSelf();
        }
        return false;
    }

    @Override
    public void onDestroy() {
        pause(true);
        saveState();
        unregisterReceiver(mPlaybackServiceBroadcastReceiver);
        mTomahawkMediaPlayer.release();
        mTomahawkMediaPlayer = null;
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        super.onDestroy();
    }

    /**
     * Called if given {@link TomahawkMediaPlayer} has been prepared for playback
     */
    @Override
    public void onPrepared(TomahawkMediaPlayer mp) {
        Log.d(TAG, "Mediaplayer is prepared.");
        handlePlayState();
    }

    /**
     * Called if an error has occurred while trying to prepare {@link TomahawkMediaPlayer}
     */
    @Override
    public boolean onError(TomahawkMediaPlayer tmp, int what, int extra) {
        String whatString = "CODE UNSPECIFIED";
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                whatString = "MEDIA_ERROR_UNKNOWN";
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                whatString = "MEDIA_ERROR_SERVER_DIED";
        }

        Log.e(TAG, "onError - " + whatString);
        if (tmp == mTomahawkMediaPlayer && isNetworkAvailable()) {
            next();
        }
        return false;
    }

    /**
     * Called if given {@link org.tomahawk.tomahawk_android.utils.TomahawkMediaPlayer} has finished
     * playing a song. Prepare the next track if possible, otherwise stop.
     */
    @Override
    public void onCompletion(TomahawkMediaPlayer mp) {
        if (mCurrentPlaylist == null) {
            stop();
            return;
        }

        Query query = mCurrentPlaylist.getNextQuery();
        if (query != null) {
            setCurrentQuery(query);
        } else {
            stop();
        }
    }

    /**
     * Initializes the {@link TomahawkMediaPlayer}. Sets the listeners and AudioStreamType.
     */
    public void initMediaPlayer() {
        mTomahawkMediaPlayer = new TomahawkMediaPlayer();
        mTomahawkMediaPlayer.setOnCompletionListener(this);
        mTomahawkMediaPlayer.setOnPreparedListener(this);
        mTomahawkMediaPlayer.setOnErrorListener(this);
    }

    /**
     * Save the current playlist in the UserPlaylists Database
     */
    private void saveState() {
        if (getCurrentPlaylist() != null) {
            long startTime = System.currentTimeMillis();
            UserCollection userCollection = (UserCollection) ((TomahawkApp) getApplication())
                    .getSourceList().getCollectionFromId(UserCollection.Id);
            userCollection.setCachedUserPlaylist(UserPlaylist
                    .fromQueryList(UserPlaylistsDataSource.CACHED_PLAYLIST_ID,
                            UserPlaylistsDataSource.CACHED_PLAYLIST_NAME,
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
        UserCollection userCollection = (UserCollection) ((TomahawkApp) getApplication())
                .getSourceList().getCollectionFromId(UserCollection.Id);
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
            mPlayState = PLAYBACKSERVICE_PLAYSTATE_PAUSED;
            if (dismissNotificationOnPause) {
                stopForeground(true);
            } else {
                updatePlayingNotification();
            }
        } else if (mPlayState == PLAYBACKSERVICE_PLAYSTATE_PAUSED
                || mPlayState == PLAYBACKSERVICE_PLAYSTATE_STOPPED) {
            mPlayState = PLAYBACKSERVICE_PLAYSTATE_PLAYING;
            updatePlayingNotification();
        }
        sendBroadcast(new Intent(BROADCAST_PLAYSTATECHANGED));
        handlePlayState();
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
     * Stop playback.
     */
    public void stop() {
        mPlayState = PLAYBACKSERVICE_PLAYSTATE_STOPPED;
        sendBroadcast(new Intent(BROADCAST_PLAYSTATECHANGED));
        handlePlayState();
        stopForeground(true);
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
            stopForeground(true);
        } else {
            updatePlayingNotification();
        }
    }

    /**
     * Update the TomahawkMediaPlayer so that it reflects the current playState
     */
    public void handlePlayState() {
        if (!isPreparing()) {
            try {
                switch (mPlayState) {
                    case PLAYBACKSERVICE_PLAYSTATE_PLAYING:
                        if (!mWakeLock.isHeld()) {
                            mWakeLock.acquire();
                        }
                        if (!mTomahawkMediaPlayer.isPlaying()) {
                            mTomahawkMediaPlayer.start();
                        }
                        break;
                    case PLAYBACKSERVICE_PLAYSTATE_PAUSED:
                        if (mTomahawkMediaPlayer.isPlaying()) {
                            mTomahawkMediaPlayer.pause();
                        }
                        if (mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                        break;
                    case PLAYBACKSERVICE_PLAYSTATE_STOPPED:
                        mTomahawkMediaPlayer.stop();
                        if (mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                }
            } catch (IllegalStateException e1) {
                Log.e(TAG,
                        "handlePlayState() IllegalStateException, msg:" + e1.getLocalizedMessage()
                                + " , preparing=" + isPreparing());
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
            Query query = mCurrentPlaylist.getNextQuery();
            if (query != null) {
                setCurrentQuery(query);
            }
        }
    }

    /**
     * Play the previous track.
     */
    public void previous() {
        if (mCurrentPlaylist != null) {
            Query query = mCurrentPlaylist.getPreviousQuery();
            if (query != null) {
                setCurrentQuery(query);
            }
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
        return mTomahawkMediaPlayer == null || mTomahawkMediaPlayer.isPreparing();
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
        Track track = null;
        if (getCurrentQuery() != null) {
            track = getCurrentQuery().getPreferredTrack();
        }
        return track;
    }

    /**
     * This method sets the current track and prepares it for playback.
     */
    public void setCurrentQuery(final Query query) {
        mNotificationAsyncBitmap.bitmap = null;
        if (mTomahawkMediaPlayer != null && query != null) {
            resolveQueriesFromTo(getCurrentPlaylist().getCurrentQueryIndex(),
                    getCurrentPlaylist().getCurrentQueryIndex() + 10);
            if (query.isPlayable()) {
                Runnable releaseRunnable = new Runnable() {
                    @Override
                    public void run() {
                        int loopCounter = 0;
                        while (true) {
                            if (loopCounter++ > 10) {
                                Log.e(TAG, "MediaPlayer was unable to prepare the track");
                                break;
                            }
                            long startTime = System.currentTimeMillis();
                            mTomahawkMediaPlayer.release();
                            initMediaPlayer();
                            long endTime = System.currentTimeMillis();
                            Log.d(TAG, "MediaPlayer reinitialize in " + (endTime - startTime)
                                    + "ms, preparing=" + isPreparing());
                            try {
                                boolean isSpotifyUrl =
                                        query.getPreferredTrackResult().getResolvedBy().getId()
                                                == TomahawkApp.RESOLVER_ID_SPOTIFY;
                                mTomahawkMediaPlayer.prepare(
                                        query.getPreferredTrackResult().getPath(),
                                        isSpotifyUrl);
                            } catch (IllegalStateException e1) {
                                Log.e(TAG, "setDataSource() IllegalStateException, msg:" + e1
                                        .getLocalizedMessage() + " , preparing=" + isPreparing());
                                continue;
                            } catch (IOException e2) {
                                Log.e(TAG, "setDataSource() IOException, msg:" + e2
                                        .getLocalizedMessage() + " , preparing=" + isPreparing());
                                continue;
                            }
                            break;
                        }
                    }
                };
                new Thread(releaseRunnable).start();

                mKillTimerHandler.removeCallbacksAndMessages(null);
                Message msg = mKillTimerHandler.obtainMessage();
                mKillTimerHandler.sendMessageDelayed(msg, DELAY_TO_KILL);

                updatePlayingNotification();
                sendBroadcast(new Intent(BROADCAST_NEWTRACK));
            } else if (((TomahawkApp) getApplication()).getPipeLine() != null
                    && !((TomahawkApp) getApplication()).getPipeLine().isResolving()) {
                next();
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
        mCurrentPlaylist = playlist;
        if (playlist != null) {
            setCurrentQuery(mCurrentPlaylist.getCurrentQuery());
        }
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    /**
     * Add given {@link ArrayList} of {@link org.tomahawk.libtomahawk.resolver.Query}s to the
     * current {@link Playlist}
     */
    public void addQueriesToCurrentPlaylist(ArrayList<Query> queries) {
        if (mCurrentPlaylist == null) {
            mCurrentPlaylist = UserPlaylist
                    .fromQueryList(UserPlaylistsDataSource.CACHED_PLAYLIST_ID,
                            UserPlaylistsDataSource.CACHED_PLAYLIST_NAME,
                            new ArrayList<Query>());
        }
        boolean wasEmpty = mCurrentPlaylist.getCount() <= 0;
        mCurrentPlaylist.addQueries(queries);
        if (wasEmpty && mCurrentPlaylist.getCount() > 0) {
            setCurrentQuery(mCurrentPlaylist.getQueryAtPos(0));
        }
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    /**
     * Add given {@link ArrayList} of {@link Track}s to the current {@link Playlist} at the given
     * position
     */
    public void addTracksToCurrentPlaylist(int position, ArrayList<Query> queries) {
        if (mCurrentPlaylist == null) {
            mCurrentPlaylist = UserPlaylist
                    .fromQueryList(UserPlaylistsDataSource.CACHED_PLAYLIST_ID,
                            UserPlaylistsDataSource.CACHED_PLAYLIST_NAME,
                            new ArrayList<Query>());
        }
        boolean wasEmpty = mCurrentPlaylist.getCount() <= 0;
        if (position < mCurrentPlaylist.getCount()) {
            mCurrentPlaylist.addQueries(position, queries);
        } else {
            mCurrentPlaylist.addQueries(queries);
        }
        if (wasEmpty && mCurrentPlaylist.getCount() > 0) {
            setCurrentQuery(mCurrentPlaylist.getQueryAtPos(0));
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
        try {
            position = mTomahawkMediaPlayer.getCurrentPosition();
        } catch (IllegalStateException e) {
            Log.e(TAG, "getPosition: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return position;
    }

    /**
     * Seeks to position msec
     */
    public void seekTo(int msec) {
        mTomahawkMediaPlayer.seekTo(msec);
    }

    /**
     * Create or update an ongoing notification
     */
    private void updatePlayingNotification() {

        Query query = getCurrentQuery();
        if (query == null) {
            return;
        }
        Track track = query.getPreferredTrack();
        if (track == null) {
            return;
        }

        Resources resources = getResources();
        Bitmap albumArtTemp;
        String albumName = "";
        String artistName = "";
        if (track.getAlbum() != null) {
            albumName = track.getAlbum().getName();
        }
        if (track.getArtist() != null) {
            artistName = track.getArtist().getName();
        }
        if (track.getAlbum() != null) {
            if (mNotificationAsyncBitmap.bitmap != null) {
                albumArtTemp = mNotificationAsyncBitmap.bitmap;
            } else {
                track.getAlbum().loadBitmap(this, mNotificationAsyncBitmap);
                albumArtTemp = BitmapFactory
                        .decodeResource(resources, R.drawable.no_album_art_placeholder);
            }
        } else {
            albumArtTemp = BitmapFactory
                    .decodeResource(resources, R.drawable.no_album_art_placeholder);
        }
        Bitmap largeAlbumArt = Bitmap.createScaledBitmap(albumArtTemp, 256, 256, false);
        Bitmap smallAlbumArt = Bitmap.createScaledBitmap(albumArtTemp,
                resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
                false);

        Intent intent = new Intent(BROADCAST_NOTIFICATIONINTENT_PREVIOUS, null, this,
                PlaybackService.class);
        PendingIntent previousPendingIntent = PendingIntent
                .getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        intent = new Intent(BROADCAST_NOTIFICATIONINTENT_PLAYPAUSE, null, this,
                PlaybackService.class);
        PendingIntent playPausePendingIntent = PendingIntent
                .getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        intent = new Intent(BROADCAST_NOTIFICATIONINTENT_NEXT, null, this, PlaybackService.class);
        PendingIntent nextPendingIntent = PendingIntent
                .getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        intent = new Intent(BROADCAST_NOTIFICATIONINTENT_EXIT, null, this, PlaybackService.class);
        PendingIntent exitPendingIntent = PendingIntent
                .getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        RemoteViews smallNotificationView;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            smallNotificationView = new RemoteViews(getPackageName(), R.layout.notification_small);
            smallNotificationView
                    .setImageViewBitmap(R.id.notification_small_imageview_albumart, smallAlbumArt);
        } else {
            smallNotificationView = new RemoteViews(getPackageName(),
                    R.layout.notification_small_compat);
        }
        smallNotificationView.setTextViewText(R.id.notification_small_textview, track.getName());
        if (TextUtils.isEmpty(albumName)) {
            smallNotificationView.setTextViewText(R.id.notification_small_textview2, artistName);
        } else {
            smallNotificationView.setTextViewText(R.id.notification_small_textview2,
                    artistName + " - " + albumName);
        }
        if (isPlaying()) {
            smallNotificationView.setImageViewResource(R.id.notification_small_imageview_playpause,
                    R.drawable.ic_player_pause);
        } else {
            smallNotificationView.setImageViewResource(R.id.notification_small_imageview_playpause,
                    R.drawable.ic_player_play);
        }
        smallNotificationView.setOnClickPendingIntent(R.id.notification_small_imageview_playpause,
                playPausePendingIntent);
        smallNotificationView
                .setOnClickPendingIntent(R.id.notification_small_imageview_next, nextPendingIntent);
        smallNotificationView
                .setOnClickPendingIntent(R.id.notification_small_imageview_exit, exitPendingIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher).setContentTitle(artistName)
                .setContentText(track.getName()).setLargeIcon(smallAlbumArt).setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX).setContent(smallNotificationView);

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
            largeNotificationView
                    .setImageViewBitmap(R.id.notification_large_imageview_albumart, largeAlbumArt);
            largeNotificationView
                    .setTextViewText(R.id.notification_large_textview, track.getName());
            largeNotificationView.setTextViewText(R.id.notification_large_textview2, artistName);
            largeNotificationView.setTextViewText(R.id.notification_large_textview3, albumName);
            if (isPlaying()) {
                largeNotificationView
                        .setImageViewResource(R.id.notification_large_imageview_playpause,
                                R.drawable.ic_player_pause);
            } else {
                largeNotificationView
                        .setImageViewResource(R.id.notification_large_imageview_playpause,
                                R.drawable.ic_player_play);
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
        startForeground(PLAYBACKSERVICE_NOTIFICATION_ID, notification);
    }

    private void resolveQueriesFromTo(int start, int end) {
        ArrayList<Query> qs = new ArrayList<Query>();
        for (int i = start; i < end; i++) {
            if (i >= 0 && i < mCurrentPlaylist.getQueries().size()) {
                Query q = mCurrentPlaylist.peekQueryAtPos(i);
                if (!q.isSolved() && !mCorrespondingQueryIds.contains(q.getQid())) {
                    qs.add(q);
                }
            }
        }
        if (!qs.isEmpty()) {
            HashSet<String> qids = mPipeLine.resolve(qs);
            mCorrespondingQueryIds.addAll(qids);
        }
    }

    private void onPipeLineResultsReported(String qId) {
        if (mCurrentPlaylist != null && mCurrentPlaylist.getCurrentQuery().getQid().equals(qId)) {
            setCurrentQuery(mCurrentPlaylist.getCurrentQuery());
        }
    }
}
