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
package org.tomahawk.libtomahawk.infosystem.hatchet;

import com.google.gson.JsonObject;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;

import org.apache.commons.io.Charsets;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.HatchetCollection;
import org.tomahawk.libtomahawk.infosystem.InfoPlugin;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.QueryParams;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistEntries;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.GsonHelper;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.android.MainThreadExecutor;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedByteArray;

import static android.os.Process.THREAD_PRIORITY_LOWEST;

/**
 * Implementation to enable the InfoSystem to retrieve data from the Hatchet API. Documentation of
 * the API can be found here https://api.hatchet.is/apidocs/
 */
public class HatchetInfoPlugin implements InfoPlugin {

    private final static String TAG = HatchetInfoPlugin.class.getSimpleName();

    public static final String HATCHET_BASE_URL = "https://api.hatchet.is";

    public static final String HATCHET_API_VERSION = "/v2";

    public static final String HATCHET_SEARCHITEM_TYPE_ALBUM = "album";

    public static final String HATCHET_SEARCHITEM_TYPE_ARTIST = "artist";

    public static final String HATCHET_SEARCHITEM_TYPE_USER = "user";

    public static final String HATCHET_SOCIALACTION_PARAMTYPE_FRIENDSFEED = "friendsFeed";

    public static final String HATCHET_SOCIALACTION_TYPE_LOVE = "love";

    public static final String HATCHET_SOCIALACTION_TYPE_FOLLOW = "follow";

    public static final String HATCHET_SOCIALACTION_TYPE_CREATECOMMENT = "createcomment";

    public static final String HATCHET_SOCIALACTION_TYPE_LATCHON = "latchOn";

    public static final String HATCHET_SOCIALACTION_TYPE_LATCHOFF = "latchOff";

    public static final String HATCHET_SOCIALACTION_TYPE_CREATEPLAYLIST = "createplaylist";

    public static final String HATCHET_SOCIALACTION_TYPE_DELETEPLAYLIST = "deleteplaylist";

    public static final String HATCHET_RELATIONSHIPS_TYPE_FOLLOW = "follow";

    public static final String HATCHET_RELATIONSHIPS_TYPE_LOVE = "love";

    public static final String HATCHET_RELATIONSHIPS_TARGETTYPE_ALBUM = "album";

    public static final String HATCHET_RELATIONSHIPS_TARGETTYPE_ARTIST = "artist";

    public static final int SOCIALACTIONS_LIMIT = 20;

    public static final int FRIENDSFEED_LIMIT = 50;

    private HatchetAuthenticatorUtils mHatchetAuthenticatorUtils;

    private final OkHttpClient mOkHttpClient;

    private final Hatchet mHatchet;

    private final Hatchet mHatchetBackground;

    private final Store mStore;

