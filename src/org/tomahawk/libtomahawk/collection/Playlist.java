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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link Playlist} is a {@link org.tomahawk.libtomahawk.collection.Playlist} created by the user
 * and stored in the database
 */
public class Playlist extends Cacheable implements AlphaComparable {

    private String mName = "";

    private List<PlaylistEntry> mEntries = new ArrayList<>();

    private String mId;

    private String mHatchetId;

    private String mCurrentRevision = "";

    private String[] mTopArtistNames;

    private long mCount;

    private boolean mIsFilled;

    /**
     * Construct a new empty {@link Playlist}.
     */
    private Playlist(String id) {
        super(Playlist.class, id);

        mId = id;
    }

    /**
     * Returns the {@link Playlist} with the given parameters. If none exists in our static {@link
     * ConcurrentHashMap} yet, construct and add it.
     *
     * @return {@link Playlist} with the given parameters
     */
    public static Playlist get(String id) {
        Cacheable cacheable = get(Playlist.class, id);
        return cacheable != null ? (Playlist) cacheable : new Playlist(id);
    }

    /**
     * Create a {@link Playlist} from a list of {@link PlaylistEntry}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromEntriesList(String id, String name, String currentRevision,
            List<PlaylistEntry> entries) {
        Playlist pl = Playlist.get(id);
        pl.setEntries(entries);
        pl.setName(name);
        pl.setCurrentRevision(currentRevision);
        return pl;
    }

    /**
     * Create a {@link Playlist} from a list of {@link PlaylistEntry}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromEmptyList(String id, String name) {
        return fromEntriesList(id, name, null, new ArrayList<PlaylistEntry>());
    }

    /**
     * Create a {@link Playlist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromQueryList(String id, String name, String currentRevision,
            List<Query> queries) {
        List<PlaylistEntry> entries = new ArrayList<>();
        for (Query query : queries) {
            entries.add(PlaylistEntry.get(id, query,
                    TomahawkMainActivity.getLifetimeUniqueStringId()));
        }
        return fromEntriesList(id, name, currentRevision, entries);
    }

    /**
     * Get the {@link org.tomahawk.libtomahawk.collection.Playlist} by providing its cache key. Only
     * use this for playlists that are not stored in the database!
     */
    public static Playlist getByKey(String id) {
        return (Playlist) get(Playlist.class, id);
    }

    public String getId() {
        return mId;
    }

    public String getHatchetId() {
        return mHatchetId;
    }

    public void setHatchetId(String hatchetId) {
        mHatchetId = hatchetId;
    }

    public void setCurrentRevision(String currentRevision) {
        mCurrentRevision = currentRevision == null ? "" : currentRevision;
    }

    public String getCurrentRevision() {
        return mCurrentRevision;
    }

    public String[] getTopArtistNames() {
        return mTopArtistNames;
    }

    public void setTopArtistNames(String[] topArtistNames) {
        mTopArtistNames = topArtistNames;
    }

    public void updateTopArtistNames() {
        final HashMap<String, Integer> countMap = new HashMap<>();
        for (PlaylistEntry entry : mEntries) {
            String artistName = entry.getArtist().getName();
            if (countMap.containsKey(artistName)) {
                countMap.put(artistName, countMap.get(artistName) + 1);
            } else {
                countMap.put(artistName, 1);
            }
        }
        String[] results = new String[0];
        if (countMap.size() > 0) {
            PriorityQueue<String> topArtistNames = new PriorityQueue<>(countMap.size(),
                    new Comparator<String>() {
                        @Override
                        public int compare(String lhs, String rhs) {
                            return countMap.get(lhs) >= countMap.get(rhs) ? -1 : 1;
                        }
                    }
            );
            topArtistNames.addAll(countMap.keySet());
            results = topArtistNames.toArray(new String[topArtistNames.size()]);
        }
        mTopArtistNames = results;
    }

    /**
     * @return this {@link Playlist}'s name
     */
    public String getName() {
        return mName;
    }

    /**
     * Set the name of this {@link Playlist}
     *
     * @param name the name to be set
     */
    public void setName(String name) {
        mName = name == null ? "" : name;
    }

    /**
     * Set this {@link Playlist}'s {@link Query}s
     */
    public void setEntries(List<PlaylistEntry> entries) {
        mEntries = entries;
    }

