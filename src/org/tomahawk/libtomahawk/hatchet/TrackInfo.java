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

import android.util.Log;

import java.util.ArrayList;

public class TrackInfo implements Info {

    private final static String TAG = TrackInfo.class.getName();

    public static final String TRACKINFO_KEY_ARTIST = "artist";

    public static final String TRACKINFO_KEY_DURATION = "duration";

    public static final String TRACKINFO_KEY_ID = "id";

    public static final String TRACKINFO_KEY_NAME = "name";

    public static final String TRACKINFO_KEY_NAMES = "names";

    public static final String TRACKINFO_KEY_PLAYS = "plays";

    public static final String TRACKINFO_KEY_URL = "url";

    private String mArtist;

    private int mDuration;

    private String mId;

    private String mName;

    private ArrayList<String> mNames;

    private int mPlays;

    private String mUrl;

    public TrackInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(TRACKINFO_KEY_ARTIST)) {
                mArtist = rawInfo.getString(TRACKINFO_KEY_ARTIST);
            }
            if (!rawInfo.isNull(TRACKINFO_KEY_DURATION)) {
                mDuration = rawInfo.getInt(TRACKINFO_KEY_DURATION);
            }
            if (!rawInfo.isNull(TRACKINFO_KEY_ID)) {
                mId = rawInfo.getString(TRACKINFO_KEY_ID);
            }
            if (!rawInfo.isNull(TRACKINFO_KEY_NAME)) {
                mName = rawInfo.getString(TRACKINFO_KEY_NAME);
            }
            if (!rawInfo.isNull(TRACKINFO_KEY_NAMES)) {
                JSONArray rawNameInfos = rawInfo.getJSONArray(TRACKINFO_KEY_NAMES);
                mNames = new ArrayList<String>();
                for (int i = 0; i < rawNameInfos.length(); i++) {
                    mNames.add(rawNameInfos.getString(i));
                }
            }
            if (!rawInfo.isNull(TRACKINFO_KEY_PLAYS)) {
                mPlays = rawInfo.getInt(TRACKINFO_KEY_PLAYS);
            }
            if (!rawInfo.isNull(TRACKINFO_KEY_URL)) {
                mUrl = rawInfo.getString(TRACKINFO_KEY_URL);
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public String getArtist() {
        return mArtist;
    }

    public int getDuration() {
        return mDuration;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public ArrayList<String> getNames() {
        return mNames;
    }

    public int getPlays() {
        return mPlays;
    }

    public String getUrl() {
        return mUrl;
    }
}
