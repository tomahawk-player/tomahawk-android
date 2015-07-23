/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.ScriptAccount;
import org.tomahawk.libtomahawk.resolver.ScriptJob;
import org.tomahawk.libtomahawk.resolver.ScriptObject;
import org.tomahawk.libtomahawk.resolver.ScriptPlugin;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverCollectionMetaData;
import org.tomahawk.libtomahawk.utils.ADeferredObject;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.database.Cursor;
import android.util.Log;
import android.widget.ImageView;

/**
 * This class represents a Collection which contains tracks/albums/artists retrieved by a
 * ScriptResolver.
 */
public class ScriptResolverCollection extends Collection implements ScriptPlugin {

    private final static String TAG = ScriptResolverCollection.class.getSimpleName();

    private ScriptObject mScriptObject;

    private ScriptAccount mScriptAccount;

    private ScriptResolverCollectionMetaData mMetaData;

    public ScriptResolverCollection(ScriptObject object, ScriptAccount account) {
        super(account.getScriptResolver().getId(), account.getName());

        mScriptObject = object;
        mScriptAccount = account;
    }

    public Deferred<ScriptResolverCollectionMetaData, Throwable, Void> getMetaData() {
        final Deferred<ScriptResolverCollectionMetaData, Throwable, Void> deferred
                = new ADeferredObject<>();
        if (mMetaData == null) {
            ScriptJob.start(mScriptObject, "settings",
                    new ScriptJob.ResultsCallback<ScriptResolverCollectionMetaData>(
                            ScriptResolverCollectionMetaData.class) {
                        @Override
                        public void onReportResults(ScriptResolverCollectionMetaData results) {
                            mMetaData = results;
                            deferred.resolve(results);
                        }
                    }, new ScriptJob.FailureCallback() {
                        @Override
                        public void onReportFailure(String errormessage) {
                            deferred.reject(new Throwable(errormessage));
                        }
                    });
        } else {
            deferred.resolve(mMetaData);
        }
        return deferred;
    }

    @Override
    public ScriptObject getScriptObject() {
        return mScriptObject;
    }

    @Override
    public ScriptAccount getScriptAccount() {
        return mScriptAccount;
    }

    @Override
    public void start(ScriptJob job) {
        mScriptAccount.startJob(job);
    }

