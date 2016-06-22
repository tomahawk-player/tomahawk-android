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
import org.tomahawk.libtomahawk.collection.PlaylistComparator;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.StationPlaylist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.QueryParams;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.GsonHelper;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.IdGenerator;
import org.tomahawk.tomahawk_android.utils.MediaWrapper;
import org.videolan.libvlc.util.AndroidUtil;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * This class provides a way of storing user created {@link org.tomahawk.libtomahawk.collection.Playlist}s
 * in the database
 */
public class DatabaseHelper {

    private static final String TAG = DatabaseHelper.class.getSimpleName();

    private static final String LOVEDITEMS_PLAYLIST_ID = "loveditems_playlist_id";

    public static final int FALSE = 0;

    public static final int TRUE = 1;

    public static final int CHUNK_SIZE = 50;

    private static class Holder {

        private static final DatabaseHelper instance = new DatabaseHelper();

    }

    public static class PlaylistsUpdatedEvent {

        public String mPlaylistId;
    }

    // Database fields
    private final SQLiteDatabase mDatabase;

    private DatabaseHelper() {
        TomahawkSQLiteHelper dbHelper = new TomahawkSQLiteHelper(TomahawkApp.getContext());
        dbHelper.close();
        mDatabase = dbHelper.getWritableDatabase();
    }

    public static DatabaseHelper get() {
        return Holder.instance;
    }

    /**
     * Store the given {@link Playlist}
     *
     * @param playlist       the given {@link Playlist}
     * @param reverseEntries set to true, if the order of the entries should be reversed before
     *                       storing in the database
     */
    public void storePlaylist(Playlist playlist, boolean reverseEntries) {
        storePlaylist(playlist.getId(), playlist, reverseEntries);
    }

    /**
     * Store the given {@link Playlist}
     *
     * @param playlist       the given {@link Playlist}
     * @param reverseEntries set to true, if the order of the entries should be reversed before
     *                       storing in the database
     */
    public void storeLovedItemsPlaylist(Playlist playlist, boolean reverseEntries) {
        storePlaylist(LOVEDITEMS_PLAYLIST_ID, playlist, reverseEntries);
    }

