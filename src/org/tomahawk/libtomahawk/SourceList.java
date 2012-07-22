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
package org.tomahawk.libtomahawk;

/**
 * This class represents a collection of all Sources, local and remote.
 */
public class SourceList {

    private static SourceList mInstance = null;

    private Source mLocalSource = null;

    /**
     * Returns instance of SourceList.
     * 
     * @return
     */
    public static SourceList instance() {
        if (mInstance == null)
            mInstance = new SourceList();
        return mInstance;
    }

    /**
     * Default constructor.
     */
    protected SourceList() {
    }

    /**
     * Sets the local source.
     * 
     * @param source
     */
    public void setLocalSource(Source source) {
        mLocalSource = source;
    }

    /**
     * Returns the Source that represents this device.
     * 
     * @return
     */
    public Source getLocalSource() {
        return mLocalSource;
    }
}
