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
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;


/**
 * Created by Anton Romanov on 6/7/16.
 */
public class AndroidMediaPlayer implements TomahawkMediaPlayer {

    private static final String TAG = AndroidMediaPlayer.class.getSimpleName();

    private static MediaPlayer sMediaPlayer = new MediaPlayer();

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
            sMediaPlayer.start();
    }

    @Override
    public void pause() {
            sMediaPlayer.pause();
    }

    @Override
    public void seekTo(long msec) {
            sMediaPlayer.seekTo((int)msec);
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
                sMediaPlayer.reset();

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
        sMediaPlayer.release();
    }

    @Override
    public long getPosition() {
        return sMediaPlayer.getCurrentPosition();
    }

    @Override
    public void setBitrate(int mode) {

    }

    @Override
    public boolean isPlaying(Query query) {
        return sMediaPlayer.isPlaying();
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
            } else {
                Log.e(TAG,
                        "Wasn't able to call onCompletion because callback object is null");
            }
        }
    }
}
