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
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.Intent;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Collection {

    private String mId;

    private String mName;

    private boolean mIsLocal;

    private ConcurrentHashMap<String, Album> mAlbums = new ConcurrentHashMap<String, Album>();

    private ConcurrentHashMap<String, Artist> mArtists = new ConcurrentHashMap<String, Artist>();

    private ConcurrentHashMap<String, Query> mQueries = new ConcurrentHashMap<String, Query>();

    private ConcurrentHashMap<Album, List<Query>> mAlbumTracks
            = new ConcurrentHashMap<Album, List<Query>>();

    private ConcurrentHashMap<Artist, List<Query>> mArtistTracks
            = new ConcurrentHashMap<Artist, List<Query>>();

    private ConcurrentHashMap<Artist, List<Album>> mArtistAlbums
            = new ConcurrentHashMap<Artist, List<Album>>();

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

    protected void sendCollectionUpdatedBroadcast() {
        Intent intent = new Intent(CollectionManager.COLLECTION_UPDATED);
        intent.putExtra(CollectionManager.COLLECTION_ID, getId());
        TomahawkApp.getContext().sendBroadcast(intent);
    }

    public void wipe() {
        mQueries = new ConcurrentHashMap<String, Query>();
        mArtists = new ConcurrentHashMap<String, Artist>();
        mAlbums = new ConcurrentHashMap<String, Album>();
        mAlbumTracks = new ConcurrentHashMap<Album, List<Query>>();
        mArtistTracks = new ConcurrentHashMap<Artist, List<Query>>();
        mArtistAlbums = new ConcurrentHashMap<Artist, List<Album>>();
    }

    public void addQuery(Query query) {
        if (!TextUtils.isEmpty(query.getName()) && !mQueries.contains(query)) {
            mQueries.put(query.getCacheKey(), query);
            if (mArtistTracks.get(query.getArtist()) == null) {
                mArtistTracks.put(query.getArtist(), new ArrayList<Query>());
            }
            mArtistTracks.get(query.getArtist()).add(query);
        }
    }

    public void addAlbumTrack(Album album, Query query) {
        if (mAlbumTracks.get(album) == null) {
            mAlbumTracks.put(album, new ArrayList<Query>());
        }
        mAlbumTracks.get(album).add(query);
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

    public void addArtist(Artist artist) {
        if (!TextUtils.isEmpty(artist.getName()) && !mArtists.contains(artist)) {
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

    /**
     * @return A {@link java.util.List} of all {@link Album}s by the given Artist.
     */
    public ArrayList<Album> getArtistAlbums(Artist artist, boolean sorted) {
        ArrayList<Album> albums = new ArrayList<Album>();
        if (mArtistAlbums.get(artist) != null) {
            albums.addAll(mArtistAlbums.get(artist));
        }
        if (sorted) {
            Collections.sort(albums, new TomahawkListItemComparator(QueryComparator.COMPARE_ALPHA));
        }
        return albums;
    }

    /**
     * @return A {@link java.util.List} of all {@link Track}s from the given Artist.
     */
    public ArrayList<Query> getArtistTracks(Artist artist, boolean sorted) {
        ArrayList<Query> queries = new ArrayList<Query>();
        if (mArtistTracks.get(artist) != null) {
            queries.addAll(mArtistTracks.get(artist));
        }
        if (sorted) {
            Collections
                    .sort(queries, new TomahawkListItemComparator(QueryComparator.COMPARE_ALPHA));
        }
        return queries;
    }

    public void addAlbum(Album album) {
        if (!TextUtils.isEmpty(album.getName()) && !mAlbums.contains(album)) {
            mAlbums.put(album.getCacheKey(), album);
            if (mArtistAlbums.get(album.getArtist()) == null) {
                mArtistAlbums.put(album.getArtist(), new ArrayList<Album>());
            }
            mArtistAlbums.get(album.getArtist()).add(album);
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
}
