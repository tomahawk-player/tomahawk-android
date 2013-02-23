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

import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.Track;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 19.01.13 This class represents a result,
 * which will be returned by a resolver.
 */
public class Result {

    private String mRid;

    private Resolver mResolver;

    private Artist mArtist;

    private Album mAlbum;

    private Track mTrack;

    private String mMimeType;

    private float mTrackScore;

    private float mAlbumScore;

    private float mArtistScore;

    public Result() {
    }

    public Result(Track track) {
        mArtist = track.getArtist();
        mAlbum = track.getAlbum();
        mTrack = track;
    }

    public Result(Artist artist) {
        mArtist = artist;
        mAlbum = artist.getAlbum();
    }

    public Result(Album album) {
        mAlbum = album;
    }

    public Track getTrack() {
        return mTrack;
    }

    public void setTrack(Track mTrack) {
        this.mTrack = mTrack;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setMimeType(String mMimeType) {
        this.mMimeType = mMimeType;
    }

    public float getTrackScore() {
        return mTrackScore;
    }

    public void setTrackScore(float score) {
        this.mTrackScore = score;
        if (getTrack() != null) {
            getTrack().setScore(score);
        }
    }

    public float getAlbumScore() {
        return mAlbumScore;
    }

    public void setAlbumScore(float score) {
        this.mAlbumScore = score;
        if (getAlbum() != null) {
            getAlbum().setScore(score);
        }
    }

    public float getArtistScore() {
        return mArtistScore;
    }

    public void setArtistScore(float score) {
        this.mArtistScore = score;
        if (getArtist() != null) {
            getArtist().setScore(score);
        }
    }

    public Artist getArtist() {
        return mArtist;
    }

    public void setArtist(Artist mArtist) {
        this.mArtist = mArtist;
    }

    public Album getAlbum() {
        return mAlbum;
    }

    public void setAlbum(Album mAlbum) {
        this.mAlbum = mAlbum;
    }

    public Resolver getResolver() {
        return mResolver;
    }

    public void setResolver(Resolver resolver) {
        this.mResolver = resolver;
    }

    public String getRid() {
        return mRid;
    }

    public void setRid(String mRid) {
        this.mRid = mRid;
    }
}
