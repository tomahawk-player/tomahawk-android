/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Collection {

    private String mId;

    private String mName;

    private boolean mIsLocal;

    protected ConcurrentHashMap<String, Album> mAlbums = new ConcurrentHashMap<String, Album>();

    protected ConcurrentHashMap<String, Artist> mArtists = new ConcurrentHashMap<String, Artist>();

    protected ConcurrentHashMap<String, Query> mQueries = new ConcurrentHashMap<String, Query>();

    protected ConcurrentHashMap<Album, List<Query>> mAlbumTracks
            = new ConcurrentHashMap<Album, List<Query>>();

    protected ConcurrentHashMap<Artist, Map<String, Query>> mArtistTracks
            = new ConcurrentHashMap<Artist, Map<String, Query>>();

    protected ConcurrentHashMap<Artist, Map<String, Album>> mArtistAlbums
            = new ConcurrentHashMap<Artist, Map<String, Album>>();

    protected ConcurrentHashMap<TomahawkListItem, Integer> mAddedTimeStamps
            = new ConcurrentHashMap<TomahawkListItem, Integer>();

    protected Collection(String id, String name, boolean isLocal) {
        mId = id;
        mName = name;
        mIsLocal = isLocal;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public boolean isLocal() {
        return mIsLocal;
    }

    public void wipe() {
        mQueries = new ConcurrentHashMap<String, Query>();
        mArtists = new ConcurrentHashMap<String, Artist>();
        mAlbums = new ConcurrentHashMap<String, Album>();
        mAlbumTracks = new ConcurrentHashMap<Album, List<Query>>();
        mArtistTracks = new ConcurrentHashMap<Artist, Map<String, Query>>();
        mArtistAlbums = new ConcurrentHashMap<Artist, Map<String, Album>>();
    }

    public void addQuery(Query query, int addedTimeStamp) {
        if (!TextUtils.isEmpty(query.getName()) && !mQueries.containsKey(query.getCacheKey())) {
            mQueries.put(query.getCacheKey(), query);
        }
        if (addedTimeStamp > 0) {
            if (mAddedTimeStamps.get(query.getAlbum()) == null
                    || mAddedTimeStamps.get(query.getAlbum()) < addedTimeStamp) {
                mAddedTimeStamps.put(query.getAlbum(), addedTimeStamp);
            }
            if (mAddedTimeStamps.get(query.getArtist()) == null
                    || mAddedTimeStamps.get(query.getArtist()) < addedTimeStamp) {
                mAddedTimeStamps.put(query.getArtist(), addedTimeStamp);
            }
            mAddedTimeStamps.put(query, addedTimeStamp);
        }
    }

    /**
     * @return A {@link java.util.List} of all {@link Track}s in this {@link Collection}
     */
    public ArrayList<Query> getQueries() {
        return getQueries(true);
    }

    /**
     * @return A {@link java.util.List} of all {@link Track}s in this {@link Collection}
     */
    public ArrayList<Query> getQueries(boolean sorted) {
        ArrayList<Query> queries = new ArrayList<Query>(mQueries.values());
        if (sorted) {
            Collections.sort(queries, new QueryComparator(QueryComparator.COMPARE_ALPHA));
        }
        return queries;
    }

    public void addArtist(Artist artist) {
        if (!TextUtils.isEmpty(artist.getName()) && !mArtists.containsKey(artist.getCacheKey())) {
            mArtists.put(artist.getCacheKey(), artist);
        }
    }

    /**
     * @return A {@link java.util.List} of all {@link org.tomahawk.libtomahawk.collection.Artist}s
     * in this {@link org.tomahawk.libtomahawk.collection.Collection}
     */
    public ArrayList<Artist> getArtists() {
        return getArtists(true);
    }

    /**
     * @return A {@link java.util.List} of all {@link org.tomahawk.libtomahawk.collection.Artist}s
     * in this {@link org.tomahawk.libtomahawk.collection.Collection}
     */
    public ArrayList<Artist> getArtists(boolean sorted) {
        ArrayList<Artist> artists = new ArrayList<Artist>(mArtists.values());
        if (sorted) {
            Collections.sort(artists,
                    new TomahawkListItemComparator(TomahawkListItemComparator.COMPARE_ALPHA));
        }
        return artists;
    }

    public void addAlbum(Album album) {
        if (!TextUtils.isEmpty(album.getName()) && !mAlbums.containsKey(album.getCacheKey())) {
            mAlbums.put(album.getCacheKey(), album);
        }
    }

    /**
     * @return A {@link java.util.List} of all {@link org.tomahawk.libtomahawk.collection.Album}s in
     * this {@link org.tomahawk.libtomahawk.collection.Collection}
     */
    public ArrayList<Album> getAlbums() {
        return getAlbums(true);
    }

    /**
     * @return A {@link java.util.List} of all {@link org.tomahawk.libtomahawk.collection.Artist}s
     * in this {@link org.tomahawk.libtomahawk.collection.Collection}
     */
    public ArrayList<Album> getAlbums(boolean sorted) {
        ArrayList<Album> albums = new ArrayList<Album>(mAlbums.values());
        if (sorted) {
            Collections.sort(albums,
                    new TomahawkListItemComparator(TomahawkListItemComparator.COMPARE_ALPHA));
        }
        return albums;
    }

    public void addArtistAlbum(Artist artist, Album album) {
        if (mArtistAlbums.get(artist) == null) {
            mArtistAlbums.put(artist, new ConcurrentHashMap<String, Album>());
        }
        mArtistAlbums.get(artist).put(album.getCacheKey(), album);
    }

    /**
     * @return A {@link java.util.List} of all {@link Album}s by the given Artist.
     */
    public ArrayList<Album> getArtistAlbums(Artist artist, boolean sorted) {
        ArrayList<Album> albums = new ArrayList<Album>();
        if (mArtistAlbums.get(artist) != null) {
            albums.addAll(mArtistAlbums.get(artist).values());
        }
        if (sorted) {
            Collections.sort(albums, new TomahawkListItemComparator(QueryComparator.COMPARE_ALPHA));
        }
        return albums;
    }

    public void addArtistTracks(Artist artist, Query query) {
        if (mArtistTracks.get(artist) == null) {
            mArtistTracks.put(artist, new ConcurrentHashMap<String, Query>());
        }
        mArtistTracks.get(artist).put(query.getCacheKey(), query);
    }

    /**
     * @return A {@link java.util.List} of all {@link Track}s from the given Artist.
     */
    public ArrayList<Query> getArtistTracks(Artist artist, boolean sorted) {
        ArrayList<Query> queries = new ArrayList<Query>();
        if (mArtistTracks.get(artist) != null) {
            queries.addAll(mArtistTracks.get(artist).values());
        }
        if (sorted) {
            Collections
                    .sort(queries, new TomahawkListItemComparator(QueryComparator.COMPARE_ALPHA));
        }
        return queries;
    }

    public void addAlbumTracks(Album album, List<Query> queries) {
        mAlbumTracks.put(album, queries);
    }

    /**
     * @return A {@link java.util.List} of all {@link Track}s from the given Album.
     */
    public ArrayList<Query> getAlbumTracks(Album album, boolean sorted) {
        ArrayList<Query> queries = new ArrayList<Query>();
        if (mAlbumTracks.get(album) != null) {
            queries.addAll(mAlbumTracks.get(album));
        }
        if (sorted) {
            Collections.sort(queries, new QueryComparator(QueryComparator.COMPARE_ALBUMPOS));
        }
        return queries;
    }

    public int getAddedTimeStamp(TomahawkListItem item) {
        Integer timestamp = mAddedTimeStamps.get(item);
        if (timestamp == null) {
            return Integer.MAX_VALUE;
        } else {
            return timestamp;
        }
    }

    public ConcurrentHashMap<TomahawkListItem, Integer> getAddedTimeStamps() {
        return mAddedTimeStamps;
    }
}
