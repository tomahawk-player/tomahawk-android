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

public class ArtistInfo implements Info {

    private final static String TAG = ArtistInfo.class.getName();

    public static final String ARTISTINFO_KEY_ALBUMS = "albums";

    public static final String ARTISTINFO_KEY_DISAMBIGUATION = "disambiguation";

    public static final String ARTISTINFO_KEY_ID = "id";

    public static final String ARTISTINFO_KEY_IMAGES = "images";

    public static final String ARTISTINFO_KEY_LINKS = "links";

    public static final String ARTISTINFO_KEY_MEMBERS = "members";

    public static final String ARTISTINFO_KEY_NAME = "name";

    public static final String ARTISTINFO_KEY_NAMES = "names";

    public static final String ARTISTINFO_KEY_RESOURCES = "resources";

    public static final String ARTISTINFO_KEY_URL = "url";

    public static final String ARTISTINFO_KEY_WIKIABSTRACT = "wikiabstract";

    private ArrayList<String> mAlbums;

    private String mDisambiguation;

    private String mId;

    private ArrayList<String> mImages;

    private ArrayList<MemberInfo> mMembers;

    private String mName;

    private ArrayList<String> mNames;

    private ArrayList<ResourceInfo> mResources;

    private String mUrl;

    private String mWikiAbstract;

    public ArtistInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(ARTISTINFO_KEY_ALBUMS)) {
                JSONArray rawNameInfos = rawInfo.getJSONArray(ARTISTINFO_KEY_ALBUMS);
                mAlbums = new ArrayList<String>();
                for (int i = 0; i < rawNameInfos.length(); i++) {
                    mAlbums.add(rawNameInfos.getString(i));
                }
            }
            if (!rawInfo.isNull(ARTISTINFO_KEY_DISAMBIGUATION)) {
                mDisambiguation = rawInfo.getString(ARTISTINFO_KEY_DISAMBIGUATION);
            }
            if (!rawInfo.isNull(ARTISTINFO_KEY_ID)) {
                mId = rawInfo.getString(ARTISTINFO_KEY_ID);
            }
            if (!rawInfo.isNull(ARTISTINFO_KEY_IMAGES)) {
                JSONArray rawNameInfos = rawInfo.getJSONArray(ARTISTINFO_KEY_IMAGES);
                mImages = new ArrayList<String>();
                for (int i = 0; i < rawNameInfos.length(); i++) {
                    mImages.add(rawNameInfos.getString(i));
                }
            }
            if (!rawInfo.isNull(ARTISTINFO_KEY_MEMBERS)) {
                JSONArray rawResourceInfos = rawInfo.getJSONArray(ARTISTINFO_KEY_MEMBERS);
                mMembers = new ArrayList<MemberInfo>();
                for (int i = 0; i < rawResourceInfos.length(); i++) {
                    mMembers.add(new MemberInfo(rawResourceInfos.getJSONObject(i)));
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
                    mResources.add(new ResourceInfo(rawResourceInfos.getJSONObject(i)));
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

    public ArrayList<String> getAlbums() {
        return mAlbums;
    }

    public String getDisambiguation() {
        return mDisambiguation;
    }

    public String getId() {
        return mId;
    }

    public ArrayList<String> getImages() {
        return mImages;
    }

    public ArrayList<MemberInfo> getMembers() {
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
