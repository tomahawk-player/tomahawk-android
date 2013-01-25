/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
package org.tomahawk.libtomahawk.audio;

import java.io.IOException;

import org.tomahawk.libtomahawk.Track;
import org.tomahawk.libtomahawk.playlist.Playlist;
import org.tomahawk.tomahawk_android.R;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PlaybackService extends Service implements OnCompletionListener, OnErrorListener, OnPreparedListener {

    private static String TAG = PlaybackService.class.getName();

    private final IBinder mBinder = new PlaybackServiceBinder();

    public static final String BROADCAST_NEWTRACK = "org.tomahawk.libtomahawk.audio.PlaybackService.BROADCAST_NEWTRACK";
    public static final String BROADCAST_PLAYLISTCHANGED = "org.tomahawk.libtomahawk.audio.PlaybackService.BROADCAST_PLAYLISTCHANGED";
    public static final String BROADCAST_PLAYSTATECHANGED = "org.tomahawk.libtomahawk.audio.PlaybackService.BROADCAST_PLAYSTATECHANGED";

    private static final int PLAYBACKSERVICE_PLAYSTATE_PLAYING = 0;
    private static final int PLAYBACKSERVICE_PLAYSTATE_PAUSED = 1;
    private static final int PLAYBACKSERVICE_PLAYSTATE_STOPPED = 2;

    private int mPlayState = PLAYBACKSERVICE_PLAYSTATE_PLAYING;

    private Playlist mCurrentPlaylist;
    private MediaPlayer mMediaPlayer;
    private PowerManager.WakeLock mWakeLock;
    private boolean mIsPreparing;
    private HeadsetBroadcastReceiver mHeadsetBroadcastReceiver;
    private Handler mHandler;

    public static class PlaybackServiceConnection implements ServiceConnection {

        private PlaybackServiceConnectionListener mPlaybackServiceConnectionListener;

        public interface PlaybackServiceConnectionListener {
            public void setPlaybackService(PlaybackService ps);

            public void onPlaybackServiceReady();
        }

        public PlaybackServiceConnection(PlaybackServiceConnectionListener playbackServiceConnectedListener) {
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
    };

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
                if (mStartCallTime > 0 && (System.currentTimeMillis() - mStartCallTime < 30000)) {
                    mVolumeIncreaseFader.run();
                    start();
                }

                mStartCallTime = 0L;
                break;
            }
        }
    }

    /**
     * Listens for Headset changes.
     */
    private class HeadsetBroadcastReceiver extends BroadcastReceiver {
        private boolean headsetConnected;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("state")) {
                if (headsetConnected && intent.getIntExtra("state", 0) == 0) {
                    headsetConnected = false;
                    if (isPlaying()) {
                        pause();
                    }
                } else if (!headsetConnected && intent.getIntExtra("state", 0) == 1) {
                    headsetConnected = true;
                }
            }
        }
    }

    public class PlaybackServiceBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    /**
     * This Runnable is used to increase the volume gently.
     */
    private Runnable mVolumeIncreaseFader = new Runnable() {

        private float mVolume = 0f;

        @Override
        public void run() {

            mMediaPlayer.setVolume(mVolume, mVolume);

            if (mVolume < 1.0f) {
                mVolume += .05f;
                mHandler.postDelayed(mVolumeIncreaseFader, 250);
            } else
                mVolume = 0;
        }
    };

    /* (non-Javadoc)
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {

        mHandler = new Handler();

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneCallListener(), PhoneStateListener.LISTEN_CALL_STATE);

        initMediaPlayer();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        mHeadsetBroadcastReceiver = new HeadsetBroadcastReceiver();
        registerReceiver(mHeadsetBroadcastReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    }

    /**
     * Initializes the mediaplayer. Sets the listeners and AudioStreamType.
     */
    public void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);
    }

    /* (non-Javadoc)
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        unregisterReceiver(mHeadsetBroadcastReceiver);
        mMediaPlayer.release();
    }

    /* (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void handlePlayState() {
        if (!mIsPreparing) {
            switch (mPlayState) {
            case PLAYBACKSERVICE_PLAYSTATE_PLAYING:
                if (!mWakeLock.isHeld())
                    mWakeLock.acquire();
                mMediaPlayer.start();
                createPlayingNotification();
                break;
            case PLAYBACKSERVICE_PLAYSTATE_PAUSED:
                //Workaround. First start the mediaplayer to correctly initialize its playback position.
                mMediaPlayer.start();
                mMediaPlayer.pause();
                if (mWakeLock.isHeld())
                    mWakeLock.release();
                stopForeground(true);
                break;
            case PLAYBACKSERVICE_PLAYSTATE_STOPPED:
                //Workaround. First start the mediaplayer to correctly initialize its playback position.
                mMediaPlayer.start();
                mMediaPlayer.stop();
                if (mWakeLock.isHeld())
                    mWakeLock.release();
                stopForeground(true);
            }
        }
    }

    /**
     * Start or pause playback.
     */
    public void playPause() {
        if (mPlayState == PLAYBACKSERVICE_PLAYSTATE_PLAYING)
            mPlayState = PLAYBACKSERVICE_PLAYSTATE_PAUSED;
        else if (mPlayState == PLAYBACKSERVICE_PLAYSTATE_PAUSED || mPlayState == PLAYBACKSERVICE_PLAYSTATE_STOPPED)
            mPlayState = PLAYBACKSERVICE_PLAYSTATE_PLAYING;
        sendBroadcast(new Intent(BROADCAST_PLAYSTATECHANGED));
        handlePlayState();
    }

    /**
     * Initial start of playback. Acquires wakelock and creates a notification
     *
     */
    public void start() {
        mPlayState = PLAYBACKSERVICE_PLAYSTATE_PLAYING;
        sendBroadcast(new Intent(BROADCAST_PLAYSTATECHANGED));
        handlePlayState();
    }

    /**
     * Stop playback.
     */
    public void stop() {
        mPlayState = PLAYBACKSERVICE_PLAYSTATE_STOPPED;
        sendBroadcast(new Intent(BROADCAST_PLAYSTATECHANGED));
        handlePlayState();
    }

    /**
     * Pause playback.
     */
    public void pause() {
        mPlayState = PLAYBACKSERVICE_PLAYSTATE_PAUSED;
        sendBroadcast(new Intent(BROADCAST_PLAYSTATECHANGED));
        handlePlayState();
    }

    /**
     * Start playing the next Track.
     */
    public void next() {
        Track track = mCurrentPlaylist.getNextTrack();
        if (track != null) {
            try {
                setCurrentTrack(track);
            } catch (IOException e) {
                Log.e(TAG, "next(): " + IOException.class.getName() + ": " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Play the previous track.
     */
    public void previous() {
        Track track = mCurrentPlaylist.getPreviousTrack();
        if (track != null) {
            try {
                setCurrentTrack(track);
            } catch (IOException e) {
                Log.e(TAG, "previous(): " + IOException.class.getName() + ": " + e.getLocalizedMessage());
            }
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

        String extraString = "CODE UNSPECIFIED";
        switch (extra) {
        case MediaPlayer.MEDIA_ERROR_IO:
            extraString = "MEDIA_ERROR_IO";
            break;

        case MediaPlayer.MEDIA_ERROR_MALFORMED:
            extraString = "MEDIA_ERROR_MALFORMED";
            break;

        case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
            extraString = "MEDIA_ERROR_UNSUPPORTED";
            break;
        case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
            extraString = "MEDIA_ERROR_TIMED_OUT";
        }

        Log.e(TAG, "onError - " + whatString + " - " + extraString);
        next();
        return false;
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
        } else
            stop();
    }

    /* (non-Javadoc)
     * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "Mediaplayer is prepared.");
        mIsPreparing = false;
        handlePlayState();
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
        return mIsPreparing;
    }

    /**
     * Get the current Playlist
     *
     * @return
     */
    public Playlist getCurrentPlaylist() {
        return mCurrentPlaylist;
    }

    /**
     * Set the current Playlist to playlist and set the current Track to the
     * Playlist's current Track.
     * 
     * @param playlist
     * @throws IOException
     */
    public void setCurrentPlaylist(Playlist playlist) throws IOException {
        mCurrentPlaylist = playlist;
        setCurrentTrack(mCurrentPlaylist.getCurrentTrack());
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    /**
     * Set the current playlist to shuffle mode.
     *
     * @param shuffled
     */
    public void setShuffled(boolean shuffled) {
        mCurrentPlaylist.setShuffled(shuffled);
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    /**
     * Set the current playlist to repeat mode.
     *
     * @param repeating
     */
    public void setRepeating(boolean repeating) {
        mCurrentPlaylist.setRepeating(repeating);
        sendBroadcast(new Intent(BROADCAST_PLAYLISTCHANGED));
    }

    /**
     * Get the current Track
     *
     * @return
     */
    public Track getCurrentTrack() {

        if (mCurrentPlaylist == null)
            return null;

        return mCurrentPlaylist.getCurrentTrack();
    }

    /**
     * This method sets the current track and prepares it for playback.
     * 
     * @param track
     * @throws IOException
     */
    public void setCurrentTrack(Track track) throws IOException {
        if (!mIsPreparing) {
            mMediaPlayer.reset();
        } else {
            mMediaPlayer.release();
            initMediaPlayer();
        }
        mMediaPlayer.setDataSource(track.getPath());
        mMediaPlayer.prepareAsync();
        mIsPreparing = true;

        sendBroadcast(new Intent(BROADCAST_NEWTRACK));
    }

    /**
     * Create an ongoing notification and start this service in the foreground.
     */
    @SuppressWarnings("deprecation")
    private void createPlayingNotification() {

        Track track = getCurrentTrack();

        Notification notification = new Notification(R.drawable.ic_launcher, track.getName(),
                System.currentTimeMillis());

        Context context = getApplicationContext();
        CharSequence contentTitle = track.getArtist().getName();
        CharSequence contentText = track.getName();
        Intent notificationIntent = new Intent(this, PlaybackActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT,
                notificationIntent, 0);

        notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE
                | Notification.FLAG_NO_CLEAR;
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

        startForeground(3, notification);
    }

    /**
     * Returns the position of playback in the current Track.
     */
    public int getPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    /**
     * Seeks to position msec
     * @param msec
     */
    public void seekTo(int msec) {
        mMediaPlayer.seekTo(msec);
    }
}
