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
package org.tomahawk.libtomahawk.collection;

import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a entry in a playlist. It is needed because we need to be able to
 * distinguish for example between two same queries in a Playlist.
 */
public class PlaylistEntry implements TomahawkListItem {

    private static ConcurrentHashMap<String, PlaylistEntry> sPlaylistEntries
            = new ConcurrentHashMap<String, PlaylistEntry>();

    private String mCacheKey;

    private String mId;

    private Query mQuery;

    private String mPlaylistId;

    /**
     * Construct a new {@link org.tomahawk.libtomahawk.collection.PlaylistEntry}
     */
    private PlaylistEntry(String playlistId, Query query, String entryId) {
        mPlaylistId = playlistId;
        mQuery = query;
        mId = entryId;
        mCacheKey = TomahawkUtils.getCacheKey(this);
    }

    /**
     * Returns the {@link PlaylistEntry} with the given parameters. If none exists in our static
     * {@link ConcurrentHashMap} yet, construct and add it.
     *
     * @return {@link PlaylistEntry} with the given parameters
     */
    public static PlaylistEntry get(String playlistId, Query query, String entryId) {
        PlaylistEntry entry = new PlaylistEntry(playlistId, query, entryId);
        return ensureCache(entry);
    }

    /**
     * If PlaylistEntry is already in our cache, return that. Otherwise add it to the cache.
     */
    private static PlaylistEntry ensureCache(PlaylistEntry playlistEntry) {
        if (!sPlaylistEntries.containsKey(playlistEntry.getCacheKey())) {
            sPlaylistEntries.put(playlistEntry.getCacheKey(), playlistEntry);
        }
        return sPlaylistEntries.get(playlistEntry.getCacheKey());
    }

    /**
     * Get the {@link org.tomahawk.libtomahawk.collection.PlaylistEntry} by providing its cache key
     */
    public static PlaylistEntry getPlaylistEntryByKey(String key) {
        return sPlaylistEntries.get(key);
    }

    @Override
    public String getCacheKey() {
        return mCacheKey;
    }

    @Override
    public String getName() {
        return mQuery.getName();
    }

    @Override
    public Artist getArtist() {
        return mQuery.getArtist();
    }

    @Override
    public Album getAlbum() {
        return mQuery.getAlbum();
    }

    @Override
    public ArrayList<Query> getQueries() {
        return mQuery.getQueries();
    }

    @Override
    public Image getImage() {
        return mQuery.getImage();
    }

    public String getId() {
        return mId;
    }

    public String getPlaylistId() {
        return mPlaylistId;
    }

    public Query getQuery() {
        return mQuery;
    }
}
