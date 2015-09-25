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
 * This class is used to compare two {@link Playlist}s.
 */
public class PlaylistComparator implements Comparator<Playlist> {

    /**
     * Construct this {@link PlaylistComparator}
     */
    public PlaylistComparator() {
    }

    /**
     * The actual comparison method
     *
     * @param p1 First Playlist to compare
     * @param p2 Second Playlist to compare
     * @return int containing comparison score
     */
    @Override
    public int compare(Playlist p1, Playlist p2) {
        int score = p1.getName().compareTo(p2.getName());
        if (score == 0) {
            score = p1.getId().compareTo(p2.getId());
            if (score == 0 && p1.getHatchetId() != null && p2.getHatchetId() != null) {
                score = p1.getHatchetId().compareTo(p2.getHatchetId());
            }
        }
        return score;
    }
}
