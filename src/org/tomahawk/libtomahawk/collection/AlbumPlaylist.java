/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
 * This class represents a {@link Playlist} including all the {@link Track}s on an {@link Album}.
 */
public class AlbumPlaylist extends Playlist {

    /**
     * Create an {@link AlbumPlaylist} from an {@link Album} and set the current {@link Query} to
     * the first {@link Query} of the given {@link Album}.
     *
     * @param id    the id of the {@link AlbumPlaylist} to construct
     * @param album The {@link Album} to construct the {@link Playlist} from
     * @return The constructed {@link AlbumPlaylist}
     */
    public static AlbumPlaylist fromAlbum(long id, Album album) {
        AlbumPlaylist pl = new AlbumPlaylist(id, album.getName());
        pl.setQueries(album.getQueries());
        pl.setCurrentQueryIndex(0);
        return pl;
    }

    /**
     * Creates an {@link AlbumPlaylist} from an {@link Album} and sets the current {@link Query} to
     * the {@link Query} at idx.
     *
     * @param id           the id of the {@link AlbumPlaylist} to construct
     * @param album        The {@link Album} to construct the {@link Playlist} from
     * @param currentQuery The current {@link Query} to be set
     * @return The constructed {@link AlbumPlaylist}
     */
    public static AlbumPlaylist fromAlbum(long id, Album album, Query currentQuery) {
        AlbumPlaylist pl = AlbumPlaylist.fromAlbum(id, album);
        pl.setCurrentQuery(currentQuery);
        return pl;
    }

    /**
     * Construct a new empty {@link AlbumPlaylist}.
     *
     * @param id   the id of the {@link AlbumPlaylist} to construct
     * @param name the name of the {@link AlbumPlaylist} to construct
     */
    protected AlbumPlaylist(long id, String name) {
        super(id, name);
    }
}
