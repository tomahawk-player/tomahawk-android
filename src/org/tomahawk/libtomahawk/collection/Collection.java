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

import java.util.List;
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

    public abstract Promise<Set<Query>, Throwable, Void> getQueries();

    public abstract Promise<Set<Artist>, Throwable, Void> getArtists();

    public abstract Promise<Set<Artist>, Throwable, Void> getAlbumArtists();

    public abstract Promise<Set<Album>, Throwable, Void> getAlbums();

    public abstract Promise<List<Album>, Throwable, Void> getArtistAlbums(Artist artist);

    public abstract Promise<Boolean, Throwable, Void> hasArtistAlbums(Artist artist);

    public abstract Promise<List<Query>, Throwable, Void> getAlbumTracks(Album album);

    public abstract Promise<Boolean, Throwable, Void> hasAlbumTracks(Album album);

}
