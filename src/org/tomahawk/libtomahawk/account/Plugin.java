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
import java.util.HashMap;

import org.tomahawk.libtomahawk.network.Connection;
import org.tomahawk.libtomahawk.network.TomahawkNetworkUtils;

import android.util.Log;

/**
 * Abstract representation of a Tomahawk SIP plugin.
 */
public abstract class Plugin {

    private static final String TAG = Plugin.class.getName();

    private Account account = null;

    /**
     * Constructs a new SipPlugin for the given account.
     * 
     * @param account
     */
    public Plugin(Account account) {
        this.account = account;
    }

    /**
     * Connect to another Tomahawk peer.
     * 
     * @param host
     * @param port
     * @param key
     * @param conn
     * @throws IOException
     */
    protected void connectToHost(InetAddress host, int port, String key, final Connection conn)
            throws IOException {

        if (port < 0 || conn == null)
            return;

        if (key.length() > 0 && conn.getMsg() == null) {

            HashMap<String, String> m = new HashMap<String, String>();
            m.put("conntype", "accept-offer");
            m.put("key", key);
            m.put("port", Integer.toString(TomahawkNetworkUtils.getDefaultTwkPort()));
            m.put("controlid", "9f99f0e3-2c97-499e-8d1d-d806213656b3");

            conn.setFirstMsg(m);
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                conn.start();
            }
        }).start();
        Log.d(TAG, "Attempting to connect to peer" + host.toString());
    }

    /**
     * Get the account this SipPlugin manages.
     * 
     * @return account
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Connect this SIP plugin.
     * 
     * @throws InterruptedException
     */
    public abstract void connectPlugin() throws InterruptedException;

    /**
     * Disconnect this SIP plugin.
     */
    public abstract void disconnectPlugin();
}
