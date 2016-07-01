/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
 *   Copyright 2016, Anton Romanov
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
package org.tomahawk.tomahawk_android.mediaplayers;

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.utils.ThreadManager;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.io.IOException;

public class AndroidMediaPlayer extends TomahawkMediaPlayer {

    private static final String TAG = AndroidMediaPlayer.class.getSimpleName();

    private static MediaPlayer sMediaPlayer = null;

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    private int mPlayState = PlaybackStateCompat.STATE_NONE;

    private TomahawkMediaPlayerCallback mMediaPlayerCallback;

    private class CompletionListener implements MediaPlayer.OnCompletionListener {

        public void onCompletion(MediaPlayer mp) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "onCompletion()");
                    if (mMediaPlayerCallback != null) {
                        mMediaPlayerCallback.onCompletion(AndroidMediaPlayer.this, mPreparedQuery);
                    } else {
                        Log.e(TAG,
                                "Wasn't able to call onCompletion because callback object is null");
                    }
                }
            };
            ThreadManager.get().executePlayback(AndroidMediaPlayer.this, r);
        }
    }

    @Override
    public void play() {
        mPlayState = PlaybackStateCompat.STATE_PLAYING;
        handlePlayState();
    }

    @Override
    public void pause() {
        mPlayState = PlaybackStateCompat.STATE_PAUSED;
        handlePlayState();
    }

    @Override
    public void seekTo(long msec) {
        if (sMediaPlayer != null) {
            try {
                sMediaPlayer.seekTo((int) msec);
            } catch (IllegalStateException e) {
                //ignored
            }
        }
    }

    @Override
    public void prepare(final Query query, final TomahawkMediaPlayerCallback callback) {
        Log.d(TAG, "prepare() query: " + query);
        mMediaPlayerCallback = callback;
        mPreparedQuery = null;
        mPreparingQuery = query;
        if (sMediaPlayer != null) {
            try {
                sMediaPlayer.stop();
            } catch (IllegalStateException e) {
                //ignored
            }
        }
        getStreamUrl(query.getPreferredTrackResult()).done(new DoneCallback<String>() {
            @Override
            public void onDone(String url) {
                Log.d(TAG, "Received stream url: " + url + " for query: " + query);
                if (mPreparingQuery != null && mPreparingQuery == query) {
                    Log.d(TAG, "Starting to prepare stream url: " + url + " for query: " + query);
                    if (sMediaPlayer != null) {
                        try {
                            sMediaPlayer.stop();
                        } catch (IllegalStateException e) {
                            //ignored
                        }
                        sMediaPlayer.release();
                    }
                    sMediaPlayer = new MediaPlayer();

                    sMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                    try {
                        sMediaPlayer.setDataSource(url);
                        sMediaPlayer.prepare();
                    } catch (IOException | IllegalStateException e) {
                        Log.e(TAG, "prepare - ", e);
                        callback.onError(AndroidMediaPlayer.this, "MediaPlayerEncounteredError");
                    }

                    sMediaPlayer.setOnCompletionListener(new CompletionListener());

                    mPreparedQuery = mPreparingQuery;
                    mPreparingQuery = null;
                    handlePlayState();
                    callback.onPrepared(AndroidMediaPlayer.this, mPreparedQuery);
                    Log.d(TAG, "onPrepared() url: " + url + " for query: " + query);
                } else {
                    Log.d(TAG, "Ignoring stream url: " + url + " for query: " + query
                            + ", because preparing query is: " + mPreparingQuery);
                }
            }
        });
    }

    @Override
    public void release() {
        Log.d(TAG, "release");
        mPreparedQuery = null;
        mPreparingQuery = null;
        if (sMediaPlayer != null) {
            try {
                sMediaPlayer.stop();
            } catch (IllegalStateException e) {
                //ignored
            }
            sMediaPlayer.release();
        }
        mMediaPlayerCallback = null;
    }

    @Override
    public long getPosition() {
        if (sMediaPlayer != null) {
            try {
                return sMediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                //ignored
            }
        }
        return 0;
    }

    @Override
    public void setBitrate(int mode) {
    }

    @Override
    public boolean isPlaying(Query query) {
        try {
            return sMediaPlayer != null && sMediaPlayer.isPlaying();
        } catch (IllegalStateException e) {
            //ignored
        }
        return false;
    }

    @Override
    public boolean isPreparing(Query query) {
        return mPreparingQuery != null && mPreparingQuery == query;
    }

    @Override
    public boolean isPrepared(Query query) {
        return mPreparedQuery != null && mPreparedQuery == query;
    }

    private void handlePlayState() {
        if (sMediaPlayer != null && mPreparedQuery != null) {
            try {
                if (mPlayState == PlaybackStateCompat.STATE_PAUSED
                        && sMediaPlayer.isPlaying()) {
                    sMediaPlayer.pause();
                } else if (mPlayState == PlaybackStateCompat.STATE_PLAYING
                        && !sMediaPlayer.isPlaying()) {
                    sMediaPlayer.start();
                }
            } catch (IllegalStateException e) {
                //ignored
            }
        }
    }
}
