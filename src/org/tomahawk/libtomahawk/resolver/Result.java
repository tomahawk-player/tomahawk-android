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

/**
 * This class represents a {@link Result}, which will be returned by a {@link Resolver}.
 */
public class Result {

    private Resolver mResolver;

    private Artist mArtist;

    private Album mAlbum;

    private Track mTrack;

    private float mTrackScore;

    private float mAlbumScore;

    private float mArtistScore;

    /**
     * Default constructor
     */
    public Result() {
    }

    /**
     * Construct a new {@link Result} with the given {@link Track}
     */
    public Result(Track track) {
        mArtist = track.getArtist();
        mAlbum = track.getAlbum();
        mTrack = track;
    }

    /**
     * Construct a new {@link Result} with the given {@link Artist}
     */
    public Result(Artist artist) {
        mArtist = artist;
        mAlbum = artist.getAlbum();
    }

    /**
     * Construct a new {@link Result} with the given {@link Album}
     */
    public Result(Album album) {
        mAlbum = album;
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
        if (getTrack() != null) {
            getTrack().setScore(score);
        }
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
        if (getAlbum() != null) {
            getAlbum().setScore(score);
        }
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
        if (getArtist() != null) {
            getArtist().setScore(score);
        }
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
    public Resolver getResolver() {
        return mResolver;
    }

    /**
     * Set the given {@link Resolver} as this {@link Result}'s {@link Resolver}
     */
    public void setResolver(Resolver resolver) {
        this.mResolver = resolver;
    }
}
