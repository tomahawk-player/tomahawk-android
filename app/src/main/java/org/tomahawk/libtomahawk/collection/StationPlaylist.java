/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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
import com.google.gson.JsonObject;

import org.jdeferred.AlwaysCallback;
import org.jdeferred.Deferred;
import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.tomahawk.libtomahawk.infosystem.stations.ScriptPlaylistGenerator;
import org.tomahawk.libtomahawk.infosystem.stations.ScriptPlaylistGeneratorManager;
import org.tomahawk.libtomahawk.infosystem.stations.ScriptPlaylistGeneratorResult;
import org.tomahawk.libtomahawk.infosystem.stations.ScriptPlaylistGeneratorSearchResult;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.ADeferredObject;
import org.tomahawk.libtomahawk.utils.GsonHelper;

import android.support.v4.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StationPlaylist extends Playlist {

    private String mSessionId;

    private Playlist mPlaylist;

    private List<Pair<Artist, String>> mArtists;

    private List<Pair<Track, String>> mTracks;

    private List<String> mGenres;

    private final List<Query> mCandidates = new ArrayList<>();

    private long mCreatedTimeStamp = 0L;

    private long mPlayedTimeStamp = 0L;

    private Deferred<List<Query>, Throwable, Void> mFillDeferred;

    private StationPlaylist(List<Pair<Artist, String>> artists, List<Pair<Track, String>> tracks,
            List<String> genres) {
        super(getCacheKey(artists, tracks, genres));

        mArtists = artists;
        mTracks = tracks;
        mGenres = genres;

        String name = "";
        if (mArtists != null) {
            for (Pair<Artist, String> artist : mArtists) {
                if (!name.isEmpty()) {
                    name += ", ";
                }
                name += artist.first.getPrettyName();
            }
        }
        if (mTracks != null) {
            for (Pair<Track, String> track : mTracks) {
                if (!name.isEmpty()) {
                    name += ", ";
                }
                name += track.first.getArtist().getPrettyName() + " - " + track.first.getName();
            }
        }
        if (mGenres != null) {
            for (String genre : mGenres) {
                if (!name.isEmpty()) {
                    name += ", ";
                }
                name += genre;
            }
        }
        setName(name);

        mCreatedTimeStamp = System.currentTimeMillis();
    }

    private StationPlaylist(Playlist playlist) {
        super(getCacheKey(playlist));

        mPlaylist = playlist;

        setName(mPlaylist.getName());

        mCreatedTimeStamp = System.currentTimeMillis();
    }

    private static String getCacheKey(Playlist playlist) {
        return "station_" + playlist.getCacheKey();
    }

    private static String getCacheKey(List<Pair<Artist, String>> artists,
            List<Pair<Track, String>> tracks, List<String> genres) {
        String key = "station_";
        if (artists != null) {
            Collections.sort(artists, new Comparator<Pair<Artist, String>>() {
                @Override
                public int compare(Pair<Artist, String> lhs, Pair<Artist, String> rhs) {
                    return lhs.first.getName().compareToIgnoreCase(rhs.first.getName());
                }
            });
            for (Pair<Artist, String> artist : artists) {
                key += "♠" + artist.first.getCacheKey();
            }
        }
        if (tracks != null) {
            Collections.sort(tracks, new Comparator<Pair<Track, String>>() {
                @Override
                public int compare(Pair<Track, String> lhs, Pair<Track, String> rhs) {
                    return lhs.first.getName().compareToIgnoreCase(rhs.first.getName());
                }
            });
            for (Pair<Track, String> track : tracks) {
                key += "♠" + track.first.getCacheKey();
            }
        }
        if (genres != null) {
            Collections.sort(genres);
            for (String genre : genres) {
                key += "♠" + genre;
            }
        }
        return key;
    }

    public static StationPlaylist get(String json) {
        JsonObject jsonObject = GsonHelper.get().fromJson(json, JsonObject.class);

        List<Pair<Artist, String>> artists = null;
        if (jsonObject.has("artists") && jsonObject.get("artists").isJsonArray()) {
            artists = new ArrayList<>();
            for (JsonElement element : jsonObject.getAsJsonArray("artists")) {
                if (element.isJsonObject()) {
                    JsonObject o = element.getAsJsonObject();
                    Artist artist = Artist.get(o.get("artist").getAsString());
                    JsonElement idElement = o.get(getIdKey());
                    String id = "";
                    if (idElement != null) {
                        id = idElement.getAsString();
                    }
                    artists.add(new Pair<>(artist, id));
                }
            }
        }

        List<Pair<Track, String>> tracks = null;
        if (jsonObject.has("tracks") && jsonObject.get("tracks").isJsonArray()) {
            tracks = new ArrayList<>();
            for (JsonElement element : jsonObject.getAsJsonArray("tracks")) {
                if (element.isJsonObject()) {
                    JsonObject o = element.getAsJsonObject();
                    Artist artist = Artist.get(o.get("artist").getAsString());
                    Album album = Album.get(o.get("album").getAsString(), artist);
                    Track track = Track.get(o.get("track").getAsString(), album, artist);
                    JsonElement idElement = o.get(getIdKey());
                    String id = "";
                    if (idElement != null) {
                        id = idElement.getAsString();
                    }
                    tracks.add(new Pair<>(track, id));
                }
            }
        }

        List<String> genres = null;
        if (jsonObject.has("genres") && jsonObject.get("genres").isJsonArray()) {
            genres = new ArrayList<>();
            for (JsonElement element : jsonObject.getAsJsonArray("genres")) {
                if (element.isJsonObject()) {
                    JsonObject o = element.getAsJsonObject();
                    genres.add(o.get("name").getAsString());
                }
            }
        }

        return get(artists, tracks, genres);
    }

    public static StationPlaylist get(List<Pair<Artist, String>> artists,
            List<Pair<Track, String>> tracks, List<String> genres) {
        Cacheable cacheable = get(Playlist.class, getCacheKey(artists, tracks, genres));
        return cacheable != null ? (StationPlaylist) cacheable
                : new StationPlaylist(artists, tracks, genres);
    }

    public static StationPlaylist get(Playlist playlist) {
        Cacheable cacheable = get(Playlist.class, getCacheKey(playlist));
        return cacheable != null ? (StationPlaylist) cacheable : new StationPlaylist(playlist);
    }

    public List<Pair<Artist, String>> getArtists() {
        return mArtists;
    }

    public List<Pair<Track, String>> getTracks() {
        return mTracks;
    }

    public List<String> getGenres() {
        return mGenres;
    }

    public Playlist getPlaylist() {
        return mPlaylist;
    }

    public void setCreatedTimeStamp(long createdTimeStamp) {
        mCreatedTimeStamp = createdTimeStamp;
    }

    public long getCreatedTimeStamp() {
        return mCreatedTimeStamp;
    }

    public long getPlayedTimeStamp() {
        return mPlayedTimeStamp;
    }

    public void setPlayedTimeStamp(long playedTimeStamp) {
        mPlayedTimeStamp = playedTimeStamp;
    }

    public Promise<List<Query>, Throwable, Void> fillPlaylist(final int limit) {
        if (mFillDeferred != null && mFillDeferred.isPending()) {
            return null;
        }
        final Deferred<List<Query>, Throwable, Void> fillDeferred = new ADeferredObject<>();
        mFillDeferred = fillDeferred;
        pickSeedsFromPlaylist().done(new DoneCallback<Void>() {
            @Override
            public void onDone(Void result) {
                ScriptPlaylistGenerator generator =
                        ScriptPlaylistGeneratorManager.get().getDefaultPlaylistGenerator();
                if (mCandidates.size() >= limit) {
                    // We got enough candidates in cache
                    List<Query> queries = new ArrayList<>();
                    for (int i = 0; i < limit; i++) {
                        queries.add(mCandidates.remove(0));
                    }
                    fillDeferred.resolve(queries);
                } else if (generator != null) {
                    generator.fillPlaylist(mSessionId, mArtists, mTracks, mGenres)
                            .done(new DoneCallback<ScriptPlaylistGeneratorResult>() {
                                @Override
                                public void onDone(ScriptPlaylistGeneratorResult result) {
                                    mSessionId = result.sessionId;
                                    List<Query> queries = new ArrayList<>();
                                    if (result.results != null) {
                                        int actualLimit = Math.min(result.results.size(), limit);
                                        for (int i = 0; i < actualLimit; i++) {
                                            queries.add(result.results.remove(0));
                                        }
                                        // Add the rest to our candidate cache
                                        mCandidates.addAll(result.results);
                                    }
                                    if (queries.size() == 0) {
                                        fillDeferred.reject(
                                                new Throwable("Couldn't find suitable tracks"));
                                    } else {
                                        fillDeferred.resolve(queries);
                                    }
                                }
                            })
                            .fail(new FailCallback<Throwable>() {
                                @Override
                                public void onFail(Throwable result) {
                                    fillDeferred.reject(result);
                                }
                            });
                }
            }
        });
        return mFillDeferred;
    }

    public String toJson() {
        JsonObject json = new JsonObject();

        if (mArtists != null) {
            JsonArray artists = new JsonArray();
            for (Pair<Artist, String> artist : mArtists) {
                JsonObject o = new JsonObject();
                o.addProperty("artist", artist.first.getName());
                o.addProperty(getIdKey(), artist.second);
                artists.add(o);
            }
            json.add("artists", artists);
        }

        if (mTracks != null) {
            JsonArray tracks = new JsonArray();
            for (Pair<Track, String> track : mTracks) {
                JsonObject o = new JsonObject();
                o.addProperty("track", track.first.getName());
                o.addProperty("artist", track.first.getArtist().getName());
                o.addProperty("album", track.first.getAlbum().getName());
                o.addProperty(getIdKey(), track.second);
                tracks.add(o);
            }
            json.add("tracks", tracks);
        }

        if (mGenres != null) {
            JsonArray genres = new JsonArray();
            for (String genre : mGenres) {
                JsonObject o = new JsonObject();
                o.addProperty("name", genre);
                genres.add(o);
            }
            json.add("genres", genres);
        }

        return GsonHelper.get().toJson(json);
    }

    private Promise<Void, Void, Throwable> pickSeedsFromPlaylist() {
        ADeferredObject<Void, Void, Throwable> deferred = new ADeferredObject<>();
        if (mPlaylist == null || mTracks != null) {
            // No need to do anything
            deferred.resolve(null);
        } else if (mPlaylist.size() < 5) {
            // No need to pick random tracks, we use all of them anyways
            List<Pair<Track, String>> tracks = new ArrayList<>();
            for (PlaylistEntry entry : mPlaylist.getEntries()) {
                // Let the js resolver fetch the track ids for us
                tracks.add(new Pair<>(entry.getQuery().getBasicTrack(), ""));
            }
            mTracks = tracks;
            deferred.resolve(null);
        } else {
            pickSeedsFromPlaylist(deferred, new ArrayList<Integer>(),
                    new ArrayList<Pair<Track, String>>(), 10);
        }
        return deferred;
    }

    private void pickSeedsFromPlaylist(final ADeferredObject<Void, Void, Throwable> deferred,
            final List<Integer> pickedIndexes, final List<Pair<Track, String>> tracks,
            int attemptCount) {
        if (attemptCount-- < 0 || tracks.size() >= 5) {
            mTracks = tracks;
            deferred.resolve(null);
            return;
        }
        int size = mPlaylist.size();
        int randomIndex = (int) (Math.random() * size);
        while (pickedIndexes.contains(randomIndex)) {
            if (randomIndex + 1 < size) {
                randomIndex++;
            } else {
                randomIndex = 0;
            }
        }
        pickedIndexes.add(randomIndex);
        PlaylistEntry entry = mPlaylist.getEntryAtPos(randomIndex);
        Track candidate = entry.getQuery().getBasicTrack();
        final int finalAttemptCount = attemptCount;
        ScriptPlaylistGenerator generator =
                ScriptPlaylistGeneratorManager.get().getDefaultPlaylistGenerator();
        if (generator != null) {
            generator.search("track:" + candidate.getName()
                    + "%20artist:" + candidate.getArtist().getName()).always(
                    new AlwaysCallback<ScriptPlaylistGeneratorSearchResult, Throwable>() {
                        @Override
                        public void onAlways(Promise.State state,
                                ScriptPlaylistGeneratorSearchResult resolved, Throwable rejected) {
                            if (resolved != null && resolved.mTracks != null
                                    && resolved.mTracks.size() > 0) {
                                tracks.add(resolved.mTracks.get(0));
                            }
                            pickSeedsFromPlaylist(deferred, pickedIndexes, tracks,
                                    finalAttemptCount);
                        }
                    });
        }
    }

    private static String getIdKey() {
        return ScriptPlaylistGeneratorManager.get().getDefaultPlaylistGeneratorId() + "_id";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "( id: " + getId() + ", name: " + getName()
                + ", size: " + size() + " )@" + Integer.toHexString(hashCode());
    }
}
