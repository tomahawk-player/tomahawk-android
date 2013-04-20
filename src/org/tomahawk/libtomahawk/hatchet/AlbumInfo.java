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

import java.util.ArrayList;
import java.util.Date;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 20.04.13
 */
public class AlbumInfo {

    public static String ALBUMINFO_KEY_ARTIST = "Artist";

    public static String ALBUMINFO_KEY_ID = "Id";

    public static String ALBUMINFO_KEY_IMAGES = "Images";

    public static String ALBUMINFO_KEY_LABELS = "Labels";

    public static String ALBUMINFO_KEY_LENGTH = "Length";

    public static String ALBUMINFO_KEY_NAME = "Name";

    public static String ALBUMINFO_KEY_NAMES = "Names";

    public static String ALBUMINFO_KEY_PRODUCERS = "Producers";

    public static String ALBUMINFO_KEY_RELEASEDATE = "ReleaseDate";

    public static String ALBUMINFO_KEY_TRACKS = "Tracks";

    public static String ALBUMINFO_KEY_URL = "Url";

    public static String ALBUMINFO_KEY_WIKIABSTRACT = "WikiAbstract";

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

    public ArtistInfo getArtist() {
        return mArtist;
    }

    public void setArtist(ArtistInfo artist) {
        mArtist = artist;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public ArrayList<ImageInfo> getImages() {
        return mImages;
    }

    public void setImages(ArrayList<ImageInfo> images) {
        mImages = images;
    }

    public ArrayList<String> getLabels() {
        return mLabels;
    }

    public void setLabels(ArrayList<String> labels) {
        mLabels = labels;
    }

    public int getLength() {
        return mLength;
    }

    public void setLength(int length) {
        mLength = length;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public ArrayList<String> getNames() {
        return mNames;
    }

    public void setNames(ArrayList<String> names) {
        mNames = names;
    }

    public ArrayList<PersonInfo> getProducers() {
        return mProducers;
    }

    public void setProducers(ArrayList<PersonInfo> producers) {
        mProducers = producers;
    }

    public Date getReleaseDate() {
        return mReleaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        mReleaseDate = releaseDate;
    }

    public ArrayList<TrackInfo> getTracks() {
        return mTracks;
    }

    public void setTracks(ArrayList<TrackInfo> tracks) {
        mTracks = tracks;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getWikiAbstract() {
        return mWikiAbstract;
    }

    public void setWikiAbstract(String wikiAbstract) {
        mWikiAbstract = wikiAbstract;
    }

}
