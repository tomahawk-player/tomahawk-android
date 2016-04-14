/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.utils;

import org.tomahawk.libtomahawk.collection.Cacheable;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.resolver.Query;

import android.util.Log;

import java.util.List;

public class PlaybackManager extends Cacheable {

    public static final String TAG = PlaybackManager.class.getSimpleName();

    public static final int NOT_REPEATING = 0;

    public static final int REPEAT_ALL = 1;

    public static final int REPEAT_ONE = 2;

    public static final int NOT_SHUFFLED = 0;

    public static final int SHUFFLED = 1;

    private int mRepeatMode = NOT_REPEATING;

    private int mShuffleMode = NOT_SHUFFLED;

    private Playlist mPlaylist =
            Playlist.fromEmptyList(IdGenerator.getLifetimeUniqueStringId(), "");

    private Playlist mQueue =
            Playlist.fromEmptyList(IdGenerator.getLifetimeUniqueStringId(), "");

    private int mQueueStartPos = -1;

    private PlaylistEntry mCurrentEntry;

    private int mCurrentIndex = -1;

    private String mId;

    private Callback mCallback;

    public interface Callback {

        void onPlaylistChanged();

        void onCurrentEntryChanged();

        void onShuffleModeChanged();

        void onRepeatModeChanged();
    }

    private PlaybackManager(String id, Callback callback) {
        super(PlaybackManager.class, id);

        mId = id;
        mCallback = callback;
    }

    public static PlaybackManager get(String id, Callback callback) {
        Cacheable cacheable = get(PlaybackManager.class, id);
        return cacheable != null ? (PlaybackManager) cacheable : new PlaybackManager(id, callback);
    }

    public static PlaybackManager getByKey(String id) {
        return (PlaybackManager) get(PlaybackManager.class, id);
    }

    public String getId() {
        return mId;
    }

    public Playlist getPlaylist() {
        return mPlaylist;
    }

    public void setPlaylist(Playlist playlist) {
        setPlaylist(playlist, null);
    }

    public void setPlaylist(Playlist playlist, PlaylistEntry currentEntry) {
        mRepeatMode = NOT_REPEATING;
        mShuffleMode = NOT_SHUFFLED;
        mPlaylist = playlist;
        if (currentEntry == null) {
            currentEntry = playlist.getEntryAtPos(0);
        }
        setCurrentEntry(currentEntry, false);
        mCallback.onPlaylistChanged();
    }

    /**
     * @param position int containing the position in the current playback list
     * @return the {@link PlaylistEntry} which has been found at the given position
     */
    public PlaylistEntry getPlaybackListEntry(int position) {
        if (position < mQueueStartPos) {
            // The requested entry is positioned before the queue
            return mPlaylist.getEntryAtPos(position, mShuffleMode == SHUFFLED);
        } else if (position < mQueueStartPos + mQueue.size()) {
            // Getting the entry from the queue
            return mQueue.getEntryAtPos(position - mQueueStartPos);
        } else {
            // The requested entry is positioned after the queue
            return mPlaylist.getEntryAtPos(position - mQueue.size(), mShuffleMode == SHUFFLED);
        }
    }

    /**
     * @param entry The {@link PlaylistEntry} to get the index for
     * @return an int containing the index of the given {@link PlaylistEntry} inside the current
     * playback list
     */
    public int getPlaybackListIndex(PlaylistEntry entry) {
        int index = mQueue.getIndexOfEntry(entry);
        if (index >= 0) {
            // Found entry in queue
            return index + mQueueStartPos;
        } else {
            index = mPlaylist.getIndexOfEntry(entry, mShuffleMode == SHUFFLED);
            if (index < 0) {
                Log.e(TAG,
                        "getPlaybackListIndex - Couldn't find given entry in mQueue or mPlaylist.");
                return -1;
            }
            if (index < mQueueStartPos) {
                // Found entry and its positioned before the queue
                return index;
            } else {
                // Found entry and its positioned after the queue
                return index + mQueue.size();
            }
        }
    }

    public boolean isPartOfQueue(int position) {
        return position >= mQueueStartPos && position < mQueueStartPos + mQueue.size();
    }

    public int getNumeration(int position) {
        int numeration;
        if (position < mQueueStartPos) {
            // Positioned before the queue
            numeration = position;
        } else if (position < mQueueStartPos + mQueue.size()) {
            // From the queue
            numeration = position - mQueueStartPos;
        } else {
            // Positioned after the queue
            numeration = position - mQueue.size();
        }
        return numeration - mCurrentIndex;
    }

    public int getPlaybackListSize() {
        return mQueue.size() + mPlaylist.size();
    }

    public void setCurrentEntry(PlaylistEntry currentEntry) {
        setCurrentEntry(currentEntry, true);
    }

