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

import android.util.Log;

/**
 * Manages Tomahawk's account's
 */
public class AccountManager {

    private static final String TAG = AccountManager.class.getName();

    private static AccountManager mInstance = null;
    private ArrayList<Account> mAccounts = null;

    /**
     * Returns the AccountManager instance.
     * 
     * @return
     */
    public static AccountManager instance() {
        if (mInstance == null)
            mInstance = new AccountManager();
        return mInstance;
    }

    /**
     * Construct a new Account manager.
     */
    protected AccountManager() {
        mAccounts = new ArrayList<Account>();
    }

    /**
     * Initialize all active accounts.
     */
    public void initAccounts() {
        Log.d(TAG, "Initializing accounts.");

        mAccounts.add(new TomahawkServerAccount());

        for (final Account account : mAccounts) {

            new Thread(new Runnable() {

                @Override
                public void run() {
                    account.authenticate();
                }
            }).start();
        }
    }
}