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
import java.util.List;
import java.util.Map;

public class HatchetUserInfo extends Mappable {

    public String about;

    public int followCount = -1;

    public int followersCount = -1;

    public List<String> images;

    public Map<String, String> links;

    public String name;

    public String nowplaying;

    public Date nowplayingtimestamp;

    public int totalPlays = -1;

    public String url;

    public boolean isFull;

    public HatchetUserInfo() {
    }
}
