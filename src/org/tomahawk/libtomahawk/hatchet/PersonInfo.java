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
public class PersonInfo implements Info {

    private final static String TAG = PersonInfo.class.getName();

    public static final String PERSONINFO_KEY_DISAMBIGUATION = "Disambiguation";

    public static final String PERSONINFO_KEY_ID = "Id";

    public static final String PERSONINFO_KEY_IMAGES = "Images";

    public static final String PERSONINFO_KEY_LIFESPAN = "LifeSpan";

    public static final String PERSONINFO_KEY_MEMBERSHIPS = "Memberships";

    public static final String PERSONINFO_KEY_NAME = "Name";

    public static final String PERSONINFO_KEY_NAMES = "Names";

    public static final String PERSONINFO_KEY_URL = "Url";

    private String mDisambiguation;

    private String mId;

    private ArrayList<ImageInfo> mImages;

    private TimeSpanInfo mLifeSpan;

    private ArrayList<MemberInfo> mMemberships;

    private String mName;

    private ArrayList<String> mNames;

    private String mUrl;

    public PersonInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(PERSONINFO_KEY_DISAMBIGUATION)) {
                mDisambiguation = rawInfo.getString(PERSONINFO_KEY_DISAMBIGUATION);
            }
            if (!rawInfo.isNull(PERSONINFO_KEY_ID)) {
                mId = rawInfo.getString(PERSONINFO_KEY_ID);
            }
            if (!rawInfo.isNull(PERSONINFO_KEY_IMAGES)) {
                JSONArray rawImageInfos = rawInfo.getJSONArray(PERSONINFO_KEY_IMAGES);
                mImages = new ArrayList<ImageInfo>();
                for (int i = 0; i < rawImageInfos.length(); i++) {
                    ImageInfo imageInfo = new ImageInfo(rawImageInfos.getJSONObject(i));
                    mImages.add(imageInfo);
                }
            }
            if (!rawInfo.isNull(PERSONINFO_KEY_LIFESPAN)) {
                mLifeSpan = new TimeSpanInfo(rawInfo.getJSONObject(PERSONINFO_KEY_LIFESPAN));
            }
            if (!rawInfo.isNull(PERSONINFO_KEY_MEMBERSHIPS)) {
                JSONArray rawMembershipInfos = rawInfo.getJSONArray(PERSONINFO_KEY_MEMBERSHIPS);
                mMemberships = new ArrayList<MemberInfo>();
                for (int i = 0; i < rawMembershipInfos.length(); i++) {
                    MemberInfo memberInfo = new MemberInfo(rawMembershipInfos.getJSONObject(i));
                    mMemberships.add(memberInfo);
                }
            }
            if (!rawInfo.isNull(PERSONINFO_KEY_NAME)) {
                mName = rawInfo.getString(PERSONINFO_KEY_NAME);
            }
            if (!rawInfo.isNull(PERSONINFO_KEY_NAMES)) {
                JSONArray rawNameInfos = rawInfo.getJSONArray(PERSONINFO_KEY_NAMES);
                mNames = new ArrayList<String>();
                for (int i = 0; i < rawNameInfos.length(); i++) {
                    mNames.add(rawNameInfos.getString(i));
                }
            }
            if (!rawInfo.isNull(PERSONINFO_KEY_URL)) {
                mUrl = rawInfo.getString(PERSONINFO_KEY_URL);
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public String getDisambiguation() {
        return mDisambiguation;
    }

    public String getId() {
        return mId;
    }

    public ArrayList<ImageInfo> getImages() {
        return mImages;
    }

    public TimeSpanInfo getLifeSpan() {
        return mLifeSpan;
    }

    public ArrayList<MemberInfo> getMemberships() {
        return mMemberships;
    }

    public String getName() {
        return mName;
    }

    public ArrayList<String> getNames() {
        return mNames;
    }

    public String getUrl() {
        return mUrl;
    }
}
