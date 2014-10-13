/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.libtomahawk.authentication.models;

public class HatchetAuthResponse {

    public String access_token;

    public String canonical_username;

    public String email;

    public String error;

    public String error_description;

    public String error_uri;

    public long expires_in;

    public String message;

    public String passwordresettoken;

    public String refresh_token;

    public long refresh_token_expires_in;

    public String socialaccesstoken;

    public long socialaccesstokenexpiration;

    public String socialaccesstokensecret;

    public String socialauthurl;

    public String token_type;

    public boolean verified;

    public HatchetAuthResponse() {
    }
}
