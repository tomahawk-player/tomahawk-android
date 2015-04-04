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

import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to compare two {@link TomahawkListItem}s.
 */
public class TomahawkListItemComparator
        implements Comparator<TomahawkListItem> {

    //Modes which determine with which method are compared
    public static final int COMPARE_ALPHA = 0;

    public static final int COMPARE_ARTIST_ALPHA = 1;

    public static final int COMPARE_RECENTLY_ADDED = 2;

    //Flag containing the current mode to be used
    private int mFlag = COMPARE_ALPHA;

    private HashMap mTimeStampMap;

    /**
     * Construct this {@link TomahawkListItemComparator}
     *
     * @param flag The mode which determines with which method {@link TomahawkListItem}s are
     *             compared
     */
    public TomahawkListItemComparator(int flag) {
        mFlag = flag;
    }

    /**
     * Construct this {@link TomahawkListItemComparator}
     *
     * @param flag         The mode which determines with which method {@link TomahawkListItem}s are
     *                     compared
     * @param timeStampMap the ConcurrentHashMap used to determine the timeStamps of the
     *                     TomahawkListItems which will be sorted
     */
    public TomahawkListItemComparator(int flag, ConcurrentHashMap timeStampMap) {
        mFlag = flag;
        mTimeStampMap = new HashMap(timeStampMap);
    }

    /**
     * The actual comparison method
     *
     * @param a1 First {@link TomahawkListItem} object
     * @param a2 Second {@link TomahawkListItem} Object
     * @return int containing comparison score
     */
    public int compare(TomahawkListItem a1, TomahawkListItem a2) {
        int result = 0;
        switch (mFlag) {
            case COMPARE_ALPHA:
                result = a1.getName().compareToIgnoreCase(a2.getName());
                break;
            case COMPARE_ARTIST_ALPHA:
                result = a1.getArtist().getName().compareToIgnoreCase(a2.getArtist().getName());
                break;
            case COMPARE_RECENTLY_ADDED:
                Long a1TimeStamp;
                if (a1 instanceof Query) {
                    a1TimeStamp = (Long) mTimeStampMap.get(a1);
                } else {
                    a1TimeStamp = (Long) mTimeStampMap.get(a1.getName().toLowerCase());
                }
                Long a2TimeStamp;
                if (a1 instanceof Query) {
                    a2TimeStamp = (Long) mTimeStampMap.get(a2);
                } else {
                    a2TimeStamp = (Long) mTimeStampMap.get(a2.getName().toLowerCase());
                }
                if (a1TimeStamp == null) {
                    a1TimeStamp = 0L;
                }
                if (a2TimeStamp == null) {
                    a2TimeStamp = 0L;
                }

                if (a1TimeStamp > a2TimeStamp) {
                    result = -1;
                } else if (a1TimeStamp < a2TimeStamp) {
                    result = 1;
                } else {
                    result = 0;
                }
                break;
        }
        return result;
    }
}
