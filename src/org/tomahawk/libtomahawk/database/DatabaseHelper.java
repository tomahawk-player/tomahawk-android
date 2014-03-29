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
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class provides a way of storing user created {@link org.tomahawk.libtomahawk.collection.UserPlaylist}s
 * in the database
 */
public class DatabaseHelper {

    private static DatabaseHelper instance;

    public static final String USERPLAYLISTSDATASOURCE_RESULTSREPORTED
            = "org.tomahawk.tomahawk_android.userplaylistsdatasource_resultsreported";

    public static final String CACHED_PLAYLIST_NAME = "Last used playlist";

    public static final String CACHED_PLAYLIST_ID = "cached_playlist_id";

    public static final String LOVEDITEMS_PLAYLIST_NAME = "My loved tracks";

    public static final String LOVEDITEMS_PLAYLIST_ID = "loveditems_playlist_id";

    public static final int FALSE = 0;

    public static final int TRUE = 1;

    private Context mContext;

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
            TomahawkSQLiteHelper.TRACKS_COLUMN_ALBUMNAME,
            TomahawkSQLiteHelper.TRACKS_COLUMN_RESULTHINT,
            TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET};

    private String[] mAllSearchHistoryColumns = {TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ID,
            TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ENTRY};

    private String[] mAllInfoSystemOpLogColumns = {TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_ID,
            TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TYPE,
            TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_JSONSTRING,
            TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TIMESTAMP};

    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Long>> mPlaylistPosToIdMap
            = new ConcurrentHashMap<String, ConcurrentHashMap<Integer, Long>>();

    private DatabaseHelper() {
    }

    public static DatabaseHelper getInstance() {
        if (instance == null) {
            synchronized (DatabaseHelper.class) {
                if (instance == null) {
                    instance = new DatabaseHelper();
                }
            }
        }
        return instance;
    }

    public void setContext(Context context) {
        mContext = context;
        mDbHelper = new TomahawkSQLiteHelper(context);
    }

    public boolean isInitialized() {
        return mContext != null;
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
    public void storeUserPlaylist(final UserPlaylist playlist) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    ContentValues values = new ContentValues();
                    values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_NAME, playlist.getName());
                    values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_CURRENTTRACKINDEX,
                            playlist.getCurrentQueryIndex());
                    String insertId = playlist.getId();
                    if (!playlist.isHatchetPlaylist()) {
                        values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ISHATCHETPLAYLIST,
                                FALSE);
                    } else {
                        values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_CURRENTREVISION,
                                playlist.getCurrentRevision());
                        values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ISHATCHETPLAYLIST,
                                TRUE);
                    }
                    values.put(TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID, insertId);
                    mDatabase.beginTransaction();
                    mDatabase.insertWithOnConflict(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, null,
                            values,
                            SQLiteDatabase.CONFLICT_REPLACE);
                    // Delete every already associated Track entry
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                            TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = \"" + insertId
                                    + "\"",
                            null
                    );
                    // Store every single Track in the database and store the relationship
                    // by storing the playlists's id with it
                    for (Query query : playlist.getQueries()) {
                        values.clear();
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS, insertId);
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME,
                                query.getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME,
                                query.getArtist().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ALBUMNAME,
                                query.getAlbum().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_RESULTHINT,
                                query.updateResultHint());
                        if (query.isFetchedViaHatchet()) {
                            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                                    TRUE);
                        } else {
                            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                                    FALSE);
                        }
                        mDatabase.insert(TomahawkSQLiteHelper.TABLE_TRACKS, null, values);
                    }
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast();
                }
            }
        }).start();
    }

    /**
     * @return the stored {@link org.tomahawk.libtomahawk.collection.UserPlaylist} with
     * CACHED_PLAYLIST_ID as its id
     */
    public UserPlaylist getCachedUserPlaylist() {
        return getUserPlaylist(CACHED_PLAYLIST_ID);
    }

    /**
     * @return the stored {@link org.tomahawk.libtomahawk.collection.UserPlaylist} with
     * LOVEDITEMS_PLAYLIST_ID as its id
     */
    public UserPlaylist getLovedItemsUserPlaylist() {
        return getUserPlaylist(LOVEDITEMS_PLAYLIST_ID);
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
    private ArrayList<UserPlaylist> getUserPlaylists(boolean localPlaylists) {
        ArrayList<UserPlaylist> playListList = new ArrayList<UserPlaylist>();
        Cursor userplaylistsCursor = mDatabase
                .query(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, mAllUserPlaylistsColumns,
                        TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ISHATCHETPLAYLIST + " = "
                                + (localPlaylists ? FALSE : TRUE)
                                + " AND " + TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID + " != \""
                                + CACHED_PLAYLIST_ID + "\""
                                + " AND " + TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID + " != \""
                                + LOVEDITEMS_PLAYLIST_ID + "\"", null, null, null, null
                );
        userplaylistsCursor.moveToFirst();
        while (!userplaylistsCursor.isAfterLast()) {
            UserPlaylist userPlaylist = getUserPlaylist(userplaylistsCursor.getString(0));
            playListList.add(userPlaylist);
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
            ConcurrentHashMap<Integer, Long> queryIdMap = new ConcurrentHashMap<Integer, Long>();
            Cursor tracksCursor = mDatabase
                    .query(TomahawkSQLiteHelper.TABLE_TRACKS, mAllTracksColumns,
                            TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = \""
                                    + playlistId + "\"", null, null, null, null
                    );
            queries = new ArrayList<Query>();
            tracksCursor.moveToFirst();
            int positionCounter = 0;
            while (!tracksCursor.isAfterLast()) {
                String trackName = tracksCursor.getString(2);
                String artistName = tracksCursor.getString(3);
                String albumName = tracksCursor.getString(4);
                String resultHint = tracksCursor.getString(5);
                Query query = Query.get(trackName, albumName, artistName, resultHint, false,
                        tracksCursor.getInt(6) == TRUE);
                queryIdMap.put(positionCounter++, tracksCursor.getLong(0));
                queries.add(query);
                tracksCursor.moveToNext();
            }
            mPlaylistPosToIdMap.put(playlistId, queryIdMap);
            Query currentQuery = null;
            if (queries.size() > userplaylistsCursor.getInt(3)) {
                currentQuery = queries.get(userplaylistsCursor.getInt(3));
            }
            UserPlaylist userPlaylist = UserPlaylist
                    .fromQueryList(playlistId, userplaylistsCursor.getString(2),
                            userplaylistsCursor.getString(4),
                            userplaylistsCursor.getInt(1) == TRUE, queries,
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
    public void deleteUserPlaylist(final String playlistId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    mDatabase.beginTransaction();
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                            TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = \""
                                    + playlistId + "\"", null
                    );
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS,
                            TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID + " = \"" + playlistId
                                    + "\"", null
                    );
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast();
                }
            }
        }).start();
    }

    /**
     * Delete the {@link org.tomahawk.libtomahawk.resolver.Query} with the given key in the {@link
     * org.tomahawk.libtomahawk.collection.UserPlaylist} with the given playlistId
     */
    public void deleteQueryInUserPlaylist(final String playlistId, final int position) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    Long id = mPlaylistPosToIdMap.get(playlistId).get(position);
                    mDatabase.beginTransaction();
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                            TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = \""
                                    + playlistId + "\""
                                    + " and " + TomahawkSQLiteHelper.TRACKS_COLUMN_ID + " = " + id,
                            null
                    );
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast();
                }
            }
        }).start();
    }

    /**
     * Add the given {@link ArrayList} of {@link Track}s to the {@link
     * org.tomahawk.libtomahawk.collection.UserPlaylist} with the given playlistId
     */
    public void addQueriesToUserPlaylist(final String playlistId, final ArrayList<Query> queries) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    ContentValues values = new ContentValues();
                    Cursor userplaylistsCursor = mDatabase
                            .query(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS,
                                    mAllUserPlaylistsColumns,
                                    TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID + " = \""
                                            + playlistId
                                            + "\"", null, null, null, null
                            );
                    if (userplaylistsCursor.moveToFirst()) {
                        mDatabase.beginTransaction();
                        // Store every single Track in the database and store the relationship
                        // by storing the playlists's id with it
                        for (Query query : queries) {
                            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS,
                                    playlistId);
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
                    sendReportResultsBroadcast();
                }
            }
        }).start();
    }

    /**
     * Checks if a query with the same album/track/artistName as the given query is included in the
     * lovedItems UserPlaylist
     *
     * @return whether or not the given query is loved
     */
    public boolean isItemLoved(Query query) {
        Cursor userplaylistsCursor = mDatabase
                .query(TomahawkSQLiteHelper.TABLE_USERPLAYLISTS, mAllUserPlaylistsColumns,
                        TomahawkSQLiteHelper.USERPLAYLISTS_COLUMN_ID + " = \""
                                + LOVEDITEMS_PLAYLIST_ID + "\"", null, null, null, null
                );
        if (userplaylistsCursor.moveToFirst()) {
            Cursor tracksCursor = mDatabase
                    .query(TomahawkSQLiteHelper.TABLE_TRACKS, mAllTracksColumns,
                            TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS + " = \""
                                    + LOVEDITEMS_PLAYLIST_ID + "\"", null, null, null, null
                    );
            tracksCursor.moveToFirst();
            while (!tracksCursor.isAfterLast()) {
                String trackName = tracksCursor.getString(2);
                String artistName = tracksCursor.getString(3);
                String albumName = tracksCursor.getString(4);
                if (query.getName().equals(trackName) && query.getArtist().getName()
                        .equals(artistName) && query.getAlbum().getName().equals(albumName)) {
                    tracksCursor.close();
                    userplaylistsCursor.close();
                    return true;
                }
                tracksCursor.moveToNext();
            }
            tracksCursor.close();
            userplaylistsCursor.close();
        }
        return false;
    }

    /**
     * Store the given query as a lovedItem, if isLoved is true. Otherwise remove(unlove) the
     * query.
     */
    public void setLovedItem(Query query, boolean isLoved) {
        if (isLoved) {
            ArrayList<Query> queries = new ArrayList<Query>();
            queries.add(query);
            addQueriesToUserPlaylist(DatabaseHelper.LOVEDITEMS_PLAYLIST_ID, queries);
        } else {
            mDatabase.beginTransaction();
            mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_IDUSERPLAYLISTS
                            + " = \"" + LOVEDITEMS_PLAYLIST_ID + "\""
                            + " AND " + TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME
                            + " = \"" + query.getName() + "\""
                            + " AND " + TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME
                            + " = \"" + query.getArtist().getName() + "\""
                            + " AND " + TomahawkSQLiteHelper.TRACKS_COLUMN_ALBUMNAME
                            + " = \"" + query.getAlbum().getName() + "\"", null
            );
            mDatabase.setTransactionSuccessful();
            mDatabase.endTransaction();
        }
    }

    public Cursor getSearchHistoryCursor(String entry) {
        return mDatabase.query(TomahawkSQLiteHelper.TABLE_SEARCHHISTORY, mAllSearchHistoryColumns,
                TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ENTRY + " LIKE '" + entry + "%'",
                null, null, null, TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ID + " DESC");
    }

    public void addEntryToSearchHistory(String entry) {
        ContentValues values = new ContentValues();
        mDatabase.beginTransaction();
        values.put(TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ENTRY, entry);
        mDatabase.insert(TomahawkSQLiteHelper.TABLE_SEARCHHISTORY, null, values);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * Add an operation to the log. This operation log is being used to store pending operations, so
     * that this operation can be executed, if we have the opportunity to do so.
     *
     * @param opToLog   InfoRequestData object containing the type of the operation, which
     *                  determines where and how to send the data to the API. Contains also the
     *                  JSON-String which contains the data to send.
     * @param timeStamp a timestamp indicating when this operation has been added to the oplog
     */
    public void addOpToInfoSystemOpLog(InfoRequestData opToLog, int timeStamp) {
        ContentValues values = new ContentValues();
        mDatabase.beginTransaction();
        values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TYPE, opToLog.getType());
        values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_JSONSTRING,
                opToLog.getJsonStringToSend());
        values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TIMESTAMP, timeStamp);
        mDatabase.insert(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG, null, values);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * Remove the operation with the given id from the InfoSystem-OpLog table
     *
     * @param opLogId the id of the operation to remove from the InfoSystem-OpLog table
     */
    public void removeOpFromInfoSystemOpLog(int opLogId) {
        mDatabase.beginTransaction();
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG,
                TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_ID + " = " + opLogId, null);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * @return an InfoRequestData object that contains all data that should be delivered to the API
     */
    public List<InfoRequestData> getLoggedOps() {
        List<InfoRequestData> loggedOps = new ArrayList<InfoRequestData>();
        Cursor opLogCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG,
                mAllInfoSystemOpLogColumns, null, null, null, null,
                TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TIMESTAMP + " DESC");
        opLogCursor.moveToFirst();
        while (!opLogCursor.isAfterLast()) {
            String requestId = TomahawkMainActivity.getSessionUniqueStringId();
            InfoRequestData infoRequestData = new InfoRequestData(requestId, opLogCursor.getInt(0),
                    opLogCursor.getInt(1), opLogCursor.getString(2));
            loggedOps.add(infoRequestData);
            opLogCursor.moveToNext();
        }
        opLogCursor.close();
        return loggedOps;
    }

    /**
     * Send a broadcast indicating that userplaylists have been changed in the database and should
     * be refetched
     */
    private void sendReportResultsBroadcast() {
        Intent reportIntent = new Intent(USERPLAYLISTSDATASOURCE_RESULTSREPORTED);
        mContext.sendBroadcast(reportIntent);
    }
}
