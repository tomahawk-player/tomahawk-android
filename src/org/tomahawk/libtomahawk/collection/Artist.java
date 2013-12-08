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

import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.QueryComparator;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents an {@link Artist}.
 */
public class Artist implements TomahawkBaseAdapter.TomahawkListItem {

    private static ConcurrentHashMap<String, Artist> sArtists
            = new ConcurrentHashMap<String, Artist>();

    private String mName;

    private ArrayList<Album> mAlbums;

    private ArrayList<Query> mQueries;

    /**
     * Construct a new {@link Artist} with the given name
     */
    private Artist(String artistName) {
        mName = artistName;
        mAlbums = new ArrayList<Album>();
        mQueries = new ArrayList<Query>();
    }

    /**
     * Returns the {@link Artist} with the given id. If none exists in our static {@link
     * ConcurrentHashMap} yet, construct and add it.
     *
     * @return {@link Artist} with the given id
     */
    public static Artist get(String artistName) {
        Artist artist = new Artist(artistName);
        String key = TomahawkUtils.getCacheKey(artist);
        if (!sArtists.containsKey(key)) {
            sArtists.put(key, artist);
        }
        return sArtists.get(key);
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
        if (!mAlbums.isEmpty()) {
            return mAlbums.get(0);
        }
        return null;
    }

    /**
     * @param query the {@link org.tomahawk.libtomahawk.resolver.Query} to be added
     */
    public void addQuery(Query query) {
        mQueries.add(query);
    }

    /**
     * @return list of all {@link org.tomahawk.libtomahawk.resolver.Query}s from this object.
     */
    public ArrayList<Query> getQueries() {
        Collections.sort(mQueries, new QueryComparator(QueryComparator.COMPARE_ALBUMPOS));
        return mQueries;
    }

    /**
     * Add an {@link Album} to this object
     *
     * @param album the {@link Album} to be added
     */
    public void addAlbum(Album album) {
        mAlbums.add(album);
    }

    /**
     * Clear all {@link Album}s.
     */
    public void clearAlbums() {
        mAlbums = new ArrayList<Album>();
    }

    /**
     * Get a list of all {@link Album}s from this object.
     *
     * @return list of all {@link Album}s from this object.
     */
    public ArrayList<Album> getAlbums() {
        Collections.sort(mAlbums, new AlbumComparator(AlbumComparator.COMPARE_ALPHA));
        return mAlbums;
    }
}
