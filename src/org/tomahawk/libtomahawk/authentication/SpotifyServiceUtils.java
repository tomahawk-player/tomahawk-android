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
package org.tomahawk.libtomahawk.authentication;

import org.tomahawk.tomahawk_android.services.SpotifyService;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class SpotifyServiceUtils {

    // Used for debug logging
    private static final String TAG = SpotifyServiceUtils.class.getSimpleName();

    public static void sendMsg(Messenger messenger, int msg) {
        try {
            messenger.send(Message.obtain(null, msg));
        } catch (RemoteException e) {
            Log.e(TAG, "sendMsg: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }


    public static void sendMsg(Messenger messenger, int msg, String value) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString(SpotifyService.STRING_KEY, value);
            Message message = Message.obtain(null, msg);
            message.setData(bundle);
            messenger.send(message);
        } catch (RemoteException e) {
            Log.e(TAG, "sendMsg: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }


    public static void sendMsg(Messenger messenger, int msg, String value, String value2) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString(SpotifyService.STRING_KEY, value);
            bundle.putString(SpotifyService.STRING_KEY2, value2);
            Message message = Message.obtain(null, msg);
            message.setData(bundle);
            messenger.send(message);
        } catch (RemoteException e) {
            Log.e(TAG, "sendMsg: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public static void sendMsg(Messenger messenger, int msg, int value) {
        try {
            messenger.send(Message.obtain(null, msg, value, 0));
        } catch (RemoteException e) {
            Log.e(TAG, "sendMsg: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public static void registerMsg(Messenger messenger, Messenger messengerToRegister) {
        try {
            Message message = Message.obtain(null, SpotifyService.MSG_REGISTERCLIENT);
            message.replyTo = messengerToRegister;
            messenger.send(message);
        } catch (RemoteException e) {
            Log.e(TAG, "registerMsg: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }
}
