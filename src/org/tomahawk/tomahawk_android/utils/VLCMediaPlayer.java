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

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class wraps a libvlc mediaplayer instance.
 */
public class VLCMediaPlayer implements MediaPlayerInterface {

    private static String TAG = VLCMediaPlayer.class.getName();

    private static VLCMediaPlayer instance;

    private MediaPlayer.OnPreparedListener mOnPreparedListener;

    private MediaPlayer.OnCompletionListener mOnCompletionListener;

    private MediaPlayer.OnErrorListener mOnErrorListener;

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    private LibVLC mLibVLC;

    private VLCMediaPlayerReceiver mVLCMediaPlayerReceiver;

    private ConcurrentHashMap<Result, String> mTranslatedUrls
            = new ConcurrentHashMap<Result, String>();

    /**
     * Handles incoming broadcasts.
     */
    private class VLCMediaPlayerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (PipeLine.PIPELINE_URLTRANSLATIONREPORTED.equals(intent.getAction())) {
                String resultKey = intent
                        .getStringExtra(PipeLine.PIPELINE_URLTRANSLATIONREPORTED_RESULTKEY);
                String url = intent.getStringExtra(PipeLine.PIPELINE_URLTRANSLATIONREPORTED_URL);
                final Result result = Result.getResultByKey(resultKey);
                mTranslatedUrls.put(Result.getResultByKey(resultKey), url);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mTranslatedUrls.remove(result);
                    }
                }, 300000);
                if (mPreparingQuery != null
                        && result == mPreparingQuery.getPreferredTrackResult()) {
                    prepare(mPreparingQuery);
                }
            }
        }
    }

    private VLCMediaPlayer() {
        // Initialize and register Receiver
        if (mVLCMediaPlayerReceiver == null) {
            mVLCMediaPlayerReceiver = new VLCMediaPlayerReceiver();
            IntentFilter intentFilter = new IntentFilter(PipeLine.PIPELINE_URLTRANSLATIONREPORTED);
            TomahawkApp.getContext().registerReceiver(mVLCMediaPlayerReceiver, intentFilter);
        }

        try {
            mLibVLC = LibVLC.getInstance();
            mLibVLC.init(TomahawkApp.getContext());
            mLibVLC.setHardwareAcceleration(LibVLC.HW_ACCELERATION_DISABLED);
        } catch (LibVlcException e) {
            Log.e(TAG, "<init>: Failed to initialize LibVLC: " + e.getLocalizedMessage());
        }
    }

    public static VLCMediaPlayer getInstance() {
        if (instance == null) {
            synchronized (VLCMediaPlayer.class) {
                if (instance == null) {
                    instance = new VLCMediaPlayer();
                }
            }
        }
        return instance;
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        if (mLibVLC != null) {
            mLibVLC.setVolume((int) (leftVolume + rightVolume * 50));
        }
    }

    /**
     * Start playing the previously prepared {@link org.tomahawk.libtomahawk.collection.Track}
     */
    @Override
    public void start() throws IllegalStateException {
        Log.d(TAG, "start()");
        if (mLibVLC != null) {
            if (!mLibVLC.isPlaying()) {
                mLibVLC.play();
            }
        }
    }

    /**
     * Pause playing the current {@link org.tomahawk.libtomahawk.collection.Track}
     */
    @Override
    public void pause() throws IllegalStateException {
        Log.d(TAG, "pause()");
        if (mLibVLC != null) {
            if (mLibVLC.isPlaying()) {
                mLibVLC.pause();
            }
        }
    }

    /**
     * Seek to the given playback position (in ms)
     */
    @Override
    public void seekTo(int msec) throws IllegalStateException {
        Log.d(TAG, "seekTo()");
        if (mLibVLC != null && mPreparedQuery != null
                && mPreparedQuery.getPreferredTrackResult().getResolvedBy().getId()
                != PipeLine.RESOLVER_ID_BEATSMUSIC) {
            mLibVLC.setTime(msec);
        }
    }

    /**
     * Prepare the given url
     */
    private MediaPlayerInterface prepare(Query query) {
        mPreparedQuery = null;
        mPreparingQuery = query;
        release();
        if (mLibVLC == null) {
            return null;
        }
        Result result = query.getPreferredTrackResult();
        if (result.getResolvedBy().getId() == PipeLine.RESOLVER_ID_BEATSMUSIC) {
            if (mTranslatedUrls.get(result) == null) {
                ((ScriptResolver) result.getResolvedBy()).getStreamUrl(result);
            } else {
                mLibVLC.playMRL(LibVLC.PathToURI(mTranslatedUrls.get(result)));
                onPrepared(null);
            }
        } else {
            mLibVLC.playMRL(LibVLC.PathToURI(result.getPath()));
            onPrepared(null);
        }
        return this;
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
        return prepare(query);
    }

    @Override
    public void release() {
        Log.d(TAG, "release()");
        TomahawkApp.getContext()
                .sendBroadcast(new Intent(PlaybackService.BROADCAST_VLCMEDIAPLAYER_RELEASED));
        pause();
    }

    /**
     * @return the current track position
     */
    @Override
    public int getPosition() {
        if (mLibVLC != null && mPreparedQuery != null) {
            return (int) mLibVLC.getTime();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isPlaying(Query query) {
        return mLibVLC != null && isPrepared(query) && mLibVLC.isPlaying();
    }

    @Override
    public boolean isPreparing(Query query) {
        return mPreparingQuery != null && mPreparingQuery == query;
    }

    @Override
    public boolean isPrepared(Query query) {
        return mPreparedQuery != null && mPreparedQuery == query;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared()");
        mPreparedQuery = mPreparingQuery;
        mPreparingQuery = null;
        mOnPreparedListener.onPrepared(mp);
        TomahawkApp.getContext()
                .sendBroadcast(new Intent(PlaybackService.BROADCAST_VLCMEDIAPLAYER_PREPARED));
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
