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

import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a helper class to declare the different column names inside our database, and to create
 * and call the proper SQL commands onCreate and onUpgrade
 */
public class TomahawkSQLiteHelper extends SQLiteOpenHelper {

    public static final String TAG = TomahawkSQLiteHelper.class.getSimpleName();

    public static final String TABLE_PLAYLISTS = "playlists";

    public static final String PLAYLISTS_COLUMN_ID = "id";

    public static final String PLAYLISTS_COLUMN_NAME = "name";

    public static final String PLAYLISTS_COLUMN_CURRENTTRACKINDEX = "currenttrackindex";

    public static final String PLAYLISTS_COLUMN_CURRENTREVISION = "currentrevision";

    public static final String PLAYLISTS_COLUMN_HATCHETID = "hatchetid";

    public static final String TABLE_TRACKS = "tracks";

    public static final String TRACKS_COLUMN_ID = "id";

    public static final String TRACKS_COLUMN_PLAYLISTID = "playlistid";

    public static final String TRACKS_COLUMN_TRACKNAME = "trackname";

    public static final String TRACKS_COLUMN_ARTISTNAME = "artistname";

    public static final String TRACKS_COLUMN_ALBUMNAME = "albumname";

    public static final String TRACKS_COLUMN_RESULTHINT = "resulthint";

    public static final String TRACKS_COLUMN_ISFETCHEDVIAHATCHET = "isfetchedviahatchet";

    public static final String TRACKS_COLUMN_PLAYLISTENTRYID = "playlistentryid";

    public static final String TRACKS_COLUMN_PLAYLISTENTRYINDEX = "playlistentryindex";

    public static final String TABLE_SEARCHHISTORY = "searchhistory";

    public static final String SEARCHHISTORY_COLUMN_ID = BaseColumns._ID;

    public static final String SEARCHHISTORY_COLUMN_ENTRY = "entry";

    public static final String TABLE_INFOSYSTEMOPLOG = "infosystemoplog";

    public static final String INFOSYSTEMOPLOG_COLUMN_ID = "id";

    public static final String INFOSYSTEMOPLOG_COLUMN_TYPE = "type";

    public static final String INFOSYSTEMOPLOG_COLUMN_HTTPTYPE = "httptype";

    public static final String INFOSYSTEMOPLOG_COLUMN_JSONSTRING = "jsonstring";

    public static final String INFOSYSTEMOPLOG_COLUMN_PARAMS = "params";

    public static final String INFOSYSTEMOPLOG_COLUMN_TIMESTAMP = "timestamp";

    public static final String TABLE_LOVED_ALBUMS = "starred_albums";

    public static final String LOVED_ALBUMS_COLUMN_ID = "id";

    public static final String LOVED_ALBUMS_COLUMN_ARTISTNAME = "artistname";

    public static final String LOVED_ALBUMS_COLUMN_ALBUMNAME = "albumname";

    public static final String TABLE_LOVED_ARTISTS = "starred_artists";

    public static final String LOVED_ARTISTS_COLUMN_ID = "id";

    public static final String LOVED_ARTISTS_COLUMN_ARTISTNAME = "artistname";

    //Legacy
    public static final String TABLE_ALBUMS = "albums";

    private static final String DATABASE_NAME = "userplaylists.db";

    private static final int DATABASE_VERSION = 11;

    // Database creation sql statements
    private static final String CREATE_TABLE_PLAYLISTS =
            "CREATE TABLE `" + TABLE_PLAYLISTS + "` (  `"
                    + PLAYLISTS_COLUMN_ID + "` TEXT PRIMARY KEY ,  `"
                    + PLAYLISTS_COLUMN_NAME + "` TEXT , `"
                    + PLAYLISTS_COLUMN_CURRENTTRACKINDEX + "` INTEGER , `"
                    + PLAYLISTS_COLUMN_CURRENTREVISION + "` TEXT , `"
                    + PLAYLISTS_COLUMN_HATCHETID + "` TEXT );";

    private static final String CREATE_TABLE_TRACKS =
            "CREATE TABLE `" + TABLE_TRACKS + "` (  `"
                    + TRACKS_COLUMN_ID + "` INTEGER PRIMARY KEY AUTOINCREMENT, `"
                    + TRACKS_COLUMN_PLAYLISTID + "` TEXT ,  `"
                    + TRACKS_COLUMN_TRACKNAME + "` TEXT ,`"
                    + TRACKS_COLUMN_ARTISTNAME + "` TEXT ,`"
                    + TRACKS_COLUMN_ALBUMNAME + "` TEXT ,`"
                    + TRACKS_COLUMN_RESULTHINT + "` TEXT ,`"
                    + TRACKS_COLUMN_ISFETCHEDVIAHATCHET + "` INTEGER ,`"
                    + TRACKS_COLUMN_PLAYLISTENTRYID + "` TEXT ,`"
                    + TRACKS_COLUMN_PLAYLISTENTRYINDEX + "` INTEGER ,"
                    + " FOREIGN KEY (`" + TRACKS_COLUMN_PLAYLISTID + "`)"
                    + " REFERENCES `" + TABLE_PLAYLISTS + "` (`" + PLAYLISTS_COLUMN_ID
                    + "`));";

    private static final String CREATE_TABLE_SEARCHHISTORY =
            "CREATE TABLE `" + TABLE_SEARCHHISTORY + "` (  `"
                    + SEARCHHISTORY_COLUMN_ID + "` INTEGER PRIMARY KEY AUTOINCREMENT, `"
                    + SEARCHHISTORY_COLUMN_ENTRY + "` TEXT UNIQUE ON CONFLICT REPLACE);";

