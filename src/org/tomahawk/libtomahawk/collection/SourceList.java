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

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a collection of all {@link Source}s, local and remote.
 */
public class SourceList {

    private List<Source> mSources = null;

    private Source mLocalSource = null;

    /**
     * Default constructor.
     */
    public SourceList() {
        mSources = new ArrayList<Source>();
    }

    /**
     * Sets the local {@link Source}.
     */
    public void setLocalSource(Source source) {
        mSources.remove(mLocalSource);
        mLocalSource = source;
        mSources.add(mLocalSource);
    }

    /**
     * @return the {@link Source} that represents this device.
     */
    public Source getLocalSource() {
        return mLocalSource;
    }

    /**
     * @return all {@link Source}s
     */
    public List<Source> getSources() {
        return mSources;
    }

    /**
     * @return the {@link Collection} with this id.
     */
    public Collection getCollectionFromId(int id) {

        for (Source source : getSources()) {
            if (source.getCollection().getId() == id) {
                return source.getCollection();
            }
        }
        return null;
    }
}
