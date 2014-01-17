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
import org.codehaus.jackson.map.ObjectMapper;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Artist;
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

    public static final String HATCHET_USER = "users";

    public static final String HATCHET_PLAYLISTS = "playlists";

    public static final String HATCHET_PLAYLISTS_ENTRIES = "entries";

    public static final String HATCHET_PARAM_NAME = "name";

    public static final String HATCHET_PARAM_ID = "id";

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

    public String resolve(Artist artist) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(HATCHET_PARAM_NAME, artist.getName());
        String requestId = resolve(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS, params);
        mItemsToBeFilled.put(requestId, artist);
        return requestId;
    }

    public String resolve(int type, HashMap<String, String> params) {
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
        Map<String, String> params = new HashMap<String, String>(1);
        String rawJsonString;
        switch (infoRequestData.getType()) {
            case InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS_ALL:
                Map<PlaylistInfo, PlaylistEntries> resultMap
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
                    resultMap.put(playlistInfo, playlistEntries);
                }
                infoRequestData.setInfoResultMap(resultMap);
                return true;
            case InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS:
                rawJsonString = TomahawkUtils.httpsGet(
                        buildQuery(infoRequestData.getType(), infoRequestData.getParams()));
                infoRequestData.setInfoResult(
                        mObjectMapper.readValue(rawJsonString, Artists.class));
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
                    Map<String, String> params = new HashMap<String, String>();
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
                                        InfoSystemUtils.fillArtistWithArtistInfo(
                                                (Artist) mItemsToBeFilled
                                                        .get(infoRequestData.getRequestId()),
                                                artists.artists.get(0), image);
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

    private static String buildQuery(int type, Map<String, String> params)
            throws UnsupportedEncodingException {
        String queryString = null;
        switch (type) {
            case InfoRequestData.INFOREQUESTDATA_TYPE_USERS:
                queryString = InfoSystem.HATCHET_BASE_URL + "/"
                        + InfoSystem.HATCHET_VERSION + "/"
                        + InfoSystem.HATCHET_USER + "/";
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS:
                queryString = InfoSystem.HATCHET_BASE_URL + "/"
                        + InfoSystem.HATCHET_VERSION + "/"
                        + InfoSystem.HATCHET_USER + "/"
                        + params.remove(HATCHET_PARAM_ID) + "/"
                        + HATCHET_PLAYLISTS;
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_ENTRIES:
                queryString = InfoSystem.HATCHET_BASE_URL + "/"
                        + InfoSystem.HATCHET_VERSION + "/"
                        + InfoSystem.HATCHET_PLAYLISTS + "/"
                        + params.remove(HATCHET_PARAM_ID) + "/"
                        + HATCHET_PLAYLISTS_ENTRIES;
                break;
            case InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS:
                queryString = InfoSystem.HATCHET_BASE_URL + "/"
                        + InfoSystem.HATCHET_VERSION + "/"
                        + InfoSystem.HATCHET_ARTISTS + "/";
                break;
        }
        // append every parameter we didn't use
        if (params != null && params.size() > 0) {
            queryString += "?" + TomahawkUtils.paramsListToString(params);
        }
        return queryString;
    }
}
