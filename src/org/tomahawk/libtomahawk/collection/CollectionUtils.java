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

import org.jdeferred.Deferred;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.TomahawkApp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class CollectionUtils {

    public static Deferred<Set<Album>, String, Object> getArtistAlbums(Artist artist,
            Collection collection) {
        if (collection != null) {
            return collection.getArtistAlbums(artist, false);
        } else {
            HatchetCollection hatchetCollection = (HatchetCollection) CollectionManager
                    .getInstance().getCollection(TomahawkApp.PLUGINNAME_HATCHET);
            return hatchetCollection.getArtistAlbums(artist, false);
        }
    }

    public static Deferred<Set<Query>, String, Object> getAlbumTracks(Album album,
            Collection collection) {
        if (collection != null) {
            return collection.getAlbumTracks(album, true);
        } else {
            HatchetCollection hatchetCollection = (HatchetCollection) CollectionManager
                    .getInstance().getCollection(TomahawkApp.PLUGINNAME_HATCHET);
            return hatchetCollection.getAlbumTracks(album, true);
        }
    }

    public static ArrayList<Query> getArtistTopHits(Artist artist) {
        HatchetCollection hatchetCollection = (HatchetCollection) CollectionManager
                .getInstance().getCollection(TomahawkApp.PLUGINNAME_HATCHET);
        return hatchetCollection.getArtistTopHits(artist);
    }

    public static boolean allFromOneArtist(java.util.Collection<Query> items) {
        if (items.size() < 2) {
            return true;
        }
        Iterator<Query> iterator = items.iterator();
        Query item = iterator.next();
        for (int i = 1; i < items.size(); i++) {
            Query itemToCompare = iterator.next();
            if (itemToCompare.getArtist() != item.getArtist()) {
                return false;
            }
            item = itemToCompare;
        }
        return true;
    }
}
