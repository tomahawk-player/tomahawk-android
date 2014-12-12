/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class holds the metadata retrieved via Hatchet.
 */
public class HatchetCollection extends Collection {

    private ConcurrentHashMap<Artist, List<Query>> mArtistTopHits
            = new ConcurrentHashMap<Artist, List<Query>>();

    public HatchetCollection() {
        super(TomahawkApp.PLUGINNAME_HATCHET, "", true);
    }

    public void addArtistTopHits(Artist artist, List<Query> topHits) {
        mArtistTopHits.put(artist, topHits);
    }

    /**
     * @return A {@link java.util.List} of all top hits {@link Track}s from the given Artist.
     */
    public ArrayList<Query> getArtistTopHits(Artist artist) {
        ArrayList<Query> queries = new ArrayList<Query>();
        if (mArtistTopHits.get(artist) != null) {
            queries.addAll(mArtistTopHits.get(artist));
        }
        return queries;
    }
}
