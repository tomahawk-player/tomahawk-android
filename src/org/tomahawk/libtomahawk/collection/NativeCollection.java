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

import org.jdeferred.Promise;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.BetterDeferredManager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

public abstract class NativeCollection extends Collection {

    private final boolean mIsLocal;

    protected NativeCollection(String id, String name, boolean isLocal) {
        super(id, name);

        mIsLocal = isLocal;
    }

    public boolean isLocal() {
        return mIsLocal;
    }

    @Override
    public Promise<Set<Query>, Throwable, Void> getQueries() {
        BetterDeferredManager dm = new BetterDeferredManager();
        return dm.when(new Callable<Set<Query>>() {
            @Override
            public Set<Query> call() throws Exception {
                return mQueries;
            }
        });
    }

    public void addArtist(Artist artist) {
        mArtists.add(artist);
    }

    @Override
    public Promise<Set<Artist>, Throwable, Void> getArtists() {
        BetterDeferredManager dm = new BetterDeferredManager();
        return dm.when(new Callable<Set<Artist>>() {
            @Override
            public Set<Artist> call() throws Exception {
                return mArtists;
            }
        });
    }

    public void addAlbumArtist(Artist artist) {
        mAlbumArtists.add(artist);
    }

    @Override
    public Promise<Set<Artist>, Throwable, Void> getAlbumArtists() {
        BetterDeferredManager dm = new BetterDeferredManager();
        return dm.when(new Callable<Set<Artist>>() {
            @Override
            public Set<Artist> call() throws Exception {
                return mAlbumArtists;
            }
        });
    }

    public void addAlbum(Album album) {
        mAlbums.add(album);
    }

    @Override
    public Promise<Set<Album>, Throwable, Void> getAlbums() {
        BetterDeferredManager dm = new BetterDeferredManager();
        return dm.when(new Callable<Set<Album>>() {
            @Override
            public Set<Album> call() throws Exception {
                return new HashSet<>(mAlbums);
            }
        });
    }

    public void addArtistAlbum(Artist artist, Album album) {
        if (mArtistAlbums.get(artist) == null) {
            mArtistAlbums.put(artist, new HashSet<Album>());
        }
        mArtistAlbums.get(artist).add(album);
    }

    @Override
    public Promise<Set<Album>, Throwable, Void> getArtistAlbums(final Artist artist,
            boolean onlyIfCached) {
        BetterDeferredManager dm = new BetterDeferredManager();
        return dm.when(new Callable<Set<Album>>() {
            @Override
            public Set<Album> call() throws Exception {
                Set<Album> albums = new HashSet<>();
                if (mArtistAlbums.get(artist) != null) {
                    albums.addAll(mArtistAlbums.get(artist));
                }
                return albums;
            }
        });
    }

    public void addAlbumTracks(Album album, Set<Query> queries) {
        mAlbumTracks.put(album, queries);
    }

    public void addAlbumTrack(Album album, Query query) {
        if (mAlbumTracks.get(album) == null) {
            mAlbumTracks.put(album, new HashSet<Query>());
        }
        mAlbumTracks.get(album).add(query);
    }

    @Override
    public Promise<Set<Query>, Throwable, Void> getAlbumTracks(final Album album,
            boolean onlyIfCached) {
        BetterDeferredManager dm = new BetterDeferredManager();
        return dm.when(new Callable<Set<Query>>() {
            @Override
            public Set<Query> call() throws Exception {
                Set<Query> queries = new HashSet<>();
                if (mAlbumTracks.get(album) != null) {
                    queries.addAll(mAlbumTracks.get(album));
                }
                return queries;
            }
        });
    }
}
