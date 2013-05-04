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
public class ArtistInfo implements Info {

    private final static String TAG = ArtistInfo.class.getName();

    public static final String ARTISTINFO_KEY_DISAMBIGUATION = "Disambiguation";

    public static final String ARTISTINFO_KEY_ID = "Id";

    public static final String ARTISTINFO_KEY_IMAGES = "Images";

    public static final String ARTISTINFO_KEY_MEMBERS = "Members";

    public static final String ARTISTINFO_KEY_NAME = "Name";

    public static final String ARTISTINFO_KEY_NAMES = "Names";

    public static final String ARTISTINFO_KEY_RESOURCES = "Resources";

    public static final String ARTISTINFO_KEY_URL = "Url";

    public static final String ARTISTINFO_KEY_WIKIABSTRACT = "WikiAbstract";

    private String mDisambiguation;

    private String mId;

    private ArrayList<ImageInfo> mImages;

    private ArrayList<PersonInfo> mMembers;

    private String mName;

    private ArrayList<String> mNames;

    private ArrayList<ResourceInfo> mResources;

    private String mUrl;

    private String mWikiAbstract;

    public void parseInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(ARTISTINFO_KEY_DISAMBIGUATION)) {
                mDisambiguation = rawInfo.getString(ARTISTINFO_KEY_DISAMBIGUATION);
            }
            if (!rawInfo.isNull(ARTISTINFO_KEY_ID)) {
                mId = rawInfo.getString(ARTISTINFO_KEY_ID);
            }
            if (!rawInfo.isNull(ARTISTINFO_KEY_IMAGES)) {
                JSONArray rawImageInfos = rawInfo.getJSONArray(ARTISTINFO_KEY_IMAGES);
                mImages = new ArrayList<ImageInfo>();
                for (int i = 0; i < rawImageInfos.length(); i++) {
                    ImageInfo imageInfo = new ImageInfo();
                    imageInfo.parseInfo(rawImageInfos.getJSONObject(i));
                    mImages.add(imageInfo);
                }
            }
            if (!rawInfo.isNull(ARTISTINFO_KEY_MEMBERS)) {
                JSONArray rawResourceInfos = rawInfo.getJSONArray(ARTISTINFO_KEY_MEMBERS);
                mMembers = new ArrayList<PersonInfo>();
                for (int i = 0; i < rawResourceInfos.length(); i++) {
                    PersonInfo personInfo = new PersonInfo();
                    personInfo.parseInfo(rawResourceInfos.getJSONObject(i));
                    mMembers.add(personInfo);
                }
            }
            if (!rawInfo.isNull(ARTISTINFO_KEY_NAME)) {
                mName = rawInfo.getString(ARTISTINFO_KEY_NAME);
            }
            if (!rawInfo.isNull(ARTISTINFO_KEY_NAMES)) {
                JSONArray rawNameInfos = rawInfo.getJSONArray(ARTISTINFO_KEY_NAMES);
                mNames = new ArrayList<String>();
                for (int i = 0; i < rawNameInfos.length(); i++) {
                    mNames.add(rawNameInfos.getString(i));
                }
            }
            if (!rawInfo.isNull(ARTISTINFO_KEY_RESOURCES)) {
                JSONArray rawResourceInfos = rawInfo.getJSONArray(ARTISTINFO_KEY_RESOURCES);
                mResources = new ArrayList<ResourceInfo>();
                for (int i = 0; i < rawResourceInfos.length(); i++) {
                    ResourceInfo resourceInfo = new ResourceInfo();
                    resourceInfo.parseInfo(rawResourceInfos.getJSONObject(i));
                    mResources.add(resourceInfo);
                }
            }
            if (!rawInfo.isNull(ARTISTINFO_KEY_URL)) {
                mUrl = rawInfo.getString(ARTISTINFO_KEY_URL);
            }
            if (!rawInfo.isNull(ARTISTINFO_KEY_WIKIABSTRACT)) {
                mWikiAbstract = rawInfo.getString(ARTISTINFO_KEY_WIKIABSTRACT);
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

    public ArrayList<PersonInfo> getMembers() {
        return mMembers;
    }

    public String getName() {
        return mName;
    }

    public ArrayList<String> getNames() {
        return mNames;
    }

    public ArrayList<ResourceInfo> getResources() {
        return mResources;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getWikiAbstract() {
        return mWikiAbstract;
    }
}
