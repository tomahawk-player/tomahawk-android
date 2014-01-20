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

    private ConcurrentHashMap<String, Album> mAlbums = new ConcurrentHashMap<String, Album>();

    private ConcurrentHashMap<String, Album> mLocalAlbums = new ConcurrentHashMap<String, Album>();

    private ConcurrentHashMap<String, Album> mAlbumsFetchedViaHatchet
            = new ConcurrentHashMap<String, Album>();

    private ConcurrentHashMap<String, Query> mQueries = new ConcurrentHashMap<String, Query>();

    private ConcurrentHashMap<String, Query> mLocalQueries = new ConcurrentHashMap<String, Query>();

    private ArrayList<Query> mTopHits = new ArrayList<Query>();

    private String mImage;

    /**
     * Construct a new {@link Artist} with the given name
     */
    private Artist(String artistName) {
        mName = artistName;
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
     * Get the {@link org.tomahawk.libtomahawk.collection.Artist} by providing its cache key
     */
    public static Artist getArtistByKey(String key) {
        return sArtists.get(key);
    }

    /**
     * @return A {@link java.util.List} of all {@link Artist}s
     */
    public static ArrayList<Artist> getArtists() {
        ArrayList<Artist> artists = new ArrayList<Artist>(sArtists.values());
        Collections.sort(artists,
                new TomahawkListItemComparator(TomahawkListItemComparator.COMPARE_ALPHA));
        return artists;
    }

    /**
     * @return A {@link java.util.List} of local all {@link Artist}s
     */
    public static ArrayList<Artist> getLocalArtists() {
        ArrayList<Artist> artists = new ArrayList<Artist>();
        for (Artist artist : sArtists.values()) {
            if (artist.hasLocalQueries()) {
                artists.add(artist);
            }
        }
        Collections.sort(artists,
                new TomahawkListItemComparator(TomahawkListItemComparator.COMPARE_ALPHA));
        return artists;
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
            ArrayList<Album> albums = new ArrayList<Album>(mAlbums.values());
            return albums.get(0);
        }
        return null;
    }

    /**
     * @param query the {@link org.tomahawk.libtomahawk.resolver.Query} to be added
     */
    public void addQuery(Query query) {
        String key = TomahawkUtils.getCacheKey(query);
        synchronized (this) {
            if (!mQueries.containsKey(key)) {
                mQueries.put(key, query);
                boolean isLocalQuery = query.getPreferredTrackResult() != null
                        && query.getPreferredTrackResult()
                        .getResolvedBy() instanceof DataBaseResolver;
                if (isLocalQuery) {
                    mLocalQueries.put(key, query);
                }
            }
        }
    }

    /**
     * @return list of all {@link org.tomahawk.libtomahawk.resolver.Query}s from this object.
     */
    public ArrayList<Query> getQueries() {
        ArrayList<Query> queries = new ArrayList<Query>(mQueries.values());
        Collections.sort(queries, new QueryComparator(QueryComparator.COMPARE_ALPHA));
        return queries;
    }

    /**
     * Get a list of all local {@link org.tomahawk.libtomahawk.resolver.Query}s from this {@link
     * org.tomahawk.libtomahawk.collection.Artist}.
     */
    public ArrayList<Query> getLocalQueries() {
        ArrayList<Query> queries = new ArrayList<Query>(mLocalQueries.values());
        Collections.sort(queries, new QueryComparator(QueryComparator.COMPARE_ALPHA));
        return queries;
    }

    public ArrayList<Query> getTopHits() {
        return mTopHits;
    }

    public void setTopHits(ArrayList<Query> topHits) {
        mTopHits = topHits;
    }

    /**
     * Add an {@link Album} to this object
     *
     * @param album the {@link Album} to be added
     */
    public void addAlbum(Album album) {
        String key = TomahawkUtils.getCacheKey(album);
        if (!mAlbums.containsKey(key)) {
            mAlbums.put(key, album);
        }
        synchronized (this) {
            if (!mAlbums.containsKey(key)) {
                mAlbums.put(key, album);
                boolean isLocalAlbum = album.hasLocalQueries();
                if (isLocalAlbum) {
                    mLocalAlbums.put(key, album);
                }
            }
        }
    }

    /**
     * Get a list of all {@link Album}s from this object.
     *
     * @return list of all {@link Album}s from this object.
     */
    public ArrayList<Album> getAlbums() {
        if (mAlbumsFetchedViaHatchet.size() > 0) {
            return new ArrayList<Album>(mAlbumsFetchedViaHatchet.values());
        }
        ArrayList<Album> albums = new ArrayList<Album>(mAlbums.values());
        Collections.sort(albums,
                new TomahawkListItemComparator(TomahawkListItemComparator.COMPARE_ALPHA));
        return albums;
    }

    /**
     * Get a list of all local {@link Album}s from this object.
     *
     * @return list of all local {@link Album}s from this object.
     */
    public ArrayList<Album> getLocalAlbums() {
        ArrayList<Album> albums = new ArrayList<Album>(mLocalAlbums.values());
        Collections.sort(albums,
                new TomahawkListItemComparator(TomahawkListItemComparator.COMPARE_ALPHA));
        return albums;
    }

    public void setAlbumsFetchedViaHatchet(
            ConcurrentHashMap<String, Album> albumsFetchedViaHatchet) {
        mAlbumsFetchedViaHatchet = albumsFetchedViaHatchet;
    }

    /**
     * @return whether or not this {@link org.tomahawk.libtomahawk.collection.Artist} has local
     * queries
     */
    public boolean hasLocalQueries() {
        return mLocalQueries.size() > 0;
    }

    /**
     * @return whether or not this {@link org.tomahawk.libtomahawk.collection.Artist} has albums
     * which have been fetched via Hatchet
     */
    public boolean hasAlbumsFetchedViaHatchet() {
        return mAlbumsFetchedViaHatchet.size() > 0;
    }

    public String getImage() {
        return mImage;
    }

    public void setImage(String image) {
        mImage = image;
    }
}
