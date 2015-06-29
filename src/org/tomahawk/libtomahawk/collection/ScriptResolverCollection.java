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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import org.jdeferred.Deferred;
import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.resolver.ScriptAccount;
import org.tomahawk.libtomahawk.resolver.ScriptJob;
import org.tomahawk.libtomahawk.resolver.ScriptObject;
import org.tomahawk.libtomahawk.resolver.ScriptPlugin;
import org.tomahawk.libtomahawk.resolver.ScriptUtils;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverCollectionMetaData;
import org.tomahawk.libtomahawk.utils.ADeferredObject;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.util.Log;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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

        // initialize everything
        getAlbums();
        getQueries();
        getArtists();
    }

    public Deferred<ScriptResolverCollectionMetaData, String, Object> getMetaData() {
        final Deferred<ScriptResolverCollectionMetaData, String, Object> deferred
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
                String completeIconPath = "file:///android_asset/" + mScriptAccount.getPath()
                        + "/content/" + result.iconfile;
                TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                        completeIconPath);
            }
        });
    }

    @Override
    public Promise<Set<Query>, Throwable, Void> getQueries() {
        final Deferred<Set<Query>, Throwable, Void> deferred = new ADeferredObject<>();
        if (mQueries != null) {
            deferred.resolve(mQueries);
        } else {
            getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
                @Override
                public void onDone(ScriptResolverCollectionMetaData result) {
                    HashMap<String, Object> a = new HashMap<>();
                    a.put("id", result.id);
                    final long timeBefore = System.currentTimeMillis();
                    ScriptJob.start(mScriptObject, "tracks", a,
                            new ScriptJob.ResultsArrayCallback() {
                                @Override
                                public void onReportResults(JsonArray results) {
                                    Log.d(TAG,
                                            "Received " + results.size() + " trackResults in " + (
                                                    System.currentTimeMillis() - timeBefore)
                                                    + "ms");
                                    long time = System.currentTimeMillis();
                                    ArrayList<Result> parsedResults = ScriptUtils.parseResultList(
                                            mScriptAccount.getScriptResolver(), results);
                                    Log.d(TAG,
                                            "Parsed " + parsedResults.size() + " trackResults in "
                                                    + (
                                                    System.currentTimeMillis() - time) + "ms");
                                    time = System.currentTimeMillis();
                                    Set<Query> queries = new HashSet<>();
                                    for (Result r : parsedResults) {
                                        Query query = Query.get(r, false);
                                        query.addTrackResult(r, 1f);
                                        queries.add(query);
                                    }
                                    Log.d(TAG, "Converted " + parsedResults.size()
                                            + " trackResults in " + (
                                            System.currentTimeMillis() - time) + "ms");
                                    mQueries = queries;
                                    deferred.resolve(queries);
                                }
                            });
                }
            });
        }
        return deferred;
    }

    @Override
    public Promise<Set<Artist>, Throwable, Void> getArtists() {
        final Deferred<Set<Artist>, Throwable, Void> deferred = new ADeferredObject<>();
        if (mArtists != null) {
            deferred.resolve(mArtists);
        } else {
            getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
                @Override
                public void onDone(ScriptResolverCollectionMetaData result) {
                    HashMap<String, Object> a = new HashMap<>();
                    a.put("id", result.id);
                    final long timeBefore = System.currentTimeMillis();
                    ScriptJob.start(mScriptObject, "artists", a,
                            new ScriptJob.ResultsArrayCallback() {
                                @Override
                                public void onReportResults(JsonArray results) {
                                    Log.d(TAG,
                                            "Received " + results.size() + " artistResults in " + (
                                                    System.currentTimeMillis() - timeBefore)
                                                    + "ms");
                                    long time = System.currentTimeMillis();
                                    Set<Artist> artists = new HashSet<>();
                                    for (JsonElement result : results) {
                                        Artist artist = Artist
                                                .get(ScriptUtils
                                                        .getNodeChildAsText(result, "artist"));
                                        artists.add(artist);
                                    }
                                    Log.d(TAG,
                                            "Converted " + artists.size() + " artistResults in " + (
                                                    System.currentTimeMillis() - time) + "ms");
                                    mArtists = artists;
                                    deferred.resolve(artists);
                                }
                            });
                }
            });
        }
        return deferred;
    }

    @Override
    public Promise<Set<Artist>, Throwable, Void> getAlbumArtists() {
        final Deferred<Set<Artist>, Throwable, Void> deferred = new ADeferredObject<>();
        if (mAlbumArtists != null) {
            deferred.resolve(mAlbumArtists);
        } else {
            getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
                @Override
                public void onDone(ScriptResolverCollectionMetaData result) {
                    HashMap<String, Object> a = new HashMap<>();
                    a.put("id", result.id);
                    ScriptJob.start(mScriptObject, "albumArtists", a,
                            new ScriptJob.ResultsArrayCallback() {
                                @Override
                                public void onReportResults(JsonArray results) {
                                    Set<Artist> artists = new HashSet<>();
                                    for (JsonElement result : results) {
                                        Artist artist = Artist.get(
                                                ScriptUtils
                                                        .getNodeChildAsText(result, "albumArtist"));
                                        artists.add(artist);
                                    }
                                    mAlbumArtists = artists;
                                    deferred.resolve(artists);
                                }
                            });
                }
            });
        }
        return deferred;
    }

    @Override
    public Promise<Set<Album>, Throwable, Void> getAlbums() {
        final Deferred<Set<Album>, Throwable, Void> deferred = new ADeferredObject<>();
        if (mAlbums != null) {
            deferred.resolve(mAlbums);
        } else {
            getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
                @Override
                public void onDone(ScriptResolverCollectionMetaData result) {
                    final HashMap<String, Object> a = new HashMap<>();
                    a.put("id", result.id);
                    final long timeBefore = System.currentTimeMillis();
                    ScriptJob.start(mScriptObject, "albums", a,
                            new ScriptJob.ResultsArrayCallback() {
                                @Override
                                public void onReportResults(JsonArray results) {
                                    Log.d(TAG,
                                            "Received " + results.size() + " albumResults in " + (
                                                    System.currentTimeMillis() - timeBefore)
                                                    + "ms");
                                    long time = System.currentTimeMillis();
                                    Set<Album> albums = new HashSet<>();
                                    for (JsonElement result : results) {
                                        Artist albumArtist = Artist.get(
                                                ScriptUtils
                                                        .getNodeChildAsText(result, "albumArtist"));
                                        Album album = Album.get(
                                                ScriptUtils.getNodeChildAsText(result, "album"),
                                                albumArtist);
                                        albums.add(album);
                                    }
                                    Log.d(TAG,
                                            "Converted " + albums.size() + " albumResults in " + (
                                                    System.currentTimeMillis() - time) + "ms");
                                    mAlbums = albums;
                                    deferred.resolve(albums);
                                }
                            });
                }
            });
        }
        return deferred;
    }

    @Override
    public Promise<Set<Album>, Throwable, Void> getArtistAlbums(final Artist artist,
            boolean onlyIfCached) {
        final Deferred<Set<Album>, Throwable, Void> deferred = new ADeferredObject<>();
        if (mArtistAlbums.get(artist) != null) {
            deferred.resolve(mArtistAlbums.get(artist));
        } else if (!onlyIfCached) {
            getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
                @Override
                public void onDone(ScriptResolverCollectionMetaData result) {
                    HashMap<String, Object> a = new HashMap<>();
                    a.put("id", result.id);
                    a.put("artist", artist.getName());
                    a.put("artistDisambiguation", "");
                    ScriptJob.start(mScriptObject, "artistAlbums", a,
                            new ScriptJob.ResultsArrayCallback() {
                                @Override
                                public void onReportResults(JsonArray results) {
                                    Set<Album> albums = new HashSet<>();
                                    for (JsonElement result : results) {
                                        Artist albumArtist = Artist.get(
                                                ScriptUtils
                                                        .getNodeChildAsText(result, "albumArtist"));
                                        Album album = Album.get(
                                                ScriptUtils.getNodeChildAsText(result, "album"),
                                                albumArtist);
                                        albums.add(album);
                                    }
                                    mArtistAlbums.put(artist, albums);
                                    deferred.resolve(albums);
                                }
                            });
                }
            });
        } else {
            deferred.reject(new Throwable("No cached result available!"));
        }
        return deferred;
    }

    @Override
    public Promise<Set<Query>, Throwable, Void> getAlbumTracks(final Album album,
            boolean onlyIfCached) {
        final Deferred<Set<Query>, Throwable, Void> deferred = new ADeferredObject<>();
        final long time = System.currentTimeMillis();
        if (mAlbumTracks.get(album) != null) {
            deferred.resolve(mAlbumTracks.get(album));
        } else if (!onlyIfCached) {
            getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
                @Override
                public void onDone(ScriptResolverCollectionMetaData result) {
                    Log.d("perftest",
                            "getMetadata in " + (System.currentTimeMillis() - time) + "ms");
                    HashMap<String, Object> a = new HashMap<>();
                    a.put("id", result.id);
                    a.put("albumArtist", album.getArtist().getName());
                    a.put("albumArtistDisambiguation", "");
                    a.put("album", album.getName());
                    final long time = System.currentTimeMillis();
                    ScriptJob.start(mScriptObject, "albumTracks", a,
                            new ScriptJob.ResultsArrayCallback() {
                                @Override
                                public void onReportResults(JsonArray results) {
                                    Log.d("perftest",
                                            "albumTracks in " + (System.currentTimeMillis() - time)
                                                    + "ms");
                                    long time = System.currentTimeMillis();
                                    ArrayList<Result> parsedResults = ScriptUtils.parseResultList(
                                            mScriptAccount.getScriptResolver(), results);
                                    Log.d("perftest",
                                            "albumTracks parsed in " + (System.currentTimeMillis()
                                                    - time) + "ms");
                                    time = System.currentTimeMillis();
                                    Set<Query> queries = new HashSet<>();
                                    for (Result r : parsedResults) {
                                        Query query = Query.get(r, false);
                                        float trackScore = query.howSimilar(r);
                                        query.addTrackResult(r, trackScore);
                                        queries.add(query);
                                    }
                                    Log.d("perftest",
                                            "albumTracks converted in " + (
                                                    System.currentTimeMillis()
                                                            - time) + "ms");
                                    mAlbumTracks.put(album, queries);
                                    deferred.resolve(queries);
                                }
                            });
                }
            });
        } else {
            deferred.reject(new Throwable("No cached result available!"));
        }
        return deferred;
    }
}
