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

import java.util.ArrayList;

/**
 * A {@link UserPlaylist} is a {@link org.tomahawk.libtomahawk.collection.Playlist} created by the
 * user and stored in the database
 */
public class UserPlaylist extends Playlist implements TomahawkListItem {

    private String mId;

    private String mCurrentRevision;

    private boolean mIsHatchetPlaylist;

    private ArrayList<Artist> mContentHeaderArtists = new ArrayList<Artist>();

    /**
     * Construct a new empty {@link UserPlaylist}.
     */
    private UserPlaylist(String id, String name, String currentRevision,
            boolean isHatchetPlaylist) {
        super(name);
        mId = id;
        mCurrentRevision = currentRevision;
        mIsHatchetPlaylist = isHatchetPlaylist;
    }

    /**
     * Create a {@link UserPlaylist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link UserPlaylist}
     */
    public static UserPlaylist fromQueryList(String id, String name, String currentRevision,
            boolean isHatchetPlaylist, ArrayList<Query> queries, Query currentQuery) {
        UserPlaylist pl = new UserPlaylist(id, name, currentRevision, isHatchetPlaylist);
        pl.setQueries(queries);
        if (currentQuery == null) {
            pl.setCurrentQueryIndex(0);
        } else {
            pl.setCurrentQuery(currentQuery);
        }
        return pl;
    }

    /**
     * Create a {@link UserPlaylist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link UserPlaylist}
     */
    public static UserPlaylist fromQueryList(String id, String name, String currentRevision,
            ArrayList<Query> queries) {
        return UserPlaylist.fromQueryList(id, name, currentRevision, true, queries, null);
    }

    /**
     * Create a {@link UserPlaylist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link UserPlaylist}
     */
    public static UserPlaylist fromQueryList(String id, String name, ArrayList<Query> queries,
            Query currentQuery) {
        return UserPlaylist.fromQueryList(id, name, null, false, queries, currentQuery);
    }

    /**
     * Create a {@link UserPlaylist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link UserPlaylist}
     */
    public static UserPlaylist fromQueryList(String id, String name, ArrayList<Query> queries) {
        return UserPlaylist.fromQueryList(id, name, null, false, queries, null);
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
        return super.getName();
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
    public ArrayList<Query> getQueries(boolean onlyLocal) {
        return getQueries();
    }
}
