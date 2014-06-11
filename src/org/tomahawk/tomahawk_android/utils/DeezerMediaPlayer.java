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
package org.tomahawk.tomahawk_android.utils;

import com.deezer.sdk.network.request.event.DeezerError;
import com.deezer.sdk.player.TrackPlayer;
import com.deezer.sdk.player.event.OnBufferErrorListener;
import com.deezer.sdk.player.event.OnPlayerErrorListener;
import com.deezer.sdk.player.event.OnPlayerStateChangeListener;
import com.deezer.sdk.player.event.PlayerState;
import com.deezer.sdk.player.exception.TooManyPlayersExceptions;
import com.deezer.sdk.player.networkcheck.WifiAndMobileNetworkStateChecker;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.DeezerAuthenticatorUtils;
import org.tomahawk.libtomahawk.resolver.Query;

import android.app.Application;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class DeezerMediaPlayer
        implements MediaPlayerInterface, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static String TAG = DeezerMediaPlayer.class.getName();

    private static DeezerMediaPlayer instance;

    private MediaPlayer.OnPreparedListener mOnPreparedListener;

    private MediaPlayer.OnCompletionListener mOnCompletionListener;

    private MediaPlayer.OnErrorListener mOnErrorListener;

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    private TrackPlayer mPlayer;

    private PlayerHandler mPlayerHandler;

    private class PlayerHandler implements OnPlayerStateChangeListener, OnPlayerErrorListener,
            OnBufferErrorListener {

        @Override
        public void onBufferError(final Exception ex, double percent) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "onBufferError: ", ex);
                    onError(null, 0, 0);
                }
            });
        }

        @Override
        public void onPlayerError(final Exception ex, long timePosition) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "onPlayerError: ", ex);
                    onError(null, 0, 0);
                }
            });
        }

        @Override
        public void onPlayerStateChange(final PlayerState state, long timePosition) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (state == PlayerState.READY) {
                        onPrepared(null);
                    } else if (state == PlayerState.PLAYBACK_COMPLETED) {
                        onCompletion(null);
                    }
                }
            });
        }
    }

    private DeezerMediaPlayer() {
        mPlayerHandler = new PlayerHandler();
    }

    public static DeezerMediaPlayer getInstance() {
        if (instance == null) {
            synchronized (DeezerMediaPlayer.class) {
                if (instance == null) {
                    instance = new DeezerMediaPlayer();
                }
            }
        }
        return instance;
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
    }

    /**
     * Start playing the previously prepared {@link org.tomahawk.libtomahawk.collection.Track}
     */
    @Override
    public void start() throws IllegalStateException {
        Log.d(TAG, "start()");
        if (mPlayer != null) {
            synchronized (this) {
                if (!isPlaying(mPreparedQuery)
                        && mPlayer.getPlayerState() != PlayerState.WAITING_FOR_DATA
                        && mPlayer.getPlayerState() != PlayerState.STOPPED) {
                    mPlayer.play();
                }
            }
        }
    }

    /**
     * Pause playing the current {@link org.tomahawk.libtomahawk.collection.Track}
     */
    @Override
    public void pause() throws IllegalStateException {
        Log.d(TAG, "pause()");
        if (mPlayer != null) {
            synchronized (this) {
                if (isPlaying(mPreparedQuery)
                        && mPlayer.getPlayerState() != PlayerState.WAITING_FOR_DATA
                        && mPlayer.getPlayerState() != PlayerState.STOPPED) {
                    mPlayer.pause();
                }
            }
        }
    }

    /**
     * Seek to the given playback position (in ms)
     */
    @Override
    public void seekTo(int msec) throws IllegalStateException {
        Log.d(TAG, "seekTo()");
        if (mPlayer != null) {
            synchronized (this) {
                mPlayer.seek(msec);
            }
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
        mOnErrorListener = onErrorListener;
        mPreparedQuery = null;
        mPreparingQuery = query;
        release();
        DeezerAuthenticatorUtils authUtils =
                (DeezerAuthenticatorUtils) AuthenticatorManager.getInstance()
                        .getAuthenticatorUtils(
                                AuthenticatorManager.AUTHENTICATOR_ID_DEEZER);
        try {
            if (mPlayer == null) {
                mPlayer = new TrackPlayer(application, authUtils.getDeezerConnect(),
                        new WifiAndMobileNetworkStateChecker());
                mPlayer.addOnBufferErrorListener(mPlayerHandler);
                mPlayer.addOnPlayerErrorListener(mPlayerHandler);
                mPlayer.addOnPlayerStateChangeListener(mPlayerHandler);
            }
        } catch (TooManyPlayersExceptions e) {
            Log.e(TAG, "<init>: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (DeezerError e) {
            Log.e(TAG, "<init>: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        String strippedPath = query.getPreferredTrackResult().getPath()
                .replace("deezer://track/", "");
        String[] parts = strippedPath.split("/");
        synchronized (this) {
            mPlayer.playTrack(Long.valueOf(parts[0]));
        }
        return this;
    }

    @Override
    public void release() {
        Log.d(TAG, "release()");
        mPreparedQuery = null;
        synchronized (this) {
            if (mPlayer != null && mPlayer.getPlayerState() != PlayerState.STOPPED) {
                mPlayer.stop();
            }
        }
    }

    /**
     * @return the current track position
     */
    @Override
    public int getPosition() {
        if (mPlayer != null && mPreparedQuery != null) {
            synchronized (this) {
                return (int) mPlayer.getPosition();
            }
        } else {
            return 0;
        }
    }

    @Override
    public boolean isPlaying(Query query) {
        Log.d(TAG, "isPlaying: state: " + mPlayer.getPlayerState().toString());
        synchronized (this) {
            return mPlayer != null && mPreparedQuery == query
                    && mPlayer.getPlayerState() == PlayerState.PLAYING;
        }
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