    /**
     * Store the given {@link Playlist}
     *
     * @param playlistId     the id under which to store the given {@link Playlist}
     * @param playlist       the given {@link Playlist}
     * @param reverseEntries set to true, if the order of the entries should be reversed before
     *                       storing in the database
     */
    private void storePlaylist(final String playlistId, final Playlist playlist,
            final boolean reverseEntries) {
        List<PlaylistEntry> entries = playlist.getEntries();

        ContentValues values = new ContentValues();
        values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME, playlist.getName());
        if (playlist.isFilled()) {
            values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION,
                    playlist.getCurrentRevision());
        }
        values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID, playlistId);
        values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_HATCHETID,
                playlist.getHatchetId());
        values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_TRACKCOUNT, entries.size());

        mDatabase.beginTransaction();
        mDatabase.insertWithOnConflict(TomahawkSQLiteHelper.TABLE_PLAYLISTS, null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE);
        // Delete every already associated Track entry
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = ?",
                new String[]{playlistId});

        // Store every single Track in the database and store the relationship
        // by storing the playlists's id with it
        for (int i = 0; i < entries.size(); i++) {
            PlaylistEntry entry;
            if (reverseEntries) {
                entry = entries.get(entries.size() - 1 - i);
            } else {
                entry = entries.get(i);
            }
            values.clear();
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID, playlistId);
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
        PlaylistsUpdatedEvent event = new PlaylistsUpdatedEvent();
        event.mPlaylistId = playlistId;
        EventBus.getDefault().post(event);
    }

    /**
     * Rename the given {@link Playlist}
     *
     * @param playlist the given {@link Playlist}
     * @param newName  the new playlist name
     */
    public void renamePlaylist(final Playlist playlist, final String newName) {
        if (playlist != null) {
            String topArtistsString = "";
            for (String s : playlist.getTopArtistNames()) {
                topArtistsString += s + "\t\t";
            }

            ContentValues values = new ContentValues();
            values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME, newName);
            values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID, playlist.getId());
            values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME, playlist.getName());
            if (playlist.isFilled()) {
                values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION,
                        playlist.getCurrentRevision());
            }
            values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_HATCHETID,
                    playlist.getHatchetId());
            values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_TOPARTISTS,
                    topArtistsString);

            mDatabase.beginTransaction();
            mDatabase.insertWithOnConflict(TomahawkSQLiteHelper.TABLE_PLAYLISTS, null,
                    values, SQLiteDatabase.CONFLICT_REPLACE);
            mDatabase.setTransactionSuccessful();
            mDatabase.endTransaction();
            PlaylistsUpdatedEvent event = new PlaylistsUpdatedEvent();
            event.mPlaylistId = playlist.getId();
            EventBus.getDefault().post(event);
        } else {
            Log.e(TAG, "renamePlaylist: playlist is null");
        }
    }

    /**
     * Update the given {@link Playlist}
     *
     * @param playlist the given {@link Playlist}
     */
    public void updatePlaylist(final Playlist playlist) {
        if (playlist != null) {
            String topArtistsString = "";
            for (String s : playlist.getTopArtistNames()) {
                topArtistsString += s + "\t\t";
            }

            ContentValues values = new ContentValues();
            values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID, playlist.getId());
            values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME, playlist.getName());
            if (playlist.isFilled()) {
                values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION,
                        playlist.getCurrentRevision());
            }
            values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_HATCHETID,
                    playlist.getHatchetId());
            values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_TOPARTISTS,
                    topArtistsString);

            mDatabase.beginTransaction();
            mDatabase.insertWithOnConflict(TomahawkSQLiteHelper.TABLE_PLAYLISTS, null,
                    values, SQLiteDatabase.CONFLICT_REPLACE);
            mDatabase.setTransactionSuccessful();
            mDatabase.endTransaction();
            PlaylistsUpdatedEvent event = new PlaylistsUpdatedEvent();
            event.mPlaylistId = playlist.getId();
            EventBus.getDefault().post(event);
        } else {
            Log.e(TAG, "updatePlaylist: playlist is null");
        }
    }

    /**
     * Update the given Playlist's hatchet id
     *
     * @param playlistId the id of the playlist to update
     * @param hatchetId  the new hatchet id to set
     */
    public void updatePlaylistHatchetId(final String playlistId,
            final String hatchetId) {
        Playlist playlist = getEmptyPlaylist(playlistId);
        if (playlist != null) {
            String topArtistsString = "";
            if (playlist.getTopArtistNames() != null) {
                for (String s : playlist.getTopArtistNames()) {
                    topArtistsString += s + "\t\t";
                }
            }

            ContentValues values = new ContentValues();
            values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID, playlist.getId());
            values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME, playlist.getName());
            if (playlist.isFilled()) {
                values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION,
                        playlist.getCurrentRevision());
            }
            values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_HATCHETID, hatchetId);
            values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_TOPARTISTS,
                    topArtistsString);

            mDatabase.beginTransaction();
            mDatabase.insertWithOnConflict(TomahawkSQLiteHelper.TABLE_PLAYLISTS, null,
                    values, SQLiteDatabase.CONFLICT_REPLACE);
            mDatabase.setTransactionSuccessful();
            mDatabase.endTransaction();
            PlaylistsUpdatedEvent event = new PlaylistsUpdatedEvent();
            event.mPlaylistId = playlist.getId();
            EventBus.getDefault().post(event);
        } else {
            Log.e(TAG, "updatePlaylistHatchetId: playlist is null, id: " + playlistId);
        }
    }

    /**
     * @return every stored {@link org.tomahawk.libtomahawk.collection.Playlist} in the database
     */
    public List<Playlist> getPlaylists() {
        final List<Playlist> playListList = new ArrayList<>();
        String[] columns = new String[]{TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID};

        Cursor playlistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                columns, TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " != ?",
                new String[]{LOVEDITEMS_PLAYLIST_ID}, null, null, null);
        playlistsCursor.moveToFirst();
        while (!playlistsCursor.isAfterLast()) {
            Playlist playlist = getEmptyPlaylist(playlistsCursor.getString(0));
            if (playlist != null) {
                playListList.add(playlist);
            }
            playlistsCursor.moveToNext();
        }
        playlistsCursor.close();
        Collections.sort(playListList, new PlaylistComparator());
        return playListList;
    }

    /**
     * @param playlistId the id by which to get the correct {@link org.tomahawk.libtomahawk.collection.Playlist}
     * @return the playlist's hatchet id, null if playlist not found
     */
    public String getPlaylistHatchetId(String playlistId) {
        if (playlistId == null) {
            return null;
        }
        String[] columns = new String[]{TomahawkSQLiteHelper.PLAYLISTS_COLUMN_HATCHETID};
        Cursor playlistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                columns, TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                new String[]{playlistId}, null, null, null);
        String hatchetId = null;
        if (playlistsCursor.moveToFirst()) {
            hatchetId = playlistsCursor.getString(0);
        }
        playlistsCursor.close();
        return hatchetId;
    }

    /**
     * @param hatchetId the id by which to get the correct {@link org.tomahawk.libtomahawk.collection.Playlist}
     * @return the playlist's local id, null if playlist not found
     */
    public String getPlaylistLocalId(String hatchetId) {
        String[] columns = new String[]{TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID};

        Cursor playlistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                columns, TomahawkSQLiteHelper.PLAYLISTS_COLUMN_HATCHETID + " = ?",
                new String[]{hatchetId}, null, null, null);
        String localId = null;
        if (playlistsCursor.moveToFirst()) {
            localId = playlistsCursor.getString(0);
        }
        playlistsCursor.close();
        return localId;
    }

    /**
     * @param playlistId the id by which to get the correct {@link org.tomahawk.libtomahawk.collection.Playlist}
     * @return the playlist's name, null if playlist not found
     */
    public String getPlaylistName(String playlistId) {
        String[] columns = new String[]{TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME};

        Cursor playlistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                columns, TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                new String[]{playlistId}, null, null, null);
        String name = null;
        if (playlistsCursor.moveToFirst()) {
            name = playlistsCursor.getString(0);
        }
        playlistsCursor.close();
        return name;
    }

    /**
     * @param playlistId the id by which to get the correct {@link org.tomahawk.libtomahawk.collection.Playlist}
     * @return the stored {@link org.tomahawk.libtomahawk.collection.Playlist} with playlistId as
     * its id
     */
    public Playlist getEmptyPlaylist(String playlistId) {
        String[] columns = new String[]{TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_HATCHETID,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_TOPARTISTS};

        Cursor playlistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                columns, TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                new String[]{playlistId}, null, null, null);
        if (playlistsCursor.moveToFirst()) {
            Playlist playlist = Playlist.get(playlistId);
            playlist.setName(playlistsCursor.getString(0));
            playlist.setCurrentRevision(playlistsCursor.getString(1));
            playlist.setHatchetId(playlistsCursor.getString(2));
            String rawTopArtistsString = playlistsCursor.getString(3);
            if (rawTopArtistsString != null && rawTopArtistsString.length() > 0) {
                playlist.setTopArtistNames(rawTopArtistsString.split("\t\t"));
            }
            playlistsCursor.close();
            playlist.setCount(getPlaylistTrackCount(playlistId));
            return playlist;
        }
        playlistsCursor.close();
        return null;
    }

    public Playlist getLovedItemsPlaylist() {
        return getPlaylist(LOVEDITEMS_PLAYLIST_ID, true);
    }

    /**
     * @param playlistId the id by which to get the correct {@link org.tomahawk.libtomahawk.collection.Playlist}
     * @return the stored {@link org.tomahawk.libtomahawk.collection.Playlist} with playlistId as
     * its id
     */
    public Playlist getPlaylist(String playlistId) {
        return getPlaylist(playlistId, false);
    }

    /**
     * @param playlistId the id by which to get the correct {@link org.tomahawk.libtomahawk.collection.Playlist}
     * @return the stored {@link org.tomahawk.libtomahawk.collection.Playlist} with playlistId as
     * its id
     */
    private Playlist getPlaylist(String playlistId, boolean reverseEntries) {
        String[] columns = new String[]{TomahawkSQLiteHelper.PLAYLISTS_COLUMN_NAME,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_CURRENTREVISION,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_HATCHETID,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_TOPARTISTS};

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
            ArrayList<PlaylistEntry> entries = new ArrayList<>();
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
                    entryId = IdGenerator.getLifetimeUniqueStringId();
                }
                PlaylistEntry entry = PlaylistEntry.get(playlistId, query, entryId);
                entries.add(entry);
                tracksCursor.moveToNext();
            }
            Playlist playlist = Playlist.fromEntryList(playlistId,
                    playlistsCursor.getString(0), playlistsCursor.getString(1), entries);
            playlist.setHatchetId(playlistsCursor.getString(2));
            playlist.setFilled(true);
            tracksCursor.close();
            String rawTopArtistsString = playlistsCursor.getString(3);
            if (rawTopArtistsString != null && rawTopArtistsString.length() > 0) {
                playlist.setTopArtistNames(rawTopArtistsString.split("\t\t"));
            }
            playlistsCursor.close();
            playlist.setCount(getPlaylistTrackCount(playlistId));
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
    public long getPlaylistTrackCount(String playlistId) {
        long trackCount = -1;
        String[] columns = new String[]{TomahawkSQLiteHelper.PLAYLISTS_COLUMN_TRACKCOUNT};
        Cursor playlistsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                columns, TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                new String[]{playlistId}, null, null, null);
        if (playlistsCursor.moveToFirst()) {
            if (playlistsCursor.isNull(0)) {
                // if no trackcount is stored, we calculate it and store it
                trackCount = DatabaseUtils.queryNumEntries(mDatabase,
                        TomahawkSQLiteHelper.TABLE_TRACKS,
                        TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = ?",
                        new String[]{playlistId});
                mDatabase.beginTransaction();
                ContentValues values = new ContentValues();
                values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_TRACKCOUNT, trackCount);
                mDatabase.update(TomahawkSQLiteHelper.TABLE_PLAYLISTS, values,
                        TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                        new String[]{playlistId});
                mDatabase.setTransactionSuccessful();
                mDatabase.endTransaction();
            } else {
                trackCount = playlistsCursor.getLong(0);
            }
        }
        playlistsCursor.close();
        return trackCount;
    }

    /**
     * Delete the {@link org.tomahawk.libtomahawk.collection.Playlist} with the given id
     *
     * @param playlistId String containing the id of the {@link org.tomahawk.libtomahawk.collection.Playlist}
     *                   to be deleted
     */
    public void deletePlaylist(final String playlistId) {
        mDatabase.beginTransaction();
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = ?",
                new String[]{playlistId});
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_PLAYLISTS,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                new String[]{playlistId});
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
        PlaylistsUpdatedEvent event = new PlaylistsUpdatedEvent();
        event.mPlaylistId = playlistId;
        EventBus.getDefault().post(event);
    }

    /**
     * Delete the {@link org.tomahawk.libtomahawk.collection.PlaylistEntry} with the given key in
     * the {@link org.tomahawk.libtomahawk.collection.Playlist} with the given playlistId
     */
    public void deleteEntryInPlaylist(final String playlistId, final String entryId) {
        long trackCount = getPlaylistTrackCount(playlistId);
        mDatabase.beginTransaction();
        trackCount -= mDatabase.delete(TomahawkSQLiteHelper.TABLE_TRACKS,
                TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTID + " = ? AND "
                        + TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTENTRYID
                        + " = ?", new String[]{playlistId, entryId});
        ContentValues values = new ContentValues();
        values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_TRACKCOUNT, trackCount);
        mDatabase.update(TomahawkSQLiteHelper.TABLE_PLAYLISTS, values,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                new String[]{playlistId});
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
        PlaylistsUpdatedEvent event = new PlaylistsUpdatedEvent();
        event.mPlaylistId = playlistId;
        EventBus.getDefault().post(event);
    }

    /**
     * Add the given {@link ArrayList} of {@link Track}s to the {@link
     * org.tomahawk.libtomahawk.collection.Playlist} with the given playlistId
     */
    public void addQueriesToPlaylist(final String playlistId, final ArrayList<Query> queries) {
        long trackCount = getPlaylistTrackCount(playlistId);

        mDatabase.beginTransaction();
        // Store every single Track in the database and store the relationship
        // by storing the playlists's id with it
        for (Query query : queries) {
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
                    trackCount);
            if (query.isFetchedViaHatchet()) {
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                        TRUE);
            } else {
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                        FALSE);
            }
            if (mDatabase.insert(TomahawkSQLiteHelper.TABLE_TRACKS, null, values) != -1) {
                trackCount++;
            }
        }
        ContentValues values = new ContentValues();
        values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_TRACKCOUNT, trackCount);
        mDatabase.update(TomahawkSQLiteHelper.TABLE_PLAYLISTS, values,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                new String[]{playlistId});
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
        PlaylistsUpdatedEvent event = new PlaylistsUpdatedEvent();
        event.mPlaylistId = playlistId;
        EventBus.getDefault().post(event);
    }

    /**
     * Add the given {@link ArrayList} of {@link Track}s to the {@link
     * org.tomahawk.libtomahawk.collection.Playlist} with the given playlistId
     */
    public void addEntriesToPlaylist(final String playlistId,
            final ArrayList<PlaylistEntry> entries) {
        long trackCount = getPlaylistTrackCount(playlistId);

        mDatabase.beginTransaction();
        // Store every single Track in the database and store the relationship
        // by storing the playlists's id with it
        for (PlaylistEntry entry : entries) {
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
                    trackCount);
            if (entry.getQuery().isFetchedViaHatchet()) {
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                        TRUE);
            } else {
                values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_ISFETCHEDVIAHATCHET,
                        FALSE);
            }
            values.put(TomahawkSQLiteHelper.TRACKS_COLUMN_PLAYLISTENTRYID,
                    entry.getId());
            if (mDatabase.insert(TomahawkSQLiteHelper.TABLE_TRACKS, null, values) != -1) {
                trackCount++;
            }
        }
        ContentValues values = new ContentValues();
        values.put(TomahawkSQLiteHelper.PLAYLISTS_COLUMN_TRACKCOUNT, trackCount);
        mDatabase.update(TomahawkSQLiteHelper.TABLE_PLAYLISTS, values,
                TomahawkSQLiteHelper.PLAYLISTS_COLUMN_ID + " = ?",
                new String[]{playlistId});
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
        PlaylistsUpdatedEvent event = new PlaylistsUpdatedEvent();
        event.mPlaylistId = playlistId;
        EventBus.getDefault().post(event);
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
     * Store the given query as a lovedItem, if isLoved is true. Otherwise remove(unlove) the
     * query.
     */
    public void setLovedItem(Query query, boolean isLoved) {
        if (isLoved) {
            ArrayList<Query> queries = new ArrayList<>();
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
            PlaylistsUpdatedEvent event = new PlaylistsUpdatedEvent();
            event.mPlaylistId = LOVEDITEMS_PLAYLIST_ID;
            EventBus.getDefault().post(event);
        }
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
            String paramsJsonString = GsonHelper.get().toJson(opToLog.getQueryParams());
            values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_PARAMS, paramsJsonString);
        }
        mDatabase.insert(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG, null, values);
        long logCount = getLoggedOpsCount();
        values = new ContentValues();
        values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOGINFO_COLUMN_LOGCOUNT, logCount + 1);
        mDatabase.update(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOGINFO, values, null, null);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * Remove the operation with the given id from the InfoSystem-OpLog table
     *
     * @param loggedOp the opLog that should be removed from the InfoSystem-OpLog table
     */
    public void removeOpFromInfoSystemOpLog(InfoRequestData loggedOp) {
        mDatabase.beginTransaction();
        int deletedLogs = mDatabase.delete(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG,
                TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_ID + " = ?",
                new String[]{String.valueOf(loggedOp.getLoggedOpId())});
        long logCount = getLoggedOpsCount();
        ContentValues values = new ContentValues();
        values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOGINFO_COLUMN_LOGCOUNT,
                logCount - deletedLogs);
        mDatabase.update(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOGINFO, values, null, null);
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
        int deletedLogs = 0;
        for (InfoRequestData loggedOp : loggedOps) {
            deletedLogs = mDatabase.delete(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG,
                    TomahawkSQLiteHelper.INFOSYSTEMOPLOG_COLUMN_ID + " = ?",
                    new String[]{String.valueOf(loggedOp.getLoggedOpId())});
        }
        long logCount = getLoggedOpsCount();
        ContentValues values = new ContentValues();
        values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOGINFO_COLUMN_LOGCOUNT,
                logCount - deletedLogs);
        mDatabase.update(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOGINFO, values, null, null);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
    }

    /**
     * @return an InfoRequestData object that contains all data that should be delivered to the API
     */
    public List<InfoRequestData> getLoggedOps() {
        List<InfoRequestData> loggedOps = new ArrayList<>();
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
            String requestId = IdGenerator.getSessionUniqueStringId();
            String paramJsonString = opLogCursor.getString(4);
            QueryParams params = null;
            if (paramJsonString != null) {
                params = GsonHelper.get().fromJson(paramJsonString, QueryParams.class);
            }
            InfoRequestData infoRequestData = new InfoRequestData(requestId, opLogCursor.getInt(1),
                    params, opLogCursor.getInt(0), opLogCursor.getInt(2), opLogCursor.getString(3),
                    true);
            loggedOps.add(infoRequestData);
            opLogCursor.moveToNext();
        }
        opLogCursor.close();
        return loggedOps;
    }

    /**
     * @return the count of all logged ops that should be delivered to the API
     */
    public long getLoggedOpsCount() {
        long logCount;
        String[] columns = new String[]{TomahawkSQLiteHelper.INFOSYSTEMOPLOGINFO_COLUMN_LOGCOUNT};
        Cursor opLogsCursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOGINFO,
                columns, null, null, null, null, null);
        if (!opLogsCursor.moveToFirst() || opLogsCursor.isNull(0)) {
            // if no logcount is stored, we calculate it and store it
            logCount = DatabaseUtils
                    .queryNumEntries(mDatabase, TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOG);
            mDatabase.beginTransaction();
            ContentValues values = new ContentValues();
            values.put(TomahawkSQLiteHelper.INFOSYSTEMOPLOGINFO_COLUMN_LOGCOUNT, logCount);
            mDatabase.update(TomahawkSQLiteHelper.TABLE_INFOSYSTEMOPLOGINFO, values, null,
                    null);
            mDatabase.setTransactionSuccessful();
            mDatabase.endTransaction();
        } else {
            logCount = opLogsCursor.getLong(0);
        }
        opLogsCursor.close();
        return logCount;
    }

    private static void safePut(ContentValues values, String key, String value) {
        if (value == null) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }

    /**
     * Add the given {@link MediaWrapper}s to the database.
     *
     * @param mws which you like to add to the database
     */
    public synchronized void addMedias(List<MediaWrapper> mws) {
        ContentValues values = new ContentValues();

        mDatabase.beginTransaction();
        for (MediaWrapper mw : mws) {
            values.put(TomahawkSQLiteHelper.MEDIA_LOCATION, mw.getLocation());
            values.put(TomahawkSQLiteHelper.MEDIA_TIME, mw.getTime());
            values.put(TomahawkSQLiteHelper.MEDIA_LENGTH, mw.getLength());
            values.put(TomahawkSQLiteHelper.MEDIA_TYPE, mw.getType());
            values.put(TomahawkSQLiteHelper.MEDIA_TITLE, mw.getTitle());
            safePut(values, TomahawkSQLiteHelper.MEDIA_ARTIST, mw.getArtist());
            safePut(values, TomahawkSQLiteHelper.MEDIA_GENRE, mw.getGenre());
            safePut(values, TomahawkSQLiteHelper.MEDIA_ALBUM, mw.getAlbum());
            safePut(values, TomahawkSQLiteHelper.MEDIA_ALBUMARTIST, mw.getAlbumArtist());
            values.put(TomahawkSQLiteHelper.MEDIA_WIDTH, mw.getWidth());
            values.put(TomahawkSQLiteHelper.MEDIA_HEIGHT, mw.getHeight());
            values.put(TomahawkSQLiteHelper.MEDIA_ARTWORKURL, mw.getArtworkURL());
            values.put(TomahawkSQLiteHelper.MEDIA_AUDIOTRACK, mw.getAudioTrack());
            values.put(TomahawkSQLiteHelper.MEDIA_SPUTRACK, mw.getSpuTrack());
            values.put(TomahawkSQLiteHelper.MEDIA_TRACKNUMBER, mw.getTrackNumber());
            values.put(TomahawkSQLiteHelper.MEDIA_DISCNUMBER, mw.getDiscNumber());
            values.put(TomahawkSQLiteHelper.MEDIA_LASTMODIFIED, mw.getLastModified());
            mDatabase.replace(TomahawkSQLiteHelper.TABLE_MEDIA, "NULL", values);
        }
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();

    }

    public synchronized HashMap<String, MediaWrapper> getMedias() {
        Cursor cursor;
        HashMap<String, MediaWrapper> medias = new HashMap<>();
        int chunk_count = 0;
        int count;

        do {
            count = 0;
            cursor = mDatabase.rawQuery(String.format(Locale.US,
                    "SELECT %s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s FROM %s LIMIT %d OFFSET %d",
                    TomahawkSQLiteHelper.MEDIA_LOCATION, //0 string
                    TomahawkSQLiteHelper.MEDIA_TIME, //1 long
                    TomahawkSQLiteHelper.MEDIA_LENGTH, //2 long
                    TomahawkSQLiteHelper.MEDIA_TYPE, //3 int
                    TomahawkSQLiteHelper.MEDIA_TITLE, //4 string
                    TomahawkSQLiteHelper.MEDIA_ARTIST, //5 string
                    TomahawkSQLiteHelper.MEDIA_GENRE, //6 string
                    TomahawkSQLiteHelper.MEDIA_ALBUM, //7 string
                    TomahawkSQLiteHelper.MEDIA_ALBUMARTIST, //8 string
                    TomahawkSQLiteHelper.MEDIA_WIDTH, //9 int
                    TomahawkSQLiteHelper.MEDIA_HEIGHT, //10 int
                    TomahawkSQLiteHelper.MEDIA_ARTWORKURL, //11 string
                    TomahawkSQLiteHelper.MEDIA_AUDIOTRACK, //12 int
                    TomahawkSQLiteHelper.MEDIA_SPUTRACK, //13 int
                    TomahawkSQLiteHelper.MEDIA_TRACKNUMBER, // 14 int
                    TomahawkSQLiteHelper.MEDIA_DISCNUMBER, //15 int
                    TomahawkSQLiteHelper.MEDIA_LASTMODIFIED, //16 long
                    TomahawkSQLiteHelper.TABLE_MEDIA,
                    CHUNK_SIZE,
                    chunk_count * CHUNK_SIZE), null);

            if (cursor.moveToFirst()) {
                try {
                    do {
                        final Uri uri = AndroidUtil.LocationToUri(cursor.getString(0));
                        MediaWrapper media = new MediaWrapper(uri,
                                cursor.getLong(1),      // MEDIA_TIME
                                cursor.getLong(2),      // MEDIA_LENGTH
                                cursor.getInt(3),       // MEDIA_TYPE
                                null,                   // MEDIA_PICTURE
                                cursor.getString(4),    // MEDIA_TITLE
                                cursor.getString(5),    // MEDIA_ARTIST
                                cursor.getString(6),    // MEDIA_GENRE
                                cursor.getString(7),    // MEDIA_ALBUM
                                cursor.getString(8),    // MEDIA_ALBUMARTIST
                                cursor.getInt(9),       // MEDIA_WIDTH
                                cursor.getInt(10),       // MEDIA_HEIGHT
                                cursor.getString(11),   // MEDIA_ARTWORKURL
                                cursor.getInt(12),      // MEDIA_AUDIOTRACK
                                cursor.getInt(13),      // MEDIA_SPUTRACK
                                cursor.getInt(14),      // MEDIA_TRACKNUMBER
                                cursor.getInt(15),     // MEDIA_DISCNUMBER
                                cursor.getLong(16));     // MEDIA_LAST_MODIFIED
                        medias.put(media.getUri().toString(), media);

                        count++;
                    } while (cursor.moveToNext());
                } catch (IllegalStateException e) {
                    //Google bug causing IllegalStateException, see
                    //https://code.google.com/p/android/issues/detail?id=32472
                }
            }

            cursor.close();
            chunk_count++;
        } while (count == CHUNK_SIZE);

        return medias;
    }

    public synchronized void removeMedias(Set<String> locations) {
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

    public synchronized void removeAllMedias() {
        mDatabase.beginTransaction();
        try {
            mDatabase.delete(TomahawkSQLiteHelper.TABLE_MEDIA, "1", null);
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }

    public synchronized boolean isMediaDirComplete(String path) {
        Cursor cursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_MEDIADIRS,
                new String[]{TomahawkSQLiteHelper.MEDIADIRS_PATH},
                TomahawkSQLiteHelper.MEDIADIRS_PATH + " LIKE ? || '_%'",
                new String[]{path}, null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return !exists;
    }

    public synchronized boolean isMediaDirWhiteListed(String path) {
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
                int wlDrillDownLevel = getMaxSlashCount(paths);
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
                    int blDrillDownLevel = getMaxSlashCount(paths);
                    isWhitelisted = wlDrillDownLevel > blDrillDownLevel;
                }
            }
            cursor.close();
        }
        return isWhitelisted;
    }

    private static int getSlashCount(String string) {
        int count = 0;
        char[] pathChars = string.toCharArray();
        for (char pathChar : pathChars) {
            if (pathChar == '/') {
                count++;
            }
        }
        return count;
    }

    private static int getMaxSlashCount(List<String> strings) {
        int maxCount = 0;
        for (String string : strings) {
            maxCount = Math.max(maxCount, getSlashCount(string));
        }
        return maxCount;
    }

    public synchronized void addMediaDir(String path) {
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

    public synchronized void removeMediaDir(String path) {
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

    public synchronized List<File> getMediaDirs(boolean blacklisted) {
        Cursor cursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_MEDIADIRS,
                new String[]{TomahawkSQLiteHelper.MEDIADIRS_PATH},
                TomahawkSQLiteHelper.MEDIADIRS_BLACKLISTED + "= ?",
                new String[]{String.valueOf(blacklisted ? TRUE : FALSE)},
                null, null, null);
        cursor.moveToFirst();
        List<File> paths = new ArrayList<>();
        if (!cursor.isAfterLast()) {
            do {
                File dir = new File(cursor.getString(0));
                paths.add(dir);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return paths;
    }

    public synchronized void storeStation(StationPlaylist stationPlaylist) {
        mDatabase.beginTransaction();
        ContentValues values = new ContentValues();
        values.put(TomahawkSQLiteHelper.STATIONS_COLUMN_ID, stationPlaylist.getCacheKey());
        values.put(TomahawkSQLiteHelper.STATIONS_COLUMN_JSON, stationPlaylist.toJson());
        values.put(TomahawkSQLiteHelper.STATIONS_COLUMN_CREATEDTIMESTAMP,
                stationPlaylist.getCreatedTimeStamp());
        values.put(TomahawkSQLiteHelper.STATIONS_COLUMN_PLAYEDTIMESTAMP,
                stationPlaylist.getPlayedTimeStamp());
        mDatabase.insertWithOnConflict(TomahawkSQLiteHelper.TABLE_STATIONS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
        PlaylistsUpdatedEvent event = new PlaylistsUpdatedEvent();
        event.mPlaylistId = stationPlaylist.getId();
        EventBus.getDefault().post(event);
    }

    public synchronized void deleteStation(StationPlaylist stationPlaylist) {
        mDatabase.beginTransaction();
        mDatabase.delete(TomahawkSQLiteHelper.TABLE_STATIONS,
                TomahawkSQLiteHelper.STATIONS_COLUMN_ID + " = ?",
                new String[]{stationPlaylist.getCacheKey()});
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
        PlaylistsUpdatedEvent event = new PlaylistsUpdatedEvent();
        event.mPlaylistId = stationPlaylist.getId();
        EventBus.getDefault().post(event);
    }

    public synchronized List<StationPlaylist> getStations() {
        Cursor cursor = mDatabase.query(TomahawkSQLiteHelper.TABLE_STATIONS,
                new String[]{TomahawkSQLiteHelper.STATIONS_COLUMN_JSON,
                        TomahawkSQLiteHelper.STATIONS_COLUMN_CREATEDTIMESTAMP,
                        TomahawkSQLiteHelper.STATIONS_COLUMN_PLAYEDTIMESTAMP},
                null, null, null, null,
                TomahawkSQLiteHelper.STATIONS_COLUMN_CREATEDTIMESTAMP + " DESC");
        cursor.moveToFirst();
        List<StationPlaylist> stations = new ArrayList<>();
        if (!cursor.isAfterLast()) {
            do {
                String json = cursor.getString(0);
                StationPlaylist station = StationPlaylist.get(json);
                station.setCreatedTimeStamp(cursor.getLong(1));
                station.setPlayedTimeStamp(cursor.getLong(2));
                stations.add(station);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return stations;
    }
}
