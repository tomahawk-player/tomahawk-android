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

import org.jdeferred.Deferred;
import org.tomahawk.libtomahawk.resolver.Query;

import android.widget.ImageView;

import java.util.Set;

public abstract class Collection {

    private final String mId;

    private final String mName;

    public Collection(String id, String name) {
        mId = id;
        mName = name;
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

    public Deferred<Set<Query>, String, Object> getQueries() {
        return getQueries(true);
    }

    public abstract Deferred<Set<Query>, String, Object> getQueries(boolean sorted);

    public Deferred<Set<Artist>, String, Object> getArtists() {
        return getArtists(true);
    }

    public abstract Deferred<Set<Artist>, String, Object> getArtists(boolean sorted);

    public Deferred<Set<Album>, String, Object> getAlbums() {
        return getAlbums(true);
    }

    public abstract Deferred<Set<Album>, String, Object> getAlbums(boolean sorted);

    public abstract Deferred<Set<Album>, String, Object> getArtistAlbums(Artist artist,
            boolean sorted);

    public abstract Deferred<Boolean, String, Object> hasArtistAlbums(Artist artist);

    public abstract Deferred<Set<Query>, String, Object> getAlbumTracks(Album album,
            boolean sorted);

    public abstract Deferred<Boolean, String, Object> hasAlbumTracks(Album album);

}
