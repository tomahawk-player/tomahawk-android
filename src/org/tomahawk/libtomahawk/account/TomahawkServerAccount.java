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

import android.util.Log;

/**
 * This class represents the default account for making connections with the
 * primary Tomahawk server.
 */
public class TomahawkServerAccount extends Account {

    private static final String TAG = TomahawkServerAccount.class.getName();

    private TomahawkServerPlugin mServerPlugin = null;

    /**
     * Constructor.
     */
    public TomahawkServerAccount() {
    }

    /**
     * Return the Plugin used by this account.
     */
    @Override
    public Plugin plugin() {
        if (mServerPlugin == null)
            mServerPlugin = new TomahawkServerPlugin(this);
        return mServerPlugin;
    }

    /**
     * Authenticate this account.
     * 
     * @throws s
     */
    @Override
    public void authenticate() {
        if (!isAuthenticated()) {
            try {
                ((TomahawkServerPlugin) plugin()).connectPlugin();
            } catch (InterruptedException e) {
                Log.e(TAG, "Error authenticating: " + e.toString());
            }
        }
    }

    /**
     * De-authenticate this account.
     */
    @Override
    public void deauthenticate() {
        return;
    }

    /**
     * Returns whether this account is connected.
     */
    @Override
    public boolean isAuthenticated() {
        return connectionState() == Account.CONNECTED;
    }

    /**
     * Returns the connection state of this plugin.
     */
    @Override
    public int connectionState() {
        if (mServerPlugin == null)
            return Account.DISCONNECTED;
        return mServerPlugin.connectionState();
    }
}
