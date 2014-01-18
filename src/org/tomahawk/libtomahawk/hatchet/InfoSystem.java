/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.libtomahawk.hatchet;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.apache.http.client.ClientProtocolException;
import org.codehaus.jackson.map.ObjectMapper;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;
import org.tomahawk.tomahawk_android.services.TomahawkService;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class InfoSystem {

    private final static String TAG = InfoSystem.class.getName();

    public static final String INFOSYSTEM_RESULTSREPORTED = "infosystem_resultsreported";

    public static final String INFOSYSTEM_RESULTSREPORTED_REQUESTID
            = "infosystem_resultsreported_requestid";

    public static final String HATCHET_BASE_URL = "https://api.hatchet.is";

    public static final String HATCHET_VERSION = "v1";

    public static final String HATCHET_ARTISTS = "artists";

    public static final String HATCHET_ALBUMS = "albums";

    public static final String HATCHET_TRACKS = "tracks";

    public static final String HATCHET_IMAGES = "images";

    public static final String HATCHET_USER = "users";

    public static final String HATCHET_PLAYLISTS = "playlists";

    public static final String HATCHET_PLAYLISTS_ENTRIES = "entries";

    public static final String HATCHET_SEARCHES = "searches";

    public static final String HATCHET_SEARCHITEM_TYPE_ALBUM = "album";

    public static final String HATCHET_SEARCHITEM_TYPE_ARTIST = "artist";

    public static final double HATCHET_SEARCHITEM_MIN_SCORE = 5.0;

    public static final String HATCHET_PARAM_NAME = "name";

    public static final String HATCHET_PARAM_ID = "id";

    public static final String HATCHET_PARAM_IDARRAY = "ids[]";

    public static final String HATCHET_PARAM_ARTIST_NAME = "artist_name";

    public static final String HATCHET_PARAM_TERM = "term";

    public static final String HATCHET_ACCOUNTDATA_USER_ID = "hatchet_preference_user_id";

    TomahawkApp mTomahawkApp;

    private ObjectMapper mObjectMapper;

    private static String mUserId = null;

    private ConcurrentHashMap<String, InfoRequestData> mRequests
            = new ConcurrentHashMap<String, InfoRequestData>();

    private ConcurrentHashMap<String, TomahawkBaseAdapter.TomahawkListItem> mItemsToBeFilled
            = new ConcurrentHashMap<String, TomahawkBaseAdapter.TomahawkListItem>();

    public InfoSystem(TomahawkApp tomahawkApp) {
        mTomahawkApp = tomahawkApp;
    }

    public String search(String keyword) {
        Multimap<String, String> params = HashMultimap.create(1, 1);
        params.put(HATCHET_PARAM_TERM, keyword);
        return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_SEARCHES, params);
    }

    public String resolve(Artist artist) {
        Multimap<String, String> params = HashMultimap.create(1, 1);
        params.put(HATCHET_PARAM_NAME, artist.getName());
        String requestId = resolve(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS, params);
        mItemsToBeFilled.put(requestId, artist);
        params.clear();
        params.put(HATCHET_PARAM_NAME, artist.getName());
        requestId = resolve(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_ALBUMS, params);
        mItemsToBeFilled.put(requestId, artist);
        return requestId;
    }

    public String resolve(Album album) {
        Multimap<String, String> params = HashMultimap.create(2, 1);
        params.put(HATCHET_PARAM_NAME, album.getName());
        params.put(HATCHET_PARAM_ARTIST_NAME, album.getArtist().getName());
        String requestId = resolve(InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS, params);
        mItemsToBeFilled.put(requestId, album);
        return requestId;
    }

    public String resolve(int type, Multimap<String, String> params) {
        String requestId = TomahawkApp.getSessionUniqueStringId();
        InfoRequestData infoRequestData = new InfoRequestData(requestId, type, params);
        resolve(infoRequestData);
        return infoRequestData.getRequestId();
    }

    public void resolve(InfoRequestData infoRequestData) {
        mRequests.put(infoRequestData.getRequestId(), infoRequestData);
        new JSONResponseTask().execute(infoRequestData);
    }

    public InfoRequestData getInfoRequestById(String requestId) {
        return mRequests.get(requestId);
    }

    private boolean getAndParseInfo(InfoRequestData infoRequestData)
            throws NoSuchAlgorithmException, KeyManagementException, IOException {
        long start = System.currentTimeMillis();
        Multimap<String, String> params = HashMultimap.create();
        Map<String, Map> resultMapList = new HashMap<String, Map>();
        Map<String, List> convertedResultMap = new HashMap<String, List>();
        String rawJsonString;
        if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS_ALL) {
            Map<PlaylistInfo, PlaylistEntries> playlistEntriesMap
                    = new HashMap<PlaylistInfo, PlaylistEntries>();
            params.put(HATCHET_PARAM_ID, mUserId);
            rawJsonString = TomahawkUtils.httpsGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS, params));
            Playlists playlists = mObjectMapper.readValue(rawJsonString, Playlists.class);
            for (PlaylistInfo playlistInfo : playlists.playlists) {
                params.clear();
                params.put(HATCHET_PARAM_ID, playlistInfo.id);
                rawJsonString = TomahawkUtils.httpsGet(
                        buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_ENTRIES,
                                params));
                PlaylistEntries playlistEntries = mObjectMapper
                        .readValue(rawJsonString, PlaylistEntries.class);
                playlistEntriesMap.put(playlistInfo, playlistEntries);
            }
            resultMapList.put(HATCHET_PLAYLISTS_ENTRIES, playlistEntriesMap);
            infoRequestData.setInfoResultMap(resultMapList);
            return true;
        } else if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS) {
            params.putAll(infoRequestData.getParams());
            rawJsonString = TomahawkUtils.httpsGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS, params));
            infoRequestData.setInfoResult(
                    mObjectMapper.readValue(rawJsonString, Artists.class));
            return true;
        } else if (infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_ALBUMS) {
            params.putAll(infoRequestData.getParams());
            Map<AlbumInfo, Tracks> tracksMap = new HashMap<AlbumInfo, Tracks>();
            Map<AlbumInfo, Image> imageMap = new HashMap<AlbumInfo, Image>();
            rawJsonString = TomahawkUtils.httpsGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS, params));
            Artists artists = mObjectMapper.readValue(rawJsonString, Artists.class);

            if (artists.artists != null && artists.artists.size() > 0) {
                params.put(HATCHET_PARAM_ID, artists.artists.get(0).id);
                rawJsonString = TomahawkUtils.httpsGet(
                        buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_ALBUMS,
                                params));
                Charts charts = mObjectMapper.readValue(rawJsonString, Charts.class);
                Map<String, Image> chartImageMap = new HashMap<String, Image>();
                for (Image image : charts.images) {
                    chartImageMap.put(image.id, image);
                }
                for (AlbumInfo albumInfo : charts.albums) {
                    if (albumInfo.images != null && albumInfo.images.size() > 0) {
                        imageMap.put(albumInfo, chartImageMap.get(albumInfo.images.get(0)));
                    }
                    params.clear();
                    for (String trackId : albumInfo.tracks) {
                        params.put(HATCHET_PARAM_IDARRAY, trackId);
                    }
                    rawJsonString = TomahawkUtils.httpsGet(
                            buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_TRACKS, params));
                    Tracks tracks = mObjectMapper.readValue(rawJsonString, Tracks.class);
                    tracksMap.put(albumInfo, tracks);
                }
                resultMapList.put(HATCHET_TRACKS, tracksMap);
                resultMapList.put(HATCHET_IMAGES, imageMap);
                infoRequestData.setInfoResultMap(resultMapList);
            }
            return true;
        } else if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS) {
            params.putAll(infoRequestData.getParams());
            rawJsonString = TomahawkUtils.httpsGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS, params));
            infoRequestData.setInfoResult(
                    mObjectMapper.readValue(rawJsonString, Albums.class));
            return true;
        } else if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_SEARCHES) {
            params.putAll(infoRequestData.getParams());
            rawJsonString = TomahawkUtils.httpsGet(
                    buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_SEARCHES, params));
            Search search = mObjectMapper.readValue(rawJsonString, Search.class);
            Map<String, AlbumInfo> albumInfoMap = new HashMap<String, AlbumInfo>();
            for (AlbumInfo albumInfo : search.albums) {
                albumInfoMap.put(albumInfo.id, albumInfo);
            }
            Map<String, ArtistInfo> artistInfoMap = new HashMap<String, ArtistInfo>();
            for (ArtistInfo artistInfo : search.artists) {
                artistInfoMap.put(artistInfo.id, artistInfo);
            }
            Map<String, Image> imageMap = new HashMap<String, Image>();
            for (Image image : search.images) {
                imageMap.put(image.id, image);
            }
            List<Album> albums = new ArrayList<Album>();
            List<Artist> artists = new ArrayList<Artist>();
            for (SearchItem searchItem : search.searchResults) {
                if (searchItem.score > HATCHET_SEARCHITEM_MIN_SCORE) {
                    if (HATCHET_SEARCHITEM_TYPE_ALBUM.equals(searchItem.type)) {
                        AlbumInfo albumInfo = albumInfoMap.get(searchItem.album);
                        if (albumInfo != null) {
                            Image image = null;
                            if (albumInfo.images != null && albumInfo.images.size() > 0) {
                                image = imageMap.get(albumInfo.images.get(0));
                            }
                            albums.add(InfoSystemUtils.albumInfoToAlbum(albumInfo,
                                    artistInfoMap.get(albumInfo.artist).name, null, image));
                        }
                    } else if (HATCHET_SEARCHITEM_TYPE_ARTIST.equals(searchItem.type)) {
                        ArtistInfo artistInfo = artistInfoMap.get(searchItem.artist);
                        if (artistInfo != null) {
                            Image image = null;
                            if (artistInfo.images != null && artistInfo.images.size() > 0) {
                                image = imageMap.get(artistInfo.images.get(0));
                            }
                            artists.add(InfoSystemUtils.artistInfoToArtist(artistInfo, image));
                        }
                    }
                }
            }
            convertedResultMap.put(HATCHET_ALBUMS, albums);
            convertedResultMap.put(HATCHET_ARTISTS, artists);
            infoRequestData.setConvertedResultMap(convertedResultMap);
            return true;
        }
        Log.d(TAG, "doInBackground(...) took " + (System.currentTimeMillis() - start)
                + "ms to finish");
        return false;
    }

    private class JSONResponseTask extends AsyncTask<InfoRequestData, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(InfoRequestData... infoRequestDatas) {
            ArrayList<String> doneRequestsIds = new ArrayList<String>();
            if (mObjectMapper == null) {
                mObjectMapper = new ObjectMapper();
            }
            try {
                // Before we do anything, fetch the mUserId corresponding to the currently logged in
                // user's username
                Account account = null;
                AccountManager am = AccountManager.get(mTomahawkApp.getApplicationContext());
                // If mUserId isn't set yet, try to fetch it from the hatchet account's userData
                if (mUserId == null && am != null) {
                    Account[] accounts = am
                            .getAccountsByType(mTomahawkApp.getString(R.string.accounttype_string));
                    if (accounts != null) {
                        for (Account acc : accounts) {
                            if (TomahawkService.AUTHENTICATOR_NAME_HATCHET.equals(
                                    am.getUserData(acc, TomahawkService.AUTHENTICATOR_NAME))) {
                                mUserId = am.getUserData(acc, HATCHET_ACCOUNTDATA_USER_ID);
                                account = acc;
                            }
                        }
                    }
                }
                // If we couldn't fetch the user's id from the account's userData, get it from the
                // API. Don't even bother to try if we don't have an account to store it with.
                if (mUserId == null && account != null) {
                    Multimap<String, String> params = HashMultimap.create(1, 1);
                    params.put(HATCHET_PARAM_NAME, AuthenticatorUtils.getUserId(mTomahawkApp,
                            TomahawkService.AUTHENTICATOR_NAME_HATCHET));
                    String query = buildQuery(InfoRequestData.INFOREQUESTDATA_TYPE_USERS,
                            params);
                    String rawJsonString = TomahawkUtils.httpsGet(query);
                    Users users = mObjectMapper.readValue(rawJsonString, Users.class);
                    if (users.users != null && users.users.size() > 0) {
                        mUserId = users.users.get(0).id;
                        am.setUserData(account, HATCHET_ACCOUNTDATA_USER_ID, mUserId);
                    }
                }
                for (InfoRequestData infoRequestData : infoRequestDatas) {
                    if (getAndParseInfo(infoRequestData)) {
                        doneRequestsIds.add(infoRequestData.getRequestId());
                        if (mItemsToBeFilled.containsKey(infoRequestData.getRequestId())) {
                            switch (infoRequestData.getType()) {
                                case InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS:
                                    Artists artists = ((Artists) infoRequestData.getInfoResult());
                                    if (artists.artists != null && artists.artists.size() > 0
                                            && artists.images != null && artists.images.size() > 0
                                            && artists.images != null
                                            && artists.images.size() > 0) {
                                        ArtistInfo artistInfo = artists.artists.get(0);
                                        String imageId = artistInfo.images.get(0);
                                        Image image = null;
                                        for (Image img : artists.images) {
                                            if (img.id.equals(imageId)) {
                                                image = img;
                                            }
                                        }
                                        Artist artist = (Artist) mItemsToBeFilled
                                                .get(infoRequestData.getRequestId());
                                        InfoSystemUtils.fillArtistWithArtistInfo(artist,
                                                artists.artists.get(0), image);
                                    }
                                    break;
                                case InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_ALBUMS:
                                    Artist artist = (Artist) mItemsToBeFilled
                                            .get(infoRequestData.getRequestId());
                                    InfoSystemUtils.fillArtistWithCharts(artist,
                                            infoRequestData.getInfoResultMap().get(HATCHET_TRACKS),
                                            infoRequestData.getInfoResultMap().get(HATCHET_IMAGES));
                                    break;
                                case InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS:
                                    Albums albums = ((Albums) infoRequestData.getInfoResult());
                                    if (albums.albums != null && albums.albums.size() > 0
                                            && albums.images != null && albums.images.size() > 0
                                            && albums.images != null
                                            && albums.images.size() > 0) {
                                        AlbumInfo albumInfo = albums.albums.get(0);
                                        String imageId = albumInfo.images.get(0);
                                        Image image = null;
                                        for (Image img : albums.images) {
                                            if (img.id.equals(imageId)) {
                                                image = img;
                                            }
                                        }
                                        InfoSystemUtils.fillAlbumWithAlbumInfo(
                                                (Album) mItemsToBeFilled.get(
                                                        infoRequestData.getRequestId()),
                                                albums.albums.get(0), image);
                                    }
                                    break;
                            }
                        }
                    }
                }
            } catch (ClientProtocolException e) {
                Log.e(TAG, "JSONResponseTask: " + e.getClass() + ": " + e.getLocalizedMessage());
            } catch (IOException e) {
                Log.e(TAG, "JSONResponseTask: " + e.getClass() + ": " + e.getLocalizedMessage());
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "JSONResponseTask: " + e.getClass() + ": " + e.getLocalizedMessage());
            } catch (KeyManagementException e) {
                Log.e(TAG, "JSONResponseTask: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
            return doneRequestsIds;
        }

        @Override
        protected void onPostExecute(ArrayList<String> doneRequestsIds) {
            for (String doneRequestId : doneRequestsIds) {
                sendReportResultsBroadcast(doneRequestId);
            }
        }
    }

    /**
     * Send a broadcast containing the id of the resolved inforequest.
     */
    private void sendReportResultsBroadcast(String requestId) {
        Intent reportIntent = new Intent(INFOSYSTEM_RESULTSREPORTED);
        reportIntent.putExtra(INFOSYSTEM_RESULTSREPORTED_REQUESTID, requestId);
        mTomahawkApp.sendBroadcast(reportIntent);
    }

    private static String buildQuery(int type, Multimap<String, String> params)
            throws UnsupportedEncodingException {
        String queryString = null;
        java.util.Collection<String> paramStrings;
        Iterator<String> iterator;
        switch (type) {
            case InfoRequestData.INFOREQUESTDATA_TYPE_USERS:
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_USER + "/";
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS:
                paramStrings = params.get(HATCHET_PARAM_ID);
                iterator = paramStrings.iterator();
                queryString = HATCHET_BASE_URL + "/"
                        + HATCHET_VERSION + "/"
                        + HATCHET_USER + "/"
                        + iterator.next() + "/"
                        + HATCHET_PLAYLISTS;
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
        }
        // append every parameter we didn't use
        if (params != null && params.size() > 0) {
            queryString += "?" + TomahawkUtils.paramsListToString(params);
        }
        return queryString;
    }
}
