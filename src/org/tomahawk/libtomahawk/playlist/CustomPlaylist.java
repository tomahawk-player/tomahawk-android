/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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

import java.util.ArrayList;

import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.TomahawkBaseAdapter;
import org.tomahawk.libtomahawk.Track;

public class CustomPlaylist extends Playlist implements TomahawkBaseAdapter.TomahawkListItem {
    private long mId;

    /**
     * Create a CustomPlaylist from a list of tracks.
     *
     * @return a reference to the constructed CustomPlaylist
     */
    public static CustomPlaylist fromTrackList(String name, ArrayList<Track> tracks) {
        CustomPlaylist pl = new CustomPlaylist(name);
        pl.setTracks(tracks);
        pl.setCurrentTrack(tracks.get(0));
        return pl;
    }

    /**
     * Creates a CustomPlaylist from a list of tracks and sets the current Track
     * to the given track
     *
     * @return a reference to the constructed CustomPlaylist
     */
    public static CustomPlaylist fromTrackList(String name, ArrayList<Track> tracks, Track currentTrack) {
        CustomPlaylist pl = new CustomPlaylist(name);
        pl.setTracks(tracks);
        pl.setCurrentTrack(currentTrack);
        return pl;
    }

    /**
     * Creates a CustomPlaylist from a list of tracks and sets the current Track index
     * to the given track index
     *
     * @return a reference to the constructed CustomPlaylist
     */
    public static CustomPlaylist fromTrackList(String name, ArrayList<Track> tracks, int currentTrackIndex) {
        CustomPlaylist pl = new CustomPlaylist(name);
        pl.setTracks(tracks);
        pl.setCurrentTrackIndex(currentTrackIndex);
        return pl;
    }

    /**
     * Construct a new empty CustomPlaylist.
     */
    protected CustomPlaylist(String name) {
        super(name);
    }

    public void setId(long id) {
        this.mId = id;
    }

    public long getId() {
        return mId;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public Artist getArtist() {
        return null;
    }

    @Override
    public Album getAlbum() {
        return null;
    }
}
