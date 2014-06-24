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

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class Collection {

    private String mId;

    private String mName;

    private boolean mIsLocal;

    protected ConcurrentHashMap<String, Album> mAlbums = new ConcurrentHashMap<String, Album>();

    protected ConcurrentHashMap<String, Artist> mArtists = new ConcurrentHashMap<String, Artist>();

    protected ConcurrentHashMap<String, Query> mQueries = new ConcurrentHashMap<String, Query>();

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
