/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
 *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
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

import java.util.ArrayList;

/**
 * This class represents a {@link Collection} of media.
 */
public abstract class Collection {

    public static final String COLLECTION_UPDATED
            = "org.tomahawk.tomahawk_android.COLLECTION_UPDATED";

    /**
     * Default constructor.
     */
    public Collection() {
    }

    /**
     * Caches an {@link Artist} inside the playlist
     */
    public abstract void setCachedArtist(Artist artist);

    /**
     * @return the cached {@link Artist}
     */
    public abstract Artist getCachedArtist();

    /**
     * Caches an {@link Album} inside the {@link Playlist}
     */
    public abstract void setCachedAlbum(Album album);

    /**
     * @return the cached {@link Album}
     */
    public abstract Album getCachedAlbum();

    /**
     * Return a list of all {@link org.tomahawk.libtomahawk.resolver.Query}s.
     */
    public abstract ArrayList<Query> getQueries();

    /**
     * Return a list of all {@link UserPlaylist}s.
     */
    public abstract ArrayList<UserPlaylist> getUserPlaylists();

    /**
     * Get the {@link UserPlaylist} by giving the {@link UserPlaylist}'s ID
     *
     * @return the {@link UserPlaylist} object
     */
    public abstract UserPlaylist getUserPlaylistById(long id);

    /**
     * Update this {@link Collection}'s content.
     */
    public abstract void update();

    /**
     * @return the ID of this {@link Collection} object
     */
    public abstract int getId();

    /**
     * Returns whether this {@link Collection} is a {@link UserCollection}.
     */
    public boolean isLocal() {
        return false;
    }
}
