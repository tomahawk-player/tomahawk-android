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

import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Track;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class represents a Playlist including all the Tracks on an Album.
 */
public class AlbumPlaylist extends Playlist {

    /**
     * Creator used to parcel AlbumPlaylist inside Intent's.
     */
    @SuppressWarnings("rawtypes")
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Playlist createFromParcel(Parcel in) {
            return new AlbumPlaylist(in);
        }

        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }
    };

    /**
     * Constructs a new AlbumPlaylist from the Parcel.
     */
    protected AlbumPlaylist(Parcel in) {
        super(in);
    }
    /**
     * Create an AlbumPlaylist from Album.
     * 
     * @return
     */
    public static AlbumPlaylist fromAlbum(Album album) {
        AlbumPlaylist pl = new AlbumPlaylist(album.getName());
        pl.setTracks(album.getTracks());
        pl.setCurrentTrack(album.getTracks().get(0));
        return pl;
    }

    /**
     * Creates an AlbumPlaylist from Album and sets the current Track to the
     * Track at idx.
     */
    public static AlbumPlaylist fromAlbum(Album album, Track currentTrack) {
        AlbumPlaylist pl = new AlbumPlaylist(album.getName());
        pl.setTracks(album.getTracks());
        pl.setCurrentTrack(currentTrack);
        return pl;
    }

    /**
     * Construct a new empty AlbumPlaylist.
     */
    protected AlbumPlaylist(String name) {
        super(name);
    }
}
