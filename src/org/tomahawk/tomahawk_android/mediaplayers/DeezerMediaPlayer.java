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

import com.deezer.sdk.network.connect.DeezerConnect;
import com.deezer.sdk.network.request.event.DeezerError;
import com.deezer.sdk.player.TrackPlayer;
import com.deezer.sdk.player.event.OnBufferErrorListener;
import com.deezer.sdk.player.event.OnPlayerErrorListener;
import com.deezer.sdk.player.event.OnPlayerStateChangeListener;
import com.deezer.sdk.player.event.PlayerState;
import com.deezer.sdk.player.exception.TooManyPlayersExceptions;
import com.deezer.sdk.player.networkcheck.WifiAndMobileNetworkStateChecker;

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.ScriptJob;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverAccessTokenResult;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class DeezerMediaPlayer implements TomahawkMediaPlayer {

    private static final String TAG = DeezerMediaPlayer.class.getSimpleName();

    public final static String APP_ID = "138751";

    private static class Holder {

        private static final DeezerMediaPlayer instance = new DeezerMediaPlayer();

    }

    private TomahawkMediaPlayerCallback mMediaPlayerCallback;

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    private TrackPlayer mPlayer;

    private final PlayerHandler mPlayerHandler = new PlayerHandler();

    private class PlayerHandler implements OnPlayerStateChangeListener, OnPlayerErrorListener,
            OnBufferErrorListener {

        @Override
        public void onBufferError(final Exception ex, double percent) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "onBufferError: ", ex);
                    Log.d(TAG, "onError()");
                    mPreparedQuery = null;
                    mPreparingQuery = null;
                    mMediaPlayerCallback.onError("onBufferError");
                }
            });
        }

        @Override
        public void onPlayerError(final Exception ex, long timePosition) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "onPlayerError: ", ex);
                    mPreparedQuery = null;
                    mPreparingQuery = null;
                    mMediaPlayerCallback.onError("onPlayerError");
                }
            });
        }

        @Override
        public void onPlayerStateChange(final PlayerState state, long timePosition) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (state == PlayerState.READY) {
                        Log.d(TAG, "onPrepared()");
                        mPreparedQuery = mPreparingQuery;
                        mPreparingQuery = null;
                        mMediaPlayerCallback.onPrepared(mPreparedQuery);
                    } else if (state == PlayerState.PLAYBACK_COMPLETED) {
                        Log.d(TAG, "onCompletion()");
                        mMediaPlayerCallback.onCompletion(mPreparedQuery);
                    }
                }
            });
        }
    }

    private DeezerMediaPlayer() {
    }

    public static DeezerMediaPlayer getInstance() {
        return Holder.instance;
    }

    /**
     * Start playing the previously prepared {@link org.tomahawk.libtomahawk.collection.Track}
     */
    @Override
    public void start() throws IllegalStateException {
        Log.d(TAG, "start()");
        if (mPlayer != null && mPlayer.getPlayerState() != PlayerState.RELEASED) {
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
        if (mPlayer != null && mPlayer.getPlayerState() != PlayerState.RELEASED) {
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
        Log.d(TAG, "seekTo() state: "
                + (mPlayer != null ? mPlayer.getPlayerState().toString() : "null"));
        if (mPlayer != null && mPlayer.getPlayerState() != PlayerState.RELEASED) {
            synchronized (this) {
                try {
                    mPlayer.seek(msec);
                } catch (Exception e) {
                    Log.e(TAG, "seekTo: " + e.getClass() + ": " + e.getLocalizedMessage());
                    Log.d(TAG, "onError()");
                    mPreparedQuery = null;
                    mPreparingQuery = null;
                    mMediaPlayerCallback.onError("Exception while seeking");
                }
            }
        }
    }

    /**
     * Prepare the given url
     */
    @Override
    public TomahawkMediaPlayer prepare(final Application application, final Query query,
            TomahawkMediaPlayerCallback callback) {
        Log.d(TAG, "prepare()");
        mMediaPlayerCallback = callback;
        mPreparedQuery = null;
        mPreparingQuery = query;
        release();

        ((ScriptResolver) PipeLine.getInstance().getResolver(TomahawkApp.PLUGINNAME_DEEZER))
                .getAccessToken(new ScriptJob.ResultsCallback<ScriptResolverAccessTokenResult>(
                        ScriptResolverAccessTokenResult.class) {
                    @Override
                    public void onReportResults(ScriptResolverAccessTokenResult results) {
                        DeezerConnect deezerConnect = new DeezerConnect(APP_ID);
                        deezerConnect.setAccessToken(TomahawkApp.getContext(), results.accessToken);
                        deezerConnect.setAccessExpires(results.accessTokenExpires);
                        try {
                            if (mPlayer == null
                                    || mPlayer.getPlayerState() == PlayerState.RELEASED) {
                                mPlayer = new TrackPlayer(application, deezerConnect,
                                        new WifiAndMobileNetworkStateChecker());
                                mPlayer.addOnBufferErrorListener(mPlayerHandler);
                                mPlayer.addOnPlayerErrorListener(mPlayerHandler);
                                mPlayer.addOnPlayerStateChangeListener(mPlayerHandler);
                            }
                        } catch (TooManyPlayersExceptions | DeezerError e) {
                            Log.e(TAG, "<init>: " + e.getClass() + ": " + e.getLocalizedMessage());
                        }
                        String strippedPath = query.getPreferredTrackResult().getPath()
                                .replace("deezer://track/", "");
                        String[] parts = strippedPath.split("/");
                        synchronized (this) {
                            mPlayer.playTrack(Long.valueOf(parts[0]));
                        }
                    }
                });
        return this;
    }

    @Override
    public void release() {
        Log.d(TAG, "release()");
        mPreparedQuery = null;
        synchronized (this) {
            if (mPlayer != null && mPlayer.getPlayerState() != PlayerState.RELEASED
                    && mPlayer.getPlayerState() != PlayerState.STOPPED) {
                mPlayer.stop();
            }
        }
    }

    /**
     * @return the current track position
     */
    @Override
    public int getPosition() {
        if (mPlayer != null && mPlayer.getPlayerState() != PlayerState.RELEASED
                && mPreparedQuery != null) {
            synchronized (this) {
                return (int) mPlayer.getPosition();
            }
        } else {
            return 0;
        }
    }

    @Override
    public boolean isPlaying(Query query) {
        Log.d(TAG, "isPlaying: state: "
                + (mPlayer != null ? mPlayer.getPlayerState().toString() : "null"));
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
}
