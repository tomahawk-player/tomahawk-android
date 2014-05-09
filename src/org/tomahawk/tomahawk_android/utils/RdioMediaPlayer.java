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

import com.rdio.android.api.OAuth1WebViewActivity;
import com.rdio.android.api.Rdio;
import com.rdio.android.api.RdioListener;

import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * This class wraps a standard {@link android.media.MediaPlayer} object.
 */
public class RdioMediaPlayer
        implements MediaPlayerInterface, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, RdioListener {

    private static String TAG = RdioMediaPlayer.class.getName();

    private static RdioMediaPlayer instance;

    private MediaPlayer.OnPreparedListener mOnPreparedListener;

    private MediaPlayer.OnCompletionListener mOnCompletionListener;

    private MediaPlayer.OnErrorListener mOnErrorListener;

    private MediaPlayer mMediaPlayer;

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    public static final String RDIO_APPKEY = "Z3FiN3oyejhoNmU3ZTc2emNicTl3ZHlw";

    public static final String RDIO_APPKEYSECRET = "YlhyRndVZUY5cQ==";

    private Rdio mRdio;

    private RdioMediaPlayer() {
        Account account = new Account("Rdio-Account",
                TomahawkApp.getContext().getString(R.string.accounttype_string));
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        String accessToken = null;
        String accessTokenSecret = null;
        if (am != null) {
            accessToken = am.getUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN);
            accessTokenSecret = am.getUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN_SECRET);
        }
        try {
            mRdio = new Rdio(new String(Base64.decode(RDIO_APPKEY, Base64.DEFAULT), "UTF-8"),
                    new String(Base64.decode(RDIO_APPKEYSECRET, Base64.DEFAULT), "UTF-8"),
                    accessToken, accessTokenSecret, TomahawkApp.getContext(), this);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "<init>: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        if (accessToken == null || accessTokenSecret == null) {
            // If either one is null, reset both of them
            storeTokenAndSecret(null, null);
        } else {
            Log.d(TAG, "Found cached credentials:");
            mRdio.prepareForPlayback();
        }
    }

    public static RdioMediaPlayer getInstance() {
        if (instance == null) {
            synchronized (RdioMediaPlayer.class) {
                if (instance == null) {
                    instance = new RdioMediaPlayer();
                }
            }
        }
        return instance;
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
    public MediaPlayerInterface prepare(Context context, Query query,
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
        if (mRdio == null) {
            return null;
        }
        mMediaPlayer = mRdio.getPlayerForTrack(
                query.getPreferredTrackResult().getPath().replace("rdio://track/", ""), null, true);
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

    public Rdio getRdio() {
        return mRdio;
    }

    @Override
    public void onRdioReadyForPlayback() {
        Log.d(TAG, "Rdio SDK is ready for playback");
    }

    @Override
    public void onRdioUserPlayingElsewhere() {
        Log.d(TAG, "Tell the user that playback is stopping.");
    }

    /*
     * Dispatched by the Rdio object once the setTokenAndSecret call has finished, and the credentials are
     * ready to be used to make API calls.  The token & token secret are passed in so that you can
     * save/cache them for future re-use.
     * @see com.rdio.android.api.RdioListener#onRdioAuthorised(java.lang.String, java.lang.String)
     */
    @Override
    public void onRdioAuthorised(String accessToken, String accessTokenSecret) {
        Log.d(TAG, "Rdio Application authorised, saving access token & secret.");

        storeTokenAndSecret(accessToken, accessTokenSecret);
    }

    private void storeTokenAndSecret(String accessToken, String accessTokenSecret) {
        Account account = new Account("Rdio-Account",
                TomahawkApp.getContext().getString(R.string.accounttype_string));
        AccountManager am = AccountManager.get(TomahawkApp.getContext());
        if (am != null) {
            am.addAccountExplicitly(account, null, new Bundle());
            am.setUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN, accessToken);
            am.setUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN_SECRET,
                    accessTokenSecret);
        }
    }
}
