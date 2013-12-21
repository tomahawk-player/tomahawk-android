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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class PlaylistInfo implements Info {

    private final static String TAG = PlaylistInfo.class.getName();

    public static final String PLAYLISTINFO_KEY_ID = "id";

    public static final String PLAYLISTINFO_KEY_TITLE = "title";

    public static final String PLAYLISTINFO_KEY_CREATED = "created";

    public static final String PLAYLISTINFO_KEY_CURRENTREVISION = "currentrevision";

    public static final String PLAYLISTINFO_KEY_USER = "user";

    public static final String PLAYLISTINFO_KEY_LINKS = "links";

    private String mId;

    private String mTitle;

    private Date mCreated;

    private String mCurrentRevision;

    private String mUser;

    private HashMap<String, String> mLinks;

    public PlaylistInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_ID)) {
                mId = rawInfo.getString(PLAYLISTINFO_KEY_ID);
            }
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_TITLE)) {
                mTitle = rawInfo.getString(PLAYLISTINFO_KEY_TITLE);
            }
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_CREATED)) {
                mCreated = TomahawkUtils.stringToDate(rawInfo.getString(PLAYLISTINFO_KEY_CREATED));
            }
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_CURRENTREVISION)) {
                mCurrentRevision = rawInfo.getString(PLAYLISTINFO_KEY_CURRENTREVISION);
            }
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_USER)) {
                mUser = rawInfo.getString(PLAYLISTINFO_KEY_USER);
            }
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_LINKS)) {
                JSONObject links = rawInfo.getJSONObject(PLAYLISTINFO_KEY_LINKS);
                Iterator<?> keys = links.keys();
                mLinks = new HashMap<String, String>();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    mLinks.put(key, (String) links.get(key));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public String getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public Date getCreated() {
        return mCreated;
    }

    public String getCurrentRevision() {
        return mCurrentRevision;
    }

    public String getUser() {
        return mUser;
    }

    public HashMap<String, String> getLinks() {
        return mLinks;
    }
}
