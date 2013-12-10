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
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;

import java.util.ArrayList;

/**
 * A {@link UserPlaylist} is a {@link Playlist} created by the user and stored in the database
 */
public class UserPlaylist extends Playlist implements TomahawkBaseAdapter.TomahawkListItem {

    /**
     * Create a {@link UserPlaylist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link UserPlaylist}
     */
    public static UserPlaylist fromQueryList(long id, String name, ArrayList<Query> queries) {
        UserPlaylist pl = new UserPlaylist(id, name);
        pl.setQueries(queries);
        pl.setCurrentQueryIndex(0);
        return pl;
    }

    /**
     * Create a {@link UserPlaylist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link UserPlaylist}
     */
    public static UserPlaylist fromQueryList(String name, ArrayList<Query> queries) {
        UserPlaylist pl = UserPlaylist.fromQueryList(TomahawkApp.getUniqueId(), name, queries);
        return pl;
    }

    /**
     * Creates a {@link UserPlaylist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s
     * and sets the current {@link org.tomahawk.libtomahawk.resolver.Query}
     *
     * @return a reference to the constructed {@link UserPlaylist}
     */
    public static UserPlaylist fromQueryList(long id, String name, ArrayList<Query> queries,
            int currentQueryIndex) {
        UserPlaylist pl = UserPlaylist.fromQueryList(id, name, queries);
        pl.setCurrentQueryIndex(currentQueryIndex);
        return pl;
    }

    /**
     * Creates a {@link UserPlaylist} from a list of {@link org.tomahawk.libtomahawk.resolver.Query}s
     * and sets the current {@link org.tomahawk.libtomahawk.resolver.Query}
     *
     * @return a reference to the constructed {@link UserPlaylist}
     */
    public static UserPlaylist fromQueryList(String name, ArrayList<Query> queries,
            int currentQueryIndex) {
        UserPlaylist pl = UserPlaylist
                .fromQueryList(TomahawkApp.getUniqueId(), name, queries, currentQueryIndex);
        return pl;
    }

    /**
     * Construct a new empty {@link UserPlaylist}.
     */
    protected UserPlaylist(long id, String name) {
        super(id, name);
    }

    /**
     * @return this object' name
     */
    @Override
    public String getName() {
        return super.getName();
    }

    /**
     * @return always null. This method needed to comply to the {@link org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}
     * interface.
     */
    @Override
    public Artist getArtist() {
        return null;
    }

    /**
     * @return always null. This method needed to comply to the {@link org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}
     * interface.
     */
    @Override
    public Album getAlbum() {
        return null;
    }
}
