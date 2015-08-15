package org.tomahawk.libtomahawk.infosystem.hatchet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.AlphaComparator;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.ListItemString;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.GsonHelper;
import org.tomahawk.libtomahawk.utils.ISO8601Utils;

import android.util.SparseArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class Store {

    public static final int TYPE_IMAGES = 0;

    public static final int TYPE_ARTISTS = 1;

    public static final int TYPE_ALBUMS = 2;

    public static final int TYPE_TRACKS = 3;

    public static final int TYPE_USERS = 4;

    public static final int TYPE_PLAYLISTENTRIES = 5;

    public static final int TYPE_PLAYLISTS = 6;

    public static final int TYPE_SOCIALACTIONS = 7;

    public static final int TYPE_SEARCHES = 8;

    public static final int TYPE_SEARCHRESULTS = 9;

    private final SparseArray<Map> mCache = new SparseArray<>();

    private OkHttpClient mOkHttpClient;

    private Hatchet mHatchet;

    public Store(OkHttpClient okHttpClient, Hatchet hatchet) {
        mOkHttpClient = okHttpClient;
        mHatchet = hatchet;
        mCache.put(TYPE_IMAGES, new ConcurrentHashMap<String, Image>());
        mCache.put(TYPE_ARTISTS, new ConcurrentHashMap<String, Artist>());
        mCache.put(TYPE_ALBUMS, new ConcurrentHashMap<String, Album>());
        mCache.put(TYPE_TRACKS, new ConcurrentHashMap<String, Query>());
        mCache.put(TYPE_USERS, new ConcurrentHashMap<String, User>());
        mCache.put(TYPE_PLAYLISTENTRIES, new ConcurrentHashMap<String, Query>());
        mCache.put(TYPE_PLAYLISTS, new ConcurrentHashMap<String, Playlist>());
        mCache.put(TYPE_SOCIALACTIONS, new ConcurrentHashMap<String, SocialAction>());
        mCache.put(TYPE_SEARCHES, new ConcurrentHashMap<String, Search>());
        mCache.put(TYPE_SEARCHRESULTS, new ConcurrentHashMap<String, SearchResult>());
    }

    public Object findRecord(String id, int resultType) throws IOException {
        Map cache = mCache.get(resultType);
        if (cache == null) {
            throw new IOException("Couldn't find cache for given type.");
        }
        Object record = cache.get(id);
        if (record == null) {
            List<String> ids = new ArrayList<>();
            ids.add(String.valueOf(id));
            if (resultType == TYPE_IMAGES) {
                storeRecords(mHatchet.getImages(ids), resultType);
            } else if (resultType == TYPE_ARTISTS) {
                storeRecords(mHatchet.getArtists(ids, null), resultType);
            } else if (resultType == TYPE_ALBUMS) {
                storeRecords(mHatchet.getAlbums(ids, null, null), resultType);
            } else if (resultType == TYPE_TRACKS) {
                storeRecords(mHatchet.getTracks(ids, null, null), resultType);
            } else if (resultType == TYPE_USERS) {
                storeRecords(mHatchet.getUsers(ids, null, null, null), resultType);
            } else if (resultType == TYPE_PLAYLISTENTRIES) {
                throw new IOException(
                        "Can't fetch playlist entry. There's no endpoint for that :(");
            } else if (resultType == TYPE_PLAYLISTS) {
                storeRecords(mHatchet.getPlaylists(ids), resultType);
            } else if (resultType == TYPE_SOCIALACTIONS) {
                throw new IOException("Can't fetch social action. There's no endpoint for that :(");
            }
            record = cache.get(id);
            if (record == null) {
                throw new IOException("Couldn't fetch entity from server.");
            }
            cache.put(id, record);
        }
        return record;
    }

    public List storeRecords(JsonObject object, int resultType) throws IOException {
        return storeRecords(object, resultType, -1);
    }

    public List storeRecords(JsonObject object, int resultType, int requestType)
            throws IOException {
        List results = new ArrayList();
        JsonElement elements = object.get("images");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = getAsString(o, "id");
                    String url = getAsString(o, "url");
                    int width = getAsInt(o, "width");
                    int height = getAsInt(o, "height");
                    Image image = Image.get(url, true, width, height);
                    mCache.get(TYPE_IMAGES).put(id, image);
                    if (resultType == TYPE_IMAGES) {
                        results.add(image);
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
                    String name = getAsString(o, "name");
                    String wiki = getAsString(o, "wikiabstract");
                    Artist artist = Artist.get(name);
                    artist.setBio(new ListItemString(wiki));
                    JsonElement images = get(o, "images");
                    if (images instanceof JsonArray && ((JsonArray) images).size() > 0) {
                        String imageId = ((JsonArray) images).get(0).getAsString();
                        Image image = (Image) findRecord(imageId, TYPE_IMAGES);
                        artist.setImage(image);
                    }

                    if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_ALBUMS) {
                        JsonElement rawAlbums = get(o, "albums");
                        if (rawAlbums instanceof JsonObject) {
                            results.addAll(storeRecords((JsonObject) rawAlbums, TYPE_ALBUMS));
                        }
                    } else if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_TOPHITS) {
                        JsonElement rawTopHits = get(o, "topHits");
                        if (rawTopHits instanceof JsonObject) {
                            results.addAll(storeRecords((JsonObject) rawTopHits, TYPE_TRACKS));
                        }
                    }
                    mCache.get(TYPE_ARTISTS).put(id, artist);
                    if (resultType == TYPE_ARTISTS) {
                        results.add(artist);
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
                    String name = getAsString(o, "name");
                    String artistId = getAsString(o, "artist");
                    Artist artist = (Artist) findRecord(artistId, TYPE_ARTISTS);
                    Album album = Album.get(name, artist);
                    JsonElement images = get(o, "images");
                    if (images instanceof JsonArray && ((JsonArray) images).size() > 0) {
                        String imageId = ((JsonArray) images).get(0).getAsString();
                        Image image = (Image) findRecord(imageId, TYPE_IMAGES);
                        album.setImage(image);
                    }

                    if (requestType == InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS_TRACKS) {
                        JsonElement rawTracks = get(o, "tracks");
                        if (rawTracks instanceof JsonObject) {
                            results.addAll(storeRecords((JsonObject) rawTracks, TYPE_TRACKS));
                        }
                    }
                    mCache.get(TYPE_ALBUMS).put(id, album);
                    if (resultType == TYPE_ALBUMS) {
                        results.add(album);
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
                    String name = getAsString(o, "name");
                    String artistId = getAsString(o, "artist");
                    Artist artist = (Artist) findRecord(artistId, TYPE_ARTISTS);
                    Query query = Query.get(name, null, artist.getName(), false, true);
                    mCache.get(TYPE_TRACKS).put(id, query);
                    if (resultType == TYPE_TRACKS) {
                        results.add(query);
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
                        Query nowplaying = (Query) findRecord(nowplayingId, TYPE_TRACKS);
                        user.setNowPlaying(nowplaying);
                    }
                    String nowplayingtimestamp = getAsString(o, "nowplayingtimestamp");
                    user.setNowPlayingTimeStamp(ISO8601Utils.parse(nowplayingtimestamp));

                    if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS) {
                        JsonElement rawPlaylists = get(o, "playlists");
                        if (rawPlaylists instanceof JsonObject) {
                            List playlists =
                                    storeRecords((JsonObject) rawPlaylists, TYPE_PLAYLISTS);
                            user.setPlaylists(playlists);
                        }
                    } else if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS) {
                        JsonElement rawLovedItems = get(o, "lovedItems");
                        if (rawLovedItems instanceof JsonObject) {
                            List playlists =
                                    storeRecords((JsonObject) rawLovedItems, TYPE_PLAYLISTS);
                            if (playlists != null && playlists.size() > 0) {
                                user.setFavorites((Playlist) playlists.get(0));
                            }
                        }
                    } else if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDALBUMS) {
                        JsonElement rawLovedAlbums = get(o, "lovedAlbums");
                        if (rawLovedAlbums instanceof JsonObject) {
                            List albums =
                                    storeRecords((JsonObject) rawLovedAlbums, TYPE_ALBUMS);
                            user.setStarredAlbums(albums);
                        }
                    } else if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDARTISTS) {
                        JsonElement rawLovedArtists = get(o, "lovedArtists");
                        if (rawLovedArtists instanceof JsonObject) {
                            List artists =
                                    storeRecords((JsonObject) rawLovedArtists, TYPE_ARTISTS);
                            user.setStarredArtists(artists);
                        }
                    } else if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYBACKLOG) {
                        JsonElement rawPlaybackLog = get(o, "playbacklog");
                        if (rawPlaybackLog instanceof JsonObject) {
                            List playlists =
                                    storeRecords((JsonObject) rawPlaybackLog, TYPE_PLAYLISTS);
                            if (playlists != null && playlists.size() > 0) {
                                user.setPlaybackLog((Playlist) playlists.get(0));
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
                            storeRecords(follows, -1);
                            JsonElement relationships = get(follows, "relationships");
                            if (relationships instanceof JsonArray) {
                                TreeMap<User, String> followsMap =
                                        new TreeMap<>(new AlphaComparator());
                                for (JsonElement relationship : (JsonArray) relationships) {
                                    JsonObject relationshipObj = (JsonObject) relationship;
                                    String relationshipId = getAsString(relationshipObj, "id");
                                    String userId = getAsString(relationshipObj,
                                            isFollows ? "targetUser" : "user");
                                    User followedUser = (User) findRecord(userId, TYPE_USERS);
                                    followsMap.put(followedUser, relationshipId);
                                }
                                if (isFollows) {
                                    user.setFollowings(followsMap);
                                } else {
                                    user.setFollowers(followsMap);
                                }
                            }
                        }
                    } else if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYBACKLOG) {
                        JsonElement rawPlaybackLog = get(o, "playbacklog");
                        if (rawPlaybackLog instanceof JsonObject) {
                            List playlists =
                                    storeRecords((JsonObject) rawPlaybackLog, TYPE_PLAYLISTS);
                            if (playlists != null && playlists.size() > 0) {
                                user.setPlaybackLog((Playlist) playlists.get(0));
                            }
                        }
                    }
                    mCache.get(TYPE_USERS).put(id, user);
                    if (resultType == TYPE_USERS) {
                        results.add(user);
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
                    String trackId = getAsString(o, "track");
                    Query query = (Query) findRecord(trackId, TYPE_TRACKS);
                    mCache.get(TYPE_PLAYLISTENTRIES).put(id, query);
                    if (resultType == TYPE_PLAYLISTENTRIES) {
                        results.add(query);
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
                    Playlist playlist =
                            Playlist.fromQueryList(title, currentrevision, new ArrayList<Query>());
                    playlist.setHatchetId(id);
                    if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES) {
                        JsonElement rawEntries = get(o, "playlistEntries");
                        if (rawEntries instanceof JsonObject) {
                            storeRecords((JsonObject) rawEntries, -1);
                            JsonElement entryIds = get((JsonObject) rawEntries, "playlistEntries");
                            if (entryIds instanceof JsonArray) {
                                playlist.setEntries(new ArrayList<PlaylistEntry>());
                                for (JsonElement entry : (JsonArray) entryIds) {
                                    String entryId = entry.getAsString();
                                    Query query = (Query) findRecord(entryId, TYPE_PLAYLISTENTRIES);
                                    playlist.addQuery(query);
                                }
                            }
                        }
                    }
                    mCache.get(TYPE_PLAYLISTS).put(id, playlist);
                    if (resultType == TYPE_PLAYLISTS) {
                        results.add(playlist);
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
                    String trackId = getAsString(o, "track");
                    Query query = (Query) findRecord(trackId, TYPE_TRACKS);
                    mCache.get(TYPE_PLAYLISTENTRIES).put(id, query);
                    if (resultType == TYPE_PLAYLISTENTRIES) {
                        results.add(query);
                    }
                }
            }
        }
        elements = object.get("playbacklog");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) elements;
                    String id = getAsString(o, "id");
                    JsonArray playbacklogEntries = get(o, "playbacklogEntries").getAsJsonArray();
                    ArrayList<Query> queries = new ArrayList<>();
                    for (JsonElement entry : playbacklogEntries) {
                        String entryId = entry.getAsString();
                        Query query = (Query) findRecord(entryId, TYPE_TRACKS);
                        queries.add(query);
                    }
                    Playlist playlist = Playlist.fromQueryList("Playbacklog", null, queries);
                    playlist.setHatchetId(id);
                    mCache.get(TYPE_PLAYLISTS).put(id, playlist);
                    if (resultType == TYPE_PLAYLISTS) {
                        results.add(playlist);
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
                    SocialAction socialAction = SocialAction.get(id);
                    String action = getAsString(o, "action");
                    socialAction.setAction(action);
                    String date = getAsString(o, "date");
                    socialAction.setDate(ISO8601Utils.parse(date));
                    String actionType = getAsString(o, "type");
                    socialAction.setType(actionType);
                    String trackId = getAsString(o, "track");
                    if (trackId != null) {
                        Query query = (Query) findRecord(trackId, TYPE_TRACKS);
                        socialAction.setQuery(query);
                    }
                    String artistId = getAsString(o, "artist");
                    if (artistId != null) {
                        Artist artist = (Artist) findRecord(artistId, TYPE_ARTISTS);
                        socialAction.setArtist(artist);
                    }
                    String albumId = getAsString(o, "album");
                    if (albumId != null) {
                        Album album = (Album) findRecord(albumId, TYPE_ALBUMS);
                        socialAction.setAlbum(album);
                    }
                    String userId = getAsString(o, "user");
                    if (userId != null) {
                        User user = (User) findRecord(userId, TYPE_USERS);
                        socialAction.setUser(user);
                    }
                    String targetId = getAsString(o, "target");
                    if (targetId != null) {
                        User target = (User) findRecord(targetId, TYPE_USERS);
                        socialAction.setTarget(target);
                    }
                    String playlistId = getAsString(o, "playlist");
                    if (playlistId != null) {
                        Playlist playlist = (Playlist) findRecord(playlistId, TYPE_PLAYLISTS);
                        socialAction.setPlaylist(playlist);
                    }
                    mCache.get(TYPE_SOCIALACTIONS).put(id, socialAction);
                    if (resultType == TYPE_SOCIALACTIONS) {
                        results.add(socialAction);
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
                    float score = getAsFloat(o, "score");
                    SearchResult searchResult = null;
                    String trackId = getAsString(o, "track");
                    if (trackId != null) {
                        Query query = (Query) findRecord(trackId, TYPE_TRACKS);
                        searchResult = new SearchResult(score, query);
                    }
                    String artistId = getAsString(o, "artist");
                    if (artistId != null) {
                        Artist artist = (Artist) findRecord(artistId, TYPE_ARTISTS);
                        searchResult = new SearchResult(score, artist);
                    }
                    String albumId = getAsString(o, "album");
                    if (albumId != null) {
                        Album album = (Album) findRecord(albumId, TYPE_ALBUMS);
                        searchResult = new SearchResult(score, album);
                    }
                    String userId = getAsString(o, "user");
                    if (userId != null) {
                        User user = (User) findRecord(userId, TYPE_USERS);
                        searchResult = new SearchResult(score, user);
                    }
                    String playlistId = getAsString(o, "playlist");
                    if (playlistId != null) {
                        Playlist playlist = (Playlist) findRecord(playlistId, TYPE_PLAYLISTS);
                        searchResult = new SearchResult(score, playlist);
                    }
                    if (searchResult == null) {
                        throw new IOException("searchResult contained no actual result object!");
                    }
                    mCache.get(TYPE_SEARCHRESULTS).put(id, searchResult);
                    if (resultType == TYPE_SEARCHRESULTS) {
                        results.add(searchResult);
                    }
                }
            }
        }
        elements = object.get("searches");
        if (elements instanceof JsonArray) {
            for (JsonElement element : (JsonArray) elements) {
                if (element instanceof JsonObject) {
                    JsonObject o = (JsonObject) element;
                    String id = get(o, "id").getAsString();
                    JsonArray rawSearchResults = get(o, "searchResults").getAsJsonArray();
                    ArrayList<SearchResult> searchResults = new ArrayList<>();
                    for (JsonElement rawSearchResult : rawSearchResults) {
                        String resultId = rawSearchResult.getAsString();
                        SearchResult searchResult =
                                (SearchResult) findRecord(resultId, TYPE_SEARCHRESULTS);
                        searchResults.add(searchResult);
                    }
                    Search search = new Search(searchResults);
                    mCache.get(TYPE_SEARCHES).put(id, search);
                    if (resultType == TYPE_SEARCHES) {
                        results.add(search);
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
                        .url(HatchetInfoPlugin.HATCHET_BASE_URL
                                + links.get(memberName).getAsString())
                        .build();
                Response response = mOkHttpClient.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("API request with URL '" + request.urlString()
                            + "' not successful. Code was " + response.code());
                }
                element =
                        GsonHelper.get().fromJson(response.body().charStream(), JsonElement.class);
            }
        }
        return element;
    }
}