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

import java.io.Serializable;

import android.database.Cursor;

public class Track implements Serializable {

    private static final long serialVersionUID = 1L;

    private long mId;

    /**
     * Path of file or URL.
     */
    private String mPath;
    private String mTitle;
    private Album mAlbum;
    private Artist mArtist;
    private long mDuration;
    private int mTrackNumber;

    public Track(long l) {
        setId(l);
    }

    public void populate(Cursor cursor) {
        setId(cursor.getLong(0)); // MediaStore.Audio.Media._ID,
        setPath(cursor.getString(1)); // MediaStore.Audio.Media.DATA,
        setTitle(cursor.getString(2)); // MediaStore.Audio.Media.TITLE,
        setDuration(cursor.getLong(3)); // MediaStore.Audio.Media.DURATION,
        setTrackNumber(cursor.getInt(4)); // MediaStore.Audio.Media.TRACK
    }

    @Override
    public String toString() {
        return getTitle();
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        this.mId = id;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        this.mPath = path;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public Album getAlbum() {
        return mAlbum;
    }

    public void setAlbum(Album album) {
        this.mAlbum = album;
    }

    public Artist getArtist() {
        return mArtist;
    }

    public void setArtist(Artist artist) {
        this.mArtist = artist;
    }

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(long duration) {
        this.mDuration = duration;
    }

    public int getTrackNumber() {
        return mTrackNumber;
    }

    public void setTrackNumber(int trackNumber) {
        this.mTrackNumber = trackNumber;
    }
}
