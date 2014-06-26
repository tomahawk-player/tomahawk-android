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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import org.apache.http.client.ClientProtocolException;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoPlugin;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.InfoSystemUtils;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation to enable the InfoSystem to retrieve data from the Hatchet API. Documentation of
 * the API can be found here https://api.hatchet.is/apidocs/
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HatchetInfoPlugin extends InfoPlugin {

    private final static String TAG = HatchetInfoPlugin.class.getSimpleName();

    public static final String HATCHET_BASE_URL = "https://api.hatchet.is";

    public static final String HATCHET_VERSION = "v1";

    public static final String HATCHET_ARTISTS = "artists";

    public static final String HATCHET_ARTISTS_TOPHITS = "topHits";

    public static final String HATCHET_CHARTITEMS = "chartItems";

    public static final String HATCHET_ALBUMS = "albums";

    public static final String HATCHET_TRACKS = "tracks";

    public static final String HATCHET_IMAGES = "images";

    public static final String HATCHET_USERS = "users";

    public static final String HATCHET_PLAYLISTS = "playlists";

    public static final String HATCHET_PLAYLISTS_ENTRIES = "entries";

    public static final String HATCHET_SEARCHES = "searches";

    public static final String HATCHET_SEARCHITEM_TYPE_ALBUM = "album";

    public static final String HATCHET_SEARCHITEM_TYPE_ARTIST = "artist";

    public static final String HATCHET_SEARCHITEM_TYPE_USER = "user";

    public static final String HATCHET_PLAYBACKLOGENTRIES = "playbackLogEntries";

    public static final String HATCHET_PLAYBACKLOGENTRIES_NOWPLAYING = "nowplaying";

    public static final String HATCHET_SOCIALACTIONS = "socialActions";

    public static final String HATCHET_SOCIALACTION_TYPE_LOVE = "love";

    public static final String HATCHET_SOCIALACTION_TYPE_FOLLOW = "follow";

    public static final String HATCHET_SOCIALACTION_TYPE_CREATECOMMENT = "createcomment";

    public static final String HATCHET_SOCIALACTION_TYPE_LATCHON = "latchOn";

    public static final String HATCHET_SOCIALACTION_TYPE_LATCHOFF = "latchOff";

    public static final String HATCHET_SOCIALACTION_TYPE_CREATEPLAYLIST = "createplaylist";

    public static final String HATCHET_FRIENDSFEED = "friendsFeed";

    public static final String HATCHET_LOVEDITEMS = "lovedItems";

    public static final String HATCHET_PLAYBACKLOG = "playbackLog";

    public static final String HATCHET_RELATIONSHIPS = "relationships";

    public static final String HATCHET_RELATIONSHIPS_TYPE_FOLLOW = "follow";

    public static final String HATCHET_RELATIONSHIPS_TYPE_LOVE = "love";

    public static final String HATCHET_RELATIONSHIPS_TARGETTYPE_ALBUM = "album";

    public static final String HATCHET_RELATIONSHIPS_TARGETTYPE_ARTIST = "artist";

    public static final double HATCHET_SEARCHITEM_MIN_SCORE = 5.0;

    public static final String HATCHET_PARAM_NAME = "name";

    public static final String HATCHET_PARAM_ID = "id";

    public static final String HATCHET_PARAM_IDARRAY = "ids[]";

    public static final String HATCHET_PARAM_ARTIST_NAME = "artist_name";

    public static final String HATCHET_PARAM_TERM = "term";

    public static final String HATCHET_PARAMS_AUTHORIZATION = "authorization";

    public static final String HATCHET_PARAM_TYPE = "type";

    public static final String HATCHET_PARAM_USERID = "user_id";

    public static final String HATCHET_PARAM_TARGETUSERID = "target_user_id";

    public static final String HATCHET_PARAM_TARGETTYPE = "target_type";

    public static final String HATCHET_ACCOUNTDATA_USER_ID = "hatchet_preference_user_id";

    private Context mContext;

    private HatchetAuthenticatorUtils mHatchetAuthenticatorUtils;

    private ObjectMapper mObjectMapper;

    private static String mUserId = null;

    private ConcurrentHashMap<String, TomahawkListItem> mItemsToBeFilled
            = new ConcurrentHashMap<String, TomahawkListItem>();

    public HatchetInfoPlugin(Context context) {
        mContext = context;
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
    private boolean getAndParseInfo(InfoRequestData infoRequestData)
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        long start = System.currentTimeMillis();
        Multimap<String, String> params = LinkedListMultimap.create();
        Map<String, Map> resultMapList = new HashMap<String, Map>();
        String rawJsonString;
        if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS) {
            rawJsonString = TomahawkUtils.httpGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_USERS,
                            infoRequestData.getParams())
            );
            infoRequestData
                    .setInfoResult(mObjectMapper.readValue(rawJsonString, HatchetUsers.class));
            return true;
        } else if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_SELF) {
            if (TextUtils.isEmpty(mUserId)) {
                return false;
            }
            params.put(HATCHET_PARAM_IDARRAY, mUserId);
            rawJsonString = TomahawkUtils.httpGet(buildQuery(
                    InfoRequestData.INFOREQUESTDATA_TYPE_USERS, params));
            infoRequestData
                    .setInfoResult(mObjectMapper.readValue(rawJsonString, HatchetUsers.class));
            return true;
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS) {
            if (TextUtils.isEmpty(mUserId)) {
                return false;
            }
            params.put(HATCHET_PARAM_ID, mUserId);
            rawJsonString = TomahawkUtils.httpGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS, params));
            infoRequestData
                    .setInfoResult(mObjectMapper.readValue(rawJsonString, HatchetPlaylists.class));
            return true;
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_ENTRIES) {
            rawJsonString = TomahawkUtils.httpGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_ENTRIES,
                            infoRequestData.getParams())
            );
            infoRequestData.setInfoResult(mObjectMapper
                    .readValue(rawJsonString, HatchetPlaylistEntries.class));
            return true;
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS) {
            if (TextUtils.isEmpty(mUserId)) {
                return false;
            }
            Map<HatchetPlaylistInfo, HatchetPlaylistEntries> playlistEntriesMap
                    = new HashMap<HatchetPlaylistInfo, HatchetPlaylistEntries>();
            params.put(HATCHET_PARAM_ID, mUserId);
            rawJsonString = TomahawkUtils.httpGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS, params));
            HatchetPlaylistEntries playlistEntries = mObjectMapper
                    .readValue(rawJsonString, HatchetPlaylistEntries.class);
            playlistEntriesMap.put(playlistEntries.playlist, playlistEntries);
            resultMapList.put(HATCHET_PLAYLISTS_ENTRIES, playlistEntriesMap);
            infoRequestData.setInfoResultMap(resultMapList);
            return true;
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_SOCIALACTIONS) {
            rawJsonString = TomahawkUtils.httpGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_SOCIALACTIONS,
                            infoRequestData.getParams())
            );
            infoRequestData.setInfoResult(
                    mObjectMapper.readValue(rawJsonString, HatchetSocialActionResponse.class));
            return true;
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_FRIENDSFEED) {
            rawJsonString = TomahawkUtils.httpGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_FRIENDSFEED,
                            infoRequestData.getParams())
            );
            infoRequestData.setInfoResult(
                    mObjectMapper.readValue(rawJsonString, HatchetSocialActionResponse.class));
            return true;
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYBACKLOG) {
            rawJsonString = TomahawkUtils.httpGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYBACKLOG,
                            infoRequestData.getParams())
            );
            infoRequestData.setInfoResult(
                    mObjectMapper.readValue(rawJsonString, HatchetPlaybackLogsResponse.class));
            return true;
        } else if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS) {
            rawJsonString = TomahawkUtils.httpGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS,
                            infoRequestData.getParams())
            );
            infoRequestData.setInfoResult(
                    mObjectMapper.readValue(rawJsonString, HatchetArtists.class));
            return true;
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_ALBUMS) {
            rawJsonString = TomahawkUtils.httpGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS,
                            infoRequestData.getParams())
            );
            HatchetArtists artists = mObjectMapper.readValue(rawJsonString, HatchetArtists.class);

            if (artists.artists != null && artists.artists.size() > 0) {
                Map<HatchetAlbumInfo, HatchetTracks> tracksMap
                        = new HashMap<HatchetAlbumInfo, HatchetTracks>();
                Map<HatchetAlbumInfo, HatchetImage> imageMap
                        = new HashMap<HatchetAlbumInfo, HatchetImage>();
                params.put(HATCHET_PARAM_ID, artists.artists.get(0).id);
                rawJsonString = TomahawkUtils.httpGet(
                        buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_ALBUMS,
                                params)
                );
                HatchetCharts charts = mObjectMapper.readValue(rawJsonString, HatchetCharts.class);
                Map<String, HatchetImage> chartImageMap = new HashMap<String, HatchetImage>();
                if (charts.images != null) {
                    for (HatchetImage image : charts.images) {
                        chartImageMap.put(image.id, image);
                    }
                }
                if (charts.albums != null) {
                    for (HatchetAlbumInfo albumInfo : charts.albums) {
                        if (albumInfo.images != null && albumInfo.images.size() > 0) {
                            imageMap.put(albumInfo, chartImageMap.get(albumInfo.images.get(0)));
                        }
                        if (albumInfo.tracks != null && albumInfo.tracks.size() > 0) {
                            params.clear();
                            for (String trackId : albumInfo.tracks) {
                                params.put(HATCHET_PARAM_IDARRAY, trackId);
                            }
                            rawJsonString = TomahawkUtils.httpGet(
                                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_TRACKS,
                                            params)
                            );
                            HatchetTracks tracks = mObjectMapper
                                    .readValue(rawJsonString, HatchetTracks.class);
                            tracksMap.put(albumInfo, tracks);
                        }
                    }
                }
                resultMapList.put(HATCHET_TRACKS, tracksMap);
                resultMapList.put(HATCHET_IMAGES, imageMap);
            }
            infoRequestData.setInfoResultMap(resultMapList);
            return true;
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_TOPHITS) {
            rawJsonString = TomahawkUtils.httpGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS,
                            infoRequestData.getParams())
            );
            HatchetArtists artists = mObjectMapper.readValue(rawJsonString, HatchetArtists.class);

            if (artists.artists != null && artists.artists.size() > 0) {
                Map<HatchetChartItem, HatchetTrackInfo> tracksMap
                        = new LinkedHashMap<HatchetChartItem, HatchetTrackInfo>();
                params.put(HATCHET_PARAM_ID, artists.artists.get(0).id);
                rawJsonString = TomahawkUtils.httpGet(
                        buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_TOPHITS,
                                params)
                );
                HatchetCharts charts = mObjectMapper.readValue(rawJsonString, HatchetCharts.class);
                Map<String, HatchetTrackInfo> trackInfoMap
                        = new HashMap<String, HatchetTrackInfo>();
                if (charts.tracks != null) {
                    for (HatchetTrackInfo trackInfo : charts.tracks) {
                        trackInfoMap.put(trackInfo.id, trackInfo);
                    }
                }
                if (charts.chartItems != null) {
                    for (HatchetChartItem chartItem : charts.chartItems) {
                        tracksMap.put(chartItem, trackInfoMap.get(chartItem.track));
                    }
                }
                resultMapList.put(HATCHET_TRACKS, tracksMap);
            }
            infoRequestData.setInfoResultMap(resultMapList);
            return true;
        } else if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS) {
            rawJsonString = TomahawkUtils.httpGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS,
                            infoRequestData.getParams())
            );
            HatchetAlbums albums = mObjectMapper.readValue(rawJsonString, HatchetAlbums.class);
            if (albums.albums != null && albums.albums.size() > 0) {
                HatchetAlbumInfo albumInfo = albums.albums.get(0);
                Map<String, HatchetImage> imageMap = new HashMap<String, HatchetImage>();
                if (albums.images != null) {
                    for (HatchetImage image : albums.images) {
                        imageMap.put(image.id, image);
                    }
                }
                Map<HatchetAlbumInfo, HatchetTracks> tracksMap
                        = new HashMap<HatchetAlbumInfo, HatchetTracks>();
                if (albumInfo.tracks != null && albumInfo.tracks.size() > 0) {
                    params.clear();
                    for (String trackId : albumInfo.tracks) {
                        params.put(HATCHET_PARAM_IDARRAY, trackId);
                    }
                    rawJsonString = TomahawkUtils.httpGet(
                            buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_TRACKS, params));
                    HatchetTracks tracks = mObjectMapper
                            .readValue(rawJsonString, HatchetTracks.class);
                    tracksMap.put(albumInfo, tracks);
                }
                infoRequestData.setInfoResult(albumInfo);
                resultMapList.put(HATCHET_IMAGES, imageMap);
                resultMapList.put(HATCHET_TRACKS, tracksMap);
            }
            infoRequestData.setInfoResultMap(resultMapList);
            return true;
        } else if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_SEARCHES) {
            rawJsonString = TomahawkUtils.httpGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_SEARCHES,
                            infoRequestData.getParams())
            );
            infoRequestData
                    .setInfoResult(mObjectMapper.readValue(rawJsonString, HatchetSearch.class));
            return true;
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_FOLLOWERS
                || infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_FOLLOWINGS) {
            rawJsonString = TomahawkUtils.httpGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS,
                            infoRequestData.getParams())
            );
            HatchetRelationshipsStruct relationshipsStruct = mObjectMapper
                    .readValue(rawJsonString, HatchetRelationshipsStruct.class);
            for (HatchetRelationshipStruct relationship : relationshipsStruct.relationships) {
                String userId;
                if (infoRequestData.getType()
                        == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_FOLLOWERS) {
                    userId = relationship.user;
                } else {
                    userId = relationship.targetUser;
                }
                params.put(HATCHET_PARAM_IDARRAY, userId);
            }
            rawJsonString = TomahawkUtils
                    .httpGet(buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_USERS, params));
            HatchetUsers hatchetUsers = mObjectMapper
                    .readValue(rawJsonString, HatchetUsers.class);
            infoRequestData.setInfoResult(hatchetUsers);
            return true;
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_STARREDALBUMS
                || infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_STARREDARTISTS) {
            if (!infoRequestData.getParams().containsKey(HATCHET_PARAM_USERID)) {
                infoRequestData.getParams().put(HATCHET_PARAM_USERID, mUserId);
            }
            rawJsonString = TomahawkUtils.httpGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS,
                            infoRequestData.getParams())
            );
            HatchetRelationshipsStruct relationshipsStruct = mObjectMapper
                    .readValue(rawJsonString, HatchetRelationshipsStruct.class);
            if (relationshipsStruct.relationships.isEmpty()) {
                return false;
            }
            infoRequestData.setInfoResult(relationshipsStruct);
            return true;
        }
        Log.d(TAG, "doInBackground(...) took " + (System.currentTimeMillis() - start)
                + "ms to finish");
        return false;
    }

    /**
     * Convert the given InfoRequestData's result data. This is the processing step done after we've
     * fetched the results from the API and parsed the JSON Data into our java objects. This method
     * basically converts the Hatchet-specific java objects into tomahawk-android specific objects.
     */
    private void convertParsedItem(InfoRequestData infoRequestData) {
        Collection hatchetCollection = CollectionManager.getInstance()
                .getCollection(TomahawkApp.PLUGINNAME_HATCHET);
        Map<String, List> convertedResultMap = new HashMap<String, List>();
        if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS) {
            HatchetPlaylists playlists = (HatchetPlaylists) infoRequestData.getInfoResult();
            List<UserPlaylist> userPlaylists = new ArrayList<UserPlaylist>();
            for (HatchetPlaylistInfo playlistInfo : playlists.playlists) {
                userPlaylists.add(InfoSystemUtils.convertToUserPlaylist(playlistInfo));
            }
            convertedResultMap.put(HATCHET_PLAYLISTS, userPlaylists);
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS) {
            Map<HatchetPlaylistInfo, HatchetPlaylistEntries> playlistInfoMap = infoRequestData
                    .getInfoResultMap()
                    .get(HatchetInfoPlugin.HATCHET_PLAYLISTS_ENTRIES);
            List<HatchetPlaylistInfo> playlistInfos = new ArrayList<HatchetPlaylistInfo>(
                    playlistInfoMap.keySet());
            if (playlistInfos.size() > 0) {
                HatchetPlaylistInfo playlistInfo = playlistInfos.get(0);
                playlistInfo.id = DatabaseHelper.LOVEDITEMS_PLAYLIST_ID;
                List<UserPlaylist> userPlaylists = new ArrayList<UserPlaylist>();
                UserPlaylist userPlaylist = InfoSystemUtils
                        .convertToUserPlaylist(playlistInfo);
                userPlaylist = InfoSystemUtils.fillUserPlaylist(userPlaylist,
                        playlistInfoMap.get(playlistInfo));
                userPlaylists.add(userPlaylist);
                convertedResultMap.put(HATCHET_PLAYLISTS, userPlaylists);
            }
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_SEARCHES) {
            HatchetSearch search = (HatchetSearch) infoRequestData.getInfoResult();
            Map<String, HatchetUserInfo> userInfoMap = new HashMap<String, HatchetUserInfo>();
            if (search.users != null) {
                for (HatchetUserInfo userInfo : search.users) {
                    userInfoMap.put(userInfo.id, userInfo);
                }
            }
            Map<String, HatchetAlbumInfo> albumInfoMap = new HashMap<String, HatchetAlbumInfo>();
            if (search.albums != null) {
                for (HatchetAlbumInfo albumInfo : search.albums) {
                    albumInfoMap.put(albumInfo.id, albumInfo);
                }
            }
            Map<String, HatchetArtistInfo> artistInfoMap = new HashMap<String, HatchetArtistInfo>();
            if (search.artists != null) {
                for (HatchetArtistInfo artistInfo : search.artists) {
                    artistInfoMap.put(artistInfo.id, artistInfo);
                }
            }
            Map<String, HatchetImage> imageMap = new HashMap<String, HatchetImage>();
            if (search.images != null) {
                for (HatchetImage image : search.images) {
                    imageMap.put(image.id, image);
                }
            }
            Map<String, HatchetTrackInfo> trackInfoMap = new HashMap<String, HatchetTrackInfo>();
            if (search.tracks != null) {
                for (HatchetTrackInfo trackInfo : search.tracks) {
                    trackInfoMap.put(trackInfo.id, trackInfo);
                }
            }
            if (search.searchResults != null) {
                List<Album> albums = new ArrayList<Album>();
                List<Artist> artists = new ArrayList<Artist>();
                List<User> users = new ArrayList<User>();
                for (HatchetSearchItem searchItem : search.searchResults) {
                    if (searchItem.score > HATCHET_SEARCHITEM_MIN_SCORE) {
                        if (HATCHET_SEARCHITEM_TYPE_ALBUM.equals(searchItem.type)) {
                            HatchetAlbumInfo albumInfo = albumInfoMap.get(searchItem.album);
                            if (albumInfo != null) {
                                HatchetImage image = null;
                                if (albumInfo.images != null && albumInfo.images.size() > 0) {
                                    image = imageMap.get(albumInfo.images.get(0));
                                }
                                Album album = InfoSystemUtils.convertToAlbum(albumInfo,
                                        artistInfoMap.get(albumInfo.artist).name, null, image);
                                albums.add(album);
                                hatchetCollection.addAlbum(album);
                            }
                        } else if (HATCHET_SEARCHITEM_TYPE_ARTIST.equals(searchItem.type)) {
                            HatchetArtistInfo artistInfo = artistInfoMap.get(searchItem.artist);
                            if (artistInfo != null) {
                                HatchetImage image = null;
                                if (artistInfo.images != null && artistInfo.images.size() > 0) {
                                    image = imageMap.get(artistInfo.images.get(0));
                                }
                                Artist artist = InfoSystemUtils.convertToArtist(artistInfo, image);
                                artists.add(artist);
                                hatchetCollection.addArtist(artist);
                            }
                        } else if (HATCHET_SEARCHITEM_TYPE_USER.equals(searchItem.type)) {
                            HatchetUserInfo userInfo = userInfoMap.get(searchItem.user);
                            users.add(InfoSystemUtils.convertToUser(userInfo, trackInfoMap,
                                    artistInfoMap, imageMap));
                        }
                    }
                }
                convertedResultMap.put(HATCHET_ALBUMS, albums);
                convertedResultMap.put(HATCHET_ARTISTS, artists);
                convertedResultMap.put(HATCHET_USERS, users);
            }
        } else if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_SELF) {
            if (infoRequestData.getInfoResult() != null) {
                HatchetUsers users = (HatchetUsers) infoRequestData.getInfoResult();
                if (users.users != null && users.users.size() > 0) {
                    Map<String, HatchetArtistInfo> artistInfoMap
                            = new HashMap<String, HatchetArtistInfo>();
                    if (users.artists != null) {
                        for (HatchetArtistInfo artistInfo : users.artists) {
                            artistInfoMap.put(artistInfo.id, artistInfo);
                        }
                    }
                    Map<String, HatchetImage> imageMap = new HashMap<String, HatchetImage>();
                    if (users.images != null) {
                        for (HatchetImage image : users.images) {
                            imageMap.put(image.id, image);
                        }
                    }
                    Map<String, HatchetTrackInfo> trackInfoMap
                            = new HashMap<String, HatchetTrackInfo>();
                    if (users.tracks != null) {
                        for (HatchetTrackInfo trackInfo : users.tracks) {
                            trackInfoMap.put(trackInfo.id, trackInfo);
                        }
                    }
                    ArrayList<User> convertedUsers = new ArrayList<User>();
                    convertedUsers.add(InfoSystemUtils.convertToUser(
                            users.users.get(0), trackInfoMap, artistInfoMap, imageMap));
                    convertedResultMap.put(HATCHET_USERS, convertedUsers);
                }
            }
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_STARREDALBUMS) {
            if (infoRequestData.getInfoResult() != null) {
                HatchetRelationshipsStruct relationShips
                        = (HatchetRelationshipsStruct) infoRequestData.getInfoResult();
                Map<String, HatchetAlbumInfo> albumInfoMap
                        = new HashMap<String, HatchetAlbumInfo>();
                for (HatchetAlbumInfo albumInfo : relationShips.albums) {
                    albumInfoMap.put(albumInfo.id, albumInfo);
                }
                Map<String, HatchetArtistInfo> artistInfoMap
                        = new HashMap<String, HatchetArtistInfo>();
                for (HatchetArtistInfo artistInfo : relationShips.artists) {
                    artistInfoMap.put(artistInfo.id, artistInfo);
                }
                List<Album> convertedAlbums = new ArrayList<Album>();
                if (relationShips.relationships != null) {
                    for (HatchetRelationshipStruct relationship : relationShips.relationships) {
                        HatchetAlbumInfo albumInfo = albumInfoMap.get(relationship.targetAlbum);
                        Album album = InfoSystemUtils
                                .convertToAlbum(albumInfo, artistInfoMap.get(albumInfo.artist).name,
                                        null, null);
                        convertedAlbums.add(album);
                        hatchetCollection.addAlbum(album);
                    }
                }
                convertedResultMap.put(HATCHET_ALBUMS, convertedAlbums);
            }
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_STARREDARTISTS) {
            if (infoRequestData.getInfoResult() != null) {
                HatchetRelationshipsStruct relationShips
                        = (HatchetRelationshipsStruct) infoRequestData.getInfoResult();
                Map<String, HatchetArtistInfo> artistInfoMap
                        = new HashMap<String, HatchetArtistInfo>();
                for (HatchetArtistInfo artistInfo : relationShips.artists) {
                    artistInfoMap.put(artistInfo.id, artistInfo);
                }
                List<Artist> convertedArtists = new ArrayList<Artist>();
                if (relationShips.relationships != null) {
                    for (HatchetRelationshipStruct relationship : relationShips.relationships) {
                        HatchetArtistInfo artistInfo = artistInfoMap.get(relationship.targetArtist);
                        Artist artist = InfoSystemUtils.convertToArtist(artistInfo, null);
                        convertedArtists.add(artist);
                        hatchetCollection.addArtist(artist);
                    }
                }
                convertedResultMap.put(HATCHET_ARTISTS, convertedArtists);
            }
        }
        infoRequestData.setConvertedResultMap(convertedResultMap);
        if (mItemsToBeFilled.containsKey(infoRequestData.getRequestId())) {
            // We have an item that wants to be filled/enriched with data from Hatchet
            if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS) {
                if (infoRequestData.getInfoResult() != null) {
                    HatchetArtists artists = ((HatchetArtists) infoRequestData.getInfoResult());
                    if (artists.artists != null && artists.artists.size() > 0
                            && artists.images != null
                            && artists.images.size() > 0 && artists.images != null
                            && artists.images.size() > 0) {
                        HatchetArtistInfo artistInfo = artists.artists.get(0);
                        String imageId = artistInfo.images.get(0);
                        HatchetImage image = null;
                        for (HatchetImage img : artists.images) {
                            if (img.id.equals(imageId)) {
                                image = img;
                            }
                        }
                        Artist artist = (Artist) mItemsToBeFilled
                                .get(infoRequestData.getRequestId());
                        InfoSystemUtils
                                .fillArtist(artist, artists.artists.get(0), image);
                        hatchetCollection.addArtist(artist);
                    }
                }
            } else if (infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_ALBUMS) {
                if (infoRequestData.getInfoResultMap() != null) {
                    Artist artist = (Artist) mItemsToBeFilled.get(infoRequestData.getRequestId());
                    List<Album> albums = InfoSystemUtils.convertToAlbumList(artist,
                            infoRequestData.getInfoResultMap().get(HATCHET_TRACKS),
                            infoRequestData.getInfoResultMap().get(HATCHET_IMAGES));
                    for (Album album : albums) {
                        artist.addAlbum(album);
                        hatchetCollection.addAlbum(album);
                    }
                    hatchetCollection.addArtist(artist);
                }
            } else if (infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_TOPHITS) {
                if (infoRequestData.getInfoResultMap() != null) {
                    Artist artist = (Artist) mItemsToBeFilled.get(infoRequestData.getRequestId());
                    InfoSystemUtils.fillArtist(artist,
                            infoRequestData.getInfoResultMap().get(HATCHET_TRACKS));
                    hatchetCollection.addArtist(artist);
                }
            } else if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS) {
                if (infoRequestData.getInfoResultMap() != null) {
                    HatchetAlbumInfo albumInfo = ((HatchetAlbumInfo) infoRequestData
                            .getInfoResult());
                    Map<HatchetAlbumInfo, HatchetImage> imageMap
                            = ((Map<HatchetAlbumInfo, HatchetImage>) infoRequestData
                            .getInfoResultMap().get(HATCHET_IMAGES));
                    Map<HatchetAlbumInfo, HatchetTracks> tracksMap
                            = ((Map<HatchetAlbumInfo, HatchetTracks>) infoRequestData
                            .getInfoResultMap().get(HATCHET_TRACKS));
                    if (albumInfo != null && albumInfo.images != null
                            && albumInfo.images.size() > 0) {
                        HatchetImage image = imageMap.get(albumInfo.images.get(0));
                        HatchetTracks tracks = tracksMap.get(albumInfo);
                        Album album = (Album) mItemsToBeFilled.get(infoRequestData.getRequestId());
                        InfoSystemUtils.fillAlbum(album, albumInfo, image);
                        if (tracks != null) {
                            InfoSystemUtils.fillAlbum(album, tracks.tracks);
                        }
                        hatchetCollection.addAlbum(album);
                        for (Query query : album.getQueries()) {
                            hatchetCollection.addAlbumTrack(album, query);
                        }
                    }
                }
            } else if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS) {
                if (infoRequestData.getInfoResult() != null) {
                    User user = (User) mItemsToBeFilled.get(infoRequestData.getRequestId());
                    HatchetUsers users = (HatchetUsers) infoRequestData.getInfoResult();
                    if (users.users != null && users.users.size() > 0) {
                        Map<String, HatchetArtistInfo> artistInfoMap
                                = new HashMap<String, HatchetArtistInfo>();
                        if (users.artists != null) {
                            for (HatchetArtistInfo artistInfo : users.artists) {
                                artistInfoMap.put(artistInfo.id, artistInfo);
                            }
                        }
                        Map<String, HatchetImage> imageMap = new HashMap<String, HatchetImage>();
                        if (users.images != null) {
                            for (HatchetImage image : users.images) {
                                imageMap.put(image.id, image);
                            }
                        }
                        Map<String, HatchetTrackInfo> trackInfoMap
                                = new HashMap<String, HatchetTrackInfo>();
                        if (users.tracks != null) {
                            for (HatchetTrackInfo trackInfo : users.tracks) {
                                trackInfoMap.put(trackInfo.id, trackInfo);
                            }
                        }
                        InfoSystemUtils.fillUser(user, users.users.get(0), trackInfoMap,
                                artistInfoMap, imageMap);
                    }
                }
            } else if (infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_SOCIALACTIONS
                    || infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_FRIENDSFEED) {
                if (infoRequestData.getInfoResult() != null) {
                    User user = (User) mItemsToBeFilled.get(infoRequestData.getRequestId());
                    HatchetSocialActionResponse response
                            = (HatchetSocialActionResponse) infoRequestData.getInfoResult();
                    if (response.socialActions != null && response.socialActions.size() > 0) {
                        Map<String, HatchetTrackInfo> trackInfoMap
                                = new HashMap<String, HatchetTrackInfo>();
                        if (response.tracks != null) {
                            for (HatchetTrackInfo trackInfo : response.tracks) {
                                trackInfoMap.put(trackInfo.id, trackInfo);
                            }
                        }
                        Map<String, HatchetArtistInfo> artistInfoMap
                                = new HashMap<String, HatchetArtistInfo>();
                        if (response.artists != null) {
                            for (HatchetArtistInfo artistInfo : response.artists) {
                                artistInfoMap.put(artistInfo.id, artistInfo);
                            }
                        }
                        Map<String, HatchetAlbumInfo> albumInfoMap
                                = new HashMap<String, HatchetAlbumInfo>();
                        if (response.albums != null) {
                            for (HatchetAlbumInfo albumInfo : response.albums) {
                                albumInfoMap.put(albumInfo.id, albumInfo);
                            }
                        }
                        Map<String, HatchetUserInfo> userInfoMap
                                = new HashMap<String, HatchetUserInfo>();
                        if (response.users != null) {
                            for (HatchetUserInfo userInfo : response.users) {
                                userInfoMap.put(userInfo.id, userInfo);
                            }
                        }
                        ArrayList<SocialAction> socialActions = new ArrayList<SocialAction>();
                        for (HatchetSocialAction hatchetSocialAction : response.socialActions) {
                            socialActions.add(InfoSystemUtils
                                    .convertToSocialAction(hatchetSocialAction, trackInfoMap,
                                            artistInfoMap, albumInfoMap, userInfoMap));
                        }
                        if (infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_SOCIALACTIONS) {
                            user.setSocialActions(socialActions);
                        } else if (infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_FRIENDSFEED) {
                            user.setFriendsFeed(socialActions);
                        }
                    }
                }
            } else if (infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYBACKLOG) {
                if (infoRequestData.getInfoResult() != null) {
                    User user = (User) mItemsToBeFilled.get(infoRequestData.getRequestId());
                    HatchetPlaybackLogsResponse response
                            = (HatchetPlaybackLogsResponse) infoRequestData.getInfoResult();
                    if (response.playbackLogEntries != null
                            && response.playbackLogEntries.size() > 0) {
                        Map<String, HatchetPlaybackItemResponse> playbackItemMap
                                = new HashMap<String, HatchetPlaybackItemResponse>();
                        if (response.playbackLogEntries != null) {
                            for (HatchetPlaybackItemResponse playbackItem : response.playbackLogEntries) {
                                playbackItemMap.put(playbackItem.id, playbackItem);
                            }
                        }
                        Map<String, HatchetTrackInfo> trackInfoMap
                                = new HashMap<String, HatchetTrackInfo>();
                        if (response.tracks != null) {
                            for (HatchetTrackInfo trackInfo : response.tracks) {
                                trackInfoMap.put(trackInfo.id, trackInfo);
                            }
                        }
                        Map<String, HatchetArtistInfo> artistInfoMap
                                = new HashMap<String, HatchetArtistInfo>();
                        if (response.artists != null) {
                            for (HatchetArtistInfo artistInfo : response.artists) {
                                artistInfoMap.put(artistInfo.id, artistInfo);
                            }
                        }
                        ArrayList<Query> playbackItems = InfoSystemUtils
                                .convertToQueryList(response.playbackLog, playbackItemMap,
                                        trackInfoMap,
                                        artistInfoMap);
                        user.setPlaybackLog(playbackItems);
                    }
                }
            } else if (infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_FOLLOWERS
                    || infoRequestData.getType()
                    == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_FOLLOWINGS) {
                if (infoRequestData.getInfoResult() != null) {
                    User user = (User) mItemsToBeFilled.get(infoRequestData.getRequestId());
                    HatchetUsers users = (HatchetUsers) infoRequestData.getInfoResult();
                    if (users.users != null && users.users.size() > 0) {
                        Map<String, HatchetArtistInfo> artistInfoMap
                                = new HashMap<String, HatchetArtistInfo>();
                        if (users.artists != null) {
                            for (HatchetArtistInfo artistInfo : users.artists) {
                                artistInfoMap.put(artistInfo.id, artistInfo);
                            }
                        }
                        Map<String, HatchetImage> imageMap = new HashMap<String, HatchetImage>();
                        if (users.images != null) {
                            for (HatchetImage image : users.images) {
                                imageMap.put(image.id, image);
                            }
                        }
                        Map<String, HatchetTrackInfo> trackInfoMap
                                = new HashMap<String, HatchetTrackInfo>();
                        if (users.tracks != null) {
                            for (HatchetTrackInfo trackInfo : users.tracks) {
                                trackInfoMap.put(trackInfo.id, trackInfo);
                            }
                        }
                        ArrayList<User> convertedUsers = new ArrayList<User>();
                        for (HatchetUserInfo userInfo : users.users) {
                            User convertedUser = InfoSystemUtils
                                    .convertToUser(userInfo, trackInfoMap, artistInfoMap, imageMap);
                            convertedUsers.add(convertedUser);
                        }
                        if (infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_FOLLOWERS) {
                            user.setFollowers(convertedUsers);
                        } else {
                            user.setFollowings(convertedUsers);
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the user id of the currently logged in Hatchet user
     */
    private void getUserid() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        Map<String, String> data = new HashMap<String, String>();
        data.put(HATCHET_ACCOUNTDATA_USER_ID, null);
        TomahawkUtils.getUserDataForAccount(mContext, data,
                AuthenticatorUtils.AUTHENTICATOR_NAME_HATCHET);
        mUserId = data.get(HATCHET_ACCOUNTDATA_USER_ID);
        String userName = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET).getUserName();
        if (mUserId == null && userName != null) {
            // If we couldn't fetch the user's id from the account's userData, get it from the API.
            Multimap<String, String> params = HashMultimap.create(1, 1);
            params.put(HATCHET_PARAM_NAME, userName);
            String query = buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_USERS,
                    params);
            String rawJsonString = TomahawkUtils.httpGet(query);
            HatchetUsers users = mObjectMapper.readValue(rawJsonString, HatchetUsers.class);
            if (users.users != null && users.users.size() > 0) {
                mUserId = users.users.get(0).id;
                data = new HashMap<String, String>();
                data.put(HATCHET_ACCOUNTDATA_USER_ID, mUserId);
                TomahawkUtils.setUserDataForAccount(mContext, data,
                        AuthenticatorUtils.AUTHENTICATOR_NAME_HATCHET);
            }
        }
    }

    /**
     * _send_ data to the Hatchet API (e.g. nowPlaying, playbackLogs etc.)
     */
    public void send(final InfoRequestData infoRequestData) {
        TomahawkRunnable runnable = new TomahawkRunnable(
                TomahawkRunnable.PRIORITY_IS_INFOSYSTEM_MEDIUM) {
            @Override
            public void run() {
                ArrayList<String> doneRequestsIds = new ArrayList<String>();
                if (mObjectMapper == null) {
                    mObjectMapper = InfoSystemUtils.constructObjectMapper();
                }
                try {
                    // Before we do anything, get the accesstoken
                    String accessToken = mHatchetAuthenticatorUtils.ensureAccessTokens();
                    if (accessToken != null) {
                        if (infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES
                                || infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES_NOWPLAYING
                                || infoRequestData.getType()
                                == InfoRequestData.INFOREQUESTDATA_TYPE_SOCIALACTIONS) {
                            String data = infoRequestData.getJsonStringToSend();
                            Map<String, String> extraHeaders = new HashMap<String, String>();
                            extraHeaders.put(HATCHET_PARAMS_AUTHORIZATION, accessToken);
                            TomahawkUtils.httpPost(buildQuery(infoRequestData.getType(), null),
                                    extraHeaders, data, TomahawkUtils.HTTP_CONTENT_TYPE_JSON);
                            doneRequestsIds.add(infoRequestData.getRequestId());
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "send: " + e.getClass() + ": " + e.getLocalizedMessage());
                } catch (IOException e) {
                    Log.e(TAG, "send: " + e.getClass() + ": " + e.getLocalizedMessage());
                } catch (NoSuchAlgorithmException e) {
                    Log.e(TAG, "send: " + e.getClass() + ": " + e.getLocalizedMessage());
                } catch (KeyManagementException e) {
                    Log.e(TAG, "send: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
                InfoSystem.getInstance().reportResults(doneRequestsIds);
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
                == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_ENTRIES
                || infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS) {
            priority = TomahawkRunnable.PRIORITY_IS_INFOSYSTEM_LOW;
        } else {
            priority = TomahawkRunnable.PRIORITY_IS_INFOSYSTEM_MEDIUM;
        }
        TomahawkRunnable runnable = new TomahawkRunnable(priority) {
            @Override
            public void run() {
                ArrayList<String> doneRequestsIds = new ArrayList<String>();
                if (mObjectMapper == null) {
                    mObjectMapper = InfoSystemUtils.constructObjectMapper();
                }
                try {
                    // Before we do anything, fetch the mUserId corresponding to the currently logged in
                    // user's username
                    getUserid();
                    if (getAndParseInfo(infoRequestData)) {
                        convertParsedItem(infoRequestData);
                        doneRequestsIds.add(infoRequestData.getRequestId());
                    }
                } catch (ClientProtocolException e) {
                    Log.e(TAG, "resolve: " + e.getClass() + ": " + e.getLocalizedMessage());
                } catch (IOException e) {
                    Log.e(TAG, "resolve: " + e.getClass() + ": " + e.getLocalizedMessage());
                } catch (NoSuchAlgorithmException e) {
                    Log.e(TAG, "resolve: " + e.getClass() + ": " + e.getLocalizedMessage());
                } catch (KeyManagementException e) {
                    Log.e(TAG, "resolve: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
                InfoSystem.getInstance().reportResults(doneRequestsIds);
            }
        };
        ThreadManager.getInstance().execute(runnable);
    }

    /**
     * Build a query URL for the given parameters, with which we can request the result JSON from
     * the Hatchet API
     *
     * @return the built query url
     */
    private static String buildQuery(int type, Multimap<String, String> paramsIn)
            throws UnsupportedEncodingException {
        Multimap<String, String> params = null;
        if (paramsIn != null) {
            params = LinkedListMultimap.create(paramsIn);
        }
        String queryString = null;
        java.util.Collection<String> paramStrings;
        Iterator<String> iterator;
        switch (type) {
            case InfoRequestData.INFOREQUESTDATA_TYPE_USERS:
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_USERS + "/";
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS:
                paramStrings = params.get(HATCHET_PARAM_ID);
                iterator = paramStrings.iterator();
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_USERS + "/"
                        + iterator.next() + "/"
                        + HATCHET_PLAYLISTS;
                params.removeAll(HATCHET_PARAM_ID);
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS:
                paramStrings = params.get(HATCHET_PARAM_ID);
                iterator = paramStrings.iterator();
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_USERS + "/"
                        + iterator.next() + "/"
                        + HATCHET_LOVEDITEMS;
                params.removeAll(HATCHET_PARAM_ID);
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_USERS_SOCIALACTIONS:
                paramStrings = params.get(HATCHET_PARAM_ID);
                iterator = paramStrings.iterator();
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_USERS + "/"
                        + iterator.next() + "/"
                        + HATCHET_SOCIALACTIONS;
                params.removeAll(HATCHET_PARAM_ID);
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_USERS_FRIENDSFEED:
                paramStrings = params.get(HATCHET_PARAM_ID);
                iterator = paramStrings.iterator();
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_USERS + "/"
                        + iterator.next() + "/"
                        + HATCHET_FRIENDSFEED;
                params.removeAll(HATCHET_PARAM_ID);
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYBACKLOG:
                paramStrings = params.get(HATCHET_PARAM_ID);
                iterator = paramStrings.iterator();
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_USERS + "/"
                        + iterator.next() + "/"
                        + HATCHET_PLAYBACKLOG;
                params.removeAll(HATCHET_PARAM_ID);
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_ENTRIES:
                paramStrings = params.get(HATCHET_PARAM_ID);
                iterator = paramStrings.iterator();
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_PLAYLISTS + "/"
                        + iterator.next() + "/"
                        + HATCHET_PLAYLISTS_ENTRIES;
                params.removeAll(HATCHET_PARAM_ID);
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS:
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_ARTISTS + "/";
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_ALBUMS:
                paramStrings = params.get(HATCHET_PARAM_ID);
                iterator = paramStrings.iterator();
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_ARTISTS + "/"
                        + iterator.next() + "/"
                        + HATCHET_ALBUMS + "/";
                params.removeAll(HATCHET_PARAM_ID);
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_TOPHITS:
                paramStrings = params.get(HATCHET_PARAM_ID);
                iterator = paramStrings.iterator();
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_ARTISTS + "/"
                        + iterator.next() + "/"
                        + HATCHET_ARTISTS_TOPHITS + "/";
                params.removeAll(HATCHET_PARAM_ID);
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_TRACKS:
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_TRACKS + "/";
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS:
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_ALBUMS + "/";
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_SEARCHES:
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_SEARCHES + "/";
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES:
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_PLAYBACKLOGENTRIES + "/";
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES_NOWPLAYING:
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_PLAYBACKLOGENTRIES + "/"
                        + HATCHET_PLAYBACKLOGENTRIES_NOWPLAYING + "/";
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_SOCIALACTIONS:
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_SOCIALACTIONS + "/";
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS:
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_RELATIONSHIPS + "/";
                break;
        }
        // append every parameter we didn't use
        if (params != null && params.size() > 0) {
            queryString += "?" + TomahawkUtils.paramsListToString(params);
        }
        return queryString;
    }
}
