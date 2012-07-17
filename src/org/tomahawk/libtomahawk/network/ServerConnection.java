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
 * Represents a Tomahawk ControlConnection. Used for LAN communications.
 */
public class ServerConnection extends Connection {

    private final String TAG = ServerConnection.class.getName();

    private String id = null;

    /**
     * Construct a new ServerConnection.
     * 
     * @param parent
     * @param ha
     * @throws IOException
     */
    public ServerConnection(InetAddress peer, int port, String id) throws IOException {
        super(peer, port);

        this.id = id;
    }

    /**
     * Setup this server connection.
     */
    public void setup() {
        Log.d(TAG, "Setting up a new ServerConnection.");

    }

    /**
     * Handle an incoming message.
     * 
     * @param id
     */
    public void handleMsg(Msg msg) {
        Log.d(TAG, "Handle msg: " + msg.toString());
    }

    /**
     * Set the remote id.
     * 
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the remote id.
     * 
     * @return
     */
    public String getId() {
        return id;
    }
}
