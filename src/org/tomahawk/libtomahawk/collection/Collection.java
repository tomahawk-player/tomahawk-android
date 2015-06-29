/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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

import android.widget.ImageView;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Collection {

    private final String mId;

    private final String mName;

    protected Set<Album> mAlbums;

    protected Set<Artist> mArtists;

    protected Set<Artist> mAlbumArtists;

    protected Set<Query> mQueries;

    protected ConcurrentHashMap<Album, Set<Query>> mAlbumTracks
            = new ConcurrentHashMap<>();

    protected ConcurrentHashMap<Artist, Set<Album>> mArtistAlbums
            = new ConcurrentHashMap<>();

    public Collection(String id, String name) {
        mId = id;
        mName = name;
    }

    public void wipe() {
        mQueries.clear();
        mArtists.clear();
        mAlbums.clear();
        mAlbumTracks.clear();
        mArtistAlbums.clear();
    }

    /**
     * Load this {@link NativeCollection}'s icon into the given {@link ImageView}
     */
    public abstract void loadIcon(ImageView imageView, boolean grayOut);

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public abstract Promise<Set<Query>, Throwable, Void> getQueries();

    public abstract Promise<Set<Artist>, Throwable, Void> getArtists();

    public abstract Promise<Set<Artist>, Throwable, Void> getAlbumArtists();

    public abstract Promise<Set<Album>, Throwable, Void> getAlbums();

    public abstract Promise<Set<Album>, Throwable, Void> getArtistAlbums(Artist artist,
            boolean onlyIfCached);

    public abstract Promise<Set<Query>, Throwable, Void> getAlbumTracks(Album album,
            boolean onlyIfCached);

}
