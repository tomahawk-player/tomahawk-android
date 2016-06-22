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
package org.tomahawk.libtomahawk.infosystem.hatchet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.AlphaComparator;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.ListItemString;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistComparator;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.QueryParams;
import org.tomahawk.libtomahawk.infosystem.Relationship;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.GsonHelper;
import org.tomahawk.libtomahawk.utils.ISO8601Utils;
import org.tomahawk.libtomahawk.utils.NetworkUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.android.MainThreadExecutor;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

import static android.os.Process.THREAD_PRIORITY_LOWEST;

public class Store {

    private final static String TAG = Store.class.getSimpleName();

    public static final String HATCHET_BASE_URL = "https://api.hatchet.is";

    public static final String HATCHET_API_VERSION = "/v2";

    private final Cache mCache = new Cache();

    private static class Cache {

        private Map<Class, Map> mCaches = new HashMap<>();

        public Cache() {
        }

        public <T> void addCache(Class<T> clss) {
            mCaches.put(clss, new HashMap<String, T>());
        }

        public <T> void put(Class<T> clss, String id, T object) {
            mCaches.get(clss).put(id, object);
        }

        public <T> T get(Class<T> clss, String id) {
            return (T) mCaches.get(clss).get(id);
        }

    }

    private final OkHttpClient mOkHttpClient;

    private final Hatchet mHatchet;

    private final Hatchet mHatchetBackground;

