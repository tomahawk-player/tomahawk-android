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

    private ArrayList<Query> mQueries;

    private ArrayList<Query> mShuffledQueries;

    private int mCurrentQueryIndex;

    private boolean mShuffled;

    private boolean mRepeating;

    private static ConcurrentHashMap<String, Playlist> sPlaylists
            = new ConcurrentHashMap<String, Playlist>();

    private String mId;

    private String mCurrentRevision;

    private boolean mIsHatchetPlaylist;

    private ArrayList<Artist> mContentHeaderArtists = new ArrayList<Artist>();

    private boolean mIsFilled;

    /**
     * Construct a new empty {@link Playlist}.
     */
    private Playlist(String id, String name, String currentRevision,
            boolean isHatchetPlaylist) {
        mName = name;
        mShuffled = false;
        mRepeating = false;
        setQueries(new ArrayList<Query>());
        mId = id;
        mCurrentRevision = currentRevision;
        mIsHatchetPlaylist = isHatchetPlaylist;
    }

    /**
     * Create a {@link Playlist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromQueryList(String id, String name, String currentRevision,
            boolean isHatchetPlaylist, ArrayList<Query> queries, int currentQueryIndex) {
        if (id == null) {
            id = "";
        }
        if (currentRevision == null) {
            currentRevision = "";
        }
        Playlist pl = new Playlist(id, name, currentRevision, isHatchetPlaylist);
        pl.setQueries(queries);
        pl.setCurrentQueryIndex(currentQueryIndex);
        sPlaylists.put(id, pl);
        return sPlaylists.get(id);
    }

    /**
     * Create a {@link Playlist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromQueryList(String id, String name, String currentRevision,
            ArrayList<Query> queries) {
        return Playlist.fromQueryList(id, name, currentRevision, true, queries, 0);
    }

    /**
     * Create a {@link Playlist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromQueryList(String id, String name, ArrayList<Query> queries,
            int currentQueryIndex) {
        return Playlist.fromQueryList(id, name, null, false, queries, currentQueryIndex);
    }

    /**
     * Create a {@link Playlist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromQueryList(String id, String name, ArrayList<Query> queries) {
        return Playlist.fromQueryList(id, name, null, false, queries, 0);
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

    public String getCurrentRevision() {
        return mCurrentRevision;
    }

    public boolean isHatchetPlaylist() {
        return mIsHatchetPlaylist;
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
    public void setQueries(java.util.Collection<Query> queries) {
        mQueries = (ArrayList<Query>) queries;
        mCurrentQueryIndex = 0;
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
     * @return the current {@link Query}
     */
    public Query getCurrentQuery() {
        List<Query> querys = mShuffled ? mShuffledQueries : mQueries;
        if (querys != null && mCurrentQueryIndex >= 0 && mCurrentQueryIndex < querys.size()) {
            return querys.get(mCurrentQueryIndex);
        }
        return null;
    }

    /**
     * @return the current {@link Query}'s index
     */
    public int getCurrentQueryIndex() {
        return mCurrentQueryIndex;
    }

    /**
     * @return the next {@link Query}
     */
    public Query getNextQuery() {
        List<Query> querys = mShuffled ? mShuffledQueries : mQueries;
        if (mCurrentQueryIndex + 1 < querys.size()) {
            Query query = querys.get(mCurrentQueryIndex + 1);
            mCurrentQueryIndex = mCurrentQueryIndex + 1;
            return query;
        } else if (mRepeating) {
            mCurrentQueryIndex = 0;
            return getFirstQuery();
        }
        return null;
    }

    /**
     * @return the previous {@link Query}
     */
    public Query getPreviousQuery() {
        List<Query> querys = mShuffled ? mShuffledQueries : mQueries;
        if (mCurrentQueryIndex - 1 >= 0) {
            Query query = querys.get(mCurrentQueryIndex - 1);
            mCurrentQueryIndex = mCurrentQueryIndex - 1;
            return query;
        } else if (mRepeating) {
            mCurrentQueryIndex = querys.size() - 1;
            return getLastQuery();
        }
        return null;
    }

    /**
     * Get the {@link Query} at the given position
     */
    public Query getQueryAtPos(int i) {
        if (i >= 0 && i < (mShuffled ? mShuffledQueries.size() : mQueries.size())) {
            mCurrentQueryIndex = i;
            return mShuffled ? mShuffledQueries.get(i) : mQueries.get(i);
        }
        return null;
    }

    /**
     * @return the first {@link Query} of this playlist
     */
    public Query getFirstQuery() {
        if (mShuffled ? mShuffledQueries.isEmpty() : mQueries.isEmpty()) {
            return null;
        }

        return mShuffled ? mShuffledQueries.get(0) : mQueries.get(0);
    }

    /**
     * @return the last {@link Query} of this playlist
     */
    public Query getLastQuery() {
        if (mShuffled ? mShuffledQueries.isEmpty() : mQueries.isEmpty()) {
            return null;
        }

        return mShuffled ? mShuffledQueries.get(mShuffledQueries.size() - 1)
                : mQueries.get(mQueries.size() - 1);
    }

    /**
     * @return this {@link Playlist}'s name
     */
    @Override
    public String toString() {
        return mName;
    }

    /**
     * @return true, if the {@link Playlist} has a next {@link Query}, otherwise false
     */
    public boolean hasNextQuery() {
        return peekNextQuery() != null;
    }

    /**
     * @return true, if the {@link Playlist} has a previous {@link Query}, otherwise false
     */
    public boolean hasPreviousQuery() {
        return peekPreviousQuery() != null;
    }

    /**
     * Returns the next {@link Query} but does not update the internal {@link Query} iterator.
     *
     * @return Returns next {@link Query}. Returns null if there is none.
     */
    public Query peekNextQuery() {
        List<Query> querys = mShuffled ? mShuffledQueries : mQueries;
        if (mCurrentQueryIndex + 1 < querys.size()) {
            return querys.get(mCurrentQueryIndex + 1);
        } else if (mRepeating) {
            return getFirstQuery();
        }
        return null;
    }

    /**
     * Returns the previous {@link Query} but does not update the internal {@link Query} iterator.
     *
     * @return Returns previous {@link Query}. Returns null if there is none.
     */
    public Query peekPreviousQuery() {
        if (mCurrentQueryIndex - 1 >= 0) {
            List<Query> querys = mShuffled ? mShuffledQueries : mQueries;
            return querys.get(mCurrentQueryIndex - 1);
        } else if (mRepeating) {
            return getLastQuery();
        }
        return null;
    }

    /**
     * Returns the {@link Query} at the given position but does not update the internal {@link
     * Query} iterator.
     *
     * @return Returns the {@link Query} at the given position. Returns null if there is none.
     */
    public Query peekQueryAtPos(int i) {
        if (i >= 0 && i < (mShuffled ? mShuffledQueries.size() : mQueries.size())) {
            return mShuffled ? mShuffledQueries.get(i) : mQueries.get(i);
        }
        return null;
    }

    /**
     * Set this {@link Playlist} to shuffle mode.
     */
    @SuppressWarnings("unchecked")
    public void setShuffled(boolean shuffled) {
        Query oldCurrentQuery = getCurrentQuery();
        mShuffled = shuffled;
        int i = 0;

        if (shuffled) {
            mShuffledQueries = (ArrayList<Query>) mQueries.clone();
            Collections.shuffle(mShuffledQueries);
        } else {
            mShuffledQueries = null;
        }

        List<Query> querys = mShuffled ? mShuffledQueries : mQueries;
        while (i < querys.size()) {
            if (oldCurrentQuery == querys.get(i)) {
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
        return mQueries.size();
    }

    /**
     * Return all querys in the {@link Playlist}
     */
    public ArrayList<Query> getQueries() {
        return mShuffled ? mShuffledQueries : mQueries;
    }

    /**
     * Add an {@link ArrayList} of {@link Query}s at the given position
     */
    public void addQueries(int position, ArrayList<Query> querys) {
        (mShuffled ? mShuffledQueries : mQueries).addAll(position, querys);
    }

    /**
     * Append an {@link ArrayList} of {@link Query}s at the end of this playlist
     */
    public void addQueries(ArrayList<Query> querys) {
        (mShuffled ? mShuffledQueries : mQueries).addAll(querys);
    }

    /**
     * Remove the {@link Query} at the given position from this playlist
     */
    public void deleteQueryAtPos(int position) {
        if (mShuffledQueries != null) {
            mShuffledQueries.remove((mShuffled ? mShuffledQueries : mQueries).get(position));
        }
        (mShuffled ? mShuffledQueries : mQueries).remove(position);
        if (mCurrentQueryIndex > (mShuffled ? mShuffledQueries : mQueries).size()) {
            mCurrentQueryIndex--;
        }
    }

    /**
     * Remove the given {@link Query} from this playlist
     */
    public void deleteQuery(Query query) {
        if (mShuffledQueries != null) {
            mShuffledQueries.remove(query);
        }
        (mShuffled ? mShuffledQueries : mQueries).remove(query);
        if (mCurrentQueryIndex > (mShuffled ? mShuffledQueries : mQueries).size()) {
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