    /**
     * @return the next {@link Query}
     */
    public PlaylistEntry getNextEntry(PlaylistEntry entry) {
        int index = mEntries.indexOf(entry);
        if (index + 1 < mEntries.size()) {
            return mEntries.get(index + 1);
        }
        return null;
    }

    /**
     * @return the previous {@link Query}
     */
    public PlaylistEntry getPreviousEntry(PlaylistEntry entry) {
        int index = mEntries.indexOf(entry);
        if (index - 1 >= 0) {
            return mEntries.get(index - 1);
        }
        return null;
    }

    /**
     * @return the first {@link PlaylistEntry} of this playlist
     */
    public PlaylistEntry getFirstEntry() {
        if (mEntries.isEmpty()) {
            return null;
        }
        return mEntries.get(0);
    }

    /**
     * @return the last {@link PlaylistEntry} of this playlist
     */
    public PlaylistEntry getLastEntry() {
        if (mEntries.isEmpty()) {
            return null;
        }
        return mEntries.get(mEntries.size() - 1);
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
    public boolean hasNextEntry(PlaylistEntry entry) {
        return getNextEntry(entry) != null;
    }

    /**
     * @return true, if the {@link Playlist} has a previous {@link PlaylistEntry}, otherwise false
     */
    public boolean hasPreviousEntry(PlaylistEntry entry) {
        return getPreviousEntry(entry) != null;
    }

    /**
     * Return the current count of querys in the {@link Playlist}
     */
    public int size() {
        return mEntries.size();
    }

    /**
     * Return all PlaylistEntries in the {@link Playlist}
     */
    public List<PlaylistEntry> getEntries() {
        return mEntries;
    }

    /**
     * Add an {@link ArrayList} of {@link PlaylistEntry}s at the given position
     */
    public void addEntries(int position, ArrayList<PlaylistEntry> entries) {
        mEntries.addAll(position, entries);
    }

    /**
     * Add an {@link ArrayList} of {@link PlaylistEntry}s at the given position
     */
    public void addEntries(ArrayList<PlaylistEntry> entries) {
        mEntries.addAll(entries);
    }

    /**
     * Append {@link Query} at the end of this playlist
     *
     * @return the {@link PlaylistEntry} that got created and added to this {@link Playlist}
     */
    public PlaylistEntry addQuery(Query query) {
        PlaylistEntry entry = PlaylistEntry.get(mId, query,
                TomahawkMainActivity.getLifetimeUniqueStringId());
        mEntries.add(entry);
        return entry;
    }

    /**
     * Append an {@link ArrayList} of {@link Query}s at the end of this playlist
     */
    public void addQueries(List<Query> queries) {
        List<PlaylistEntry> playlistEntries = new ArrayList<>();
        for (Query query : queries) {
            playlistEntries.add(PlaylistEntry.get(mId, query,
                    TomahawkMainActivity.getLifetimeUniqueStringId()));
        }
        mEntries.addAll(playlistEntries);
    }

    /**
     * Remove the {@link PlaylistEntry} at the given position from this playlist
     */
    public void deleteEntryAtPos(int position) {
        mEntries.remove(position);
    }

    /**
     * Remove the given {@link Query} from this playlist
     */
    public boolean deleteEntry(PlaylistEntry entry) {
        return mEntries.remove(entry);
    }

    public List<Query> getQueries() {
        List<Query> queries = new ArrayList<>();
        for (PlaylistEntry entry : mEntries) {
            queries.add(entry.getQuery());
        }
        return queries;
    }

    public long getCount() {
        return mCount;
    }

    public void setCount(long count) {
        mCount = count;
    }

    public boolean isFilled() {
        return mIsFilled;
    }

    public void setFilled(boolean isFilled) {
        mIsFilled = isFilled;
    }

    public PlaylistEntry getEntryWithQuery(Query query) {
        for (PlaylistEntry entry : mEntries) {
            if (entry.getQuery().equals(query)) {
                return entry;
            }
        }
        return null;
    }

    public PlaylistEntry getEntryAtPos(int position) {
        if (position < mEntries.size()) {
            return mEntries.get(position);
        }
        return null;
    }

    public int getIndexOfEntry(PlaylistEntry entry) {
        return mEntries.indexOf(entry);
    }

    public String getUserId() {
        if (mHatchetId != null) {
            String[] s = mHatchetId.split("_");
            return s[0];
        }
        return null;
    }
}
