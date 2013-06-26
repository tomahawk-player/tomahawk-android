/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
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
package org.tomahawk.tomahawk_android.utils;

import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.resolver.spotify.LibSpotifyWrapper;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.media.AudioTrack;
import android.media.MediaPlayer;

import java.io.IOException;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 23.06.13
 */
public class TomahawkMediaPlayer
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private MediaPlayer mMediaPlayer;

    private AudioTrack mAudioTrack;

    private boolean mUseMediaPlayer;

    private boolean mIsPreparing;

    private boolean mIsPlaying;

    public int mPositionOffset = 0; //only used for AudioTrack

    private OnPreparedListener mOnPreparedListener;

    private OnErrorListener mOnErrorListener;

    private OnCompletionListener mOnCompletionListener;


    public TomahawkMediaPlayer(MediaPlayer mediaPlayer, AudioTrack audioTrack) {
        mMediaPlayer = mediaPlayer;
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mAudioTrack = audioTrack;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mUseMediaPlayer) {
            onPrepared();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (mUseMediaPlayer) {
            return onError(what, extra);
        } else {
            return false;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mUseMediaPlayer) {
            onCompletion();
        }
    }

    public void onPrepared() {
        mIsPreparing = false;
        mOnPreparedListener.onPrepared(this);
    }

    public boolean onError(int what, int extra) {
        return mOnErrorListener.onError(this, what, extra);
    }

    public void onCompletion() {
        mOnCompletionListener.onCompletion(this);
    }


    public int getCurrentPosition() {
        if (mUseMediaPlayer) {
            return mMediaPlayer.getCurrentPosition();
        } else {
            return mPositionOffset
                    + (mAudioTrack.getPlaybackHeadPosition() / mAudioTrack.getSampleRate()) * 1000;
        }
    }

    public void setVolume(float leftVolume, float rightVolume) {
        mAudioTrack.setStereoVolume(leftVolume, rightVolume);
        mMediaPlayer.setVolume(leftVolume, rightVolume);
    }

    public void release() {
        mAudioTrack.release();
        mMediaPlayer.release();
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

    public void start() {
        mIsPlaying = true;
        if (mUseMediaPlayer) {
            mMediaPlayer.start();
        } else {
            mAudioTrack.play();
        }
    }

    public void pause() {
        mIsPlaying = false;
        if (mUseMediaPlayer) {
            mMediaPlayer.pause();
        } else {
            mAudioTrack.pause();
        }
    }

    public void stop() {
        mIsPlaying = false;
        if (mUseMediaPlayer) {
            mMediaPlayer.stop();
        } else {
            mAudioTrack.pause();
            mAudioTrack.flush();
        }
    }

    public void seekTo(int msec) {
        if (mUseMediaPlayer) {
            mMediaPlayer.seekTo(msec);
        } else {
            mAudioTrack.flush();
            mPositionOffset = msec
                    - (mAudioTrack.getPlaybackHeadPosition() / mAudioTrack.getSampleRate()) * 1000;
            LibSpotifyWrapper.seek(msec);
        }
    }

    public void prepare(Track track) throws IllegalStateException, IOException {
        mIsPreparing = true;
        mAudioTrack.pause();
        mAudioTrack.flush();
        if (!track.isLocal() && track.getResolver().getId() == TomahawkApp.RESOLVER_ID_SPOTIFY) {
            mUseMediaPlayer = false;
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.reset();
            LibSpotifyWrapper.togglePlay(track.getPath(), this);
        } else {
            mUseMediaPlayer = true;
            mMediaPlayer.setDataSource(track.getPath());
            mMediaPlayer.prepare();
        }
    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    public AudioTrack getAudioTrack() {
        return mAudioTrack;
    }

    public boolean isPreparing() {
        return mIsPreparing;
    }
}
