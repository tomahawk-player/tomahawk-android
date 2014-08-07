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

import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link Playlist} is a {@link org.tomahawk.libtomahawk.collection.Playlist} created by the user
 * and stored in the database
 */
public class Playlist implements TomahawkListItem {

    private String mName;

    private ArrayList<PlaylistEntry> mEntries = new ArrayList<PlaylistEntry>();

    private ArrayList<PlaylistEntry> mShuffledEntries;

    private int mCurrentQueryIndex;

    private boolean mShuffled;

    private boolean mRepeating;

    private static ConcurrentHashMap<String, Playlist> sPlaylists
            = new ConcurrentHashMap<String, Playlist>();

    private String mId;

    private String mHatchetId;

    private String mCurrentRevision;

    private ArrayList<Artist> mContentHeaderArtists = new ArrayList<Artist>();

    private boolean mIsFilled;

    /**
     * Construct a new empty {@link Playlist}.
     */
    private Playlist(String id, String name, String currentRevision) {
        mName = name;
        mShuffled = false;
        mRepeating = false;
        mId = id;
        mCurrentRevision = currentRevision;
    }

    /**
     * Returns the {@link Playlist} with the given parameters. If none exists in our static {@link
     * ConcurrentHashMap} yet, construct and add it.
     *
     * @return {@link Playlist} with the given parameters
     */
    public static Playlist get(String id, String name, String currentRevision) {
        Playlist playlist = new Playlist(id, name, currentRevision);
        return ensureCache(playlist);
    }

    /**
     * If PlaylistEntry is already in our cache, return that. Otherwise add it to the cache.
     */
    private static Playlist ensureCache(Playlist playlist) {
        if (!sPlaylists.containsKey(playlist.getCacheKey())) {
            sPlaylists.put(playlist.getCacheKey(), playlist);
        }
        return sPlaylists.get(playlist.getCacheKey());
    }

    /**
     * Create a {@link Playlist} from a list of {@link PlaylistEntry}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromEntriesList(String name, String currentRevision,
            ArrayList<PlaylistEntry> playlistEntries, String currentEntryId) {
        String id = TomahawkMainActivity.getLifetimeUniqueStringId();
        if (currentRevision == null) {
            currentRevision = "";
        }
        Playlist pl = Playlist.get(id, name, currentRevision);
        pl.setEntries(playlistEntries);
        pl.setCurrentEntry(currentEntryId);
        return pl;
    }

    /**
     * Create a {@link Playlist} from a list of {@link PlaylistEntry}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromEntriesList(String name, String currentRevision,
            ArrayList<PlaylistEntry> playlistEntries, int currentQueryIndex) {
        String id = TomahawkMainActivity.getLifetimeUniqueStringId();
        if (currentRevision == null) {
            currentRevision = "";
        }
        Playlist pl = Playlist.get(id, name, currentRevision);
        pl.setEntries(playlistEntries);
        pl.setCurrentQueryIndex(currentQueryIndex);
        return pl;
    }

    /**
     * Create a {@link Playlist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromQueryList(String name, String currentRevision,
            ArrayList<Query> queries, int currentQueryIndex) {
        String id = TomahawkMainActivity.getLifetimeUniqueStringId();
        if (currentRevision == null) {
            currentRevision = "";
        }
        Playlist pl = Playlist.get(id, name, currentRevision);
        ArrayList<PlaylistEntry> playlistEntries = new ArrayList<PlaylistEntry>();
        for (Query query : queries) {
            playlistEntries.add(PlaylistEntry.get(id, query,
                    TomahawkMainActivity.getLifetimeUniqueStringId()));
        }
        pl.setEntries(playlistEntries);
        pl.setCurrentQueryIndex(currentQueryIndex);
        return pl;
    }

    /**
     * Create a {@link Playlist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromQueryList(String name, ArrayList<Query> queries,
            String currentQueryCacheKey) {
        Playlist playlist = Playlist.fromQueryList(name, null, queries, 0);
        playlist.setCurrentQuery(currentQueryCacheKey);
        return playlist;
    }

    /**
     * Create a {@link Playlist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromQueryList(String name, ArrayList<Query> queries,
            int currentQueryIndex) {
        return Playlist.fromQueryList(name, null, queries, currentQueryIndex);
    }

    /**
     * Create a {@link Playlist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromQueryList(String name, ArrayList<Query> queries) {
        return Playlist.fromQueryList(name, null, queries, 0);
    }

    /**
     * Get the {@link org.tomahawk.libtomahawk.collection.Playlist} by providing its cache key
     */
    public static Playlist getPlaylistById(String key) {
        return sPlaylists.get(key);
    }

