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
package org.tomahawk.libtomahawk.collection;

import org.jdeferred.Deferred;
import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;
import org.tomahawk.libtomahawk.database.CollectionDb;
import org.tomahawk.libtomahawk.database.CollectionDbManager;
import org.tomahawk.libtomahawk.resolver.FuzzyIndex;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.resolver.ScriptAccount;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.libtomahawk.utils.ADeferredObject;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;

/**
 * This class represents a Collection which contains tracks/albums/artists which are being stored in
 * a local sqlite db.
 */
public abstract class DbCollection extends Collection {

    private final static String TAG = DbCollection.class.getSimpleName();

    private Set<Query> mWaitingQueries = Collections
            .newSetFromMap(new ConcurrentHashMap<Query, Boolean>());

    private Resolver mResolver;

    private boolean mInitialized;

    public class InitializedEvent {

        String collectionId;
    }

    public DbCollection(Resolver resolver) {
        super(resolver.getId(), resolver.getPrettyName());

        mResolver = resolver;
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public void setInitialized(boolean initialized) {
        mInitialized = initialized;
        getCollectionId().done(new DoneCallback<String>() {
            @Override
            public void onDone(final String collectionId) {
                TomahawkRunnable r = new TomahawkRunnable(
                        TomahawkRunnable.PRIORITY_IS_DATABASEACTION) {
                    @Override
                    public void run() {
                        invokeWaitingJobs();
                    }
                };
                ThreadManager.get().execute(r);
                InitializedEvent event = new InitializedEvent();
                event.collectionId = collectionId;
                EventBus.getDefault().post(event);
            }
        });
    }

    public String getIconBackgroundPath() {
        if (mResolver instanceof ScriptResolver) {
            ScriptAccount account = ((ScriptResolver) mResolver).getScriptAccount();
            return account.getIconBackgroundPath();
        }
        return null;
    }

    public abstract Promise<String, Throwable, Void> getCollectionId();

    public void resolve(final Query query) {
        getCollectionId().done(new DoneCallback<String>() {
            @Override
            public void onDone(final String collectionId) {
                final CollectionDb db = CollectionDbManager.get().getCollectionDb(collectionId);
                if (!mInitialized) {
                    mWaitingQueries.add(query);
                    Log.d(TAG, collectionId + " - Added query to the waiting queue because the "
                            + "FuzzyIndex is still initializing.");
                } else {
                    TomahawkRunnable r = new TomahawkRunnable(
                            TomahawkRunnable.PRIORITY_IS_RESOLVING) {
                        @Override
                        public void run() {
                            List<FuzzyIndex.IndexResult> indexResults =
                                    db.getFuzzyIndex().searchIndex(query);
                            if (indexResults.size() > 0) {
                                String[] ids = new String[indexResults.size()];
                                for (int i = 0; i < indexResults.size(); i++) {
                                    FuzzyIndex.IndexResult indexResult = indexResults.get(i);
                                    ids[i] = String.valueOf(indexResult.id);
                                }
                                CollectionDb.WhereInfo whereInfo = new CollectionDb.WhereInfo();
                                whereInfo.connection = "OR";
                                whereInfo.where.put(CollectionDb.ID, ids);
                                Cursor cursor = db.tracks(whereInfo, null);
                                CollectionCursor<Result> collectionCursor = new CollectionCursor<>(
                                        cursor, Result.class, mResolver, null);
                                ArrayList<Result> results = new ArrayList<>();
                                for (int i = 0; i < collectionCursor.size(); i++) {
                                    results.add(collectionCursor.get(i));
                                }
                                collectionCursor.close();
                                PipeLine.get().reportResults(query, results, mResolver.getId());
                            }
                        }
                    };
                    ThreadManager.get().execute(r, query);
                }
            }
        });
    }

    private synchronized void invokeWaitingJobs() {
        Log.d(TAG, "Resolving " + mWaitingQueries.size() + " waiting queries");
        for (Query query : mWaitingQueries) {
            resolve(query);
        }
        mWaitingQueries.clear();
    }

    @Override
    public Promise<Playlist, Throwable, Void> getQueries(final int sortMode) {
        final Deferred<Playlist, Throwable, Void> deferred = new ADeferredObject<>();
        getCollectionId().done(new DoneCallback<String>() {
            @Override
            public void onDone(final String collectionId) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String[] orderBy;
                        switch (sortMode) {
                            case SORT_ALPHA:
                                orderBy = new String[]{CollectionDb.TRACKS_TRACK};
                                break;
                            case SORT_ARTIST_ALPHA:
                                orderBy = new String[]{CollectionDb.ARTISTS_ARTIST};
                                break;
                            case SORT_LAST_MODIFIED:
                                orderBy = new String[]{CollectionDb.TRACKS_LASTMODIFIED + " DESC"};
                                break;
                            default:
                                Log.e(TAG,
                                        collectionId + " - getQueries - sortMode not supported!");
                                return;
                        }
                        CollectionDb db = CollectionDbManager.get().getCollectionDb(collectionId);
                        String currentRevision = String.valueOf(db.tracksCurrentRevision());
                        Playlist playlist = Playlist.get(
                                collectionId + "_tracks_" + currentRevision + "_" + sortMode);
                        if (playlist.getCurrentRevision().isEmpty()) {
                            Cursor cursor = db.tracks(null, orderBy);
                            if (cursor == null) {
                                deferred.resolve(null);
                                return;
                            }
                            CollectionCursor<PlaylistEntry> collectionCursor
                                    = new CollectionCursor<>(
                                    cursor, PlaylistEntry.class, mResolver, playlist);
                            playlist.setCursor(collectionCursor);
                            playlist.setFilled(true);
                            playlist.setCurrentRevision(currentRevision);
                        }
                        deferred.resolve(playlist);
                    }
                }).start();
            }
        });
        return deferred;
    }

    @Override
    public Promise<CollectionCursor<Artist>, Throwable, Void> getArtists(final int sortMode) {
        final Deferred<CollectionCursor<Artist>, Throwable, Void> deferred
                = new ADeferredObject<>();
        getCollectionId().done(new DoneCallback<String>() {
            @Override
            public void onDone(final String collectionId) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String[] orderBy;
                        switch (sortMode) {
                            case SORT_ALPHA:
                                orderBy = new String[]{
                                        CollectionDb.ARTISTS_ARTIST + " COLLATE NOCASE "};
                                break;
                            case SORT_LAST_MODIFIED:
                                orderBy = new String[]{CollectionDb.ARTISTS_LASTMODIFIED + " DESC"};
                                break;
                            default:
                                Log.e(TAG,
                                        collectionId + " - getArtists - sortMode not supported!");
                                return;
                        }
                        Cursor cursor = CollectionDbManager.get().getCollectionDb(collectionId)
                                .artists(orderBy);
                        CollectionCursor<Artist> collectionCursor =
                                new CollectionCursor<>(cursor, Artist.class, null, null);
                        deferred.resolve(collectionCursor);
                    }
                }).start();
            }
        });
        return deferred;
    }

    @Override
    public Promise<CollectionCursor<Artist>, Throwable, Void> getAlbumArtists(final int sortMode) {
        final Deferred<CollectionCursor<Artist>, Throwable, Void> deferred
                = new ADeferredObject<>();
        getCollectionId().done(new DoneCallback<String>() {
            @Override
            public void onDone(final String collectionId) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String[] orderBy;
                        switch (sortMode) {
                            case SORT_ALPHA:
                                orderBy = new String[]{
                                        CollectionDb.ARTISTS_ARTIST + " COLLATE NOCASE "};
                                break;
                            case SORT_LAST_MODIFIED:
                                orderBy = new String[]{CollectionDb.ARTISTS_LASTMODIFIED + " DESC"};
                                break;
                            default:
                                Log.e(TAG, collectionId
                                        + " - getAlbumArtists - sortMode not supported!");
                                return;
                        }
                        Cursor cursor = CollectionDbManager.get().getCollectionDb(collectionId)
                                .albumArtists(orderBy);
                        CollectionCursor<Artist> collectionCursor =
                                new CollectionCursor<>(cursor, Artist.class, null, null);
                        deferred.resolve(collectionCursor);
                    }
                }).start();
            }
        });
        return deferred;
    }

    @Override
    public Promise<CollectionCursor<Album>, Throwable, Void> getAlbums(final int sortMode) {
        final Deferred<CollectionCursor<Album>, Throwable, Void> deferred = new ADeferredObject<>();
        getCollectionId().done(new DoneCallback<String>() {
            @Override
            public void onDone(final String collectionId) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String[] orderBy;
                        switch (sortMode) {
                            case SORT_ALPHA:
                                orderBy = new String[]{
                                        CollectionDb.ALBUMS_ALBUM + " COLLATE NOCASE "};
                                break;
                            case SORT_ARTIST_ALPHA:
                                orderBy = new String[]{
                                        CollectionDb.ARTISTS_ARTIST + " COLLATE NOCASE "};
                                break;
                            case SORT_LAST_MODIFIED:
                                orderBy = new String[]{CollectionDb.ALBUMS_LASTMODIFIED + " DESC"};
                                break;
                            default:
                                Log.e(TAG, collectionId + " - getAlbums - sortMode not supported!");
                                return;
                        }
                        Cursor cursor = CollectionDbManager.get().getCollectionDb(collectionId)
                                .albums(orderBy);
                        CollectionCursor<Album> collectionCursor =
                                new CollectionCursor<>(cursor, Album.class, null, null);
                        deferred.resolve(collectionCursor);
                    }
                }).start();
            }
        });
        return deferred;
    }

    @Override
    public Promise<CollectionCursor<Album>, Throwable, Void> getArtistAlbums(final Artist artist) {
        final Deferred<CollectionCursor<Album>, Throwable, Void> deferred = new ADeferredObject<>();
        getCollectionId().done(new DoneCallback<String>() {
            @Override
            public void onDone(final String result) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Cursor cursor = CollectionDbManager.get().getCollectionDb(result)
                                .artistAlbums(artist.getName(), "");
                        if (cursor == null) {
                            deferred.resolve(null);
                            return;
                        }
                        CollectionCursor<Album> collectionCursor =
                                new CollectionCursor<>(cursor, Album.class, null, null);
                        deferred.resolve(collectionCursor);
                    }
                }).start();
            }
        });
        return deferred;
    }

    @Override
    public Promise<Playlist, Throwable, Void> getArtistTracks(final Artist artist) {
        final Deferred<Playlist, Throwable, Void> deferred = new ADeferredObject<>();
        getCollectionId().done(new DoneCallback<String>() {
            @Override
            public void onDone(final String collectionId) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        CollectionDb db = CollectionDbManager.get().getCollectionDb(collectionId);
                        String currentRevision =
                                String.valueOf(db.artistCurrentRevision(artist.getName(), ""));
                        Playlist playlist = Playlist.get(
                                collectionId + "_" + artist.getCacheKey() + "_" + currentRevision);
                        if (playlist.getCurrentRevision().isEmpty()) {
                            Cursor cursor = db.artistTracks(artist.getName(), "");
                            if (cursor == null) {
                                deferred.resolve(null);
                                return;
                            }
                            CollectionCursor<PlaylistEntry> collectionCursor
                                    = new CollectionCursor<>(
                                    cursor, PlaylistEntry.class, mResolver, playlist);
                            playlist.setCursor(collectionCursor);
                            playlist.setFilled(true);
                            playlist.setCurrentRevision(currentRevision);
                        }
                        deferred.resolve(playlist);
                    }
                }).start();
            }
        });
        return deferred;
    }

    @Override
    public Promise<Playlist, Throwable, Void> getAlbumTracks(final Album album) {
        final Deferred<Playlist, Throwable, Void> deferred = new ADeferredObject<>();
        getCollectionId().done(new DoneCallback<String>() {
            @Override
            public void onDone(final String collectionId) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        CollectionDb db = CollectionDbManager.get().getCollectionDb(collectionId);
                        String currentRevision = String.valueOf(db.albumCurrentRevision(
                                album.getName(), album.getArtist().getName(), ""));
                        Playlist playlist = Playlist.get(
                                collectionId + "_" + album.getCacheKey() + "_" + currentRevision);
                        if (playlist.getCurrentRevision().isEmpty()) {
                            Cursor cursor = db.albumTracks(
                                    album.getName(), album.getArtist().getName(), "");
                            if (cursor == null) {
                                deferred.resolve(null);
                                return;
                            }
                            CollectionCursor<PlaylistEntry> collectionCursor
                                    = new CollectionCursor<>(
                                    cursor, PlaylistEntry.class, mResolver, playlist);
                            playlist.setCursor(collectionCursor);
                            playlist.setFilled(true);
                            playlist.setCurrentRevision(currentRevision);
                        }
                        deferred.resolve(playlist);
                    }
                }).start();
            }
        });
        return deferred;
    }

    @Override
    public Promise<Integer, Throwable, Void> getAlbumTrackCount(final Album album) {
        final Deferred<Integer, Throwable, Void> deferred = new ADeferredObject<>();
        getCollectionId().done(new DoneCallback<String>() {
            @Override
            public void onDone(final String collectionId) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Cursor cursor = CollectionDbManager.get().getCollectionDb(collectionId)
                                .albumTracks(album.getName(), album.getArtist().getName(), "");
                        if (cursor == null) {
                            deferred.resolve(null);
                            return;
                        }
                        deferred.resolve(cursor.getCount());
                        cursor.close();
                    }
                }).start();
            }
        });
        return deferred;
    }
}
