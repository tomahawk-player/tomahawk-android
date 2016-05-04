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

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a entry in a playlist. It is needed because we need to be able to
 * distinguish for example between two same queries in a Playlist.
 */
public class PlaylistEntry extends Cacheable {

    private final String mId;

    private final Query mQuery;

    private final String mPlaylistId;

    /**
     * Construct a new {@link org.tomahawk.libtomahawk.collection.PlaylistEntry}
     */
    private PlaylistEntry(String playlistId, Query query, String entryId) {
        super(PlaylistEntry.class, getCacheKey(playlistId, entryId));

        mPlaylistId = playlistId;
        mQuery = query;
        mId = entryId;
    }

    /**
     * Returns the {@link PlaylistEntry} with the given parameters. If none exists in our static
     * {@link ConcurrentHashMap} yet, construct and add it.
     *
     * @return {@link PlaylistEntry} with the given parameters
     */
    public static PlaylistEntry get(String playlistId, Query query, String entryId) {
        Cacheable cacheable = get(PlaylistEntry.class, getCacheKey(playlistId, entryId));
        return cacheable != null ? (PlaylistEntry) cacheable
                : new PlaylistEntry(playlistId, query, entryId);
    }

    public static PlaylistEntry getByKey(String cacheKey) {
        return (PlaylistEntry) get(PlaylistEntry.class, cacheKey);
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

    public Artist getArtist() {
        return mQuery.getArtist();
    }

    public Album getAlbum() {
        return mQuery.getAlbum();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "( playlistId: " + mPlaylistId + ", entryId: " + mId
                + ", " + mQuery.toShortString() + " )@" + Integer.toHexString(hashCode());
    }
}
