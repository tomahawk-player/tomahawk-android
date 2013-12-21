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
public class ImageInfo implements Info {

    private final static String TAG = ImageInfo.class.getName();

    public static final String IMAGEINFO_KEY_WIDTH = "Width";

    public static final String IMAGEINFO_KEY_HEIGHT = "Height";

    public static final String IMAGEINFO_KEY_URL = "Url";

    private int mWidth;

    private int mHeight;

    private String mUrl;

    public void parseInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(IMAGEINFO_KEY_WIDTH)) {
                mWidth = rawInfo.getInt(IMAGEINFO_KEY_WIDTH);
            }
            if (!rawInfo.isNull(IMAGEINFO_KEY_HEIGHT)) {
                mHeight = rawInfo.getInt(IMAGEINFO_KEY_HEIGHT);
            }
            if (!rawInfo.isNull(IMAGEINFO_KEY_URL)) {
                mUrl = rawInfo.getString(IMAGEINFO_KEY_URL);
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public String getUrl() {
        return mUrl;
    }
}
