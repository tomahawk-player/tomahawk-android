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

import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.DbCollection;
import org.tomahawk.libtomahawk.resolver.FuzzyIndex;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverTrack;
import org.tomahawk.libtomahawk.utils.StringUtils;
import org.tomahawk.tomahawk_android.utils.PreferenceUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionDb extends SQLiteOpenHelper {

    public static final String TAG = CollectionDb.class.getSimpleName();

    public static final String ID = "_id";

    public static final String TABLE_ARTISTS = "artists";

    public static final String ARTISTS_ARTIST = "artist";

    public static final String ARTISTS_ARTISTDISAMBIGUATION = "artistDisambiguation";

    public static final String ARTISTS_LASTMODIFIED = "artistLastModified";

    public static final String ARTISTS_TYPE = "artistType";

    public static final String TABLE_ALBUMARTISTS = "albumArtists";

    public static final String ALBUMARTISTS_ALBUMARTIST = "albumArtist";

    public static final String ALBUMARTISTS_ALBUMARTISTDISAMBIGUATION = "albumArtistDisambiguation";

    public static final String ALBUMARTISTS_LASTMODIFIED = "albumArtistLastModified";

    public static final String TABLE_ALBUMS = "albums";

    public static final String ALBUMS_ALBUM = "album";

    public static final String ALBUMS_IMAGEPATH = "imagePath";

    public static final String ALBUMS_ALBUMARTISTID = "albumArtistId";

    public static final String ALBUMS_LASTMODIFIED = "albumLastModified";

    public static final String ALBUMS_TYPE = "albumType";

    public static final String TABLE_ARTISTALBUMS = "artistAlbums";

    public static final String ARTISTALBUMS_ALBUMID = "albumId";

    public static final String ARTISTALBUMS_ARTISTID = "artistId";

    public static final String TABLE_TRACKS = "tracks";

    public static final String TRACKS_TRACK = "track";

    public static final String TRACKS_ARTISTID = "artistId";

    public static final String TRACKS_ALBUMID = "albumId";

    public static final String TRACKS_URL = "url";

    public static final String TRACKS_DURATION = "duration";

    public static final String TRACKS_ALBUMPOS = "albumPos";

    public static final String TRACKS_LINKURL = "linkUrl";

    public static final String TRACKS_LASTMODIFIED = "trackLastModified";

    public static final String TABLE_REVISIONHISTORY = "revisionHistory";

    public static final String REVISIONHISTORY_ACTION = "action";

    public static final String REVISIONHISTORY_TRACKCOUNT = "trackCount";

    public static final String REVISIONHISTORY_REVISION = "revision";

    public static final String REVISIONHISTORY_TIMESTAMP = "timeStamp";

    protected static final int ACTION_WIPE = 0;

    protected static final int ACTION_ADDTRACKS = 1;

    protected static final int TYPE_DEFAULT = 0;

    // This type marks an entry that has been explicitly loved.
    // E.g. an artist that has been loved by the user.
    protected static final int TYPE_HATCHET_EXPLICIT = 1;

    // This type marks an entry that has not been explicitly loved by the user but has been added to
    // the collection because it was necessary in order to love another entry. E.g. an artist that
    // was added implicitly because an album has been loved by the user.
    protected static final int TYPE_HATCHET_IMPLICIT = 2;

    private static final String CREATE_TABLE_ARTISTS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_ARTISTS + " ("
            + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + ARTISTS_ARTIST + " TEXT,"
            + ARTISTS_ARTISTDISAMBIGUATION + " TEXT,"
            + ARTISTS_LASTMODIFIED + " INTEGER,"
            + ARTISTS_TYPE + " INTEGER,"
            + "UNIQUE (" + ARTISTS_ARTIST + ", " + ARTISTS_ARTISTDISAMBIGUATION + ", "
            + ARTISTS_TYPE
            + ") ON CONFLICT IGNORE);";

    private static final String CREATE_TABLE_ALBUMARTISTS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_ALBUMARTISTS + " ("
            + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + ALBUMARTISTS_ALBUMARTIST + " TEXT,"
            + ALBUMARTISTS_ALBUMARTISTDISAMBIGUATION + " TEXT,"
            + ALBUMARTISTS_LASTMODIFIED + " INTEGER,"
            + "UNIQUE (" + ALBUMARTISTS_ALBUMARTIST + ", " + ALBUMARTISTS_ALBUMARTISTDISAMBIGUATION
            + ") ON CONFLICT IGNORE);";

    private static final String CREATE_TABLE_ALBUMS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_ALBUMS + " ("
            + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + ALBUMS_ALBUM + " TEXT,"
            + ALBUMS_ALBUMARTISTID + " INTEGER,"
            + ALBUMS_IMAGEPATH + " TEXT,"
            + ALBUMS_LASTMODIFIED + " INTEGER,"
            + ALBUMS_TYPE + " INTEGER,"
            + "UNIQUE (" + ALBUMS_ALBUM + ", " + ALBUMS_ALBUMARTISTID + ", " + ALBUMS_TYPE
            + ") ON CONFLICT IGNORE,"
            + "FOREIGN KEY(" + ALBUMS_ALBUMARTISTID + ") REFERENCES "
            + TABLE_ALBUMARTISTS + "(" + ID + "));";

    private static final String CREATE_TABLE_ARTISTALBUMS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_ARTISTALBUMS + " ("
            + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + ARTISTALBUMS_ALBUMID + " INTEGER,"
            + ARTISTALBUMS_ARTISTID + " INTEGER,"
            + "UNIQUE (" + ARTISTALBUMS_ALBUMID + ", " + ARTISTALBUMS_ARTISTID
            + ") ON CONFLICT IGNORE,"
            + "FOREIGN KEY(" + ARTISTALBUMS_ALBUMID + ") REFERENCES "
            + TABLE_ALBUMS + "(" + ID + "),"
            + "FOREIGN KEY(" + ARTISTALBUMS_ARTISTID + ") REFERENCES "
            + TABLE_ARTISTS + "(" + ID + "));";

    private static final String CREATE_TABLE_TRACKS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_TRACKS + " ("
            + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + TRACKS_TRACK + " TEXT,"
            + TRACKS_ARTISTID + " INTEGER,"
            + TRACKS_ALBUMID + " INTEGER,"
            + TRACKS_URL + " TEXT,"
            + TRACKS_DURATION + " INTEGER,"
            + TRACKS_ALBUMPOS + " INTEGER,"
            + TRACKS_LINKURL + " TEXT,"
            + TRACKS_LASTMODIFIED + " INTEGER,"
            + "UNIQUE (" + TRACKS_TRACK + ", " + TRACKS_ARTISTID + ", " + TRACKS_ALBUMID
            + ") ON CONFLICT IGNORE,"
            + "FOREIGN KEY(" + TRACKS_ARTISTID + ") REFERENCES "
            + TABLE_ARTISTS + "(" + ID + "),"
            + "FOREIGN KEY(" + TRACKS_ALBUMID + ") REFERENCES "
            + TABLE_ALBUMS + "(" + ID + "));";

    private static final String CREATE_TABLE_REVISIONHISTORY = "CREATE TABLE IF NOT EXISTS "
            + TABLE_REVISIONHISTORY + " ("
            + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + REVISIONHISTORY_ACTION + " INTEGER,"
            + REVISIONHISTORY_TRACKCOUNT + " INTEGER,"
            + REVISIONHISTORY_REVISION + " TEXT,"
            + REVISIONHISTORY_TIMESTAMP + " INTEGER );";

    private static final int DB_VERSION = 5;

    private static final String DB_FILE_SUFFIX = "_collection.db";

    protected final SQLiteDatabase mDb;

    private static final String LAST_COLLECTION_DB_UPDATE_SUFFIX = "_last_collection_db_update";

    public static class WhereInfo {

        public String connection;

        public Map<String, String[]> where = new HashMap<>();

        public boolean equals = true;

    }

    private static class JoinInfo {

        String table;

        Map<String, String> conditions = new HashMap<>();

    }

    private String mCollectionId;

    private FuzzyIndex mFuzzyIndex;

    public CollectionDb(Context context, String collectionId) {
        super(context, collectionId + DB_FILE_SUFFIX, null, DB_VERSION);

        Log.d(TAG, "Constructed CollectionDb '" + collectionId + DB_FILE_SUFFIX + "' with version "
                + DB_VERSION + ", objectId: " + this.hashCode());

        mCollectionId = collectionId;

        close();
        mDb = getWritableDatabase();

        mFuzzyIndex = new FuzzyIndex(this);
    }

    public String getCollectionId() {
        return mCollectionId;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate - CollectionDb '" + db.getPath() + "' with version "
                + db.getVersion() + ", objectId: " + this.hashCode());
        db.execSQL(CREATE_TABLE_ARTISTS);
        db.execSQL(CREATE_TABLE_ALBUMARTISTS);
        db.execSQL(CREATE_TABLE_ALBUMS);
        db.execSQL(CREATE_TABLE_ARTISTALBUMS);
        db.execSQL(CREATE_TABLE_TRACKS);
        db.execSQL(CREATE_TABLE_REVISIONHISTORY);
        Log.d(TAG, "onCreate finished - CollectionDb '" + db.getPath() + "' with version "
                + db.getVersion() + ", objectId: " + this.hashCode());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                + ", which might destroy all old data");
        if (oldVersion < 4) {
            wipe(db);
        }
        if (oldVersion < 5) {
            db.execSQL(CREATE_TABLE_REVISIONHISTORY);
            long lastDbUpdate =
                    PreferenceUtils.getLong(mCollectionId + LAST_COLLECTION_DB_UPDATE_SUFFIX);
            if (lastDbUpdate > 0) {
                storeNewRevision(db, String.valueOf(lastDbUpdate), ACTION_ADDTRACKS);
            }
        }
    }

    public synchronized void addTracks(List<ScriptResolverTrack> tracks) {
        long time = System.currentTimeMillis();

        // Check if we want to store the album as a compilation album (with artist "Various Artists")
        Map<String, Set<String>> albumArtists = new HashMap<>();
        for (ScriptResolverTrack track : tracks) {
            if (track.artist == null) {
                track.artist = "";
            }
            if (track.artistDisambiguation == null) {
                track.artistDisambiguation = "";
            }
            if (track.album == null) {
                track.album = "";
            }
            if (track.albumArtist == null) {
                track.albumArtist = "";
            }
            if (track.albumArtistDisambiguation == null) {
                track.albumArtistDisambiguation = "";
            }
            if (track.track == null) {
                track.track = "";
            }
            Set<String> artists = albumArtists.get(track.album + "♠" + track.albumArtist);
            if (artists == null) {
                artists = new HashSet<>();
                albumArtists.put(track.album + "♠" + track.albumArtist, artists);
            }
            if (artists.size() < 2) {
                artists.add(track.artist);
            }
        }

        Map<String, Long> artistLastModifiedMap = new HashMap<>();
        // First we insert all artists and albumArtists
        mDb.beginTransaction();
        for (ScriptResolverTrack track : tracks) {
            if (albumArtists.get(track.album + "♠" + track.albumArtist).size() > 1) {
                ContentValues values = new ContentValues();
                values.put(ARTISTS_ARTIST, Artist.COMPILATION_ARTIST.getName());
                values.put(ARTISTS_ARTISTDISAMBIGUATION, "");
                String artistKey = Artist.COMPILATION_ARTIST.getName() + "♠" + "";
                Long lastModified = artistLastModifiedMap.get(artistKey);
                if (lastModified == null || lastModified < track.lastModified) {
                    artistLastModifiedMap.put(artistKey, track.lastModified);
                    lastModified = track.lastModified;
                }
                values.put(ARTISTS_LASTMODIFIED, lastModified);
                values.put(ARTISTS_TYPE, TYPE_DEFAULT);
                mDb.insert(TABLE_ARTISTS, null, values);
            }
            ContentValues values = new ContentValues();
            values.put(ARTISTS_ARTIST, track.artist);
            values.put(ARTISTS_ARTISTDISAMBIGUATION, track.artistDisambiguation);
            String artistKey = track.artist + "♠" + track.artistDisambiguation;
            Long lastModified = artistLastModifiedMap.get(artistKey);
            if (lastModified == null || lastModified < track.lastModified) {
                artistLastModifiedMap.put(artistKey, track.lastModified);
                lastModified = track.lastModified;
            }
            values.put(ARTISTS_LASTMODIFIED, lastModified);
            values.put(ARTISTS_TYPE, TYPE_DEFAULT);
            mDb.insert(TABLE_ARTISTS, null, values);
            values = new ContentValues();
            values.put(ALBUMARTISTS_ALBUMARTIST, track.albumArtist);
            values.put(ALBUMARTISTS_ALBUMARTISTDISAMBIGUATION, track.albumArtistDisambiguation);
            values.put(ALBUMARTISTS_LASTMODIFIED, lastModified);
            mDb.insert(TABLE_ALBUMARTISTS, null, values);
        }
        mDb.setTransactionSuccessful();
        mDb.endTransaction();

        Cursor cursor = mDb.query(TABLE_ARTISTS,
                new String[]{ID, ARTISTS_ARTIST, ARTISTS_ARTISTDISAMBIGUATION},
                null, null, null, null, null);
        Map<String, Integer> cachedArtists = cursorToMap(cursor);

        Map<String, Long> albumLastModifiedMap = new HashMap<>();
        mDb.beginTransaction();
        for (ScriptResolverTrack track : tracks) {
            ContentValues values = new ContentValues();
            values.put(ALBUMS_ALBUM, track.album);
            int albumArtistId;
            if (albumArtists.get(track.album + "♠" + track.albumArtist).size() == 1) {
                albumArtistId = cachedArtists.get(
                        concatKeys(track.artist, track.artistDisambiguation));
            } else {
                albumArtistId = cachedArtists.get(
                        concatKeys(Artist.COMPILATION_ARTIST.getName(), ""));
            }
            values.put(ALBUMS_ALBUMARTISTID, albumArtistId);
            values.put(ALBUMS_IMAGEPATH, track.imagePath);
            String artistKey = track.album + "♠" + albumArtistId;
            Long lastModified = albumLastModifiedMap.get(artistKey);
            if (lastModified == null || lastModified < track.lastModified) {
                albumLastModifiedMap.put(artistKey, track.lastModified);
                lastModified = track.lastModified;
            }
            values.put(ALBUMS_LASTMODIFIED, lastModified);
            values.put(ALBUMS_TYPE, TYPE_DEFAULT);
            mDb.insert(TABLE_ALBUMS, null, values);
        }
        mDb.setTransactionSuccessful();
        mDb.endTransaction();

        cursor = mDb.query(TABLE_ALBUMS,
                new String[]{ID, ALBUMS_ALBUM, ALBUMS_ALBUMARTISTID},
                null, null, null, null, null);
        Map<String, Integer> cachedAlbums = cursorToMap(cursor);

        mDb.beginTransaction();
        for (ScriptResolverTrack track : tracks) {
            ContentValues values = new ContentValues();
            int albumArtistId;
            if (albumArtists.get(track.album + "♠" + track.albumArtist).size() == 1) {
                albumArtistId = cachedArtists.get(
                        concatKeys(track.artist, track.artistDisambiguation));
            } else {
                albumArtistId = cachedArtists.get(
                        concatKeys(Artist.COMPILATION_ARTIST.getName(), ""));
            }
            int artistId = cachedArtists.get(concatKeys(track.artist, track.artistDisambiguation));
            int albumId = cachedAlbums.get(concatKeys(track.album, albumArtistId));
            values.put(ARTISTALBUMS_ARTISTID, artistId);
            values.put(ARTISTALBUMS_ALBUMID, albumId);
            mDb.insert(TABLE_ARTISTALBUMS, null, values);
            values = new ContentValues();
            values.put(TRACKS_TRACK, track.track);
            values.put(TRACKS_ARTISTID, artistId);
            values.put(TRACKS_ALBUMID, albumId);
            values.put(TRACKS_URL, track.url);
            values.put(TRACKS_DURATION, (int) track.duration);
            values.put(TRACKS_LINKURL, track.linkUrl);
            values.put(TRACKS_ALBUMPOS, track.albumpos);
            values.put(TRACKS_LASTMODIFIED, track.lastModified);
            mDb.insert(TABLE_TRACKS, null, values);
        }
        mDb.setTransactionSuccessful();
        mDb.endTransaction();

        Log.d(TAG, "Added " + tracks.size() + " tracks in " + (System.currentTimeMillis() - time)
                + "ms");
        if (tracks.size() > 0) {
            storeNewRevision(String.valueOf(System.currentTimeMillis()), ACTION_ADDTRACKS);
        }
        mFuzzyIndex.ensureIndex();
        ((DbCollection) CollectionManager.get().getCollection(mCollectionId)).setInitialized(true);
    }

    private static Map<String, Integer> cursorToMap(Cursor cursor) {
        Map<String, Integer> map = new HashMap<>();
        try {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                do {
                    map.put(concatKeys(cursor.getString(1), cursor.getString(2)),
                            cursor.getInt(0));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return map;
    }

    public synchronized void wipe() {
        wipe(mDb);
    }

    private void wipe(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `" + TABLE_ARTISTS + "`;");
        db.execSQL(CREATE_TABLE_ARTISTS);
        db.execSQL("DROP TABLE IF EXISTS `" + TABLE_ALBUMARTISTS + "`;");
        db.execSQL(CREATE_TABLE_ALBUMARTISTS);
        db.execSQL("DROP TABLE IF EXISTS `" + TABLE_ALBUMS + "`;");
        db.execSQL(CREATE_TABLE_ALBUMS);
        db.execSQL("DROP TABLE IF EXISTS `" + TABLE_ARTISTALBUMS + "`;");
        db.execSQL(CREATE_TABLE_ARTISTALBUMS);
        db.execSQL("DROP TABLE IF EXISTS `" + TABLE_TRACKS + "`;");
        db.execSQL(CREATE_TABLE_TRACKS);
        storeNewRevision(db, String.valueOf(System.currentTimeMillis()), ACTION_WIPE);
    }

    /**
     * Convenience method. Uses a default set of fields.
     */
    public synchronized Cursor tracks(WhereInfo where, String[] orderBy) {
        String[] fields = new String[]{ARTISTS_ARTIST, ARTISTS_ARTISTDISAMBIGUATION, ALBUMS_ALBUM,
                TRACKS_TRACK, TRACKS_DURATION, TRACKS_URL, TRACKS_LINKURL, TRACKS_ALBUMPOS,
                TRACKS_LASTMODIFIED, TRACKS_ALBUMID};
        return tracks(where, orderBy, fields);
    }

    public synchronized Cursor tracks(WhereInfo where, String[] orderBy, String[] fields) {
        List<JoinInfo> joinInfos = new ArrayList<>();
        JoinInfo joinInfo = new JoinInfo();
        joinInfo.table = TABLE_ARTISTS;
        joinInfo.conditions.put(TABLE_TRACKS + "." + TRACKS_ARTISTID, TABLE_ARTISTS + "." + ID);
        joinInfos.add(joinInfo);
        joinInfo = new JoinInfo();
        joinInfo.table = TABLE_ALBUMS;
        joinInfo.conditions.put(TABLE_TRACKS + "." + TRACKS_ALBUMID, TABLE_ALBUMS + "." + ID);
        joinInfos.add(joinInfo);
        String[] groupBy = new String[]{TRACKS_TRACK, ARTISTS_ARTIST, ALBUMS_ALBUM};
        return sqlSelect(TABLE_TRACKS, fields, where, joinInfos, orderBy, groupBy, null,
                TRACKS_LASTMODIFIED, false);
    }

    public synchronized long tracksCurrentRevision() {
        String[] fields = new String[]{TRACKS_LASTMODIFIED};
        long currentRevision = -1;
        Cursor cursor = null;
        try {
            cursor = sqlSelect(TABLE_TRACKS, fields, null, null,
                    new String[]{TRACKS_LASTMODIFIED + " DESC"}, null, null, null, false);
            if (cursor.moveToFirst()) {
                currentRevision = cursor.getLong(0);
            } else {
                Log.e(TAG, "tracksCurrentRevision - no tracks in table!");
                return -1;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return currentRevision;
    }

    public synchronized Cursor albums(String[] orderBy) {
        String[] fields = new String[]{ALBUMS_ALBUM, ARTISTS_ARTIST, ARTISTS_ARTISTDISAMBIGUATION,
                ALBUMS_IMAGEPATH, ALBUMS_LASTMODIFIED};
        List<JoinInfo> joinInfos = new ArrayList<>();
        JoinInfo joinInfo = new JoinInfo();
        joinInfo.table = TABLE_ARTISTS;
        joinInfo.conditions.put(
                TABLE_ALBUMS + "." + ALBUMS_ALBUMARTISTID, TABLE_ARTISTS + "." + ID);
        joinInfos.add(joinInfo);
        WhereInfo whereInfo = new WhereInfo();
        whereInfo.connection = "AND";
        whereInfo.where.put(ALBUMS_ALBUM, new String[]{""});
        whereInfo.equals = false;
        String[] groupBy = new String[]{ALBUMS_ALBUM, ARTISTS_ARTIST, ARTISTS_ARTISTDISAMBIGUATION};
        return sqlSelect(TABLE_ALBUMS, fields, whereInfo, joinInfos, orderBy, groupBy, ALBUMS_TYPE,
                ALBUMS_LASTMODIFIED, false);
    }

    public synchronized Cursor artists(String[] orderBy) {
        String[] fields = new String[]{ARTISTS_ARTIST, ARTISTS_ARTISTDISAMBIGUATION,
                ARTISTS_LASTMODIFIED};
        JoinInfo joinInfo = new JoinInfo();
        joinInfo.table = TABLE_ARTISTS;
        WhereInfo whereInfo = new WhereInfo();
        whereInfo.connection = "AND";
        whereInfo.where.put(ARTISTS_ARTIST, new String[]{Artist.COMPILATION_ARTIST.getName(), ""});
        whereInfo.equals = false;
        String[] groupBy = new String[]{ARTISTS_ARTIST, ARTISTS_ARTISTDISAMBIGUATION};
        return sqlSelect(TABLE_ARTISTS, fields, whereInfo, null, orderBy, groupBy, ARTISTS_TYPE,
                ARTISTS_LASTMODIFIED, false);
    }

    public synchronized Cursor albumArtists(String[] orderBy) {
        String[] fields = new String[]{ALBUMARTISTS_ALBUMARTIST,
                ALBUMARTISTS_ALBUMARTISTDISAMBIGUATION, ALBUMARTISTS_LASTMODIFIED};
        String[] groupBy = new String[]{ALBUMARTISTS_ALBUMARTIST,
                ALBUMARTISTS_ALBUMARTISTDISAMBIGUATION};
        WhereInfo whereInfo = new WhereInfo();
        whereInfo.connection = "AND";
        whereInfo.where.put(ARTISTS_ARTIST, new String[]{Artist.COMPILATION_ARTIST.getName(), ""});
        whereInfo.equals = false;
        return sqlSelect(TABLE_ALBUMARTISTS, fields, whereInfo, null, orderBy, groupBy, null,
                ALBUMARTISTS_LASTMODIFIED, false);
    }

    public synchronized long artistCurrentRevision(String artist, String artistDisambiguation) {
        String[] fields = new String[]{ARTISTS_LASTMODIFIED};
        WhereInfo whereInfo = new WhereInfo();
        whereInfo.connection = "AND";
        whereInfo.where.put(ARTISTS_ARTIST, new String[]{artist});
        whereInfo.where.put(ARTISTS_ARTISTDISAMBIGUATION, new String[]{artistDisambiguation});
        long currentRevision = -1;
        Cursor cursor = null;
        try {
            cursor = sqlSelect(TABLE_ARTISTS, fields, whereInfo, null, null, null, null, null,
                    false);
            if (cursor.moveToFirst()) {
                currentRevision = cursor.getLong(0);
            } else {
                Log.e(TAG, "artistCurrentRevision - Couldn't find artist with given name!");
                return -1;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return currentRevision;
    }

    public synchronized Cursor artistAlbums(String artist, String artistDisambiguation) {
        String[] fields = new String[]{ID};
        WhereInfo whereInfo = new WhereInfo();
        whereInfo.connection = "AND";
        whereInfo.where.put(ARTISTS_ARTIST, new String[]{artist});
        whereInfo.where.put(ARTISTS_ARTISTDISAMBIGUATION, new String[]{artistDisambiguation});
        int artistId;
        Cursor cursor = null;
        try {
            cursor = sqlSelect(TABLE_ARTISTS, fields, whereInfo, null, null, null, ARTISTS_TYPE,
                    null, true);
            if (cursor.moveToFirst()) {
                artistId = cursor.getInt(0);
            } else {
                Log.e(TAG, "artistAlbums - Couldn't find artist with given name!");
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        fields = new String[]{ALBUMS_ALBUM, ARTISTS_ARTIST, ARTISTS_ARTISTDISAMBIGUATION,
                ALBUMS_IMAGEPATH, ALBUMS_LASTMODIFIED};
        whereInfo = new WhereInfo();
        whereInfo.connection = "AND";
        whereInfo.where.put(ARTISTALBUMS_ARTISTID, new String[]{String.valueOf(artistId)});
        List<JoinInfo> joinInfos = new ArrayList<>();
        JoinInfo joinInfo = new JoinInfo();
        joinInfo.table = TABLE_ALBUMS;
        joinInfo.conditions.put(
                TABLE_ARTISTALBUMS + "." + ARTISTALBUMS_ALBUMID, TABLE_ALBUMS + "." + ID);
        joinInfos.add(joinInfo);
        joinInfo = new JoinInfo();
        joinInfo.table = TABLE_ARTISTS;
        joinInfo.conditions.put(
                TABLE_ALBUMS + "." + ALBUMS_ALBUMARTISTID, TABLE_ARTISTS + "." + ID);
        joinInfos.add(joinInfo);
        return sqlSelect(TABLE_ARTISTALBUMS, fields, whereInfo, joinInfos,
                new String[]{ALBUMS_ALBUM}, null, ALBUMS_TYPE, null, true);
    }

    public synchronized long albumCurrentRevision(String album, String albumArtist,
            String albumArtistDisambiguation) {
        String[] fields = new String[]{ID};
        WhereInfo whereInfo = new WhereInfo();
        whereInfo.connection = "AND";
        whereInfo.where.put(ARTISTS_ARTIST, new String[]{albumArtist});
        whereInfo.where.put(ARTISTS_ARTISTDISAMBIGUATION, new String[]{albumArtistDisambiguation});
        int artistId;
        Cursor cursor = null;
        try {
            cursor = sqlSelect(TABLE_ARTISTS, fields, whereInfo, null, null, null, null, null,
                    false);
            if (cursor.moveToFirst()) {
                artistId = cursor.getInt(0);
            } else {
                Log.e(TAG, "albumCurrentRevision - Couldn't find artist with given name!");
                return -1;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        fields = new String[]{ALBUMS_LASTMODIFIED};
        whereInfo = new WhereInfo();
        whereInfo.connection = "AND";
        whereInfo.where.put(ALBUMS_ALBUM, new String[]{album});
        whereInfo.where.put(ALBUMS_ALBUMARTISTID, new String[]{String.valueOf(artistId)});
        long currentRevision = -1;
        cursor = null;
        try {
            cursor = sqlSelect(TABLE_ALBUMS, fields, whereInfo, null, null, null, null, null,
                    false);
            if (cursor.moveToFirst()) {
                currentRevision = cursor.getLong(0);
            } else {
                Log.e(TAG, "albumCurrentRevision - Couldn't find album with given name!");
                return -1;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return currentRevision;
    }

    public synchronized Cursor albumTracks(String album, String albumArtist,
            String albumArtistDisambiguation) {
        String[] fields = new String[]{ID};
        WhereInfo whereInfo = new WhereInfo();
        whereInfo.connection = "AND";
        whereInfo.where.put(ARTISTS_ARTIST, new String[]{albumArtist});
        whereInfo.where.put(ARTISTS_ARTISTDISAMBIGUATION, new String[]{albumArtistDisambiguation});
        int artistId;
        Cursor cursor = null;
        try {
            cursor = sqlSelect(TABLE_ARTISTS, fields, whereInfo, null, null, null, ARTISTS_TYPE,
                    null, true);
            if (cursor.moveToFirst()) {
                artistId = cursor.getInt(0);
            } else {
                Log.e(TAG, "albumTracks - Couldn't find artist with given name!");
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        fields = new String[]{ID};
        whereInfo = new WhereInfo();
        whereInfo.connection = "AND";
        whereInfo.where.put(ALBUMS_ALBUM, new String[]{album});
        whereInfo.where.put(ALBUMS_ALBUMARTISTID, new String[]{String.valueOf(artistId)});
        int albumId;
        cursor = null;
        try {
            cursor = sqlSelect(TABLE_ALBUMS, fields, whereInfo, null, null, null, ALBUMS_TYPE,
                    null, true);
            if (cursor.moveToFirst()) {
                albumId = cursor.getInt(0);
            } else {
                Log.e(TAG, "albumTracks - Couldn't find album with given name!");
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        whereInfo = new WhereInfo();
        whereInfo.connection = "AND";
        whereInfo.where.put(TRACKS_ALBUMID, new String[]{String.valueOf(albumId)});
        return tracks(whereInfo, new String[]{TRACKS_ALBUMPOS});
    }

    public synchronized Cursor artistTracks(String artist, String artistDisambiguation) {
        String[] fields = new String[]{ID};
        WhereInfo whereInfo = new WhereInfo();
        whereInfo.connection = "AND";
        whereInfo.where.put(ARTISTS_ARTIST, new String[]{artist});
        whereInfo.where.put(ARTISTS_ARTISTDISAMBIGUATION, new String[]{artistDisambiguation});
        int artistId;
        Cursor cursor = null;
        try {
            cursor = sqlSelect(TABLE_ARTISTS, fields, whereInfo, null, null, null, ARTISTS_TYPE,
                    null, true);
            if (cursor.moveToFirst()) {
                artistId = cursor.getInt(0);
            } else {
                Log.e(TAG, "artistTracks - Couldn't find artist with given name!");
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        whereInfo = new WhereInfo();
        whereInfo.connection = "AND";
        whereInfo.where.put(TRACKS_ARTISTID, new String[]{String.valueOf(artistId)});
        return tracks(whereInfo, new String[]{TRACKS_ALBUMID});
    }

    private Cursor sqlSelect(String table, String[] fields, WhereInfo where,
            List<JoinInfo> joinInfos, String[] orderBy, String[] groupBy, String typeColumn,
            String lastModifiedColumn, boolean filterAllLoved) {
        String whereString = "";
        List<String> allWhereValues = new ArrayList<>();
        if (where != null) {
            whereString = " WHERE ";
            boolean notFirst = false;
            for (String whereKey : where.where.keySet()) {
                String[] whereValues = where.where.get(whereKey);
                for (String whereValue : whereValues) {
                    if (notFirst) {
                        whereString += " " + where.connection + " ";
                    }
                    notFirst = true;
                    whereString += table + "." + whereKey + (where.equals ? " = " : " != ") + "?";
                    allWhereValues.add(whereValue);
                }
            }
        }
        if (typeColumn != null) {
            if (whereString.isEmpty()) {
                whereString = " WHERE ";
            } else {
                whereString += " AND ";
            }
            // filter out all implicitly added items
            whereString += typeColumn + " != ?";
            allWhereValues.add(String.valueOf(TYPE_HATCHET_IMPLICIT));
        }
        if (filterAllLoved) {
            if (whereString.isEmpty()) {
                whereString = " WHERE ";
            } else {
                whereString += " AND ";
            }
            // filter out all explicitly added items
            whereString += typeColumn + " != ?";
            allWhereValues.add(String.valueOf(TYPE_HATCHET_EXPLICIT));
        }

        String joinString = "";
        if (joinInfos != null) {
            for (JoinInfo joinInfo : joinInfos) {
                joinString += " INNER JOIN " + joinInfo.table + " ON ";
                boolean notFirst = false;
                for (String joinKey : joinInfo.conditions.keySet()) {
                    if (notFirst) {
                        joinString += " AND ";
                    }
                    notFirst = true;
                    joinString += joinKey + " = " + joinInfo.conditions.get(joinKey);
                }
            }
        }

        String orderString = StringUtils.join(" , ", orderBy);
        if (orderBy != null) {
            orderString = " ORDER BY " + orderString;
        } else {
            orderString = "";
        }

        String deduplicationOrderString = "";
        String groupString = StringUtils.join(" , ", groupBy);
        if (groupBy != null) {
            groupString = " GROUP BY " + groupString;
            if (lastModifiedColumn != null) {
                deduplicationOrderString = " ORDER BY " + lastModifiedColumn;
            }
        } else {
            groupString = "";
        }

        String fieldsString = StringUtils.join(", ", fields);
        if (fields == null) {
            fieldsString = "*";
        }

        String statement = "SELECT * FROM ( SELECT " + fieldsString + " FROM " + table + joinString
                + whereString + deduplicationOrderString + " ) " + groupString + orderString;
        String[] allWhereValuesArray = null;
        if (allWhereValues.size() > 0) {
            allWhereValuesArray = allWhereValues.toArray(new String[allWhereValues.size()]);
        }
        return mDb.rawQuery(statement, allWhereValuesArray);
    }

    private void storeNewRevision(String revision, int action) {
        storeNewRevision(mDb, revision, action);
    }

    private static void storeNewRevision(SQLiteDatabase db, String revision, int action) {
        Cursor cursor = db.query(TABLE_TRACKS, new String[]{ID}, null, null, null, null, null);
        int trackCount = cursor.getCount();
        cursor.close();

        db.beginTransaction();
        ContentValues values = new ContentValues();
        values.put(REVISIONHISTORY_ACTION, action);
        values.put(REVISIONHISTORY_TRACKCOUNT, trackCount);
        values.put(REVISIONHISTORY_REVISION, revision);
        values.put(REVISIONHISTORY_TIMESTAMP, System.currentTimeMillis());
        db.insert(TABLE_REVISIONHISTORY, null, values);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public String getRevision() {
        Cursor cursor = null;
        try {
            cursor = mDb.query(TABLE_REVISIONHISTORY, new String[]{REVISIONHISTORY_REVISION},
                    null, null, null, null, REVISIONHISTORY_TIMESTAMP + " DESC", "1");
            if (!cursor.moveToFirst()) {
                return null;
            }
            return cursor.getString(0);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public long getLastUpdated() {
        Cursor cursor = null;
        try {
            cursor = mDb.query(TABLE_REVISIONHISTORY, new String[]{REVISIONHISTORY_TIMESTAMP},
                    null, null, null, null, REVISIONHISTORY_TIMESTAMP + " DESC", "1");
            if (!cursor.moveToFirst()) {
                return -1;
            }
            return cursor.getLong(0);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static String concatKeys(Object... keys) {
        String result = "";
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                result += "♣";
            }
            result += keys[i];
        }
        return result;
    }

    public FuzzyIndex getFuzzyIndex() {
        return mFuzzyIndex;
    }

}
