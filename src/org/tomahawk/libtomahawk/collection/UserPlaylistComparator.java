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

import java.util.Comparator;

/**
 * This class is used to compare two {@link org.tomahawk.libtomahawk.collection.UserPlaylist}s.
 */
public class UserPlaylistComparator implements Comparator<UserPlaylist> {

    //Modes which determine with which method albums are compared
    public static final int COMPARE_ALPHA = 1;

    //Flag containing the current mode to be used
    private static int mFlag = COMPARE_ALPHA;

    /**
     * Construct this {@link org.tomahawk.libtomahawk.collection.UserPlaylistComparator}
     *
     * @param flag The mode which determines with which method {@link org.tomahawk.libtomahawk.collection.UserPlaylist}s
     *             are compared
     */
    public UserPlaylistComparator(int flag) {
        super();
        mFlag = flag;
    }

    /**
     * The actual comparison method
     *
     * @param u1 First {@link org.tomahawk.libtomahawk.collection.UserPlaylist} object
     * @param u2 Second {@link org.tomahawk.libtomahawk.collection.UserPlaylist} Object
     * @return int containing comparison score
     */
    public int compare(UserPlaylist u1, UserPlaylist u2) {
        switch (mFlag) {
            case COMPARE_ALPHA:
                return u1.getName().compareTo(u2.getName());
        }
        return 0;
    }
}
