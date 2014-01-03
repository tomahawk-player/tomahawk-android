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

import org.json.JSONException;
import org.json.JSONObject;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class PlaylistEntryInfo implements Info {

    private final static String TAG = PlaylistEntryInfo.class.getName();

    public static final String PLAYLISTINFO_KEY_ID = "id";

    public static final String PLAYLISTINFO_KEY_TRACK = "track";

    public static final String PLAYLISTINFO_KEY_ALBUM = "album";

    private String mId;

    private String mTrack;

    private String mAlbum;

    public PlaylistEntryInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_ID)) {
                mId = rawInfo.getString(PLAYLISTINFO_KEY_ID);
            }
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_TRACK)) {
                mTrack = rawInfo.getString(PLAYLISTINFO_KEY_TRACK);
            }
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_ALBUM)) {
                mAlbum = rawInfo.getString(PLAYLISTINFO_KEY_ALBUM);
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public String getId() {
        return mId;
    }

    public String getTrack() {
        return mTrack;
    }

    public String getAlbum() {
        return mAlbum;
    }
}
