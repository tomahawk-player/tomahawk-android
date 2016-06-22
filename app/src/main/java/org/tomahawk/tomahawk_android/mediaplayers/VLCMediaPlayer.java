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

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.PreferenceUtils;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.AndroidUtil;

import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;

/**
 * This class wraps a libvlc mediaplayer instance.
 */
public class VLCMediaPlayer implements TomahawkMediaPlayer {

    private static final String TAG = VLCMediaPlayer.class.getSimpleName();

    private static MediaPlayer sMediaPlayer;

    private static LibVLC sLibVLC;

    static {
        ArrayList<String> options = new ArrayList<>();
        options.add("--http-reconnect");
        options.add("--network-caching=2000");
        sLibVLC = new LibVLC(options);
        sMediaPlayer = new MediaPlayer(sLibVLC);
    }

    private TomahawkMediaPlayerCallback mMediaPlayerCallback;

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    private final ConcurrentHashMap<Result, String> mTranslatedUrls
            = new ConcurrentHashMap<>();

    private class MediaPlayerListener implements MediaPlayer.EventListener {

        @Override
        public void onEvent(MediaPlayer.Event event) {
            switch (event.type) {
                case MediaPlayer.Event.EncounteredError:
                    Log.d(TAG, "onError()");
                    mPreparedQuery = null;
                    mPreparingQuery = null;
                    if (mMediaPlayerCallback != null) {
                        mMediaPlayerCallback.onError(
                                VLCMediaPlayer.this, "MediaPlayerEncounteredError");
                    } else {
                        Log.e(TAG, "Wasn't able to call onError because callback object is null");
                    }
                    break;
                case MediaPlayer.Event.EndReached:
                    Log.d(TAG, "onCompletion()");
                    if (mMediaPlayerCallback != null) {
                        mMediaPlayerCallback.onCompletion(VLCMediaPlayer.this, mPreparedQuery);
                    } else {
                        Log.e(TAG,
                                "Wasn't able to call onCompletion because callback object is null");
                    }
                    break;
            }
        }
    }

    public VLCMediaPlayer() {
        sMediaPlayer = new MediaPlayer(sLibVLC);
        if (PreferenceUtils.getBoolean(PreferenceUtils.EQUALIZER_ENABLED)) {
            MediaPlayer.Equalizer equalizer = MediaPlayer.Equalizer.create();
            float[] bands = PreferenceUtils.getFloatArray(PreferenceUtils.EQUALIZER_VALUES);
            equalizer.setPreAmp(bands[0]);
            for (int i = 0; i < MediaPlayer.Equalizer.getBandCount(); i++) {
                equalizer.setAmp(i, bands[i + 1]);
            }
            sMediaPlayer.setEqualizer(equalizer);
        }
        sMediaPlayer.setEventListener(new MediaPlayerListener());
        EventBus.getDefault().register(this);
    }

    public static LibVLC getLibVlcInstance() {
        return sLibVLC;
    }

    public static MediaPlayer getMediaPlayerInstance() {
        return sMediaPlayer;
    }

    @SuppressWarnings("unused")
    public void onEventAsync(PipeLine.StreamUrlEvent event) {
        Log.d(TAG, "Received stream url: " + event.mResult + ", " + event.mUrl);
        mTranslatedUrls.put(event.mResult, event.mUrl);
        if (mMediaPlayerCallback != null && mPreparingQuery != null
                && event.mResult == mPreparingQuery.getPreferredTrackResult()) {
            prepare(mPreparingQuery);
        } else {
            Log.e(TAG, "Received stream url: Wasn't able to prepare: " + event.mResult + ", "
                    + event.mUrl);
        }
    }

    /**
     * Start playing the previously prepared {@link org.tomahawk.libtomahawk.collection.Track}
     */
    @Override
    public void play() throws IllegalStateException {
        Log.d(TAG, "play()");
        if (!getMediaPlayerInstance().isPlaying()) {
            getMediaPlayerInstance().play();
        }
    }

    @Override
    public void tryPrepareNext(Query query) {

    }
    /**
     * Pause playing the current {@link org.tomahawk.libtomahawk.collection.Track}
     */
    @Override
    public void pause() throws IllegalStateException {
        Log.d(TAG, "pause()");
        if (getMediaPlayerInstance().isPlaying()) {
            getMediaPlayerInstance().pause();
        }
    }

    /**
     * Seek to the given playback position (in ms)
     */
    @Override
    public void seekTo(long msec) throws IllegalStateException {
        Log.d(TAG, "seekTo()");
        if (mPreparedQuery != null && !TomahawkApp.PLUGINNAME_BEATSMUSIC.equals(
                mPreparedQuery.getPreferredTrackResult().getResolvedBy().getId())) {
            getMediaPlayerInstance().setTime(msec);
        }
    }

    /**
     * Prepare the given url
     */
    private void prepare(final Query query) {
        Log.d(TAG, "prepare()");
        TomahawkRunnable r = new TomahawkRunnable(1) {
            @Override
            public void run() {
                getMediaPlayerInstance().stop();
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
                Media media = new Media(sLibVLC, AndroidUtil.LocationToUri(path));
                getMediaPlayerInstance().setMedia(media);
                mPreparedQuery = mPreparingQuery;
                mPreparingQuery = null;
                mMediaPlayerCallback.onPrepared(VLCMediaPlayer.this, mPreparedQuery);
                Log.d(TAG, "onPrepared()");
            }
        };
        ThreadManager.get().executePlayback(r);
    }

    /**
     * Prepare the given url
     */
    @Override
    public void prepare(Query query, TomahawkMediaPlayerCallback callback) {
        mMediaPlayerCallback = callback;
        prepare(query);
    }

    @Override
    public void release() {
        Log.d(TAG, "release()");
        mPreparedQuery = null;
        mPreparingQuery = null;
        getMediaPlayerInstance().stop();
        mMediaPlayerCallback = null;
    }

    /**
     * @return the current track position
     */
    @Override
    public long getPosition() {
        if (mPreparedQuery != null) {
            return getMediaPlayerInstance().getTime();
        } else {
            return 0L;
        }
    }

    @Override
    public void setBitrate(int bitrateMode) {
    }

    @Override
    public boolean isPlaying(Query query) {
        return isPrepared(query) && getMediaPlayerInstance().isPlaying();
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
