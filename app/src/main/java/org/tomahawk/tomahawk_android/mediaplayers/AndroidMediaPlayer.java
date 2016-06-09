package org.tomahawk.tomahawk_android.mediaplayers;

import android.media.MediaPlayer;
import android.util.Log;

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
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

    private static MediaPlayer sMediaPlayer = null;

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
        if (sMediaPlayer != null)
            sMediaPlayer.start();
    }

    @Override
    public void pause() {
        if (sMediaPlayer != null)
            sMediaPlayer.pause();
    }

    @Override
    public void seekTo(long msec) {
        if (sMediaPlayer != null)
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
                sMediaPlayer = new MediaPlayer();

                try {
                    sMediaPlayer.setDataSource(path);
                    sMediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                    callback.onError(
                            AndroidMediaPlayer.this, "MediaPlayerEncounteredError");
                }

                // Media media = new Media(sLibVLC, AndroidUtil.LocationToUri(path));
               // getMediaPlayerInstance().setMedia(media);
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
        if (sMediaPlayer != null)
            sMediaPlayer.release();
        sMediaPlayer = null;
    }

    @Override
    public long getPosition() {
        if (sMediaPlayer != null)
            return sMediaPlayer.getCurrentPosition();
        else
            return 0;
    }

    @Override
    public void setBitrate(int mode) {

    }

    @Override
    public boolean isPlaying(Query query) {
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
}
