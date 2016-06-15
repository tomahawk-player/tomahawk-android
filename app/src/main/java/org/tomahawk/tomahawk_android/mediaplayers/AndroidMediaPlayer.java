package org.tomahawk.tomahawk_android.mediaplayers;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.libtomahawk.utils.NetworkUtils;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;


/**
 * Created by Anton Romanov on 6/7/16.
 */
public class AndroidMediaPlayer implements TomahawkMediaPlayer {

    private static final String TAG = AndroidMediaPlayer.class.getSimpleName();

    private static HashMap<Query, MediaPlayer> mMediaPlayers = new HashMap<>();

    private static HashSet<MediaPlayer> mBufferedMediaPlayers = new HashSet<>();

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    private final ConcurrentHashMap<Result, String> mTranslatedUrls
            = new ConcurrentHashMap<>();

    private TomahawkMediaPlayerCallback mMediaPlayerCallback;

    public AndroidMediaPlayer() {
        EventBus.getDefault().register(this);
    }

    @Override
    public void play() {
        MediaPlayer sMediaPlayer = mMediaPlayers.get(mPreparedQuery);
        if (sMediaPlayer != null && !sMediaPlayer.isPlaying())
            sMediaPlayer.start();
    }

    @Override
    public void pause() {
        MediaPlayer sMediaPlayer = mMediaPlayers.get(mPreparedQuery);
        if (sMediaPlayer != null)
            sMediaPlayer.pause();
    }

    @Override
    public void seekTo(long msec) {
        MediaPlayer sMediaPlayer = mMediaPlayers.get(mPreparedQuery);
        if (sMediaPlayer != null)
            sMediaPlayer.seekTo((int)msec);
    }

    @Override
    public void tryPrepareNext(final Query query) {
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

        MediaPlayer sMediaPlayer = new MediaPlayer();
        mMediaPlayers.put(query, sMediaPlayer);

        sMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            String finalUrl = NetworkUtils.getFinalURL(path);
            if (finalUrl != null)
                path = finalUrl;
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        MediaPlayer currentMediaPlayer = mMediaPlayers.get(mPreparedQuery);
        if (currentMediaPlayer != null)
            currentMediaPlayer.setNextMediaPlayer(sMediaPlayer);
        mPreparingQuery = null;
        Log.d(TAG, "Prepared next track for playback");
    }

    @Override
    public void prepare(final Query query, final TomahawkMediaPlayerCallback callback) {
        Log.d(TAG, "prepare()");
        mMediaPlayerCallback = callback;
        // Since this one is supposedly the one we want to play lets do some cleanup work
        final Iterator mapIter = mMediaPlayers.keySet().iterator();
        while(mapIter.hasNext()) {
            Query q = (Query)mapIter.next();
            if (!q.equals(query)) {
                MediaPlayer mp = mMediaPlayers.get(q);
                if (mp != null)
                    mp.release();
                mapIter.remove();
            }
        }
        if (mMediaPlayers.containsKey(query)) {
            MediaPlayer mediaPlayer = mMediaPlayers.get(query);
            if (mediaPlayer == null) {
                mMediaPlayers.remove(query);
                callback.onError(AndroidMediaPlayer.this, "MediaPlayerEncounteredError");
            } else {
                mPreparingQuery = null;
                mPreparedQuery = query;
                callback.onPrepared(AndroidMediaPlayer.this, mPreparedQuery);
                if (mBufferedMediaPlayers.contains(mediaPlayer))
                    mMediaPlayerCallback.onBufferingComplete(AndroidMediaPlayer.this);
                Log.d(TAG, "onPrepared()");
            }
            return;
        }
        mBufferedMediaPlayers.clear();
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

                MediaPlayer sMediaPlayer = new MediaPlayer();
                mMediaPlayers.put(query, sMediaPlayer);

                sMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                try {
                    String finalUrl = NetworkUtils.getFinalURL(path);
                    if (finalUrl != null)
                        path = finalUrl;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    sMediaPlayer.setDataSource(path);
                    sMediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                    callback.onError(
                            AndroidMediaPlayer.this, "MediaPlayerEncounteredError");
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
        MediaPlayer sMediaPlayer = mMediaPlayers.remove(mPreparedQuery);
        if (sMediaPlayer != null) {
            sMediaPlayer.release();
            mBufferedMediaPlayers.remove(sMediaPlayer);
        }
        mPreparedQuery = null;
    }

    @Override
    public long getPosition() {
        MediaPlayer sMediaPlayer = mMediaPlayers.get(mPreparedQuery);
        if (sMediaPlayer != null)
            return sMediaPlayer.getCurrentPosition();
        return 0;
    }

    @Override
    public void setBitrate(int mode) {

    }

    @Override
    public boolean isPlaying(Query query) {
        MediaPlayer sMediaPlayer = mMediaPlayers.get(mPreparedQuery);
        if (sMediaPlayer != null)
            return sMediaPlayer.isPlaying();
        return false;
    }

    @Override
    public boolean isPreparing(Query query) {
        return mPreparingQuery != null && mPreparingQuery == query;
    }

    @Override
    public boolean isPrepared(Query query) {
        return mPreparedQuery != null && mPreparedQuery == query;
    }

    private class CompletionListener implements MediaPlayer.OnCompletionListener {
        public void onCompletion(MediaPlayer mp) {
            Log.d(TAG, "onCompletion()");
            if (mMediaPlayerCallback != null) {
                mMediaPlayerCallback.onCompletion(AndroidMediaPlayer.this, mPreparedQuery);
                mp.release();
                mPreparedQuery = null;
            } else {
                Log.e(TAG,
                        "Wasn't able to call onCompletion because callback object is null");
            }
        }
    }

    private class BufferingUpdateListener implements MediaPlayer.OnBufferingUpdateListener {
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (percent == 100) {
                if (mMediaPlayerCallback != null && mp == mMediaPlayers.get(mPreparedQuery)) {
                    mMediaPlayerCallback.onBufferingComplete(AndroidMediaPlayer.this);
                }
                mBufferedMediaPlayers.add(mp);
                mp.setOnBufferingUpdateListener(null);
            }
        }
    }
}
