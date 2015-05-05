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

import com.google.common.base.Charsets;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;

import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.TomahawkListItemComparator;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoPlugin;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.InfoSystemUtils;
import org.tomahawk.libtomahawk.infosystem.JacksonConverter;
import org.tomahawk.libtomahawk.infosystem.QueryParams;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetAlbumInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetAlbums;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetArtistInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetArtists;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetCharts;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetImage;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaybackLogsResponse;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistEntries;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetRelationshipStruct;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetRelationshipsStruct;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetSearch;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetSearchItem;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetSocialAction;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetSocialActionResponse;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetTrackInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetTracks;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetUserInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetUsers;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.android.MainThreadExecutor;
import retrofit.client.OkClient;
import retrofit.mime.TypedByteArray;

import static android.os.Process.THREAD_PRIORITY_LOWEST;

/**
 * Implementation to enable the InfoSystem to retrieve data from the Hatchet API. Documentation of
 * the API can be found here https://api.hatchet.is/apidocs/
 */
public class HatchetInfoPlugin implements InfoPlugin {

    private final static String TAG = HatchetInfoPlugin.class.getSimpleName();

    public static final String HATCHET_BASE_URL = "https://api.hatchet.is/v1";

    public static final String HATCHET_SEARCHITEM_TYPE_ALBUM = "album";

    public static final String HATCHET_SEARCHITEM_TYPE_ARTIST = "artist";

    public static final String HATCHET_SEARCHITEM_TYPE_USER = "user";

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

    public static final double HATCHET_SEARCHITEM_MIN_SCORE = 5.0;

    public static final int SOCIALACTIONS_LIMIT = 20;

    public static final int FRIENDSFEED_LIMIT = 50;

    private HatchetAuthenticatorUtils mHatchetAuthenticatorUtils;

    private final ConcurrentHashMap<String, TomahawkListItem> mItemsToBeFilled
            = new ConcurrentHashMap<>();

    private final Hatchet mHatchet;

