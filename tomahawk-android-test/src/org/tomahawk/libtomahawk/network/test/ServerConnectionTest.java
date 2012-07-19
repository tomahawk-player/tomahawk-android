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
