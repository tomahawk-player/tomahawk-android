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
package org.tomahawk.libtomahawk;

import java.util.ArrayList;
import java.util.HashMap;

import android.database.Cursor;

/**
 * Class which represents a Tomahawk Album.
 */
public class Album {

    private HashMap<Long, Track> mTracks;

    private long mId;
    private String mName;
    private String mAlbumArt;
    private String mFirstYear;
    private String mLastYear;

    /**
     * Construct a new Album from the id
     * 
     * @param id
     */
    public Album(long id) {
        setId(id);

        mTracks = new HashMap<Long, Track>();
    }

    /**
     * Populate the track from the Cursor.
     * 
     * @param cursor
     */
    public void populate(Cursor cursor) {
        setId(cursor.getLong(0)); // MediaStore.Audio.Albums._ID
        setName(cursor.getString(1)); // MediaStore.Audio.Albums.ALBUM,
        setAlbumArt(cursor.getString(2)); // MediaStore.Audio.Albums.ALBUM_ART
        setFirstYear(cursor.getString(3)); // MediaStore.Audio.Albums.FIRST_YEAR
        setLastYear(cursor.getString(4)); // MediaStore.Audio.Albums.LAST_YEAR
    }

    /**
     * Return a the name of this Album.
     */
    @Override
    public String toString() {
        return mName;
    }

    /**
     * Add a Track to this Album.
     * 
     * @param track
     */
    public void addTrack(Track track) {
        mTracks.put(track.getId(), track);
    }

    /**
     * Get a list of all Tracks from this Album.
     * 
     * @return
     */
    public ArrayList<Track> getTracks() {
        return new ArrayList<Track>(mTracks.values());
    }

    /**
     * Return the Album id.
     * 
     * @return
     */
    public long getId() {
        return mId;
    }

    /**
     * Set the Album id.
     * 
     * @param id
     */
    public void setId(long id) {
        this.mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getAlbumArt() {
        return mAlbumArt;
    }

    public void setAlbumArt(String albumArt) {
        mAlbumArt = albumArt;
    }

    public String getFirstYear() {
        return mFirstYear;
    }

    public void setFirstYear(String firstYear) {
        mFirstYear = firstYear;
    }

    public String getLastYear() {
        return mLastYear;
    }

    public void setLastYear(String lastYear) {
        mLastYear = lastYear;
    }
}