    private void setCurrentEntry(PlaylistEntry currentEntry, boolean callback) {
        mCurrentEntry = currentEntry;
        if (mPlaylist.containsEntry(currentEntry)) {
            // We have a PlaylistEntry that is not part of the Queue
            mCurrentIndex = mPlaylist.getIndexOfEntry(currentEntry, mShuffleMode == SHUFFLED);
            mQueueStartPos = mCurrentIndex + 1;
            if (callback) {
                mCallback.onPlaylistChanged();
            }
        } else {
            // We have a PlaylistEntry that is part of the Queue
            mCurrentIndex = getPlaybackListIndex(currentEntry);
            if (callback) {
                mCallback.onCurrentEntryChanged();
            }
        }
    }

    public PlaylistEntry getCurrentEntry() {
        return mCurrentEntry;
    }

    public int getCurrentIndex() {
        return mCurrentIndex;
    }

    public Query getCurrentQuery() {
        if (mCurrentEntry != null) {
            return mCurrentEntry.getQuery();
        }
        return null;
    }

    public Track getCurrentTrack() {
        if (mCurrentEntry != null) {
            return mCurrentEntry.getQuery().getPreferredTrack();
        }
        return null;
    }

    public void addToQueue(Query query) {
        Log.d(TAG, "addToQueue: " + query.getName());
        mQueue.addQuery(0, query);
        if (getCurrentEntry() == null) {
            setCurrentEntry(getPlaybackListEntry(0), false);
        }
        mCallback.onPlaylistChanged();
    }

    public void addToQueue(List<Query> queries) {
        Log.d(TAG, "addToQueue: queries.size()= " + queries.size());
        int counter = 0;
        for (Query query : queries) {
            mQueue.addQuery(counter++, query);
        }
        if (getCurrentEntry() == null) {
            setCurrentEntry(getPlaybackListEntry(0), false);
        }
        mCallback.onPlaylistChanged();
    }

    public void deleteFromQueue(PlaylistEntry entry) {
        Log.d(TAG, "deleteFromQueue: " + entry.getQuery().getName());
        if (mQueue.deleteEntry(entry)) {
            mCallback.onPlaylistChanged();
        }
    }

    public PlaylistEntry getNextEntry() {
        return getNextEntry(mCurrentEntry);
    }

    public PlaylistEntry getNextEntry(PlaylistEntry entry) {
        if (mRepeatMode == REPEAT_ONE) {
            return entry;
        }
        int index = getPlaybackListIndex(entry);
        PlaylistEntry nextEntry = getPlaybackListEntry(index + 1);
        if (nextEntry == null && mRepeatMode == REPEAT_ALL) {
            nextEntry = getPlaybackListEntry(0);
        }
        return nextEntry;
    }

    public boolean hasNextEntry() {
        return hasNextEntry(mCurrentEntry);
    }

    public boolean hasNextEntry(PlaylistEntry entry) {
        return mRepeatMode == REPEAT_ONE || getNextEntry(entry) != null;
    }

    public PlaylistEntry getPreviousEntry() {
        return getPreviousEntry(mCurrentEntry);
    }

    public PlaylistEntry getPreviousEntry(PlaylistEntry entry) {
        if (mRepeatMode == REPEAT_ONE) {
            return entry;
        }
        int index = getPlaybackListIndex(entry);
        PlaylistEntry previousEntry = getPlaybackListEntry(index - 1);
        if (previousEntry == null && mRepeatMode == REPEAT_ALL) {
            previousEntry = getPlaybackListEntry(getPlaybackListSize() - 1);
        }
        return previousEntry;
    }

    public boolean hasPreviousEntry() {
        return hasPreviousEntry(mCurrentEntry);
    }

    public boolean hasPreviousEntry(PlaylistEntry entry) {
        return mRepeatMode == REPEAT_ONE || getPreviousEntry(entry) != null;
    }

    public int getRepeatMode() {
        return mRepeatMode;
    }

    public void setRepeatMode(int repeatingMode) {
        Log.d(TAG, "repeat from " + mRepeatMode + " to " + repeatingMode);
        if (mRepeatMode != repeatingMode) {
            mRepeatMode = repeatingMode;
            mCallback.onRepeatModeChanged();
        }
    }

    public int getShuffleMode() {
        return mShuffleMode;
    }

    /**
     * Set whether or not to enable shuffle mode on the current playlist.
     */
    public void setShuffleMode(int shuffleMode) {
        Log.d(TAG, "shuffle from " + mShuffleMode + " to " + shuffleMode);
        if (mShuffleMode != shuffleMode) {
            mShuffleMode = shuffleMode;
            if (mShuffleMode == SHUFFLED) {
                int currentIndex = -1;
                if (!mQueue.containsEntry(mCurrentEntry)) {
                    // We have a PlaylistEntry that is not part of the Queue
                    currentIndex = mPlaylist.getIndexOfEntry(mCurrentEntry);
                }
                mPlaylist.buildShuffledIndex(currentIndex);
            }
            if (!mQueue.containsEntry(mCurrentEntry)) {
                // mCurrentEntry not part of queue, refresh mCurrentIndex
                setCurrentEntry(mCurrentEntry);
            } else {
                // mCurrentEntry is part of queue, put the queue at the very top
                mQueueStartPos = 0;
            }
            mCallback.onPlaylistChanged();
            mCallback.onShuffleModeChanged();
        }
    }

}
