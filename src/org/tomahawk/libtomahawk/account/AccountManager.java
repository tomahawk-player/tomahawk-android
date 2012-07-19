/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
package org.tomahawk.libtomahawk.account;

import java.util.ArrayList;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

/**
 * Manages Tomahawk's account's
 */
public class AccountManager implements Handler.Callback {

    private static final String TAG = AccountManager.class.getName();

    private static final int AUTHENTICATE_ACCOUNT_MSG = 0;

    private Looper mLooper;
    private Handler mHandler;

    private ArrayList<Account> mAccounts = null;

    /**
     * Construct a new Account manager.
     */
    public AccountManager() {
        mAccounts = new ArrayList<Account>();

        HandlerThread thread = new HandlerThread("PlaybackService",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mLooper = thread.getLooper();
        mHandler = new Handler(mLooper, this);
    }

    /**
     * Initialize all active accounts.
     */
    public void initAccounts() {
        Log.d(TAG, "Initializing accounts.");

        mAccounts.add(new TomahawkServerAccount());

        for (final Account account : mAccounts) {
            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(AUTHENTICATE_ACCOUNT_MSG, 0, 0, account), 400);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {

        switch (msg.what) {

        case AUTHENTICATE_ACCOUNT_MSG:
            ((Account) msg.obj).authenticate();
            return true;

        default:
            Log.e(TAG, "Cannot handle Message: " + msg.toString());

        }

        return false;
    }
}