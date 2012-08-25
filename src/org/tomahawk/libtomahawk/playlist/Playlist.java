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
import java.util.Collections;
import java.util.ListIterator;

import org.tomahawk.libtomahawk.Track;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class represents an abstract Playlist.
 */
public abstract class Playlist implements Playable, Parcelable {

    private String mName;
    private ArrayList<Track> mTracks;
    private ArrayList<Track> mShuffledTracks;

    private ListIterator<Track> mTrackIterator;
    private Track mCurrentTrack;
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

    public Playlist(Parcel in) {
        mName = in.readString();
        setTracks((ArrayList<Track>) in.readSerializable());
        setCurrentTrack((Track) in.readSerializable());
        setShuffled(in.readByte() == 1);
        setRepeating(in.readByte() == 1);

        if (mShuffled)
            mTrackIterator = mShuffledTracks.listIterator();
        else
            mTrackIterator = mTracks.listIterator();

        while (mTrackIterator.hasNext()) {
            if (mTrackIterator.next().getId() == getCurrentTrack().getId())
                break;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeSerializable(mTracks);
        dest.writeSerializable(mCurrentTrack);
        dest.writeByte((byte) (mShuffled ? 1 : 0));
        dest.writeByte((byte) (mRepeating ? 1 : 0));
    }

    /**
     * Set the list of Tracks for this Playlist to tracks.
     */
    @Override
    public void setTracks(Collection<Track> tracks) {
        mTracks = (ArrayList<Track>) tracks;
        mTrackIterator = mTracks.listIterator();

        if (mTrackIterator.hasNext())
            mCurrentTrack = mTrackIterator.next();
        else
            mCurrentTrack = null;
    }

    /**
     * Set the current Track for this Playlist.
     * 
     * If Track cannot be found the current Track stays the same.
     */
    @Override
    public void setCurrentTrack(Track newtrack) {

        for (Track track : mShuffled ? mShuffledTracks : mTracks)
            if (newtrack.getId() == track.getId())
                mCurrentTrack = track;

    }

    /**
     * Return the current Track for this Playlist.
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
            Track track = mTrackIterator.next();
            if (track == mCurrentTrack && mTrackIterator.hasNext())
                mCurrentTrack = mTrackIterator.next();
            else if (track == mCurrentTrack && !mTrackIterator.hasNext() && mRepeating)
                setCurrentTrack(mTracks.get(0));
            else if (track == mCurrentTrack)
                mCurrentTrack = null;
            else
                mCurrentTrack = track;

            return mCurrentTrack;
        }

        if (mRepeating) {
            setCurrentTrack(mTracks.get(0));
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
            Track track = mTrackIterator.previous();
            if (track == mCurrentTrack && mTrackIterator.hasPrevious())
                mCurrentTrack = mTrackIterator.previous();
            else if (track == mCurrentTrack && !mTrackIterator.hasPrevious() && mRepeating)
                setCurrentTrack(mTracks.get(mTracks.size() - 1));
            else
                mCurrentTrack = track;

            return mCurrentTrack;
        }

        if (mRepeating) {
            setCurrentTrack(mTracks.get(mTracks.size() - 1));
            return mCurrentTrack;
        }

        return null;
    }

    /**
     * Get track at pos i in this Playlist.
     */
    @Override
    public Track getTrackAtPos(int i) {
        if (i < (mShuffled ? mShuffledTracks.size() : mTracks.size()))
            return mShuffled ? mShuffledTracks.get(i) : mTracks.get(i);

        return null;
    }

    /**
     * Get the first Track from this Playlist.
     */
    @Override
    public Track getFirstTrack() {
        if (mShuffled ? mShuffledTracks.isEmpty() : mTracks.isEmpty())
            return null;

        return mShuffled ? mShuffledTracks.get(0) : mTracks.get(0);
    }

    /**
     * Get the last Track from this Playlist.
     */
    @Override
    public Track getLastTrack() {

        if (mShuffled ? mTracks.isEmpty() : mTracks.isEmpty())
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

    /**
     * Returns true if the PlayableInterface has a next Track.
     * 
     * @return
     */
    public boolean hasNextTrack() {
        return peekNextTrack() != null ? true : false;
    }

    /**
     * Returns true if the PlayableInterface has a previous Track.
     * 
     * @return
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

        Track track = null;
        if (mTrackIterator.hasNext()) {

            track = mTrackIterator.next();
            if (track == mCurrentTrack && mTrackIterator.hasNext())
                track = mTrackIterator.next();
            else if (track == mCurrentTrack && !mTrackIterator.hasNext() && mRepeating)
                track = mTracks.get(0);
            else if (track == mCurrentTrack)
                track = null;

            mTrackIterator.previous();
        }

        if (mRepeating)
            track = mTracks.get(0);

        return track;
    }

    /**
     * Returns the previous Track but does not update the internal Track
     * iterator.
     * 
     * @return Returns previous Track. Returns null if there is none.
     */
    public Track peekPreviousTrack() {

        Track track = null;
        if (mTrackIterator.hasPrevious()) {

            track = mTrackIterator.previous();
            if (track == mCurrentTrack && mTrackIterator.hasPrevious())
                track = mTrackIterator.previous();
            else if (track == mCurrentTrack && !mTrackIterator.hasPrevious() && mRepeating)
                track = mTracks.get(mTracks.size() - 1);
            else if (track == mCurrentTrack)
                track = null;

            mTrackIterator.next();
        }

        if (mRepeating)
            mTracks.get(mTracks.size() - 1);

        return track;
    }

    /**
     * Set this playlist to shuffle mode.
     * 
     * @param shuffled
     */
    @SuppressWarnings("unchecked")
    public void setShuffled(boolean shuffled) {
        mShuffled = shuffled;

        if (shuffled) {
            mShuffledTracks = (ArrayList<Track>) mTracks.clone();
            Collections.shuffle(mShuffledTracks);
        } else
            mShuffledTracks = null;

    }

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

        if (getCount() > 0 && mTrackIterator != null) {
            if (hasPreviousTrack())
                return mTrackIterator.previousIndex()+1;
            return 0;
        }
        return -1;
    }
}
