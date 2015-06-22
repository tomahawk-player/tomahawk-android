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
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.QueryComparator;
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

import android.widget.ImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

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
    public Deferred<Set<Query>, String, Object> getQueries(final boolean sorted) {
        final Deferred<Set<Query>, String, Object> deferred = new ADeferredObject<>();
        getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
            @Override
            public void onDone(ScriptResolverCollectionMetaData result) {
                HashMap<String, Object> a = new HashMap<>();
                a.put("id", result.id);
                ScriptJob.start(mScriptObject, "tracks", a, new ScriptJob.ResultsArrayCallback() {
                    @Override
                    public void onReportResults(JsonArray results) {
                        ArrayList<Result> parsedResults = ScriptUtils.parseResultList(
                                mScriptAccount.getScriptResolver(), results);
                        Set<Query> queries;
                        if (sorted) {
                            queries = new TreeSet<>(
                                    new QueryComparator(QueryComparator.COMPARE_ALPHA));
                        } else {
                            queries = new HashSet<>();
                        }
                        for (Result r : parsedResults) {
                            Query query = Query.get(r, false);
                            float trackScore = query.howSimilar(r);
                            query.addTrackResult(r, trackScore);
                            queries.add(query);
                        }
                        deferred.resolve(queries);
                    }
                });
            }
        });
        return deferred;
    }

    @Override
    public Deferred<Set<Artist>, String, Object> getArtists(final boolean sorted) {
        final Deferred<Set<Artist>, String, Object> deferred = new ADeferredObject<>();
        getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
            @Override
            public void onDone(ScriptResolverCollectionMetaData result) {
                HashMap<String, Object> a = new HashMap<>();
                a.put("id", result.id);
                ScriptJob.start(mScriptObject, "artists", a, new ScriptJob.ResultsArrayCallback() {
                    @Override
                    public void onReportResults(JsonArray results) {
                        Set<Artist> artists;
                        if (sorted) {
                            artists = new TreeSet<>(new AlphaComparator());
                        } else {
                            artists = new HashSet<>();
                        }
                        for (JsonElement result : results) {
                            Artist artist = Artist
                                    .get(ScriptUtils.getNodeChildAsText(result, "artist"));
                            artists.add(artist);
                        }
                        deferred.resolve(artists);
                    }
                });
            }
        });
        return deferred;
    }

    @Override
    public Deferred<Set<Album>, String, Object> getAlbums(final boolean sorted) {
        final Deferred<Set<Album>, String, Object> deferred = new ADeferredObject<>();
        getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
            @Override
            public void onDone(ScriptResolverCollectionMetaData result) {
                HashMap<String, Object> a = new HashMap<>();
                a.put("id", result.id);
                ScriptJob.start(mScriptObject, "albums", a, new ScriptJob.ResultsArrayCallback() {
                    @Override
                    public void onReportResults(JsonArray results) {
                        Set<Album> albums;
                        if (sorted) {
                            albums = new TreeSet<>(new AlphaComparator());
                        } else {
                            albums = new HashSet<>();
                        }
                        for (JsonElement result : results) {
                            Artist albumArtist = Artist.get(
                                    ScriptUtils.getNodeChildAsText(result, "albumArtist"));
                            Album album = Album.get(
                                    ScriptUtils.getNodeChildAsText(result, "album"), albumArtist);
                            albums.add(album);
                        }
                        deferred.resolve(albums);
                    }
                });
            }
        });
        return deferred;
    }

    @Override
    public Deferred<Set<Album>, String, Object> getArtistAlbums(final Artist artist,
            final boolean sorted) {
        final Deferred<Set<Album>, String, Object> deferred = new ADeferredObject<>();
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
                                Set<Album> albums;
                                if (sorted) {
                                    albums = new TreeSet<>(new AlphaComparator());
                                } else {
                                    albums = new HashSet<>();
                                }
                                for (JsonElement result : results) {
                                    Artist albumArtist = Artist.get(
                                            ScriptUtils.getNodeChildAsText(result, "albumArtist"));
                                    Album album = Album.get(
                                            ScriptUtils.getNodeChildAsText(result, "album"),
                                            albumArtist);
                                    albums.add(album);
                                }
                                deferred.resolve(albums);
                            }
                        });
            }
        });
        return deferred;
    }

    public Deferred<Boolean, String, Object> hasArtistAlbums(final Artist artist) {
        final Deferred<Boolean, String, Object> deferred = new ADeferredObject<>();
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
                                deferred.resolve(results.size() > 0);
                            }
                        }, new ScriptJob.FailureCallback() {
                            @Override
                            public void onReportFailure(String errormessage) {
                                deferred.resolve(false);
                            }
                        });
            }
        });
        return deferred;
    }

    @Override
    public Deferred<Set<Query>, String, Object> getAlbumTracks(final Album album,
            final boolean sorted) {
        final Deferred<Set<Query>, String, Object> deferred = new ADeferredObject<>();
        getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
            @Override
            public void onDone(ScriptResolverCollectionMetaData result) {
                HashMap<String, Object> a = new HashMap<>();
                a.put("id", result.id);
                a.put("albumArtist", album.getArtist().getName());
                a.put("albumArtistDisambiguation", "");
                a.put("album", album.getName());
                ScriptJob.start(mScriptObject, "albumTracks", a,
                        new ScriptJob.ResultsArrayCallback() {
                            @Override
                            public void onReportResults(JsonArray results) {
                                ArrayList<Result> parsedResults =
                                        ScriptUtils
                                                .parseResultList(mScriptAccount.getScriptResolver(),
                                                        results);
                                Set<Query> queries;
                                if (sorted) {
                                    queries = new TreeSet<>(
                                            new QueryComparator(QueryComparator.COMPARE_ALPHA));
                                } else {
                                    queries = new HashSet<>();
                                }
                                for (Result r : parsedResults) {
                                    Query query = Query.get(r, false);
                                    float trackScore = query.howSimilar(r);
                                    query.addTrackResult(r, trackScore);
                                    queries.add(query);
                                }
                                deferred.resolve(queries);
                            }
                        });
            }
        });
        return deferred;
    }

    public Deferred<Boolean, String, Object> hasAlbumTracks(final Album album) {
        final Deferred<Boolean, String, Object> deferred = new ADeferredObject<>();
        getMetaData().done(new DoneCallback<ScriptResolverCollectionMetaData>() {
            @Override
            public void onDone(ScriptResolverCollectionMetaData result) {
                HashMap<String, Object> a = new HashMap<>();
                a.put("id", result.id);
                a.put("albumArtist", album.getArtist().getName());
                a.put("albumArtistDisambiguation", "");
                a.put("album", album.getName());
                ScriptJob.start(mScriptObject, "albumTracks", a,
                        new ScriptJob.ResultsArrayCallback() {
                            @Override
                            public void onReportResults(JsonArray results) {
                                deferred.resolve(results.size() > 0);
                            }
                        }, new ScriptJob.FailureCallback() {
                            @Override
                            public void onReportFailure(String errormessage) {
                                deferred.resolve(false);
                            }
                        });
            }
        });
        return deferred;
    }
}
