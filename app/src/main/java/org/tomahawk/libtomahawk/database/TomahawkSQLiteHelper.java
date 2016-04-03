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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * This is a helper class to declare the different column names inside our database, and to create
 * and call the proper SQL commands onCreate and onUpgrade
 */
public class TomahawkSQLiteHelper extends SQLiteOpenHelper {

    public static final String TAG = TomahawkSQLiteHelper.class.getSimpleName();

    public static final String TABLE_STATIONS = "stations";

    public static final String STATIONS_COLUMN_ID = "id";

    public static final String STATIONS_COLUMN_JSON = "json";

    public static final String STATIONS_COLUMN_CREATEDTIMESTAMP = "createdtimestamp";

    public static final String STATIONS_COLUMN_PLAYEDTIMESTAMP = "playedtimestamp";

    public static final String TABLE_PLAYLISTS = "playlists";

    public static final String PLAYLISTS_COLUMN_ID = "id";

    public static final String PLAYLISTS_COLUMN_NAME = "name";

    public static final String PLAYLISTS_COLUMN_CURRENTTRACKINDEX = "currenttrackindex";

    public static final String PLAYLISTS_COLUMN_CURRENTREVISION = "currentrevision";

    public static final String PLAYLISTS_COLUMN_HATCHETID = "hatchetid";

    public static final String PLAYLISTS_COLUMN_TOPARTISTS = "topartists";

    public static final String PLAYLISTS_COLUMN_TRACKCOUNT = "trackcount";

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

    public static final String TABLE_INFOSYSTEMOPLOGINFO = "infosystemoploginfo";

    public static final String INFOSYSTEMOPLOGINFO_COLUMN_LOGCOUNT = "logcount";

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

    //media data
    public static final String TABLE_MEDIA = "media";

    public static final String MEDIA_LOCATION = "location";

    public static final String MEDIA_TIME = "time";

    public static final String MEDIA_LENGTH = "length";

    public static final String MEDIA_TYPE = "type";

    public static final String MEDIA_PICTURE = "picture";

    public static final String MEDIA_TITLE = "title";

    public static final String MEDIA_ARTIST = "artist";

    public static final String MEDIA_GENRE = "genre";

    public static final String MEDIA_ALBUM = "album";

    public static final String MEDIA_ALBUMARTIST = "albumartist";

    public static final String MEDIA_WIDTH = "width";

    public static final String MEDIA_HEIGHT = "height";

    public static final String MEDIA_ARTWORKURL = "artwork_url";

    public static final String MEDIA_AUDIOTRACK = "audio_track";

    public static final String MEDIA_SPUTRACK = "spu_track";

    public static final String MEDIA_LASTMODIFIED = "last_modified";

    public static final String MEDIA_DISCNUMBER = "disc_number";

    public static final String MEDIA_TRACKNUMBER = "track_number";

    public enum mediaColumn {
        MEDIA_TABLE_NAME, MEDIA_PATH, MEDIA_TIME, MEDIA_LENGTH,
        MEDIA_TYPE, MEDIA_PICTURE, MEDIA_TITLE, MEDIA_ARTIST, MEDIA_GENRE, MEDIA_ALBUM,
        MEDIA_ALBUMARTIST, MEDIA_WIDTH, MEDIA_HEIGHT, MEDIA_ARTWORKURL, MEDIA_AUDIOTRACK,
        MEDIA_SPUTRACK, MEDIA_TRACKNUMBER, MEDIA_DISCNUMBER, MEDIA_LAST_MODIFIED
    }

    public static final String TABLE_MEDIADIRS = "mediadirs";

    public static final String MEDIADIRS_PATH = "path";

    public static final String MEDIADIRS_BLACKLISTED = "blacklisted";


    public static final String TABLE_ALBUMS = "albums"; //Legacy

    private static final String DATABASE_NAME = "userplaylists.db";

    private static final int DATABASE_VERSION = 20;

    // Database creation sql statements
    private static final String CREATE_TABLE_PLAYLISTS =
            "CREATE TABLE `" + TABLE_PLAYLISTS + "` (  `"
                    + PLAYLISTS_COLUMN_ID + "` TEXT PRIMARY KEY ,  `"
                    + PLAYLISTS_COLUMN_NAME + "` TEXT , `"
                    + PLAYLISTS_COLUMN_CURRENTTRACKINDEX + "` INTEGER , `"
                    + PLAYLISTS_COLUMN_CURRENTREVISION + "` TEXT , `"
                    + PLAYLISTS_COLUMN_HATCHETID + "` TEXT , `"
                    + PLAYLISTS_COLUMN_TOPARTISTS + "` TEXT , `"
                    + PLAYLISTS_COLUMN_TRACKCOUNT + "` INTEGER );";

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

