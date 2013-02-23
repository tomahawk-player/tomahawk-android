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
package org.tomahawk.tomahawk_android.test;

import org.json.JSONObject;
import org.tomahawk.libtomahawk.network.Msg;
import org.tomahawk.libtomahawk.network.TomahawkNetworkUtils;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class TomahawkTestData {

    public static String tstFirstMsg() {

        Map<String, String> m = new HashMap<String, String>();
        m.put("conntype", "accept-offer");
        m.put("key", "whitelist");
        m.put("port", Integer.toString(TomahawkNetworkUtils.getDefaultTwkPort()));
        m.put("nodeid", tstNodeId());

        return new JSONObject(m).toString();
    }

    public static String tstNodeId() {
        return "9f99f0e3-2c97-499e-8d1d-d806213656b3";
    }

    public static byte[] getTstTwkPacket(String msg) {

        ByteBuffer buf = ByteBuffer.allocate(Msg.headerSize() + msg.length());
        buf.putInt(msg.length());
        buf.put(Msg.JSON);
        buf.put(msg.getBytes());

        return buf.array();
    }
}
