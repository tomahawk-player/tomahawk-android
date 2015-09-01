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
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link Playlist} is a {@link org.tomahawk.libtomahawk.collection.Playlist} created by the user
 * and stored in the database
 */
public class Playlist extends Cacheable implements AlphaComparable {

    private String mName = "";

    private CollectionCursor<PlaylistEntry> mCursor = null;

    private List<PlaylistEntry> mAddedEntries = new ArrayList<>();

    private Map<PlaylistEntry, Index> mCachedEntries = new HashMap<>();

    private List<Index> mIndex = new ArrayList<>();

    private static class Index {

        protected Index(int index, boolean fromMergedItems) {
            mIndex = index;
            mFromMergedItems = fromMergedItems;
        }

        int mIndex;

        boolean mFromMergedItems;
    }

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
        CollectionCursor<PlaylistEntry> cursor =
                new CollectionCursor<>(entries, PlaylistEntry.class);
        return fromCursor(id, name, currentRevision, cursor);
    }

    /**
     * Create a {@link Playlist} from a list of {@link PlaylistEntry}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromEmptyList(String id, String name) {
        CollectionCursor<PlaylistEntry> cursor =
                new CollectionCursor<>(new ArrayList<PlaylistEntry>(), PlaylistEntry.class);
        return fromCursor(id, name, null, cursor);
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
        CollectionCursor<PlaylistEntry> cursor =
                new CollectionCursor<>(entries, PlaylistEntry.class);
        return fromCursor(id, name, currentRevision, cursor);
    }

    /**
     * Create a {@link Playlist} from a {@link CollectionCursor} containing {@link PlaylistEntry}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromCursor(String id, String name, String currentRevision,
            CollectionCursor<PlaylistEntry> cursor) {
        Playlist pl = Playlist.get(id);
        pl.setName(name);
        pl.setCurrentRevision(currentRevision);
        pl.setCursor(cursor);
        return pl;
    }

    protected void setCursor(CollectionCursor<PlaylistEntry> cursor) {
        mCursor = cursor;
        initIndex();
    }

    private void initIndex() {
        for (int i = 0; i < mCursor.size(); i++) {
            mIndex.add(new Index(i, false));
        }
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
        for (PlaylistEntry entry : getEntries()) {
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
        mCursor = new CollectionCursor<>(entries, PlaylistEntry.class);
    }

    public PlaylistEntry getEntry(Index index) {
        PlaylistEntry entry;
        if (index.mFromMergedItems) {
            entry = mAddedEntries.get(index.mIndex);
        } else {
            entry = mCursor.get(index.mIndex);
        }
        mCachedEntries.put(entry, index);
        return entry;
    }

    /**
     * @return the next {@link PlaylistEntry}
     */
    public PlaylistEntry getNextEntry(PlaylistEntry entry) {
        Index index = mCachedEntries.get(entry);
        if (index == null) {
            throw new RuntimeException("Couldn't find cached PlaylistEntry.");
        }
        int position = mIndex.indexOf(index);
        if (position + 1 < mIndex.size()) {
            Index nextIndex = mIndex.get(position + 1);
            return getEntry(nextIndex);
        }
        return null;
    }

    /**
     * @return the previous {@link PlaylistEntry}
     */
    public PlaylistEntry getPreviousEntry(PlaylistEntry entry) {
        Index index = mCachedEntries.get(entry);
        if (index == null) {
            throw new RuntimeException("Couldn't find cached PlaylistEntry.");
        }
        int position = mIndex.indexOf(index);
        if (position - 1 >= 0) {
            Index nextIndex = mIndex.get(position - 1);
            return getEntry(nextIndex);
        }
        return null;
    }

    /**
     * @return the first {@link PlaylistEntry} of this playlist
     */
    public PlaylistEntry getFirstEntry() {
        return getEntry(mIndex.get(0));
    }

    /**
     * @return the last {@link PlaylistEntry} of this playlist
     */
    public PlaylistEntry getLastEntry() {
        return getEntry(mIndex.get(mIndex.size() - 1));
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
        Index index = mCachedEntries.get(entry);
        if (index == null) {
            throw new RuntimeException("Couldn't find cached PlaylistEntry.");
        }
        int position = mIndex.indexOf(index);
        return position + 1 < mIndex.size();
    }

    /**
     * @return true, if the {@link Playlist} has a previous {@link PlaylistEntry}, otherwise false
     */
    public boolean hasPreviousEntry(PlaylistEntry entry) {
        Index index = mCachedEntries.get(entry);
        if (index == null) {
            throw new RuntimeException("Couldn't find cached PlaylistEntry.");
        }
        int position = mIndex.indexOf(index);
        return position - 1 >= 0;
    }

    /**
     * Return the current count of entries in the {@link Playlist}
     */
    public int size() {
        return mIndex.size();
    }

    /**
     * Return all PlaylistEntries in the {@link Playlist}. This is a very costly operation and
     * should only be done if absolutely necessary. Consider using {@link #getEntryAtPos(int)}.
     */
    public List<PlaylistEntry> getEntries() {
        List<PlaylistEntry> entries = new ArrayList<>();
        for (Index index : mIndex) {
            entries.add(getEntry(index));
        }
        return entries;
    }

    /**
     * Add the given {@link Query} to this {@link Playlist}
     *
     * @param position the position at which to insert the given {@link Query}
     * @param query    the {@link Query} to add
     * @return the {@link PlaylistEntry} that got created and added to this {@link Playlist}
     */
    public PlaylistEntry addQuery(int position, Query query) {
        PlaylistEntry entry = PlaylistEntry.get(mId, query,
                TomahawkMainActivity.getLifetimeUniqueStringId());
        mAddedEntries.add(entry);
        Index index = new Index(mAddedEntries.size() - 1, true);
        mIndex.add(position, index);
        mCachedEntries.put(entry, index);
        return entry;
    }

    /**
     * Add the given List of {@link Query}s to this {@link Playlist}
     *
     * @param position the position at which to insert the given List of {@link Query}s
     * @param queries  the List of {@link Query}s to add
     */
    public void addQueries(int position, List<Query> queries) {
        for (Query query : queries) {
            addQuery(position, query);
        }
    }

    /**
     * Remove the given {@link Query} from this playlist
     */
    public boolean deleteEntry(PlaylistEntry entry) {
        Index index = mCachedEntries.get(entry);
        if (index == null) {
            throw new RuntimeException("Couldn't find cached PlaylistEntry.");
        }
        return mIndex.remove(index);
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

    public PlaylistEntry getEntryAtPos(int position) {
        Index index = mIndex.get(position);
        return getEntry(index);
    }

    public int getIndexOfEntry(PlaylistEntry entry) {
        Index index = mCachedEntries.get(entry);
        return mIndex.indexOf(index);
    }

    public String getUserId() {
        if (mHatchetId != null) {
            String[] s = mHatchetId.split("_");
            return s[0];
        }
        return null;
    }
}
