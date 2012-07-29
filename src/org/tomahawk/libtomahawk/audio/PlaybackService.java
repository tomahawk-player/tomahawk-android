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
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

public class PlaybackService extends Service implements Handler.Callback, OnCompletionListener,
        OnErrorListener, OnPreparedListener {

    private static String TAG = PlaybackService.class.getName();

    private final IBinder mBinder = new PlaybackServiceBinder();

    public static final String BROADCAST_NEWTRACK = "org.tomahawk.libtomahawk.audio.PlaybackService.BROADCAST_NEWTRACK";
    private static boolean mIsRunning = false;

    private Playlist mCurrentPlaylist;
    private MediaPlayer mMediaPlayer;
    private PowerManager.WakeLock mWakeLock;

    private class HeadsetBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isPlaying())
                pause();
        }
    }

    public class PlaybackServiceBinder extends Binder {
        PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    /**
     * Constructs a new PlaybackService.
     */
    @Override
    public void onCreate() {

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        registerReceiver(new HeadsetBroadcastReceiver(), new IntentFilter(
                Intent.ACTION_HEADSET_PLUG));
    }

    /**
     * This method is called when this service is started.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        setIsRunning(true);

        return startId;
    }

    /**
     * Called when the service is destroyed.
     */
    @Override
    public void onDestroy() {
        setIsRunning(false);
        stop();
    }

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

    public void start() {
        mWakeLock.acquire();
        mMediaPlayer.start();
        createPlayingNotification();
    }

    /**
     * Stop playback.
     */
    public void stop() {
        mMediaPlayer.stop();

        if (mWakeLock.isHeld())
            mWakeLock.release();
        stopForeground(true);
    }

    /**
     * Pause playback.
     */
    public void pause() {
        mMediaPlayer.pause();

        if (mWakeLock.isHeld())
            mWakeLock.release();
        stopForeground(true);
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

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Error with media player");
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

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
     * Returns true if the service is running.
     * 
     * @return
     */
    public boolean isRunning() {
        return mIsRunning;
    }

    /**
     * Sets the running state of the PlaybackService.
     * 
     * @param running
     */
    public void setIsRunning(boolean running) {
        mIsRunning = running;
    }

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
    }

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
    private void createPlayingNotification() {

        Track track = getCurrentTrack();

        Notification notification = new Notification(R.drawable.ic_launcher, track.getTitle(),
                System.currentTimeMillis());

        Context context = getApplicationContext();
        CharSequence contentTitle = track.getArtist().getName();
        CharSequence contentText = track.getTitle();
        Intent notificationIntent = new Intent(this, PlaybackActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;
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
