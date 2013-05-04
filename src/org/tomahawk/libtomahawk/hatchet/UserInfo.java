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
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 04.05.13
 */
public class UserInfo implements Info {

    private final static String TAG = UserInfo.class.getName();

    public static final String PERSONINFO_KEY_ID = "Id";

    public static final String PERSONINFO_KEY_NAME = "Name";

    public static final String PERSONINFO_KEY_URL = "Url";

    public static final String PERSONINFO_KEY_FOLLOWS = "Follows";

    private String mId;

    private String mName;

    private String mUrl;

    private ArrayList<UserInfo> mFollows;

    @Override
    public void parseInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(PERSONINFO_KEY_ID)) {
                mId = rawInfo.getString(PERSONINFO_KEY_ID);
            }
            if (!rawInfo.isNull(PERSONINFO_KEY_NAME)) {
                mName = rawInfo.getString(PERSONINFO_KEY_NAME);
            }
            if (!rawInfo.isNull(PERSONINFO_KEY_URL)) {
                mUrl = rawInfo.getString(PERSONINFO_KEY_URL);
            }
            if (!rawInfo.isNull(PERSONINFO_KEY_FOLLOWS)) {
                JSONArray rawFollowsInfos = rawInfo
                        .getJSONArray(PlaylistInfo.PLAYLISTINFO_KEY_ENTRIES);
                mFollows = new ArrayList<UserInfo>();
                for (int i = 0; i < rawFollowsInfos.length(); i++) {
                    UserInfo userInfo = new UserInfo();
                    userInfo.parseInfo(rawFollowsInfos.getJSONObject(i));
                    mFollows.add(userInfo);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String getUrl() {
        return mUrl;
    }

    public ArrayList<UserInfo> getFollows() {
        return mFollows;
    }
}
