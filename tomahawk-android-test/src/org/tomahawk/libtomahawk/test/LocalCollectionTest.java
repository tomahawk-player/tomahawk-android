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

import org.tomahawk.libtomahawk.LocalCollection;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;

import android.database.Cursor;
import android.provider.MediaStore;
import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LocalCollectionTest extends AndroidTestCase {

    private Map<Long, Artist> testArtists;

    private Map<Long, Album> testAlbums;

    private Map<Long, Track> testTracks;

    public void setUp() {
        testArtists = new HashMap<Long, Artist>();
        testAlbums = new HashMap<Long, Album>();
        testTracks = new HashMap<Long, Track>();
        initializeTestTracks();
    }

    public void testGetArtists() {
        LocalCollection coll = new LocalCollection(getContext());

        ArrayList<Long> artistIds = new ArrayList<Long>();

        for (Artist artist : testArtists.values()) {
            artistIds.add(artist.getId());
        }

        for (Artist artist : coll.getArtists()) {
            Assert.assertTrue(artistIds.contains(artist.getId()));
        }
    }

    public void testGetAlbums() {
        LocalCollection coll = new LocalCollection(getContext());

        ArrayList<Long> albumIds = new ArrayList<Long>();

        for (Album album : testAlbums.values()) {
            albumIds.add(album.getId());
        }

        for (Album album : coll.getAlbums()) {
            Assert.assertTrue(albumIds.contains(album.getId()));
        }
    }

    public void testGetTracks() {
        LocalCollection coll = new LocalCollection(getContext());

        ArrayList<Long> trackIds = new ArrayList<Long>();

        for (Track track : testTracks.values()) {
            trackIds.add(track.getId());
        }

        for (Track track : coll.getTracks()) {
            Assert.assertTrue(trackIds.contains(track.getId()));
        }
    }

    public void testIsLocal() {
        Assert.assertTrue((new LocalCollection(getContext()).isLocal()));
    }

    private void initializeTestTracks() {

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM};

        Cursor cursor = getContext().getContentResolver()
                .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
                        null);

        while (cursor != null && cursor.moveToNext()) {
            Artist artist = testArtists.get(cursor.getLong(5));
            if (artist == null) {
                artist = new Artist(cursor.getLong(5));
                artist.setName(cursor.getString(6));

                testArtists.put(artist.getId(), artist);
            }

            Album album = testAlbums.get(cursor.getLong(7));
            if (album == null) {
                album = new Album(cursor.getLong(7));
                album.setName(cursor.getString(8));

                String albumsel = MediaStore.Audio.Albums._ID + " == " + Long
                        .toString(album.getId());

                String[] albumproj = {MediaStore.Audio.Albums.ALBUM_ART,
                        MediaStore.Audio.Albums.FIRST_YEAR, MediaStore.Audio.Albums.LAST_YEAR};

                Cursor albumcursor = getContext().getContentResolver()
                        .query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumproj, albumsel,
                                null, null);

                if (albumcursor != null && albumcursor.moveToNext()) {

                    album.setAlbumArt(albumcursor.getString(0));
                    album.setFirstYear(albumcursor.getString(1));
                    album.setLastYear(albumcursor.getString(2));

                    testAlbums.put(album.getId(), album);
                }

                albumcursor.close();
            }

            Track track = testTracks.get(cursor.getLong(0));
            if (track == null) {
                track = new Track(cursor.getLong(0));
                track.setPath(cursor.getString(1));
                track.setTitle(cursor.getString(2));
                track.setDuration(cursor.getLong(3));
                track.setAlbumPos(cursor.getInt(4));

                testTracks.put(track.getId(), track);
            }

            artist.addAlbum(album);
            artist.addQuery(track);

            album.addQuery(track);
            album.setArtist(artist);

            track.setAlbum(album);
            track.setArtist(artist);
        }

        cursor.close();
    }
}
