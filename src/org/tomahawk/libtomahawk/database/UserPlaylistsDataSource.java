/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

/**
 * This class provides a way of storing user created {@link org.tomahawk.libtomahawk.collection.UserPlaylist}s
 * in the database
 */
public class UserPlaylistsDataSource {

    public static final String CACHED_PLAYLIST_NAME = "Last used playlist";

    public static final long CACHED_PLAYLIST_ID = 0;

    // Database fields
    private SQLiteDatabase mDatabase;

    private TomahawkSQLiteHelper mDbHelper;

    private PipeLine mPipeLine;

    private String[] mAllUserPlaylistsColumns = {TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID,
            TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_NAME,
            TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_CURRENTTRACKINDEX};

    private String[] mAllTracksColumns = {TomahawkSQLiteHelper.TRACKS_COLUMN_ID,
            TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS,
            TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME,
            TomahawkSQLiteHelper.TRACKS_COLUMN_IDALBUMS,
            TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME, TomahawkSQLiteHelper.TRACKS_COLUMN_PATH,
            TomahawkSQLiteHelper.TRACKS_COLUMN_BITRATE, TomahawkSQLiteHelper.TRACKS_COLUMN_DURATION,
            TomahawkSQLiteHelper.TRACKS_COLUMN_SIZE, TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNUMBER,
            TomahawkSQLiteHelper.TRACKS_COLUMN_YEAR, TomahawkSQLiteHelper.TRACKS_COLUMN_RESOLVERID,
            TomahawkSQLiteHelper.TRACKS_COLUMN_LINKURL,
            TomahawkSQLiteHelper.TRACKS_COLUMN_PURCHASEURL,
            TomahawkSQLiteHelper.TRACKS_COLUMN_SCORE};

    private String[] mAllAlbumsColumns = {TomahawkSQLiteHelper.ALBUMS_COLUMN_ID,
            TomahawkSQLiteHelper.ALBUMS_COLUMN_NAME, TomahawkSQLiteHelper.ALBUMS_COLUMN_ALBUMART,
            TomahawkSQLiteHelper.ALBUMS_COLUMN_FIRSTYEAR,
            TomahawkSQLiteHelper.ALBUMS_COLUMN_LASTYEAR};

    public UserPlaylistsDataSource(Context context, PipeLine pipeLine) {
        mDbHelper = new TomahawkSQLiteHelper(context);
        mPipeLine = pipeLine;
    }

    /**
     * Always try to close the {@link TomahawkSQLiteHelper}, in case it is still open for whatever
     * reason. Then get a reference to our database.
     */
    public void open() throws SQLException {
        mDbHelper.close();
        mDatabase = mDbHelper.getWritableDatabase();
    }

    /**
     * Close the {@link TomahawkSQLiteHelper}
     */
    public void close() {
        mDbHelper.close();
    }

    /**
     * Store the given {@link Playlist} with CACHED_PLAYLIST_ID as its id, and CACHED_PLAYLIST_NAME
     * as its name
     *
     * @param playlist the {@link Playlist} to be stored
     */
    public long storeCachedUserPlaylist(Playlist playlist) {
        return storeUserPlaylist(CACHED_PLAYLIST_ID, CACHED_PLAYLIST_NAME, playlist);
    }

    /**
     * Store the given {@link Playlist} with CACHED_PLAYLIST_ID as its id, and CACHED_PLAYLIST_NAME
     * as its name
     *
     * @param playlistName the name of the playlist to store
     * @param playlist     the {@link Playlist} to be stored
     */
    public long storeUserPlaylist(String playlistName, Playlist playlist) {
        return storeUserPlaylist(-1, playlistName, playlist);
    }

