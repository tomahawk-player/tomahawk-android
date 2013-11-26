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
package org.tomahawk.libtomahawk.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class represents an abstract {@link Playlist}.
 */
public abstract class Playlist implements Playable {

    private long mId;

    private String mName;

    private ArrayList<Track> mTracks;

    private ArrayList<Track> mShuffledTracks;

    private int mCurrentTrackIndex;

    private boolean mShuffled;

    private boolean mRepeating;

    /**
     * Create a {@link Playlist} with a list of empty {@link Track}s.
     */
    protected Playlist(long id) {
        mId = id;
        mShuffled = false;
        mRepeating = false;
        setTracks(new ArrayList<Track>());
    }

    /**
     * Create a {@link Playlist} with a list of empty {@link Track}s.
     *
     * @param name {@link String} containing the name of the to be created {@link Playlist}
     */
    protected Playlist(long id, String name) {
        mId = id;
        mName = name;
        mShuffled = false;
        mRepeating = false;
        setTracks(new ArrayList<Track>());
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
    public String getName() {
        return mName;
    }

    /**
     * Set the name of this object
     *
     * @param name the name to be set
     */
    public void setName(String name) {
        this.mName = name;
    }

    /**
     * Set this {@link Playlist}'s {@link Track}s
     */
    @Override
    public void setTracks(Collection<Track> tracks) {
        mTracks = (ArrayList<Track>) tracks;

        if (mTracks != null && !mTracks.isEmpty()) {
            mCurrentTrackIndex = 0;
        } else {
            mCurrentTrackIndex = -1;
        }
    }

    /**
     * Set the current {@link Track} of this {@link Playlist}
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

    /**
     * Set the current {@link Track} index
     *
     * @param currentTrackIndex int containig the {@link Track}'s index
     */
    public void setCurrentTrackIndex(int currentTrackIndex) {
        mCurrentTrackIndex = currentTrackIndex;
    }

    /**
     * @return the current {@link Track}
     */
    @Override
    public Track getCurrentTrack() {
        List<Track> tracks = mShuffled ? mShuffledTracks : mTracks;
        if (tracks != null && mCurrentTrackIndex >= 0 && mCurrentTrackIndex < tracks.size()) {
            return tracks.get(mCurrentTrackIndex);
        }
        return null;
    }

    /**
     * @return the current {@link Track}'s index
     */
    public int getCurrentTrackIndex() {
        return mCurrentTrackIndex;
    }

    /**
     * @return the next {@link Track}
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

    /**
     * @return the previous {@link Track}
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

    /**
     * Get the {@link Track} at the given position
     */
    @Override
    public Track getTrackAtPos(int i) {
        if (i >= 0 && i < (mShuffled ? mShuffledTracks.size() : mTracks.size())) {
            mCurrentTrackIndex = i;
            return mShuffled ? mShuffledTracks.get(i) : mTracks.get(i);
        }
        return null;
    }

    /**
     * @return the first {@link Track} of this playlist
     */
    @Override
    public Track getFirstTrack() {
        if (mShuffled ? mShuffledTracks.isEmpty() : mTracks.isEmpty()) {
            return null;
        }

        return mShuffled ? mShuffledTracks.get(0) : mTracks.get(0);
    }

    /**
     * @return the last {@link Track} of this playlist
     */
    @Override
    public Track getLastTrack() {
        if (mShuffled ? mShuffledTracks.isEmpty() : mTracks.isEmpty()) {
            return null;
        }

        return mShuffled ? mShuffledTracks.get(mShuffledTracks.size() - 1)
                : mTracks.get(mTracks.size() - 1);
    }

    /**
     * @return this {@link Playlist}'s name
     */
    @Override
    public String toString() {
        return mName;
    }

    /**
     * @return true, if the {@link Playlist} has a next {@link Track}, otherwise false
     */
    public boolean hasNextTrack() {
        return peekNextTrack() != null;
    }

    /**
     * @return true, if the {@link Playlist} has a previous {@link Track}, otherwise false
     */
    public boolean hasPreviousTrack() {
        return peekPreviousTrack() != null;
    }

    /**
     * Returns the next {@link Track} but does not update the internal {@link Track} iterator.
     *
     * @return Returns next {@link Track}. Returns null if there is none.
     */
    public Track peekNextTrack() {
        List<Track> tracks = mShuffled ? mShuffledTracks : mTracks;
        if (mCurrentTrackIndex + 1 < tracks.size()) {
            return tracks.get(mCurrentTrackIndex + 1);
        } else if (mRepeating) {
            return getFirstTrack();
        }
        return null;
    }

    /**
     * Returns the previous {@link Track} but does not update the internal {@link Track} iterator.
     *
     * @return Returns previous {@link Track}. Returns null if there is none.
     */
    public Track peekPreviousTrack() {
        if (mCurrentTrackIndex - 1 >= 0) {
            List<Track> tracks = mShuffled ? mShuffledTracks : mTracks;
            return tracks.get(mCurrentTrackIndex - 1);
        } else if (mRepeating) {
            return getLastTrack();
        }
        return null;
    }

    /**
     * Returns the {@link Track} at the given position but does not update the internal {@link
     * Track} iterator.
     *
     * @return Returns the {@link Track} at the given position. Returns null if there is none.
     */
    public Track peekTrackAtPos(int i) {
        if (i >= 0 && i < (mShuffled ? mShuffledTracks.size() : mTracks.size())) {
            return mShuffled ? mShuffledTracks.get(i) : mTracks.get(i);
        }
        return null;
    }

    /**
     * Set this {@link Playlist} to shuffle mode.
     */
    @SuppressWarnings("unchecked")
    public void setShuffled(boolean shuffled) {
        Track oldCurrentTrack = getCurrentTrack();
        mShuffled = shuffled;
        int i = 0;

        if (shuffled) {
            mShuffledTracks = (ArrayList<Track>) mTracks.clone();
            Collections.shuffle(mShuffledTracks);
        } else {
            mShuffledTracks = null;
        }

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
     * Set this {@link Playlist} to repeat mode.
     */
    public void setRepeating(boolean repeating) {
        mRepeating = repeating;
    }

    /**
     * Return whether this {@link Playlist} is currently shuffled.
     */
    public boolean isShuffled() {
        return mShuffled;
    }

    /**
     * Return whether this {@link Playlist} is currently repeating.
     */
    public boolean isRepeating() {
        return mRepeating;
    }

    /**
     * Return the current count of tracks in the {@link Playlist}
     */
    public int getCount() {
        return mTracks.size();
    }

    /**
     * Return all tracks in the {@link Playlist}
     */
    public ArrayList<Track> getTracks() {
        return mShuffled ? mShuffledTracks : mTracks;
    }

    /**
     * Add an {@link ArrayList} of {@link Track}s at the given position
     */
    public void addTracks(int position, ArrayList<Track> tracks) {
        (mShuffled ? mShuffledTracks : mTracks).addAll(position, tracks);
    }

    /**
     * Append an {@link ArrayList} of {@link Track}s at the end of this playlist
     */
    public void addTracks(ArrayList<Track> tracks) {
        (mShuffled ? mShuffledTracks : mTracks).addAll(tracks);
    }

    /**
     * Remove the {@link Track} at the given position from this playlist
     */
    public void deleteTrackAtPos(int position) {
        if (mShuffledTracks != null) {
            (!mShuffled ? mShuffledTracks : mTracks)
                    .remove((mShuffled ? mShuffledTracks : mTracks).get(position));
        }
        (mShuffled ? mShuffledTracks : mTracks).remove(position);
        if (mCurrentTrackIndex > position) {
            mCurrentTrackIndex--;
        }
    }
}
