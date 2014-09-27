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
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link Playlist} is a {@link org.tomahawk.libtomahawk.collection.Playlist} created by the user
 * and stored in the database
 */
public class Playlist implements TomahawkListItem {

    private String mName = "";

    private ArrayList<PlaylistEntry> mEntries = new ArrayList<PlaylistEntry>();

    private static ConcurrentHashMap<String, Playlist> sPlaylists
            = new ConcurrentHashMap<String, Playlist>();

    private String mId;

    private String mHatchetId;

    private String mCurrentRevision = "";

    private ArrayList<Artist> mContentHeaderArtists = new ArrayList<Artist>();

    private boolean mIsFilled;

    /**
     * Construct a new empty {@link Playlist}.
     */
    private Playlist(String id, String name, String currentRevision) {
        if (name != null) {
            mName = name;
        }
        mId = id;
        if (currentRevision != null) {
            mCurrentRevision = currentRevision;
        }
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
            ArrayList<PlaylistEntry> playlistEntries) {
        String id = TomahawkMainActivity.getLifetimeUniqueStringId();
        if (currentRevision == null) {
            currentRevision = "";
        }
        Playlist pl = Playlist.get(id, name, currentRevision);
        pl.setEntries(playlistEntries);
        return pl;
    }

    /**
     * Create a {@link Playlist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromQueryList(String name, String currentRevision,
            ArrayList<Query> queries) {
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
        return pl;
    }

    /**
     * Create a {@link Playlist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link Playlist}
     */
    public static Playlist fromQueryList(String name, ArrayList<Query> queries) {
        return Playlist.fromQueryList(name, null, queries);
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
        if (currentRevision != null) {
            mCurrentRevision = currentRevision;
        } else {
            mCurrentRevision = "";
        }
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
        if (name != null) {
            this.mName = name;
        }
    }

    /**
     * Set this {@link Playlist}'s {@link Query}s
     */
    public void setEntries(ArrayList<PlaylistEntry> entries) {
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
    public ArrayList<PlaylistEntry> getEntries() {
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
     * Append an {@link ArrayList} of {@link Query}s at the end of this playlist
     */
    public void addQueries(ArrayList<Query> queries) {
        ArrayList<PlaylistEntry> playlistEntries = new ArrayList<PlaylistEntry>();
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
    public ArrayList<Query> getQueries() {
        ArrayList<Query> queries = new ArrayList<Query>();
        for (PlaylistEntry entry : mEntries) {
            queries.add(entry.getQuery());
        }
        return queries;
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
}
