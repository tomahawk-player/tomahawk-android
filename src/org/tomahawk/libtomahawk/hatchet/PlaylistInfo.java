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
public class PlaylistInfo {

    public static String PLAYLISTINFO_KEY_CREATED = "Created";

    public static String PLAYLISTINFO_KEY_CURRENTREVISION = "CurrentRevision";

    public static String PLAYLISTINFO_KEY_ENTRIES = "Entries";

    public static String PLAYLISTINFO_KEY_ID = "Id";

    public static String PLAYLISTINFO_KEY_REVISIONS = "Revisions";

    public static String PLAYLISTINFO_KEY_TITLE = "Title";

    private Date mCreated;

    private String mCurrentRevision;

    private ArrayList<EntryInfo> mEntries;

    private String mId;

    private ArrayList<String> mRevisions;

    private String mTitle;
}
