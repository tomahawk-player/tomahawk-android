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

/**
 * A {@link CollectionPlaylist} is a {@link Playlist} containing every from {@link Track} from an
 * entire {@link Collection}
 */
public class CollectionPlaylist extends Playlist {

    /**
     * Create a {@link CollectionPlaylist} from {@link Collection}.
     */
    public static CollectionPlaylist fromCollection(long id, Collection coll) {
        CollectionPlaylist pl = new CollectionPlaylist(id, coll.toString());
        pl.setTracks(coll.getTracks());
        pl.setCurrentTrack(coll.getTracks().get(0));
        return pl;
    }

    /**
     * Creates a {@link CollectionPlaylist} from {@link Collection} and sets the current {@link
     * Track} to the {@link Track} at idx.
     */
    public static CollectionPlaylist fromCollection(long id, Collection coll, Track currentTrack) {
        CollectionPlaylist pl = new CollectionPlaylist(id, coll.toString());
        pl.setTracks(coll.getTracks());
        pl.setCurrentTrack(currentTrack);
        return pl;
    }

    /**
     * Construct a new empty {@link CollectionPlaylist}.
     *
     * @param id the id of the {@link CollectionPlaylist} to construct
     * @param id the name of the {@link CollectionPlaylist} to construct
     */
    protected CollectionPlaylist(long id, String name) {
        super(id, name);
    }
}
