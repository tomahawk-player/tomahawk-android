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

import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;
import org.tomahawk.libtomahawk.infosystem.stations.ScriptPlaylistGenerator;
import org.tomahawk.libtomahawk.infosystem.stations.ScriptPlaylistGeneratorManager;
import org.tomahawk.libtomahawk.infosystem.stations.ScriptPlaylistGeneratorResult;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.ADeferredObject;
import org.tomahawk.libtomahawk.utils.GsonHelper;

import android.support.v4.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StationPlaylist extends Playlist {

    private String mSessionId;

    private List<Artist> mArtists;

    private List<Pair<Track, String>> mTracks;

    private List<String> mGenres;

    private final Set<Query> mCandidates =
            Collections.newSetFromMap(new ConcurrentHashMap<Query, Boolean>());

    private long mCreatedTimeStamp = 0L;

    private long mPlayedTimeStamp = 0L;

    private StationPlaylist(List<Artist> artists, List<Pair<Track, String>> tracks,
            List<String> genres) {
        super(getCacheKey(artists, tracks, genres));

        mArtists = artists;
        mTracks = tracks;
        mGenres = genres;

        String name = "";
        if (mArtists != null) {
            for (Artist artist : mArtists) {
                if (!name.isEmpty()) {
                    name += ", ";
                }
                name += artist.getPrettyName();
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

    private static String getCacheKey(List<Artist> artists, List<Pair<Track, String>> tracks,
            List<String> genres) {
        String key = "station_";
        if (artists != null) {
            Collections.sort(artists, new AlphaComparator());
            for (Artist artist : artists) {
                key += "♠" + artist.getCacheKey();
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

        List<Artist> artists = null;
        if (jsonObject.has("artists") && jsonObject.get("artists").isJsonArray()) {
            artists = new ArrayList<>();
            for (JsonElement element : jsonObject.getAsJsonArray("artists")) {
                if (element.isJsonObject()) {
                    JsonObject o = element.getAsJsonObject();
                    artists.add(Artist.get(o.get("artist").getAsString()));
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
                    String songId = o.get("songId").getAsString();
                    tracks.add(new Pair<>(track, songId));
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

    public static StationPlaylist get(List<Artist> artists, List<Pair<Track, String>> tracks,
            List<String> genres) {
        Cacheable cacheable = get(Playlist.class, getCacheKey(artists, tracks, genres));
        return cacheable != null ? (StationPlaylist) cacheable
                : new StationPlaylist(artists, tracks, genres);
    }

    public List<Artist> getArtists() {
        return mArtists;
    }

    public List<Pair<Track, String>> getTracks() {
        return mTracks;
    }

    public List<String> getGenres() {
        return mGenres;
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

    public Promise<List<Query>, Throwable, Void> fillPlaylist(final boolean initial) {
        final ScriptPlaylistGenerator generator =
                ScriptPlaylistGeneratorManager.get().getDefaultPlaylistGenerator();
        final ADeferredObject<List<Query>, Throwable, Void> deferred = new ADeferredObject<>();
        if (generator != null) {
            final DoneCallback<ScriptPlaylistGeneratorResult> callback
                    = new DoneCallback<ScriptPlaylistGeneratorResult>() {
                @Override
                public void onDone(ScriptPlaylistGeneratorResult result) {
                    mSessionId = result.sessionId;
                    mCandidates.addAll(result.results);
                    deferred.resolve(result.results);
                }
            };
            if (mSessionId == null) {
                generator.createStation(mArtists, mTracks, mGenres).done(callback)
                        .done(new DoneCallback<ScriptPlaylistGeneratorResult>() {
                            @Override
                            public void onDone(ScriptPlaylistGeneratorResult result) {
                                generator.fillStation(mSessionId).done(callback);
                                if (initial) {
                                    generator.fillStation(mSessionId).done(callback);
                                }
                            }
                        });
            } else {
                generator.fillStation(mSessionId).done(callback)
                        .done(new DoneCallback<ScriptPlaylistGeneratorResult>() {
                            @Override
                            public void onDone(ScriptPlaylistGeneratorResult result) {
                                if (initial) {
                                    generator.fillStation(mSessionId).done(callback);
                                }
                            }
                        });
            }
        }
        return deferred;
    }

    public boolean isCandidate(Query query) {
        return mCandidates.contains(query);
    }

    public String toJson() {
        JsonObject json = new JsonObject();

        if (mArtists != null) {
            JsonArray artists = new JsonArray();
            for (Artist artist : mArtists) {
                JsonObject o = new JsonObject();
                o.addProperty("artist", artist.getName());
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
                o.addProperty("songId", track.second);
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
}