    private final Hatchet mHatchetBackground;

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
        OkHttpClient okHttpClient = new OkHttpClient();
        File cacheDir = new File(TomahawkApp.getContext().getCacheDir(), "responseCache");
        try {
            Cache cache = new Cache(cacheDir, 1024 * 1024 * 20);
            okHttpClient.setCache(cache);
        } catch (IOException e) {
            Log.e(TAG, "<init>: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .setEndpoint(HATCHET_BASE_URL)
                .setConverter(new JacksonConverter(InfoSystemUtils.getObjectMapper()))
                .setRequestInterceptor(requestInterceptor)
                .setClient(new OkClient(okHttpClient))
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
                .setEndpoint(HATCHET_BASE_URL)
                .setConverter(new JacksonConverter(InfoSystemUtils.getObjectMapper()))
                .setRequestInterceptor(requestInterceptor)
                .setClient(new OkClient(okHttpClient))
                .setExecutors(httpExecutor, new MainThreadExecutor())
                .build();
        mHatchetBackground = restAdapter.create(Hatchet.class);
    }

    /**
     * Start the JSONSendTask to send the given InfoRequestData's json string
     */
    @Override
    public void send(InfoRequestData infoRequestData, AuthenticatorUtils authenticatorUtils) {
        mHatchetAuthenticatorUtils = (HatchetAuthenticatorUtils) authenticatorUtils;
        send(infoRequestData);
    }

    /**
     * Start the JSONResponseTask to fetch results for the given InfoRequestData.
     *
     * @param itemToBeFilled this item will be stored and will later be enriched by the fetched
     *                       results from the Hatchet API
     */
    @Override
    public void resolve(InfoRequestData infoRequestData, TomahawkListItem itemToBeFilled) {
        mItemsToBeFilled.put(infoRequestData.getRequestId(), itemToBeFilled);
        resolve(infoRequestData);
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
        Collection hatchetCollection = CollectionManager.getInstance()
                .getCollection(TomahawkApp.PLUGINNAME_HATCHET);
        Hatchet hatchet = getImplementation(infoRequestData);

        try {
            if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS) {
                HatchetUsers users =
                        hatchet.getUsers(params.ids, params.name, params.random, params.count);
                if (users != null && users.users != null) {
                    List<Object> resultList = new ArrayList<>();
                    for (HatchetUserInfo userInfo : users.users) {
                        if (userInfo != null) {
                            HatchetTrackInfo track =
                                    TomahawkUtils.carelessGet(users.tracks, userInfo.nowplaying);
                            HatchetArtistInfo artist = null;
                            if (track != null) {
                                artist = TomahawkUtils.carelessGet(users.artists, track.artist);
                            }
                            String imageId = TomahawkUtils.carelessGet(userInfo.images, 0);
                            HatchetImage image = TomahawkUtils.carelessGet(users.images, imageId);
                            User user = InfoSystemUtils
                                    .convertToUser(userInfo, track, artist, image);
                            resultList.add(user);
                        }
                    }
                    infoRequestData.setResultList(resultList);
                    return true;
                }

            } else if (infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS) {
                HatchetPlaylistEntries entries = hatchet.getUsersPlaylists(params.userid);
                if (entries != null) {
                    List<Playlist> playlists = new ArrayList<>();
                    for (HatchetPlaylistInfo playlistInfo : entries.playlists) {
                        playlists.add(InfoSystemUtils.convertToPlaylist(playlistInfo));
                    }
                    if (mItemsToBeFilled.get(infoRequestData.getRequestId()) instanceof User) {
                        User userToBeFilled =
                                (User) mItemsToBeFilled.get(infoRequestData.getRequestId());
                        userToBeFilled.setPlaylists(playlists);
                    }
                    List<Object> results = new ArrayList<>();
                    results.addAll(playlists);
                    infoRequestData.setResultList(results);
                    return true;
                }

            } else if (infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS) {
                HatchetPlaylistEntries playlistEntries = hatchet.getPlaylists(params.playlist_id);
                if (playlistEntries != null) {
                    Playlist playlist;
                    Object itemToBeFilled =
                            mItemsToBeFilled.get(infoRequestData.getRequestId());
                    if (itemToBeFilled instanceof Playlist) {
                        playlist = (Playlist) itemToBeFilled;
                    } else {
                        playlist = DatabaseHelper.getInstance()
                                .getEmptyPlaylist(params.playlist_local_id);
                    }
                    playlist = InfoSystemUtils.fillPlaylist(playlist, playlistEntries);
                    if (playlist != null && playlistEntries.playlists != null
                            && playlistEntries.playlists.size() > 0) {
                        playlist.setCurrentRevision(
                                playlistEntries.playlists.get(0).currentrevision);
                    }
                    infoRequestData.setResult(playlist);
                    return true;
                }

            } else if (infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS) {
                User userToBeFilled =
                        (User) mItemsToBeFilled.get(infoRequestData.getRequestId());
                HatchetPlaylistEntries playlistEntries =
                        hatchet.getUsersLovedItems(userToBeFilled.getId());
                if (playlistEntries != null) {
                    if (playlistEntries.playlistEntries.size() > 0) {
                        InfoSystemUtils
                                .fillPlaylist(userToBeFilled.getFavorites(), playlistEntries);
                    }
                    infoRequestData.setResult(userToBeFilled.getFavorites());
                    return true;
                }

            } else if (infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_SOCIALACTIONS
                    || infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_FRIENDSFEED) {
                HatchetSocialActionResponse response;
                if (infoRequestData.getType()
                        == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_SOCIALACTIONS) {
                    response = hatchet.getUsersSocialActions(params.userid, params.offset,
                            params.limit);
                } else {
                    response = hatchet.getUsersFriendsFeed(params.userid, params.offset,
                            params.limit);
                }
                if (response != null) {
                    User userToBeFilled = (User) mItemsToBeFilled
                            .get(infoRequestData.getRequestId());
                    if (response.socialActions != null && response.socialActions.size() > 0) {
                        ArrayList<SocialAction> socialActions = new ArrayList<>();
                        for (HatchetSocialAction hatchetSocialAction : response.socialActions) {
                            HatchetTrackInfo track = TomahawkUtils.carelessGet(response.tracks,
                                    hatchetSocialAction.track);
                            HatchetAlbumInfo album = TomahawkUtils.carelessGet(response.albums,
                                    hatchetSocialAction.album);
                            HatchetArtistInfo artist = null;
                            if (hatchetSocialAction.artist != null) {
                                artist = TomahawkUtils.carelessGet(response.artists,
                                        hatchetSocialAction.artist);
                            } else if (track != null) {
                                artist = TomahawkUtils.carelessGet(response.artists, track.artist);
                            } else if (album != null) {
                                artist = TomahawkUtils.carelessGet(response.artists, album.artist);
                            }
                            HatchetUserInfo user = TomahawkUtils.carelessGet(response.users,
                                    hatchetSocialAction.user);
                            HatchetUserInfo target = TomahawkUtils.carelessGet(response.users,
                                    hatchetSocialAction.target);
                            HatchetPlaylistInfo playlist = TomahawkUtils
                                    .carelessGet(response.playlists,
                                            hatchetSocialAction.playlist);
                            socialActions.add(InfoSystemUtils.convertToSocialAction(
                                    hatchetSocialAction, track, artist, album, user, target,
                                    playlist));
                        }
                        if (infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_SOCIALACTIONS) {
                            userToBeFilled.setSocialActions(socialActions,
                                    Integer.valueOf(params.offset) / Integer.valueOf(params.limit));
                        } else if (infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_FRIENDSFEED) {
                            userToBeFilled.setFriendsFeed(socialActions,
                                    Integer.valueOf(params.offset) / Integer.valueOf(params.limit));
                        }
                    }
                    infoRequestData.setResult(userToBeFilled);
                    return true;
                }

            } else if (infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYBACKLOG) {
                HatchetPlaybackLogsResponse response = hatchet.getUsersPlaybackLog(params.userid);
                if (response != null) {
                    User userToBeFilled =
                            (User) mItemsToBeFilled.get(infoRequestData.getRequestId());
                    if (response.playbackLogEntries != null
                            && response.playbackLogEntries.size() > 0) {
                        ArrayList<Query> playbackItems =
                                InfoSystemUtils.convertToQueryList(response);
                        ArrayList<PlaylistEntry> entries = new ArrayList<>();
                        for (Query query : playbackItems) {
                            entries.add(PlaylistEntry.get(userToBeFilled.getPlaybackLog().getId(),
                                    query, TomahawkMainActivity.getLifetimeUniqueStringId()));
                        }
                        userToBeFilled.getPlaybackLog().setEntries(entries);
                        userToBeFilled.getPlaybackLog().setFilled(true);
                    }
                    infoRequestData.setResult(userToBeFilled);
                    return true;
                }

            } else if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS) {
                HatchetArtists artists = hatchet.getArtists(params.ids, params.name);
                if (artists != null) {
                    Artist artistToBeFilled =
                            (Artist) mItemsToBeFilled.get(infoRequestData.getRequestId());
                    if (artists.artists != null) {
                        HatchetArtistInfo artistInfo =
                                TomahawkUtils.carelessGetFirst(artists.artists.values());
                        if (artistInfo != null) {
                            String imageId = TomahawkUtils.carelessGet(artistInfo.images, 0);
                            HatchetImage image = TomahawkUtils.carelessGet(artists.images, imageId);
                            InfoSystemUtils
                                    .fillArtist(artistToBeFilled, image, artistInfo.wikiabstract);
                            hatchetCollection.addArtist(artistToBeFilled);
                        }
                    }
                    infoRequestData.setResult(artistToBeFilled);
                    return true;
                }

            } else if (infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_ALBUMS) {
                HatchetArtists artists = hatchet.getArtists(params.ids, params.name);
                if (artists != null && artists.artists != null) {
                    List<Object> convertedAlbums = new ArrayList<>();
                    HatchetArtistInfo artist =
                            TomahawkUtils.carelessGetFirst(artists.artists.values());
                    HatchetCharts charts = hatchet.getArtistsAlbums(artist.id);
                    if (charts != null && charts.albums != null) {
                        Artist convertedArtist =
                                (Artist) mItemsToBeFilled.get(infoRequestData.getRequestId());
                        for (HatchetAlbumInfo album : charts.albums.values()) {
                            String imageId = TomahawkUtils.carelessGet(album.images, 0);
                            HatchetImage image = TomahawkUtils.carelessGet(charts.images, imageId);
                            List<HatchetTrackInfo> albumTracks = null;
                            if (album.tracks.size() > 0) {
                                HatchetTracks tracks = hatchet.getTracks(album.tracks, null, null);
                                if (tracks != null) {
                                    albumTracks = tracks.tracks;
                                }
                            }
                            Album convertedAlbum =
                                    InfoSystemUtils.convertToAlbum(album, artist.name, image);
                            List<Query> convertedTracks =
                                    InfoSystemUtils.convertToQueries(albumTracks,
                                            convertedAlbum.getName(), convertedArtist.getName());
                            convertedAlbum.addQueries(convertedTracks);
                            convertedArtist.addAlbum(convertedAlbum);
                            hatchetCollection.addAlbum(convertedAlbum);
                            hatchetCollection.addArtistAlbum(convertedArtist, convertedAlbum);
                            hatchetCollection.addAlbumTracks(convertedAlbum, convertedTracks);
                            convertedAlbums.add(convertedAlbum);
                        }
                        hatchetCollection.addArtist(convertedArtist);
                    }
                    infoRequestData.setResultList(convertedAlbums);
                    return true;
                }

            } else if (infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_TOPHITS) {
                HatchetArtists artists = hatchet.getArtists(params.ids, params.name);
                if (artists != null && artists.artists != null) {
                    Artist artistToBeFilled =
                            (Artist) mItemsToBeFilled.get(infoRequestData.getRequestId());
                    HatchetArtistInfo artistInfo =
                            TomahawkUtils.carelessGetFirst(artists.artists.values());
                    if (artistInfo != null) {
                        HatchetCharts charts = hatchet.getArtistsTopHits(artistInfo.id);
                        if (charts != null) {
                            InfoSystemUtils
                                    .fillArtist(artistToBeFilled, charts.chartItems, charts.tracks);
                            hatchetCollection.addArtist(artistToBeFilled);
                        }
                    }
                    infoRequestData.setResult(artistToBeFilled);
                    return true;
                }

            } else if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS) {
                HatchetAlbums albums = hatchet.getAlbums(params.ids, params.name,
                        params.artistname);
                if (albums != null && albums.albums != null) {
                    Album album = (Album) mItemsToBeFilled.get(infoRequestData.getRequestId());
                    HatchetAlbumInfo albumInfo = TomahawkUtils.carelessGet(albums.albums, 0);
                    if (albumInfo != null) {
                        String imageId = TomahawkUtils.carelessGet(albumInfo.images, 0);
                        HatchetImage image = TomahawkUtils.carelessGet(albums.images, imageId);
                        InfoSystemUtils.fillAlbum(album, image);
                        if (albumInfo.tracks != null && albumInfo.tracks.size() > 0) {
                            HatchetTracks tracks = hatchet.getTracks(albumInfo.tracks, null, null);
                            if (tracks != null) {
                                HashSet<String> artistIds = new HashSet<>();
                                for (HatchetTrackInfo trackInfo : tracks.tracks) {
                                    artistIds.add(trackInfo.artist);
                                }
                                HatchetArtists artists =
                                        hatchet.getArtists(new ArrayList<>(artistIds), null);
                                if (artists != null) {
                                    List<Query> convertedTracks = InfoSystemUtils
                                            .convertToQueries(tracks.tracks, album.getName(),
                                                    artists.artists);
                                    album.addQueries(convertedTracks);
                                    hatchetCollection.addAlbumTracks(album, convertedTracks);
                                }
                            }
                        }
                        hatchetCollection.addAlbum(album);
                        infoRequestData.setResult(album);
                        return true;
                    }
                }

            } else if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_SEARCHES) {
                HatchetSearch search = hatchet.getSearches(params.term);
                if (search != null && search.searchResults != null) {
                    List<Object> convertedAlbums = new ArrayList<>();
                    List<Object> convertedArtists = new ArrayList<>();
                    List<Object> convertedUsers = new ArrayList<>();
                    for (HatchetSearchItem searchItem : search.searchResults) {
                        if (searchItem.score > HATCHET_SEARCHITEM_MIN_SCORE) {
                            if (HATCHET_SEARCHITEM_TYPE_ALBUM.equals(searchItem.type)) {
                                HatchetAlbumInfo albumInfo =
                                        TomahawkUtils.carelessGet(search.albums, searchItem.album);
                                if (albumInfo != null) {
                                    String imageId = TomahawkUtils.carelessGet(albumInfo.images, 0);
                                    HatchetImage image =
                                            TomahawkUtils.carelessGet(search.images, imageId);
                                    HatchetArtistInfo artistInfo =
                                            TomahawkUtils
                                                    .carelessGet(search.artists, albumInfo.artist);
                                    Album album;
                                    if (artistInfo != null) {
                                        album = InfoSystemUtils.convertToAlbum(albumInfo,
                                                artistInfo.name, image);
                                        convertedAlbums.add(album);
                                        hatchetCollection.addAlbum(album);
                                    }
                                }
                            } else if (HATCHET_SEARCHITEM_TYPE_ARTIST.equals(searchItem.type)) {
                                HatchetArtistInfo artistInfo =
                                        TomahawkUtils
                                                .carelessGet(search.artists, searchItem.artist);
                                if (artistInfo != null) {
                                    String imageId = TomahawkUtils
                                            .carelessGet(artistInfo.images, 0);
                                    HatchetImage image =
                                            TomahawkUtils.carelessGet(search.images, imageId);
                                    Artist artist = InfoSystemUtils
                                            .convertToArtist(artistInfo, image);
                                    convertedArtists.add(artist);
                                    hatchetCollection.addArtist(artist);
                                }
                            } else if (HATCHET_SEARCHITEM_TYPE_USER.equals(searchItem.type)) {
                                HatchetUserInfo user =
                                        TomahawkUtils.carelessGet(search.users, searchItem.user);
                                if (user != null) {
                                    String imageId = TomahawkUtils.carelessGet(user.images, 0);
                                    HatchetImage image = TomahawkUtils.carelessGet(search.images,
                                            imageId);
                                    convertedUsers
                                            .add(InfoSystemUtils.convertToUser(user, null, null,
                                                    image));
                                }
                            }
                        }
                    }
                    infoRequestData.setResultList(convertedAlbums);
                    infoRequestData.setResultList(convertedArtists);
                    infoRequestData.setResultList(convertedUsers);
                    return true;
                }

            } else if (infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_FOLLOWINGS
                    || infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_FOLLOWERS) {
                HatchetRelationshipsStruct relationshipsStruct = hatchet.getRelationships(
                        params.ids, params.userid, params.targettype, params.targetuserid, null,
                        null, null, params.type);
                if (relationshipsStruct != null && relationshipsStruct.relationships != null) {
                    Map<String, String> relationShipIds = new HashMap<>();
                    for (HatchetRelationshipStruct relationship : relationshipsStruct.relationships) {
                        if (infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_FOLLOWERS) {
                            relationShipIds.put(relationship.user, relationship.id);
                        } else {
                            relationShipIds.put(relationship.targetUser, relationship.id);
                        }
                    }
                    User userToBeFilled = (User) mItemsToBeFilled
                            .get(infoRequestData.getRequestId());
                    HatchetUsers users = hatchet.getUsers(
                            new ArrayList<>(relationShipIds.keySet()), params.name, null,
                            null);
                    if (users != null && users.users != null) {
                        ArrayList<User> convertedUsers = new ArrayList<>();
                        for (HatchetUserInfo user : users.users) {
                            HatchetTrackInfo track =
                                    TomahawkUtils.carelessGet(users.tracks, user.nowplaying);
                            if (track != null) {
                                String imageId = TomahawkUtils.carelessGet(user.images, 0);
                                HatchetImage image = TomahawkUtils
                                        .carelessGet(users.images, imageId);
                                HatchetArtistInfo artist =
                                        TomahawkUtils.carelessGet(users.artists, track.artist);
                                convertedUsers.add(InfoSystemUtils.convertToUser(
                                        user, track, artist, image));
                            }
                        }
                        TreeMap<User, String> relationships =
                                new TreeMap<>(new TomahawkListItemComparator(
                                        TomahawkListItemComparator.COMPARE_ALPHA));
                        for (User user : convertedUsers) {
                            relationships.put(user, relationShipIds.get(user.getId()));
                        }
                        if (infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_FOLLOWERS) {
                            userToBeFilled.setFollowers(relationships);
                        } else {
                            userToBeFilled.setFollowings(relationships);
                        }
                        infoRequestData.setResult(userToBeFilled);
                        return true;
                    }
                }

            } else if (infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_STARREDALBUMS
                    || infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_STARREDARTISTS) {
                HatchetRelationshipsStruct relationShips = hatchet.getRelationships(params.ids,
                        params.userid, params.targettype, params.targetuserid, null, null, null,
                        params.type);
                if (relationShips != null && relationShips.relationships != null) {
                    List<Album> convertedAlbums = new ArrayList<>();
                    List<Artist> convertedArtists = new ArrayList<>();
                    for (HatchetRelationshipStruct relationship : relationShips.relationships) {
                        if (infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_STARREDALBUMS) {
                            HatchetAlbumInfo album = TomahawkUtils.carelessGet(
                                    relationShips.albums, relationship.targetAlbum);
                            if (album != null) {
                                HatchetArtistInfo artist = TomahawkUtils.carelessGet(
                                        relationShips.artists, album.artist);
                                if (artist != null) {
                                    Album convertedAlbum = InfoSystemUtils.convertToAlbum(album,
                                            artist.name, null);
                                    convertedAlbums.add(convertedAlbum);
                                    hatchetCollection.addAlbum(convertedAlbum);
                                }
                            }
                        } else {
                            HatchetArtistInfo artist = TomahawkUtils.carelessGet(
                                    relationShips.artists, relationship.targetArtist);
                            if (artist != null) {
                                Artist convertedArtist =
                                        InfoSystemUtils.convertToArtist(artist, null);
                                convertedArtists.add(convertedArtist);
                                hatchetCollection.addArtist(convertedArtist);
                            }
                        }
                    }
                    if (mItemsToBeFilled.get(infoRequestData.getRequestId()) instanceof User) {
                        User userToBeFilled =
                                (User) mItemsToBeFilled.get(infoRequestData.getRequestId());
                        userToBeFilled.setStarredAlbums(convertedAlbums);
                    }
                    List<Object> convertedObjects = new ArrayList<>();
                    convertedObjects.addAll(convertedAlbums);
                    convertedObjects.addAll(convertedArtists);
                    infoRequestData.setResultList(convertedObjects);
                    return true;
                }
            }
        } catch (RetrofitError e) {
            Log.e(TAG, "getParseConvert: Request to " + e.getUrl() + " failed: " + e.getClass()
                    + ": " + e.getLocalizedMessage());
        }
        return false;
    }

    /**
     * _send_ data to the Hatchet API (e.g. nowPlaying, playbackLogs etc.)
     */
    public void send(final InfoRequestData infoRequestData) {
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
                                infoRequestData.setResult(entries);
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

    private Hatchet getImplementation(InfoRequestData infoRequestData) {
        return infoRequestData.isBackgroundRequest() ? mHatchetBackground : mHatchet;
    }
}
