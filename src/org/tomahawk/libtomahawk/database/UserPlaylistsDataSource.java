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

import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
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

    private String[] mAllUserPlaylistsColumns = {TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID,
            TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_NAME,
            TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_CURRENTTRACKINDEX};

    private String[] mAllTracksColumns = {TomahawkSQLiteHelper.TRACKS_COLUMN_KEY,
            TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS,
            TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME,
            TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME,
            TomahawkSQLiteHelper.TRACKS_COLUMN_ALBUMNAME,};

    public UserPlaylistsDataSource(Context context) {
        mDbHelper = new TomahawkSQLiteHelper(context);
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
                playlist.getCurrentQueryIndex());
        mDatabase.beginTransaction();
        if (insertId >= 0) {
            //insertId is valid. so we use it
            if (mDatabase.update(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, values,
                    TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID + " = " + insertId, null) == 0) {
                values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID, insertId);
                mDatabase.insert(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, null, values);
            }
            mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = " + insertId, null);
        } else {
            //insertId was invalid, so we use the id the database generates for us
            insertId = mDatabase.insert(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, null, values);
        }
        // Store every single Track in the database and store the relationship
        // by storing the playlists's id with it
        for (Query query : playlist.getQueries()) {
            values.clear();
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS, insertId);
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME,
                    query.getPreferredTrack().getName());
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME,
                    query.getPreferredTrack().getArtist().getName());
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ALBUMNAME,
                    query.getPreferredTrack().getAlbum().getName());
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
        ArrayList<Query> queries;
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
            queries = new ArrayList<Query>();
            tracksCursor.moveToFirst();
            while (!tracksCursor.isAfterLast()) {
                String trackName = tracksCursor.getString(2);
                String artistName = tracksCursor.getString(3);
                String albumName = tracksCursor.getString(4);
                queries.add(new Query(trackName, albumName, artistName, false));
                tracksCursor.moveToNext();
            }
            long userPlaylistsId = userplaylistsCursor.getLong(0);
            if (userplaylistsCursor.getLong(0) != CACHED_PLAYLIST_ID) {
                userPlaylistsId = TomahawkApp.getUniqueId();
            }
            UserPlaylist userPlaylist = UserPlaylist.fromQueryList(
                    userPlaylistsId, userplaylistsCursor.getString(1), queries,
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
        mDatabase.beginTransaction();
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = " + playlistId, null);
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS,
                TomahawkSQLiteHelper.TRACKS_COLUMN_KEY + " = " + playlistId, null);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * Delete the {@link org.tomahawk.libtomahawk.resolver.Query} with the given key in the {@link
     * org.tomahawk.libtomahawk.collection.UserPlaylist} with the given playlistId
     */
    public void deleteQueryInUserPlaylist(long playlistId, String key) {
        mDatabase.beginTransaction();
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = " + playlistId + " and " +
                        TomahawkSQLiteHelper.TRACKS_COLUMN_KEY + " = " + key, null);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * Add the given {@link ArrayList} of {@link Track}s to the {@link
     * org.tomahawk.libtomahawk.collection.UserPlaylist} with the given playlistId
     */
    public void addQueriesToUserPlaylist(long playlistId, ArrayList<Query> queries) {
        ContentValues values = new ContentValues();
        Cursor userplaylistsCursor = mDatabase
                .query(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, mAllUserPlaylistsColumns,
                        TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID + " = " + playlistId, null,
                        null, null, null);
        if (userplaylistsCursor.moveToFirst()) {
            mDatabase.beginTransaction();
            // Store every single Track in the database and store the relationship
            // by storing the playlists's id with it
            for (Query query : queries) {
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_KEY,
                        TomahawkUtils.getCacheKey(query));
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS, playlistId);
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME,
                        query.getPreferredTrack().getName());
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME,
                        query.getPreferredTrack().getArtist().getName());
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ALBUMNAME,
                        query.getPreferredTrack().getAlbum().getName());
                mDatabase.insert(TomahawkSQLiteHelper.TABLE_TRACKS, null, values);
            }
            mDatabase.setTransactionSuccessful();
            mDatabase.endTransaction();
        }
        userplaylistsCursor.close();
    }
}
