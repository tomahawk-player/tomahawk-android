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

/**
 * Represents an abstract Tomahawk account (e.g. "Toma.hk", "Twitter", "LastFM")
 */
public abstract class Account {

    public static final int DISCONNECTED = 0;
    public static final int CONNECTING = 1;
    public static final int CONNECTED = 2;
    public static final int DISCONNECTING = 3;

    /**
     * Construct a new Account;
     */
    public Account() {

    }

    /**
     * Return the current connection state of this account.
     * 
     * @return
     */
    public abstract int connectionState();

    /**
     * Return this accounts SIP plugin.
     * 
     * @return
     */
    public abstract Plugin plugin();

    /**
     * Authenticate this account.
     * 
     * @throws InterruptedException
     */
    public abstract void authenticate();

    /**
     * Deauthenticate this account.
     */
    public abstract void deauthenticate();

    /**
     * Returns whether or not this account is authenticated.
     * 
     * @return
     */
    public abstract boolean isAuthenticated();
}
