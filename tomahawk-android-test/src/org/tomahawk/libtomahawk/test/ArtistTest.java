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

import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Artist;

import android.test.AndroidTestCase;

public class ArtistTest extends AndroidTestCase {

    private final long mId = 12345L;

    private Artist mTstArtist;

    private String mTestArtistName = "Artist";

    public void setUp() {
        mTstArtist = new Artist(mId);
        mTstArtist.setName(mTestArtistName);
    }

    public void tearDown() {
        mTstArtist = null;
    }

    public void testAddAlbum() {
        long tstId = 123456789L;
        Album tstAlbum = new Album(tstId);
        mTstArtist.addAlbum(tstAlbum);

        Assert.assertTrue(mTstArtist.getAlbums().contains(tstAlbum));
    }
}
