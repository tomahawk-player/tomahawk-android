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
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;

import static android.support.v4.media.session.PlaybackStateCompat.*;

public class AndroidMediaPlayer implements TomahawkMediaPlayer {

    private class MediaPlayerWrap extends MediaPlayer {
        private int mPlayBackState = STATE_NONE;
        private int mBuffered = 0;
        private OnBufferingUpdateListener mBufferingListener = null;


        private class BufferingUpdateListener implements MediaPlayer.OnBufferingUpdateListener {
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                mBuffered = percent;
                if (mBufferingListener != null)
                    mBufferingListener.onBufferingUpdate(mp, percent);
            }
        }

        public void setOnBufferingUpdateListener(MediaPlayer.OnBufferingUpdateListener listener) {
            mBufferingListener = listener;
        }

        public void prepare() throws IOException {
            super.setOnBufferingUpdateListener(new BufferingUpdateListener());
            super.prepare();
            mPlayBackState = STATE_BUFFERING;
        }

        public void start(){
            if(mPlayBackState != STATE_NONE) {
                super.start();
                mPlayBackState = STATE_PLAYING;
            }
        }

        public void pause(){
            if(mPlayBackState != STATE_NONE &&
                    super.isPlaying()) {
                super.pause();
                mPlayBackState = STATE_PAUSED;
            }
        }

        public int getPlayBackState () {
            return mPlayBackState;
        }

        public int getBuffered () {
            return mBuffered;
        }
    }

    private static final String TAG = AndroidMediaPlayer.class.getSimpleName();

    private static HashMap<Query, MediaPlayerWrap> mMediaPlayers = new HashMap<>();

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    private final ConcurrentHashMap<Result, String> mTranslatedUrls = new ConcurrentHashMap<>();

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
        MediaPlayerWrap sMediaPlayer = mMediaPlayers.get(mPreparedQuery);
        if (sMediaPlayer != null && !sMediaPlayer.isPlaying())
            sMediaPlayer.start();
    }

    @Override
    public void pause() {
        MediaPlayerWrap sMediaPlayer = mMediaPlayers.get(mPreparedQuery);
        if (sMediaPlayer != null)
            sMediaPlayer.pause();
    }

    @Override
    public void seekTo(long msec) {
        MediaPlayerWrap sMediaPlayer = mMediaPlayers.get(mPreparedQuery);
        if (sMediaPlayer != null)
            sMediaPlayer.seekTo((int)msec);
    }

    @Override
    public void tryPrepareNext(final Query query) {

        TomahawkRunnable r = new TomahawkRunnable(1) {
            @Override
            public void run() {
                Log.d(TAG, "tryPrepareNext()");
                if (mPreparingQuery != null && mMediaPlayers.containsKey(mPreparingQuery))
                    mMediaPlayers.remove(mPreparingQuery);
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

                MediaPlayerWrap sMediaPlayer = new MediaPlayerWrap();
                mMediaPlayers.put(query, sMediaPlayer);

                sMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                try {
                    sMediaPlayer.setDataSource(path);
                    sMediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                    mMediaPlayers.put(mPreparingQuery, null);
                    return;
                }

                sMediaPlayer.setOnCompletionListener(new CompletionListener());
                sMediaPlayer.setOnBufferingUpdateListener(new BufferingUpdateListener());

                MediaPlayerWrap currentMediaPlayer = mMediaPlayers.get(mPreparedQuery);
                if (currentMediaPlayer != null && currentMediaPlayer != sMediaPlayer)
                    currentMediaPlayer.setNextMediaPlayer(sMediaPlayer);
                mPreparingQuery = null;
                Log.d(TAG, "Prepared next track for playback");
            }

            ;
        };
        ThreadManager.get().executePlayback(r);
    }

    @Override
    public void prepare(final Query query, final TomahawkMediaPlayerCallback callback) {
        TomahawkRunnable r = new TomahawkRunnable(1) {
            @Override
            public void run() {
                Log.d(TAG, "prepare()");
                mMediaPlayerCallback = callback;
                // Since this one is supposedly the one we want to play lets do some cleanup work
                final Iterator mapIter = mMediaPlayers.keySet().iterator();
                while(mapIter.hasNext()) {
                    Query q = (Query)mapIter.next();
                    if (!q.equals(query)) {
                        MediaPlayerWrap mp = mMediaPlayers.get(q);
                        if (mp != null) {
                            mp.release();
                        }
                        mapIter.remove();
                    }
                }
                if (mMediaPlayers.containsKey(query)) {
                    MediaPlayerWrap mediaPlayer = mMediaPlayers.get(query);
                    if (mediaPlayer == null) {
                        mMediaPlayers.remove(query);
                        callback.onError(AndroidMediaPlayer.this, "MediaPlayerEncounteredError");
                    } else {
                        mPreparingQuery = null;
                        mPreparedQuery = query;
                        callback.onPrepared(AndroidMediaPlayer.this, mPreparedQuery);
                        if (mediaPlayer.getBuffered() == 100)
                            mMediaPlayerCallback.onBufferingComplete(AndroidMediaPlayer.this);
                        Log.d(TAG, "onPrepared() for pre-prepared track");
                    }
                    return;
                }
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

                MediaPlayerWrap sMediaPlayer = new MediaPlayerWrap();
                mMediaPlayers.put(query, sMediaPlayer);

                sMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                try {
                    sMediaPlayer.setDataSource(path);
                    sMediaPlayer.prepare();
                } catch (IOException | IllegalStateException e) {
                    Log.e(TAG, "prepare - ", e);
                    callback.onError(AndroidMediaPlayer.this, "MediaPlayerEncounteredError");
                }

                sMediaPlayer.setOnCompletionListener(new CompletionListener());
                sMediaPlayer.setOnBufferingUpdateListener(new BufferingUpdateListener());

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
            if (mPreparedQuery == null)
                prepare(mPreparingQuery, mMediaPlayerCallback);
            else
                tryPrepareNext(mPreparingQuery);
        } else {
            Log.e(TAG, "Received stream url: Wasn't able to prepare: " + event.mResult + ", "
                    + event.mUrl);
        }
    }

    @Override
    public void release() {
        //We do our own management/release
    }

    @Override
    public long getPosition() {
        MediaPlayerWrap sMediaPlayer = mMediaPlayers.get(mPreparedQuery);
        if (sMediaPlayer != null)
        {
            return sMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void setBitrate(int mode) {
    }

    @Override
    public boolean isPlaying(Query query) {
        MediaPlayerWrap sMediaPlayer = mMediaPlayers.get(query);
        if (sMediaPlayer != null)
            return sMediaPlayer.isPlaying();
        return false;
    }

    @Override
    public boolean isPreparing(Query query) {
        return query == mPreparingQuery;
    }

    @Override
    public boolean isPrepared(Query query) {
        MediaPlayerWrap sMediaPlayer = mMediaPlayers.get(query);
        if (sMediaPlayer != null)
            return sMediaPlayer.getPlayBackState() != STATE_NONE && query == mPreparedQuery;
        return false;
    }


    private class BufferingUpdateListener implements MediaPlayer.OnBufferingUpdateListener {
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (percent == 100) {
                if (mMediaPlayerCallback != null && mp == mMediaPlayers.get(mPreparedQuery)) {
                    mMediaPlayerCallback.onBufferingComplete(AndroidMediaPlayer.this);
                }
                mp.setOnBufferingUpdateListener(null);
            }
        }
    }
}
