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

import java.util.HashMap;

import org.tomahawk.libtomahawk.resolver.ScriptResolver;

/**
 * This class represents a track.
 */
public class Track implements TomahawkBaseAdapter.TomahawkListItem {

    private static HashMap<Long, Track> sTracks = new HashMap<Long, Track>();

    /**
     * Path of file or URL.
     */
    private String mPath;
    private boolean mIsLocal = true;
    private String mName;
    private Album mAlbum;
    private Artist mArtist;
    private int mBitrate;
    private int mSize;
    private long mDuration;
    private int mTrackNumber;
    private int mYear;
    private long mId;
    private ScriptResolver mScriptResolver;
    private float mScore;

    public Track(long l) {
        setId(l);
    }

    /**
     * Construct a new Album from the id
     * 
     * @param id
     */
    public static Track get(long id) {

        if (!sTracks.containsKey(id))
            sTracks.put(id, new Track(id));

        return sTracks.get(id);
    }

    /* 
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getName();
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.TomahawkListItem#getName()
     */
    @Override
    public String getName() {
        return mName;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.TomahawkListItem#getArtist()
     */
    @Override
    public Artist getArtist() {
        return mArtist;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.TomahawkListItem#getAlbum()
     */
    @Override
    public Album getAlbum() {
        return mAlbum;
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

    public boolean isLocal() {
        return mIsLocal;
    }

    public void setLocal(boolean mIsLocal) {
        this.mIsLocal = mIsLocal;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public void setAlbum(Album album) {
        this.mAlbum = album;
    }

    public void setArtist(Artist artist) {
        this.mArtist = artist;
    }

    public int getBitrate() {
        return mBitrate;
    }

    public void setBitrate(int bitrate) {
        this.mBitrate = bitrate;
    }

    public int getSize() {
        return mSize;
    }

    public void setSize(int size) {
        this.mSize = size;
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

    public int getYear() {
        return mYear;
    }

    public void setYear(int year) {
        this.mYear = year;
    }

    public ScriptResolver getScriptResolver() {
        return mScriptResolver;
    }

    public void setScriptResolver(ScriptResolver scriptResolver) {
        this.mScriptResolver = scriptResolver;
    }

    public float getScore() {
        return mScore;
    }

    public void setScore(float score) {
        this.mScore = score;
    }

}
