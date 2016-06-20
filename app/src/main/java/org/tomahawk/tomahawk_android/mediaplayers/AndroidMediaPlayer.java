/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
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

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;

public class AndroidMediaPlayer implements TomahawkMediaPlayer {

    private static final String TAG = AndroidMediaPlayer.class.getSimpleName();

    private static MediaPlayer sMediaPlayer = null;

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    private final ConcurrentHashMap<Result, String> mTranslatedUrls
            = new ConcurrentHashMap<>();

    private TomahawkMediaPlayerCallback mMediaPlayerCallback;

    private class CompletionListener implements MediaPlayer.OnCompletionListener {

        public void onCompletion(MediaPlayer mp) {
            Log.d(TAG, "onCompletion()");
            if (mMediaPlayerCallback != null) {
                mMediaPlayerCallback.onCompletion(AndroidMediaPlayer.this, mPreparedQuery);
            } else {
                Log.e(TAG, "Wasn't able to call onCompletion because callback object is null");
            }
        }
    }

    public AndroidMediaPlayer() {
        EventBus.getDefault().register(this);
    }

    @Override
    public void play() {
        if (sMediaPlayer != null) {
            sMediaPlayer.start();
        }
    }

    @Override
    public void pause() {
        if (sMediaPlayer != null) {
            sMediaPlayer.pause();
        }
    }

    @Override
    public void seekTo(long msec) {
        if (sMediaPlayer != null) {
            sMediaPlayer.seekTo((int) msec);
        }
    }

    @Override
    public void prepare(final Query query, final TomahawkMediaPlayerCallback callback) {
        Log.d(TAG, "prepare()");
        mMediaPlayerCallback = callback;
        TomahawkRunnable r = new TomahawkRunnable(1) {
            @Override
            public void run() {
                mPreparedQuery = null;
                mPreparingQuery = query;
                Result result = query.getPreferredTrackResult();
                String path;
                if (mTranslatedUrls.get(result) != null) {
                    path = mTranslatedUrls.remove(result);
                } else {
                    if (result.getResolvedBy() instanceof ScriptResolver) {
                        ((ScriptResolver) result.getResolvedBy()).getStreamUrl(result);
                        return;
                    } else {
                        path = result.getPath();
                    }
                }
                release();
                sMediaPlayer = new MediaPlayer();

                sMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                try {
                    sMediaPlayer.setDataSource(path);
                    sMediaPlayer.prepare();
                } catch (IOException e) {
                    Log.e(TAG, "prepare - ", e);
                    callback.onError(AndroidMediaPlayer.this, "MediaPlayerEncounteredError");
                }

                sMediaPlayer.setOnCompletionListener(new CompletionListener());

                mPreparedQuery = mPreparingQuery;
                mPreparingQuery = null;
                callback.onPrepared(AndroidMediaPlayer.this, mPreparedQuery);
                Log.d(TAG, "onPrepared()");
            }
        };
        ThreadManager.get().executePlayback(r);
    }

    @SuppressWarnings("unused")
    public void onEventAsync(PipeLine.StreamUrlEvent event) {
        Log.d(TAG, "Received stream url: " + event.mResult + ", " + event.mUrl);
        mTranslatedUrls.put(event.mResult, event.mUrl);
        if (mMediaPlayerCallback != null && mPreparingQuery != null
                && event.mResult == mPreparingQuery.getPreferredTrackResult()) {
            prepare(mPreparingQuery, mMediaPlayerCallback);
        } else {
            Log.e(TAG, "Received stream url: Wasn't able to prepare: " + event.mResult + ", "
                    + event.mUrl);
        }
    }

    @Override
    public void release() {
        if (sMediaPlayer != null) {
            sMediaPlayer.release();
        }
    }

    @Override
    public long getPosition() {
        if (sMediaPlayer != null) {
            return sMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void setBitrate(int mode) {
    }

    @Override
    public boolean isPlaying(Query query) {
        return sMediaPlayer != null && sMediaPlayer.isPlaying();
    }

    @Override
    public boolean isPreparing(Query query) {
        return mPreparingQuery != null && mPreparingQuery == query;
    }

    @Override
    public boolean isPrepared(Query query) {
        return mPreparedQuery != null && mPreparedQuery == query;
    }
}