    private static final String CREATE_TABLE_INFOSYSTEMOPLOG =
            "CREATE TABLE `" + TABLE_INFOSYSTEMOPLOG + "` (  `"
                    + INFOSYSTEMOPLOG_COLUMN_ID + "` INTEGER PRIMARY KEY AUTOINCREMENT, `"
                    + INFOSYSTEMOPLOG_COLUMN_TYPE + "` INTEGER, `"
                    + INFOSYSTEMOPLOG_COLUMN_HTTPTYPE + "` INTEGER, `"
                    + INFOSYSTEMOPLOG_COLUMN_JSONSTRING + "` TEXT, `"
                    + INFOSYSTEMOPLOG_COLUMN_PARAMS + "` TEXT, `"
                    + INFOSYSTEMOPLOG_COLUMN_TIMESTAMP + "` INTEGER);";

    private static final String CREATE_TABLE_LOVED_ALBUMS =
            "CREATE TABLE `" + TABLE_LOVED_ALBUMS + "` (  `"
                    + LOVED_ALBUMS_COLUMN_ID + "` INTEGER PRIMARY KEY AUTOINCREMENT, `"
                    + LOVED_ALBUMS_COLUMN_ARTISTNAME + "` TEXT ,`"
                    + LOVED_ALBUMS_COLUMN_ALBUMNAME + "` TEXT);";

    private static final String CREATE_TABLE_LOVED_ARTISTS =
            "CREATE TABLE `" + TABLE_LOVED_ARTISTS + "` (  `"
                    + LOVED_ARTISTS_COLUMN_ID + "` INTEGER PRIMARY KEY AUTOINCREMENT, `"
                    + LOVED_ARTISTS_COLUMN_ARTISTNAME + "` TEXT);";

    public TomahawkSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Creates the tables
     */
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_TABLE_PLAYLISTS);
        database.execSQL(CREATE_TABLE_TRACKS);
        database.execSQL(CREATE_TABLE_SEARCHHISTORY);
        database.execSQL(CREATE_TABLE_INFOSYSTEMOPLOG);
        database.execSQL(CREATE_TABLE_LOVED_ALBUMS);
        database.execSQL(CREATE_TABLE_LOVED_ARTISTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                + ", which might destroy all old data");
        if (oldVersion == 8 || oldVersion == 9 || oldVersion == 10) {
            if (oldVersion == 8) {
                db.execSQL(CREATE_TABLE_LOVED_ALBUMS);
                db.execSQL(CREATE_TABLE_LOVED_ARTISTS);
            }
            if (oldVersion == 9) {
                db.execSQL("DROP TABLE IF EXISTS `" + TABLE_PLAYLISTS + "`;");
                db.execSQL("DROP TABLE IF EXISTS `" + TABLE_TRACKS + "`;");
                db.execSQL(CREATE_TABLE_PLAYLISTS);
                db.execSQL(CREATE_TABLE_TRACKS);
                // get all logged ops and their timestamps, so we can safely drop the table
                List<InfoRequestData> loggedOps = new ArrayList<InfoRequestData>();
                List<Integer> timeStamps = new ArrayList<Integer>();
                String[] columns = new String[]{TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_ID,
                        TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TYPE,
                        TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_JSONSTRING,
                        TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TIMESTAMP};
                Cursor opLogCursor = db.query(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG,
                        columns, null, null, null, null, null);
                opLogCursor.moveToFirst();
                while (!opLogCursor.isAfterLast()) {
                    String requestId = TomahawkMainActivity.getSessionUniqueStringId();
                    InfoRequestData infoRequestData = new InfoRequestData(requestId,
                            opLogCursor.getInt(1), null, opLogCursor.getInt(0),
                            InfoRequestData.HTTPTYPE_POST, opLogCursor.getString(2));
                    loggedOps.add(infoRequestData);
                    timeStamps.add(opLogCursor.getInt(3));
                    opLogCursor.moveToNext();
                }
                opLogCursor.close();
                db.execSQL("DROP TABLE IF EXISTS `" + TABLE_INFOSYSTEMOPLOG + "`;");
                db.execSQL(CREATE_TABLE_INFOSYSTEMOPLOG);
                // now repopulate the table with the old data
                ContentValues values = new ContentValues();
                db.beginTransaction();
                for (int i = 0; i < loggedOps.size(); i++) {
                    InfoRequestData loggedOp = loggedOps.get(i);
                    values.clear();
                    values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TYPE,
                            loggedOp.getType());
                    values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_HTTPTYPE,
                            loggedOp.getHttpType());
                    values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_JSONSTRING,
                            loggedOp.getJsonStringToSend());
                    values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TIMESTAMP,
                            timeStamps.get(i));
                    db.insert(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG, null, values);
                }
                db.setTransactionSuccessful();
                db.endTransaction();
            }
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_PLAYLISTS + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_TRACKS + "`;");
            db.execSQL(CREATE_TABLE_PLAYLISTS);
            db.execSQL(CREATE_TABLE_TRACKS);
        } else {
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_TRACKS + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_ALBUMS + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_PLAYLISTS + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_SEARCHHISTORY + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_INFOSYSTEMOPLOG + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_LOVED_ALBUMS + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_LOVED_ARTISTS + "`;");
            onCreate(db);
        }
    }

}
