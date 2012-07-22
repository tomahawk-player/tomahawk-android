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
    private String mAlbum;
    private String mArtist;
    private long mAlbumId;
    private long mArtistId;
    private long mDuration;
    private int mTrackNumber;

    public Track(long l) {
        setId(l);
    }

    public void populate(Cursor cursor) {
        setId(cursor.getLong(0));
        setPath(cursor.getString(1));
        setTitle(cursor.getString(2));
        setAlbum(cursor.getString(3));
        setArtist(cursor.getString(4));
        setAlbumId(cursor.getLong(5));
        setArtistId(cursor.getLong(6));
        setDuration(cursor.getLong(7));
        setTrackNumber(cursor.getInt(8));
    }

    @Override
    public String toString() {
        return getTitle();
    }

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String mPath) {
        this.mPath = mPath;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public String getAlbum() {
        return mAlbum;
    }

    public void setAlbum(String mAlbum) {
        this.mAlbum = mAlbum;
    }

    public String getArtist() {
        return mArtist;
    }

    public void setArtist(String mArtist) {
        this.mArtist = mArtist;
    }

    public long getAlbumId() {
        return mAlbumId;
    }

    public void setAlbumId(long mAlbumId) {
        this.mAlbumId = mAlbumId;
    }

    public long getArtistId() {
        return mArtistId;
    }

    public void setArtistId(long mArtistId) {
        this.mArtistId = mArtistId;
    }

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(long mDuration) {
        this.mDuration = mDuration;
    }

    public int getTrackNumber() {
        return mTrackNumber;
    }

    public void setTrackNumber(int mTrackNumber) {
        this.mTrackNumber = mTrackNumber;
    }
}
