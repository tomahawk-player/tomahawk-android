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

import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents an {@link Artist}.
 */
public class Artist implements TomahawkBaseAdapter.TomahawkListItem {

    private static ConcurrentHashMap<Long, Artist> sArtists = new ConcurrentHashMap<Long, Artist>();

    private long mId;

    private String mName;

    private ConcurrentHashMap<Long, Album> mAlbums;

    private ConcurrentHashMap<Long, Track> mTracks;

    private float mScore;

    /**
     * Construct a new {@link Artist} with the given id
     *
     * @param id long containing id of the to be constructed {@link Artist}
     */
    public Artist(long id) {
        mId = id;
        mAlbums = new ConcurrentHashMap<Long, Album>();
        mTracks = new ConcurrentHashMap<Long, Track>();
    }

    /**
     * Returns the {@link Artist} with the given id. If none exists in our static {@link
     * ConcurrentHashMap} yet, construct and add it.
     *
     * @param id the id used to construct the {@link Artist}
     * @return {@link Artist} with the given id
     */
    public static Artist get(long id) {

        if (!sArtists.containsKey(id)) {
            sArtists.put(id, new Artist(id));
        }

        return sArtists.get(id);
    }

    /**
     * @return this object's name
     */
    @Override
    public String toString() {
        return mName;
    }

    /**
     * @return this object' name
     */
    @Override
    public String getName() {
        return mName;
    }

    /**
     * @return this object
     */
    @Override
    public Artist getArtist() {
        return this;
    }

    /**
     * This method returns the first {@link Album} of this object. If none exists, returns null.
     * It's needed to comply to the {@link org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}
     * interface.
     *
     * @return First {@link Album} of this object. If none exists, returns null.
     */
    @Override
    public Album getAlbum() {
        Album[] albums = mAlbums.values().toArray(new Album[0]);
        if (albums[0] != null) {
            return albums[0];
        }
        return null;
    }

    /**
     * Add a {@link Track} to this object.
     *
     * @param track the {@link Track} to be added
     */
    public void addTrack(Track track) {
        mTracks.put(track.getId(), track);
    }

    /**
     * Get a list of all {@link Track}s from this object.
     *
     * @return list of all {@link Track}s from this object.
     */
    public ArrayList<Track> getTracks() {
        ArrayList<Track> list = new ArrayList<Track>(mTracks.values());
        Collections.sort(list, new TrackComparator(TrackComparator.COMPARE_DISCNUM));
        return list;
    }

    /**
     * Add an {@link Album} to this object
     *
     * @param album the {@link Album} to be added
     */
    public void addAlbum(Album album) {
        mAlbums.put(album.getId(), album);
    }

    /**
     * Clear all {@link Album}s.
     */
    public void clearAlbums() {
        mAlbums = new ConcurrentHashMap<Long, Album>();
    }

    /**
     * Get a list of all {@link Album}s from this object.
     *
     * @return list of all {@link Album}s from this object.
     */
    public ArrayList<Album> getAlbums() {
        ArrayList<Album> albums = new ArrayList<Album>(mAlbums.values());
        Collections.sort(albums, new AlbumComparator(AlbumComparator.COMPARE_ALPHA));
        return albums;
    }

    /**
     * Set the name of this object
     *
     * @param name the name to be set
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * @return this object's id
     */
    public long getId() {
        return mId;
    }

    /**
     * @return float containing the score
     */
    public float getScore() {
        return mScore;
    }

    /**
     * Set this object's score
     *
     * @param score float containing score
     */
    public void setScore(float score) {
        this.mScore = score;
    }
}
