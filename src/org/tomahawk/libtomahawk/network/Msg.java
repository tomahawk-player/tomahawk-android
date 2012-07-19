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
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.util.Log;

/**
 * This class Represents a Tomahawk Msg.
 */
public class Msg {

    private static String TAG = Msg.class.getName();

    /**
     * Represents all the different Msg types.
     */
    public static final byte RAW = 1;
    public static final byte JSON = 2;
    public static final byte PING = 32;
    public static final byte SETUP = -128;

    private String mPayload = null;
    private byte mFlags;

    public Msg() {
        mPayload = null;
        mFlags = 0;
    }

    /**
     * Construct a new Msg from the given payload and flags.
     */
    public Msg(String payload, byte flags) {
        this.mPayload = payload;
        this.mFlags = flags;
    }

    /**
     * Construct a new Msg from the given payload.
     * 
     * @param payload
     */
    public Msg(String payload) {
        this.mPayload = payload;
        this.mFlags = 0;
    }

    /**
     * Construct an empty Msg of length.
     * 
     * @param length
     * @param flag
     */
    public Msg(int length, byte flags) {
        mPayload = new String(new byte[length]);
        this.mFlags = flags;
    }

    /**
     * Write payload to SocketChannel.
     * 
     * @param socket
     * @return
     * @throws IOException
     */
    public void write(OutputStream out) throws IOException {// TODO:

        ByteBuffer buf = ByteBuffer.allocate(Msg.headerSize() + mPayload.length());
        buf.putInt(length());
        buf.put(mFlags);
        buf.put(mPayload.getBytes());

        Log.d(TAG, "Attempting to write payload: " + mPayload);
        out.write(buf.array());
    }

    /**
     * Returns the size of a Tomahawk network packet.
     * 
     * @param msg
     * @return
     */
    public static final int msgSizeFromTwkPacket(byte[] msg) {
        byte[] header = new byte[Msg.headerSize() - 1];
        for (int i = 0; i < Msg.headerSize() - 1; ++i)
            header[i] = msg[i];
        return ByteBuffer.wrap(header).getInt();
    }

    /**
     * Returns the flag from a Tomahawk network packet.
     * 
     * @param msg
     * @return
     */
    public static final byte flagFromTwkPacket(byte[] msg) {
        return msg[Msg.headerSize() - 1];
    }

    /**
     * Returns the body of a Tomahawk network packet.
     * 
     * @param msg
     * @return
     */
    public static final byte[] bodyFromTwkPacket(byte[] msg) {
        byte[] body = new byte[msg.length - Msg.headerSize()];
        for (int i = 0, j = Msg.headerSize(); j < msg.length; ++i, j++)
            body[i] = msg[j];
        return body;
    }

    /**
     * Return length of payload.
     * 
     * @return
     */
    public int length() {
        if (mPayload != null)
            return mPayload.length();
        return 0;
    }

    /**
     * Return size of header.
     * 
     * sizeof(4) => header. sizeof(1) => flags.
     * 
     * @return
     */
    public static int headerSize() {
        return 4 + 1;
    }

    /**
     * Construct a new empty Msg using the msgHeader.
     * 
     * @param msgHeader
     * @return
     */
    public static Msg begin(byte[] msgHeader) {
        return new Msg(msgSizeFromTwkPacket(msgHeader), flagFromTwkPacket(msgHeader));
    }

    /**
     * Tests whether this Msg is carrying a null payload.
     * 
     * @return
     */
    public boolean isNull() {
        return mPayload == null;
    }

    /**
     * Clears this Msg's payload and flags.
     */
    public void clear() {
        mPayload = null;
        mFlags = 0;
    }

    /**
     * Pretty string representation a Msg object.
     */
    @Override
    public String toString() {
        return "Msg {type/" + mFlags + "} - " + mPayload;
    }

    /**
     * Get the payload for this Msg.
     * 
     * @return Data this message is carrying.
     */
    public String getPayload() {
        return mPayload;
    }

    /**
     * Set a flag.
     * 
     * @param flag
     */
    public void setFlag(int flag) {
        this.mFlags |= flag;
    }

    /**
     * Return byte which carries this Msg's flags.
     * 
     * @return
     */
    public byte getFlags() {
        return mFlags;
    }

    /**
     * Tests this messages type.
     * 
     * ex: if (msg.is(Msg.JSON)) doSomething();
     * 
     * @param flags
     */
    public boolean is(int flags) {
        return ((this.mFlags & flags) == flags);
    }

    /**
     * Fills this Msg's payload with the given bytes.
     * 
     * @param bytes
     *            to fill the payload.
     */
    public void fill(byte[] bytes) {
        mPayload = new String(bytes);
    }
}
