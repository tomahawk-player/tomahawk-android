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
package org.tomahawk.libtomahawk.playlist;

import java.io.Serializable;

import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.Track;

/**
 * Class that represents a list of songs from a given Artist.
 */
public class ArtistPlaylist extends Playlist implements Serializable {

    private static final long serialVersionUID = -5293315483953783524L;

    /**
     * Create an ArtistPlaylist from Artist.
     * 
     * @return
     */
    public static ArtistPlaylist fromArtist(Artist artist) {
        ArtistPlaylist pl = new ArtistPlaylist(artist.getName());
        pl.setTracks(artist.getTracks());
        pl.setCurrentTrack(artist.getTracks().get(0));
        return pl;
    }

    /**
     * Creates an ArtistPlaylist from Artist and sets the current Track to the
     * Track at idx.
     */
    public static ArtistPlaylist fromArtist(Artist artist, Track currentTrack) {
        ArtistPlaylist pl = new ArtistPlaylist(artist.getName());
        pl.setTracks(artist.getTracks());
        pl.setCurrentTrack(currentTrack);
        return pl;
    }

    /**
     * Constructor.
     * 
     * @param name
     */
    protected ArtistPlaylist(String name) {
        super(name);
    }

}