    /**
     * Store the given {@link Playlist}
     *
     * @param insertId     the id under which the given {@link Playlist} should be stored
     * @param playlistName the name with which the given {@link Playlist} should be stored
     * @param playlist     the given {@link Playlist}
     * @return long containing the stored {@link Playlist}'s id
     */
    private long storeUserPlaylist(long insertId, String playlistName, Playlist playlist) {
        ContentValues values = new ContentValues();
        values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_NAME, playlistName);
        values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_CURRENTTRACKINDEX,
                playlist.getCurrentTrackIndex());
        mDatabase.beginTransaction();
        if (insertId >= 0) {
            //insertId is valid. so we use it
            if (mDatabase.update(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, values,
                    TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID + " = " + insertId, null) == 0) {
                values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID, insertId);
                mDatabase.insert(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, null, values);
            }
            Cursor tracksCursor = mDatabase
                    .query(TomahawkSQLiteHelper.TABLE_TRACKS, mAllTracksColumns,
                            TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = " + insertId,
                            null, null, null, null);
            tracksCursor.moveToFirst();
            while (!tracksCursor.isAfterLast()) {
                mDatabase.delete(TomahawkSQLiteHelper.TABLE_ALBUMS,
                        TomahawkSQLiteHelper.ALBUMS_COLUMN_ID + " = " + tracksCursor.getLong(3),
                        null);
                tracksCursor.moveToNext();
            }
            tracksCursor.close();
            mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = " + insertId, null);
        } else {
            //insertId was invalid, so we use the id the database generates for us
            insertId = mDatabase.insert(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, null, values);
        }
        // Store every single Track in the database and store the relationship
        // by storing the playlists's id with it
        for (Track track : playlist.getTracks()) {
            Album album = track.getAlbum();
            long albumInsertId = -1;
            if (album != null) {
                values.clear();
                values.put(TomahawkSQLiteHelper.ALBUMS_COLUMN_NAME, album.getName());
                values.put(TomahawkSQLiteHelper.ALBUMS_COLUMN_ALBUMART, album.getAlbumArtPath());
                values.put(TomahawkSQLiteHelper.ALBUMS_COLUMN_FIRSTYEAR, album.getFirstYear());
                values.put(TomahawkSQLiteHelper.ALBUMS_COLUMN_LASTYEAR, album.getLastYear());
                albumInsertId = mDatabase.insert(TomahawkSQLiteHelper.TABLE_ALBUMS, null, values);
            }
            values.clear();
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS, insertId);
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME, track.getName());
            if (albumInsertId >= 0) {
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_IDALBUMS, albumInsertId);
            }
            if (track.getArtist() != null) {
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME,
                        track.getArtist().getName());
            }
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PATH, track.getPath());
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_BITRATE, track.getBitrate());
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_DURATION, track.getDuration());
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_SIZE, track.getSize());
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNUMBER, track.getTrackNumber());
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_YEAR, track.getYear());
            if (track.getResolver() != null) {
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_RESOLVERID,
                        track.getResolver().getId());
            }
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_LINKURL, track.getLinkUrl());
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PURCHASEURL, track.getPurchaseUrl());
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_SCORE, track.getScore());
            mDatabase.insert(TomahawkSQLiteHelper.TABLE_TRACKS, null, values);
        }
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
        return insertId;
    }

    /**
     * @return the stored {@link org.tomahawk.libtomahawk.collection.UserPlaylist} with
     * CACHED_PLAYLIST_ID as its id
     */
    public UserPlaylist getCachedUserPlaylist() {
        return getUserPlaylist(CACHED_PLAYLIST_ID);
    }

    /**
     * @return every stored {@link org.tomahawk.libtomahawk.collection.UserPlaylist} in the database
     */
    public ArrayList<UserPlaylist> getAllUserPlaylists() {
        ArrayList<UserPlaylist> playListList = new ArrayList<UserPlaylist>();
        Cursor userplaylistsCursor = mDatabase
                .query(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, mAllUserPlaylistsColumns, null,
                        null, null, null, null);
        userplaylistsCursor.moveToFirst();
        while (!userplaylistsCursor.isAfterLast()) {
            UserPlaylist userPlaylist = getUserPlaylist(userplaylistsCursor.getLong(0));
            userPlaylist.setName(userplaylistsCursor.getString(1));
            playListList.add(userPlaylist);
            userplaylistsCursor.moveToNext();
        }
        userplaylistsCursor.close();
        return playListList;
    }

    /**
     * @param playlistId the id by which to get the correct {@link org.tomahawk.libtomahawk.collection.UserPlaylist}
     * @return the stored {@link org.tomahawk.libtomahawk.collection.UserPlaylist} with playlistId
     * as its id
     */
    public UserPlaylist getUserPlaylist(long playlistId) {
        ArrayList<Track> trackList;
        int currentTrackIndex;
        Cursor userplaylistsCursor = mDatabase
                .query(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, mAllUserPlaylistsColumns,
                        TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID + " = " + playlistId, null,
                        null, null, null);
        if (userplaylistsCursor.moveToFirst()) {
            currentTrackIndex = userplaylistsCursor.getInt(2);
            long iduserplaylistcollection = userplaylistsCursor.getLong(0);
            Cursor tracksCursor = mDatabase
                    .query(TomahawkSQLiteHelper.TABLE_TRACKS, mAllTracksColumns,
                            TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = "
                                    + iduserplaylistcollection, null, null, null, null);
            trackList = new ArrayList<Track>();
            tracksCursor.moveToFirst();
            while (!tracksCursor.isAfterLast()) {
                Track track = new Track(tracksCursor.getLong(0));
                track.setName(tracksCursor.getString(2));

                Cursor albumsCursor = mDatabase
                        .query(TomahawkSQLiteHelper.TABLE_ALBUMS, mAllAlbumsColumns,
                                TomahawkSQLiteHelper.ALBUMS_COLUMN_ID + " = " + tracksCursor
                                        .getString(3), null, null, null, null);
                if (albumsCursor.moveToFirst()) {
                    Album album = new Album(TomahawkApp.getUniqueId());
                    album.setName(albumsCursor.getString(1));
                    album.setAlbumArtPath(albumsCursor.getString(2));
                    album.setFirstYear(albumsCursor.getString(3));
                    album.setLastYear(albumsCursor.getString(4));
                    track.setAlbum(album);
                }
                albumsCursor.close();

                Artist artist = new Artist(TomahawkApp.getUniqueId());
                artist.setName(tracksCursor.getString(4));
                track.setArtist(artist);

                track.setPath(tracksCursor.getString(5));
                track.setBitrate(tracksCursor.getInt(6));
                track.setDuration(tracksCursor.getInt(7));
                track.setSize(tracksCursor.getInt(8));
                track.setTrackNumber(tracksCursor.getInt(9));
                track.setYear(tracksCursor.getInt(10));
                track.setResolver(mPipeLine.getResolver(tracksCursor.getInt(11)));
                track.setLinkUrl(tracksCursor.getString(12));
                track.setPurchaseUrl(tracksCursor.getString(13));
                track.setScore(tracksCursor.getFloat(14));
                trackList.add(track);
                tracksCursor.moveToNext();
            }
            UserPlaylist userPlaylist = UserPlaylist.fromTrackListWithId(
                    userplaylistsCursor.getLong(0), userplaylistsCursor.getString(1), trackList,
                    currentTrackIndex);
            tracksCursor.close();
            userplaylistsCursor.close();
            return userPlaylist;
        }
        userplaylistsCursor.close();
        return null;
    }

    /**
     * Delete the {@link org.tomahawk.libtomahawk.collection.UserPlaylist} with the given id
     *
     * @param playlistId long containing the id of the {@link org.tomahawk.libtomahawk.collection.UserPlaylist}
     *                   to be deleted
     */
    public void deleteUserPlaylist(long playlistId) {
        Cursor tracksCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_TRACKS, mAllTracksColumns,
                TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = " + playlistId, null, null,
                null, null);
        tracksCursor.moveToFirst();
        while (!tracksCursor.isAfterLast()) {
            mDatabase.delete(TomahawkSQLiteHelper.TABLE_ALBUMS,
                    TomahawkSQLiteHelper.ALBUMS_COLUMN_ID + " = " + tracksCursor.getLong(3), null);
            tracksCursor.moveToNext();
        }
        tracksCursor.close();
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = " + playlistId, null);
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS,
                TomahawkSQLiteHelper.TRACKS_COLUMN_ID + " = " + playlistId, null);
    }

    /**
     * Delete the {@link Track} with the given trackid in the {@link org.tomahawk.libtomahawk.collection.UserPlaylist}
     * with the given playlistId
     */
    public void deleteTrackInUserPlaylist(long playlistId, long trackId) {
        Cursor tracksCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_TRACKS, mAllTracksColumns,
                TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = " + playlistId + " and " +
                        TomahawkSQLiteHelper.TRACKS_COLUMN_ID + " = " + trackId, null, null, null,
                null);
        if (tracksCursor.moveToFirst()) {
            mDatabase.delete(TomahawkSQLiteHelper.TABLE_ALBUMS,
                    TomahawkSQLiteHelper.ALBUMS_COLUMN_ID + " = " + tracksCursor.getLong(3), null);
        }
        tracksCursor.close();
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = " + playlistId + " and " +
                        TomahawkSQLiteHelper.TRACKS_COLUMN_ID + " = " + trackId, null);
    }

    /**
     * Add the given {@link ArrayList} of {@link Track}s to the {@link
     * org.tomahawk.libtomahawk.collection.UserPlaylist} with the given playlistId
     */
    public void addTracksToUserPlaylist(long playlistId, ArrayList<Track> tracks) {
        ContentValues values = new ContentValues();
        Cursor userplaylistsCursor = mDatabase
                .query(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, mAllUserPlaylistsColumns,
                        TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID + " = " + playlistId, null,
                        null, null, null);
        if (userplaylistsCursor.moveToFirst()) {
            for (Track track : tracks) {
                Album album = track.getAlbum();
                long albumInsertId = -1;
                if (album != null) {
                    values.clear();
                    values.put(TomahawkSQLiteHelper.ALBUMS_COLUMN_NAME, album.getName());
                    values.put(TomahawkSQLiteHelper.ALBUMS_COLUMN_ALBUMART,
                            album.getAlbumArtPath());
                    values.put(TomahawkSQLiteHelper.ALBUMS_COLUMN_FIRSTYEAR, album.getFirstYear());
                    values.put(TomahawkSQLiteHelper.ALBUMS_COLUMN_LASTYEAR, album.getLastYear());
                    albumInsertId = mDatabase
                            .insert(TomahawkSQLiteHelper.TABLE_ALBUMS, null, values);
                }
                values.clear();
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS, playlistId);
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME, track.getName());
                if (albumInsertId >= 0) {
                    values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_IDALBUMS, albumInsertId);
                }
                if (track.getArtist() != null) {
                    values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME,
                            track.getArtist().getName());
                }
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PATH, track.getPath());
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_BITRATE, track.getBitrate());
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_DURATION, track.getDuration());
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_SIZE, track.getSize());
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNUMBER, track.getTrackNumber());
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_YEAR, track.getYear());
                if (track.getResolver() != null) {
                    values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_RESOLVERID,
                            track.getResolver().getId());
                }
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_LINKURL, track.getLinkUrl());
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PURCHASEURL, track.getPurchaseUrl());
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_SCORE, track.getScore());
                mDatabase.insert(TomahawkSQLiteHelper.TABLE_TRACKS, null, values);
            }
        }
        userplaylistsCursor.close();
    }
}
