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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

public class PlaybackService extends Service implements OnCompletionListener,
        OnErrorListener, OnPreparedListener {

    private static String TAG = PlaybackService.class.getName();

    private final IBinder mBinder = new PlaybackServiceBinder();

    public static final String BROADCAST_NEWTRACK = "org.tomahawk.libtomahawk.audio.PlaybackService.BROADCAST_NEWTRACK";
    public static final String BROADCAST_PLAYLISTCHANGED = "org.tomahawk.libtomahawk.audio.PlaybackService.BROADCAST_PLAYLISTCHANGED";
    public static final String BROADCAST_PLAYSTATECHANGED = "org.tomahawk.libtomahawk.audio.PlaybackService.BROADCAST_PLAYSTATECHANGED";

    private Playlist mCurrentPlaylist;
    private MediaPlayer mMediaPlayer;
    private PowerManager.WakeLock mWakeLock;
    private HeadsetBroadcastReceiver mHeadsetBroadcastReceiver;
    private Handler mHandler;

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

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isPlaying())
                pause();
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

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        mHeadsetBroadcastReceiver = new HeadsetBroadcastReceiver();
        registerReceiver(mHeadsetBroadcastReceiver, new IntentFilter(
                Intent.ACTION_HEADSET_PLUG));
    }

    /* (non-Javadoc)
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        unregisterReceiver(mHeadsetBroadcastReceiver);
        stop();
    }

    /* (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Start or pause playback.
     */
    public void playPause() {
        if (isPlaying())
            pause();
        else
            start();
    }

    /**
     * Initial start of playback. Acquires wakelock and creates a notification
     *
     */
    public void start() {
        if (!mWakeLock.isHeld())
            mWakeLock.acquire();
        mMediaPlayer.start();
        createPlayingNotification();
        
        sendBroadcast(new Intent(BROADCAST_PLAYSTATECHANGED));
    }

    /**
     * Stop playback.
     */
    public void stop() {
        mMediaPlayer.stop();

        if (mWakeLock.isHeld())
            mWakeLock.release();
        stopForeground(true);
        
        sendBroadcast(new Intent(BROADCAST_PLAYSTATECHANGED));
    }

    /**
     * Pause playback.
     */
    public void pause() {
        mMediaPlayer.pause();

        if (mWakeLock.isHeld())
            mWakeLock.release();
        stopForeground(true);
        
        sendBroadcast(new Intent(BROADCAST_PLAYSTATECHANGED));
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
                e.printStackTrace();
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
                e.printStackTrace();
            }
        }
    }

    /* (non-Javadoc)
     * @see android.media.MediaPlayer.OnErrorListener#onError(android.media.MediaPlayer, int, int)
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Error with media player");
        stop();
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
		Log.d(TAG, "Starting playback.");
        start();
    }

    /**
     * Returns whether this PlaybackService is currently playing media.
     */
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
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
     * This method sets the current back and prepares it for playback.
     * 
     * @param mCurrentPlaylist
     * @throws IOException
     */
    private void setCurrentTrack(Track track) throws IOException {

        mMediaPlayer.reset();
        mMediaPlayer.setDataSource(track.getPath());
        mMediaPlayer.prepareAsync();

        sendBroadcast(new Intent(BROADCAST_NEWTRACK));
    }

    /**
     * Create an ongoing notification and start this service in the foreground.
     */
    @SuppressWarnings("deprecation")
    private void createPlayingNotification() {

        Track track = getCurrentTrack();

        Notification notification = new Notification(R.drawable.ic_launcher, track.getTitle(),
                System.currentTimeMillis());

        Context context = getApplicationContext();
        CharSequence contentTitle = track.getArtist().getName();
        CharSequence contentText = track.getTitle();
        Intent notificationIntent = new Intent(this, PlaybackActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT, notificationIntent, 0);

        notification.flags |= Notification.FLAG_ONGOING_EVENT
                | Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;
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
