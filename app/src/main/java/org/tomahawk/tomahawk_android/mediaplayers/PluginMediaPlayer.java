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

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

public abstract class PluginMediaPlayer extends TomahawkMediaPlayer {

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

    private boolean mIsRequestingService = false;

    /**
     * Messenger for communicating with service.
     */
    private Messenger mService = null;

    private List<Message> mWaitingMessages = new ArrayList<>();

    private TomahawkMediaPlayerCallback mMediaPlayerCallback;

    private boolean mIsPlaying;

    private int mPlayState = PlaybackStateCompat.STATE_NONE;

    private Query mPreparedQuery;

    private Query mPreparingQuery;

    private Query mActuallyPreparingQuery;

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

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                // This is called when the connection with the service has been
                // established, giving us the service object we can use to
                // interact with the service.  We are communicating with our
                // service through an IDL interface, so get a client-side
                // representation of that from the raw service object.
                Messenger messenger = new Messenger(service);
                Message msg = Message.obtain(null, PluginMediaPlayer.MSG_REGISTER_CLIENT);
                msg.replyTo = getReceivingMessenger();
                messenger.send(msg);
                setService(messenger);
                Log.d(TAG, "Successfully attached to service! :)");
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
                Log.e(TAG, "Service crashed before we could do anything."
                        + " Waiting for it to restart and report for duty...");
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            setService(null);
            Log.e(TAG, "Service crashed :(");
        }
    };

    private boolean mRestorePosition = false;

    private String mPreparedUri;

    private long mPositionTimeStamp;

    private int mPositionOffset;

    private long mFakePositionTimeStamp;

    private long mFakePositionOffset;

    private Map<String, Query> mUriToQueryMap = new HashMap<>();

    public PluginMediaPlayer(String pluginName, String packageName) {
        mPluginName = pluginName;
        mPackageName = packageName;
    }

    public ScriptResolver getScriptResolver() {
        ScriptResolver scriptResolver = PipeLine.get().getResolver(mPluginName);
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
                    if (mp.mPreparingQuery != null
                            && mp.mActuallyPreparingQuery == mp.mPreparingQuery) {
                        mp.mActuallyPreparingQuery = null;
                        mp.mPreparedQuery = mp.mUriToQueryMap.get(uri);
                        mp.mPreparingQuery = null;
                        if (mp.mMediaPlayerCallback != null) {
                            mp.mMediaPlayerCallback.onPrepared(mp, mp.mPreparedQuery);
                        } else {
                            Log.e(TAG,
                                    "Wasn't able to call onPrepared because callback object is null");
                        }
                        mp.handlePlayState();
                        if (mp.mRestorePosition && mp.mPreparedUri != null
                                && mp.mPreparedUri.equals(uri)) {
                            mp.mRestorePosition = false;
                            mp.seekTo(mp.mPositionOffset);
                        } else {
                            mp.mPositionOffset = 0;
                            mp.mPositionTimeStamp = System.currentTimeMillis();
                        }
                        mp.mPreparedUri = uri;
                    }
                    break;
                case MSG_ONPLAY:
                    mp.mIsPlaying = true;
                    mp.mPositionTimeStamp = System.currentTimeMillis();
                    break;
                case MSG_ONPAUSE:
                    mp.mIsPlaying = false;
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
                    if (mp.mMediaPlayerCallback != null) {
                        mp.mMediaPlayerCallback.onCompletion(mp, mp.mPreparedQuery);
                    } else {
                        Log.e(TAG,
                                "Wasn't able to call onCompletion because callback object is null");
                    }
                    break;
                case MSG_ONERROR:
                    String message = msg.getData().getString(MSG_ONERROR_ARG_MESSAGE);

                    if (mp.mMediaPlayerCallback != null) {
                        mp.mMediaPlayerCallback.onError(mp, message);
                    } else {
                        Log.e(TAG, "Wasn't able to call onError because callback object is null");
                    }
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
                mWaitingMessages.add(message);
            }
        } else {
            // cache the message, will be send in setService
            mWaitingMessages.add(message);
            if (!mIsRequestingService) {
                mIsRequestingService = true;
                requestService();
            }
        }
    }

    /**
     * Construct and send off a {@link PlaybackService.RequestServiceBindingEvent} to the {@link
     * PlaybackService}. As soon as the {@link PlaybackService} has been successfully bound to the
     * PluginService {@link #setService} will be called.
     */
    private void requestService() {
        PlaybackService.RequestServiceBindingEvent event =
                new PlaybackService.RequestServiceBindingEvent(mConnection, mPackageName);
        EventBus.getDefault().post(event);
    }

    public synchronized void setService(Messenger service) {
        mIsRequestingService = false;
        mService = service;
        if (mService != null) {
            // send all cached messages
            while (!mWaitingMessages.isEmpty()) {
                callService(mWaitingMessages.remove(0));
            }
        } else {
            mRestorePosition = true;
            mPreparedQuery = null;
            mPreparingQuery = null;
            mIsPlaying = false;
        }
    }

    public synchronized boolean isBound() {
        return mService != null;
    }

    public ServiceConnection getServiceConnection() {
        return mConnection;
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
    public void prepare(Query query, TomahawkMediaPlayerCallback callback) {
        Log.d(TAG, "prepare()");
        mMediaPlayerCallback = callback;
        mPreparedQuery = null;
        mPreparingQuery = query;
        mActuallyPreparingQuery = query;
        callService(MSG_PAUSE);

        String uri = getUri(query);
        mUriToQueryMap.put(uri, query);
        prepare(uri);
    }

    /**
     * Start playing the previously prepared {@link Query}
     */
    @Override
    public void play() {
        Log.d(TAG, "play()");
        mPlayState = PlaybackStateCompat.STATE_PLAYING;
        handlePlayState();
    }

    /**
     * Pause playing the current {@link Query}
     */
    @Override
    public void pause() {
        Log.d(TAG, "pause()");
        mPlayState = PlaybackStateCompat.STATE_PAUSED;
        handlePlayState();
    }

    /**
     * Seek to the given playback position (in ms)
     */
    @Override
    public void seekTo(final long msec) {
        Log.d(TAG, "seekTo()");
        Bundle args = new Bundle();
        args.putInt(MSG_SEEK_ARG_MS, (int) msec);
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
        mPreparedQuery = null;
        mPreparingQuery = null;
        callService(MSG_PAUSE);
        mMediaPlayerCallback = null;
    }

    /**
     * @return the current track position
     */
    @Override
    public long getPosition() {
        if (mShowFakePosition) {
            if (mIsPlaying) {
                return System.currentTimeMillis() - mFakePositionTimeStamp + mFakePositionOffset;
            } else {
                return mFakePositionOffset;
            }
        } else {
            if (mIsPlaying) {
                return System.currentTimeMillis() - mPositionTimeStamp + mPositionOffset;
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

    private void handlePlayState() {
        if (mPreparedQuery != null) {
            if (mPlayState == PlaybackStateCompat.STATE_PAUSED && mIsPlaying) {
                callService(MSG_PAUSE);
            } else if (mPlayState == PlaybackStateCompat.STATE_PLAYING && !mIsPlaying) {
                callService(MSG_PLAY);
            }
        }
    }

}
