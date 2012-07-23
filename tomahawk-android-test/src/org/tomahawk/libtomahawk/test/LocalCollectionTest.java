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

import java.util.ArrayList;

import junit.framework.Assert;

import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.LocalCollection;
import org.tomahawk.libtomahawk.Track;

import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.test.AndroidTestCase;

public class LocalCollectionTest extends AndroidTestCase {

    private ArrayList<Artist> testArtists;
    private ArrayList<Album> testAlbums;
    private ArrayList<Track> testTracks;

    public void setUp() {
        initializeTestArtists();
        initializeTestAlbums();
        initializeTestTracks();
    }

    public void testGetArtists() {
        LocalCollection coll = new LocalCollection(getContext().getContentResolver());

        ArrayList<Long> artistIds = new ArrayList<Long>();

        for (Artist artist : testArtists) {
            artistIds.add(artist.getId());
        }

        for (Artist artist : coll.getArtists()) {
            Assert.assertTrue(artistIds.contains(artist.getId()));
        }
    }

    public void testGetAlbums() {
        LocalCollection coll = new LocalCollection(getContext().getContentResolver());

        ArrayList<Long> albumIds = new ArrayList<Long>();

        for (Album album : testAlbums) {
            albumIds.add(album.getId());
        }

        for (Album album : coll.getAlbums()) {
            Assert.assertTrue(albumIds.contains(album.getId()));
        }
    }

    public void testGetTracks() {
        LocalCollection coll = new LocalCollection(getContext().getContentResolver());

        ArrayList<Long> trackIds = new ArrayList<Long>();

        for (Track track : testTracks) {
            trackIds.add(track.getId());
        }

        for (Track track : coll.getTracks()) {
            Assert.assertTrue(trackIds.contains(track.getId()));
        }
    }

    public void testIsLocal() {
        Assert.assertTrue((new LocalCollection(getContext().getContentResolver()).isLocal()));
    }

    private void initializeTestArtists() {
        testArtists = new ArrayList<Artist>();

        Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;

        String[] proj = { MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.ARTIST };

        Cursor cursor = getContext().getContentResolver().query(uri, proj, null, null, null);

        while (cursor != null && cursor.moveToNext()) {
            Artist artist = new Artist(cursor.getLong(0));
            artist.populate(cursor);

            testArtists.add(artist);
        }
    }

    private void initializeTestAlbums() {
        testAlbums = new ArrayList<Album>();

        Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;

        String[] proj = { MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ART, MediaStore.Audio.Albums.FIRST_YEAR,
                MediaStore.Audio.Albums.LAST_YEAR, MediaStore.Audio.Media.ARTIST_ID };

        Cursor cursor = getContext().getContentResolver().query(uri, proj, null, null, null);

        while (cursor != null && cursor.moveToNext()) {
            Album album = new Album(cursor.getLong(0));
            album.populate(cursor);

            testAlbums.add(album);
        }
    }

    private void initializeTestTracks() {

        testTracks = new ArrayList<Track>();

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK };

        Cursor cursor = getContext().getContentResolver().query(uri, projection, selection, null,
                null);

        while (cursor != null && cursor.moveToNext()) {
            Track track = new Track(cursor.getLong(0));
            track.populate(cursor);

            testTracks.add(track);
        }
    }
}
