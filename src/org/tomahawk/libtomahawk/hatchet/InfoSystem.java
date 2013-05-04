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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 20.04.13
 */
public class InfoSystem {

    private final static String TAG = InfoSystem.class.getName();

    public static final String INFOSYSTEM_RESULTSREPORTED = "infosystem_resultsreported";

    public static final String INFOSYSTEM_RESULTSREPORTED_REQUESTID
            = "infosystem_resultsreported_requestid";

    public static final String HATCHET_BASE_URL = "http://api.hatchet.is";

    public static final String HATCHET_ARTIST_PATH = "artist";

    public static final String HATCHET_ARTISTS_PATH = "artists";

    public static final String HATCHET_ALBUM_PATH = "album";

    public static final String HATCHET_ALBUMS_PATH = "albums";

    public static final String HATCHET_TRACK_PATH = "track";

    public static final String HATCHET_TRACKS_PATH = "tracks";

    public static final String HATCHET_USER_PATH = "user";

    public static final String HATCHET_PERSON_PATH = "person";

    public static final String HATCHET_INFO_PATH = "info";

    public static final String HATCHET_PLAYLIST_PATH = "playlist";

    public static final String HATCHET_PLAYLISTS_PATH = "playlists";

    public static final String HATCHET_PLAYBACKLOG_PATH = "playbacklog";

    public static final String HATCHET_LOVED_PATH = "loved";

    public static final String HATCHET_FEED_PATH = "feed";

    public static final String HATCHET_CHARTS_PATH = "charts";

    public static final String HATCHET_SIMILARITY_PATH = "similarity";

    TomahawkApp mTomahawkApp;

    private ConcurrentHashMap<String, InfoRequestData> mRequests
            = new ConcurrentHashMap<String, InfoRequestData>();

    private ConcurrentHashMap<String, Info> mCachedInfos = new ConcurrentHashMap<String, Info>();

    public InfoSystem(TomahawkApp tomahawkApp) {
        mTomahawkApp = tomahawkApp;
    }

    public void resolve(InfoRequestData infoRequestData) {
        mRequests.put(infoRequestData.getRequestId(), infoRequestData);
        new JSONResponseTask().execute(infoRequestData);
    }

    public InfoRequestData getInfoRequestById(String requestId) {
        return mRequests.get(requestId);
    }

