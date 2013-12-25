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
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;

import java.util.ArrayList;

/**
 * A {@link org.tomahawk.libtomahawk.collection.HatchetUserPlaylist} is a {@link
 * org.tomahawk.libtomahawk.collection.Playlist} created by the user and stored in the database
 */
public class HatchetUserPlaylist extends Playlist implements TomahawkBaseAdapter.TomahawkListItem {

    private String mId;

    /**
     * Construct a new empty {@link org.tomahawk.libtomahawk.collection.HatchetUserPlaylist}.
     */
    private HatchetUserPlaylist(String id, String name) {
        super(name);
        mId = id;
    }

    /**
     * Create a {@link org.tomahawk.libtomahawk.collection.HatchetUserPlaylist} from a list of
     * {@link org.tomahawk.libtomahawk.resolver.Query}s.
     *
     * @return a reference to the constructed {@link org.tomahawk.libtomahawk.collection.HatchetUserPlaylist}
     */
    public static HatchetUserPlaylist fromQueryList(String id, String name,
            ArrayList<Query> queries) {
        HatchetUserPlaylist pl = new HatchetUserPlaylist(id, name);
        pl.setQueries(queries);
        pl.setCurrentQueryIndex(0);
        return pl;
    }

    public String getId() {
        return mId;
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