    private static final String CREATE_TABLE_INFOSYSTEMOPLOGINFO =
            "CREATE TABLE `" + TABLE_INFOSYSTEMOPLOGINFO + "` (  `"
                    + INFOSYSTEMOPLOGINFO_COLUMN_LOGCOUNT + "` INTEGER);";

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

    private static final String CREATE_TABLE_MEDIA = "CREATE TABLE IF NOT EXISTS "
            + TABLE_MEDIA + " ("
            + MEDIA_LOCATION + " TEXT PRIMARY KEY NOT NULL, "
            + MEDIA_TIME + " INTEGER, "
            + MEDIA_LENGTH + " INTEGER, "
            + MEDIA_TYPE + " INTEGER, "
            + MEDIA_PICTURE + " BLOB, "
            + MEDIA_TITLE + " TEXT, "
            + MEDIA_ARTIST + " TEXT, "
            + MEDIA_GENRE + " TEXT, "
            + MEDIA_ALBUM + " TEXT, "
            + MEDIA_ALBUMARTIST + " TEXT, "
            + MEDIA_WIDTH + " INTEGER, "
            + MEDIA_HEIGHT + " INTEGER, "
            + MEDIA_ARTWORKURL + " TEXT, "
            + MEDIA_AUDIOTRACK + " INTEGER, "
            + MEDIA_SPUTRACK + " INTEGER, "
            + MEDIA_TRACKNUMBER + " INTEGER, "
            + MEDIA_DISCNUMBER + " INTEGER, "
            + MEDIA_LASTMODIFIED + " INTEGER"
            + ");";

    private static final String CREATE_TABLE_MEDIADIRS = "CREATE TABLE "
            + TABLE_MEDIADIRS + " ("
            + MEDIADIRS_PATH + " TEXT PRIMARY KEY NOT NULL, "
            + MEDIADIRS_BLACKLISTED + " INTEGER "
            + ");";

    private static final String CREATE_TABLE_STATIONS =
            "CREATE TABLE `" + TABLE_STATIONS + "` (  `"
                    + STATIONS_COLUMN_ID + "` TEXT PRIMARY KEY, `"
                    + STATIONS_COLUMN_JSON + "` TEXT, `"
                    + STATIONS_COLUMN_CREATEDTIMESTAMP + "` INTEGER, `"
                    + STATIONS_COLUMN_PLAYEDTIMESTAMP + "` TEXT );";

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
        database.execSQL(CREATE_TABLE_INFOSYSTEMOPLOGINFO);
        database.execSQL(CREATE_TABLE_INFOSYSTEMOPLOG);
        database.execSQL(CREATE_TABLE_LOVED_ALBUMS);
        database.execSQL(CREATE_TABLE_LOVED_ARTISTS);
        database.execSQL(CREATE_TABLE_MEDIA);
        database.execSQL(CREATE_TABLE_MEDIADIRS);
        database.execSQL(CREATE_TABLE_STATIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                + ", which might destroy all old data");
        if (oldVersion < 11) {
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_TRACKS + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_ALBUMS + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_PLAYLISTS + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_SEARCHHISTORY + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_INFOSYSTEMOPLOGINFO + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_INFOSYSTEMOPLOG + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_LOVED_ALBUMS + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_LOVED_ARTISTS + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_MEDIA + "`;");
            db.execSQL("DROP TABLE IF EXISTS `" + TABLE_MEDIADIRS + "`;");
            onCreate(db);
        } else {
            if (oldVersion < 13) {
                db.execSQL("DROP TABLE IF EXISTS `" + CREATE_TABLE_MEDIADIRS + "`;");
                db.execSQL(CREATE_TABLE_MEDIADIRS);
            }
            if (oldVersion < 16) {
                db.execSQL("ALTER TABLE `" + TABLE_PLAYLISTS + "` ADD COLUMN `"
                        + PLAYLISTS_COLUMN_TOPARTISTS + "` TEXT");
            }
            if (oldVersion < 17) {
                db.execSQL("ALTER TABLE `" + TABLE_PLAYLISTS + "` ADD COLUMN `"
                        + PLAYLISTS_COLUMN_TRACKCOUNT + "` INTEGER");
                db.execSQL("DROP TABLE IF EXISTS `" + TABLE_INFOSYSTEMOPLOGINFO + "`;");
                db.execSQL(CREATE_TABLE_INFOSYSTEMOPLOGINFO);
            }
            if (oldVersion < 19) {
                db.execSQL("DROP TABLE IF EXISTS `" + TABLE_MEDIA + "`;");
                db.execSQL(CREATE_TABLE_MEDIA);
            }
            if (oldVersion < 20) {
                db.execSQL(CREATE_TABLE_STATIONS);
            }
        }
    }

}
