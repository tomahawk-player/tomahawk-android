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

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class InfoSystem {

    private final static String TAG = InfoSystem.class.getName();

    public static final String INFOSYSTEM_RESULTSREPORTED = "infosystem_resultsreported";

    public static final String INFOSYSTEM_RESULTSREPORTED_REQUESTID
            = "infosystem_resultsreported_requestid";

    public static final String HATCHET_BASE_URL = "https://api.hatchet.is";

    public static final String HATCHET_VERSION = "v1";

    public static final String HATCHET_ARTIST_PATH = "artist";

    public static final String HATCHET_ARTISTS_PATH = "artists";

    public static final String HATCHET_ALBUM_PATH = "album";

    public static final String HATCHET_ALBUMS_PATH = "albums";

    public static final String HATCHET_TRACK_PATH = "track";

    public static final String HATCHET_TRACKS_PATH = "tracks";

    public static final String HATCHET_USER_PATH = "users";

    public static final String HATCHET_PERSON_PATH = "person";

    public static final String HATCHET_INFO_PATH = "info";

    public static final String HATCHET_PLAYLIST_PATH = "playlist";

    public static final String HATCHET_PLAYLISTS_PATH = "playlists";

    public static final String HATCHET_PLAYLISTENTRY_PATH = "entries";

    public static final String HATCHET_PLAYBACKLOG_PATH = "playbacklog";

    public static final String HATCHET_LOVED_PATH = "loved";

    public static final String HATCHET_FEED_PATH = "feed";

    public static final String HATCHET_CHARTS_PATH = "charts";

    public static final String HATCHET_SIMILARITY_PATH = "similarity";

    public static final String HATCHET_NAME_PATH = "name";

    public static final String HATCHET_KEY_USERS = "users";

    public static final String HATCHET_KEY_PLAYLISTS = "playlists";

    public static final String HATCHET_KEY_ALBUMS = "albums";

    public static final String HATCHET_KEY_ARTISTS = "artists";

    public static final String HATCHET_KEY_PLAYLISTENTRIES = "playlistEntries";

    public static final String HATCHET_KEY_TRACKS = "tracks";

    TomahawkApp mTomahawkApp;

    private ConcurrentHashMap<String, InfoRequestData> mRequests
            = new ConcurrentHashMap<String, InfoRequestData>();

    public InfoSystem(TomahawkApp tomahawkApp) {
        mTomahawkApp = tomahawkApp;
    }

    public String resolve(int type, HashMap<String, String> params) {
        try {
            String requestId = TomahawkApp.getUniqueStringId();
            String queryString = buildQuery(type, params);
            InfoRequestData infoRequestData = new InfoRequestData(requestId, type, queryString);
            resolve(infoRequestData);
            return infoRequestData.getRequestId();
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "InfoRequestData (constructor): " + e.getClass() + ": " + e
                    .getLocalizedMessage());
        }
        return null;
    }

    public void resolve(InfoRequestData infoRequestData) {
        mRequests.put(infoRequestData.getRequestId(), infoRequestData);
        new JSONResponseTask().execute(infoRequestData);
    }

    public InfoRequestData getInfoRequestById(String requestId) {
        return mRequests.get(requestId);
    }

    private boolean getAndParseInfo(InfoRequestData infoRequestData) {
        try {
            long start = System.currentTimeMillis();
            JSONObject rawInfo = new JSONObject(
                    TomahawkUtils.httpsGet(infoRequestData.getQueryString()));
            HashMap<String, ArrayList<Info>> result
                    = new HashMap<String, ArrayList<Info>>();
            ArrayList<TomahawkBaseAdapter.TomahawkListItem> convertedResults
                    = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
            switch (infoRequestData.getType()) {
                case InfoRequestData.INFOREQUESTDATA_TYPE_ALL_PLAYLISTS_FROM_USER:
                    if (!rawInfo.isNull(HATCHET_KEY_USERS)) {
                        UserInfo userInfo = new UserInfo(
                                rawInfo.getJSONArray(HATCHET_KEY_USERS).getJSONObject(0));
                        HashMap<String, String> params = new HashMap<String, String>();
                        params.put(UserInfo.USERINFO_KEY_ID, userInfo.getId());
                        rawInfo = new JSONObject(TomahawkUtils.httpsGet(buildQuery(
                                InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTSINFO,
                                params)));
                        if (!rawInfo.isNull(HATCHET_KEY_PLAYLISTS)) {
                            JSONArray jsonArray = rawInfo.getJSONArray(HATCHET_KEY_PLAYLISTS);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                PlaylistInfo playlistInfo = new PlaylistInfo(
                                        jsonArray.getJSONObject(i));
                                params.clear();
                                params.put(PlaylistInfo.PLAYLISTINFO_KEY_ID, playlistInfo.getId());
                                rawInfo = new JSONObject(TomahawkUtils.httpsGet(buildQuery(
                                        InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTSENTRYINFO,
                                        params)));
                                ArrayList<PlaylistEntryInfo> playlistEntryInfos
                                        = new ArrayList<PlaylistEntryInfo>();
                                HashMap<String, ArtistInfo> artistInfos
                                        = new HashMap<String, ArtistInfo>();
                                ArrayList<AlbumInfo> albumInfos
                                        = new ArrayList<AlbumInfo>();
                                HashMap<String, TrackInfo> trackInfos
                                        = new HashMap<String, TrackInfo>();
                                if (!rawInfo.isNull(HATCHET_KEY_PLAYLISTENTRIES)) {
                                    JSONArray array = rawInfo
                                            .getJSONArray(HATCHET_KEY_PLAYLISTENTRIES);
                                    for (int j = 0; j < array.length(); j++) {
                                        PlaylistEntryInfo playlistEntryInfo = new PlaylistEntryInfo(
                                                array.getJSONObject(j));
                                        playlistEntryInfos.add(playlistEntryInfo);
                                    }
                                }
                                if (!rawInfo.isNull(HATCHET_KEY_ARTISTS)) {
                                    JSONArray array = rawInfo.getJSONArray(HATCHET_KEY_ARTISTS);
                                    for (int j = 0; j < array.length(); j++) {
                                        ArtistInfo artistInfo = new ArtistInfo(
                                                array.getJSONObject(j));
                                        artistInfos.put(artistInfo.getId(), artistInfo);
                                    }
                                }
                                if (!rawInfo.isNull(HATCHET_KEY_TRACKS)) {
                                    JSONArray array = rawInfo.getJSONArray(HATCHET_KEY_TRACKS);
                                    for (int j = 0; j < array.length(); j++) {
                                        TrackInfo trackInfo = new TrackInfo(
                                                array.getJSONObject(j));
                                        trackInfos.put(trackInfo.getId(), trackInfo);
                                    }
                                }
                                if (!rawInfo.isNull(HATCHET_KEY_ALBUMS)) {
                                    JSONArray array = rawInfo.getJSONArray(HATCHET_KEY_ALBUMS);
                                    for (int j = 0; j < array.length(); j++) {
                                        AlbumInfo albumInfo = new AlbumInfo(
                                                array.getJSONObject(j));
                                        albumInfos.add(albumInfo);
                                    }
                                }
                                convertedResults.add(playlistInfoToUserPlaylist(playlistInfo,
                                        playlistEntryInfos, artistInfos, trackInfos, albumInfos));
                            }
                        }
                    }
                    infoRequestData.setConvertedResults(convertedResults);
                    return true;
                case InfoRequestData.INFOREQUESTDATA_TYPE_USERSINFO:
                    if (!rawInfo.isNull(HATCHET_KEY_USERS)) {
                        JSONArray jsonArray = rawInfo.getJSONArray(HATCHET_KEY_USERS);
                        ArrayList<Info> infos = new ArrayList<Info>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            infos.add(new UserInfo(jsonArray.getJSONObject(i)));
                        }
                        result.put(HATCHET_KEY_USERS, infos);
                    }
                    infoRequestData.setInfoResults(result);
                    return true;
                case InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTSINFO:
                    if (!rawInfo.isNull(HATCHET_KEY_PLAYLISTS)) {
                        JSONArray jsonArray = rawInfo.getJSONArray(HATCHET_KEY_PLAYLISTS);
                        ArrayList<Info> infos = new ArrayList<Info>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            infos.add(new PlaylistInfo(jsonArray.getJSONObject(i)));
                        }
                        result.put(HATCHET_KEY_PLAYLISTS, infos);
                    }
                    infoRequestData.setInfoResults(result);
                    return true;
                case InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTSENTRYINFO:
                    if (!rawInfo.isNull(HATCHET_KEY_PLAYLISTENTRIES)) {
                        JSONArray jsonArray = rawInfo.getJSONArray(HATCHET_KEY_PLAYLISTENTRIES);
                        ArrayList<Info> infos = new ArrayList<Info>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            infos.add(new PlaylistEntryInfo(jsonArray.getJSONObject(i)));
                        }
                        result.put(HATCHET_KEY_PLAYLISTENTRIES, infos);
                    }
                    if (!rawInfo.isNull(HATCHET_KEY_TRACKS)) {
                        JSONArray jsonArray = rawInfo.getJSONArray(HATCHET_KEY_TRACKS);
                        ArrayList<Info> infos = new ArrayList<Info>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            infos.add(new TrackInfo(jsonArray.getJSONObject(i)));
                        }
                        result.put(HATCHET_KEY_TRACKS, infos);
                    }
                    if (!rawInfo.isNull(HATCHET_KEY_ARTISTS)) {
                        JSONArray jsonArray = rawInfo.getJSONArray(HATCHET_KEY_ARTISTS);
                        ArrayList<Info> infos = new ArrayList<Info>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            infos.add(new ArtistInfo(jsonArray.getJSONObject(i)));
                        }
                        result.put(HATCHET_KEY_ARTISTS, infos);
                    }
                    if (!rawInfo.isNull(HATCHET_KEY_ALBUMS)) {
                        JSONArray jsonArray = rawInfo.getJSONArray(HATCHET_KEY_ALBUMS);
                        ArrayList<Info> infos = new ArrayList<Info>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            infos.add(new AlbumInfo(jsonArray.getJSONObject(i)));
                        }
                        result.put(HATCHET_KEY_ALBUMS, infos);
                    }
                    infoRequestData.setInfoResults(result);
                    return true;
            }
            Log.d(TAG, "doInBackground(...) took " + (System.currentTimeMillis() - start)
                    + "ms to finish");
        } catch (ClientProtocolException e) {
            Log.e(TAG, "JSONResponseTask: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG, "JSONResponseTask: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (JSONException e) {
            Log.e(TAG, "JSONResponseTask: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "JSONResponseTask: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (KeyManagementException e) {
            Log.e(TAG, "JSONResponseTask: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return false;
    }

    private class JSONResponseTask extends AsyncTask<InfoRequestData, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(InfoRequestData... infoRequestDatas) {
            ArrayList<String> doneRequestsIds = new ArrayList<String>();
            for (InfoRequestData infoRequestData : infoRequestDatas) {
                if (getAndParseInfo(infoRequestData)) {
                    doneRequestsIds.add(infoRequestData.getRequestId());
                }
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

    private static String buildQuery(int type, HashMap<String, String> params)
            throws UnsupportedEncodingException {
        String queryString = null;
        switch (type) {
            case InfoRequestData.INFOREQUESTDATA_TYPE_USERSINFO:
                queryString = InfoSystem.HATCHET_BASE_URL + "/"
                        + InfoSystem.HATCHET_VERSION + "/"
                        + InfoSystem.HATCHET_USER_PATH + "/";
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_ALL_PLAYLISTS_FROM_USER:
                queryString = InfoSystem.HATCHET_BASE_URL + "/"
                        + InfoSystem.HATCHET_VERSION + "/"
                        + InfoSystem.HATCHET_USER_PATH + "/";
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTSINFO:
                queryString = InfoSystem.HATCHET_BASE_URL + "/"
                        + InfoSystem.HATCHET_VERSION + "/"
                        + InfoSystem.HATCHET_USER_PATH + "/"
                        + params.remove(UserInfo.USERINFO_KEY_ID) + "/"
                        + HATCHET_PLAYLISTS_PATH;
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTSENTRYINFO:
                queryString = InfoSystem.HATCHET_BASE_URL + "/"
                        + InfoSystem.HATCHET_VERSION + "/"
                        + InfoSystem.HATCHET_PLAYLISTS_PATH + "/"
                        + params.remove(PlaylistInfo.PLAYLISTINFO_KEY_ID) + "/"
                        + HATCHET_PLAYLISTENTRY_PATH;
                break;
        }
        // append every parameter we didn't use
        queryString += TomahawkUtils.paramsListToString(params);
        return queryString;
    }

    private UserPlaylist playlistInfoToUserPlaylist(PlaylistInfo playlistInfo,
            ArrayList<PlaylistEntryInfo> playlistEntryInfos,
            HashMap<String, ArtistInfo> artistInfos,
            HashMap<String, TrackInfo> trackInfos, ArrayList<AlbumInfo> albumInfos) {
        ArrayList<Query> queries = new ArrayList<Query>();
        for (PlaylistEntryInfo playlistEntryInfo : playlistEntryInfos) {
            TrackInfo trackInfo = trackInfos.get(playlistEntryInfo.getTrack());
            ArtistInfo artistInfo = artistInfos.get(trackInfo.getArtist());
            AlbumInfo albumInfo = null;
            for (AlbumInfo info : albumInfos) {
                for (String track : info.getTracks()) {
                    if (track.equals(trackInfo.getId())) {
                        albumInfo = info;
                        break;
                    }
                }
                if (albumInfo != null) {
                    break;
                }
            }
            String albumname = "";
            if (albumInfo != null) {
                albumname = albumInfo.getName();
            }
            queries.add(new Query(trackInfo.getName(), albumname, artistInfo.getName(), false));
        }
        return UserPlaylist.fromQueryList(TomahawkApp.getUniqueId(), playlistInfo.getTitle(),
                queries);
    }
}
