/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
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
package org.tomahawk.libtomahawk.infosystem.hatchet.models;

import java.util.Date;

public class HatchetSocialAction extends Mappable {

    public String action;

    public String album;

    public String albumString;

    public String artist;

    public String artistString;

    public Date date;

    public Date timestamp;

    public String playlist;

    public String target;

    public String track;

    public String trackString;

    public String type;

    public String user;

    public HatchetSocialAction() {
    }
}
