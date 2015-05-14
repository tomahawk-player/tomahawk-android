/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.tomahawk.aidl.IPluginService;
import org.tomahawk.aidl.IPluginServiceCallback;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.ScriptJob;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverAccessTokenResult;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.MediaPlayerInterface;
import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import de.greenrobot.event.EventBus;

/**
 * This class wraps all functionality to be able to directly playback spotify-resolved tracks with
 * OpenSLES .
 */
public class SpotifyMediaPlayer implements MediaPlayerInterface {

    private static final String TAG = SpotifyMediaPlayer.class.getSimpleName();

    // String tags used to store Spotify's preferred bitrate
    private static final String SPOTIFY_PREF_BITRATE
            = "org.tomahawk.tomahawk_android.spotify_pref_bitrate";

    public static final int SPOTIFY_PREF_BITRATE_MODE_LOW = 0;

    public static final int SPOTIFY_PREF_BITRATE_MODE_MEDIUM = 1;

    public static final int SPOTIFY_PREF_BITRATE_MODE_HIGH = 2;

    private static class Holder {

        private static final SpotifyMediaPlayer instance = new SpotifyMediaPlayer();

    }

    private MediaPlayer.OnPreparedListener mOnPreparedListener;

    private MediaPlayer.OnCompletionListener mOnCompletionListener;

    private boolean mIsPlaying;

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    private boolean mOverrideCurrentPosition = false;

    private long mPositionTimeStamp;

    private int mPositionOffset;

    private SpotifyMediaPlayer() {
    }

    public static SpotifyMediaPlayer getInstance() {
        return Holder.instance;
    }

    public void setService(IPluginService service) {
        mService = service;
    }

    private IPluginService mService;

    public IPluginServiceCallback getServiceCallback() {
        return mServiceCallback;
    }

    private IPluginServiceCallback mServiceCallback = new IPluginServiceCallback.Stub() {

        @Override
        public void onPause() throws RemoteException {
            mPositionOffset =
                    (int) (System.currentTimeMillis() - mPositionTimeStamp) + mPositionOffset;
            mPositionTimeStamp = System.currentTimeMillis();
        }

        @Override
        public void onPlay() throws RemoteException {
            mPositionTimeStamp = System.currentTimeMillis();
        }

        @Override
        public void onPrepared() throws RemoteException {
            SpotifyMediaPlayer.this.onPrepared(null);
        }

        @Override
        public void onPlayerEndOfTrack() throws RemoteException {
            onCompletion(null);
        }

        @Override
        public void onPlayerPositionChanged(int position, long timeStamp) throws RemoteException {
            if (!mOverrideCurrentPosition) {
                mPositionTimeStamp = timeStamp;
                mPositionOffset = position;
            }
        }
    };

    private final ResetOverrideHandler mResetOverrideHandler = new ResetOverrideHandler(this);

    private static class ResetOverrideHandler extends WeakReferenceHandler<SpotifyMediaPlayer> {

        public ResetOverrideHandler(SpotifyMediaPlayer referencedObject) {
            super(referencedObject);
        }

        @Override
        public void handleMessage(Message msg) {
            if (getReferencedObject() != null) {
                getReferencedObject().mOverrideCurrentPosition = false;
            }
        }
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
    }

