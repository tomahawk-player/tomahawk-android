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
import org.tomahawk.libtomahawk.collection.Cacheable;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.mediaplayers.AndroidMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.DeezerMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.SpotifyMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.VLCMediaPlayer;

/**
 * This class represents a {@link Result}, which will be returned by a {@link Resolver}.
 */
public class Result extends Cacheable {

    private Class mMediaPlayerClass;

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

    private boolean mIsLocal = false;

    private String mLinkUrl;

    private String mPurchaseUrl;

    private boolean isResolved;

    /**
     * Construct a new {@link Result} with the given {@link Track}
     */
    private Result(String url, Track track, Resolver resolvedBy) {
        super(Result.class, getCacheKey(url, track.getName(), track.getAlbum().getName(),
                track.getArtist().getName()));

        if (url == null) {
            mPath = "";
        } else {
            mPath = url;
            isResolved = true;
        }
        mResolvedBy = resolvedBy;
        if (TomahawkApp.PLUGINNAME_SPOTIFY.equals(mResolvedBy.getId())) {
            mMediaPlayerClass = SpotifyMediaPlayer.class;
        } else if (TomahawkApp.PLUGINNAME_DEEZER.equals(mResolvedBy.getId())) {
            mMediaPlayerClass = DeezerMediaPlayer.class;
        } else if (TomahawkApp.PLUGINNAME_AMZN.equals(mResolvedBy.getId())) {
            mMediaPlayerClass = AndroidMediaPlayer.class;
        } else {
            mMediaPlayerClass = VLCMediaPlayer.class;
            if (TomahawkApp.PLUGINNAME_USERCOLLECTION.equals(mResolvedBy.getId())) {
                mIsLocal = true;
            }
        }
        mArtist = track.getArtist();
        mAlbum = track.getAlbum();
        mTrack = track;
    }

    /**
     * Construct a new {@link Result} with the given {@link Artist}
     */
    private Result(Artist artist) {
        super(Result.class, getCacheKey(artist.getName()));

        mArtist = artist;
    }

    /**
     * Construct a new {@link Result} with the given {@link Album}
     */
    private Result(Album album) {
        super(Result.class, getCacheKey(album.getName(), album.getArtist().getName()));

        mAlbum = album;
    }

    public static Result get(String url, Track track, Resolver resolvedBy) {
        Cacheable cacheable = get(Result.class, getCacheKey(url, track.getName(),
                track.getAlbum().getName(), track.getArtist().getName()));
        return cacheable != null ? (Result) cacheable : new Result(url, track, resolvedBy);
    }

    public static Result get(Artist artist) {
        Cacheable cacheable = get(Result.class, getCacheKey(artist.getName()));
        return cacheable != null ? (Result) cacheable : new Result(artist);
    }

    public static Result get(Album album) {
        Cacheable cacheable = get(Result.class,
                getCacheKey(album.getName(), album.getArtist().getName()));
        return cacheable != null ? (Result) cacheable : new Result(album);
    }

    public Class getMediaPlayerClass() {
        return mMediaPlayerClass;
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
     * @return Whether or not this Result has been resolved locally
     */
    public boolean isLocal() {
        return mIsLocal;
    }

    /**
     * @return the filePath/url to this {@link org.tomahawk.libtomahawk.resolver.Result}'s audio
     * data
     */
    public String getPath() {
        return mPath;
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
