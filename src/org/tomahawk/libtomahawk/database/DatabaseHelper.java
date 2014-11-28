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

import com.fasterxml.jackson.core.JsonProcessingException;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystemUtils;
import org.tomahawk.libtomahawk.infosystem.QueryParams;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.utils.MediaWithDate;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * This class provides a way of storing user created {@link org.tomahawk.libtomahawk.collection.Playlist}s
 * in the database
 */
public class DatabaseHelper {

    private static final String TAG = DatabaseHelper.class.getSimpleName();

    public static final String PLAYLISTSDATASOURCE_RESULTSREPORTED
            = "org.tomahawk.tomahawk_android.playlistsdatasource_resultsreported";

    public static final String PLAYLISTSDATASOURCE_RESULTSREPORTED_PLAYLISTID
            = "org.tomahawk.tomahawk_android.playlistsdatasource_resultsreported_playlistid";

    public static final String CACHED_PLAYLIST_NAME = "Last used playlist";

    public static final String CACHED_PLAYLIST_ID = "cached_playlist_id";

    public static final String QUEUE_NAME = "Queue";

    public static final String QUEUE_ID = "queue_id";

    public static final String LOVEDITEMS_PLAYLIST_NAME = "My loved tracks";

    public static final String LOVEDITEMS_PLAYLIST_ID = "loveditems_playlist_id";

    public static final int FALSE = 0;

    public static final int TRUE = 1;

    public static final int CHUNK_SIZE = 50;

    private static class Holder {

        private static final DatabaseHelper instance = new DatabaseHelper();

    }

    // Database fields
    private SQLiteDatabase mDatabase;

    private DatabaseHelper() {
        TomahawkSQLiteHelper dbHelper = new TomahawkSQLiteHelper(TomahawkApp.getContext());
        dbHelper.close();
        mDatabase = dbHelper.getWritableDatabase();
    }

    public static DatabaseHelper getInstance() {
        return Holder.instance;
    }

    /**
     * Store the given {@link Playlist}
     *
     * @param playlist the given {@link Playlist}
     */
    public void storePlaylist(final Playlist playlist) {
        storePlaylist(playlist, false);
    }

