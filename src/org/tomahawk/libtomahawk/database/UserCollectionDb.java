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
package org.tomahawk.libtomahawk.database;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.List;

public class UserCollectionDb extends CollectionDb {

    public static final String TAG = UserCollectionDb.class.getSimpleName();

    public UserCollectionDb(Context context, String collectionId) {
        super(context, collectionId);
    }

    public void addArtists(List<Artist> artists, List<Long> lastModifieds) {
        mDb.beginTransaction();
        for (int i = 0, artistsSize = artists.size(); i < artistsSize; i++) {
            Artist artist = artists.get(i);
            ContentValues values = new ContentValues();
            values.put(ARTISTS_ARTIST, artist.getName());
            values.put(ARTISTS_ARTISTDISAMBIGUATION, "");
            values.put(ARTISTS_TYPE, TYPE_HATCHET_EXPLICIT);
            long lastModified;
            if (lastModifieds != null && i < lastModifieds.size()) {
                lastModified = lastModifieds.get(i);
            } else {
                lastModified = Long.MAX_VALUE;
            }
            values.put(ARTISTS_LASTMODIFIED, lastModified);
            mDb.insert(TABLE_ARTISTS, null, values);
        }
        mDb.setTransactionSuccessful();
        mDb.endTransaction();
    }

    public void remove(Artist artist) {
        mDb.beginTransaction();
        mDb.delete(TABLE_ARTISTS, ARTISTS_ARTIST + " = ? AND " + ARTISTS_TYPE + " = ?",
                new String[]{artist.getName(), String.valueOf(TYPE_HATCHET_EXPLICIT)});
        mDb.setTransactionSuccessful();
        mDb.endTransaction();
    }

    /**
     * Checks if an artist with the same artistName as the given artist is loved
     *
     * @return whether or not the given artist is loved
     */
    public boolean isLoved(Artist artist) {
        String[] columns = new String[]{ID};
        Cursor artistsCursor = mDb.query(TABLE_ARTISTS, columns,
                ARTISTS_ARTIST + " = ? AND " + ARTISTS_TYPE + " = ?",
                new String[]{artist.getName(), String.valueOf(TYPE_HATCHET_EXPLICIT)}, null, null,
                null);
        boolean isLoved = artistsCursor.getCount() > 0;
        artistsCursor.close();
        return isLoved;
    }

    public void addAlbums(List<Album> albums, List<Long> lastModifieds) {
        // Add the album's artist as an implicitly loved entry
        mDb.beginTransaction();
        for (Album album : albums) {
            ContentValues values = new ContentValues();
            values.put(ARTISTS_ARTIST, album.getArtist().getName());
            values.put(ARTISTS_ARTISTDISAMBIGUATION, "");
            values.put(ARTISTS_TYPE, TYPE_HATCHET_IMPLICIT);
            values.put(ARTISTS_LASTMODIFIED, Long.MAX_VALUE);
            mDb.insert(TABLE_ARTISTS, null, values);
        }
        mDb.setTransactionSuccessful();
        mDb.endTransaction();

        // Add the album as an explicitly loved entry
        mDb.beginTransaction();
        for (int i = 0, albumsSize = albums.size(); i < albumsSize; i++) {
            Album album = albums.get(i);
            ContentValues values = new ContentValues();
            values.put(ALBUMS_ALBUM, album.getName());
            values.put(ALBUMS_ALBUMARTISTID,
                    getArtistId(album.getArtist().getName(), TYPE_HATCHET_IMPLICIT));
            values.put(ALBUMS_TYPE, TYPE_HATCHET_EXPLICIT);
            long lastModified;
            if (lastModifieds != null && i < lastModifieds.size()) {
                lastModified = lastModifieds.get(i);
            } else {
                lastModified = Long.MAX_VALUE;
            }
            values.put(ALBUMS_LASTMODIFIED, lastModified);
            mDb.insert(TABLE_ALBUMS, null, values);
        }
        mDb.setTransactionSuccessful();
        mDb.endTransaction();
    }

    public void remove(Album album) {
        mDb.beginTransaction();
        int albumArtistId = getArtistId(album.getArtist().getName(), TYPE_HATCHET_IMPLICIT);
        mDb.delete(TABLE_ARTISTS, ARTISTS_ARTIST + " = ? AND " + ARTISTS_TYPE + " = ?",
                new String[]{album.getArtist().getName(),
                        String.valueOf(TYPE_HATCHET_IMPLICIT)});
        mDb.delete(TABLE_ALBUMS, ALBUMS_ALBUM + " = ? AND " + ALBUMS_ALBUMARTISTID + " = ? AND "
                        + ALBUMS_TYPE + " = ?",
                new String[]{album.getName(), String.valueOf(albumArtistId),
                        String.valueOf(TYPE_HATCHET_EXPLICIT)});
        mDb.setTransactionSuccessful();
        mDb.endTransaction();
    }

    /**
     * Checks if an album with the same albumName as the given album is loved
     *
     * @return whether or not the given album is loved
     */
    public boolean isLoved(Album album) {
        int albumArtistId = getArtistId(album.getArtist().getName(), TYPE_HATCHET_IMPLICIT);
        String[] columns = new String[]{ID};
        Cursor albumsCursor = mDb.query(TABLE_ALBUMS, columns,
                ALBUMS_ALBUM + " = ? AND " + ALBUMS_ALBUMARTISTID + " = ? AND "
                        + ALBUMS_TYPE + " = ?",
                new String[]{album.getName(), String.valueOf(albumArtistId),
                        String.valueOf(TYPE_HATCHET_EXPLICIT)}, null, null, null
        );
        boolean isLoved = albumsCursor.getCount() > 0;
        albumsCursor.close();
        return isLoved;
    }

    private int getArtistId(String artistName, int artistType) {
        Cursor cursor = null;
        try {
            cursor = mDb.query(TABLE_ARTISTS, new String[]{ID},
                    ARTISTS_ARTIST + " = ? AND " + ARTISTS_TYPE + " = ?",
                    new String[]{artistName, String.valueOf(artistType)}, null, null, null);
            cursor.moveToFirst();
            if (cursor.getCount() == 0) {
                return -1;
            }
            return cursor.getInt(0);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
