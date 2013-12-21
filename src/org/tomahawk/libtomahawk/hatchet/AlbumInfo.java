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
import org.tomahawk.libtomahawk.utils.TomahawkUtils;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

public class AlbumInfo implements Info {

    private final static String TAG = AlbumInfo.class.getName();

    public static final String ALBUMINFO_KEY_ARTIST = "artist";

    public static final String ALBUMINFO_KEY_ID = "id";

    public static final String ALBUMINFO_KEY_IMAGES = "images";

    public static final String ALBUMINFO_KEY_LABELS = "labels";

    public static final String ALBUMINFO_KEY_LENGTH = "length";

    public static final String ALBUMINFO_KEY_NAME = "name";

    public static final String ALBUMINFO_KEY_NAMES = "names";

    public static final String ALBUMINFO_KEY_PRODUCERS = "producers";

    public static final String ALBUMINFO_KEY_RELEASEDATE = "releaseDate";

    public static final String ALBUMINFO_KEY_TRACKS = "tracks";

    public static final String ALBUMINFO_KEY_URL = "url";

    public static final String ALBUMINFO_KEY_WIKIABSTRACT = "wikiabstract";

    private String mArtist;

    private String mId;

    private ArrayList<String> mImages;

    //private ArrayList<Label> mLabels;

    private int mLength;

    private String mName;

    private ArrayList<String> mNames;

    //private ArrayList<Person> mProducers;

    private Date mReleaseDate;

    private ArrayList<String> mTracks;

    private String mUrl;

    private String mWikiAbstract;

    public AlbumInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(ALBUMINFO_KEY_ARTIST)) {
                mArtist = rawInfo.getString(ALBUMINFO_KEY_ARTIST);
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_ID)) {
                mId = rawInfo.getString(ALBUMINFO_KEY_ID);
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_IMAGES)) {
                JSONArray jsonArray = rawInfo.getJSONArray(ALBUMINFO_KEY_IMAGES);
                mImages = new ArrayList<String>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    mImages.add(jsonArray.getString(i));
                }
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_LABELS)) {
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_LENGTH)) {
                mLength = rawInfo.getInt(ALBUMINFO_KEY_LENGTH);
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_NAME)) {
                mName = rawInfo.getString(ALBUMINFO_KEY_NAME);
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_NAMES)) {
                JSONArray jsonArray = rawInfo.getJSONArray(ALBUMINFO_KEY_NAMES);
                mNames = new ArrayList<String>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    mNames.add(jsonArray.getString(i));
                }
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_PRODUCERS)) {
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_RELEASEDATE)) {
                mReleaseDate = TomahawkUtils
                        .stringToDate(rawInfo.getString(ALBUMINFO_KEY_RELEASEDATE));
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_TRACKS)) {
                JSONArray jsonArray = rawInfo.getJSONArray(ALBUMINFO_KEY_TRACKS);
                mTracks = new ArrayList<String>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    mTracks.add(jsonArray.getString(i));
                }
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_URL)) {
                mUrl = rawInfo.getString(ALBUMINFO_KEY_URL);
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_WIKIABSTRACT)) {
                mWikiAbstract = rawInfo.getString(ALBUMINFO_KEY_WIKIABSTRACT);
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public String getArtist() {
        return mArtist;
    }

    public String getId() {
        return mId;
    }

    public ArrayList<String> getImages() {
        return mImages;
    }

    public int getLength() {
        return mLength;
    }

    public String getName() {
        return mName;
    }

    public ArrayList<String> getNames() {
        return mNames;
    }

    public Date getReleaseDate() {
        return mReleaseDate;
    }

    public ArrayList<String> getTracks() {
        return mTracks;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getWikiAbstract() {
        return mWikiAbstract;
    }

}
