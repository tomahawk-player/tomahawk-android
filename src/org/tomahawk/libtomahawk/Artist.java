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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.tomahawk.libtomahawk.TomahawkListArrayAdapter.TomahawkListItem;

/**
 * This class is used to compare two Artists.
 */
class ArtistComparator implements Comparator<Artist> {
    public int compare(Artist a1, Artist a2) {
        return a1.getName().compareTo(a2.getName());
    }
}

/**
 * This class represents an Artist.
 */
public class Artist implements TomahawkListItem, Serializable {

    private static final long serialVersionUID = -5358580053668357261L;

    private long mId;
    private String mName;
    private HashMap<Long, Album> mAlbums;
    private HashMap<Long, Track> mTracks;

    public Artist(long id) {
        mId = id;
        mAlbums = new HashMap<Long, Album>();
        mTracks = new HashMap<Long, Track>();
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
        return this;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.TomahawkListItem#getAlbum()
     */
    @Override
    public Album getAlbum() {
        // TODO Auto-generated method stub
        return null;
    }

    public void addTrack(Track track) {
        mTracks.put(track.getId(), track);
    }

    public ArrayList<Track> getTracks() {
        ArrayList<Track> list = new ArrayList<Track>(mTracks.values());
        Collections.sort(list, new TrackComparator(TrackComparator.COMPARE_DISCNUM));
        return list;
    }

    public void addAlbum(Album album) {
        mAlbums.put(album.getId(), album);
    }

    public ArrayList<Album> getAlbums() {
        ArrayList<Album> albums = new ArrayList<Album>(mAlbums.values());
        Collections.sort(albums, new AlbumComparator());
        return albums;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setId(long id) {
        mId = id;
    }

    public long getId() {
        return mId;
    }
}
