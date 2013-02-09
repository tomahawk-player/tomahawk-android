/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.tomahawk.libtomahawk.Track;

/**
 * This class represents an abstract Playlist.
 */
public abstract class Playlist implements Playable {

    private String mName;
    private ArrayList<Track> mTracks;
    private ArrayList<Track> mShuffledTracks;

    private int mCurrentTrackIndex;
    private boolean mShuffled;
    private boolean mRepeating;

    /**
     * Create a playlist with a list of empty tracks.
     */
    protected Playlist(String name) {
        mName = name;
        mShuffled = false;
        mRepeating = false;
        setTracks(new ArrayList<Track>());
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.playlist.Playable#setTracks(java.util.Collection)
     */
    @Override
    public void setTracks(Collection<Track> tracks) {
        mTracks = (ArrayList<Track>) tracks;

        if (mTracks != null && !mTracks.isEmpty()) {
            mCurrentTrackIndex = 0;
        } else
            mCurrentTrackIndex = -1;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.playlist.Playable#setCurrentTrack(org.tomahawk.libtomahawk.Track)
     */
    @Override
    public void setCurrentTrack(Track newtrack) {
        List<Track> tracks = mShuffled ? mShuffledTracks : mTracks;
        int i = 0;
        while (i < tracks.size()) {
            Track track = tracks.get(i);
            if (newtrack.getId() == track.getId()) {
                mCurrentTrackIndex = i;
                break;
            }
            i++;
        }
    }

    public void setCurrentTrackIndex(int currentTrackIndex) {
        mCurrentTrackIndex = currentTrackIndex;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.playlist.Playable#getCurrentTrack()
     */
    @Override
    public Track getCurrentTrack() {
        List<Track> tracks = mShuffled ? mShuffledTracks : mTracks;
        if (tracks != null && mCurrentTrackIndex >= 0 && mCurrentTrackIndex < tracks.size())
            return tracks.get(mCurrentTrackIndex);
        return null;
    }

    public int getCurrentTrackIndex() {
        return mCurrentTrackIndex;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.playlist.Playable#getNextTrack()
     */
    @Override
    public Track getNextTrack() {
        List<Track> tracks = mShuffled ? mShuffledTracks : mTracks;
        if (mCurrentTrackIndex + 1 < tracks.size()) {
            Track track = tracks.get(mCurrentTrackIndex + 1);
            mCurrentTrackIndex = mCurrentTrackIndex + 1;
            return track;
        } else if (mRepeating) {
            mCurrentTrackIndex = 0;
            return getFirstTrack();
        }
        return null;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.playlist.Playable#getPreviousTrack()
     */
    @Override
    public Track getPreviousTrack() {
        List<Track> tracks = mShuffled ? mShuffledTracks : mTracks;
        if (mCurrentTrackIndex - 1 >= 0) {
            Track track = tracks.get(mCurrentTrackIndex - 1);
            mCurrentTrackIndex = mCurrentTrackIndex - 1;
            return track;
        } else if (mRepeating) {
            mCurrentTrackIndex = tracks.size() - 1;
            return getLastTrack();
        }
        return null;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.playlist.Playable#getTrackAtPos(int)
     */
    @Override
    public Track getTrackAtPos(int i) {
        if (i >= 0 && i < (mShuffled ? mShuffledTracks.size() : mTracks.size())) {
            mCurrentTrackIndex = i;
            return mShuffled ? mShuffledTracks.get(i) : mTracks.get(i);
        }
        return null;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.playlist.Playable#getFirstTrack()
     */
    @Override
    public Track getFirstTrack() {
        if (mShuffled ? mShuffledTracks.isEmpty() : mTracks.isEmpty())
            return null;

        return mShuffled ? mShuffledTracks.get(0) : mTracks.get(0);
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.playlist.Playable#getLastTrack()
     */
    @Override
    public Track getLastTrack() {
        if (mShuffled ? mShuffledTracks.isEmpty() : mTracks.isEmpty())
            return null;

        return mShuffled ? mShuffledTracks.get(mShuffledTracks.size() - 1) : mTracks.get(mTracks.size() - 1);
    }

    /**
     * Return the name of this Playlist.
     */
    @Override
    public String toString() {
        return mName;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.playlist.Playable#hasNextTrack()
     */
    public boolean hasNextTrack() {
        return peekNextTrack() != null ? true : false;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.playlist.Playable#hasPreviousTrack()
     */
    public boolean hasPreviousTrack() {
        return peekPreviousTrack() != null ? true : false;
    }

    /**
     * Returns the next Track but does not update the internal Track iterator.
     *
     * @return Returns next Track. Returns null if there is none.
     */
    public Track peekNextTrack() {
        List<Track> tracks = mShuffled ? mShuffledTracks : mTracks;
        if (mCurrentTrackIndex + 1 < tracks.size()) {
            Track track = tracks.get(mCurrentTrackIndex + 1);
            return track;
        } else if (mRepeating) {
            return getFirstTrack();
        }
        return null;
    }

    /**
     * Returns the previous Track but does not update the internal Track
     * iterator.
     *
     * @return Returns previous Track. Returns null if there is none.
     */
    public Track peekPreviousTrack() {
        if (mCurrentTrackIndex - 1 >= 0) {
            List<Track> tracks = mShuffled ? mShuffledTracks : mTracks;
            Track track = tracks.get(mCurrentTrackIndex - 1);
            return track;
        } else if (mRepeating) {
            return getLastTrack();
        }
        return null;
    }

    /**
     * Returns the Track at the given position but does not update the internal Track
     * iterator.
     *
     * @return Returns the Track at the given position. Returns null if there is none.
     */
    public Track peekTrackAtPos(int i) {
        if (i >= 0 && i < (mShuffled ? mShuffledTracks.size() : mTracks.size())) {
            return mShuffled ? mShuffledTracks.get(i) : mTracks.get(i);
        }
        return null;
    }

    /**
     * Set this playlist to shuffle mode.
     *
     * @param shuffled
     */
    @SuppressWarnings("unchecked")
    public void setShuffled(boolean shuffled) {
        Track oldCurrentTrack = getCurrentTrack();
        mShuffled = shuffled;
        int i = 0;

        if (shuffled) {
            mShuffledTracks = (ArrayList<Track>) mTracks.clone();
            Collections.shuffle(mShuffledTracks);
        } else
            mShuffledTracks = null;

        List<Track> tracks = mShuffled ? mShuffledTracks : mTracks;
        while (i < tracks.size()) {
            if (oldCurrentTrack == tracks.get(i)) {
                mCurrentTrackIndex = i;
                break;
            }
            i++;
        }
    }

    /**
     * Set this playlist to repeat mode.
     *
     * @param repeating
     */
    public void setRepeating(boolean repeating) {
        mRepeating = repeating;
    }

    /**
     * Return whether this Playlist is currently shuffled.
     *
     * @return
     */
    public boolean isShuffled() {
        return mShuffled;
    }

    /**
     * Return whether this Playlist is currently repeating.
     *
     * @return
     */
    public boolean isRepeating() {
        return mRepeating;
    }

    /**
     * Return the current count of tracks in the playlist
     *
     * @return
     */
    public int getCount() {
        return mTracks.size();
    }

    /**
     * Return the position of the currently played track inside the playlist
     *
     * @return
     */
    public int getPosition() {
        return mCurrentTrackIndex;
    }

    /**
     * Return all tracks in the playlist
     *
     * @return
     */
    public ArrayList<Track> getTracks() {
        return mShuffled ? mShuffledTracks : mTracks;
    }
}