    /**
     * Store the given {@link Playlist}
     *
     * @param playlist       the given {@link Playlist}
     * @param reverseEntries set to true, if the order of the entries should be reversed before
     *                       storing in the database
     */
    public void storePlaylist(final Playlist playlist, final boolean reverseEntries) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    ContentValues values = new ContentValues();
                    values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME, playlist.getName());
                    values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION,
                            playlist.getCurrentRevision());
                    values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID, playlist.getId());
                    values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_HATCHETID,
                            playlist.getHatchetId());

                    mDatabase.beginTransaction();
                    mDatabase.insertWithOnConflict(TomahawkSQLiteHelper.TABLE_PLAYLISTS, null,
                            values,
                            SQLiteDatabase.CONFLICT_REPLACE);
                    // Delete every already associated Track entry
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                            TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = ?",
                            new String[]{playlist.getId()});

                    // Store every single Track in the database and store the relationship
                    // by storing the playlists's id with it
                    ArrayList<PlaylistEntry> entries = playlist.getEntries();
                    for (int i = 0; i < entries.size(); i++) {
                        PlaylistEntry entry;
                        if (reverseEntries) {
                            entry = entries.get(entries.size() - 1 - i);
                        } else {
                            entry = entries.get(i);
                        }
                        values.clear();
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID, playlist.getId());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME,
                                entry.getQuery().getBasicTrack().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME,
                                entry.getQuery().getBasicTrack().getArtist().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ALBUMNAME,
                                entry.getQuery().getBasicTrack().getAlbum().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_RESULTHINT,
                                entry.getQuery().getTopTrackResultKey());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTENTRYINDEX, i);
                        if (entry.getQuery().isFetchedViaHatchet()) {
                            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                                    TRUE);
                        } else {
                            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                                    FALSE);
                        }
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTENTRYID,
                                entry.getId());
                        mDatabase.insert(TomahawkSQLiteHelper.TABLE_TRACKS, null, values);
                    }
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast(playlist.getId());
                }
            }
        }).start();
    }

    /**
     * Rename the given {@link Playlist}
     *
     * @param playlist the given {@link Playlist}
     * @param newName  the new playlist name
     */
    public void renamePlaylist(final Playlist playlist, final String newName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    ContentValues values = new ContentValues();
                    values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME, newName);
                    String insertId = playlist.getId();
                    values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION,
                            playlist.getCurrentRevision());
                    values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID, insertId);

                    mDatabase.beginTransaction();
                    mDatabase.insertWithOnConflict(TomahawkSQLiteHelper.TABLE_PLAYLISTS, null,
                            values, SQLiteDatabase.CONFLICT_REPLACE);
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast(playlist.getId());
                }
            }
        }).start();
    }

    /**
     * @return the stored {@link org.tomahawk.libtomahawk.collection.Playlist} with
     * CACHED_PLAYLIST_ID as its id
     */
    public Playlist getCachedPlaylist() {
        return getPlaylist(CACHED_PLAYLIST_ID);
    }

    /**
     * @return every stored {@link org.tomahawk.libtomahawk.collection.Playlist} in the database
     */
    public ArrayList<Playlist> getPlaylists() {
        ArrayList<Playlist> playListList = new ArrayList<Playlist>();
        String[] columns = new String[]{TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID};

        Cursor playlistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                columns, TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " != ? AND "
                        + TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " != ?",
                new String[]{CACHED_PLAYLIST_ID, LOVEDITEMS_PLAYLIST_ID}, null, null, null
        );
        playlistsCursor.moveToFirst();
        while (!playlistsCursor.isAfterLast()) {
            Playlist playlist = getEmptyPlaylist(playlistsCursor.getString(0));
            if (playlist != null) {
                playListList.add(playlist);
            }
            playlistsCursor.moveToNext();
        }
        playlistsCursor.close();
        return playListList;
    }

    /**
     * @param playlistId the id by which to get the correct {@link org.tomahawk.libtomahawk.collection.Playlist}
     * @return the stored {@link org.tomahawk.libtomahawk.collection.Playlist} with playlistId as
     * its id
     */
    public Playlist getEmptyPlaylist(String playlistId) {
        String[] columns = new String[]{TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_HATCHETID};

        Cursor playlistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                columns, TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                new String[]{playlistId}, null, null, null);
        if (playlistsCursor.moveToFirst()) {
            Playlist playlist = Playlist.get(playlistId,
                    playlistsCursor.getString(0), playlistsCursor.getString(1));
            playlist.setHatchetId(playlistsCursor.getString(2));
            playlistsCursor.close();
            return playlist;
        }
        playlistsCursor.close();
        return null;
    }

    /**
     * @param playlistId the id by which to get the correct {@link org.tomahawk.libtomahawk.collection.Playlist}
     * @return the stored {@link org.tomahawk.libtomahawk.collection.Playlist} with playlistId as
     * its id
     */
    public Playlist getPlaylist(String playlistId) {
        if (playlistId.equals(LOVEDITEMS_PLAYLIST_ID)) {
            return getPlaylist(playlistId, true);
        } else {
            return getPlaylist(playlistId, false);
        }
    }

    /**
     * @param playlistId the id by which to get the correct {@link org.tomahawk.libtomahawk.collection.Playlist}
     * @return the stored {@link org.tomahawk.libtomahawk.collection.Playlist} with playlistId as
     * its id
     */
    public Playlist getPlaylist(String playlistId, boolean reverseEntries) {
        String[] columns = new String[]{TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_HATCHETID};

        Cursor playlistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                columns, TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                new String[]{playlistId}, null, null, null);
        if (playlistsCursor.moveToFirst()) {
            columns = new String[]{TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_ALBUMNAME,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_RESULTHINT,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTENTRYID};
            Cursor tracksCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_TRACKS, columns,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = ?",
                    new String[]{playlistId}, null, null,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTENTRYINDEX + (reverseEntries
                            ? " DESC" : " ASC"));
            ArrayList<PlaylistEntry> queries = new ArrayList<PlaylistEntry>();
            tracksCursor.moveToFirst();
            while (!tracksCursor.isAfterLast()) {
                String trackName = tracksCursor.getString(0);
                String artistName = tracksCursor.getString(1);
                String albumName = tracksCursor.getString(2);
                String resultHint = tracksCursor.getString(3);
                Query query = Query.get(trackName, albumName, artistName, resultHint, false,
                        tracksCursor.getInt(4) == TRUE);
                String entryId;
                if (tracksCursor.getString(5) != null) {
                    entryId = tracksCursor.getString(5);
                } else {
                    entryId = TomahawkMainActivity.getLifetimeUniqueStringId();
                }
                PlaylistEntry entry = PlaylistEntry.get(playlistId, query, entryId);
                queries.add(entry);
                tracksCursor.moveToNext();
            }
            Playlist playlist = Playlist.fromEntriesList(playlistsCursor.getString(0),
                    playlistsCursor.getString(1), queries);
            playlist.setId(playlistId);
            playlist.setHatchetId(playlistsCursor.getString(2));
            playlist.setFilled(true);
            tracksCursor.close();
            playlistsCursor.close();
            return playlist;
        }
        playlistsCursor.close();
        return null;
    }

    /**
     * @param playlistId the id by which to get the correct {@link org.tomahawk.libtomahawk.collection.Playlist}
     * @return the stored {@link org.tomahawk.libtomahawk.collection.Playlist} with playlistId as
     * its id
     */
    public int getPlaylistTrackCount(String playlistId) {
        int count = 0;
        String[] columns = new String[]{TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTTRACKINDEX};

        Cursor playlistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                columns, TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                new String[]{playlistId}, null, null, null);
        if (playlistsCursor.moveToFirst()) {
            columns = new String[]{TomahawkSQLiteHelper.TRACKS_COLUMN_ID};
            Cursor tracksCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_TRACKS, columns,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = ?",
                    new String[]{playlistId}, null, null, null);
            count = tracksCursor.getCount();
            tracksCursor.close();
        }
        playlistsCursor.close();
        return count;
    }

    /**
     * Delete the {@link org.tomahawk.libtomahawk.collection.Playlist} with the given id
     *
     * @param playlistId String containing the id of the {@link org.tomahawk.libtomahawk.collection.Playlist}
     *                   to be deleted
     */
    public void deletePlaylist(final String playlistId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {

                    mDatabase.beginTransaction();
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                            TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = ?",
                            new String[]{playlistId});
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                            TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                            new String[]{playlistId});
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast(playlistId);
                }
            }
        }).start();
    }

    /**
     * Delete the {@link org.tomahawk.libtomahawk.collection.PlaylistEntry} with the given key in
     * the {@link org.tomahawk.libtomahawk.collection.Playlist} with the given playlistId
     */
    public void deleteEntryInPlaylist(final String playlistId, final String entryId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {

                    mDatabase.beginTransaction();
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                            TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = ? AND "
                                    + TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTENTRYID
                                    + " = ?", new String[]{playlistId, entryId});
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast(playlistId);
                }
            }
        }).start();
    }

    /**
     * Add the given {@link ArrayList} of {@link Track}s to the {@link
     * org.tomahawk.libtomahawk.collection.Playlist} with the given playlistId
     */
    public void addQueriesToPlaylist(final String playlistId, final ArrayList<Query> queries) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    int trackCount = getPlaylistTrackCount(playlistId);

                    mDatabase.beginTransaction();
                    // Store every single Track in the database and store the relationship
                    // by storing the playlists's id with it
                    for (int i = 0; i < queries.size(); i++) {
                        Query query = queries.get(i);
                        ContentValues values = new ContentValues();
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID,
                                playlistId);
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME,
                                query.getBasicTrack().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME,
                                query.getBasicTrack().getArtist().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ALBUMNAME,
                                query.getBasicTrack().getAlbum().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_RESULTHINT,
                                query.getTopTrackResultKey());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTENTRYINDEX,
                                trackCount + i);
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
                    sendReportResultsBroadcast(playlistId);
                }
            }
        }).start();
    }

    /**
     * Add the given {@link ArrayList} of {@link Track}s to the {@link
     * org.tomahawk.libtomahawk.collection.Playlist} with the given playlistId
     */
    public void addEntriesToPlaylist(final String playlistId,
            final ArrayList<PlaylistEntry> entries) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    int trackCount = getPlaylistTrackCount(playlistId);

                    mDatabase.beginTransaction();
                    // Store every single Track in the database and store the relationship
                    // by storing the playlists's id with it
                    for (int i = 0; i < entries.size(); i++) {
                        PlaylistEntry entry = entries.get(i);
                        ContentValues values = new ContentValues();
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID,
                                playlistId);
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME,
                                entry.getQuery().getBasicTrack().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME,
                                entry.getQuery().getBasicTrack().getArtist().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ALBUMNAME,
                                entry.getQuery().getBasicTrack().getAlbum().getName());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_RESULTHINT,
                                entry.getQuery().getTopTrackResultKey());
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTENTRYINDEX,
                                trackCount + i);
                        if (entry.getQuery().isFetchedViaHatchet()) {
                            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                                    TRUE);
                        } else {
                            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                                    FALSE);
                        }
                        values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTENTRYID,
                                entry.getId());
                        mDatabase.insert(TomahawkSQLiteHelper.TABLE_TRACKS, null, values);
                    }
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast(playlistId);
                }
            }
        }).start();
    }

    /**
     * Checks if a query with the same track/artistName as the given query is included in the
     * lovedItems Playlist
     *
     * @return whether or not the given query is loved
     */
    public boolean isItemLoved(Query query) {
        String[] columns = new String[]{TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME,
                TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME};

        Cursor tracksCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_TRACKS, columns,
                TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = ?",
                new String[]{LOVEDITEMS_PLAYLIST_ID}, null, null, null
        );
        tracksCursor.moveToFirst();
        while (!tracksCursor.isAfterLast()) {
            String trackName = tracksCursor.getString(0);
            String artistName = tracksCursor.getString(1);
            if (query.getName().equalsIgnoreCase(trackName)
                    && query.getArtist().getName().equalsIgnoreCase(artistName)) {
                tracksCursor.close();
                return true;
            }
            tracksCursor.moveToNext();
        }
        tracksCursor.close();
        return false;
    }

    /**
     * Checks if an artist with the same artistName as the given artist is loved
     *
     * @return whether or not the given artist is loved
     */
    public boolean isItemLoved(Artist artist) {
        boolean isLoved = false;
        String[] columns = new String[]{TomahawkSQLiteHelper.LOVED_ARTISTS_COLUMN_ARTISTNAME};

        Cursor artistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_LOVED_ARTISTS, columns,
                TomahawkSQLiteHelper.LOVED_ARTISTS_COLUMN_ARTISTNAME + " = ?",
                new String[]{artist.getName()}, null, null, null);
        if (artistsCursor.getCount() > 0) {
            isLoved = true;
        }
        artistsCursor.close();
        return isLoved;
    }

    /**
     * Checks if an album with the same albumName as the given album is loved
     *
     * @return whether or not the given album is loved
     */
    public boolean isItemLoved(Album album) {
        boolean isLoved = false;
        String[] columns = new String[]{TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ALBUMNAME,
                TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ARTISTNAME};

        Cursor albumsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_LOVED_ALBUMS, columns,
                TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ALBUMNAME + " = ? AND "
                        + TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ARTISTNAME + " = ?",
                new String[]{album.getName(), album.getArtist().getName()}, null, null, null
        );
        if (albumsCursor.getCount() > 0) {
            isLoved = true;
        }
        albumsCursor.close();
        return isLoved;
    }

    /**
     * Store the given query as a lovedItem, if isLoved is true. Otherwise remove(unlove) the
     * query.
     */
    public void setLovedItem(Query query, boolean isLoved) {
        if (isLoved) {
            ArrayList<Query> queries = new ArrayList<Query>();
            queries.add(query);
            addQueriesToPlaylist(LOVEDITEMS_PLAYLIST_ID, queries);
        } else {

            mDatabase.beginTransaction();
            mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                    TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = ? AND "
                            + TomahawkSQLiteHelper.TRACKS_COLUMN_TRACKNAME + " = ? AND "
                            + TomahawkSQLiteHelper.TRACKS_COLUMN_ARTISTNAME + " = ?",
                    new String[]{LOVEDITEMS_PLAYLIST_ID, query.getName(),
                            query.getArtist().getName()});
            mDatabase.setTransactionSuccessful();
            mDatabase.endTransaction();
            sendReportResultsBroadcast(LOVEDITEMS_PLAYLIST_ID);
        }
    }

    /**
     * Store the given artist as a lovedItem, if isLoved is true. Otherwise remove(unlove) it.
     */
    public void setLovedItem(final Artist artist, final boolean isLoved) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {

                    mDatabase.beginTransaction();
                    if (isLoved) {
                        ContentValues values = new ContentValues();
                        values.put(TomahawkSQLiteHelper.LOVED_ARTISTS_COLUMN_ARTISTNAME,
                                artist.getName());
                        mDatabase.insert(TomahawkSQLiteHelper.TABLE_LOVED_ARTISTS, null, values);
                    } else {
                        mDatabase.delete(TomahawkSQLiteHelper.TABLE_LOVED_ARTISTS,
                                TomahawkSQLiteHelper.LOVED_ARTISTS_COLUMN_ARTISTNAME + " = ?",
                                new String[]{artist.getName()});
                    }
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                }
            }
        }).start();
    }

    /**
     * Store the given album as a lovedItem, if isLoved is true. Otherwise remove(unlove) it.
     */
    public void setLovedItem(final Album album, final boolean isLoved) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {

                    mDatabase.beginTransaction();
                    if (isLoved) {
                        ContentValues values = new ContentValues();
                        values.put(TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ALBUMNAME,
                                album.getName());
                        values.put(TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ARTISTNAME,
                                album.getArtist().getName());
                        mDatabase.insert(TomahawkSQLiteHelper.TABLE_LOVED_ALBUMS, null, values);
                    } else {
                        mDatabase.delete(TomahawkSQLiteHelper.TABLE_LOVED_ALBUMS,
                                TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ALBUMNAME + " = ? AND "
                                        + TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ARTISTNAME
                                        + " = ?",
                                new String[]{album.getName(), album.getArtist().getName()});
                    }
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                }
            }
        }).start();
    }

    public void storeStarredArtists(final List<Artist> artists) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {

                    mDatabase.beginTransaction();
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_LOVED_ARTISTS, null, null);
                    for (Artist artist : artists) {
                        ContentValues values = new ContentValues();
                        values.put(TomahawkSQLiteHelper.LOVED_ARTISTS_COLUMN_ARTISTNAME,
                                artist.getName());
                        mDatabase.insert(TomahawkSQLiteHelper.TABLE_LOVED_ARTISTS, null, values);
                    }
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast(null);
                }
            }
        }).start();
    }

    public void storeStarredAlbums(final List<Album> albums) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {

                    mDatabase.beginTransaction();
                    mDatabase.delete(TomahawkSQLiteHelper.TABLE_LOVED_ALBUMS, null, null);
                    for (Album album : albums) {
                        ContentValues values = new ContentValues();
                        values.put(TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ALBUMNAME,
                                album.getName());
                        values.put(TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ARTISTNAME,
                                album.getArtist().getName());
                        mDatabase.insert(TomahawkSQLiteHelper.TABLE_LOVED_ALBUMS, null, values);
                    }
                    mDatabase.setTransactionSuccessful();
                    mDatabase.endTransaction();
                    sendReportResultsBroadcast(null);
                }
            }
        }).start();
    }

    public ArrayList<Artist> getStarredArtists() {
        ArrayList<Artist> starredArtists = new ArrayList<Artist>();
        String[] columns = new String[]{TomahawkSQLiteHelper.LOVED_ARTISTS_COLUMN_ARTISTNAME};

        Cursor artistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_LOVED_ARTISTS, columns,
                null, null, null, null, null);
        artistsCursor.moveToFirst();
        while (!artistsCursor.isAfterLast()) {
            String artistName = artistsCursor.getString(0);
            starredArtists.add(Artist.get(artistName));
            artistsCursor.moveToNext();
        }
        artistsCursor.close();
        return starredArtists;
    }

    public ArrayList<Album> getStarredAlbums() {
        ArrayList<Album> starredAlbums = new ArrayList<Album>();
        String[] columns = new String[]{TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ARTISTNAME,
                TomahawkSQLiteHelper.LOVED_ALBUMS_COLUMN_ALBUMNAME};

        Cursor albumsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_LOVED_ALBUMS,
                columns, null, null, null, null, null);
        albumsCursor.moveToFirst();
        while (!albumsCursor.isAfterLast()) {
            String artistName = albumsCursor.getString(0);
            String albumName = albumsCursor.getString(1);
            starredAlbums.add(Album.get(albumName, Artist.get(artistName)));
            albumsCursor.moveToNext();
        }
        albumsCursor.close();
        return starredAlbums;
    }

    public Cursor getSearchHistoryCursor(String entry) {

        return mDatabase.query(TomahawkSQLiteHelper.TABLE_SEARCHHISTORY, null,
                TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ENTRY + " LIKE ?",
                new String[]{entry + "%"}, null, null,
                TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ID + " DESC");
    }

    public void addEntryToSearchHistory(String entry) {
        ContentValues values = new ContentValues();

        mDatabase.beginTransaction();
        values.put(TomahawkSQLiteHelper.SEARCHHISTORY_COLUMN_ENTRY, entry.trim());
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
        values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_HTTPTYPE, opToLog.getHttpType());
        values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TIMESTAMP, timeStamp);
        if (opToLog.getJsonStringToSend() != null) {
            values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_JSONSTRING,
                    opToLog.getJsonStringToSend());
        }
        if (opToLog.getQueryParams() != null) {
            String paramsJsonString = null;
            try {
                paramsJsonString = InfoSystemUtils.getObjectMapper()
                        .writeValueAsString(opToLog.getQueryParams());
            } catch (JsonProcessingException e) {
                Log.e(TAG,
                        "addOpToInfoSystemOpLog: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
            values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_PARAMS, paramsJsonString);
        }
        mDatabase.insert(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG, null, values);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * Remove the operation with the given id from the InfoSystem-OpLog table
     *
     * @param loggedOps a list of all the operations to remove from the InfoSystem-OpLog table
     */
    public void removeOpsFromInfoSystemOpLog(List<InfoRequestData> loggedOps) {

        mDatabase.beginTransaction();
        for (InfoRequestData loggedOp : loggedOps) {
            mDatabase.delete(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG,
                    TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_ID + " = ?",
                    new String[]{String.valueOf(loggedOp.getLoggedOpId())});
        }
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
                TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_ID + " = ?",
                new String[]{String.valueOf(opLogId)});
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * @return an InfoRequestData object that contains all data that should be delivered to the API
     */
    public List<InfoRequestData> getLoggedOps() {
        List<InfoRequestData> loggedOps = new ArrayList<InfoRequestData>();
        String[] columns = new String[]{TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_ID,
                TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TYPE,
                TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_HTTPTYPE,
                TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_JSONSTRING,
                TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_PARAMS};

        Cursor opLogCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG,
                columns, null, null, null, null,
                TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_TIMESTAMP + " DESC");
        opLogCursor.moveToFirst();
        while (!opLogCursor.isAfterLast()) {
            String requestId = TomahawkMainActivity.getSessionUniqueStringId();
            String paramJsonString = opLogCursor.getString(4);
            QueryParams params = null;
            if (paramJsonString != null) {
                try {
                    params = InfoSystemUtils.getObjectMapper()
                            .readValue(paramJsonString, QueryParams.class);
                } catch (IOException e) {
                    Log.e(TAG, "getLoggedOps: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
            }
            InfoRequestData infoRequestData = new InfoRequestData(requestId, opLogCursor.getInt(1),
                    params, opLogCursor.getInt(0), opLogCursor.getInt(2), opLogCursor.getString(3));
            loggedOps.add(infoRequestData);
            opLogCursor.moveToNext();
        }
        opLogCursor.close();
        return loggedOps;
    }

    /**
     * @return the count of all logged ops that should be delivered to the API
     */
    public int getLoggedOpsCount() {
        String[] columns = new String[]{TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_ID};

        Cursor opLogCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG,
                columns, null, null, null, null, null);
        int count = opLogCursor.getCount();
        opLogCursor.close();
        return count;
    }

    /**
     * Send a broadcast indicating that playlists have been changed in the database and should be
     * refetched
     */
    private void sendReportResultsBroadcast(String playlistId) {
        Intent reportIntent = new Intent(PLAYLISTSDATASOURCE_RESULTSREPORTED);
        if (playlistId != null) {
            reportIntent.putExtra(PLAYLISTSDATASOURCE_RESULTSREPORTED_PLAYLISTID, playlistId);
        }
        TomahawkApp.getContext().sendBroadcast(reportIntent);
    }

    /**
     * Add a new media to the database. The picture can only added by update.
     *
     * @param media which you like to add to the database
     */
    public synchronized void addMedia(MediaWithDate media) {
        ContentValues values = new ContentValues();

        values.put(TomahawkSQLiteHelper.MEDIA_LOCATION, media.getLocation());
        values.put(TomahawkSQLiteHelper.MEDIA_TIME, media.getTime());
        values.put(TomahawkSQLiteHelper.MEDIA_LENGTH, media.getLength());
        values.put(TomahawkSQLiteHelper.MEDIA_TYPE, media.getType());
        values.put(TomahawkSQLiteHelper.MEDIA_TITLE, media.getTitle());
        values.put(TomahawkSQLiteHelper.MEDIA_ARTIST, media.getArtist());
        values.put(TomahawkSQLiteHelper.MEDIA_GENRE, media.getGenre());
        values.put(TomahawkSQLiteHelper.MEDIA_ALBUM, media.getAlbum());
        values.put(TomahawkSQLiteHelper.MEDIA_WIDTH, media.getWidth());
        values.put(TomahawkSQLiteHelper.MEDIA_HEIGHT, media.getHeight());
        values.put(TomahawkSQLiteHelper.MEDIA_ARTWORKURL, media.getArtworkURL());
        values.put(TomahawkSQLiteHelper.MEDIA_AUDIOTRACK, media.getAudioTrack());
        values.put(TomahawkSQLiteHelper.MEDIA_SPUTRACK, media.getSpuTrack());
        values.put(TomahawkSQLiteHelper.MEDIA_DATEADDED, media.getDateAdded());

        mDatabase.beginTransaction();
        mDatabase.replace(TomahawkSQLiteHelper.TABLE_MEDIA, "NULL", values);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();

    }

    /**
     * Check if the item is already in the database
     *
     * @param location of the item (primary key)
     * @return True if the item exists, false if it does not
     */
    public synchronized boolean mediaItemExists(String location) {
        try {
            Cursor cursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_MEDIA,
                    new String[]{TomahawkSQLiteHelper.MEDIA_LOCATION},
                    TomahawkSQLiteHelper.MEDIA_LOCATION + "=?",
                    new String[]{location},
                    null, null, null);
            boolean exists = cursor.moveToFirst();
            cursor.close();
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "Query failed");
            return false;
        }
    }

    /**
     * Get all paths from the items in the database
     *
     * @return list of File
     */
    @SuppressWarnings("unused")
    private synchronized HashSet<File> getMediaFiles() {
        HashSet<File> files = new HashSet<File>();
        Cursor cursor;

        cursor = mDatabase.query(
                TomahawkSQLiteHelper.TABLE_MEDIA,
                new String[]{TomahawkSQLiteHelper.MEDIA_LOCATION},
                null, null, null, null, null);
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            do {
                File file = new File(cursor.getString(0));
                files.add(file);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return files;
    }

    public synchronized HashMap<String, MediaWithDate> getMedias() {
        Cursor cursor;
        HashMap<String, MediaWithDate> medias = new HashMap<String, MediaWithDate>();
        int chunk_count = 0;
        int count = 0;

        do {
            count = 0;
            cursor = mDatabase.rawQuery(String.format(Locale.US,
                    "SELECT %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s FROM %s LIMIT %d OFFSET %d",
                    TomahawkSQLiteHelper.MEDIA_TIME, //0 long
                    TomahawkSQLiteHelper.MEDIA_LENGTH, //1 long
                    TomahawkSQLiteHelper.MEDIA_TYPE, //2 int
                    TomahawkSQLiteHelper.MEDIA_TITLE, //3 string
                    TomahawkSQLiteHelper.MEDIA_ARTIST, //4 string
                    TomahawkSQLiteHelper.MEDIA_GENRE, //5 string
                    TomahawkSQLiteHelper.MEDIA_ALBUM, //6 string
                    TomahawkSQLiteHelper.MEDIA_WIDTH, //7 int
                    TomahawkSQLiteHelper.MEDIA_HEIGHT, //8 int
                    TomahawkSQLiteHelper.MEDIA_ARTWORKURL, //9 string
                    TomahawkSQLiteHelper.MEDIA_AUDIOTRACK, //10 string
                    TomahawkSQLiteHelper.MEDIA_SPUTRACK, //11 string
                    TomahawkSQLiteHelper.MEDIA_LOCATION, //12 string
                    TomahawkSQLiteHelper.MEDIA_DATEADDED, //13 long
                    TomahawkSQLiteHelper.TABLE_MEDIA,
                    CHUNK_SIZE,
                    chunk_count * CHUNK_SIZE), null);

            if (cursor.moveToFirst()) {
                do {
                    String location = cursor.getString(12);
                    MediaWithDate media = new MediaWithDate(location,
                            cursor.getLong(0),      // MEDIA_TIME
                            cursor.getLong(1),      // MEDIA_LENGTH
                            cursor.getInt(2),       // MEDIA_TYPE
                            null,                   // MEDIA_PICTURE
                            cursor.getString(3),    // MEDIA_TITLE
                            cursor.getString(4),    // MEDIA_ARTIST
                            cursor.getString(5),    // MEDIA_GENRE
                            cursor.getString(6),    // MEDIA_ALBUM
                            cursor.getInt(7),       // MEDIA_WIDTH
                            cursor.getInt(8),       // MEDIA_HEIGHT
                            cursor.getString(9),    // MEDIA_ARTWORKURL
                            cursor.getInt(10),      // MEDIA_AUDIOTRACK
                            cursor.getInt(11),      // MEDIA_SPUTRACK
                            cursor.getLong(13));    // MEDIA_DATEADDED
                    medias.put(media.getLocation(), media);

                    count++;
                } while (cursor.moveToNext());
            }

            cursor.close();
            chunk_count++;
        } while (count == CHUNK_SIZE);

        return medias;
    }

    public synchronized MediaWithDate getMedia(String location) {
        Cursor cursor;
        MediaWithDate media = null;

        try {
            cursor = mDatabase.query(
                    TomahawkSQLiteHelper.TABLE_MEDIA,
                    new String[]{
                            TomahawkSQLiteHelper.MEDIA_TIME, //0 long
                            TomahawkSQLiteHelper.MEDIA_LENGTH, //1 long
                            TomahawkSQLiteHelper.MEDIA_TYPE, //2 int
                            TomahawkSQLiteHelper.MEDIA_TITLE, //3 string
                            TomahawkSQLiteHelper.MEDIA_ARTIST, //4 string
                            TomahawkSQLiteHelper.MEDIA_GENRE, //5 string
                            TomahawkSQLiteHelper.MEDIA_ALBUM, //6 string
                            TomahawkSQLiteHelper.MEDIA_WIDTH, //7 int
                            TomahawkSQLiteHelper.MEDIA_HEIGHT, //8 int
                            TomahawkSQLiteHelper.MEDIA_ARTWORKURL, //9 string
                            TomahawkSQLiteHelper.MEDIA_AUDIOTRACK, //10 string
                            TomahawkSQLiteHelper.MEDIA_SPUTRACK, //11 string
                            TomahawkSQLiteHelper.MEDIA_DATEADDED, //12 long
                    },
                    TomahawkSQLiteHelper.MEDIA_LOCATION + "=?",
                    new String[]{location},
                    null, null, null);
        } catch (IllegalArgumentException e) {
            // java.lang.IllegalArgumentException: the bind value at index 1 is null
            return null;
        }
        if (cursor.moveToFirst()) {
            media = new MediaWithDate(location,
                    cursor.getLong(0),
                    cursor.getLong(1),
                    cursor.getInt(2),
                    null, // lazy loading, see getPicture()
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6),
                    cursor.getInt(7),
                    cursor.getInt(8),
                    cursor.getString(9),
                    cursor.getInt(10),
                    cursor.getInt(11),
                    cursor.getLong(12));
        }
        cursor.close();
        return media;
    }

    public synchronized Bitmap getPicture(Context context, String location) {
        /* Used for the lazy loading */
        Cursor cursor;
        Bitmap picture = null;
        byte[] blob;

        cursor = mDatabase.query(
                TomahawkSQLiteHelper.TABLE_MEDIA,
                new String[]{TomahawkSQLiteHelper.MEDIA_PICTURE},
                TomahawkSQLiteHelper.MEDIA_LOCATION + "=?",
                new String[]{location},
                null, null, null);
        if (cursor.moveToFirst()) {
            blob = cursor.getBlob(0);
            if (blob != null && blob.length > 1 && blob.length < 500000) {
                try {
                    picture = BitmapFactory.decodeByteArray(blob, 0, blob.length);
                } catch (OutOfMemoryError e) {
                    picture = null;
                } finally {
                    blob = null;
                }
            }
        }
        cursor.close();
        return picture;
    }

    public synchronized void removeMedia(String location) {
        mDatabase.beginTransaction();
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_MEDIA,
                TomahawkSQLiteHelper.MEDIA_LOCATION + "=?",
                new String[]{location});
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    public void removeMedias(Set<String> locations) {
        mDatabase.beginTransaction();
        try {
            for (String location : locations) {
                mDatabase.delete(TomahawkSQLiteHelper.TABLE_MEDIA,
                        TomahawkSQLiteHelper.MEDIA_LOCATION + "=?", new String[]{location});
            }
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }

    public synchronized void updateMedia(String location, TomahawkSQLiteHelper.mediaColumn col,
            Object object) {

        if (location == null) {
            return;
        }

        ContentValues values = new ContentValues();
        switch (col) {
            case MEDIA_PICTURE:
                if (object != null) {
                    Bitmap picture = (Bitmap) object;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    picture.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    values.put(TomahawkSQLiteHelper.MEDIA_PICTURE, out.toByteArray());
                } else {
                    values.put(TomahawkSQLiteHelper.MEDIA_PICTURE, new byte[1]);
                }
                break;
            case MEDIA_TIME:
                if (object != null) {
                    values.put(TomahawkSQLiteHelper.MEDIA_TIME, (Long) object);
                }
                break;
            case MEDIA_AUDIOTRACK:
                if (object != null) {
                    values.put(TomahawkSQLiteHelper.MEDIA_AUDIOTRACK, (Integer) object);
                }
                break;
            case MEDIA_SPUTRACK:
                if (object != null) {
                    values.put(TomahawkSQLiteHelper.MEDIA_SPUTRACK, (Integer) object);
                }
                break;
            case MEDIA_LENGTH:
                if (object != null) {
                    values.put(TomahawkSQLiteHelper.MEDIA_LENGTH, (Long) object);
                }
                break;
            default:
                return;
        }
        mDatabase.beginTransaction();
        mDatabase.update(TomahawkSQLiteHelper.TABLE_MEDIA, values,
                TomahawkSQLiteHelper.MEDIA_LOCATION + "=?", new String[]{location});
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    public static void setPicture(MediaWithDate m, Bitmap p) {
        Log.d(TAG, "Setting new picture for " + m.getTitle());
        try {
            getInstance().updateMedia(
                    m.getLocation(),
                    TomahawkSQLiteHelper.mediaColumn.MEDIA_PICTURE,
                    p);
        } catch (SQLiteFullException e) {
            Log.d(TAG, "SQLiteFullException while setting picture");
        }
        m.setPictureParsed(true);
    }

    public boolean isMediaDirComplete(String path) {
        Cursor cursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_MEDIADIRS,
                new String[]{TomahawkSQLiteHelper.MEDIADIRS_PATH},
                TomahawkSQLiteHelper.MEDIADIRS_PATH + " LIKE ? || '_%'",
                new String[]{path}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return !exists;
    }

    public boolean isMediaDirWhiteListed(String path) {
        Cursor cursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_MEDIADIRS,
                new String[]{TomahawkSQLiteHelper.MEDIADIRS_PATH},
                TomahawkSQLiteHelper.MEDIADIRS_PATH + "= ? AND "
                        + TomahawkSQLiteHelper.MEDIADIRS_BLACKLISTED + "= ?",
                new String[]{path, String.valueOf(FALSE)}, null, null, null);
        boolean isWhitelisted = cursor.moveToFirst();
        cursor.close();
        if (!isWhitelisted) {
            cursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_MEDIADIRS,
                    new String[]{TomahawkSQLiteHelper.MEDIADIRS_PATH},
                    "? LIKE " + TomahawkSQLiteHelper.MEDIADIRS_PATH + " || '%' AND "
                            + TomahawkSQLiteHelper.MEDIADIRS_BLACKLISTED + "= ?",
                    new String[]{path, String.valueOf(FALSE)}, null, null, null);
            isWhitelisted = cursor.moveToFirst();
            if (isWhitelisted) {
                List<String> paths = new ArrayList<>();
                while (!cursor.isAfterLast()) {
                    paths.add(cursor.getString(0));
                    cursor.moveToNext();
                }
                cursor.close();
                int wlDrillDownLevel = getMaxBackslashCount(paths);
                cursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_MEDIADIRS,
                        new String[]{TomahawkSQLiteHelper.MEDIADIRS_PATH},
                        "? LIKE " + TomahawkSQLiteHelper.MEDIADIRS_PATH + " || '%' AND "
                                + TomahawkSQLiteHelper.MEDIADIRS_BLACKLISTED + "= ?",
                        new String[]{path, String.valueOf(TRUE)}, null, null, null);
                if (!cursor.moveToFirst()) {
                    isWhitelisted = true;
                } else {
                    paths = new ArrayList<>();
                    while (!cursor.isAfterLast()) {
                        paths.add(cursor.getString(0));
                        cursor.moveToNext();
                    }
                    cursor.close();
                    int blDrillDownLevel = getMaxBackslashCount(paths);
                    isWhitelisted = wlDrillDownLevel > blDrillDownLevel;
                }
            }
            cursor.close();
        }
        return isWhitelisted;
    }

    private static int getBackslashCount(String string) {
        int count = 0;
        char[] pathChars = string.toCharArray();
        for (int i = 0; i < pathChars.length; i++) {
            if (pathChars[i] == '/') {
                count++;
            }
        }
        return count;
    }

    private static int getMaxBackslashCount(List<String> strings) {
        int maxCount = 0;
        for (int i = 0; i < strings.size(); i++) {
            maxCount = Math.max(maxCount, getBackslashCount(strings.get(i)));
        }
        return maxCount;
    }

    public void addMediaDir(String path) {
        Log.d(TAG, "Adding mediaDir: " + path);
        mDatabase.beginTransaction();
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_MEDIADIRS,
                TomahawkSQLiteHelper.MEDIADIRS_PATH + " LIKE ? || '%'", new String[]{path});
        Log.d(TAG, "Removed mediaDir from white/blacklist: " + path);
        if (!isMediaDirWhiteListed(path)) {
            ContentValues values = new ContentValues();
            values.put(TomahawkSQLiteHelper.MEDIADIRS_PATH, path);
            values.put(TomahawkSQLiteHelper.MEDIADIRS_BLACKLISTED, FALSE);
            mDatabase.insert(TomahawkSQLiteHelper.TABLE_MEDIADIRS, null, values);
            Log.d(TAG, "Added mediaDir to whitelist: " + path);
        }
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    public void removeMediaDir(String path) {
        Log.d(TAG, "Removing mediaDir: " + path);
        mDatabase.beginTransaction();
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_MEDIADIRS,
                TomahawkSQLiteHelper.MEDIADIRS_PATH + " LIKE ? || '%'", new String[]{path});
        Log.d(TAG, "Removed mediaDir from white/blacklist: " + path);
        if (isMediaDirWhiteListed(path)) {
            ContentValues values = new ContentValues();
            values.put(TomahawkSQLiteHelper.MEDIADIRS_PATH, path);
            values.put(TomahawkSQLiteHelper.MEDIADIRS_BLACKLISTED, TRUE);
            mDatabase.insert(TomahawkSQLiteHelper.TABLE_MEDIADIRS, null, values);
            Log.d(TAG, "Added mediaDir to blacklist: " + path);
        }
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    public List<String> getMediaDirs(boolean blacklisted) {
        Cursor cursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_MEDIADIRS,
                new String[]{TomahawkSQLiteHelper.MEDIADIRS_PATH},
                TomahawkSQLiteHelper.MEDIADIRS_BLACKLISTED + "= ?",
                new String[]{String.valueOf(blacklisted ? TRUE : FALSE)},
                null, null, null);
        cursor.moveToFirst();
        ArrayList<String> dirs = new ArrayList<String>();
        while (!cursor.isAfterLast()) {
            dirs.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return dirs;
    }
}
