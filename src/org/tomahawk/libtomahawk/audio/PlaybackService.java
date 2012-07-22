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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

public class PlaybackService extends Service implements Handler.Callback, OnCompletionListener,
        OnErrorListener, OnPreparedListener {

    private static String TAG = PlaybackService.class.getName();

    private static final int BROADCAST_NEWTRACK = 0;

    private static PlaybackService mInstance;
    private static final Object[] mWait = new Object[0];

    private Track mCurrentTrack;
    private MediaPlayer mMediaPlayer;
    private Looper mLooper;
    private Handler mHandler;

    /**
     * Constructs a new PlaybackService.
     */
    @Override
    public void onCreate() {

        HandlerThread thread = new HandlerThread("PlaybackService",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mLooper = thread.getLooper();
        mHandler = new Handler(mLooper, this);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);

        mInstance = this;
        synchronized (mWait) {
            mWait.notifyAll();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Track track = (Track) intent.getSerializableExtra("track");

        try {
            setCurrentTrack(track);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return startId;
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
        mMediaPlayer.start();
    }

    /**
     * Stop playback.
     */
    public void stop() {
        mMediaPlayer.stop();
    }

    /**
	 * Pause playback.
	 */
	public void pause() {
		mMediaPlayer.pause();
	}

	/**
	 * Get the PlaybackService for the given Context.
	 */
    public static PlaybackService get(Context context) {
        if (mInstance == null) {
            context.startService(new Intent(context, PlaybackService.class));

            while (mInstance == null) {
                try {
                    synchronized (mWait) {
                        mWait.wait();
                    }
                } catch (InterruptedException ignored) {
                }
            }
        }

        return mInstance;
    }

    /**
     * Returns whether there is a valid PlaybackService instance.
     */
    public static boolean hasInstance() {
        return mInstance != null;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Error with media player");
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
    }

    @Override
    public boolean handleMessage(Message msg) {

        switch (msg.what) {

        case BROADCAST_NEWTRACK:
            stockMusicBroadcast();

        }
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
		Log.d(TAG, "Starting playback.");
        playPause();
    }

    /**
     * Returns whether this PlaybackService is currently playing media.
     */
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    public Track getCurrentTrack() {
        return mCurrentTrack;
    }

    /**
     * This method sets the current back and prepares it for playback.
     * 
     * @param mCurrentTrack
     * @throws IOException
     */
    public void setCurrentTrack(Track track) throws IOException {
        mCurrentTrack = track;

        mMediaPlayer.reset();
        mMediaPlayer.setDataSource(mCurrentTrack.getPath());
        mMediaPlayer.prepareAsync();

        mHandler.sendMessage(mHandler.obtainMessage(BROADCAST_NEWTRACK, -1, 0, null));
    }

    /**
     * Send a broadcast emulating that of the stock music player.
     * 
     * Borrow from Vanilla Music Player. Thanks!
     */
    private void stockMusicBroadcast() {

        Track track = mCurrentTrack;

        Intent intent = new Intent("com.android.music.playstatechanged");
        intent.putExtra("playing", 1);
        if (track != null) {
            intent.putExtra("track", track.getTitle());
            intent.putExtra("album", track.getAlbum());
            intent.putExtra("artist", track.getArtist());
            intent.putExtra("songid", track.getId());
            intent.putExtra("albumid", track.getAlbumId());
        }
        sendBroadcast(intent);
    }

}
