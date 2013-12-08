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
package org.tomahawk.libtomahawk.test;

import junit.framework.Assert;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Track;

import android.test.AndroidTestCase;

public class AlbumTest extends AndroidTestCase {

    private final long mId = 12345L;

    private Album mTstAlbum;

    private String mTestAlbumName = "Album";

    public void setUp() {
        mTstAlbum = new Album(mId);
        mTstAlbum.setName(mTestAlbumName);
    }

    public void tearDown() {
        mTstAlbum = null;
    }

    public void testAddTrack() {
        long tstId = 4574657L;
        Track tstTrack = new Track(tstId);

        mTstAlbum.addQuery(tstTrack);
        Assert.assertTrue(mTstAlbum.getQueries().contains(tstTrack));
    }
}
