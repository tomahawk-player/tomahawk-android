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
 * This class is used to compare two {@link org.tomahawk.libtomahawk.resolver.Query}s.
 */
public class QueryComparator implements Comparator<Query> {

    //Modes which determine with which method are compared
    public static final int COMPARE_ALBUMPOS = 0;

    public static final int COMPARE_ALPHA = 1;

    public static final int COMPARE_TRACK_SCORE = 2;

    //Flag containing the current mode to be used
    private static int mFlag = COMPARE_ALBUMPOS;

    /**
     * Construct this {@link QueryComparator}
     *
     * @param flag The mode which determines with which method {@link org.tomahawk.libtomahawk.resolver.Query}s
     *             are compared
     */
    public QueryComparator(int flag) {
        super();
        mFlag = flag;
    }

    /**
     * The actual comparison method
     *
     * @param q1 First {@link org.tomahawk.libtomahawk.resolver.Query} object
     * @param q2 Second {@link org.tomahawk.libtomahawk.resolver.Query} Object
     * @return int containing comparison score
     */
    public int compare(Query q1, Query q2) {
        switch (mFlag) {
            case COMPARE_ALBUMPOS:
                Integer num1 = q1.getPreferredTrack().getAlbumPos();
                Integer num2 = q2.getPreferredTrack().getAlbumPos();
                return num1.compareTo(num2);
            case COMPARE_ALPHA:
                return q1.getName().compareTo(q2.getName());
            case COMPARE_TRACK_SCORE:
                Float score1 = 0f;
                if (q1.getPreferredTrackResult() != null) {
                    score1 = q1.getPreferredTrackResult().getTrackScore();
                }
                Float score2 = 0f;
                if (q2.getPreferredTrackResult() != null) {
                    score2 = q2.getPreferredTrackResult().getTrackScore();
                }
                int result = score2.compareTo(score1);
                if (result == 0) {
                    Integer weight1 = 0;
                    if (q1.getPreferredTrackResult() != null) {
                        weight1 = q1.getPreferredTrackResult().getResolvedBy().getWeight();
                    }
                    Integer weight2 = 0;
                    if (q1.getPreferredTrackResult() != null) {
                        weight2 = q2.getPreferredTrackResult().getResolvedBy().getWeight();
                    }
                    result = weight1.compareTo(weight2);
                }
                return result;
        }
        return 0;
    }
}