    public String getCacheKey() {
        return mId;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        sPlaylists.remove(getCacheKey());
        mId = id;
        ensureCache(this);
    }

    public String getHatchetId() {
        return mHatchetId;
    }

    public void setHatchetId(String hatchetId) {
        mHatchetId = hatchetId;
    }

    public void setCurrentRevision(String currentRevision) {
        mCurrentRevision = currentRevision;
    }

    public String getCurrentRevision() {
        return mCurrentRevision;
    }

    public ArrayList<Artist> getContentHeaderArtists() {
        return mContentHeaderArtists;
    }

    public void addContentHeaderArtists(Artist artist) {
        mContentHeaderArtists.add(artist);
    }

    /**
     * @return this object' name
     */
    @Override
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
     * Set this {@link Playlist}'s {@link Query}s
     */
    public void setEntries(ArrayList<PlaylistEntry> entries) {
        mEntries = entries;
        mCurrentQueryIndex = 0;
    }

    /**
     * Set the current entry
     *
     * @param entryId the Entry's id
     */
    public void setCurrentEntry(String entryId) {
        for (int i = 0; i < (mShuffled ? mShuffledEntries.size() : mEntries.size()); i++) {
            if (peekEntryAtPos(i).getId().equals(entryId)) {
                mCurrentQueryIndex = i;
            }
        }
    }

    /**
     * Set the current query
     *
     * @param queryKey the Queries id
     */
    public void setCurrentQuery(String queryKey) {
        for (int i = 0; i < (mShuffled ? mShuffledEntries.size() : mEntries.size()); i++) {
            if (peekEntryAtPos(i).getQuery().getCacheKey().equals(queryKey)) {
                mCurrentQueryIndex = i;
            }
        }
    }

    /**
     * Set the current {@link Query} index
     *
     * @param currentQueryIndex int containig the {@link Query}'s index
     */
    public void setCurrentQueryIndex(int currentQueryIndex) {
        mCurrentQueryIndex = currentQueryIndex;
    }

    /**
     * @return the current {@link Query}'s index
     */
    public int getCurrentQueryIndex() {
        return mCurrentQueryIndex;
    }

    /**
     * @return the current {@link org.tomahawk.libtomahawk.collection.PlaylistEntry}
     */
    public PlaylistEntry getCurrentEntry() {
        List<PlaylistEntry> playlistEntries = mShuffled ? mShuffledEntries : mEntries;
        if (playlistEntries != null && mCurrentQueryIndex >= 0
                && mCurrentQueryIndex < playlistEntries.size()) {
            return playlistEntries.get(mCurrentQueryIndex);
        }
        return null;
    }

    /**
     * @return the next {@link Query}
     */
    public PlaylistEntry getNextEntry() {
        List<PlaylistEntry> playlistEntries = mShuffled ? mShuffledEntries : mEntries;
        if (mCurrentQueryIndex + 1 < playlistEntries.size()) {
            PlaylistEntry entry = playlistEntries.get(mCurrentQueryIndex + 1);
            mCurrentQueryIndex = mCurrentQueryIndex + 1;
            return entry;
        } else if (mRepeating) {
            mCurrentQueryIndex = 0;
            return getFirstEntry();
        }
        return null;
    }

