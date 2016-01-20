/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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

import java.util.Comparator;

/**
 * This class is used to compare two {@link Object}s.
 */
public class AlphaComparator implements Comparator<AlphaComparable> {

    /**
     * The comparison method
     *
     * @param o1 First object to compare
     * @param o2 Second object to compare
     * @return int containing comparison score
     */
    @Override
    public int compare(AlphaComparable o1, AlphaComparable o2) {
        return o1.getName().compareToIgnoreCase(o2.getName());
    }
}
