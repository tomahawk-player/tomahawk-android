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
package org.tomahawk.tomahawk_android.utils;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.resolver.Query;

import java.util.ArrayList;

/**
 * This interface represents an item displayed in some sort of list.
 */
public interface TomahawkListItem {

    /**
     * @return the unique cache key of this object
     */
    public String getCacheKey();

    /**
     * @return the corresponding name/title
     */
    public String getName();

    /**
     * @return the corresponding {@link org.tomahawk.libtomahawk.collection.Artist}
     */
    public Artist getArtist();

    /**
     * @return the corresponding {@link org.tomahawk.libtomahawk.collection.Album}
     */
    public Album getAlbum();

    /**
     * @return the corresponding list of {@link org.tomahawk.libtomahawk.resolver.Query}s
     */
    public ArrayList<Query> getQueries();

    /**
     * @return a corresponding image like an artist's portrait image or an album cover
     */
    public Image getImage();
}