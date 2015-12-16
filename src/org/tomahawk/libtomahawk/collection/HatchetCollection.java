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
import org.tomahawk.libtomahawk.utils.ADeferredObject;
import org.tomahawk.libtomahawk.utils.ImageUtils;
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

    private final ConcurrentHashMap<Album, Playlist> mAlbumTracks
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Artist, List<Album>> mArtistAlbums
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Artist, Playlist> mArtistTopHits
            = new ConcurrentHashMap<>();

    public HatchetCollection() {
        super(TomahawkApp.PLUGINNAME_HATCHET, "");
    }

    @Override
    public void loadIcon(ImageView imageView, boolean grayOut) {
        ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                R.drawable.ic_hatchet, grayOut ? R.color.disabled_resolver : 0);
    }

    public void wipe() {
        mArtists.clear();
        mAlbums.clear();
        mAlbumTracks.clear();
        mArtistAlbums.clear();
    }

    @Override
    public Promise<Playlist, Throwable, Void> getQueries(int sortMode) {
        return null;
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

    public void addArtistAlbums(Artist artist, List<Album> albums) {
        Collections.sort(albums, new AlphaComparator());
        mArtistAlbums.put(artist, albums);
    }

    @Override
    public Promise<CollectionCursor<Album>, Throwable, Void> getArtistAlbums(final Artist artist) {
        final Deferred<CollectionCursor<Album>, Throwable, Void> deferred = new ADeferredObject<>();
        CollectionCursor<Album> collectionCursor = null;
        if (mArtistAlbums.get(artist) != null) {
            List<Album> albums = new ArrayList<>();
            albums.addAll(mArtistAlbums.get(artist));
            collectionCursor = new CollectionCursor<>(albums, Album.class);
        }
        return deferred.resolve(collectionCursor);
    }

    @Override
    public Promise<Playlist, Throwable, Void> getArtistTracks(Artist artist) {
        Deferred<Playlist, Throwable, Void> deferred = new ADeferredObject<>();
        return deferred.resolve(null);
    }

    public void addAlbumTracks(Album album, Playlist playlist) {
        mAlbumTracks.put(album, playlist);
    }

    @Override
    public Promise<Playlist, Throwable, Void> getAlbumTracks(final Album album) {
        Deferred<Playlist, Throwable, Void> deferred = new ADeferredObject<>();
        return deferred.resolve(mAlbumTracks.get(album));
    }

    @Override
    public Promise<Integer, Throwable, Void> getAlbumTrackCount(final Album album) {
        Deferred<Integer, Throwable, Void> deferred = new ADeferredObject<>();
        if (mAlbumTracks.get(album) != null) {
            return deferred.resolve(mAlbumTracks.get(album).size());
        }
        return deferred
                .reject(new Throwable("Couldn't find album " + album.getName() + " in collection"));
    }

    public void addArtistTopHits(Artist artist, Playlist playlist) {
        mArtistTopHits.put(artist, playlist);
    }

    /**
     * @return A {@link java.util.List} of all top hits {@link Track}s from the given Artist.
     */
    public Promise<Playlist, Throwable, Void> getArtistTopHits(final Artist artist) {
        Deferred<Playlist, Throwable, Void> deferred = new ADeferredObject<>();
        return deferred.resolve(mArtistTopHits.get(artist));
    }
}
