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
package org.tomahawk.tomahawk_android.services;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.AskAccessActivity;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteController;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.KeyEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Service to fetch all metadata from other media player apps. Does nothing if not run on Kitkat.
 * Compat code is included in MicroService instead.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class RemoteControllerService extends NotificationListenerService
        implements RemoteController.OnClientUpdateListener {

    private static final String TAG = RemoteControllerService.class.getSimpleName();

    //dimensions in pixels for artwork
    private static final int BITMAP_HEIGHT = 1024;

    private static final int BITMAP_WIDTH = 1024;

    private RemoteController mRemoteController;

    private Context mContext;

    @Override
    public IBinder onBind(Intent intent) {
        if ("android.service.notification.NotificationListenerService".equals(intent.getAction())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                DatabaseHelper.getInstance().ensureInit();
                InfoSystem.getInstance().ensureInit();
                AuthenticatorManager.getInstance().ensureInit();
                setRemoteControllerEnabled();
            }
        }
        return super.onBind(intent);
    }

    /**
     * Enables the RemoteController thus allowing us to receive metadata updates.
     */
    public void setRemoteControllerEnabled() {
        Log.d(TAG, "setRemoteControllerEnabled");
        mContext = getApplicationContext();
        mRemoteController = new RemoteController(mContext, this);
        if (((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE))
                .registerRemoteController(mRemoteController)) {
            mRemoteController.setArtworkConfiguration(BITMAP_WIDTH, BITMAP_HEIGHT);
            setSynchronizationMode(mRemoteController,
                    RemoteController.POSITION_SYNCHRONIZATION_CHECK);
        } else {
            attemptAskAccess();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setRemoteControllerDisabled();
        }
    }

    /**
     * Disables RemoteController.
     */
    public void setRemoteControllerDisabled() {
        Log.d(TAG, "setRemoteControllerDisabled");
        if (mContext != null
                && mContext.getSystemService(Context.AUDIO_SERVICE) instanceof AudioManager) {
            ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE))
                    .unregisterRemoteController(mRemoteController);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {
    }

    @Override
    public void onClientChange(boolean arg0) {
    }

    @Override
    public void onClientMetadataUpdate(RemoteController.MetadataEditor arg0) {
        Log.d(TAG, "onClientMetadataUpdate");
        String trackName = arg0.getString(MediaMetadataRetriever.METADATA_KEY_TITLE, null);
        String artistName = arg0.getString(MediaMetadataRetriever.METADATA_KEY_ARTIST, null);
        String albumArtistName = arg0
                .getString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, null);
        String albumName = arg0.getString(MediaMetadataRetriever.METADATA_KEY_ALBUM, null);
        MicroService.scrobbleTrack(trackName, artistName, albumName, albumArtistName);
    }

    @Override
    public void onClientPlaybackStateUpdate(int arg0) {
    }

    @Override
    public void onClientPlaybackStateUpdate(int arg0, long arg1, long arg2, float arg3) {
    }

    @Override
    public void onClientTransportControlUpdate(int arg0) {
    }

    /**
     * This method lets us avoid a bug in RemoteController which results in an exception when
     * calling RemoteController#setSynchronizationMode(int) (doesn't seem to work though)
     */
    private void setSynchronizationMode(RemoteController controller, int sync) {
        if ((sync != RemoteController.POSITION_SYNCHRONIZATION_NONE) && (sync
                != RemoteController.POSITION_SYNCHRONIZATION_CHECK)) {
            throw new IllegalArgumentException("Unknown synchronization mode " + sync);
        }

        Class<?> iRemoteControlDisplayClass;

        try {
            iRemoteControlDisplayClass = Class.forName("android.media.IRemoteControlDisplay");
        } catch (ClassNotFoundException e1) {
            throw new RuntimeException(
                    "Class IRemoteControlDisplay doesn't exist, can't access it with reflection");
        }

        Method remoteControlDisplayWantsPlaybackPositionSyncMethod;
        try {
            remoteControlDisplayWantsPlaybackPositionSyncMethod = AudioManager.class
                    .getDeclaredMethod("remoteControlDisplayWantsPlaybackPositionSync",
                            iRemoteControlDisplayClass, boolean.class);
            remoteControlDisplayWantsPlaybackPositionSyncMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    "Method remoteControlDisplayWantsPlaybackPositionSync() doesn't exist, can't access it with reflection");
        }

        Object rcDisplay;
        Field rcDisplayField;
        try {
            rcDisplayField = RemoteController.class.getDeclaredField("mRcd");
            rcDisplayField.setAccessible(true);
            rcDisplay = rcDisplayField.get(mRemoteController);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Field mRcd doesn't exist, can't access it with reflection");
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Field mRcd can't be accessed - access denied");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Field mRcd can't be accessed - invalid argument");
        }

        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        try {
            remoteControlDisplayWantsPlaybackPositionSyncMethod
                    .invoke(am, iRemoteControlDisplayClass.cast(rcDisplay), true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(
                    "Method remoteControlDisplayWantsPlaybackPositionSync() invocation failure - access denied");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Method remoteControlDisplayWantsPlaybackPositionSync() invocation failure - invalid arguments");
        } catch (InvocationTargetException e) {
            throw new RuntimeException(
                    "Method remoteControlDisplayWantsPlaybackPositionSync() invocation failure - invalid invocation target");
        }
    }

    /**
     * Send a keyEvent with the given keyCode to the RemoteController
     *
     * @param keyCode the keyCode that should be send to the RemoteController
     * @return return true if both clicks (up and down) were delivered successfully
     */
    private boolean sendKeyEvent(int keyCode) {
        //send "down" and "up" keyevents.
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        boolean first = mRemoteController.sendMediaKeyEvent(keyEvent);
        keyEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
        boolean second = mRemoteController.sendMediaKeyEvent(keyEvent);

        return first && second;
    }

    /**
     * Starts the AskAccessActivity in order to ask the user for permission to the notification
     * listener, if the user hasn't been asked before and is logged into hatchet
     */
    public static void attemptAskAccess() {
        if (!PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext())
                .getBoolean(AskAccessActivity.ASKED_FOR_ACCESS, false)) {
            askAccess();
        }
    }

    /**
     * Starts the AskAccessActivity in order to ask the user for permission to the notification
     * listener, if the user is logged into Hatchet and we don't already have access
     */
    public static void askAccess() {
        if (AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET).isLoggedIn()
                && !isRemoteControllerServiceRunning()) {
            TomahawkApp.getContext().startActivity(
                    new Intent(TomahawkApp.getContext(), AskAccessActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            );
        }
    }

    /**
     * @return true if the RemoteControllerService is already running, therefore indicating that the
     * user has already granted us NotificationListener access
     */
    private static boolean isRemoteControllerServiceRunning() {
        ActivityManager manager = (ActivityManager) TomahawkApp.getContext().getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (RemoteControllerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
