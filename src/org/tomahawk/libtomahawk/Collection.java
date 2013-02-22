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
package org.tomahawk.libtomahawk;

import java.util.List;

import org.tomahawk.libtomahawk.playlist.CustomPlaylist;
import org.tomahawk.libtomahawk.playlist.Playlist;

/**
 * This class represents a {@link Collection} of media.
 */
public abstract class Collection {

    public static final String COLLECTION_UPDATED = "org.tomahawk.libtomahawk.Collection.COLLECTION_UPDATED";

    /**
     * Get all {@link Artist}'s associated with this {@link Collection}.
     */
    public abstract List<Artist> getArtists();

    /**
     *  Get the {@link Artist} by giving the {@link Artist}'s ID
     *  @param id
     *  @return the {@link Artist} object*/
    public abstract Artist getArtistById(Long id);

    /**
     * Caches an artist inside the playlist
     * @param artist
     */
    public abstract void setCachedArtist(Artist artist);

    /**
     * @return the cached artist
     */
    public abstract Artist getCachedArtist();

    /**
     * Get all {@link Album}s from this {@link Collection}.
     */
    public abstract List<Album> getAlbums();

    /**
     *  Get the {@link Album} by giving the {@link Album}'s ID
     *  @param id
     *  @return the {@link Album} object*/
    public abstract Album getAlbumById(Long id);

    /**
     * Caches an album inside the playlist
     * @param album
     */
    public abstract void setCachedAlbum(Album album);

    /**
     * @return the cached album
     */
    public abstract Album getCachedAlbum();

    /**
     * Return a list of all {@link Track}s.
     */
    public abstract List<Track> getTracks();

    /**
     *  Get the {@link Track} by giving the {@link Track}'s ID
     *  @param id
     *  @return the {@link Track} object*/
    public abstract Track getTrackById(Long id);

    /**
     * Return a list of all {@link CustomPlaylist}s.
     */
    public abstract List<CustomPlaylist> getCustomPlaylists();

    /**
     *  Get the {@link CustomPlaylist} by giving the {@link CustomPlaylist}'s ID
     *  @param id
     *  @return the {@link CustomPlaylist} object*/
    public abstract CustomPlaylist getCustomPlaylistById(Long id);

    /**
     * Add a playlist to the collection
     */
    public abstract void addCustomPlaylist(long playlistId,CustomPlaylist customPlaylist);

    /**
     * Update this {@link Collection}'s content.
     */
    public abstract void update();

    /** @return the ID of this {@link Collection} object*/
    public abstract int getId();

    /**
     * Default constructor.
     */
    public Collection() {
    }

    /**
     * Returns whether this {@link Collection} is a {@link UserCollection}.
     */
    public boolean isLocal() {
        return false;
    }
}
