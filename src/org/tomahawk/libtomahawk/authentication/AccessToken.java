/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.libtomahawk.authentication;

import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class AccessToken {

    String token;

    String remotehost;

    String localhost;

    String type;

    int port;

    int expiration;

    AccessToken(String token, String remotehost, String type, int port, int expiration) {
        this.token = token;
        this.remotehost = remotehost;
        this.type = type;
        this.port = port;
        this.expiration = expiration;

        try {
            localhost = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {

            WifiManager wifiMan = (WifiManager) TomahawkApp.getContext()
                    .getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInf = wifiMan.getConnectionInfo();

            localhost = Integer.toString(wifiInf.getIpAddress());
        }
    }
}