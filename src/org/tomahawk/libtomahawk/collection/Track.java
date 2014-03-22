/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
package org.tomahawk.libtomahawk.collection;

import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a {@link Track}.
 */
public class Track implements TomahawkListItem {

    private static ConcurrentHashMap<String, Track> sTracks
            = new ConcurrentHashMap<String, Track>();

    private String mName;

    private Album mAlbum;

    private Artist mArtist;

    private long mDuration;

    private int mYear;

    private int mAlbumPos;

    private int mDiscNumber;

    /**
     * Construct a new {@link Track}
     */
    private Track(String trackName, Album album, Artist artist) {
        if (trackName == null) {
            mName = "";
        } else {
            mName = trackName;
        }
        mAlbum = album;
        mArtist = artist;
    }

    /**
     * Returns the {@link Track} with the given id. If none exists in our static {@link
     * ConcurrentHashMap} yet, construct and add it.
     *
     * @return {@link Track} with the given id
     */
    public static Track get(String trackName, Album album, Artist artist) {
        if (artist == null) {
            artist = Artist.get("");
        }
        if (album == null) {
            album = Album.get("", artist);
        }
        Track track = new Track(trackName, album, artist);
        String key = TomahawkUtils.getCacheKey(track);
        if (!sTracks.containsKey(key)) {
            sTracks.put(key, track);
        }
        return sTracks.get(key);
    }

    /**
     * Get the {@link org.tomahawk.libtomahawk.collection.Track} by providing its cache key
     */
    public static Track getTrackByKey(String key) {
        return sTracks.get(key);
    }

    /**
     * @return the {@link Track}'s name
     */
    @Override
    public String toString() {
        return mName;
    }

    /**
     * @return the {@link Track}'s name
     */
    @Override
    public String getName() {
        return mName;
    }

    /**
     * @return the {@link Track}'s {@link Artist}
     */
    @Override
    public Artist getArtist() {
        return mArtist;
    }

    /**
     * @return the {@link Track}'s {@link Album}
     */
    @Override
    public Album getAlbum() {
        return mAlbum;
    }

    @Override
    public ArrayList<Query> getQueries(boolean onlyLocal) {
        return null;
    }

    @Override
    public ArrayList<Query> getQueries() {
        return null;
    }

    @Override
    public Image getImage() {
        if (mAlbum.getImage() != null && !TextUtils.isEmpty(mAlbum.getImage().getImagePath())) {
            return mAlbum.getImage();
        } else {
            return mArtist.getImage();
        }
    }

    /**
     * @return this {@link Track}'s duration
     */
    public long getDuration() {
        return mDuration;
    }

    /**
     * Set this {@link Track}'s duration
     */
    public void setDuration(long duration) {
        this.mDuration = duration;
    }

    /**
     * @return this {@link Track}'s track number
     */
    public int getAlbumPos() {
        return mAlbumPos;
    }

    /**
     * Set this {@link Track}'s track number
     */
    public void setAlbumPos(int albumPos) {
        this.mAlbumPos = albumPos;
    }

    /**
     * @return this {@link Track}'s year
     */
    public int getYear() {
        return mYear;
    }

    /**
     * Set this {@link Track}'s year
     */
    public void setYear(int year) {
        this.mYear = year;
    }

    /**
     * @return this {@link Track}'s disc number
     */
    public int getDiscNumber() {
        return mDiscNumber;
    }

    /**
     * Set this {@link Track}'s disc number
     */
    public void setDiscNumber(int discNumber) {
        mDiscNumber = discNumber;
    }

}
