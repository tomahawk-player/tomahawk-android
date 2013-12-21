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
public class TrackChartItemInfo implements Info {

    private final static String TAG = TrackChartItemInfo.class.getName();

    public static final String TRACKCHARTITEMINFO_KEY_TRACKCHARTITEMS = "TrackChartItems";

    public static final String TRACKCHARTITEMINFO_KEY_PLAYS = "Plays";

    public static final String TRACKCHARTITEMINFO_KEY_TRACK = "Track";

    private int mPlays;

    private TrackInfo mTrack;

    public void parseInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(TRACKCHARTITEMINFO_KEY_PLAYS)) {
                mPlays = rawInfo.getInt(TRACKCHARTITEMINFO_KEY_PLAYS);
            }
            if (!rawInfo.isNull(TRACKCHARTITEMINFO_KEY_TRACK)) {
                JSONObject rawTrackInfo = rawInfo.getJSONObject(TRACKCHARTITEMINFO_KEY_TRACK);
                mTrack = new TrackInfo(rawTrackInfo);
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public int getPlays() {
        return mPlays;
    }

    public TrackInfo getTrack() {
        return mTrack;
    }

}
