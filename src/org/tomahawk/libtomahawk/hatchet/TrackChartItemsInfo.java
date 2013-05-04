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

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 20.04.13
 */
public class TrackChartItemsInfo implements Info {

    private final static String TAG = TrackChartItemsInfo.class.getName();

    public static final String TRACKCHARTITEMINFO_KEY_TRACKCHARTITEMS = "TrackChartItems";

    private ArrayList<TrackChartItemInfo> mTrackChartItems;

    @Override
    public void parseInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(TRACKCHARTITEMINFO_KEY_TRACKCHARTITEMS)) {
                JSONArray rawTrackChartItemInfos = rawInfo
                        .getJSONArray(TRACKCHARTITEMINFO_KEY_TRACKCHARTITEMS);
                mTrackChartItems = new ArrayList<TrackChartItemInfo>();
                for (int i = 0; i < rawTrackChartItemInfos.length(); i++) {
                    TrackChartItemInfo trackChartItemInfo = new TrackChartItemInfo();
                    trackChartItemInfo.parseInfo(rawTrackChartItemInfos.getJSONObject(i));
                    mTrackChartItems.add(trackChartItemInfo);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public ArrayList<TrackChartItemInfo> getTrackChartItems() {
        return mTrackChartItems;
    }

}
