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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;

/**
 * This class is used to compare two Albums.
 */
class AlbumComparator implements Comparator<Album> {
    public int compare(Album a1, Album a2) {
        return a1.getName().compareTo(a2.getName());
    }
}

/**
 * Class which represents a Tomahawk Album.
 */
public class Album implements TomahawkBaseAdapter.TomahawkListItem {

    private static HashMap<Long, Album> sAlbums = new HashMap<Long, Album>();

    private static CoverCache sCoverCache;
    private HashMap<Long, Track> mTracks;

    private long mId;
    private String mName;
    private String mAlbumArt;
    private String mFirstYear;
    private String mLastYear;
    private Artist mArtist;

    /**
     * Cache album cover art.
     */
    private static class CoverCache extends LruCache<String, Bitmap> {

        private static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();

        static {
            sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
            sBitmapOptions.inDither = false;
        }

        public CoverCache() {
            super(4 * 1024 * 1024);
        }

        @Override
        public Bitmap create(String path) {
            return BitmapFactory.decodeFile(path, sBitmapOptions);
        }

        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getRowBytes() * value.getHeight();
        }
    }

    public Album(long id) {
        setId(id);
        mTracks = new HashMap<Long, Track>();
    }

    /**
     * Construct a new Album from the id
     * 
     * @param id
     */
    public static Album get(long id) {

        if (!sAlbums.containsKey(id))
            sAlbums.put(id, new Album(id));

        return sAlbums.get(id);
    }

    /* 
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return mName;
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
        return this;
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
        ArrayList<Track> tracks = new ArrayList<Track>(mTracks.values());
        Collections.sort(tracks, new TrackComparator(TrackComparator.COMPARE_DISCNUM));
        return tracks;
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

    public void setName(String name) {
        mName = name;
    }

    public Bitmap getAlbumArt() {

        if (mAlbumArt == null)
            return null;

        if (sCoverCache == null)
            sCoverCache = new CoverCache();

        return sCoverCache.get(mAlbumArt);
    }

    public String getAlbumArtPath() {
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

    public void setArtist(Artist artist) {
        mArtist = artist;
    }
}
