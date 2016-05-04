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

import android.text.TextUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a {@link Track}.
 */
public class Track extends Cacheable implements AlphaComparable, ArtistAlphaComparable {

    private final String mName;

    private final Album mAlbum;

    private final Artist mArtist;

    private long mDuration;

    private int mYear;

    private int mAlbumPos;

    private int mDiscNumber;

    /**
     * Construct a new {@link Track}
     */
    private Track(String trackName, Album album, Artist artist) {
        super(Track.class, getCacheKey(trackName, album.getName(), artist.getName()));

        mName = trackName != null ? trackName : "";
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
        Cacheable cacheable = get(Track.class,
                getCacheKey(trackName, album.getName(), artist.getName()));
        return cacheable != null ? (Track) cacheable : new Track(trackName, album, artist);
    }

    public static Track getByKey(String cacheKey) {
        return (Track) get(Track.class, cacheKey);
    }

    /**
     * @return the {@link Track}'s name
     */
    public String getName() {
        return mName;
    }

    /**
     * @return the {@link Track}'s {@link Artist}
     */
    public Artist getArtist() {
        return mArtist;
    }

    /**
     * @return the {@link Track}'s {@link Album}
     */
    public Album getAlbum() {
        return mAlbum;
    }

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

    public String toShortString() {
        return "'" + getName() + "'" + " by " + getArtist().toShortString() + " on "
                + getAlbum().toShortString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "( " + toShortString() + " )@"
                + Integer.toHexString(hashCode());
    }

}
