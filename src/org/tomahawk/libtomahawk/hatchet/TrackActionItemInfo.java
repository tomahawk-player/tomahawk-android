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
import org.tomahawk.libtomahawk.resolver.TomahawkUtils;

import android.util.Log;

import java.util.Date;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 04.05.13
 */
public class TrackActionItemInfo implements Info {

    private final static String TAG = TrackActionItemInfo.class.getName();

    public static final String TRACKACTIONITEMINFO_KEY_PLAYBACKLOGS = "PlaybackLogs";

    public static final String TRACKACTIONITEMINFO_KEY_LOVES = "Loves";

    public static final String TRACKACTIONITEMINFO_KEY_TIMESTAMP = "TimeStamp";

    public static final String TRACKACTIONITEMINFO_KEY_TRACK = "Track";

    private Date mTimeStamp;

    private TrackInfo mTrack;

    @Override
    public void parseInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(TRACKACTIONITEMINFO_KEY_TIMESTAMP)) {
                mTimeStamp = TomahawkUtils
                        .stringToDate(rawInfo.getString(TRACKACTIONITEMINFO_KEY_TIMESTAMP));
            }
            if (!rawInfo.isNull(TRACKACTIONITEMINFO_KEY_TRACK)) {
                JSONObject rawTrackInfo = rawInfo.getJSONObject(TRACKACTIONITEMINFO_KEY_TRACK);
                mTrack = new TrackInfo();
                mTrack.parseInfo(rawTrackInfo);
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public Date getTimeStamp() {
        return mTimeStamp;
    }

    public TrackInfo getTrack() {
        return mTrack;
    }
}
