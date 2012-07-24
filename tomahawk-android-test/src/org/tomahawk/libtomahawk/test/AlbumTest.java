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
import org.tomahawk.libtomahawk.Track;

import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
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

    public void testPopulate() {

        Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;

        String[] proj = { MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ART, MediaStore.Audio.Albums.FIRST_YEAR,
                MediaStore.Audio.Albums.LAST_YEAR, MediaStore.Audio.Media.ARTIST_ID };

        Cursor cursor = getContext().getContentResolver().query(uri, proj, null, null, null);

        if (cursor != null && cursor.moveToNext()) {
            Album album = new Album(cursor.getLong(0));
            album.populate(cursor);

            Assert.assertEquals(album.getId(), cursor.getLong(0));
            Assert.assertEquals(album.getName(), cursor.getString(1));
            Assert.assertEquals(album.getAlbumArt(), cursor.getString(2));
            Assert.assertEquals(album.getFirstYear(), cursor.getString(3));
            Assert.assertEquals(album.getLastYear(), cursor.getString(4));
        } else
            Assert.assertTrue(false);
    }

    public void testAddTrack() {
        long tstId = 4574657L;
        Track tstTrack = new Track(tstId);

        mTstAlbum.addTrack(tstTrack);
        Assert.assertTrue(mTstAlbum.getTracks().contains(tstTrack));
    }
}
