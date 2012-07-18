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
package org.tomahawk.libtomahawk.network;

import java.io.IOException;
import java.net.InetAddress;

import android.util.Log;

/**
 * Basic networking utilities for Tomahawk API.
 */
public class TomahawkNetworkUtils {

    private static final String TAG = TomahawkNetworkUtils.class.getName();

    /**
     * Returns the default Tomahawk networking port.
     */
    public static final int getDefaultTwkPort() {
        return 50210;
    }

    /**
     * Determine whether the given ip address is whitelisted.
     * 
     * @param ip
     * @return
     */
    public static final boolean isIPWhitelisted(InetAddress ip) {
        Log.d(TAG, "Checking IP whitelist status.");
        return true;
    }

    public static InetAddress getDefaultTwkServerAddress() {
        try {
            return InetAddress.getByName("192.168.1.109");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

}
