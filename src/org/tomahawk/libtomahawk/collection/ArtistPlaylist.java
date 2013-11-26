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
 * This class represents a {@link Playlist} including all the {@link Track}s from an {@link
 * Artist}.
 */
public class ArtistPlaylist extends Playlist {

    /**
     * Create an {@link ArtistPlaylist} from an {@link Artist} and set the current {@link Track} to
     * the first {@link Track} of the given {@link Artist}.
     *
     * @param id     the id of the {@link ArtistPlaylist} to construct
     * @param artist The {@link Artist} to construct the {@link Playlist} from
     * @return The constructed {@link ArtistPlaylist}
     */
    public static ArtistPlaylist fromArtist(long id, Artist artist) {
        ArtistPlaylist pl = new ArtistPlaylist(id, artist.getName());
        pl.setTracks(artist.getTracks());
        pl.setCurrentTrack(artist.getTracks().get(0));
        return pl;
    }

    /**
     * Creates an {@link ArtistPlaylist} from an {@link Artist} and sets the current {@link Track}
     * to the {@link Track} at idx.
     *
     * @param id           the id of the {@link ArtistPlaylist} to construct
     * @param artist       The {@link Artist} to construct the {@link Playlist} from
     * @param currentTrack The current {@link Track} to be set
     * @return The constructed {@link ArtistPlaylist}
     */
    public static ArtistPlaylist fromArtist(long id, Artist artist, Track currentTrack) {
        ArtistPlaylist pl = new ArtistPlaylist(id, artist.getName());
        pl.setTracks(artist.getTracks());
        pl.setCurrentTrack(currentTrack);
        return pl;
    }

    /**
     * Construct a new empty {@link ArtistPlaylist}.
     *
     * @param id   the id of the {@link ArtistPlaylist} to construct
     * @param name the name of the {@link ArtistPlaylist} to construct
     */
    protected ArtistPlaylist(long id, String name) {
        super(id, name);
    }

}
