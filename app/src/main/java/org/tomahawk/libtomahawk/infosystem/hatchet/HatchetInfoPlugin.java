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

import org.apache.commons.io.Charsets;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.HatchetCollection;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.infosystem.InfoPlugin;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.QueryParams;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistEntries;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.ISO8601Utils;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit.RetrofitError;
import retrofit.mime.TypedByteArray;

/**
 * Implementation to enable the InfoSystem to retrieve data from the Hatchet API. Documentation of
 * the API can be found here https://api.hatchet.is/apidocs/
 */
public class HatchetInfoPlugin implements InfoPlugin {

    private final static String TAG = HatchetInfoPlugin.class.getSimpleName();

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

    private final Store mStore;

    public HatchetInfoPlugin() {
        mStore = new Store();
    }

    /**
     * _fetch_ data from the Hatchet API (e.g. artist's top-hits, image etc.)
     */
    public void resolve(final InfoRequestData infoRequestData) {
        int priority;
        if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_TOPHITSANDALBUMS) {
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
                    InfoSystem.get().reportResults(infoRequestData, success);
                } catch (IOException e) {
                    Log.e(TAG, "resolve: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
            }
        };
        ThreadManager.get().execute(runnable);
    }

    /**
     * Core method of this InfoPlugin. Gets and parses the ordered results.
     *
     * @param infoRequestData InfoRequestData object containing the input parameters.
     * @return true if the type of the given InfoRequestData was valid and could be processed. false
     * otherwise
     */
    private boolean getParseConvert(InfoRequestData infoRequestData) throws IOException {
        QueryParams params = infoRequestData.getQueryParams();
        HatchetCollection hatchetCollection = CollectionManager.get().getHatchetCollection();
        Hatchet hatchet = mStore.getImplementation(infoRequestData.isBackgroundRequest());

        try {
            int type = infoRequestData.getType();
            if (type >= InfoRequestData.INFOREQUESTDATA_TYPE_USERS
                    && type < InfoRequestData.INFOREQUESTDATA_TYPE_USERS + 100) {
                JsonObject object =
                        hatchet.getUsers(params.ids, params.name, params.random, params.count);
                if (object == null) {
                    return false;
                }
                List<User> users = mStore.storeRecords(object, User.class, type,
                        infoRequestData.isBackgroundRequest());
                infoRequestData.setResultList(users);
                return true;

            } else if (type >= InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS
                    && type < InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS + 100) {
                JsonObject object = hatchet.getPlaylists(params.playlist_id);
                if (object == null) {
                    return false;
                }
                List<Playlist> playlists = mStore.storeRecords(object, Playlist.class, type,
                        infoRequestData.isBackgroundRequest());
                infoRequestData.setResultList(playlists);
                return true;

            } else if (type >= InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS
                    && type < InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS + 100) {
                JsonObject object = hatchet.getArtists(params.ids, params.name);
                if (object == null) {
                    return false;
                }
                if (type == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_TOPHITSANDALBUMS) {
                    List<Query> topHits = mStore.storeRecords(object, Query.class, type,
                            infoRequestData.isBackgroundRequest());
                    Artist artist = Artist.get(params.name);
                    Playlist playlist = Playlist.fromQueryList(TomahawkApp.PLUGINNAME_HATCHET + "_"
                            + artist.getCacheKey(), null, null, topHits);
                    hatchetCollection.addArtistTopHits(artist, playlist);

                    List<Album> albums = mStore.storeRecords(object, Album.class, type,
                            infoRequestData.isBackgroundRequest());
                    if (albums.size() > 0) {
                        for (Album album : albums) {
                            hatchetCollection.addAlbum(album);
                        }
                        Album firstAlbum = albums.get(0);
                        hatchetCollection.addArtistAlbums(firstAlbum.getArtist(), albums);
                    }
                }
                List<Artist> artists = mStore.storeRecords(object, Artist.class, type,
                        infoRequestData.isBackgroundRequest());
                for (Artist artist : artists) {
                    hatchetCollection.addArtist(artist);
                }
                infoRequestData.setResultList(artists);
                return true;

            } else if (type >= InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS
                    && type < InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS + 100) {
                JsonObject object = hatchet.getAlbums(params.ids, params.name, params.artistname);
                if (object == null) {
                    return false;
                }
                if (type == InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS_TRACKS) {
                    List<Query> tracks = mStore.storeRecords(object, Query.class, type,
                            infoRequestData.isBackgroundRequest());
                    Artist artist = Artist.get(params.artistname);
                    Album album = Album.get(params.name, artist);
                    Playlist playlist = Playlist.fromQueryList(TomahawkApp.PLUGINNAME_HATCHET + "_"
                            + album.getCacheKey(), null, null, tracks);
                    playlist.setFilled(true);
                    hatchetCollection.addAlbumTracks(album, playlist);
                }
                List<Album> albums = mStore.storeRecords(object, Album.class, type,
                        infoRequestData.isBackgroundRequest());
                for (Album album : albums) {
                    hatchetCollection.addAlbum(album);
                }
                infoRequestData.setResultList(albums);
                return true;

            } else if (type == InfoRequestData.INFOREQUESTDATA_TYPE_SEARCHES) {
                JsonObject object = hatchet.getSearches(params.term);
                if (object == null) {
                    return false;
                }
                List<Search> searches = mStore.storeRecords(object, Search.class, type,
                        infoRequestData.isBackgroundRequest());
                infoRequestData.setResultList(searches);
                return true;

            } else if (type == InfoRequestData.INFOREQUESTDATA_TYPE_SOCIALACTIONS) {
                JsonObject object = hatchet.getSocialActions(null, params.userid, params.type,
                        ISO8601Utils.format(params.before_date), params.limit);
                if (object == null) {
                    return false;
                }
                List<SocialAction> socialActions = mStore.storeRecords(object, SocialAction.class,
                        type, infoRequestData.isBackgroundRequest(), params);
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
                Hatchet hatchet = mStore.getImplementation(infoRequestData.isBackgroundRequest());
                // Before we do anything, get the accesstoken
                boolean success = false;
                boolean discard = false;
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
                                        new TypedByteArray("application/json; charset=utf-8",
                                                data.getBytes(Charsets.UTF_8)));
                            } else if (infoRequestData.getHttpType()
                                    == InfoRequestData.HTTPTYPE_DELETE) {
                                hatchet.deletePlaylistsPlaylistEntries(accessToken,
                                        infoRequestData.getQueryParams().entry_id,
                                        infoRequestData.getQueryParams().playlist_id);
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
                        discard = true;
                    } catch (RetrofitError e) {
                        Log.e(TAG, "send: Request to " + e.getUrl() + " failed: " + e.getClass()
                                + ": " + e.getLocalizedMessage());
                        if (e.getResponse() != null && e.getResponse().getStatus() == 500) {
                            Log.e(TAG, "send: discarding oplog that has failed to be sent to " + e
                                    .getUrl());
                            discard = true;
                        }
                    }
                }
                InfoSystem.get().onLoggedOpsSent(doneRequestsIds, discard);
                InfoSystem.get().reportResults(infoRequestData, success);
            }
        };
        ThreadManager.get().execute(runnable);
    }
}
