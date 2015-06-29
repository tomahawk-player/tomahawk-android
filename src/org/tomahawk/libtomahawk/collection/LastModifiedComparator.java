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
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to compare two {@link Object}s.
 */
public class LastModifiedComparator<T> implements Comparator<T> {

    private Map<T, Long> mTimeStampMap;

    /**
     * Construct this {@link LastModifiedComparator}
     *
     * @param timeStampMap the ConcurrentHashMap used to determine the timeStamps of the
     *                     TomahawkListItems which will be sorted
     */
    public LastModifiedComparator(Map<T, Long> timeStampMap) {
        mTimeStampMap = new HashMap<>(timeStampMap);
    }

    /**
     * The actual comparison method
     *
     * @param o1 First object to compare
     * @param o2 Second object to compare
     * @return int containing comparison score
     */
    @Override
    public int compare(T o1, T o2) {
        Long a1TimeStamp = mTimeStampMap.get(o1);
        if (a1TimeStamp == null) {
            a1TimeStamp = 0L;
        }
        Long a2TimeStamp = mTimeStampMap.get(o2);
        if (a2TimeStamp == null) {
            a2TimeStamp = 0L;
        }

        if (a1TimeStamp > a2TimeStamp) {
            return -1;
        } else if (a1TimeStamp < a2TimeStamp) {
            return 1;
        } else {
            return 0;
        }
    }
}
