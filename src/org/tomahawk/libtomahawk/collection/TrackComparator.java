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

import java.util.Comparator;

/**
 * This class is used to compare two Tracks.
 */
public class TrackComparator implements Comparator<Track> {

    public static final int COMPARE_DISCNUM = 0;

    public static final int COMPARE_ALPHA = 1;

    public static final int COMPARE_SCORE = 2;

    private static int mFlag = COMPARE_DISCNUM;

    public TrackComparator(int flag) {
        super();
        mFlag = flag;
    }

    public int compare(Track t1, Track t2) {

        switch (mFlag) {

            case COMPARE_DISCNUM:
                Integer num1 = t1.getTrackNumber();
                Integer num2 = t2.getTrackNumber();
                return num1.compareTo(num2);

            case COMPARE_ALPHA:
                return t1.getName().compareTo(t2.getName());

            case COMPARE_SCORE:
                Float score1 = t1.getScore();
                Float score2 = t2.getScore();
                return score2.compareTo(score1);

        }

        return 0;
    }
}