    @Override
    public void loadIcon(final ImageView imageView, final boolean grayOut) {
        getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
            @Override
            public void onDone(ScriptResolverCollectionMetaData result) {
                String completeIconPath = mScriptAccount.getPath() + "/content/" + result.iconfile;
                TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                        completeIconPath);
            }
        });
    }

    @Override
    public Promise<CollectionCursor<Query>, Throwable, Void> getQueries(final int sortMode) {
        final Deferred<CollectionCursor<Query>, Throwable, Void> deferred = new ADeferredObject<>();
        getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
            @Override
            public void onDone(ScriptResolverCollectionMetaData result) {
                String[] orderBy;
                switch (sortMode) {
                    case SORT_ALPHA:
                        orderBy = new String[]{CollectionDb.TRACKS_TRACK};
                        break;
                    case SORT_ARTIST_ALPHA:
                        orderBy = new String[]{CollectionDb.ARTISTS_ARTIST};
                        break;
                    case SORT_LAST_MODIFIED:
                        orderBy = new String[]{CollectionDb.TRACKS_TRACK};    //TODO
                        break;
                    default:
                        Log.e(TAG, "getQueries - sortMode not supported!");
                        return;
                }
                Cursor cursor =
                        CollectionDbManager.get().getCollectionDb(result.id).tracks(null, orderBy);
                CollectionCursor<Query> collectionCursor = new CollectionCursor<>(cursor,
                        Query.class, mScriptAccount.getScriptResolver());
                deferred.resolve(collectionCursor);
            }
        });
        return deferred;
    }

    @Override
    public Promise<CollectionCursor<Artist>, Throwable, Void> getArtists(final int sortMode) {
        final Deferred<CollectionCursor<Artist>, Throwable, Void> deferred
                = new ADeferredObject<>();
        getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
            @Override
            public void onDone(ScriptResolverCollectionMetaData result) {
                String[] orderBy;
                switch (sortMode) {
                    case SORT_ALPHA:
                        orderBy = new String[]{CollectionDb.ARTISTS_ARTIST};
                        break;
                    case SORT_LAST_MODIFIED:
                        orderBy = new String[]{CollectionDb.ARTISTS_ARTIST};   //TODO
                        break;
                    default:
                        Log.e(TAG, "getArtists - sortMode not supported!");
                        return;
                }
                Cursor cursor =
                        CollectionDbManager.get().getCollectionDb(result.id).artists(orderBy);
                CollectionCursor<Artist> collectionCursor = new CollectionCursor<>(cursor,
                        Artist.class, mScriptAccount.getScriptResolver());
                deferred.resolve(collectionCursor);
            }
        });
        return deferred;
    }

    @Override
    public Promise<CollectionCursor<Artist>, Throwable, Void> getAlbumArtists(final int sortMode) {
        final Deferred<CollectionCursor<Artist>, Throwable, Void> deferred
                = new ADeferredObject<>();
        getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
            @Override
            public void onDone(ScriptResolverCollectionMetaData result) {
                String[] orderBy;
                switch (sortMode) {
                    case SORT_ALPHA:
                        orderBy = new String[]{CollectionDb.ARTISTS_ARTIST};
                        break;
                    case SORT_LAST_MODIFIED:
                        orderBy = new String[]{CollectionDb.ARTISTS_ARTIST};    //TODO
                        break;
                    default:
                        Log.e(TAG, "getAlbumArtists - sortMode not supported!");
                        return;
                }
                Cursor cursor =
                        CollectionDbManager.get().getCollectionDb(result.id).albumArtists(orderBy);
                CollectionCursor<Artist> collectionCursor = new CollectionCursor<>(cursor,
                        Artist.class, mScriptAccount.getScriptResolver());
                deferred.resolve(collectionCursor);
            }
        });
        return deferred;
    }

    @Override
    public Promise<CollectionCursor<Album>, Throwable, Void> getAlbums(final int sortMode) {
        final Deferred<CollectionCursor<Album>, Throwable, Void> deferred = new ADeferredObject<>();
        getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
            @Override
            public void onDone(ScriptResolverCollectionMetaData result) {
                String[] orderBy;
                switch (sortMode) {
                    case SORT_ALPHA:
                        orderBy = new String[]{CollectionDb.ALBUMS_ALBUM};
                        break;
                    case SORT_ARTIST_ALPHA:
                        orderBy = new String[]{CollectionDb.ARTISTS_ARTIST};
                        break;
                    case SORT_LAST_MODIFIED:
                        orderBy = new String[]{CollectionDb.ALBUMS_ALBUM};    //TODO
                        break;
                    default:
                        Log.e(TAG, "getAlbums - sortMode not supported!");
                        return;
                }
                Cursor cursor =
                        CollectionDbManager.get().getCollectionDb(result.id).albums(orderBy);
                CollectionCursor<Album> collectionCursor = new CollectionCursor<>(cursor,
                        Album.class, mScriptAccount.getScriptResolver());
                deferred.resolve(collectionCursor);
            }
        });
        return deferred;
    }

    @Override
    public Promise<CollectionCursor<Album>, Throwable, Void> getArtistAlbums(final Artist artist) {
        final Deferred<CollectionCursor<Album>, Throwable, Void> deferred = new ADeferredObject<>();
        getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
            @Override
            public void onDone(ScriptResolverCollectionMetaData result) {
                Cursor cursor = CollectionDbManager.get().getCollectionDb(result.id)
                        .artistAlbums(artist.getName(), "");
                CollectionCursor<Album> collectionCursor = new CollectionCursor<>(cursor,
                        Album.class, mScriptAccount.getScriptResolver());
                deferred.resolve(collectionCursor);
            }
        });
        return deferred;
    }

    @Override
    public Promise<CollectionCursor<Query>, Throwable, Void> getAlbumTracks(final Album album) {
        final Deferred<CollectionCursor<Query>, Throwable, Void> deferred = new ADeferredObject<>();
        getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
            @Override
            public void onDone(ScriptResolverCollectionMetaData result) {
                Cursor cursor = CollectionDbManager.get().getCollectionDb(result.id)
                        .albumTracks(album.getName(), album.getArtist().getName(), "");
                CollectionCursor<Query> collectionCursor = new CollectionCursor<>(cursor,
                        Query.class, mScriptAccount.getScriptResolver());
                deferred.resolve(collectionCursor);
            }
        });
        return deferred;
    }
}
