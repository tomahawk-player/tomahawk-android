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
package org.tomahawk.libtomahawk.resolver;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;

import android.text.TextUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a {@link Result}, which will be returned by a {@link Resolver}.
 */
public class Result {

    private static ConcurrentHashMap<String, Result> mResults
            = new ConcurrentHashMap<String, Result>();

    private Artist mArtist;

    private Album mAlbum;

    private Track mTrack;

    /**
     * Path of file or URL.
     */
    private String mPath;

    private int mBitrate;

    private int mSize;

    private Resolver mResolvedBy;

    private String mLinkUrl;

    private String mPurchaseUrl;

    private float mScore;

    private boolean isResolved;

    private float mTrackScore;

    private float mAlbumScore;

    private float mArtistScore;

    /**
     * Construct a new {@link Result} with the given {@link Track}
     */
    private Result(String url, Query query) {
        setPath(url);
        mArtist = query.getArtist();
        mAlbum = query.getAlbum();
        mTrack = query.getPreferredTrackResult().getTrack();
    }

    /**
     * Construct a new {@link Result} with the given {@link Track}
     */
    private Result(String url, Track track) {
        setPath(url);
        mArtist = track.getArtist();
        mAlbum = track.getAlbum();
        mTrack = track;
    }

    /**
     * Construct a new {@link Result} with the given {@link Artist}
     */
    private Result(String url, Artist artist) {
        setPath(url);
        mArtist = artist;
        mAlbum = artist.getAlbum();
    }

    /**
     * Construct a new {@link Result} with the given {@link Album}
     */
    private Result(Album album) {
        mAlbum = album;
    }

    public static Result get(String url, Query query) {
        Result result = mResults.get(url);
        if (result == null) {
            result = new Result(url, query);
            mResults.put(url, result);
        }
        return result;
    }

    public static Result get(String url, Track track) {
        Result result = mResults.get(url);
        if (result == null) {
            result = new Result(url, track);
            mResults.put(url, result);
        }
        return result;
    }

    public static Result get(String url, Album album) {
        Result result = mResults.get(url);
        if (result == null) {
            result = new Result(album);
            mResults.put(url, result);
        }
        return result;
    }

    public static Result get(String url, Artist artist) {
        Result result = mResults.get(url);
        if (result == null) {
            result = new Result(url, artist);
            mResults.put(url, result);
        }
        return result;
    }

    /**
     * @return the {@link Track} associated with this {@link Result}
     */
    public Track getTrack() {
        return mTrack;
    }

    /**
     * Set the given {@link Track} as this {@link Result}'s {@link Track}
     */
    public void setTrack(Track mTrack) {
        this.mTrack = mTrack;
    }

    /**
     * @return the {@link Track}'s score associated with this {@link Result}
     */
    public float getTrackScore() {
        return mTrackScore;
    }

    /**
     * Set the given {@link Track}'s score as this {@link Result}'s {@link Track}'s score
     */
    public void setTrackScore(float score) {
        this.mTrackScore = score;
    }

    /**
     * @return the {@link Album}'s score associated with this {@link Result}
     */
    public float getAlbumScore() {
        return mAlbumScore;
    }

    /**
     * Set the given {@link Album}'s score as this {@link Result}'s {@link Album}'s score
     */
    public void setAlbumScore(float score) {
        this.mAlbumScore = score;
    }

    /**
     * @return the {@link Artist}'s score associated with this {@link Result}
     */
    public float getArtistScore() {
        return mArtistScore;
    }

    /**
     * Set the given {@link Artist}'s score as this {@link Result}'s {@link Artist}'s score
     */
    public void setArtistScore(float score) {
        this.mArtistScore = score;
    }

    /**
     * @return the {@link Artist} associated with this {@link Result}
     */
    public Artist getArtist() {
        return mArtist;
    }

    /**
     * Set the given {@link Artist} as this {@link Result}'s {@link Artist}
     */
    public void setArtist(Artist mArtist) {
        this.mArtist = mArtist;
    }

    /**
     * @return the {@link Album} associated with this {@link Result}
     */
    public Album getAlbum() {
        return mAlbum;
    }

    /**
     * Set the given {@link Album} as this {@link Result}'s {@link Album}
     */
    public void setAlbum(Album mAlbum) {
        this.mAlbum = mAlbum;
    }

    /**
     * @return the {@link Resolver} associated with this {@link Result}
     */
    public Resolver getResolvedBy() {
        return mResolvedBy;
    }

    /**
     * Set the given {@link Resolver} as this {@link Result}'s {@link Resolver}
     */
    public void setResolvedBy(Resolver resolvedBy) {
        this.mResolvedBy = resolvedBy;
    }

    /**
     * @return the filePath/url to this {@link org.tomahawk.libtomahawk.resolver.Result}'s audio
     * data
     */
    public String getPath() {
        return mPath;
    }

    /**
     * Set the filePath/url to this {@link org.tomahawk.libtomahawk.resolver.Result}'s audio data
     *
     * @param path the filePath/url to this {@link org.tomahawk.libtomahawk.resolver.Result}'s audio
     *             data
     */
    public void setPath(String path) {
        this.mPath = path;
        if (path != null && !TextUtils.isEmpty(path)) {
            isResolved = true;
        }
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
