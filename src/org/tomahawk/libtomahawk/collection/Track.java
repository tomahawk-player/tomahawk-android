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

import org.tomahawk.libtomahawk.resolver.DataBaseResolver;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;

import android.text.TextUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a {@link Track}.
 */
public class Track implements TomahawkBaseAdapter.TomahawkListItem {

    private static ConcurrentHashMap<Long, Track> sTracks = new ConcurrentHashMap<Long, Track>();

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

    private Resolver mResolver;

    private String mLinkUrl;

    private String mPurchaseUrl;

    private float mScore;

    private boolean isResolved;

    /**
     * Construct a new {@link Track}
     */
    public Track() {
    }

    /**
     * Construct a new {@link Track} with the given id
     *
     * @param id the id used to construct the {@link Track}
     */
    public Track(long id) {
        setId(id);
    }

    /**
     * Returns the {@link Track} with the given id. If none exists in our static {@link
     * ConcurrentHashMap} yet, construct and add it.
     *
     * @param id the id used to construct the {@link Track}
     * @return {@link Track} with the given id
     */
    public static Track get(long id) {

        if (!sTracks.containsKey(id)) {
            sTracks.put(id, new Track(id));
        }

        return sTracks.get(id);
    }

    /**
     * @return the {@link Track}'s name
     */
    @Override
    public String toString() {
        return getName();
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

    /**
     * @return the {@link Track}'s id.
     */
    public long getId() {
        return mId;
    }

    /**
     * Set the {@link Track}'s id.
     *
     * @param id long containing the id
     */
    public void setId(long id) {
        this.mId = id;
    }

    /**
     * @return the filePath/url to this {@link Track}'s audio data
     */
    public String getPath() {
        return mPath;
    }

    /**
     * Set the filePath/url to this {@link Track}'s audio data
     *
     * @param path the filePath/url to this {@link Track}'s audio data
     */
    public void setPath(String path) {
        this.mPath = path;
        if (path != null && !TextUtils.isEmpty(path)) {
            isResolved = true;
        }
    }

    /**
     * @return whether or not this {@link Track} is local
     */
    public boolean isLocal() {
        return mIsLocal;
    }

    /**
     * Set whether or not this {@link Track} is local
     */
    public void setLocal(boolean mIsLocal) {
        this.mIsLocal = mIsLocal;
    }

    /**
     * Set this {@link Track}'s name
     */
    public void setName(String name) {
        this.mName = name;
    }

    /**
     * Set this {@link Track}'s {@link Album}
     */
    public void setAlbum(Album album) {
        this.mAlbum = album;
    }

    /**
     * Set this {@link Track}'s {@link Artist}
     */
    public void setArtist(Artist artist) {
        this.mArtist = artist;
    }

    /**
     * @return this {@link Track}'s bitrate
     */
    public int getBitrate() {
        return mBitrate;
    }

    /**
     * Set this {@link Track}'s bitrate
     */
    public void setBitrate(int bitrate) {
        this.mBitrate = bitrate;
    }

    /**
     * @return this {@link Track}'s filesize
     */
    public int getSize() {
        return mSize;
    }

    /**
     * Set this {@link Track}'s filesize
     */
    public void setSize(int size) {
        this.mSize = size;
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
    public int getTrackNumber() {
        return mTrackNumber;
    }

    /**
     * Set this {@link Track}'s track number
     */
    public void setTrackNumber(int trackNumber) {
        this.mTrackNumber = trackNumber;
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
     * @return this {@link Track}'s resolver
     */
    public Resolver getResolver() {
        return mResolver;
    }

    /**
     * Set this {@link Track}'s resolver
     */
    public void setResolver(Resolver resolver) {
        if (!(resolver instanceof DataBaseResolver)) {
            setLocal(false);
        } else {
            setLocal(true);
        }
        this.mResolver = resolver;
    }

    /**
     * @return this {@link Track}'s score
     */
    public float getScore() {
        return mScore;
    }

    /**
     * Set this {@link Track}'s score
     */
    public void setScore(float score) {
        this.mScore = score;
    }

    /**
     * @return this {@link Track}'s purchase url
     */
    public String getPurchaseUrl() {
        return mPurchaseUrl;
    }

    /**
     * Set this {@link Track}'s purchase url
     */
    public void setPurchaseUrl(String mPurchaseUrl) {
        this.mPurchaseUrl = mPurchaseUrl;
    }

    /**
     * @return this {@link Track}'s link url
     */
    public String getLinkUrl() {
        return mLinkUrl;
    }

    /**
     * Set this {@link Track}'s link url
     */
    public void setLinkUrl(String mLinkUrl) {
        this.mLinkUrl = mLinkUrl;
    }

    /**
     * @return whether or not this {@link Track} has been resolved
     */
    public boolean isResolved() {
        return isResolved;
    }

}
