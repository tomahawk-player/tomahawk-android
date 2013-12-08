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
package org.tomahawk.libtomahawk.resolver;

import java.util.Comparator;

/**
 * This class is used to compare two {@link org.tomahawk.libtomahawk.resolver.Result}s.
 */
public class ResultComparator implements Comparator<Result> {

    //Modes which determine with which method are compared

    public static final int COMPARE_SCORE = 2;

    //Flag containing the current mode to be used
    private static int mFlag = COMPARE_SCORE;

    /**
     * Construct this {@link ResultComparator}
     *
     * @param flag The mode which determines with which method {@link org.tomahawk.libtomahawk.resolver.Result}s
     *             are compared
     */
    public ResultComparator(int flag) {
        super();
        mFlag = flag;
    }

    /**
     * The actual comparison method
     *
     * @param r1 First {@link org.tomahawk.libtomahawk.resolver.Result} object
     * @param r2 Second {@link org.tomahawk.libtomahawk.resolver.Result} Object
     * @return int containing comparison score
     */
    public int compare(Result r1, Result r2) {
        switch (mFlag) {
            case COMPARE_SCORE:
                Float score1 = r1.getScore();
                Float score2 = r2.getScore();
                return score2.compareTo(score1);
        }
        return 0;
    }
}
