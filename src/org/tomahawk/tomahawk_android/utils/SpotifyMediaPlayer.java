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

import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.spotify.SpotifyServiceUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.SpotifyService;

import android.app.Application;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

/**
 * This class wraps all functionality to be able to directly playback spotify-resolved tracks with
 * OpenSLES .
 */
public class SpotifyMediaPlayer implements MediaPlayerInterface {

    private static String TAG = SpotifyMediaPlayer.class.getSimpleName();

    private static SpotifyMediaPlayer instance;

    private MediaPlayer.OnPreparedListener mOnPreparedListener;

    private MediaPlayer.OnCompletionListener mOnCompletionListener;

    private boolean mIsPlaying;

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    private boolean mOverrideCurrentPosition = false;

    private int mSpotifyCurrentPosition = 0;

    private boolean mSpotifyIsInitialized;

    private Messenger mToSpotifyMessenger = null;

    private Messenger mFromSpotifyMessenger = new Messenger(new FromSpotifyHandler());

    private SpotifyMediaPlayer() {
    }

    public static SpotifyMediaPlayer getInstance() {
        if (instance == null) {
            synchronized (SpotifyMediaPlayer.class) {
                if (instance == null) {
                    instance = new SpotifyMediaPlayer();
                }
            }
        }
        return instance;
    }

    /**
     * Handler of incoming messages from the SpotifyService's messenger.
     */
    private class FromSpotifyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SpotifyService.MSG_ONINIT:
                    mSpotifyIsInitialized = true;
                    break;
                case SpotifyService.MSG_ONPREPARED:
                    onPrepared(null);
                    break;
                case SpotifyService.MSG_ONPLAYERENDOFTRACK:
                    onCompletion(null);
                    break;
                case SpotifyService.MSG_ONPLAYERPOSITIONCHANGED:
                    if (!mOverrideCurrentPosition) {
                        mSpotifyCurrentPosition = msg.arg1;
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    public void setToSpotifyMessenger(Messenger toSpotifyMessenger) {
        mToSpotifyMessenger = toSpotifyMessenger;
        if (mToSpotifyMessenger != null) {
            SpotifyServiceUtils.registerMsg(mToSpotifyMessenger, mFromSpotifyMessenger);
        }
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
    }

    /**
     * Start playing the previously prepared {@link org.tomahawk.libtomahawk.collection.Track}
     */
    @Override
    public void start() {
        Log.d(TAG, "start()");
        mIsPlaying = true;
        if (mToSpotifyMessenger != null) {
            if (mSpotifyIsInitialized) {
                SpotifyServiceUtils.sendMsg(mToSpotifyMessenger, SpotifyService.MSG_PLAY);
            }
        } else {
            TomahawkApp.getContext()
                    .sendBroadcast(new Intent(SpotifyService.REQUEST_SPOTIFYSERVICE));
        }
    }

    /**
     * Pause playing the current {@link org.tomahawk.libtomahawk.collection.Track}
     */
    @Override
    public void pause() {
        Log.d(TAG, "pause()");
        mIsPlaying = false;
        if (mToSpotifyMessenger != null) {
            if (mSpotifyIsInitialized) {
                SpotifyServiceUtils.sendMsg(mToSpotifyMessenger, SpotifyService.MSG_PAUSE);
            }
        } else {
            TomahawkApp.getContext()
                    .sendBroadcast(new Intent(SpotifyService.REQUEST_SPOTIFYSERVICE));
        }
    }

    /**
     * Seek to the given playback position (in ms)
     */
    @Override
    public void seekTo(int msec) {
        Log.d(TAG, "seekTo()");
        if (mToSpotifyMessenger != null) {
            if (mSpotifyIsInitialized) {
                SpotifyServiceUtils.sendMsg(mToSpotifyMessenger, SpotifyService.MSG_SEEK, msec);
                mSpotifyCurrentPosition = msec;
                mOverrideCurrentPosition = true;
                // After 1 second, we set mOverrideCurrentPosition to false again
                new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        mOverrideCurrentPosition = false;
                    }
                }.sendEmptyMessageDelayed(1337, 1000);
            }
        } else {
            TomahawkApp.getContext()
                    .sendBroadcast(new Intent(SpotifyService.REQUEST_SPOTIFYSERVICE));
        }
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
        mSpotifyCurrentPosition = 0;
        mPreparedQuery = null;
        mPreparingQuery = query;
        if (mToSpotifyMessenger != null) {
            if (mSpotifyIsInitialized) {
                SpotifyServiceUtils.sendMsg(mToSpotifyMessenger, SpotifyService.MSG_PREPARE,
                        query.getPreferredTrackResult().getPath());
            }
        } else {
            TomahawkApp.getContext()
                    .sendBroadcast(new Intent(SpotifyService.REQUEST_SPOTIFYSERVICE));
        }
        return this;
    }

    @Override
    public void release() {
        Log.d(TAG, "release()");
        pause();
    }

    /**
     * @return the current track position
     */
    @Override
    public int getPosition() {
        return mSpotifyCurrentPosition;
    }

    @Override
    public boolean isPlaying(Query query) {
        return mPreparedQuery == query && mIsPlaying;
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
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion()");
        mOnCompletionListener.onCompletion(mp);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }
}
