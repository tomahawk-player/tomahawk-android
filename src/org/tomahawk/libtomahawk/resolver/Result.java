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
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.mediaplayers.DeezerMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.RdioMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.SpotifyMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.VLCMediaPlayer;
import org.tomahawk.tomahawk_android.utils.MediaPlayerInterface;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a {@link Result}, which will be returned by a {@link Resolver}.
 */
public class Result {

    public static int RESULT_TYPE_TRACK = 0;

    public static int RESULT_TYPE_ALBUM = 1;

    public static int RESULT_TYPE_ARTIST = 2;

    private static ConcurrentHashMap<String, Result> sResults
            = new ConcurrentHashMap<String, Result>();

    private MediaPlayerInterface mMediaPlayerInterface;

    // Normally cache keys are unique. In this case the result's cache key is only unique in the
    // context of a _single_ Query.
    private String mCacheKey;

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

    private float mTrackScore;

    private float mAlbumScore;

    private float mArtistScore;

    private int mType = RESULT_TYPE_TRACK;

    /**
     * Construct a new {@link Result} with the given {@link Track}
     */
    private Result(String url, Track track, Resolver resolvedBy, String queryKey) {
        if (url == null) {
            mPath = "";
        } else {
            mPath = url;
            isResolved = true;
        }
        mResolvedBy = resolvedBy;
        if (TomahawkApp.PLUGINNAME_SPOTIFY.equals(mResolvedBy.getId())) {
            mMediaPlayerInterface = SpotifyMediaPlayer.getInstance();
        } else if (TomahawkApp.PLUGINNAME_RDIO.equals(mResolvedBy.getId())) {
            mMediaPlayerInterface = RdioMediaPlayer.getInstance();
        } else if (TomahawkApp.PLUGINNAME_DEEZER.equals(mResolvedBy.getId())) {
            mMediaPlayerInterface = DeezerMediaPlayer.getInstance();
        } else {
            mMediaPlayerInterface = VLCMediaPlayer.getInstance();
            if (TomahawkApp.PLUGINNAME_USERCOLLECTION.equals(mResolvedBy.getId())) {
                mIsLocal = true;
            }
        }
        mArtist = track.getArtist();
        mAlbum = track.getAlbum();
        mTrack = track;
        if (mCacheKey == null) {
            mCacheKey = TomahawkUtils.getCacheKey(this, queryKey);
        }
    }

    /**
     * Construct a new {@link Result} with the given {@link Artist}
     */
    private Result(Artist artist, String queryKey) {
        mArtist = artist;
        mAlbum = artist.getAlbum();
        if (mCacheKey == null) {
            mCacheKey = TomahawkUtils.getCacheKey(this, queryKey);
        }
    }

    /**
     * Construct a new {@link Result} with the given {@link Album}
     */
    private Result(Album album, String queryKey) {
        mAlbum = album;
        if (mCacheKey == null) {
            mCacheKey = TomahawkUtils.getCacheKey(this, queryKey);
        }
    }

    public static Result get(String url, Track track, Resolver resolvedBy, String queryKey) {
        Result result = new Result(url, track, resolvedBy, queryKey);
        return ensureCache(result);
    }

    public static Result get(Artist artist, String queryKey) {
        Result result = new Result(artist, queryKey);
        return ensureCache(result);
    }

    public static Result get(Album album, String queryKey) {
        Result result = new Result(album, queryKey);
        return ensureCache(result);
    }

    /**
     * If Result is already in our cache, return that. Otherwise add it to the cache.
     */
    private static Result ensureCache(Result result) {
        if (!sResults.containsKey(result.getCacheKey())) {
            sResults.put(result.getCacheKey(), result);
        }
        return sResults.get(result.getCacheKey());
    }

    /**
     * Get the {@link Result} by providing its cache key
     */
    public static Result getResultByKey(String key) {
        return sResults.get(key);
    }

    public MediaPlayerInterface getMediaPlayerInterface() {
        return mMediaPlayerInterface;
    }

    public String getCacheKey() {
        return mCacheKey;
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

    /**
     * @return The type of this result. Can be RESULT_TYPE_TRACK, RESULT_TYPE_ALBUM or
     * RESULT_TYPE_ARTIST. This determines how we will display this result later on.
     */
    public int getType() {
        return mType;
    }

    /**
     * Set the type of this result. Can be RESULT_TYPE_TRACK, RESULT_TYPE_ALBUM or
     * RESULT_TYPE_ARTIST. This determines how we will display this result later on.
     */
    public void setType(int type) {
        mType = type;
    }
}
