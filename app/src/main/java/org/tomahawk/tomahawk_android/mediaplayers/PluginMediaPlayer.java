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
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

public abstract class PluginMediaPlayer implements TomahawkMediaPlayer {

    private static final String TAG = PluginMediaPlayer.class.getSimpleName();

    /**
     * Command to the service to register a client, receiving callbacks from the service. The
     * Message's replyTo field must be a Messenger of the client where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks from the service.
     * The Message's replyTo field must be a Messenger of the client as previously given with
     * MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * Commands to the service
     */
    protected static final int MSG_PREPARE = 100;

    protected static final String MSG_PREPARE_ARG_URI = "uri";

    protected static final String MSG_PREPARE_ARG_ACCESSTOKEN = "accessToken";

    protected static final String MSG_PREPARE_ARG_ACCESSTOKENEXPIRES = "accessTokenExpires";

    protected static final int MSG_PLAY = 101;

    protected static final int MSG_PAUSE = 102;

    protected static final int MSG_SEEK = 103;

    protected static final String MSG_SEEK_ARG_MS = "ms";

    protected static final int MSG_SETBITRATE = 104;

    protected static final String MSG_SETBITRATE_ARG_MODE = "mode";

    /**
     * Commands to the client
     */
    protected static final int MSG_ONPAUSE = 200;

    protected static final int MSG_ONPLAY = 201;

    protected static final int MSG_ONPREPARED = 202;

    protected static final String MSG_ONPREPARED_ARG_URI = "uri";

    protected static final int MSG_ONPLAYERENDOFTRACK = 203;

    protected static final int MSG_ONPLAYERPOSITIONCHANGED = 204;

    protected static final String MSG_ONPLAYERPOSITIONCHANGED_ARG_POSITION = "position";

    protected static final String MSG_ONPLAYERPOSITIONCHANGED_ARG_TIMESTAMP = "timestamp";

    protected static final int MSG_ONERROR = 205;

    protected static final String MSG_ONERROR_ARG_MESSAGE = "message";

    private String mPluginName;

    private String mPackageName;

    private int mMinVersionCode;

    private boolean mIsRequestingService = false;

    /**
     * Messenger for communicating with service.
     */
    private Messenger mService = null;

    private List<Message> mWaitingMessages = new ArrayList<>();

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

    private Map<String, Query> mUriToQueryMap = new HashMap<>();

