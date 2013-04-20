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
package org.tomahawk.libtomahawk.hatchet;

import java.util.ArrayList;
import java.util.Date;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 20.04.13
 */
public class PersonInfo {

    private String mDisambiguation;

    private String mId;

    private ArrayList<ImageInfo> mImages;

    private String mName;

    private ArrayList<String> mNames;

    private TimeSpanInfo mLifeSpan;

    private Date mStartsAt;

    private Date mEndsAt;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public Date getStartsAt() {
        return mStartsAt;
    }

    public void setStartsAt(Date startsAt) {
        mStartsAt = startsAt;
    }

    public Date getEndsAt() {
        return mEndsAt;
    }

    public void setEndsAt(Date endsAt) {
        mEndsAt = endsAt;
    }

}
