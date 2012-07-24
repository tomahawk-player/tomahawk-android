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

import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;

import org.tomahawk.libtomahawk.Track;

/**
 * This class represents an abstract Playlist.
 */
public abstract class Playlist implements PlayableInterface {

    private ArrayList<Track> mTracks;
    private ListIterator<Track> mTrackIterator;
    private Track mCurrentTrack;

    /**
     * Create a playlist with a list of empty tracks.
     */
    protected Playlist() {
        setTracks(new ArrayList<Track>());
    }

    /**
     * Set the list of Tracks for this playlist to tracks.
     */
    @Override
    public void setTracks(Collection<Track> tracks) {
        mTracks = (ArrayList<Track>) tracks;
        mTrackIterator = (ListIterator<Track>) mTracks.iterator();

        if (mTrackIterator.hasNext())
            mCurrentTrack = mTrackIterator.next();
        else
            mCurrentTrack = null;
    }

    /**
     * Return the current track for this playlist.
     */
    @Override
    public Track getCurrentTrack() {
        return mCurrentTrack;
    }

    /**
     * Get the next Track from this Playlist.
     */
    @Override
    public Track getNextTrack() {

        if (mTrackIterator.hasNext()) {
            mCurrentTrack = mTrackIterator.next();
            return mCurrentTrack;
        }

        return null;
    }

    /**
     * Get the previous Track from this Playlist.
     */
    @Override
    public Track getPreviousTrack() {

        if (mTrackIterator.hasPrevious()) {
            mCurrentTrack = mTrackIterator.previous();
            return mCurrentTrack;
        }

        return null;
    }

    /**
     * Get track at pos i in this Playlist.
     */
    @Override
    public Track getTrackAtPos(int i) {
        if (i < mTracks.size())
            return mTracks.get(i);

        return null;
    }

    /**
     * Get the first Track from this Playlist.
     */
    @Override
    public Track getFirstTrack() {
        if (mTracks.isEmpty())
            return null;

        return mTracks.get(0);
    }

    /**
     * Get the last Track from this Playlist.
     */
    @Override
    public Track getLastTrack() {

        if (mTracks.isEmpty())
            return null;

        return mTracks.get(mTracks.size() - 1);
    }
}