    /**
     * @return the previous {@link Query}
     */
    public PlaylistEntry getPreviousEntry() {
        List<PlaylistEntry> playlistEntries = mShuffled ? mShuffledEntries : mEntries;
        if (mCurrentQueryIndex - 1 >= 0) {
            PlaylistEntry entry = playlistEntries.get(mCurrentQueryIndex - 1);
            mCurrentQueryIndex = mCurrentQueryIndex - 1;
            return entry;
        } else if (mRepeating) {
            mCurrentQueryIndex = playlistEntries.size() - 1;
            return getLastEntry();
        }
        return null;
    }

    /**
     * @return the first {@link PlaylistEntry} of this playlist
     */
    public PlaylistEntry getFirstEntry() {
        if (mShuffled ? mShuffledEntries.isEmpty() : mEntries.isEmpty()) {
            return null;
        }

        return mShuffled ? mShuffledEntries.get(0) : mEntries.get(0);
    }

    /**
     * @return the last {@link PlaylistEntry} of this playlist
     */
    public PlaylistEntry getLastEntry() {
        if (mShuffled ? mShuffledEntries.isEmpty() : mEntries.isEmpty()) {
            return null;
        }

        return mShuffled ? mShuffledEntries.get(mShuffledEntries.size() - 1)
                : mEntries.get(mEntries.size() - 1);
    }

    /**
     * @return this {@link Playlist}'s name
     */
    @Override
    public String toString() {
        return mName;
    }

    /**
     * @return true, if the {@link Playlist} has a next {@link PlaylistEntry}, otherwise false
     */
    public boolean hasNextEntry() {
        return peekNextEntry() != null;
    }

    /**
     * @return true, if the {@link Playlist} has a previous {@link PlaylistEntry}, otherwise false
     */
    public boolean hasPreviousEntry() {
        return peekPreviousEntry() != null;
    }

    /**
     * Returns the next {@link PlaylistEntry} but does not update the internal {@link PlaylistEntry}
     * iterator.
     *
     * @return Returns next {@link PlaylistEntry}. Returns null if there is none.
     */
    public PlaylistEntry peekNextEntry() {
        List<PlaylistEntry> playlistEntries = mShuffled ? mShuffledEntries : mEntries;
        if (mCurrentQueryIndex + 1 < playlistEntries.size()) {
            return playlistEntries.get(mCurrentQueryIndex + 1);
        } else if (mRepeating) {
            return getFirstEntry();
        }
        return null;
    }

    /**
     * Returns the previous {@link Query} but does not update the internal {@link Query} iterator.
     *
     * @return Returns previous {@link Query}. Returns null if there is none.
     */
    public PlaylistEntry peekPreviousEntry() {
        if (mCurrentQueryIndex - 1 >= 0) {
            List<PlaylistEntry> playlistEntries = mShuffled ? mShuffledEntries : mEntries;
            return playlistEntries.get(mCurrentQueryIndex - 1);
        } else if (mRepeating) {
            return getLastEntry();
        }
        return null;
    }

    /**
     * Returns the {@link PlaylistEntry} at the given position but does not update the internal
     * {@link PlaylistEntry} iterator.
     *
     * @return Returns the {@link PlaylistEntry} at the given position. Returns null if there is
     * none.
     */
    public PlaylistEntry peekEntryAtPos(int i) {
        if (i >= 0 && i < (mShuffled ? mShuffledEntries.size() : mEntries.size())) {
            return mShuffled ? mShuffledEntries.get(i) : mEntries.get(i);
        }
        return null;
    }

