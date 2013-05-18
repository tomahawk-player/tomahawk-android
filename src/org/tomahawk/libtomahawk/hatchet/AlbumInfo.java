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

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 20.04.13
 */
public class AlbumInfo implements Info {

    private final static String TAG = AlbumInfo.class.getName();

    public static final String ALBUMINFO_KEY_ARTIST = "Artist";

    public static final String ALBUMINFO_KEY_ID = "Id";

    public static final String ALBUMINFO_KEY_IMAGES = "Images";

    public static final String ALBUMINFO_KEY_LABELS = "Labels";

    public static final String ALBUMINFO_KEY_LENGTH = "Length";

    public static final String ALBUMINFO_KEY_NAME = "Name";

    public static final String ALBUMINFO_KEY_NAMES = "Names";

    public static final String ALBUMINFO_KEY_PRODUCERS = "Producers";

    public static final String ALBUMINFO_KEY_RELEASEDATE = "ReleaseDate";

    public static final String ALBUMINFO_KEY_TRACKS = "Tracks";

    public static final String ALBUMINFO_KEY_URL = "Url";

    public static final String ALBUMINFO_KEY_WIKIABSTRACT = "WikiAbstract";

    private ArtistInfo mArtist;

    private String mId;

    private ArrayList<ImageInfo> mImages;

    private ArrayList<String> mLabels;

    private int mLength;

    private String mName;

    private ArrayList<String> mNames;

    private ArrayList<PersonInfo> mProducers;

    private Date mReleaseDate;

    private ArrayList<TrackInfo> mTracks;

    private String mUrl;

    private String mWikiAbstract;

    @Override
    public void parseInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(ALBUMINFO_KEY_ARTIST)) {
                JSONObject rawArtistInfo = rawInfo.getJSONObject(ALBUMINFO_KEY_ARTIST);
                mArtist = new ArtistInfo();
                mArtist.parseInfo(rawArtistInfo);
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_ID)) {
                mId = rawInfo.getString(ALBUMINFO_KEY_ID);
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_IMAGES)) {
                JSONArray rawImageInfos = rawInfo.getJSONArray(ALBUMINFO_KEY_IMAGES);
                mImages = new ArrayList<ImageInfo>();
                for (int i = 0; i < rawImageInfos.length(); i++) {
                    ImageInfo imageInfo = new ImageInfo();
                    imageInfo.parseInfo(rawImageInfos.getJSONObject(i));
                    mImages.add(imageInfo);
                }
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_LABELS)) {
                JSONArray rawImageInfos = rawInfo.getJSONArray(ALBUMINFO_KEY_LABELS);
                mLabels = new ArrayList<String>();
                for (int i = 0; i < rawImageInfos.length(); i++) {
                    mLabels.add(rawImageInfos.getString(i));
                }
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_LENGTH)) {
                mLength = rawInfo.getInt(ALBUMINFO_KEY_LENGTH);
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_NAME)) {
                mName = rawInfo.getString(ALBUMINFO_KEY_NAME);
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_NAMES)) {
                JSONArray rawImageInfos = rawInfo.getJSONArray(ALBUMINFO_KEY_NAMES);
                mNames = new ArrayList<String>();
                for (int i = 0; i < rawImageInfos.length(); i++) {
                    mNames.add(rawImageInfos.getString(i));
                }
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_PRODUCERS)) {
                JSONArray rawProducerInfos = rawInfo.getJSONArray(ALBUMINFO_KEY_PRODUCERS);
                mProducers = new ArrayList<PersonInfo>();
                for (int i = 0; i < rawProducerInfos.length(); i++) {
                    PersonInfo personInfo = new PersonInfo();
                    personInfo.parseInfo(rawProducerInfos.getJSONObject(i));
                    mProducers.add(personInfo);
                }
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_RELEASEDATE)) {
                mReleaseDate = TomahawkUtils
                        .stringToDate(rawInfo.getString(ALBUMINFO_KEY_RELEASEDATE));
            }
            if (!rawInfo.isNull(ALBUMINFO_KEY_TRACKS)) {
                JSONArray rawTrackInfos = rawInfo.getJSONArray(ALBUMINFO_KEY_TRACKS);
                mTracks = new ArrayList<TrackInfo>();
                for (int i = 0; i < rawTrackInfos.length(); i++) {
                    TrackInfo trackInfo = new TrackInfo();
                    trackInfo.parseInfo(rawTrackInfos.getJSONObject(i));
                    mTracks.add(trackInfo);
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

    public ArtistInfo getArtist() {
        return mArtist;
    }

    public String getId() {
        return mId;
    }

    public ArrayList<ImageInfo> getImages() {
        return mImages;
    }

    public ArrayList<String> getLabels() {
        return mLabels;
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

    public ArrayList<PersonInfo> getProducers() {
        return mProducers;
    }

    public Date getReleaseDate() {
        return mReleaseDate;
    }

    public ArrayList<TrackInfo> getTracks() {
        return mTracks;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getWikiAbstract() {
        return mWikiAbstract;
    }

}