    private class JSONResponseTask extends AsyncTask<InfoRequestData, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(InfoRequestData... infoRequestDatas) {
            ArrayList<String> doneRequestsIds = new ArrayList<String>();
            HttpClient httpClient = new DefaultHttpClient();
            StringBuilder builder = new StringBuilder();
            try {
                for (InfoRequestData infoRequestData : infoRequestDatas) {
                    long start = System.currentTimeMillis();
                    if (infoRequestData.isUseCache() && mCachedInfos
                            .containsKey(infoRequestData.getCacheKey())) {
                        infoRequestData.mResult = mCachedInfos.get(infoRequestData.getCacheKey());
                        doneRequestsIds.add(infoRequestData.getRequestId());
                    } else {
                        HttpGet httpGet = infoRequestData.getRequestGet();
                        HttpResponse httpResponse = httpClient.execute(httpGet);
                        StatusLine statusLine = httpResponse.getStatusLine();
                        if (statusLine.getStatusCode() == 200) {
                            HttpEntity httpEntity = httpResponse.getEntity();
                            InputStream content = httpEntity.getContent();
                            BufferedReader bufferedReader = new BufferedReader(
                                    new InputStreamReader(content));
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                builder.append(line);
                            }
                            JSONObject rawInfo = new JSONObject(builder.toString());
                            switch (infoRequestData.getType()) {
                                case InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMINFO:
                                    infoRequestData.mResult = new AlbumInfo();
                                    infoRequestData.mResult.parseInfo(rawInfo);
                                    mCachedInfos.put(infoRequestData.getCacheKey(),
                                            infoRequestData.mResult);
                                    doneRequestsIds.add(infoRequestData.getRequestId());
                                    break;
                                case InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTINFO:
                                    infoRequestData.mResult = new ArtistInfo();
                                    infoRequestData.mResult.parseInfo(rawInfo);
                                    mCachedInfos.put(infoRequestData.getCacheKey(),
                                            infoRequestData.mResult);
                                    doneRequestsIds.add(infoRequestData.getRequestId());
                                    break;
                                case InfoRequestData.INFOREQUESTDATA_TYPE_USERINFO:
                                    infoRequestData.mResult = new UserInfo();
                                    infoRequestData.mResult.parseInfo(rawInfo);
                                    mCachedInfos.put(infoRequestData.getCacheKey(),
                                            infoRequestData.mResult);
                                    doneRequestsIds.add(infoRequestData.getRequestId());
                                    break;
                                case InfoRequestData.INFOREQUESTDATA_TYPE_PERSONINFO:
                                    infoRequestData.mResult = new PersonInfo();
                                    infoRequestData.mResult.parseInfo(rawInfo);
                                    mCachedInfos.put(infoRequestData.getCacheKey(),
                                            infoRequestData.mResult);
                                    doneRequestsIds.add(infoRequestData.getRequestId());
                                    break;
                                case InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTINFO:
                                    infoRequestData.mResult = new PlaylistInfo();
                                    infoRequestData.mResult.parseInfo(rawInfo);
                                    mCachedInfos.put(infoRequestData.getCacheKey(),
                                            infoRequestData.mResult);
                                    doneRequestsIds.add(infoRequestData.getRequestId());
                                    break;
                                case InfoRequestData.INFOREQUESTDATA_TYPE_USERPLAYLISTS:
                                    infoRequestData.mResult = new PlaylistsInfo();
                                    infoRequestData.mResult.parseInfo(rawInfo);
                                    mCachedInfos.put(infoRequestData.getCacheKey(),
                                            infoRequestData.mResult);
                                    doneRequestsIds.add(infoRequestData.getRequestId());
                                    break;
                                case InfoRequestData.INFOREQUESTDATA_TYPE_USERPLAYBACKLOG:
                                    infoRequestData.mResult = new TrackActionItemsInfo();
                                    infoRequestData.mResult.parseInfo(rawInfo);
                                    mCachedInfos.put(infoRequestData.getCacheKey(),
                                            infoRequestData.mResult);
                                    doneRequestsIds.add(infoRequestData.getRequestId());
                                    break;
                                case InfoRequestData.INFOREQUESTDATA_TYPE_USERLOVED:
                                    infoRequestData.mResult = new TrackActionItemsInfo();
                                    infoRequestData.mResult.parseInfo(rawInfo);
                                    mCachedInfos.put(infoRequestData.getCacheKey(),
                                            infoRequestData.mResult);
                                    doneRequestsIds.add(infoRequestData.getRequestId());
                                    break;
                                case InfoRequestData.INFOREQUESTDATA_TYPE_USERFEED:

                                case InfoRequestData.INFOREQUESTDATA_TYPE_TRACKCHARTS:
                                    infoRequestData.mResult = new TrackChartItemsInfo();
                                    infoRequestData.mResult.parseInfo(rawInfo);
                                    mCachedInfos.put(infoRequestData.getCacheKey(),
                                            infoRequestData.mResult);
                                    doneRequestsIds.add(infoRequestData.getRequestId());
                                    break;
                                case InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTCHARTS:
                                    infoRequestData.mResult = new ArtistChartItemsInfo();
                                    infoRequestData.mResult.parseInfo(rawInfo);
                                    mCachedInfos.put(infoRequestData.getCacheKey(),
                                            infoRequestData.mResult);
                                    doneRequestsIds.add(infoRequestData.getRequestId());
                                    break;
                                case InfoRequestData.INFOREQUESTDATA_TYPE_USERTRACKCHARTS:
                                    infoRequestData.mResult = new TrackChartItemsInfo();
                                    infoRequestData.mResult.parseInfo(rawInfo);
                                    mCachedInfos.put(infoRequestData.getCacheKey(),
                                            infoRequestData.mResult);
                                    doneRequestsIds.add(infoRequestData.getRequestId());
                                    break;
                                case InfoRequestData.INFOREQUESTDATA_TYPE_USERARTISTCHARTS:
                                    infoRequestData.mResult = new ArtistChartItemsInfo();
                                    infoRequestData.mResult.parseInfo(rawInfo);
                                    mCachedInfos.put(infoRequestData.getCacheKey(),
                                            infoRequestData.mResult);
                                    doneRequestsIds.add(infoRequestData.getRequestId());
                                    break;
                            }
                        } else {
                            Log.e(TAG,
                                    "JSONResponseTask Failed to download: StatusCode='" + statusLine
                                            .getStatusCode() + "', URI='" + httpGet.getURI()
                                            .toString() + "'");
                        }
                    }
                    Log.d(TAG, "doInBackground(...) took " + (System.currentTimeMillis() - start)
                            + "ms to finish, useCache = " + infoRequestData.isUseCache());
                }
            } catch (ClientProtocolException e) {
                Log.e(TAG, "JSONResponseTask: " + e.getClass() + ": " + e.getLocalizedMessage());
            } catch (IOException e) {
                Log.e(TAG, "JSONResponseTask: " + e.getClass() + ": " + e.getLocalizedMessage());
            } catch (JSONException e) {
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
}
