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

import java.util.Date;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 20.04.13
 */
public class TimeSpanInfo implements Info {

    private final static String TAG = TimeSpanInfo.class.getName();

    public static final String TIMESPANINFO_KEY_STARTSAT = "StartsAt";

    public static final String TIMESPANINFO_KEY_ENDSAT = "EndsAt";

    private Date mStartsAt;

    private Date mEndsAt;

    public TimeSpanInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(TIMESPANINFO_KEY_STARTSAT)) {
                mStartsAt = TomahawkUtils
                        .stringToDate(rawInfo.getString(TIMESPANINFO_KEY_STARTSAT));
            }
            if (!rawInfo.isNull(TIMESPANINFO_KEY_ENDSAT)) {
                mEndsAt = TomahawkUtils.stringToDate(rawInfo.getString(TIMESPANINFO_KEY_ENDSAT));
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public Date getStartsAt() {
        return mStartsAt;
    }

    public Date getEndsAt() {
        return mEndsAt;
    }
}
