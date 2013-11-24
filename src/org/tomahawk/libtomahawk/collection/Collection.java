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

import java.util.List;

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
     * Get all {@link Artist}'s associated with this {@link Collection}.
     */
    public abstract List<Artist> getArtists();

    /**
     * Get the {@link Artist} by giving the {@link Artist}'s ID
     *
     * @return the {@link Artist} object
     */
    public abstract Artist getArtistById(Long id);

    /**
     * Caches an {@link Artist} inside the playlist
     */
    public abstract void setCachedArtist(Artist artist);

    /**
     * @return the cached {@link Artist}
     */
    public abstract Artist getCachedArtist();

    /**
     * Get all {@link Album}s from this {@link Collection}.
     */
    public abstract List<Album> getAlbums();

    /**
     * Get the {@link Album} by giving the {@link Album}'s ID
     *
     * @return the {@link Album} object
     */
    public abstract Album getAlbumById(Long id);

    /**
     * Caches an {@link Album} inside the {@link Playlist}
     */
    public abstract void setCachedAlbum(Album album);

    /**
     * @return the cached {@link Album}
     */
    public abstract Album getCachedAlbum();

    /**
     * Return a list of all {@link Track}s.
     */
    public abstract List<Track> getTracks();

    /**
     * Get the {@link Track} by giving the {@link Track}'s ID
     *
     * @return the {@link Track} object
     */
    public abstract Track getTrackById(Long id);

    /**
     * Return a list of all {@link UserPlaylist}s.
     */
    public abstract List<UserPlaylist> getCustomPlaylists();

    /**
     * Get the {@link UserPlaylist} by giving the {@link UserPlaylist}'s ID
     *
     * @return the {@link UserPlaylist} object
     */
    public abstract UserPlaylist getCustomPlaylistById(Long id);

    /**
     * Add a {@link Playlist} to the {@link Collection}
     */
    public abstract void addCustomPlaylist(long playlistId, UserPlaylist userPlaylist);

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
