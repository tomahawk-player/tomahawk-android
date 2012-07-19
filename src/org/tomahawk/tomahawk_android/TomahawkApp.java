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
package org.tomahawk.tomahawk_android;

import org.tomahawk.libtomahawk.account.AccountManager;

/**
 * This class contains the main application logic for Tomahawk.
 */
public class TomahawkApp {

    private static TomahawkApp instance = null;

    private AccountManager mAccountManager = null;

    /**
     * Returns TomahawkApp instance.
     * 
     * @return singleton instance of this class.
     */
    public static TomahawkApp instance() {
        if (instance == null)
            instance = new TomahawkApp();
        return instance;
    }

    /**
     * TomahawkApp constructor.
     */
    protected TomahawkApp() {
        mAccountManager = new AccountManager();
    }

    /**
     * Initialize the Tomahawk app.
     */
    public void initialize() {
        initAccounts();
    }

    /**
     * Initialize a new Tomahawk servant.
     */
    public void initAccounts() {
        mAccountManager.initAccounts();
    }

    /**
     * Returns the Tomahawk AccountManager;
     */
    public AccountManager getAccountManager() {
        return mAccountManager;
    }
}
