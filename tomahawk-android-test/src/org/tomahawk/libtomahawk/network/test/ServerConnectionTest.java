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
package org.tomahawk.libtomahawk.network.test;

import java.io.IOException;

import junit.framework.Assert;

import org.tomahawk.libtomahawk.network.ServerConnection;
import org.tomahawk.libtomahawk.network.TomahawkNetworkUtils;

import android.test.AndroidTestCase;

/**
 * Test class for Connection.
 */
public class ServerConnectionTest extends AndroidTestCase {

    private static final String id = new String("aee330b2-de0d-4b0b-af45-2759375357e0");
    ServerConnection conn = null;

    public void setUp() throws IOException {
        conn = new ServerConnection(TomahawkNetworkUtils.getDefaultTwkServerAddress(),
                TomahawkNetworkUtils.getDefaultTwkPort(), id);
    }

    public void testIsConnected() {
        Assert.assertTrue(conn != null);
        Assert.assertFalse(conn.isConnected());
    }
}
