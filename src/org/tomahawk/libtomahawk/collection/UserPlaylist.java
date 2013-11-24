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
package org.tomahawk.libtomahawk.collection;

import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;

import java.util.ArrayList;

/**
 * A {@link UserPlaylist} is a {@link Playlist} created by the user and stored in the database
 */
public class UserPlaylist extends Playlist implements TomahawkBaseAdapter.TomahawkListItem {

    public static final String LAST_USED_PLAYLIST_NAME = "Last used playlist";

    private long mId;

    /**
     * Create a {@link UserPlaylist} from a list of {@link Track}s.
     *
     * @return a reference to the constructed {@link UserPlaylist}
     */
    public static UserPlaylist fromTrackList(ArrayList<Track> tracks) {
        if (tracks == null) {
            tracks = new ArrayList<Track>();
        }
        UserPlaylist pl = new UserPlaylist();
        pl.setTracks(tracks);
        if (tracks.size() > 0) {
            pl.setCurrentTrack(tracks.get(0));
        }
        return pl;
    }

    /**
     * Create a {@link UserPlaylist} from a list of {@link Track}s.
     *
     * @return a reference to the constructed {@link UserPlaylist}
     */
    public static UserPlaylist fromTrackList(String name, ArrayList<Track> tracks) {
        if (tracks == null) {
            tracks = new ArrayList<Track>();
        }
        UserPlaylist pl = new UserPlaylist(name);
        pl.setTracks(tracks);
        if (tracks.size() > 0) {
            pl.setCurrentTrack(tracks.get(0));
        }
        return pl;
    }

    /**
     * Creates a {@link UserPlaylist} from a list of {@link Track}s and sets the current {@link
     * Track} to the given {@link Track}
     *
     * @return a reference to the constructed {@link UserPlaylist}
     */
    public static UserPlaylist fromTrackList(String name, ArrayList<Track> tracks,
            Track currentTrack) {
        if (tracks == null) {
            tracks = new ArrayList<Track>();
        }
        UserPlaylist pl = new UserPlaylist(name);
        pl.setTracks(tracks);
        pl.setCurrentTrack(currentTrack);
        return pl;
    }

    /**
     * Creates a {@link UserPlaylist} from a list of {@link Track}s and sets the current {@link
     * Track} index to the given {@link Track} index
     *
     * @return a reference to the constructed {@link UserPlaylist}
     */
    public static UserPlaylist fromTrackList(String name, ArrayList<Track> tracks,
            int currentTrackIndex) {
        if (tracks == null) {
            tracks = new ArrayList<Track>();
        }
        UserPlaylist pl = new UserPlaylist(name);
        pl.setTracks(tracks);
        pl.setCurrentTrackIndex(currentTrackIndex);
        return pl;
    }

    /**
     * Construct a new empty {@link UserPlaylist}.
     */
    protected UserPlaylist() {
        super();
    }

    /**
     * Construct a new empty {@link UserPlaylist}.
     */
    protected UserPlaylist(String name) {
        super(name);
    }

    /**
     * Set the object's id.
     *
     * @param id long containing the id
     */
    public void setId(long id) {
        this.mId = id;
    }

    /**
     * @return this object's id
     */
    public long getId() {
        return mId;
    }

    /**
     * @return this object' name
     */
    @Override
    public String getName() {
        return super.getName();
    }

    /**
     * @return always null. This method needed to comply to the {@link org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}
     * interface.
     */
    @Override
    public Artist getArtist() {
        return null;
    }

    /**
     * @return always null. This method needed to comply to the {@link org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}
     * interface.
     */
    @Override
    public Album getAlbum() {
        return null;
    }
}
