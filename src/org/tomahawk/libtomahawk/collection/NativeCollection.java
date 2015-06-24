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
import org.tomahawk.libtomahawk.utils.BetterDeferredManager;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public abstract class NativeCollection extends Collection {

    private final boolean mIsLocal;

    protected ConcurrentHashMap<String, Album> mAlbums = new ConcurrentHashMap<>();

    protected ConcurrentHashMap<String, Artist> mArtists = new ConcurrentHashMap<>();

    protected ConcurrentHashMap<String, Artist> mAlbumArtists = new ConcurrentHashMap<>();

    protected ConcurrentHashMap<String, Query> mQueries = new ConcurrentHashMap<>();

    protected ConcurrentHashMap<Album, Set<Query>> mAlbumTracks
            = new ConcurrentHashMap<>();

    protected ConcurrentHashMap<Artist, Set<Album>> mArtistAlbums
            = new ConcurrentHashMap<>();

    protected final ConcurrentHashMap<Query, Long> mQueryTimeStamps
            = new ConcurrentHashMap<>();

    protected final ConcurrentHashMap<Artist, Long> mArtistTimeStamps
            = new ConcurrentHashMap<>();

    protected final ConcurrentHashMap<Album, Long> mAlbumTimeStamps
            = new ConcurrentHashMap<>();

    protected NativeCollection(String id, String name, boolean isLocal) {
        super(id, name);

        mIsLocal = isLocal;
    }

    public boolean isLocal() {
        return mIsLocal;
    }

    public void wipe() {
        mQueries = new ConcurrentHashMap<>();
        mArtists = new ConcurrentHashMap<>();
        mAlbums = new ConcurrentHashMap<>();
        mAlbumTracks = new ConcurrentHashMap<>();
        mArtistAlbums = new ConcurrentHashMap<>();
    }

    public void addQuery(Query query, long addedTimeStamp) {
        if (!TextUtils.isEmpty(query.getName()) && !mQueries.containsKey(query.getCacheKey())) {
            mQueries.put(query.getCacheKey(), query);
        }
        if (addedTimeStamp > 0) {
            if (mAlbumTimeStamps.get(query.getAlbum()) == null
                    || mAlbumTimeStamps.get(query.getAlbum()) < addedTimeStamp) {
                mAlbumTimeStamps.put(query.getAlbum(), addedTimeStamp);
            }
            if (mArtistTimeStamps.get(query.getArtist()) == null
                    || mArtistTimeStamps.get(query.getArtist()) < addedTimeStamp) {
                mArtistTimeStamps.put(query.getArtist(), addedTimeStamp);
            }
            mQueryTimeStamps.put(query, addedTimeStamp);
        }
    }

    @Override
    public Promise<List<Query>, Throwable, Void> getQueries() {
        BetterDeferredManager dm = new BetterDeferredManager();
        return dm.when(new Callable<List<Query>>() {
            @Override
            public List<Query> call() throws Exception {
                return new ArrayList<>(mQueries.values());
            }
        });
    }

    public void addArtist(Artist artist) {
        mArtists.put(artist.getCacheKey(), artist);
    }

    @Override
    public Promise<List<Artist>, Throwable, Void> getArtists() {
        BetterDeferredManager dm = new BetterDeferredManager();
        return dm.when(new Callable<List<Artist>>() {
            @Override
            public List<Artist> call() throws Exception {
                return new ArrayList<>(mArtists.values());
            }
        });
    }

    public void addAlbumArtist(Artist artist) {
        mAlbumArtists.put(artist.getCacheKey(), artist);
    }

    @Override
    public Promise<List<Artist>, Throwable, Void> getAlbumArtists() {
        BetterDeferredManager dm = new BetterDeferredManager();
        return dm.when(new Callable<List<Artist>>() {
            @Override
            public List<Artist> call() throws Exception {
                return new ArrayList<>(mAlbumArtists.values());
            }
        });
    }

    public void addAlbum(Album album) {
        mAlbums.put(album.getCacheKey(), album);
    }

    @Override
    public Promise<List<Album>, Throwable, Void> getAlbums() {
        BetterDeferredManager dm = new BetterDeferredManager();
        return dm.when(new Callable<List<Album>>() {
            @Override
            public List<Album> call() throws Exception {
                return new ArrayList<>(mAlbums.values());
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
    public Promise<List<Album>, Throwable, Void> getArtistAlbums(final Artist artist) {
        BetterDeferredManager dm = new BetterDeferredManager();
        return dm.when(new Callable<List<Album>>() {
            @Override
            public List<Album> call() throws Exception {
                List<Album> albums = new ArrayList<>();
                if (mArtistAlbums.get(artist) != null) {
                    albums.addAll(mArtistAlbums.get(artist));
                }
                return albums;
            }
        });
    }

    public Promise<Boolean, Throwable, Void> hasArtistAlbums(Artist artist) {
        final Deferred<Boolean, Throwable, Void> deferred = new ADeferredObject<>();
        return deferred
                .resolve(mArtistAlbums.get(artist) != null && mArtistAlbums.get(artist).size() > 0);
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
    public Promise<List<Query>, Throwable, Void> getAlbumTracks(final Album album) {
        BetterDeferredManager dm = new BetterDeferredManager();
        return dm.when(new Callable<List<Query>>() {
            @Override
            public List<Query> call() throws Exception {
                List<Query> queries = new ArrayList<>();
                if (mAlbumTracks.get(album) != null) {
                    queries.addAll(mAlbumTracks.get(album));
                }
                return queries;
            }
        });
    }

    public Promise<Boolean, Throwable, Void> hasAlbumTracks(Album album) {
        Deferred<Boolean, Throwable, Void> deferred = new ADeferredObject<>();
        return deferred
                .resolve(mAlbumTracks.get(album) != null && mAlbumTracks.get(album).size() > 0);
    }

    public ConcurrentHashMap<Query, Long> getQueryTimeStamps() {
        return mQueryTimeStamps;
    }

    public ConcurrentHashMap<Artist, Long> getArtistTimeStamps() {
        return mArtistTimeStamps;
    }

    public ConcurrentHashMap<Album, Long> getAlbumTimeStamps() {
        return mAlbumTimeStamps;
    }
}
