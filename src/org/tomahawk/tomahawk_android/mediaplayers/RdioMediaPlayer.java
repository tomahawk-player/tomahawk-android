/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.RdioAuthenticatorUtils;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.MediaPlayerInterface;

import android.app.Application;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

/**
 * This class wraps a standard {@link android.media.MediaPlayer} object.
 */
public class RdioMediaPlayer implements MediaPlayerInterface, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static final String TAG = RdioMediaPlayer.class.getSimpleName();

    private static class Holder {

        private static final RdioMediaPlayer instance = new RdioMediaPlayer();

    }

    private MediaPlayer.OnPreparedListener mOnPreparedListener;

    private MediaPlayer.OnCompletionListener mOnCompletionListener;

    private MediaPlayer.OnErrorListener mOnErrorListener;

    private MediaPlayer mMediaPlayer;

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    private RdioMediaPlayer() {
    }

    public static RdioMediaPlayer getInstance() {
        return Holder.instance;
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(leftVolume, rightVolume);
        }
    }

    /**
     * Start playing the previously prepared {@link org.tomahawk.libtomahawk.collection.Track}
     */
    @Override
    public void start() throws IllegalStateException {
        Log.d(TAG, "start()");
        if (mMediaPlayer != null) {
            if (!mMediaPlayer.isPlaying()) {
                mMediaPlayer.start();
            }
            // mMediaplayer.seekTo(0) should be called whenever a Track has just been prepared
            // and is being started. This workaround is needed because of a bug in Android 4.4.
            if (mMediaPlayer.getCurrentPosition() == 0) {
                mMediaPlayer.seekTo(0);
            }
        }
    }

    /**
     * Pause playing the current {@link org.tomahawk.libtomahawk.collection.Track}
     */
    @Override
    public void pause() throws IllegalStateException {
        Log.d(TAG, "pause()");
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
        }
    }

    /**
     * Seek to the given playback position (in ms)
     */
    @Override
    public void seekTo(int msec) throws IllegalStateException {
        Log.d(TAG, "seekTo()");
        // seekTo is currently disabled due to a bug in the rdio SDK
        // (https://github.com/rdio/api/issues/114)
    }

    /**
     * Prepare the given url
     */
    @Override
    public MediaPlayerInterface prepare(Application application, Query query,
            MediaPlayer.OnPreparedListener onPreparedListener,
            MediaPlayer.OnCompletionListener onCompletionListener,
            MediaPlayer.OnErrorListener onErrorListener) {
        Log.d(TAG, "prepare()");
        mOnPreparedListener = onPreparedListener;
        mOnCompletionListener = onCompletionListener;
        mOnErrorListener = onErrorListener;
        mPreparedQuery = null;
        mPreparingQuery = query;
        release();
        RdioAuthenticatorUtils authUtils = (RdioAuthenticatorUtils) AuthenticatorManager
                .getInstance().getAuthenticatorUtils(TomahawkApp.PLUGINNAME_RDIO);
        if (authUtils.getRdio() == null) {
            return null;
        }
        try {
            mMediaPlayer = authUtils.getRdio().getPlayerForTrack(
                    query.getPreferredTrackResult().getPath().replace("rdio://track/", ""), null,
                    true);
        } catch (IllegalStateException e) {
            Log.e(TAG, "prepare: " + e.getClass() + ": " + e.getLocalizedMessage());
            return null;
        }
        if (mMediaPlayer == null) {
            return null;
        }
        try {
            mMediaPlayer.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare: " + e.getClass() + ": " + e.getLocalizedMessage());
            return null;
        }
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        return this;
    }

    @Override
    public void release() {
        Log.d(TAG, "release()");
        mPreparedQuery = null;
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    /**
     * @return the current track position
     */
    @Override
    public int getPosition() {
        if (mMediaPlayer != null && mPreparedQuery != null) {
            return mMediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isPlaying(Query query) {
        return mMediaPlayer != null && mPreparedQuery == query && mMediaPlayer.isPlaying();
    }

    @Override
    public boolean isPreparing(Query query) {
        return mPreparingQuery == query;
    }

    @Override
    public boolean isPrepared(Query query) {
        return mPreparedQuery == query;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared()");
        mPreparedQuery = mPreparingQuery;
        mPreparingQuery = null;
        mOnPreparedListener.onPrepared(mp);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onError()");
        mPreparedQuery = null;
        mPreparingQuery = null;
        return mOnErrorListener.onError(mp, what, extra);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion()");
        mOnCompletionListener.onCompletion(mp);
    }
}