    /**
     * Start playing the previously prepared {@link org.tomahawk.libtomahawk.collection.Track}
     */
    @Override
    public void start() {
        Log.d(TAG, "start()");
        mIsPlaying = true;
        if (mService != null) {
            try {
                mService.play();
            } catch (RemoteException e) {
                Log.e(TAG, "start: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        } else {
            EventBus.getDefault().post(new PlaybackService.RequestServiceBindingEvent());
        }
    }

    /**
     * Pause playing the current {@link org.tomahawk.libtomahawk.collection.Track}
     */
    @Override
    public void pause() {
        Log.d(TAG, "pause()");
        mIsPlaying = false;
        if (mService != null) {
            try {
                mService.pause();
            } catch (RemoteException e) {
                Log.e(TAG, "pause: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        } else {
            EventBus.getDefault().post(new PlaybackService.RequestServiceBindingEvent());
        }
    }

    /**
     * Seek to the given playback position (in ms)
     */
    @Override
    public void seekTo(int msec) {
        Log.d(TAG, "seekTo()");
        if (mService != null) {
            try {
                mService.seek(msec);
            } catch (RemoteException e) {
                Log.e(TAG, "seekTo: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
            mPositionOffset = msec;
            mPositionTimeStamp = System.currentTimeMillis();
            mOverrideCurrentPosition = true;
            // After 1 second, we set mOverrideCurrentPosition to false again
            mResetOverrideHandler.sendEmptyMessageDelayed(1337, 1000);
        } else {
            EventBus.getDefault().post(new PlaybackService.RequestServiceBindingEvent());
        }
    }

    /**
     * Prepare the given url
     */
    @Override
    public MediaPlayerInterface prepare(Application application, final Query query,
            MediaPlayer.OnPreparedListener onPreparedListener,
            MediaPlayer.OnCompletionListener onCompletionListener,
            MediaPlayer.OnErrorListener onErrorListener) {
        Log.d(TAG, "prepare()");
        mOnPreparedListener = onPreparedListener;
        mOnCompletionListener = onCompletionListener;
        mPositionOffset = 0;
        mPositionTimeStamp = System.currentTimeMillis();
        mPreparedQuery = null;
        mPreparingQuery = query;
        if (mService != null) {
            ((ScriptResolver) PipeLine.getInstance().getResolver(TomahawkApp.PLUGINNAME_SPOTIFY))
                    .getAccessToken(new ScriptJob.ResultsCallback<ScriptResolverAccessTokenResult>(
                            ScriptResolverAccessTokenResult.class) {
                        @Override
                        public void onReportResults(ScriptResolverAccessTokenResult results) {
                            String[] pathParts =
                                    query.getPreferredTrackResult().getPath().split("/");
                            String uri = "spotify:track:" + pathParts[pathParts.length - 1];
                            try {
                                mService.prepare(uri, results.accessToken);
                            } catch (RemoteException e) {
                                Log.e(TAG, "prepare: " + e.getClass() + ": " + e
                                        .getLocalizedMessage());
                            }
                        }
                    });
        } else {
            EventBus.getDefault().post(new PlaybackService.RequestServiceBindingEvent());
        }
        return this;
    }

    @Override
    public void release() {
        Log.d(TAG, "release()");
        pause();
    }

    /**
     * @return the current track position
     */
    @Override
    public int getPosition() {
        if (mIsPlaying) {
            return (int) (System.currentTimeMillis() - mPositionTimeStamp) + mPositionOffset;
        } else {
            return mPositionOffset;
        }
    }

    @Override
    public boolean isPlaying(Query query) {
        return mPreparedQuery == query && mIsPlaying;
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
        mPositionOffset = 0;
        mPositionTimeStamp = System.currentTimeMillis();
        mPreparedQuery = mPreparingQuery;
        mPreparingQuery = null;
        mOnPreparedListener.onPrepared(mp);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion()");
        mOnCompletionListener.onCompletion(mp);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    public void setBitRate(int bitrateMode) {
        if (mService != null) {
            try {
                mService.setBitRate(bitrateMode);
            } catch (RemoteException e) {
                Log.e(TAG, "setBitRate: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        } else {
            EventBus.getDefault().post(new PlaybackService.RequestServiceBindingEvent());
        }
    }

    public void updateBitrate() {
        ConnectivityManager conMan = (ConnectivityManager) TomahawkApp.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Log.d(TAG, "Updating bitrate to HIGH, because we have a Wifi connection");
            setBitRate(SpotifyMediaPlayer.SPOTIFY_PREF_BITRATE_MODE_HIGH);
        } else {
            Log.d(TAG, "Updating bitrate to user setting, because we don't have a Wifi connection");
            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(TomahawkApp.getContext());
            int prefbitrate = preferences.getInt(
                    SpotifyMediaPlayer.SPOTIFY_PREF_BITRATE,
                    SpotifyMediaPlayer.SPOTIFY_PREF_BITRATE_MODE_MEDIUM);
            setBitRate(prefbitrate);
        }
    }
}
