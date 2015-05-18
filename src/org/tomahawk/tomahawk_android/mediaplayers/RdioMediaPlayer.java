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

import com.rdio.android.api.Rdio;
import com.rdio.android.api.RdioListener;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.ScriptJob;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverAccessTokenResult;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverAppKeysResult;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.app.Application;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

import de.greenrobot.event.EventBus;

/**
 * This class wraps a standard {@link android.media.MediaPlayer} object.
 */
public class RdioMediaPlayer
        implements TomahawkMediaPlayer, android.media.MediaPlayer.OnPreparedListener,
        android.media.MediaPlayer.OnErrorListener, android.media.MediaPlayer.OnCompletionListener {

    private static final String TAG = RdioMediaPlayer.class.getSimpleName();

    private static class Holder {

        private static final RdioMediaPlayer instance = new RdioMediaPlayer();

    }

    private TomahawkMediaPlayerCallback mMediaPlayerCallback;

    private android.media.MediaPlayer mMediaPlayer;

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    private Rdio mRdio;

    private RdioListener mRdioListener = new RdioListener() {

        @Override
        public void onRdioReadyForPlayback() {
            Log.d(TAG, "Rdio SDK is ready for playback");
            prepareQuery(mPreparingQuery);
        }

        @Override
        public void onRdioUserPlayingElsewhere() {
            Log.d(TAG, "onRdioUserPlayingElsewhere()");
            AuthenticatorManager.ConfigTestResultEvent event
                    = new AuthenticatorManager.ConfigTestResultEvent();
            event.mComponent = this;
            event.mType = AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_PLAYINGELSEWHERE;
            EventBus.getDefault().post(event);
            final ScriptResolver rdioResolver = (ScriptResolver) PipeLine.getInstance()
                    .getResolver(TomahawkApp.PLUGINNAME_RDIO);
            AuthenticatorManager.showToast(rdioResolver.getPrettyName(), event);
        }

        /*
         * Dispatched by the Rdio object once the setTokenAndSecret call has finished, and the credentials are
         * ready to be used to make API calls.  The token & token secret are passed in so that you can
         * save/cache them for future re-use.
         * @see com.rdio.android.api.RdioListener#onRdioAuthorised(java.lang.String, java.lang.String)
         */
        @Override
        public void onRdioAuthorised(String extraToken, String extraTokenSecret) {
            Log.d(TAG, "Rdio Application authorised.");
        }
    };

    private RdioMediaPlayer() {
    }

    public static RdioMediaPlayer getInstance() {
        return Holder.instance;
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
    public TomahawkMediaPlayer prepare(Application application, Query query,
            TomahawkMediaPlayerCallback callback) {
        Log.d(TAG, "prepare()");
        mMediaPlayerCallback = callback;
        mPreparedQuery = null;
        mPreparingQuery = query;
        release();

        if (mRdio == null || !mRdio.isReady()) {
            final ScriptResolver rdioResolver = (ScriptResolver) PipeLine.getInstance()
                    .getResolver(TomahawkApp.PLUGINNAME_RDIO);
            rdioResolver.getAppKeys(new ScriptJob.ResultsCallback<ScriptResolverAppKeysResult>(
                    ScriptResolverAppKeysResult.class) {
                @Override
                public void onReportResults(final ScriptResolverAppKeysResult appKeysResult) {
                    rdioResolver.getAccessToken(
                            new ScriptJob.ResultsCallback<ScriptResolverAccessTokenResult>(
                                    ScriptResolverAccessTokenResult.class) {
                                @Override
                                public void onReportResults(
                                        ScriptResolverAccessTokenResult accessTokenResult) {
                                    mRdio = new Rdio(appKeysResult.appKey,
                                            appKeysResult.appSecret,
                                            accessTokenResult.accessToken,
                                            accessTokenResult.accessTokenSecret,
                                            TomahawkApp.getContext(), mRdioListener);
                                    mRdio.prepareForPlayback();
                                }
                            });
                }
            });
        } else {
            prepareQuery(mPreparingQuery);
        }
        return this;
    }

    private void prepareQuery(Query query) {
        if (query != null) {
            try {
                mMediaPlayer = mRdio.getPlayerForTrack(
                        mPreparingQuery.getPreferredTrackResult().getPath()
                                .replace("rdio://track/", ""), null, true);
            } catch (IllegalStateException e) {
                Log.e(TAG, "prepare: " + e.getClass() + ": "
                        + e.getLocalizedMessage());
                return;
            }
            if (mMediaPlayer == null) {
                Log.e(TAG, "prepare: MediaPlayer returned by "
                        + "Rdio#getPlayerForTrack() was null");
                return;
            }
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnPreparedListener(RdioMediaPlayer.this);
            mMediaPlayer.setOnErrorListener(RdioMediaPlayer.this);
            mMediaPlayer.setOnCompletionListener(RdioMediaPlayer.this);
            try {
                mMediaPlayer.prepare();
            } catch (IOException e) {
                Log.e(TAG, "prepare: " + e.getClass() + ": "
                        + e.getLocalizedMessage());
            }
        }
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
    public void onPrepared(android.media.MediaPlayer mp) {
        Log.d(TAG, "onPrepared()");
        mPreparedQuery = mPreparingQuery;
        mPreparingQuery = null;
        mMediaPlayerCallback.onPrepared(mPreparedQuery);
    }

    @Override
    public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onError()");
        String whatString = "CODE UNSPECIFIED";
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                whatString = "MEDIA_ERROR_UNKNOWN";
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                whatString = "MEDIA_ERROR_SERVER_DIED";
        }
        mPreparedQuery = null;
        mPreparingQuery = null;
        mMediaPlayerCallback.onError(whatString);
        return false;
    }

    @Override
    public void onCompletion(android.media.MediaPlayer mp) {
        Log.d(TAG, "onCompletion()");
        mMediaPlayerCallback.onCompletion(mPreparedQuery);
    }
}
