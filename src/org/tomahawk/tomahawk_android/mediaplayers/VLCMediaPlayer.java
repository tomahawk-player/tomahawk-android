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
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.EqualizerFragment;
import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.LibVlcUtil;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;

/**
 * This class wraps a libvlc mediaplayer instance.
 */
public class VLCMediaPlayer implements TomahawkMediaPlayer {

    private static final String TAG = VLCMediaPlayer.class.getSimpleName();

    private static class Holder {

        private static final VLCMediaPlayer instance = new VLCMediaPlayer();

    }

    private LibVLC mLibVLC;

    private TomahawkMediaPlayerCallback mMediaPlayerCallback;

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    private final ConcurrentHashMap<Result, String> mTranslatedUrls
            = new ConcurrentHashMap<>();

    private final Handler mVlcHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Bundle data = msg.getData();
            if (data != null) {
                switch (data.getInt("event")) {
                    case EventHandler.MediaPlayerEncounteredError:
                        Log.d(TAG, "onError()");
                        mPreparedQuery = null;
                        mPreparingQuery = null;
                        mMediaPlayerCallback.onError("MediaPlayerEncounteredError");
                        break;
                    case EventHandler.MediaPlayerEndReached:
                        Log.d(TAG, "onCompletion()");
                        mMediaPlayerCallback.onCompletion(mPreparedQuery);
                        break;
                    default:
                        return false;
                }
                return true;
            }
            return false;
        }
    });

    private VLCMediaPlayer() {
        mLibVLC = new LibVLC();
        mLibVLC.setHttpReconnect(true);
        mLibVLC.setNetworkCaching(2000);
        SharedPreferences pref =
                PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext());
        if (pref.getBoolean(EqualizerFragment.EQUALIZER_ENABLED_PREFERENCE_KEY, false)) {
            mLibVLC.setEqualizer(TomahawkUtils.getFloatArray(pref,
                    EqualizerFragment.EQUALIZER_VALUES_PREFERENCE_KEY));
        }
        try {
            mLibVLC.init(TomahawkApp.getContext());
        } catch (LibVlcException e) {
            throw new IllegalStateException("LibVLC initialisation failed: "
                    + LibVlcUtil.getErrorMsg());
        }
        EventBus.getDefault().register(this);
    }

    public LibVLC getLibVlcInstance() {
        return mLibVLC;
    }

    public static VLCMediaPlayer getInstance() {
        return Holder.instance;
    }

    @SuppressWarnings("unused")
    public void onEventAsync(PipeLine.StreamUrlEvent event) {
        mTranslatedUrls.put(event.mResult, event.mUrl);
        if (mPreparingQuery != null
                && event.mResult == mPreparingQuery.getPreferredTrackResult()) {
            prepare(mPreparingQuery);
        }
    }

    /**
     * Start playing the previously prepared {@link org.tomahawk.libtomahawk.collection.Track}
     */
    @Override
    public void start() throws IllegalStateException {
        Log.d(TAG, "start()");
        if (!getLibVlcInstance().isPlaying()) {
            getLibVlcInstance().play();
        }
    }

    /**
     * Pause playing the current {@link org.tomahawk.libtomahawk.collection.Track}
     */
    @Override
    public void pause() throws IllegalStateException {
        Log.d(TAG, "pause()");
        if (getLibVlcInstance().isPlaying()) {
            getLibVlcInstance().pause();
        }
    }

    /**
     * Seek to the given playback position (in ms)
     */
    @Override
    public void seekTo(int msec) throws IllegalStateException {
        Log.d(TAG, "seekTo()");
        if (mPreparedQuery != null && !TomahawkApp.PLUGINNAME_BEATSMUSIC.equals(
                mPreparedQuery.getPreferredTrackResult().getResolvedBy().getId())) {
            getLibVlcInstance().setTime(msec);
        }
    }

    /**
     * Prepare the given url
     */
    private TomahawkMediaPlayer prepare(Query query) {
        mPreparedQuery = null;
        mPreparingQuery = query;
        release();
        Result result = query.getPreferredTrackResult();
        String path;
        if (mTranslatedUrls.get(result) != null) {
            path = mTranslatedUrls.remove(result);
        } else {
            if (result.getResolvedBy() instanceof ScriptResolver) {
                ((ScriptResolver) result.getResolvedBy()).getStreamUrl(result);
                return this;
            } else {
                path = result.getPath();
            }
        }
        getLibVlcInstance().playMRL(LibVLC.PathToURI(path));
        Log.d(TAG, "onPrepared()");
        mPreparedQuery = mPreparingQuery;
        mPreparingQuery = null;
        mMediaPlayerCallback.onPrepared(mPreparedQuery);
        EventHandler.getInstance().addHandler(mVlcHandler);
        return this;
    }

    /**
     * Prepare the given url
     */
    @Override
    public TomahawkMediaPlayer prepare(Application application, Query query,
            TomahawkMediaPlayerCallback callback) {
        Log.d(TAG, "prepare()");
        mMediaPlayerCallback = callback;
        return prepare(query);
    }

    @Override
    public void release() {
        Log.d(TAG, "release()");
        EventHandler.getInstance().removeHandler(mVlcHandler);
        pause();
    }

    /**
     * @return the current track position
     */
    @Override
    public int getPosition() {
        if (mPreparedQuery != null) {
            return (int) getLibVlcInstance().getTime();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isPlaying(Query query) {
        return isPrepared(query) && getLibVlcInstance().isPlaying();
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