    /**
     * Set this {@link Playlist} to shuffle mode.
     */
    @SuppressWarnings("unchecked")
    public void setShuffled(boolean shuffled) {
        PlaylistEntry oldCurrentPlaylistEntry = getCurrentEntry();
        mShuffled = shuffled;
        int i = 0;

        if (shuffled) {
            mShuffledEntries = (ArrayList<PlaylistEntry>) mEntries.clone();
            Collections.shuffle(mShuffledEntries);
        } else {
            mShuffledEntries = null;
        }

        List<PlaylistEntry> querys = mShuffled ? mShuffledEntries : mEntries;
        while (i < querys.size()) {
            if (oldCurrentPlaylistEntry == querys.get(i)) {
                mCurrentQueryIndex = i;
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
     * Return the current count of querys in the {@link Playlist}
     */
    public int getCount() {
        return mEntries.size();
    }

    /**
     * Return all PlaylistEntries in the {@link Playlist}
     */
    public ArrayList<PlaylistEntry> getEntries() {
        return mShuffled ? mShuffledEntries : mEntries;
    }

    /**
     * Return all queries in the {@link Playlist}
     */
    public ArrayList<Query> getQueries() {
        ArrayList<Query> queries = new ArrayList<Query>();
        for (PlaylistEntry entry : mShuffled ? mShuffledEntries : mEntries) {
            queries.add(entry.getQuery());
        }
        return queries;
    }

    /**
     * Add an {@link ArrayList} of {@link Query}s at the given position
     */
    public void addQueries(int position, ArrayList<Query> queries) {
        ArrayList<PlaylistEntry> playlistEntries = new ArrayList<PlaylistEntry>();
        for (Query query : queries) {
            playlistEntries.add(PlaylistEntry.get(mId, query,
                    TomahawkMainActivity.getLifetimeUniqueStringId()));
        }
        (mShuffled ? mShuffledEntries : mEntries).addAll(position, playlistEntries);
    }

    /**
     * Append an {@link ArrayList} of {@link Query}s at the end of this playlist
     */
    public void addQueries(ArrayList<Query> queries) {
        ArrayList<PlaylistEntry> playlistEntries = new ArrayList<PlaylistEntry>();
        for (Query query : queries) {
            playlistEntries.add(PlaylistEntry.get(mId, query,
                    TomahawkMainActivity.getLifetimeUniqueStringId()));
        }
        (mShuffled ? mShuffledEntries : mEntries).addAll(playlistEntries);
    }

    /**
     * Remove the {@link PlaylistEntry} at the given position from this playlist
     */
    public void deleteEntryAtPos(int position) {
        if (mShuffledEntries != null) {
            mShuffledEntries.remove((mShuffled ? mShuffledEntries : mEntries).get(position));
        }
        (mShuffled ? mShuffledEntries : mEntries).remove(position);
        if (mCurrentQueryIndex > (mShuffled ? mShuffledEntries : mEntries).size()) {
            mCurrentQueryIndex--;
        }
    }

    /**
     * Remove the given {@link Query} from this playlist
     */
    public void deleteEntry(PlaylistEntry entry) {
        if (mShuffledEntries != null) {
            mShuffledEntries.remove(entry);
        }
        (mShuffled ? mShuffledEntries : mEntries).remove(entry);
        if (mCurrentQueryIndex > (mShuffled ? mShuffledEntries : mEntries).size()) {
            mCurrentQueryIndex--;
        }
    }

    /**
     * @return always null. This method needed to comply to the {@link TomahawkListItem} interface.
     */
    @Override
    public Artist getArtist() {
        return null;
    }

    /**
     * @return always null. This method needed to comply to the {@link TomahawkListItem} interface.
     */
    @Override
    public Album getAlbum() {
        return null;
    }

    @Override
    public Image getImage() {
        for (Artist artist : mContentHeaderArtists) {
            if (artist.getImage() != null && !TextUtils.isEmpty(artist.getImage().getImagePath())) {
                return artist.getImage();
            }
        }
        return null;
    }

    public boolean isFilled() {
        return mIsFilled;
    }

    public void setFilled(boolean isFilled) {
        mIsFilled = isFilled;
    }
}
