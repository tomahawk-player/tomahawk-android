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
import org.tomahawk.libtomahawk.collection.CustomPlaylist;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.database.UserPlaylistsDataSource;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.PlaybackActivity;
import org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment;

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
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
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
import android.support.v4.app.TaskStackBuilder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;
import java.util.ArrayList;

public class PlaybackService extends Service
        implements OnCompletionListener, OnErrorListener, OnPreparedListener {

    private static String TAG = PlaybackService.class.getName();

    private final IBinder mBinder = new PlaybackServiceBinder();

    private boolean mHasBoundServices;

    public static final String BROADCAST_NEWTRACK
            = "org.tomahawk.libtomahawk.audio.PlaybackService.BROADCAST_NEWTRACK";

    public static final String BROADCAST_PLAYLISTCHANGED
            = "org.tomahawk.libtomahawk.audio.PlaybackService.BROADCAST_PLAYLISTCHANGED";

    public static final String BROADCAST_PLAYSTATECHANGED
            = "org.tomahawk.libtomahawk.audio.PlaybackService.BROADCAST_PLAYSTATECHANGED";

    public static final String BROADCAST_NOTIFICATIONINTENT_PREVIOUS
            = "org.tomahawk.libtomahawk.audio.PlaybackService.BROADCAST_NOTIFICATIONINTENT_PREVIOUS";

    public static final String BROADCAST_NOTIFICATIONINTENT_PLAYPAUSE
            = "org.tomahawk.libtomahawk.audio.PlaybackService.BROADCAST_NOTIFICATIONINTENT_PLAYPAUSE";

    public static final String BROADCAST_NOTIFICATIONINTENT_NEXT
            = "org.tomahawk.libtomahawk.audio.PlaybackService.BROADCAST_NOTIFICATIONINTENT_NEXT";

    public static final String BROADCAST_NOTIFICATIONINTENT_EXIT
            = "org.tomahawk.libtomahawk.audio.PlaybackService.BROADCAST_NOTIFICATIONINTENT_EXIT";

    private static final int PLAYBACKSERVICE_PLAYSTATE_PLAYING = 0;

    private static final int PLAYBACKSERVICE_PLAYSTATE_PAUSED = 1;

    private static final int PLAYBACKSERVICE_PLAYSTATE_STOPPED = 2;

    private int mPlayState = PLAYBACKSERVICE_PLAYSTATE_PLAYING;

    private static final int PLAYBACKSERVICE_NOTIFICATION_ID = 1;

    private static final int DELAY_TO_KILL = 300000;

    private Playlist mCurrentPlaylist;

    private TomahawkMediaPlayer mTomahawkMediaPlayer;

    private PowerManager.WakeLock mWakeLock;

    private ServiceBroadcastReceiver mServiceBroadcastReceiver;

    private Handler mHandler;

    private UserPlaylistsDataSource mUserPlaylistsDataSource;

    private BitmapItem.AsyncBitmap mNotificationAsyncBitmap = new BitmapItem.AsyncBitmap(null);

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

        /* (non-Javadoc)
         * @see android.telephony.PhoneStateListener#onCallStateChanged(int, java.lang.String)
         */
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
                    try {
                        if (mTomahawkMediaPlayer != null
                                && mTomahawkMediaPlayer.getCurrentPosition() == 0) {
                            setCurrentTrack(getCurrentTrack());
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } catch (IllegalStateException e2) {
                        e2.printStackTrace();
                    }
            }
        }
    }

    private class ServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (isPlaying()) {
                    pause();
                }
            } else if (intent.hasExtra("state") && intent.getIntExtra("state", 0) == 1) {

                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(TomahawkApp.getContext());
                boolean playbackOnHeadsetInsert = prefs
                        .getBoolean(FakePreferenceFragment.FAKEPREFERENCEFRAGMENT_KEY_PLUGINTOPLAY,
                                false);

                if (!isPlaying() && playbackOnHeadsetInsert) {
                    start();
                }
            } else if (intent.getAction().equals(BitmapItem.BITMAPITEM_BITMAPLOADED)) {
                if (mCurrentPlaylist.getCurrentTrack() != null && intent
                        .getStringExtra(BitmapItem.BITMAPITEM_BITMAPLOADED_PATH)
                        .equals(mCurrentPlaylist.getCurrentTrack().getAlbum().getAlbumArtPath())) {
                    updatePlayingNotification();
                }
            }
        }
    }

    public class PlaybackServiceBinder extends Binder {

        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    private class TomahawkMediaPlayer extends MediaPlayer {

        private boolean mIsPreparing;
    }

    /**
     * This Runnable is used to increase the volume gently.
     */
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

    /* (non-Javadoc)
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {

        mHandler = new Handler();

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(
                Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneCallListener(), PhoneStateListener.LISTEN_CALL_STATE);
        telephonyManager
                .listen(new PhoneCallListener(), PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        mServiceBroadcastReceiver = new ServiceBroadcastReceiver();
        registerReceiver(mServiceBroadcastReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        registerReceiver(mServiceBroadcastReceiver,
                new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        registerReceiver(mServiceBroadcastReceiver,
                new IntentFilter(BitmapItem.BITMAPITEM_BITMAPLOADED));

        mKillTimerHandler.removeCallbacksAndMessages(null);
        Message msg = mKillTimerHandler.obtainMessage();
        mKillTimerHandler.sendMessageDelayed(msg, DELAY_TO_KILL);

        mUserPlaylistsDataSource = new UserPlaylistsDataSource(this,
                ((TomahawkApp) getApplication()).getPipeLine());
        mUserPlaylistsDataSource.open();

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

    /* (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
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

    /* (non-Javadoc)
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        pause(true);
        saveState();
        mUserPlaylistsDataSource.close();
        unregisterReceiver(mServiceBroadcastReceiver);
        mTomahawkMediaPlayer.release();
        mTomahawkMediaPlayer = null;
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        super.onDestroy();
    }

    /* (non-Javadoc)
     * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "Mediaplayer is prepared.");
        mTomahawkMediaPlayer.mIsPreparing = false;
        handlePlayState();
    }

    /* (non-Javadoc)
     * @see android.media.MediaPlayer.OnCompletionListener#onCompletion(android.media.MediaPlayer)
     */
    @Override
    public void onCompletion(MediaPlayer mp) {

        if (mCurrentPlaylist == null) {
            stop();
            return;
        }

        Track track = mCurrentPlaylist.getNextTrack();
        if (track != null) {
            try {
                setCurrentTrack(track);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            stop();
        }
    }

    /* (non-Javadoc)
     * @see android.media.MediaPlayer.OnErrorListener#onError(android.media.MediaPlayer, int, int)
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
        if (mp == mTomahawkMediaPlayer && isNetworkAvailable()) {
            next();
        }
        return false;
    }

    /**
     * Initializes the mediaplayer. Sets the listeners and AudioStreamType.
     */
    public void initMediaPlayer() {
        mTomahawkMediaPlayer = new TomahawkMediaPlayer();
        mTomahawkMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mTomahawkMediaPlayer.setOnCompletionListener(this);
        mTomahawkMediaPlayer.setOnPreparedListener(this);
        mTomahawkMediaPlayer.setOnErrorListener(this);
    }

    /**
     * Save the current playlist in the UserCollection
     */
    private void saveState() {
        if (getCurrentPlaylist() != null) {
            UserCollection userCollection = ((UserCollection) ((TomahawkApp) getApplication())
                    .getSourceList().getCollectionFromId(UserCollection.Id));
            userCollection.setCachedPlaylist(CustomPlaylist
                    .fromTrackList(UserPlaylistsDataSource.CACHED_PLAYLIST_NAME,
                            getCurrentPlaylist().getTracks()));
            long startTime = System.currentTimeMillis();
            mUserPlaylistsDataSource.storeCachedUserPlaylist(getCurrentPlaylist());
            Log.d(TAG, "Playlist stored in " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

    /**
     * Restore the current playlist from the UserCollection
     */
    private void restoreState() {
        UserCollection userCollection = ((UserCollection) ((TomahawkApp) getApplication())
                .getSourceList().getCollectionFromId(UserCollection.Id));
        try {
            setCurrentPlaylist(userCollection.getCachedCustomPlaylist());
            if (getCurrentPlaylist() == null) {
                long startTime = System.currentTimeMillis();
                setCurrentPlaylist(mUserPlaylistsDataSource.getCachedUserPlaylist());
                Log.d(TAG, "Playlist loaded in " + (System.currentTimeMillis() - startTime) + "ms");
            }
        } catch (IOException e) {
            Log.e(TAG, "restoreState(): " + IOException.class.getName() + ": " + e
                    .getLocalizedMessage());
        }
        if (getCurrentPlaylist() != null && isPlaying()) {
            pause(true);
        }
    }

    /**
     * Start or pause playback (doesn't dismiss notification on pause)
     */
    public void playPause() {
        playPause(false);
    }

    /**
     * Start or pause playback.
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
     * Pause playback.
     */
    public void pause() {
        pause(false);
    }

    /**
     * Pause playback.
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
            Track track = mCurrentPlaylist.getNextTrack();
            if (track != null) {
                try {
                    setCurrentTrack(track);
                } catch (IOException e) {
                    Log.e(TAG, "next(): " + IOException.class.getName() + ": " + e
                            .getLocalizedMessage());
                }
            }
            updatePlayingNotification();
        }
    }

    /**
     * Play the previous track.
     */
    public void previous() {
        if (mCurrentPlaylist != null) {
            Track track = mCurrentPlaylist.getPreviousTrack();
            if (track != null) {
                try {
                    setCurrentTrack(track);
                } catch (IOException e) {
                    Log.e(TAG, "previous(): " + IOException.class.getName() + ": " + e
                            .getLocalizedMessage());
                }
            }
            updatePlayingNotification();
        }
    }

    /**
     * Set the current playlist to shuffle mode.
     */
    public void setShuffled(boolean shuffled) {
        mCurrentPlaylist.setShuffled(shuffled);
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    /**
     * Set the current playlist to repeat mode.
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
        return mTomahawkMediaPlayer == null || mTomahawkMediaPlayer.mIsPreparing;
    }

    /**
     * Get the current Track
     */
    public Track getCurrentTrack() {

        if (mCurrentPlaylist == null) {
            return null;
        }

        return mCurrentPlaylist.getCurrentTrack();
    }

    /**
     * This method sets the current track and prepares it for playback.
     */
    public void setCurrentTrack(final Track track) throws IOException {
        mNotificationAsyncBitmap.bitmap = null;
        if (mTomahawkMediaPlayer != null && track != null) {
            if (track.isResolved()) {
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
                            mTomahawkMediaPlayer.mIsPreparing = true;
                            try {
                                mTomahawkMediaPlayer.setDataSource(track.getPath());
                            } catch (IllegalStateException e1) {
                                Log.e(TAG, "setDataSource() IllegalStateException, msg:" + e1
                                        .getLocalizedMessage() + " , preparing=" + isPreparing());
                                continue;
                            } catch (IOException e2) {
                                Log.e(TAG, "setDataSource() IOException, msg:" + e2
                                        .getLocalizedMessage() + " , preparing=" + isPreparing());
                                continue;
                            }
                            try {
                                mTomahawkMediaPlayer.prepare();
                            } catch (IllegalStateException e1) {
                                Log.e(TAG, "prepare() IllegalStateException, msg:" + e1
                                        .getLocalizedMessage() + " , preparing=" + isPreparing());
                                continue;
                            } catch (IOException e2) {
                                Log.e(TAG, "prepare() IOException, msg:" + e2.getLocalizedMessage()
                                        + " , preparing=" + isPreparing());
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

                sendBroadcast(new Intent(BROADCAST_NEWTRACK));
            } else {
                next();
            }
        }
    }

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
    public void setCurrentPlaylist(Playlist playlist) throws IOException {
        mCurrentPlaylist = playlist;
        if (playlist != null) {
            setCurrentTrack(mCurrentPlaylist.getCurrentTrack());
        }
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    public void addTracksToCurrentPlaylist(ArrayList<Track> tracks) {
        if (mCurrentPlaylist == null) {
            mCurrentPlaylist = CustomPlaylist.fromTrackList("Temp", new ArrayList<Track>());
        }
        boolean wasEmpty = mCurrentPlaylist.getCount() <= 0;
        mCurrentPlaylist.addTracks(tracks);
        if (wasEmpty && mCurrentPlaylist.getCount() > 0) {
            try {
                setCurrentTrack(mCurrentPlaylist.getTrackAtPos(0));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    public void addTracksToCurrentPlaylist(int position, ArrayList<Track> tracks) {
        if (mCurrentPlaylist == null) {
            mCurrentPlaylist = CustomPlaylist.fromTrackList("Temp", new ArrayList<Track>());
        }
        boolean wasEmpty = mCurrentPlaylist.getCount() <= 0;
        if (position < mCurrentPlaylist.getCount()) {
            mCurrentPlaylist.addTracks(position, tracks);
        } else {
            mCurrentPlaylist.addTracks(tracks);
        }
        if (wasEmpty && mCurrentPlaylist.getCount() > 0) {
            try {
                setCurrentTrack(mCurrentPlaylist.getTrackAtPos(0));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    public void deleteTrackAtPos(int position) {
        mCurrentPlaylist.deleteTrackAtPos(position);
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

        Track track = getCurrentTrack();
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

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(PlaybackActivity.class);
        Intent notificationIntent = getIntent(this, PlaybackActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent resultPendingIntent = stackBuilder
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
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

    /**
     * Return the {@link Intent} defined by the given parameters
     *
     * @param context the context with which the intent will be created
     * @param cls     the class which contains the activity to launch
     * @return the created intent
     */
    private static Intent getIntent(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return intent;
    }
}
