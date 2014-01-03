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
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class provides a way of storing user created {@link org.tomahawk.libtomahawk.collection.UserPlaylist}s
 * in the database
 */
public class UserPlaylistsDataSource {

    public static final String CACHED_PLAYLIST_NAME = "Last used playlist";

    public static final String CACHED_PLAYLIST_ID = "cached_playlist_id";

    public static final int ISHATCHETPLAYLIST_FALSE = 0;

    public static final int ISHATCHETPLAYLIST_TRUE = 1;

    // Database fields
    private SQLiteDatabase mDatabase;

    private TomahawkSQLiteHelper mDbHelper;

    private String[] mAllUserPlaylistsColumns = {TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID,
            TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ISHATCHETPLAYLIST,
            TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_NAME,
            TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_CURRENTTRACKINDEX,
            TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_CURRENTREVISION};

    private String[] mAllTracksColumns = {TomahawkSQLiteHelper.TRACKS_COLUMN_ID,
            TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS,
            TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME,
            TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME,
            TomahawkSQLiteHelper.TRACKS_COLUMN_ALBUMNAME,};

    private ConcurrentHashMap<String, ConcurrentHashMap<Query, Long>> mPlaylistQueryIdMap
            = new ConcurrentHashMap<String, ConcurrentHashMap<Query, Long>>();

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
     * Store the given {@link Playlist}
     *
     * @param playlist the given {@link Playlist}
     * @return String containing the stored {@link Playlist}'s id
     */
    public String storeUserPlaylist(UserPlaylist playlist) {
        ContentValues values = new ContentValues();
        values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_NAME, playlist.getName());
        values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_CURRENTTRACKINDEX,
                playlist.getCurrentQueryIndex());
        String insertId = playlist.getId();
        if (!playlist.isHatchetPlaylist()) {
            values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ISHATCHETPLAYLIST,
                    ISHATCHETPLAYLIST_FALSE);
        } else {
            values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_CURRENTREVISION,
                    playlist.getCurrentRevision());
            values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ISHATCHETPLAYLIST,
                    ISHATCHETPLAYLIST_TRUE);
        }
        values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID, insertId);
        mDatabase.beginTransaction();
        mDatabase.insertWithOnConflict(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
        // Delete every already associated Track entry
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = \"" + insertId + "\"",
                null);
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

    public ArrayList<UserPlaylist> getLocalUserPlaylists() {
        return getUserPlaylists(true);
    }

    public ArrayList<UserPlaylist> getHatchetUserPlaylists() {
        return getUserPlaylists(false);
    }

    /**
     * @return every stored {@link org.tomahawk.libtomahawk.collection.UserPlaylist} in the database
     */
    private ArrayList<UserPlaylist> getUserPlaylists(boolean onlyLocalPlaylists) {
        ArrayList<UserPlaylist> playListList = new ArrayList<UserPlaylist>();
        Cursor userplaylistsCursor = mDatabase
                .query(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, mAllUserPlaylistsColumns,
                        TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ISHATCHETPLAYLIST + " = "
                                + (onlyLocalPlaylists ? ISHATCHETPLAYLIST_FALSE
                                : ISHATCHETPLAYLIST_TRUE),
                        null, null, null, null);
        userplaylistsCursor.moveToFirst();
        while (!userplaylistsCursor.isAfterLast()) {
            if (!userplaylistsCursor.getString(0).equals(CACHED_PLAYLIST_ID)) {
                UserPlaylist userPlaylist = getUserPlaylist(userplaylistsCursor.getString(0));
                playListList.add(userPlaylist);
            }
            userplaylistsCursor.moveToNext();
        }
        userplaylistsCursor.close();
        return playListList;
    }

    /**
     * @param playlistId the id by which to get the correct {@link org.tomahawk.libtomahawk.collection.Playlist}
     * @return the stored {@link org.tomahawk.libtomahawk.collection.Playlist} with playlistId as
     * its id
     */
    public UserPlaylist getUserPlaylist(String playlistId) {
        ArrayList<Query> queries;
        Cursor userplaylistsCursor = mDatabase
                .query(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, mAllUserPlaylistsColumns,
                        TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID + " = \"" + playlistId + "\"",
                        null, null, null, null);
        if (userplaylistsCursor.moveToFirst()) {
            ConcurrentHashMap<Query, Long> queryIdMap = new ConcurrentHashMap<Query, Long>();
            Cursor tracksCursor = mDatabase
                    .query(TomahawkSQLiteHelper.TABLE_TRACKS, mAllTracksColumns,
                            TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = \""
                                    + playlistId + "\"", null, null, null, null);
            queries = new ArrayList<Query>();
            tracksCursor.moveToFirst();
            while (!tracksCursor.isAfterLast()) {
                String trackName = tracksCursor.getString(2);
                String artistName = tracksCursor.getString(3);
                String albumName = tracksCursor.getString(4);
                Query query = new Query(trackName, albumName, artistName, false);
                queryIdMap.put(query, tracksCursor.getLong(0));
                queries.add(query);
                tracksCursor.moveToNext();
            }
            mPlaylistQueryIdMap.put(playlistId, queryIdMap);
            Query currentQuery = null;
            if (queries.size() > userplaylistsCursor.getInt(3)) {
                currentQuery = queries.get(userplaylistsCursor.getInt(3));
            }
            UserPlaylist userPlaylist = UserPlaylist
                    .fromQueryList(playlistId, userplaylistsCursor.getString(2),
                            userplaylistsCursor.getString(4),
                            userplaylistsCursor.getInt(1) == ISHATCHETPLAYLIST_TRUE, queries,
                            currentQuery);
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
     * @param playlistId String containing the id of the {@link org.tomahawk.libtomahawk.collection.UserPlaylist}
     *                   to be deleted
     */
    public void deleteUserPlaylist(String playlistId) {
        mDatabase.beginTransaction();
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = \"" + playlistId + "\"",
                null);
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS,
                TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID + " = \"" + playlistId + "\"", null);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * Delete the {@link org.tomahawk.libtomahawk.resolver.Query} with the given key in the {@link
     * org.tomahawk.libtomahawk.collection.UserPlaylist} with the given playlistId
     */
    public void deleteQueryInUserPlaylist(String playlistId, Query query) {
        Long id = mPlaylistQueryIdMap.get(playlistId).get(query);
        mDatabase.beginTransaction();
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = \"" + playlistId + "\""
                        + " and " + TomahawkSQLiteHelper.TRACKS_COLUMN_ID + " = " + id, null);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * Add the given {@link ArrayList} of {@link Track}s to the {@link
     * org.tomahawk.libtomahawk.collection.UserPlaylist} with the given playlistId
     */
    public void addQueriesToUserPlaylist(String playlistId, ArrayList<Query> queries) {
        ContentValues values = new ContentValues();
        Cursor userplaylistsCursor = mDatabase
                .query(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, mAllUserPlaylistsColumns,
                        TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID + " = \"" + playlistId + "\"",
                        null, null, null, null);
        if (userplaylistsCursor.moveToFirst()) {
            mDatabase.beginTransaction();
            // Store every single Track in the database and store the relationship
            // by storing the playlists's id with it
            for (Query query : queries) {
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