    public Store() {
        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                if (!NetworkUtils.isNetworkAvailable()) {
                    int maxStale = 60 * 60 * 24 * 7; // tolerate 1-week stale
                    request.addHeader("Cache-Control", "public, max-stale=" + maxStale);
                }
                request.addHeader("Content-type", "application/json; charset=utf-8");
            }
        };
        mOkHttpClient = new OkHttpClient();
        File cacheDir = new File(TomahawkApp.getContext().getCacheDir(), "responseCache");
        com.squareup.okhttp.Cache cache = new com.squareup.okhttp.Cache(cacheDir, 1024 * 1024 * 20);
        mOkHttpClient.setCache(cache);
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .setEndpoint(HATCHET_BASE_URL + HATCHET_API_VERSION)
                .setConverter(new GsonConverter(GsonHelper.get()))
                .setRequestInterceptor(requestInterceptor)
                .setClient(new OkClient(mOkHttpClient))
                .build();
        mHatchet = restAdapter.create(Hatchet.class);

        Executor httpExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(new Runnable() {
                    @Override
                    public void run() {
                        android.os.Process.setThreadPriority(THREAD_PRIORITY_LOWEST);
                        r.run();
                    }
                }, "Retrofit-Idle-Background");
            }
        });
        restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .setEndpoint(HATCHET_BASE_URL + HATCHET_API_VERSION)
                .setConverter(new GsonConverter(GsonHelper.get()))
                .setRequestInterceptor(requestInterceptor)
                .setClient(new OkClient(mOkHttpClient))
                .setExecutors(httpExecutor, new MainThreadExecutor())
                .build();
        mHatchetBackground = restAdapter.create(Hatchet.class);

        mCache.addCache(Image.class);
        mCache.addCache(Artist.class);
        mCache.addCache(Album.class);
        mCache.addCache(Query.class);
        mCache.addCache(ChartItem.class);
        mCache.addCache(Chart.class);
        mCache.addCache(PlaybackLogEntry.class);
        mCache.addCache(PlaylistEntry.class);
        mCache.addCache(User.class);
        mCache.addCache(Playlist.class);
        mCache.addCache(SocialAction.class);
        mCache.addCache(Search.class);
        mCache.addCache(SearchResult.class);
        mCache.addCache(Relationship.class);
    }

    public Hatchet getImplementation(boolean isBackgroundRequest) {
        return isBackgroundRequest ? mHatchetBackground : mHatchet;
    }

    public <T> T findRecord(String id, Class<T> resultType, boolean isBackgroundRequest)
            throws IOException {
        T record = mCache.get(resultType, id);
        if (record == null) {
            Hatchet hatchet = getImplementation(isBackgroundRequest);
            List<String> ids = new ArrayList<>();
            ids.add(String.valueOf(id));
            if (resultType == Image.class) {
                storeRecords(hatchet.getImages(ids), resultType, isBackgroundRequest);
            } else if (resultType == Artist.class) {
                storeRecords(hatchet.getArtists(ids, null), resultType, isBackgroundRequest);
            } else if (resultType == Album.class) {
                storeRecords(hatchet.getAlbums(ids, null, null), resultType, isBackgroundRequest);
            } else if (resultType == PlaylistEntry.class) {
                storeRecords(hatchet.getTracks(ids, null, null), resultType, isBackgroundRequest);
            } else if (resultType == User.class) {
                storeRecords(hatchet.getUsers(ids, null, null, null), resultType,
                        isBackgroundRequest);
            } else if (resultType == Playlist.class) {
                storeRecords(hatchet.getPlaylists(ids), resultType, isBackgroundRequest);
            }
            record = mCache.get(resultType, id);
            if (record == null) {
                throw new IOException("Couldn't fetch entity from server.");
            }
            mCache.put(resultType, id, record);
        }
        return record;
    }

    public <T> List<T> storeRecords(JsonObject object, Class<T> resultType,
            boolean isBackgroundRequest)
            throws IOException {
        return storeRecords(object, resultType, -1, isBackgroundRequest);
    }

    public <T> List<T> storeRecords(JsonObject object, Class<T> resultType, int requestType,
            boolean isBackgroundRequest)
            throws IOException {
        return storeRecords(object, resultType, requestType, isBackgroundRequest, null);
    }

    public <T> List<T> storeRecords(JsonObject object, Class<T> resultType, int requestType,
            boolean isBackgroundRequest, QueryParams params)
            throws IOException {
        List<T> results = new ArrayList<>();
        JsonElement elements = object.get("images");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    Image image = mCache.get(Image.class, id);
                    if (image == null) {
                        String url = getAsString(o, "url");
                        int width = getAsInt(o, "width");
                        int height = getAsInt(o, "height");
                        image = Image.get(url, true, width, height);
                        mCache.put(Image.class, id, image);
                    }
                    if (resultType == Image.class) {
                        results.add((T) image);
                    }
                }
            }
        }
        elements = object.get("artists");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    Artist artist = mCache.get(Artist.class, id);
                    if (artist == null) {
                        String name = getAsString(o, "name");
                        String wiki = getAsString(o, "wikiabstract");
                        artist = Artist.get(name);
                        artist.setBio(new ListItemString(wiki));
                        JsonElement images = get(o, "images");
                        if (images instanceof JsonArray && ((JsonArray) images).size() > 0) {
                            String imageId = ((JsonArray) images).get(0).getAsString();
                            Image image = findRecord(imageId, Image.class, isBackgroundRequest);
                            artist.setImage(image);
                        }
                        mCache.put(Artist.class, id, artist);
                    }

                    if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_TOPHITSANDALBUMS) {
                        JsonElement rawAlbums = get(o, "albums");
                        if (rawAlbums instanceof JsonObject && resultType == Album.class) {
                            results.addAll(storeRecords(
                                    (JsonObject) rawAlbums, resultType, isBackgroundRequest));
                        }

                        JsonElement rawTopHits = get(o, "topHits");
                        if (rawTopHits instanceof JsonObject && resultType == Query.class) {
                            List<Chart> chartItems = storeRecords((JsonObject) rawTopHits,
                                    Chart.class, isBackgroundRequest);
                            List<Query> topHits = new ArrayList<>();
                            if (chartItems != null && chartItems.size() > 0) {
                                for (ChartItem item : chartItems.get(0).getChartItems()) {
                                    topHits.add(item.getQuery());
                                }
                            }
                            results.addAll((List<T>) topHits);
                        }
                    }
                    if (resultType == Artist.class) {
                        results.add((T) artist);
                    }
                }
            }
        }
        elements = object.get("albums");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    Album album = mCache.get(Album.class, id);
                    if (album == null) {
                        String name = getAsString(o, "name");
                        String artistId = getAsString(o, "artist");
                        Artist artist = findRecord(artistId, Artist.class, isBackgroundRequest);
                        album = Album.get(name, artist);
                        JsonElement images = get(o, "images");
                        if (images instanceof JsonArray && ((JsonArray) images).size() > 0) {
                            String imageId = ((JsonArray) images).get(0).getAsString();
                            Image image = findRecord(imageId, Image.class, isBackgroundRequest);
                            album.setImage(image);
                        }
                        String releaseType = getAsString(o, "releaseType");
                        album.setReleaseType(releaseType);
                        mCache.put(Album.class, id, album);
                    }

                    if (requestType == InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS_TRACKS) {
                        JsonElement rawTracks = get(o, "tracks");
                        if (rawTracks instanceof JsonObject && resultType == Query.class) {
                            results.addAll(storeRecords((JsonObject) rawTracks, resultType,
                                    isBackgroundRequest));
                        }
                    }
                    if (resultType == Album.class) {
                        results.add((T) album);
                    }
                }
            }
        }
        elements = object.get("tracks");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    Query query = mCache.get(Query.class, id);
                    if (query == null) {
                        String name = getAsString(o, "name");
                        String artistId = getAsString(o, "artist");
                        Artist artist = findRecord(artistId, Artist.class, isBackgroundRequest);
                        query = Query.get(name, null, artist.getName(), false, true);
                        mCache.put(Query.class, id, query);
                    }
                    if (resultType == Query.class) {
                        results.add((T) query);
                    }
                }
            }
        }
        elements = object.get("users");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    User user = User.get(id);
                    String name = getAsString(o, "name");
                    user.setName(name);
                    String about = getAsString(o, "about");
                    user.setAbout(about);
                    int followersCount = getAsInt(o, "followersCount");
                    user.setFollowersCount(followersCount);
                    int followCount = getAsInt(o, "followCount");
                    user.setFollowCount(followCount);
                    String nowplayingId = getAsString(o, "nowplaying");
                    if (nowplayingId != null) {
                        Query nowplaying =
                                findRecord(nowplayingId, Query.class, isBackgroundRequest);
                        user.setNowPlaying(nowplaying);
                    }
                    String nowplayingtimestamp = getAsString(o, "nowplayingtimestamp");
                    user.setNowPlayingTimeStamp(ISO8601Utils.parse(nowplayingtimestamp));
                    String avatar = getAsString(o, "avatar");
                    if (avatar != null) {
                        Image image = findRecord(avatar, Image.class, isBackgroundRequest);
                        user.setImage(image);
                    }

                    if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS) {
                        JsonElement rawPlaylists = get(o, "playlists");
                        if (rawPlaylists instanceof JsonObject) {
                            List<Playlist> playlists = storeRecords(
                                    (JsonObject) rawPlaylists, Playlist.class, isBackgroundRequest);
                            Collections.sort(playlists, new PlaylistComparator());
                            user.setPlaylists(playlists);
                        }
                    } else if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS) {
                        JsonElement rawLovedItems = get(o, "lovedItems");
                        if (rawLovedItems instanceof JsonObject) {
                            List<Playlist> playlists = storeRecords((JsonObject) rawLovedItems,
                                    Playlist.class, isBackgroundRequest);
                            if (playlists != null && playlists.size() > 0) {
                                user.setFavorites(playlists.get(0));
                            }
                        }
                    } else if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDALBUMS) {
                        JsonElement rawLovedAlbums = get(o, "lovedAlbums");
                        if (rawLovedAlbums instanceof JsonObject) {
                            List<Relationship> relationships = storeRecords(
                                    (JsonObject) rawLovedAlbums, Relationship.class,
                                    isBackgroundRequest);
                            List<Album> albums = new ArrayList<>();
                            for (Relationship relationship : relationships) {
                                albums.add(relationship.getAlbum());
                            }
                            user.setStarredAlbums(albums);
                        }
                    } else if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDARTISTS) {
                        JsonElement rawLovedArtists = get(o, "lovedArtists");
                        if (rawLovedArtists instanceof JsonObject) {
                            List<Relationship> relationships = storeRecords(
                                    (JsonObject) rawLovedArtists, Relationship.class,
                                    isBackgroundRequest);
                            List<Artist> artists = new ArrayList<>();
                            for (Relationship relationship : relationships) {
                                artists.add(relationship.getArtist());
                            }
                            user.setStarredArtists(artists);
                        }
                    } else if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYBACKLOG) {
                        JsonElement rawPlaybackLog = get(o, "playbacklog");
                        if (rawPlaybackLog instanceof JsonObject) {
                            List<Playlist> playlists = storeRecords((JsonObject) rawPlaybackLog,
                                    Playlist.class, isBackgroundRequest);
                            if (playlists != null && playlists.size() > 0) {
                                user.setPlaybackLog(playlists.get(0));
                            }
                        }
                    } else if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_FOLLOWS
                            || requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_FOLLOWERS) {
                        boolean isFollows =
                                requestType == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_FOLLOWS;
                        JsonElement rawFollows = get(o, isFollows ? "follows" : "followers");
                        if (rawFollows instanceof JsonObject) {
                            JsonObject follows = (JsonObject) rawFollows;
                            storeRecords(follows, null, isBackgroundRequest);
                            JsonElement relationships = get(follows, "relationships");
                            if (relationships instanceof JsonArray) {
                                TreeMap<User, String> followsMap =
                                        new TreeMap<>(new AlphaComparator());
                                for (JsonElement relationship : (JsonArray) relationships) {
                                    JsonObject relationshipObj = (JsonObject) relationship;
                                    String relationshipId = getAsString(relationshipObj, "id");
                                    String userId = getAsString(relationshipObj,
                                            isFollows ? "targetUser" : "user");
                                    User followedUser =
                                            findRecord(userId, User.class, isBackgroundRequest);
                                    followsMap.put(followedUser, relationshipId);
                                }
                                if (isFollows) {
                                    user.setFollowings(followsMap);
                                } else {
                                    user.setFollowers(followsMap);
                                }
                            }
                        }
                    }
                    mCache.put(User.class, id, user);
                    if (resultType == User.class) {
                        results.add((T) user);
                    }
                }
            }
        }
        elements = object.get("playlistEntries");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    PlaylistEntry entry = mCache.get(PlaylistEntry.class, id);
                    if (entry == null) {
                        String trackId = getAsString(o, "track");
                        Query query = findRecord(trackId, Query.class, isBackgroundRequest);
                        String playlistId = getAsString(o, "playlist");
                        entry = PlaylistEntry.get(playlistId, query, id);
                        mCache.put(PlaylistEntry.class, id, entry);
                    }
                    if (resultType == PlaylistEntry.class) {
                        results.add((T) entry);
                    }
                }
            }
        }
        elements = object.get("playlists");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    String title = getAsString(o, "title");
                    String currentrevision = getAsString(o, "currentrevision");
                    Playlist playlist = null;
                    if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES) {
                        JsonElement rawEntries = get(o, "playlistEntries");
                        if (rawEntries instanceof JsonObject) {
                            List<PlaylistEntry> entries = storeRecords((JsonObject) rawEntries,
                                    PlaylistEntry.class, isBackgroundRequest);
                            if (entries != null) {
                                playlist = Playlist.fromEntryList(id, null, null, entries);
                                playlist.setFilled(true);
                            }
                        }
                    } else {
                        JsonElement entryIds = o.get("playlistEntries");
                        if (entryIds instanceof JsonArray) {
                            List<PlaylistEntry> entries = new ArrayList<>();
                            for (JsonElement entryId : (JsonArray) entryIds) {
                                PlaylistEntry entry = findRecord(entryId.getAsString(),
                                        PlaylistEntry.class, isBackgroundRequest);
                                entries.add(entry);
                            }
                            playlist = Playlist.fromEntryList(id, null, null, entries);
                            playlist.setFilled(true);
                        }
                    }
                    if (playlist == null) {
                        playlist = Playlist.get(id);
                    }
                    playlist.setName(title);
                    playlist.setCurrentRevision(currentrevision);
                    playlist.setHatchetId(id);
                    int entryCount = getAsInt(o, "entryCount");
                    playlist.setCount(entryCount);
                    String userId = getAsString(o, "user");
                    playlist.setUserId(userId);
                    JsonElement popularArtists = get(o, "popularArtists");
                    if (popularArtists instanceof JsonArray) {
                        ArrayList<String> topArtistNames = new ArrayList<>();
                        for (JsonElement popularArtist : (JsonArray) popularArtists) {
                            String artistId = popularArtist.getAsString();
                            Artist artist = findRecord(artistId, Artist.class, isBackgroundRequest);
                            if (artist != null) {
                                topArtistNames.add(artist.getName());
                            }
                        }
                        playlist.setTopArtistNames(
                                topArtistNames.toArray(new String[topArtistNames.size()]));
                    }
                    mCache.put(Playlist.class, id, playlist);
                    if (resultType == Playlist.class) {
                        results.add((T) playlist);
                    }
                }
            }
        }
        elements = object.get("playbacklogEntries");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    PlaybackLogEntry logEntry = mCache.get(PlaybackLogEntry.class, id);
                    if (logEntry == null) {
                        String trackId = getAsString(o, "track");
                        Query query = findRecord(trackId, Query.class, isBackgroundRequest);
                        String timestamp = getAsString(o, "timestamp");
                        Date date = ISO8601Utils.parse(timestamp);
                        logEntry = new PlaybackLogEntry(query, date);
                        mCache.put(PlaybackLogEntry.class, id, logEntry);
                    }
                    if (resultType == PlaybackLogEntry.class) {
                        results.add((T) logEntry);
                    }
                }
            }
        }
        elements = object.get("playbacklogs");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    JsonArray playbacklogEntries = get(o, "playbacklogEntries").getAsJsonArray();
                    ArrayList<PlaylistEntry> entries = new ArrayList<>();
                    for (JsonElement entry : playbacklogEntries) {
                        String entryId = entry.getAsString();
                        PlaybackLogEntry logEntry =
                                findRecord(entryId, PlaybackLogEntry.class, isBackgroundRequest);
                        PlaylistEntry e = PlaylistEntry.get(id, logEntry.getQuery(), entryId);
                        entries.add(e);
                    }
                    Playlist playlist = Playlist.fromEntryList(id, "Playbacklog", null, entries);
                    playlist.setHatchetId(id);
                    playlist.setFilled(true);
                    mCache.put(Playlist.class, id, playlist);
                    if (resultType == Playlist.class) {
                        results.add((T) playlist);
                    }
                }
            }
        }
        elements = object.get("socialActions");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    SocialAction socialAction = mCache.get(SocialAction.class, id);
                    if (socialAction == null) {
                        socialAction = SocialAction.get(id);
                        String action = getAsString(o, "action");
                        socialAction.setAction(action);
                        String date = getAsString(o, "date");
                        socialAction.setDate(ISO8601Utils.parse(date));
                        String actionType = getAsString(o, "type");
                        socialAction.setType(actionType);
                        String trackId = getAsString(o, "track");
                        if (trackId != null) {
                            Query query = findRecord(trackId, Query.class, isBackgroundRequest);
                            socialAction.setQuery(query);
                        }
                        String artistId = getAsString(o, "artist");
                        if (artistId != null) {
                            Artist artist = findRecord(artistId, Artist.class, isBackgroundRequest);
                            socialAction.setArtist(artist);
                        }
                        String albumId = getAsString(o, "album");
                        if (albumId != null) {
                            Album album = findRecord(albumId, Album.class, isBackgroundRequest);
                            socialAction.setAlbum(album);
                        }
                        String userId = getAsString(o, "user");
                        if (userId != null) {
                            User user = findRecord(userId, User.class, isBackgroundRequest);
                            socialAction.setUser(user);
                        }
                        String targetId = getAsString(o, "target");
                        if (targetId != null) {
                            User target = findRecord(targetId, User.class, isBackgroundRequest);
                            socialAction.setTarget(target);
                        }
                        String playlistId = getAsString(o, "playlist");
                        if (playlistId != null) {
                            Playlist playlist =
                                    findRecord(playlistId, Playlist.class, isBackgroundRequest);
                            socialAction.setPlaylist(playlist);
                        }
                        mCache.put(SocialAction.class, id, socialAction);
                    }
                    if (resultType == SocialAction.class) {
                        results.add((T) socialAction);
                    }
                }
            }
            if (params != null) {
                User user = findRecord(params.userid, User.class, false);
                if (user != null && resultType == SocialAction.class) {
                    if (HatchetInfoPlugin.HATCHET_SOCIALACTION_PARAMTYPE_FRIENDSFEED
                            .equals(params.type)) {
                        user.setFriendsFeed((List<SocialAction>) results, params.before_date);
                    } else {
                        user.setSocialActions((List<SocialAction>) results, params.before_date);
                    }
                }
            }
        }
        elements = object.get("searchResults");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    SearchResult searchResult = mCache.get(SearchResult.class, id);
                    if (searchResult == null) {
                        float score = getAsFloat(o, "score");
                        String trackId = getAsString(o, "track");
                        if (trackId != null) {
                            Query query = findRecord(trackId, Query.class, isBackgroundRequest);
                            searchResult = new SearchResult(score, query);
                        }
                        String artistId = getAsString(o, "artist");
                        if (artistId != null) {
                            Artist artist = findRecord(artistId, Artist.class, isBackgroundRequest);
                            searchResult = new SearchResult(score, artist);
                        }
                        String albumId = getAsString(o, "album");
                        if (albumId != null) {
                            Album album = findRecord(albumId, Album.class, isBackgroundRequest);
                            searchResult = new SearchResult(score, album);
                        }
                        String userId = getAsString(o, "user");
                        if (userId != null) {
                            User user = findRecord(userId, User.class, isBackgroundRequest);
                            searchResult = new SearchResult(score, user);
                        }
                        String playlistId = getAsString(o, "playlist");
                        if (playlistId != null) {
                            Playlist playlist =
                                    findRecord(playlistId, Playlist.class, isBackgroundRequest);
                            searchResult = new SearchResult(score, playlist);
                        }
                        if (searchResult == null) {
                            throw new IOException(
                                    "searchResult contained no actual result object!");
                        }
                        mCache.put(SearchResult.class, id, searchResult);
                    }
                    if (resultType == SearchResult.class) {
                        results.add((T) searchResult);
                    }
                }
            }
        }
        elements = object.get("searches");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    JsonArray rawSearchResults = get(o, "searchResults").getAsJsonArray();
                    ArrayList<SearchResult> searchResults = new ArrayList<>();
                    for (JsonElement rawSearchResult : rawSearchResults) {
                        String resultId = rawSearchResult.getAsString();
                        SearchResult searchResult =
                                findRecord(resultId, SearchResult.class, isBackgroundRequest);
                        searchResults.add(searchResult);
                    }
                    Search search = new Search(searchResults);
                    mCache.put(Search.class, id, search);
                    if (resultType == Search.class) {
                        results.add((T) search);
                    }
                }
            }
        }
        elements = object.get("relationships");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    String type = getAsString(o, "type");
                    if (type.equals(HatchetInfoPlugin.HATCHET_RELATIONSHIPS_TYPE_LOVE)) {
                        String userId = getAsString(o, "user");
                        User user = findRecord(userId, User.class, isBackgroundRequest);
                        String dateString = getAsString(o, "date");
                        Date date = null;
                        if (dateString != null) {
                            date = ISO8601Utils.parse(dateString);
                        }
                        Relationship relationship = Relationship.get(id, type, user, date);
                        String targetId;
                        if ((targetId = getAsString(o, "targetTrack")) != null) {
                            Query query = findRecord(targetId, Query.class, isBackgroundRequest);
                            relationship.setQuery(query);
                            user.putRelationship(query, relationship);
                        } else if ((targetId = getAsString(o, "targetAlbum")) != null) {
                            Album album = findRecord(targetId, Album.class, isBackgroundRequest);
                            relationship.setAlbum(album);
                            user.putRelationship(album, relationship);
                        } else if ((targetId = getAsString(o, "targetArtist")) != null) {
                            Artist artist =
                                    findRecord(targetId, Artist.class, isBackgroundRequest);
                            relationship.setArtist(artist);
                            user.putRelationship(artist, relationship);
                        }
                        mCache.put(Relationship.class, id, relationship);
                        if (resultType == Relationship.class) {
                            results.add((T) relationship);
                        }
                    }
                }
            }
        }
        elements = object.get("chartItems");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    ChartItem item = mCache.get(ChartItem.class, id);
                    if (item == null) {
                        String trackid = getAsString(o, "track");
                        Query query = findRecord(trackid, Query.class, isBackgroundRequest);
                        int plays = getAsInt(o, "plays");
                        int listeners = getAsInt(o, "listeners");
                        item = new ChartItem(query, plays, listeners);
                        mCache.put(ChartItem.class, id, item);
                    }
                    if (resultType == ChartItem.class) {
                        results.add((T) item);
                    }
                }
            }
        }
        elements = object.get("chart");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    Chart chart = mCache.get(Chart.class, id);
                    if (chart == null) {
                        List<ChartItem> items = new ArrayList<>();
                        JsonElement chartItems = get(o, "chartItems");
                        if (chartItems instanceof JsonArray) {
                            for (JsonElement chartItemid : (JsonArray) chartItems) {
                                String itemId = chartItemid.getAsString();
                                ChartItem chartItem =
                                        findRecord(itemId, ChartItem.class, isBackgroundRequest);
                                items.add(chartItem);
                            }
                        }
                        chart = new Chart(items);
                        mCache.put(Chart.class, id, chart);
                    }
                    if (resultType == Chart.class) {
                        results.add((T) chart);
                    }
                }
            }
        }
        return results;
    }

    public int getAsInt(JsonObject object, String memberName) throws IOException {
        JsonElement element = get(object, memberName);
        if (element != null && element.isJsonPrimitive()) {
            return element.getAsInt();
        }
        return -1;
    }

    public float getAsFloat(JsonObject object, String memberName) throws IOException {
        JsonElement element = get(object, memberName);
        if (element != null && element.isJsonPrimitive()) {
            return element.getAsFloat();
        }
        return -1;
    }

    public String getAsString(JsonObject object, String memberName) throws IOException {
        JsonElement element = get(object, memberName);
        if (element != null && element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return null;
    }

    public JsonElement get(JsonObject object, String memberName) throws IOException {
        JsonElement element = object.get(memberName);
        if (element == null) {
            JsonObject links = object.getAsJsonObject("links");
            if (links != null && links.has(memberName)) {
                Request request = new Request.Builder()
                        .url(HATCHET_BASE_URL + links.get(memberName).getAsString())
                        .build();
                Log.d(TAG, "following link: " + request.urlString());
                Response response = mOkHttpClient.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("API request with URL '" + request.urlString()
                            + "' not successful. Code was " + response.code());
                }
                try {
                    element = GsonHelper.get().fromJson(
                            response.body().charStream(), JsonElement.class);
                } catch (JsonIOException | JsonSyntaxException e) {
                    throw new IOException(e);
                } finally {
                    response.body().close();
                }
            }
        }
        return element;
    }
}