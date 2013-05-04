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
import org.tomahawk.libtomahawk.resolver.TomahawkUtils;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 20.04.13
 */
public class PlaylistInfo implements Info {

    private final static String TAG = PlaylistInfo.class.getName();

    public static final String PLAYLISTINFO_KEY_PLAYLISTS = "Playlists";

    public static final String PLAYLISTINFO_KEY_CREATED = "Created";

    public static final String PLAYLISTINFO_KEY_CURRENTREVISION = "CurrentRevision";

    public static final String PLAYLISTINFO_KEY_ENTRIES = "Entries";

    public static final String PLAYLISTINFO_KEY_ID = "Id";

    public static final String PLAYLISTINFO_KEY_REVISIONS = "Revisions";

    public static final String PLAYLISTINFO_KEY_TITLE = "Title";

    private Date mCreated;

    private String mCurrentRevision;

    private ArrayList<EntryInfo> mEntries;

    private String mId;

    private ArrayList<String> mRevisions;

    private String mTitle;

    @Override
    public void parseInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_CREATED)) {
                mCreated = TomahawkUtils.stringToDate(rawInfo.getString(PLAYLISTINFO_KEY_CREATED));
            }
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_CURRENTREVISION)) {
                mCurrentRevision = rawInfo.getString(PLAYLISTINFO_KEY_CURRENTREVISION);
            }
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_ENTRIES)) {
                JSONArray rawEntryInfos = rawInfo.getJSONArray(PLAYLISTINFO_KEY_ENTRIES);
                mEntries = new ArrayList<EntryInfo>();
                for (int i = 0; i < rawEntryInfos.length(); i++) {
                    EntryInfo entryInfo = new EntryInfo();
                    entryInfo.parseInfo(rawEntryInfos.getJSONObject(i));
                    mEntries.add(entryInfo);
                }
            }
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_ID)) {
                mId = rawInfo.getString(PLAYLISTINFO_KEY_ID);
            }
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_REVISIONS)) {

            }
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_TITLE)) {
                mTitle = rawInfo.getString(PLAYLISTINFO_KEY_TITLE);
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public Date getCreated() {
        return mCreated;
    }

    public String getCurrentRevision() {
        return mCurrentRevision;
    }

    public ArrayList<EntryInfo> getEntries() {
        return mEntries;
    }

    public String getId() {
        return mId;
    }

    public ArrayList<String> getRevisions() {
        return mRevisions;
    }

    public String getTitle() {
        return mTitle;
    }
}
