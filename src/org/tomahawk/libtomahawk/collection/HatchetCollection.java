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

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.ADeferredObject;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class holds the metadata retrieved via Hatchet.
 */
public class HatchetCollection extends Collection {

    private final Set<Album> mAlbums
            = Collections.newSetFromMap(new ConcurrentHashMap<Album, Boolean>());

    private final Set<Artist> mArtists
            = Collections.newSetFromMap(new ConcurrentHashMap<Artist, Boolean>());

    private final Set<Artist> mAlbumArtists
            = Collections.newSetFromMap(new ConcurrentHashMap<Artist, Boolean>());

    private final Set<Query> mQueries
            = Collections.newSetFromMap(new ConcurrentHashMap<Query, Boolean>());

    private final ConcurrentHashMap<Album, List<Query>> mAlbumTracks
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Artist, List<Album>> mArtistAlbums
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Artist, List<Query>> mArtistTopHits
            = new ConcurrentHashMap<>();

    public HatchetCollection() {
        super(TomahawkApp.PLUGINNAME_HATCHET, "");
    }

    @Override
    public void loadIcon(ImageView imageView, boolean grayOut) {
        TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                R.drawable.ic_hatchet, grayOut);
    }

    public void wipe() {
        mQueries.clear();
        mArtists.clear();
        mAlbums.clear();
        mAlbumTracks.clear();
        mArtistAlbums.clear();
    }

    @Override
    public Promise<CollectionCursor<Query>, Throwable, Void> getQueries(int sortMode) {
        final Deferred<CollectionCursor<Query>, Throwable, Void> deferred = new ADeferredObject<>();
        Comparator comparator = null;
        switch (sortMode) {
            case SORT_ALPHA:
                comparator = new AlphaComparator();
                break;
            case SORT_ARTIST_ALPHA:
                comparator = new ArtistAlphaComparator();
                break;
            case SORT_LAST_MODIFIED:
                comparator = new AlphaComparator();  //TODO
                break;
        }
        List<Query> queries = new ArrayList<>(mQueries);
        if (comparator != null) {
            Collections.sort(queries, comparator);
        }
        CollectionCursor<Query> collectionCursor = new CollectionCursor<>(queries, Query.class);
        return deferred.resolve(collectionCursor);
    }

    public void addArtist(Artist artist) {
        mArtists.add(artist);
    }

    @Override
    public Promise<CollectionCursor<Artist>, Throwable, Void> getArtists(int sortMode) {
        final Deferred<CollectionCursor<Artist>, Throwable, Void> deferred
                = new ADeferredObject<>();
        Comparator comparator = null;
        switch (sortMode) {
            case SORT_ALPHA:
                comparator = new AlphaComparator();
                break;
            case SORT_LAST_MODIFIED:
                comparator = new AlphaComparator();   //TODO
                break;
        }
        List<Artist> artists = new ArrayList<>(mArtists);
        if (comparator != null) {
            Collections.sort(artists, comparator);
        }
        CollectionCursor<Artist> collectionCursor = new CollectionCursor<>(artists, Artist.class);
        return deferred.resolve(collectionCursor);
    }

    public void addAlbumArtist(Artist artist) {
        mAlbumArtists.add(artist);
    }

    @Override
    public Promise<CollectionCursor<Artist>, Throwable, Void> getAlbumArtists(int sortMode) {
        final Deferred<CollectionCursor<Artist>, Throwable, Void> deferred
                = new ADeferredObject<>();
        Comparator comparator = null;
        switch (sortMode) {
            case SORT_ALPHA:
                comparator = new AlphaComparator();
                break;
            case SORT_LAST_MODIFIED:
                comparator = new AlphaComparator();   //TODO
                break;
        }
        List<Artist> artists = new ArrayList<>(mAlbumArtists);
        if (comparator != null) {
            Collections.sort(artists, comparator);
        }
        CollectionCursor<Artist> collectionCursor = new CollectionCursor<>(artists, Artist.class);
        return deferred.resolve(collectionCursor);
    }

    public void addAlbum(Album album) {
        mAlbums.add(album);
    }

    @Override
    public Promise<CollectionCursor<Album>, Throwable, Void> getAlbums(int sortMode) {
        final Deferred<CollectionCursor<Album>, Throwable, Void> deferred = new ADeferredObject<>();
        Comparator comparator = null;
        switch (sortMode) {
            case SORT_ALPHA:
                comparator = new AlphaComparator();
                break;
            case SORT_ARTIST_ALPHA:
                comparator = new ArtistAlphaComparator();
                break;
            case SORT_LAST_MODIFIED:
                comparator = new AlphaComparator();   //TODO
                break;
        }
        List<Album> albums = new ArrayList<>(mAlbums);
        if (comparator != null) {
            Collections.sort(albums, comparator);
        }
        CollectionCursor<Album> collectionCursor = new CollectionCursor<>(albums, Album.class);
        return deferred.resolve(collectionCursor);
    }

    public void addArtistAlbum(Artist artist, Album album) {
        if (mArtistAlbums.get(artist) == null) {
            mArtistAlbums.put(artist, new ArrayList<Album>());
        }
        mArtistAlbums.get(artist).add(album);
    }

    @Override
    public Promise<CollectionCursor<Album>, Throwable, Void> getArtistAlbums(final Artist artist) {
        final Deferred<CollectionCursor<Album>, Throwable, Void> deferred = new ADeferredObject<>();
        List<Album> albums = new ArrayList<>();
        if (mArtistAlbums.get(artist) != null) {
            albums.addAll(mArtistAlbums.get(artist));
        }
        CollectionCursor<Album> collectionCursor
                = new CollectionCursor<>(albums, Album.class);
        return deferred.resolve(collectionCursor);
    }

    public void addAlbumTracks(Album album, List<Query> queries) {
        mAlbumTracks.put(album, queries);
    }

    public void addAlbumTrack(Album album, Query query) {
        if (mAlbumTracks.get(album) == null) {
            mAlbumTracks.put(album, new ArrayList<Query>());
        }
        mAlbumTracks.get(album).add(query);
    }

    @Override
    public Promise<CollectionCursor<Query>, Throwable, Void> getAlbumTracks(final Album album) {
        final Deferred<CollectionCursor<Query>, Throwable, Void> deferred = new ADeferredObject<>();
        List<Query> queries = new ArrayList<>();
        if (mAlbumTracks.get(album) != null) {
            queries.addAll(mAlbumTracks.get(album));
        }
        CollectionCursor<Query> collectionCursor
                = new CollectionCursor<>(queries, Query.class);
        return deferred.resolve(collectionCursor);
    }

    public void addArtistTopHits(Artist artist, List<Query> topHits) {
        mArtistTopHits.put(artist, topHits);
    }

    /**
     * @return A {@link java.util.List} of all top hits {@link Track}s from the given Artist.
     */
    public Promise<CollectionCursor<Query>, Throwable, Void> getArtistTopHits(final Artist artist) {
        final Deferred<CollectionCursor<Query>, Throwable, Void> deferred = new ADeferredObject<>();
        List<Query> queries = new ArrayList<>();
        if (mArtistTopHits.get(artist) != null) {
            queries.addAll(mArtistTopHits.get(artist));
        }
        CollectionCursor<Query> collectionCursor = new CollectionCursor<>(queries, Query.class);
        return deferred.resolve(collectionCursor);
    }
}
