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

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.tomahawk.libtomahawk.network.Connection;
import org.tomahawk.libtomahawk.network.ServerConnection;
import org.tomahawk.libtomahawk.network.TomahawkNetworkUtils;

import android.util.Log;

/**
 * This class represents a plugin which connects to primary Tomahawk server.
 */
public class TomahawkServerPlugin extends Plugin {

    private static final String TAG = TomahawkServerPlugin.class.getName();

    /**
     * Holds connection state for this plugin.
     */
    private int state = Account.DISCONNECTED;
    /**
     * Constructs a new TomahawkLANPlugin from the given TomahawkLANAccount.
     * 
     * @param account
     */
    public TomahawkServerPlugin(TomahawkServerAccount account) {
        super(account);
    }

    /**
     * Return the connection state of this plugin.
     * 
     * @return
     */
    public int connectionState() {
        return state;
    }

    /**
     * Connect this plugin to the Tomahawk server.
     * 
     * @throws InterruptedException
     */
    @Override
    public void connectPlugin() throws InterruptedException {

        try {

            state = Account.DISCONNECTED;
            while (state == Account.DISCONNECTED) {

                // Run an advertisement loop.
                try {
                    initiate();
                } catch (SocketTimeoutException e) {
                    Log.d(TAG, "Socket timeout. Sleeping...");
                }
            }

        } catch (UnknownHostException e) {
            Log.e(TAG, "Error initiating connection with Tomahawk server. " + e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "Error initiating connection with Tomahawk server. " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Disconnect this plugin.
     */
    @Override
    public void disconnectPlugin() {

    }

    /**
     * Initialize connection with Tomahawk web server.
     * 
     * @throws UnknownHostException
     * @throws IOException
     */
    private void initiate() throws UnknownHostException, IOException {

        if (state == Account.CONNECTED)
            return;

        // Hard coded connection details.
        InetAddress host = InetAddress.getByName("192.168.1.109");
        int port = TomahawkNetworkUtils.getDefaultTwkPort();
        String id = new String("aee330b2-de0d-4b0b-af45-2759375357e0");
        String key = new String("whitelist");

        Map<String, String> m = new HashMap<String, String>();
        m.put("conntype", "accept-offer");
        m.put("key", key);
        m.put("port", Integer.toString(port));
        m.put("nodeid", "9f99f0e3-2c97-499e-8d1d-d806213656b3");

        Connection conn = new ServerConnection(host, port, id);
        conn.setFirstMsg(m);
        conn.setName(host.getHostAddress());

        conn.setNodeId(id);

        connectToHost(host, port, key, conn);
        state = Account.CONNECTED;
    }
}
