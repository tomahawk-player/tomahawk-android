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

import android.util.Log;

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

    private static final String TAG = Playlist.class.getSimpleName();

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

    private boolean mIsLocal;

    private String mHatchetId;

    private String mCurrentRevision = "";

    private String[] mTopArtistNames;

    private long mCount = -1;

    private boolean mIsFilled;

    private String mUserId;

    /**
     * Construct a new empty {@link Playlist}.
     */
    private Playlist(String id, boolean isLocal) {
        super(Playlist.class, getCacheKey(isLocal, id));

        mId = id;
        mIsLocal = isLocal;
    }

    /**
     * Returns the {@link Playlist} with the given parameters. If none exists in our static {@link
     * ConcurrentHashMap} yet, construct and add it.
     *
     * @return {@link Playlist} with the given parameters
     */
    public static Playlist get(String id, boolean isLocal) {
        Cacheable cacheable = get(Playlist.class, getCacheKey(isLocal, id));
        return cacheable != null ? (Playlist) cacheable : new Playlist(id, isLocal);
    }

    /**
     * Create a {@link Playlist} from a list of {@link PlaylistEntry}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromEntriesList(String id, boolean isLocal, String name,
            String currentRevision, List<PlaylistEntry> entries) {
        CollectionCursor<PlaylistEntry> cursor =
                new CollectionCursor<>(entries, PlaylistEntry.class);
        return fromCursor(id, isLocal, name, currentRevision, cursor);
    }

    /**
     * Create a {@link Playlist} from a list of {@link PlaylistEntry}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromEmptyList(String id, boolean isLocal, String name) {
        CollectionCursor<PlaylistEntry> cursor =
                new CollectionCursor<>(new ArrayList<PlaylistEntry>(), PlaylistEntry.class);
        return fromCursor(id, isLocal, name, null, cursor);
    }

    /**
     * Create a {@link Playlist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromQueryList(String id, boolean isLocal, String name,
            String currentRevision, List<Query> queries) {
        List<PlaylistEntry> entries = new ArrayList<>();
        for (Query query : queries) {
            entries.add(PlaylistEntry.get(id, query,
                    TomahawkMainActivity.getLifetimeUniqueStringId()));
        }
        CollectionCursor<PlaylistEntry> cursor =
                new CollectionCursor<>(entries, PlaylistEntry.class);
        return fromCursor(id, isLocal, name, currentRevision, cursor);
    }

    /**
     * Create a {@link Playlist} from a {@link CollectionCursor} containing {@link PlaylistEntry}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromCursor(String id, boolean isLocal, String name,
            String currentRevision, CollectionCursor<PlaylistEntry> cursor) {
        Playlist pl = Playlist.get(id, isLocal);
        pl.setName(name);
        pl.setCurrentRevision(currentRevision);
        pl.setCursor(cursor);
        return pl;
    }

    public void clear() {
        mAddedEntries.clear();
        mCachedEntries.clear();
        mIndex.clear();
        mCursor = null;
    }

    public void setCursor(CollectionCursor<PlaylistEntry> cursor) {
        mCursor = cursor;
        initIndex();
    }

    private void initIndex() {
        mAddedEntries.clear();
        mCachedEntries.clear();
        mIndex.clear();
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
        for (int i = 0; i < size(); i++) {
            String artistName = getArtistName(i);
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
     * @return this {@link Playlist}'s name
     */
    @Override
    public String toString() {
        return mName;
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
            PlaylistEntry entry = getEntry(index);
            entries.add(entry);
            mCachedEntries.put(entry, index);
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
     * Remove the given {@link Query} from this playlist
     */
    public boolean deleteEntry(PlaylistEntry entry) {
        Index index = mCachedEntries.get(entry);
        if (index == null) {
            Log.d(TAG, "deleteEntry - couldn't find cached PlaylistEntry.");
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
        if (position < 0 || position >= mIndex.size()) {
            return null;
        }
        Index index = mIndex.get(position);
        return getEntry(index);
    }

    public int getIndexOfEntry(PlaylistEntry entry) {
        Index index = mCachedEntries.get(entry);
        return mIndex.indexOf(index);
    }

    public String getUserId() {
        return mUserId;
    }

    public void setUserId(String userId) {
        mUserId = userId;
    }

    public boolean allFromOneArtist() {
        if (size() < 2) {
            return true;
        }
        String artistname = getArtistName(0);
        for (int i = 1; i < size(); i++) {
            String artistNameToCompare = getArtistName(i);
            if (!artistNameToCompare.equals(artistname)) {
                return false;
            }
            artistname = artistNameToCompare;
        }
        return true;
    }

    public String getArtistName(int position) {
        Index index = mIndex.get(position);
        if (index.mFromMergedItems) {
            return mAddedEntries.get(index.mIndex).getArtist().getName();
        } else {
            return mCursor.getArtistName(index.mIndex);
        }
    }
}
