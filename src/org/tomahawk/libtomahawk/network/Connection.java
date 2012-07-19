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
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * This class represents a connection between two Tomahawk hosts.
 */
public abstract class Connection {

    private static final String TAG = Connection.class.getName();

    private String name = null;
    private String nodeid = null;

    private Socket socket = null;
    private Msg firstMessage = null;
    private Msg msg = null;
    private InetAddress host = null;
    private int port = TomahawkNetworkUtils.getDefaultTwkPort();

    public static final String PROTOVER = "4";

    private boolean pending = true;

    public abstract void setup();

    public abstract void handleMsg(Msg msg);


    /**
     * Constructs a new connection with the given details.
     */
    public Connection(InetAddress host, int port) {

        this.host = host;
        this.port = port;

        nodeid = null;
        setName(host.getHostName());

        msg = new Msg();

    }

    private void connectSocket() throws IOException {
        this.socket = new Socket(host, port);
        this.socket.setSoTimeout(600000);
    }

    /**
     * Read on this connection.
     * 
     * @param socket
     */
    public void start() {

        /**
         * This exception will be thrown whenever our socket fails, which means
         * we will have to catch it and appropriately re-initiate networking.
         * This could get tricky on a mobile app so I am going to leave it like
         * this until there is a proper solution in place.
         */
        try {
            connectSocket();
            Log.d(TAG, "Reading from Connection: " + getName());

            if (getName().length() == 0)
                setName(String.format("peer[%s]", socket.getInetAddress().toString()));
            checkACL();

        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

    }

    /**
     * Write current message to socket.
     * 
     * @param msg
     * @throws IOException
     */
    protected void sendMsg(Msg msg) {
        try {
            msg.write(socket.getOutputStream());
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Overloaded method. Create a new Msg and passes to sndMsg.
     * 
     * @param msg
     * @param type
     * @throws IOException
     * @throws JSONException
     */
    protected void sendMsg(String msg, byte flags) {
        try {
            sendMsg(new Msg((new JSONObject(msg)).toString(), flags));
        } catch (JSONException e) {
            Log.e(TAG, "Error sending msg: " + msg + " : " + e.toString());
        }
    }

    /**
     * Determine if a connection to this node already exists.
     * 
     * @param id
     * @return
     */
    public boolean connectedToSession(String id) {
        if (getNodeId().equals(id))
            return true;
        return false;
    }

    /**
     * Check access control lists to make sure connection is allowed.
     * 
     * @throws JSONException
     */
    protected void checkACL() throws IOException {

        if (nodeid == null) {
            doSetup();
            return;
        }

        if (TomahawkNetworkUtils.isIPWhitelisted(socket.getInetAddress())) {
            doSetup();
            return;
        }
    }

    /**
     * Setup the connection by determining whether inbound or outbound.
     * 
     * @throws JSONException
     */
    public void doSetup() throws IOException {
        if (isPending()) {
            sendMsg(getFirstMsg());
            setPending(false);
        } else
            sendMsg(Connection.PROTOVER, Msg.SETUP);

        while (socket.isConnected()) {
            readRead();
        }
    }

    /**
     * Ready read;
     * 
     * @throws IOException
     */
    protected void readRead() throws IOException {
        InputStream in = socket.getInputStream();

        if (msg.isNull()) {

            if (in.available() < Msg.headerSize())
                return;

            byte[] buffer = new byte[Msg.headerSize()];
            in.read(buffer);
            msg = Msg.begin(buffer);
        }

        if (in.available() < msg.length())
            return;

        byte[] buffer = new byte[msg.length()];
        in.read(buffer);
        msg.fill(buffer);
        Log.d(TAG, "Received message: " + new String(buffer));

        handleReadMsg();
        writePending();
    }

    // TODO
    protected void handleReadMsg() throws IOException {
        Log.d(TAG, "Logging Flags: " + Msg.SETUP);

        if (msg.is(Msg.SETUP) && msg.getPayload().equals(PROTOVER)) {
            sendMsg(new Msg("ok", Msg.SETUP));
            setup();
        } else {
            handleMsg(msg);
        }
        msg.clear();
    }

    /**
     * Returns whether a Connection has data to write.
     * 
     * @throws IOException
     */
    protected void writePending() throws IOException {
        if (!msg.isNull())
            sendMsg(msg);
    }

    /**
     * Returns whether this connection is connected.
     */
    public boolean isConnected() {
        return this.socket != null ? this.socket.isConnected() : false;
    }

    /**
     * Serialize this Map<String, String> into JSON (using Gson) and create a
     * new Msg. Then set as first message.
     * 
     * @param m
     */
    public void setFirstMsg(Map<String, String> m) {
        setFirstMsg(new Msg(new JSONObject(m).toString(), Msg.JSON));
    }

    /**
     * Set first message.
     * 
     * @param msg
     */
    protected void setFirstMsg(Msg msg) {
        this.firstMessage = msg;
    }

    /**
     * Returns firstMessage;
     * 
     * @return
     */
    public Msg getFirstMsg() {
        return firstMessage;
    }

    /**
     * Set the name of this connection.
     * 
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the name of this Connection.
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Return the InetAddresss of peer.
     * 
     * @return
     */
    public InetAddress getPeerInetAddress() {
        return socket.getInetAddress();
    }

    /**
     * Returns this object Msg.
     */
    public Msg getMsg() {
        return msg;
    }

    /**
     * Return the socket this connection is using.
     * 
     * @return
     */
    public Socket getSocket() {
        return this.socket;
    }

    /**
     * Set the socket this connection is using.
     * 
     * @param socket
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    /**
     * Get the remote port of this connection.
     * 
     * @return
     */
    public int getRemotePort() {
        return socket.getPort();
    }

    /**
     * Set the pending status of this connection.
     * 
     * Pending connections need to write asap.
     * 
     * @param pending
     */
    public void setPending(boolean pending) {
        this.pending = pending;
    }

    /**
     * Returns whether this connection is pending.
     * 
     * @return
     */
    public boolean isPending() {
        return pending;
    }

    /**
     * Returns the nodeid of this connection.
     * 
     * @return
     */
    public String getNodeId() {
        return nodeid;
    }

    /**
     * Set the nodeid for this connection.
     * 
     * @param nodeid
     */
    public void setNodeId(String nodeid) {
        this.nodeid = nodeid;
    }
}