    public HatchetInfoPlugin() {
        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                if (!TomahawkUtils.isNetworkAvailable()) {
                    int maxStale = 60 * 60 * 24 * 7; // tolerate 1-week stale
                    request.addHeader("Cache-Control", "public, max-stale=" + maxStale);
                }
                request.addHeader("Content-type", "application/json; charset=utf-8");
            }
        };
        mOkHttpClient = new OkHttpClient();
        File cacheDir = new File(TomahawkApp.getContext().getCacheDir(), "responseCache");
        try {
            Cache cache = new Cache(cacheDir, 1024 * 1024 * 20);
            mOkHttpClient.setCache(cache);
        } catch (IOException e) {
            Log.e(TAG, "<init>: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
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

        mStore = new Store(mOkHttpClient, mHatchet);
    }

    private Hatchet getImplementation(InfoRequestData infoRequestData) {
        return infoRequestData.isBackgroundRequest() ? mHatchetBackground : mHatchet;
    }

    /**
     * _fetch_ data from the Hatchet API (e.g. artist's top-hits, image etc.)
     */
    public void resolve(final InfoRequestData infoRequestData) {
        int priority;
        if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_TOPHITS) {
            priority = TomahawkRunnable.PRIORITY_IS_INFOSYSTEM_HIGH;
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS
                || infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS) {
            priority = TomahawkRunnable.PRIORITY_IS_INFOSYSTEM_LOW;
        } else {
            priority = TomahawkRunnable.PRIORITY_IS_INFOSYSTEM_MEDIUM;
        }
        TomahawkRunnable runnable = new TomahawkRunnable(priority) {
            @Override
            public void run() {
                try {
                    boolean success = getParseConvert(infoRequestData);
                    InfoSystem.getInstance().reportResults(infoRequestData, success);
                } catch (KeyManagementException | NoSuchAlgorithmException | IOException e) {
                    Log.e(TAG, "resolve: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
            }
        };
        ThreadManager.getInstance().execute(runnable);
    }

    /**
     * Core method of this InfoPlugin. Gets and parses the ordered results.
     *
     * @param infoRequestData InfoRequestData object containing the input parameters.
     * @return true if the type of the given InfoRequestData was valid and could be processed. false
     * otherwise
     */
    private boolean getParseConvert(InfoRequestData infoRequestData)
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        QueryParams params = infoRequestData.getQueryParams();
        HatchetCollection hatchetCollection = (HatchetCollection) CollectionManager.getInstance()
                .getCollection(TomahawkApp.PLUGINNAME_HATCHET);
        Hatchet hatchet = getImplementation(infoRequestData);

        try {
            int type = infoRequestData.getType();
            if (type >= InfoRequestData.INFOREQUESTDATA_TYPE_USERS
                    && type < InfoRequestData.INFOREQUESTDATA_TYPE_USERS + 100) {
                JsonObject object =
                        hatchet.getUsers(params.ids, params.name, params.random, params.count);
                List users = mStore.storeRecords(object, Store.TYPE_USERS, type);
                infoRequestData.setResultList(users);
                return true;

            } else if (type >= InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS
                    && type < InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS + 100) {
                JsonObject object =
                        hatchet.getUsers(params.ids, params.name, params.random, params.count);
                List playlists = mStore.storeRecords(object, Store.TYPE_PLAYLISTS, type);
                infoRequestData.setResultList(playlists);
                return true;

            } else if (type == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS) {
                JsonObject object = hatchet.getArtists(params.ids, params.name);
                List artists = mStore.storeRecords(object, Store.TYPE_ARTISTS, type);
                for (Object artist : artists) {
                    hatchetCollection.addArtist((Artist) artist);
                }
                infoRequestData.setResultList(artists);
                return true;

            } else if (type == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_ALBUMS) {
                JsonObject object = hatchet.getArtists(params.ids, params.name);
                List albums = mStore.storeRecords(object, Store.TYPE_ALBUMS, type);
                if (albums.size() > 0) {
                    for (Object albumObject : albums) {
                        Album album = (Album) albumObject;
                        hatchetCollection.addAlbum(album);
                    }
                    Album firstAlbum = (Album) albums.get(0);
                    hatchetCollection.addArtistAlbums(firstAlbum.getArtist(), albums);
                }
                infoRequestData.setResultList(albums);
                return true;

            } else if (type == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_TOPHITS) {
                JsonObject object = hatchet.getArtists(params.ids, params.name);
                List topHits = mStore.storeRecords(object, Store.TYPE_TRACKS, type);
                if (topHits.size() > 0) {
                    Query firstTopHit = (Query) topHits.get(0);
                    hatchetCollection.addArtistTopHits(firstTopHit.getArtist(), topHits);
                }
                infoRequestData.setResultList(topHits);
                return true;

            } else if (type == InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS) {
                JsonObject object = hatchet.getAlbums(params.ids, params.name, params.artistname);
                List albums = mStore.storeRecords(object, Store.TYPE_ALBUMS, type);
                for (Object albumObject : albums) {
                    Album album = (Album) albumObject;
                    hatchetCollection.addAlbum(album);
                }
                infoRequestData.setResultList(albums);
                return true;

            } else if (type == InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS_TRACKS) {
                JsonObject object = hatchet.getAlbums(params.ids, params.name, params.artistname);
                List tracks = mStore.storeRecords(object, Store.TYPE_TRACKS, type);
                Artist artist = Artist.get(params.artistname);
                Album album = Album.get(params.name, artist);
                hatchetCollection.addAlbumTracks(album, tracks);
                infoRequestData.setResultList(tracks);
                return true;

            } else if (type == InfoRequestData.INFOREQUESTDATA_TYPE_SEARCHES) {
                JsonObject object = hatchet.getSearches(params.term);
                List searches = mStore.storeRecords(object, Store.TYPE_SEARCHES, type);
                infoRequestData.setResultList(searches);
                return true;

            } else if (type == InfoRequestData.INFOREQUESTDATA_TYPE_SOCIALACTIONS) {
                JsonObject object = hatchet.getSocialActions(null, params.userid, params.type,
                        params.offset, params.limit);
                List socialActions = mStore.storeRecords(object, Store.TYPE_SOCIALACTIONS, type);
                infoRequestData.setResultList(socialActions);
                return true;
            }
        } catch (RetrofitError e) {
            Log.e(TAG, "getParseConvert: Request to " + e.getUrl() + " failed: " + e.getClass()
                    + ": " + e.getLocalizedMessage());
        }
        return false;
    }

    /**
     * Start the JSONSendTask to send the given InfoRequestData's json string
     */
    @Override
    public void send(final InfoRequestData infoRequestData, AuthenticatorUtils authenticatorUtils) {
        mHatchetAuthenticatorUtils = (HatchetAuthenticatorUtils) authenticatorUtils;
        TomahawkRunnable runnable = new TomahawkRunnable(
                TomahawkRunnable.PRIORITY_IS_INFOSYSTEM_MEDIUM) {
            @Override
            public void run() {
                ArrayList<String> doneRequestsIds = new ArrayList<>();
                doneRequestsIds.add(infoRequestData.getRequestId());
                Hatchet hatchet = getImplementation(infoRequestData);
                // Before we do anything, get the accesstoken
                boolean success = false;
                String accessToken = mHatchetAuthenticatorUtils.ensureAccessTokens();
                if (accessToken != null) {
                    String data = infoRequestData.getJsonStringToSend();
                    try {
                        if (infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES) {
                            hatchet.postPlaybackLogEntries(accessToken,
                                    new TypedByteArray("application/json; charset=utf-8",
                                            data.getBytes(Charsets.UTF_8)));
                        } else if (infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES_NOWPLAYING) {
                            hatchet.postPlaybackLogEntriesNowPlaying(accessToken,
                                    new TypedByteArray("application/json; charset=utf-8",
                                            data.getBytes(Charsets.UTF_8)));
                        } else if (infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_SOCIALACTIONS) {
                            hatchet.postSocialActions(accessToken,
                                    new TypedByteArray("application/json; charset=utf-8",
                                            data.getBytes(Charsets.UTF_8)));
                        } else if (infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS) {
                            if (infoRequestData.getHttpType()
                                    == InfoRequestData.HTTPTYPE_POST) {
                                HatchetPlaylistEntries entries = hatchet.postPlaylists(accessToken,
                                        new TypedByteArray("application/json; charset=utf-8",
                                                data.getBytes(Charsets.UTF_8)));
                                List<HatchetPlaylistEntries> results = new ArrayList<>();
                                results.add(entries);
                                infoRequestData.setResultList(results);
                            } else if (infoRequestData.getHttpType()
                                    == InfoRequestData.HTTPTYPE_DELETE) {
                                hatchet.deletePlaylists(accessToken,
                                        infoRequestData.getQueryParams().playlist_id);
                            } else if (infoRequestData.getHttpType()
                                    == InfoRequestData.HTTPTYPE_PUT) {
                                hatchet.putPlaylists(accessToken,
                                        infoRequestData.getQueryParams().playlist_id,
                                        new TypedByteArray("application/json; charset=utf-8",
                                                data.getBytes(Charsets.UTF_8)));
                            }
                        } else if (infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES) {
                            if (infoRequestData.getHttpType()
                                    == InfoRequestData.HTTPTYPE_POST) {
                                hatchet.postPlaylistsPlaylistEntries(accessToken,
                                        infoRequestData.getQueryParams().playlist_id,
                                        new TypedByteArray("application/json; charset=utf-8",
                                                data.getBytes(Charsets.UTF_8)));
                            } else if (infoRequestData.getHttpType()
                                    == InfoRequestData.HTTPTYPE_DELETE) {
                                hatchet.deletePlaylistsPlaylistEntries(accessToken,
                                        infoRequestData.getQueryParams().playlist_id,
                                        infoRequestData.getQueryParams().entry_id);
                            }
                        } else if (infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS) {
                            if (infoRequestData.getHttpType()
                                    == InfoRequestData.HTTPTYPE_POST) {
                                hatchet.postRelationship(accessToken,
                                        new TypedByteArray("application/json; charset=utf-8",
                                                data.getBytes(Charsets.UTF_8)));
                            } else if (infoRequestData.getHttpType()
                                    == InfoRequestData.HTTPTYPE_DELETE) {
                                hatchet.deleteRelationShip(accessToken,
                                        infoRequestData.getQueryParams().relationship_id);
                            }
                        }
                        success = true;
                    } catch (RetrofitError e) {
                        Log.e(TAG, "send: Request to " + e.getUrl() + " failed: " + e.getClass()
                                + ": " + e.getLocalizedMessage());
                    }
                }
                InfoSystem.getInstance().onLoggedOpsSent(doneRequestsIds, success);
                InfoSystem.getInstance().reportResults(infoRequestData, success);
            }
        };
        ThreadManager.getInstance().execute(runnable);
    }
}
