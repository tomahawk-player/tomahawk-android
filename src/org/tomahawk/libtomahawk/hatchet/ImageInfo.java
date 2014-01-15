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

    public static final String IMAGEINFO_KEY_HEIGHT = "height";

    public static final String IMAGEINFO_KEY_ID = "id";

    public static final String IMAGEINFO_KEY_SQUAREURL = "squareurl";

    public static final String IMAGEINFO_KEY_TYPE = "type";

    public static final String IMAGEINFO_KEY_URL = "url";

    public static final String IMAGEINFO_KEY_WIDTH = "width";

    private int mHeight;

    private String mId;

    private String mSquareUrl;

    private String mType;

    private String mUrl;

    private int mWidth;

    public ImageInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(IMAGEINFO_KEY_HEIGHT)) {
                mHeight = rawInfo.getInt(IMAGEINFO_KEY_HEIGHT);
            }
            if (!rawInfo.isNull(IMAGEINFO_KEY_ID)) {
                mId = rawInfo.getString(IMAGEINFO_KEY_ID);
            }
            if (!rawInfo.isNull(IMAGEINFO_KEY_SQUAREURL)) {
                mSquareUrl = rawInfo.getString(IMAGEINFO_KEY_SQUAREURL);
            }
            if (!rawInfo.isNull(IMAGEINFO_KEY_TYPE)) {
                mType = rawInfo.getString(IMAGEINFO_KEY_TYPE);
            }
            if (!rawInfo.isNull(IMAGEINFO_KEY_URL)) {
                mUrl = rawInfo.getString(IMAGEINFO_KEY_URL);
            }
            if (!rawInfo.isNull(IMAGEINFO_KEY_WIDTH)) {
                mWidth = rawInfo.getInt(IMAGEINFO_KEY_WIDTH);
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public int getHeight() {
        return mHeight;
    }

    public String getId() {
        return mId;
    }

    public String getSquareUrl() {
        return mSquareUrl;
    }

    public String getType() {
        return mType;
    }

    public String getUrl() {
        return mUrl;
    }

    public int getWidth() {
        return mWidth;
    }
}
