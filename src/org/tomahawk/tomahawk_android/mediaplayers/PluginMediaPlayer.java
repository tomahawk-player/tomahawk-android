/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;

import android.app.Application;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public abstract class PluginMediaPlayer extends IPluginServiceCallback.Stub
        implements TomahawkMediaPlayer {

    private static final String TAG = PluginMediaPlayer.class.getSimpleName();

    private String mPluginName;

    private String mPackageName;

    private IPluginService mService;

    private List<ServiceCall> mWaitingServiceCalls = new ArrayList<>();

    public interface ServiceCall {

        void call(IPluginService pluginService);
    }

    private TomahawkMediaPlayerCallback mMediaPlayerCallback;

    private boolean mIsPlaying;

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    private boolean mShowFakePosition = false;

    private final DisableFakePositionHandler mDisableFakePositionHandler
            = new DisableFakePositionHandler(this);

    private static class DisableFakePositionHandler
            extends WeakReferenceHandler<PluginMediaPlayer> {

        public DisableFakePositionHandler(PluginMediaPlayer referencedObject) {
            super(referencedObject);
        }

        @Override
        public void handleMessage(Message msg) {
            if (getReferencedObject() != null) {
                getReferencedObject().mShowFakePosition = false;
            }
        }
    }

    private long mPositionTimeStamp;

    private int mPositionOffset;

    private long mFakePositionTimeStamp;

    private int mFakePositionOffset;

    public PluginMediaPlayer(String pluginName, String packageName) {
        mPluginName = pluginName;
        mPackageName = packageName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public ScriptResolver getScriptResolver() {
        ScriptResolver scriptResolver =
                (ScriptResolver) PipeLine.get().getResolver(mPluginName);
        if (scriptResolver == null) {
            Log.e(TAG, "getScriptResolver - Couldn't find associated ScriptResolver!");
        }
        return scriptResolver;
    }

    public void setService(IPluginService service) {
        mService = service;
        if (mService != null) {
            for (int i = 0; i < mWaitingServiceCalls.size(); i++) {
                mWaitingServiceCalls.remove(i).call(mService);
            }
        }
    }

    /**
     * Call the {@link IPluginService} that is associated with this {@link PluginMediaPlayer}. If it
     * is not available, a binding will be requested. The given {@link ServiceCall} will be cached
     * in that case and called after a successful binding to the {@link IPluginService}.
     *
     * @param serviceCall the actual {@link ServiceCall} that should be made to the {@link
     *                    IPluginService}.
     */
    protected void callService(ServiceCall serviceCall) {
        callService(serviceCall, true);
    }

    /**
     * Call the {@link IPluginService} that is associated with this {@link PluginMediaPlayer}. If it
     * is not available, a binding will be requested.
     *
     * @param serviceCall the actual {@link ServiceCall} that should be made to the {@link
     *                    IPluginService}.
     * @param cacheCall   Whether or not the given {@link ServiceCall} should be cached if the
     *                    {@link IPluginService} is unavailable and called after a successful
     *                    binding to the {@link IPluginService}.
     */
    protected void callService(ServiceCall serviceCall, boolean cacheCall) {
        if (mService != null) {
            serviceCall.call(mService);
        } else {
            if (cacheCall) {
                mWaitingServiceCalls.add(serviceCall);
            }
            requestService();
        }
    }

    /**
     * Construct and send off a {@link PlaybackService.RequestServiceBindingEvent} to the {@link
     * PlaybackService}. As soon as the {@link PlaybackService} has been successfully bound to the
     * {@link IPluginService} {@link #setService} will be called.
     */
    private void requestService() {
        PlaybackService.RequestServiceBindingEvent event =
                new PlaybackService.RequestServiceBindingEvent(this, mPackageName);
        EventBus.getDefault().post(event);
    }

    /**
     * This method returns a {@link ServiceCall} which defines the way the {@link PluginMediaPlayer}
     * calls the {@link IPluginService} in order to prepare a given Query for playback.
     */
    public abstract ServiceCall getPrepareServiceCall(Application application, Query query);

    /**
     * Prepare the given {@link Query} for playback
     *
     * @param application an {@link Application} object that can be used as {@link
     *                    android.content.Context}
     * @param query       the {@link Query} that should be prepared for playback
     * @param callback    a {@link TomahawkMediaPlayerCallback} that should be stored and be used to
     *                    report certain callbacks back to the {@link PlaybackService}
     */
    @Override
    public TomahawkMediaPlayer prepare(Application application, final Query query,
            TomahawkMediaPlayerCallback callback) {
        Log.d(TAG, "prepare()");
        mMediaPlayerCallback = callback;
        mPositionOffset = 0;
        mPositionTimeStamp = System.currentTimeMillis();
        mPreparedQuery = null;
        mPreparingQuery = query;
        callService(getPrepareServiceCall(application, query));
        return this;
    }

    /**
     * Start playing the previously prepared {@link Query}
     */
    public void start() {
        Log.d(TAG, "start()");
        mIsPlaying = true;
        callService(new ServiceCall() {
            @Override
            public void call(IPluginService pluginService) {
                try {
                    pluginService.play();
                } catch (RemoteException e) {
                    Log.e(TAG, "start: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
            }
        });
    }

    /**
     * Pause playing the current {@link Query}
     */
    public void pause() {
        Log.d(TAG, "pause()");
        mIsPlaying = false;
        callService(new ServiceCall() {
            @Override
            public void call(IPluginService pluginService) {
                try {
                    pluginService.pause();
                } catch (RemoteException e) {
                    Log.e(TAG, "pause: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
            }
        });
    }

    /**
     * Seek to the given playback position (in ms)
     */
    public void seekTo(final int msec) {
        Log.d(TAG, "seekTo()");
        callService(new ServiceCall() {
            @Override
            public void call(IPluginService pluginService) {
                try {
                    pluginService.seek(msec);
                } catch (RemoteException e) {
                    Log.e(TAG, "seekTo: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
                mFakePositionOffset = msec;
                mFakePositionTimeStamp = System.currentTimeMillis();
                mShowFakePosition = true;
                // After 1 second, we set mShowFakePosition to false again
                mDisableFakePositionHandler.sendEmptyMessageDelayed(1337, 1000);
            }
        });
    }

    /**
     * Release any relevant resources that this {@link PluginMediaPlayer} might hold onto
     */
    public void release() {
        Log.d(TAG, "release()");
        pause();
    }

    /**
     * @return the current track position
     */
    public int getPosition() {
        if (mShowFakePosition) {
            if (mIsPlaying) {
                return (int) (System.currentTimeMillis() - mFakePositionTimeStamp)
                        + mFakePositionOffset;
            } else {
                return mFakePositionOffset;
            }
        } else {
            if (mIsPlaying) {
                return (int) (System.currentTimeMillis() - mPositionTimeStamp) + mPositionOffset;
            } else {
                return mPositionOffset;
            }
        }
    }

    public boolean isPlaying(Query query) {
        return mPreparedQuery == query && mIsPlaying;
    }

    public boolean isPreparing(Query query) {
        return mPreparingQuery == query;
    }

    public boolean isPrepared(Query query) {
        return mPreparedQuery == query;
    }

    // < Implementation of IPluginServiceCallback.Stub>
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
        Log.d(TAG, "onPrepared()");
        mPositionOffset = 0;
        mPositionTimeStamp = System.currentTimeMillis();
        mPreparedQuery = mPreparingQuery;
        mPreparingQuery = null;
        mMediaPlayerCallback.onPrepared(mPreparedQuery);
    }

    @Override
    public void onPlayerEndOfTrack() throws RemoteException {
        Log.d(TAG, "onCompletion()");
        mMediaPlayerCallback.onCompletion(mPreparedQuery);
    }

    @Override
    public void onPlayerPositionChanged(int position, long timeStamp) throws RemoteException {
        mPositionTimeStamp = timeStamp;
        mPositionOffset = position;
    }

    @Override
    public void onError(String message) throws RemoteException {
        mMediaPlayerCallback.onError(message);
    }
    // </ Implementation of IPluginServiceCallback.Stub>

}
