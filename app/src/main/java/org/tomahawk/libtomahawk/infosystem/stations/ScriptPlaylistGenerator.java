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
package org.tomahawk.libtomahawk.infosystem.stations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.ScriptAccount;
import org.tomahawk.libtomahawk.resolver.ScriptJob;
import org.tomahawk.libtomahawk.resolver.ScriptObject;
import org.tomahawk.libtomahawk.resolver.ScriptPlugin;
import org.tomahawk.libtomahawk.utils.ADeferredObject;

import android.support.v4.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptPlaylistGenerator implements ScriptPlugin {

    public static final String TAG = ScriptPlaylistGenerator.class.getSimpleName();

    private ScriptAccount mScriptAccount;

    private ScriptObject mScriptObject;

    public ScriptPlaylistGenerator(ScriptObject scriptObject, ScriptAccount account) {
        mScriptObject = scriptObject;
        mScriptAccount = account;
    }

    public Promise<ScriptPlaylistGeneratorSearchResult, Throwable, Void> search(String query) {
        Map<String, Object> args = new HashMap<>();
        args.put("query", query);
        final Deferred<ScriptPlaylistGeneratorSearchResult, Throwable, Void> deferred =
                new ADeferredObject<>();
        ScriptJob.start(mScriptObject, "search", args, new ScriptJob.ResultsObjectCallback() {
            @Override
            public void onReportResults(JsonObject results) {
                ScriptPlaylistGeneratorSearchResult result =
                        new ScriptPlaylistGeneratorSearchResult();
                JsonArray artists = results.getAsJsonArray("artists");
                if (artists != null) {
                    for (JsonElement element : artists) {
                        if (element instanceof JsonObject) {
                            JsonElement artistName = ((JsonObject) element).get("artist");
                            JsonElement id = ((JsonObject) element).get("id");
                            if (artistName != null) {
                                Artist artist = Artist.get(artistName.getAsString());
                                result.mArtists.add(new Pair<>(artist, id.getAsString()));
                            }
                        }
                    }
                }
                JsonArray albums = results.getAsJsonArray("albums");
                if (albums != null) {
                    for (JsonElement element : albums) {
                        if (element instanceof JsonObject) {
                            JsonElement artistName = ((JsonObject) element).get("artist");
                            JsonElement albumName = ((JsonObject) element).get("album");
                            if (artistName != null && albumName != null) {
                                Artist artist = Artist.get(artistName.getAsString());
                                result.mAlbums.add(Album.get(albumName.getAsString(), artist));
                            }
                        }
                    }
                }
                JsonArray tracks = results.getAsJsonArray("tracks");
                if (tracks != null) {
                    for (JsonElement element : tracks) {
                        if (element instanceof JsonObject) {
                            JsonElement artistName = ((JsonObject) element).get("artist");
                            JsonElement trackName = ((JsonObject) element).get("track");
                            JsonElement albumName = ((JsonObject) element).get("album");
                            JsonElement id = ((JsonObject) element).get("id");
                            if (artistName != null && trackName != null && albumName != null) {
                                Artist artist = Artist.get(artistName.getAsString());
                                Album album = Album.get(albumName.getAsString(), artist);
                                Track track = Track.get(trackName.getAsString(), album, artist);
                                result.mTracks.add(new Pair<>(track, id.getAsString()));
                            }
                        }
                    }
                }
                JsonArray genres = results.getAsJsonArray("genres");
                if (genres != null) {
                    for (JsonElement element : genres) {
                        if (element instanceof JsonObject) {
                            JsonElement genreName = ((JsonObject) element).get("name");
                            if (genreName != null) {
                                result.mGenres.add(genreName.getAsString());
                            }
                        }
                    }
                }
                JsonArray moods = results.getAsJsonArray("moods");
                if (moods != null) {
                    for (JsonElement element : moods) {
                        if (element instanceof JsonObject) {
                            JsonElement moodName = ((JsonObject) element).get("name");
                            if (moodName != null) {
                                result.mMoods.add(moodName.getAsString());
                            }
                        }
                    }
                }
                deferred.resolve(result);
            }
        });
        return deferred;
    }

    public Promise<ScriptPlaylistGeneratorResult, Throwable, Void> fillPlaylist(String sessionId,
            List<Pair<Artist, String>> artists, List<Pair<Track, String>> tracks,
            List<String> genres) {
        Map<String, Object> args = new HashMap<>();
        if (sessionId != null) {
            args.put("sessionId", sessionId);
        }
        if (artists != null) {
            JsonArray jsonArtists = new JsonArray();
            for (Pair<Artist, String> artist : artists) {
                JsonObject o = new JsonObject();
                o.addProperty("artist", artist.first.getName());
                o.addProperty("id", artist.second);
                jsonArtists.add(o);
            }
            args.put("artists", jsonArtists);
        }
        if (tracks != null) {
            JsonArray jsonTracks = new JsonArray();
            for (Pair<Track, String> track : tracks) {
                JsonObject o = new JsonObject();
                o.addProperty("track", track.first.getName());
                o.addProperty("artist", track.first.getArtist().getName());
                o.addProperty("album", track.first.getAlbum().getName());
                o.addProperty("id", track.second);
                jsonTracks.add(o);
            }
            args.put("tracks", jsonTracks);
        }
        if (genres != null) {
            JsonArray jsonGenres = new JsonArray();
            for (String genre : genres) {
                JsonObject o = new JsonObject();
                o.addProperty("name", genre);
                jsonGenres.add(o);
            }
            args.put("genres", jsonGenres);
        }
        final Deferred<ScriptPlaylistGeneratorResult, Throwable, Void> deferred =
                new ADeferredObject<>();
        ScriptJob.start(mScriptObject, "fillPlaylist", args, new ScriptJob.ResultsObjectCallback() {
            @Override
            public void onReportResults(JsonObject results) {
                deferred.resolve(parseResult(results));
            }
        }, new ScriptJob.FailureCallback() {
            @Override
            public void onReportFailure(String errormessage) {
                deferred.reject(new Throwable("Error while loading station: " + errormessage));
            }
        });
        return deferred;
    }

    private ScriptPlaylistGeneratorResult parseResult(JsonObject rawResults) {
        ScriptPlaylistGeneratorResult result = new ScriptPlaylistGeneratorResult();
        result.sessionId = rawResults.get("sessionId").getAsString();
        result.results = new ArrayList<>();
        for (JsonElement element : rawResults.getAsJsonArray("results")) {
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                String trackName = object.get("track").getAsString();
                String artistName = object.get("artist").getAsString();
                String albumName = object.get("album").getAsString();
                Query query = Query.get(trackName, albumName, artistName, false);
                result.results.add(query);
            }
        }
        return result;
    }

    @Override
    public ScriptAccount getScriptAccount() {
        return mScriptAccount;
    }

    @Override
    public ScriptObject getScriptObject() {
        return mScriptObject;
    }
}