    public PluginMediaPlayer(String pluginName, String packageName, int minVersionCode) {
        mPluginName = pluginName;
        mPackageName = packageName;
        mMinVersionCode = minVersionCode;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public int getMinVersionCode() {
        return mMinVersionCode;
    }

    public ScriptResolver getScriptResolver() {
        ScriptResolver scriptResolver =
                (ScriptResolver) PipeLine.get().getResolver(mPluginName);
        if (scriptResolver == null) {
            Log.e(TAG, "getScriptResolver - Couldn't find associated ScriptResolver!");
        }
        return scriptResolver;
    }

    /**
     * Handler of incoming messages from service.
     */
    private static class IncomingHandler extends WeakReferenceHandler<PluginMediaPlayer> {

        public IncomingHandler(PluginMediaPlayer referencedObject) {
            super(referencedObject);
        }

        @Override
        public void handleMessage(Message msg) {
            PluginMediaPlayer mp = getReferencedObject();
            switch (msg.what) {
                case MSG_ONPREPARED:
                    String uri = msg.getData().getString(MSG_ONPREPARED_ARG_URI);
                    Log.d(TAG, "onPrepared() - uri: " + uri);
                    mp.mPositionOffset = 0;
                    mp.mPositionTimeStamp = System.currentTimeMillis();
                    mp.mPreparedQuery = mp.mUriToQueryMap.get(uri);
                    mp.mPreparingQuery = null;
                    mp.mMediaPlayerCallback.onPrepared(mp.mPreparedQuery);
                    break;
                case MSG_ONPLAY:
                    mp.mPositionTimeStamp = System.currentTimeMillis();
                    break;
                case MSG_ONPAUSE:
                    mp.mPositionOffset =
                            (int) (System.currentTimeMillis() - mp.mPositionTimeStamp)
                                    + mp.mPositionOffset;
                    mp.mPositionTimeStamp = System.currentTimeMillis();
                    break;
                case MSG_ONPLAYERPOSITIONCHANGED:
                    long timeStamp =
                            msg.getData().getLong(MSG_ONPLAYERPOSITIONCHANGED_ARG_TIMESTAMP);
                    int position =
                            msg.getData().getInt(MSG_ONPLAYERPOSITIONCHANGED_ARG_POSITION);

                    mp.mPositionTimeStamp = timeStamp;
                    mp.mPositionOffset = position;
                    break;
                case MSG_ONPLAYERENDOFTRACK:
                    Log.d(TAG, "onCompletion()");
                    mp.mMediaPlayerCallback.onCompletion(mp.mPreparedQuery);
                    break;
                case MSG_ONERROR:
                    String message = msg.getData().getString(MSG_ONERROR_ARG_MESSAGE);

                    mp.mMediaPlayerCallback.onError(message);
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mReceivingMessenger = new Messenger(new IncomingHandler(this));

    public Messenger getReceivingMessenger() {
        return mReceivingMessenger;
    }

    public synchronized void setService(Messenger service) {
        mIsRequestingService = false;
        mService = service;
        if (mService != null) {
            // send all cached messages
            while (!mWaitingMessages.isEmpty()) {
                callService(mWaitingMessages.remove(0));
            }
        }
    }

    protected synchronized void callService(int what) {
        callService(what, null);
    }

    protected synchronized void callService(int what, Bundle bundle) {
        Message message = Message.obtain(null, what);
        message.setData(bundle);
        callService(message);
    }

    private synchronized void callService(Message message) {
        if (mService != null) {
            try {
                mService.send(message);
            } catch (RemoteException e) {
                Log.e(TAG, "Service crashed: ", e);
            }
        } else if (!mIsRequestingService) {
            // cache the message, will be send in setService
            mIsRequestingService = true;
            mWaitingMessages.add(message);
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

    public abstract String getUri(Query query);

    public abstract void prepare(String uri);

    /**
     * Prepare the given {@link Query} for playback
     *
     * @param query    the {@link Query} that should be prepared for playback
     * @param callback a {@link TomahawkMediaPlayerCallback} that should be stored and be used to
     *                 report certain callbacks back to the {@link PlaybackService}
     */
    @Override
    public TomahawkMediaPlayer prepare(Query query, TomahawkMediaPlayerCallback callback) {
        Log.d(TAG, "prepare()");
        mMediaPlayerCallback = callback;
        mPositionOffset = 0;
        mPositionTimeStamp = System.currentTimeMillis();
        mPreparedQuery = null;
        mPreparingQuery = query;

        String uri = getUri(query);
        mUriToQueryMap.put(uri, query);
        prepare(uri);
        return this;
    }

    /**
     * Start playing the previously prepared {@link Query}
     */
    @Override
    public void start() {
        Log.d(TAG, "start()");
        mIsPlaying = true;
        callService(MSG_PLAY);
    }

    /**
     * Pause playing the current {@link Query}
     */
    @Override
    public void pause() {
        Log.d(TAG, "pause()");
        mIsPlaying = false;
        callService(MSG_PAUSE);
    }

    /**
     * Seek to the given playback position (in ms)
     */
    @Override
    public void seekTo(final int msec) {
        Log.d(TAG, "seekTo()");
        Bundle args = new Bundle();
        args.putInt(MSG_SEEK_ARG_MS, msec);
        callService(MSG_SEEK, args);

        mFakePositionOffset = msec;
        mFakePositionTimeStamp = System.currentTimeMillis();
        mShowFakePosition = true;
        // After 1 second, we set mShowFakePosition to false again
        mDisableFakePositionHandler.sendEmptyMessageDelayed(1337, 1000);
    }

    /**
     * Release any relevant resources that this {@link PluginMediaPlayer} might hold onto
     */
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

}
