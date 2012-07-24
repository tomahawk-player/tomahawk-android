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
package org.tomahawk.libtomahawk;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class LocalCollection extends Collection {

    private static final String TAG = LocalCollection.class.getName();

    private ContentResolver mResolver;

    private HashMap<Long, Artist> mArtists;

    /**
     * Construct a new LocalCollection and initialize.
     * 
     * @param resolver
     */
    public LocalCollection(ContentResolver resolver) {
        mResolver = resolver;
        mArtists = new HashMap<Long, Artist>();

        initializeCollection();
    }

    /**
     * Get all Artist's associated with this Collection.
     */
    @Override
    public ArrayList<Artist> getArtists() {
        return new ArrayList<Artist>(mArtists.values());
    }

    /**
     * Get all Album's from this Collection.
     */
    @Override
    public ArrayList<Album> getAlbums() {
        ArrayList<Album> albums = new ArrayList<Album>();
        for (Artist artist : mArtists.values()) {

            albums.addAll(artist.getAlbums());
        }
        return albums;
    }

    /**
     * Return a list of all Tracks from the album.
     */
    @Override
    public ArrayList<Track> getTracks() {
        ArrayList<Track> tracks = new ArrayList<Track>();
        for (Artist artist : mArtists.values()) {

            for (Album album : artist.getAlbums()) {
                tracks.addAll(album.getTracks());
            }
        }
        return tracks;
    }

    /**
     * Returns whether this Collection is a local collection.
     */
    @Override
    public boolean isLocal() {
        return true;
    }

    /**
     * Initialize the LocalCollection of all music files on the device.
     */
    private void initializeCollection() {
        initializeArtists();
    }

    /**
     * Initialize Artists on device.
     */
    private void initializeArtists() {

        Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;

        String[] proj = { MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.ARTIST };

        Cursor cursor = mResolver.query(uri, proj, null, null, null);

        while (cursor != null && cursor.moveToNext()) {
            Log.d(TAG, "New Artist : " + cursor.getString(1));
            Artist artist = new Artist(cursor.getLong(0));
            artist.populate(cursor);

            mArtists.put(cursor.getLong(0), artist);
            initializeAlbums(artist);
        }
    }

    /**
     * Initialize Albums from Artist.
     * 
     * @param artist
     */
    private void initializeAlbums(Artist artist) {
        Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.ARTIST_ID + " == "
                + Long.toString(artist.getId());
        String[] proj = { MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ART, MediaStore.Audio.Albums.FIRST_YEAR,
                MediaStore.Audio.Albums.LAST_YEAR, MediaStore.Audio.Media.ARTIST_ID };

        Cursor cursor = mResolver.query(uri, proj, selection, null, null);

        while (cursor != null && cursor.moveToNext()) {
            Log.d(TAG, "New Album : " + cursor.getString(1));
            Album album = new Album(cursor.getLong(0));
            album.populate(cursor);
            album.setArtist(artist);

            mArtists.get(cursor.getLong(5)).addAlbum(album);
            initializeTracks(album);
        }
    }

    /**
     * Initialize Tracks from Album.
     * 
     * @param album
     */
    private void initializeTracks(Album album) {
        String selection = MediaStore.Audio.Media.ALBUM_ID + " == " + Long.toString(album.getId());
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.Audio.Media._ID,
                         MediaStore.Audio.Media.DATA,
                         MediaStore.Audio.Media.TITLE,
                         MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK };

        Cursor cursor = mResolver.query(uri, projection, selection, null, null);

        while (cursor != null && cursor.moveToNext()) {
            Log.d(TAG, "New Track : " + cursor.getString(2));
            Track track = new Track(cursor.getLong(0));
            track.populate(cursor);
            track.setAlbum(album);
            track.setArtist(album.getArtist());

            album.addTrack(track);
        }
    }
}
