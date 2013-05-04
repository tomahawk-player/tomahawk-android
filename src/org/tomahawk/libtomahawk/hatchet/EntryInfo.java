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

import android.util.Log;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 20.04.13
 */
public class EntryInfo implements Info {

    private final static String TAG = EntryInfo.class.getName();

    public static String ENTRYINFO_KEY_ID = "Id";

    public static String ENTRYINFO_KEY_TRACK = "Track";

    private String mId;

    private TrackInfo mTrack;

    @Override
    public void parseInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(ENTRYINFO_KEY_ID)) {
                mId = rawInfo.getString(ENTRYINFO_KEY_ID);
            }
            if (!rawInfo.isNull(ENTRYINFO_KEY_TRACK)) {
                mTrack = new TrackInfo();
                mTrack.parseInfo(rawInfo.getJSONObject(ENTRYINFO_KEY_TRACK));
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public String getId() {
        return mId;
    }

    public TrackInfo getTrack() {
        return mTrack;
    }
}
