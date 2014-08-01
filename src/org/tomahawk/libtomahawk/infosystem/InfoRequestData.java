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
package org.tomahawk.libtomahawk.infosystem;

import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The parameter-object which is being used in the class InfoSystem to define the request and later
 * on store results.
 */
public class InfoRequestData {

    private final static String TAG = InfoRequestData.class.getSimpleName();

    public static final int INFOREQUESTDATA_TYPE_TRACKS = 400;

    public static final int INFOREQUESTDATA_TYPE_ARTISTS = 600;

    public static final int INFOREQUESTDATA_TYPE_ARTISTS_TOPHITS = 601;

    public static final int INFOREQUESTDATA_TYPE_ARTISTS_ALBUMS = 602;

    public static final int INFOREQUESTDATA_TYPE_ALBUMS = 700;

    public static final int INFOREQUESTDATA_TYPE_USERS = 800;

    public static final int INFOREQUESTDATA_TYPE_USERS_SELF = 801;

    public static final int INFOREQUESTDATA_TYPE_USERS_PLAYLISTS = 802;

    public static final int INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS = 803;

    public static final int INFOREQUESTDATA_TYPE_USERS_SOCIALACTIONS = 804;

    public static final int INFOREQUESTDATA_TYPE_USERS_FRIENDSFEED = 805;

    public static final int INFOREQUESTDATA_TYPE_USERS_PLAYBACKLOG = 806;

    public static final int INFOREQUESTDATA_TYPE_RELATIONSHIPS = 900;

    public static final int INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_FOLLOWERS = 901;

    public static final int INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_FOLLOWINGS = 902;

    public static final int INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_STARREDARTISTS = 903;

    public static final int INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_STARREDALBUMS = 904;

    public static final int INFOREQUESTDATA_TYPE_PLAYLISTS_ENTRIES = 1000;

    public static final int INFOREQUESTDATA_TYPE_SEARCHES = 1100;

    public static final int INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES = 1200;

    public static final int INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES_NOWPLAYING = 1201;

    public static final int INFOREQUESTDATA_TYPE_SOCIALACTIONS = 1300;

    private String mRequestId;

    private int mType;

    private QueryParams mQueryParams;

    private String mJsonStringToSend;

    private int mOpLogId;

    /**
     * Storage member-variable. Used if a single object is the result.
     */
    private Map<Class, Object> mResultMap;

    /**
     * Storage member-variable. Used if one or several list of objects are the result.
     */
    private Map<Class, List<Object>> mResultListMap;

    public InfoRequestData(String requestId, int type, QueryParams queryParams) {
        mRequestId = requestId;
        mType = type;
        mQueryParams = queryParams;
    }

    public InfoRequestData(String requestId, int hatchetSpecificType, Object objectToSend) {
        mRequestId = requestId;
        mType = hatchetSpecificType;
        try {
            mJsonStringToSend = InfoSystemUtils.constructObjectMapper()
                    .writeValueAsString(objectToSend);
        } catch (IOException e) {
            Log.e(TAG, "InfoRequestData<constructor>: " + e.getClass() + ": " + e
                    .getLocalizedMessage());
        }
    }

    public InfoRequestData(String requestId, int opLogId, int hatchetSpecificType,
            String jsonStringToSend) {
        mRequestId = requestId;
        mOpLogId = opLogId;
        mType = hatchetSpecificType;
        mJsonStringToSend = jsonStringToSend;
    }

    public String getRequestId() {
        return mRequestId;
    }

    public int getType() {
        return mType;
    }

    public Map<Class, Object> getResultMap() {
        return mResultMap;
    }

    public void setResultMap(Map<Class, Object> resultMap) {
        mResultMap = resultMap;
    }

    public Map<Class, List<Object>> getResultListMap() {
        return mResultListMap;
    }

    public void setResultListMap(Map<Class, List<Object>> resultListMap) {
        mResultListMap = resultListMap;
    }

    public QueryParams getQueryParams() {
        return mQueryParams;
    }

    public String getJsonStringToSend() {
        return mJsonStringToSend;
    }

    public int getOpLogId() {
        return mOpLogId;
    }
}
