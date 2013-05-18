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

public class Source {

    private int mId;

    private String mName;

    private Collection mCollection;

    /**
     * Constructs a new Source from the given id.
     */
    public Source(Collection coll, int id, String name) {
        mId = id;
        mName = name;
        mCollection = coll;
    }

    /**
     * Returns whether this source is local.
     */
    public boolean isLocal() {
        return mCollection.isLocal();
    }

    /**
     * Returns the name of this source.
     */
    public String getName() {
        return mName;
    }

    public int getId() {
        return mId;
    }

    public Collection getCollection() {
        return mCollection;
    }
}
