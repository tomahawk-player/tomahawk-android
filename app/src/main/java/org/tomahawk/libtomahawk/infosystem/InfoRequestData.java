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

import java.util.ArrayList;
import java.util.HashMap;
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

    public static final int INFOREQUESTDATA_TYPE_ARTISTS_TOPHITSANDALBUMS = 601;

    public static final int INFOREQUESTDATA_TYPE_ALBUMS = 700;

    public static final int INFOREQUESTDATA_TYPE_ALBUMS_TRACKS = 701;

    public static final int INFOREQUESTDATA_TYPE_USERS = 800;

    public static final int INFOREQUESTDATA_TYPE_USERS_PLAYLISTS = 801;

    public static final int INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS = 802;

    public static final int INFOREQUESTDATA_TYPE_USERS_LOVEDALBUMS = 803;

    public static final int INFOREQUESTDATA_TYPE_USERS_LOVEDARTISTS = 804;

    public static final int INFOREQUESTDATA_TYPE_USERS_PLAYBACKLOG = 805;

    public static final int INFOREQUESTDATA_TYPE_USERS_FOLLOWS = 806;

    public static final int INFOREQUESTDATA_TYPE_USERS_FOLLOWERS = 807;

    public static final int INFOREQUESTDATA_TYPE_RELATIONSHIPS = 900;

    public static final int INFOREQUESTDATA_TYPE_PLAYLISTS = 1000;

    public static final int INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES = 1001;

    public static final int INFOREQUESTDATA_TYPE_SEARCHES = 1100;

    public static final int INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES = 1200;

    public static final int INFOREQUESTDATA_TYPE_SOCIALACTIONS = 1300;

    public static final int HTTPTYPE_GET = 0;

    public static final int HTTPTYPE_POST = 1;

    public static final int HTTPTYPE_PUT = 2;

    public static final int HTTPTYPE_DELETE = 3;

    private final String mRequestId;

    private final int mType;

    private final int mHttpType;

    private final QueryParams mQueryParams;

    private String mJsonStringToSend;

    private int mLoggedOpId;

    private boolean mIsBackgroundRequest;

    /**
     * Storage member-variable. Used if one or several list of objects are the result.
     */
    private Map<Class, List<Object>> mResultListMap;

    /**
     * Constructor to be used for an InfoRequestData object in a "resolve" InfoSystem request
     *
     * @param requestId the id of the to be constructed InfoRequestData
     * @param type      the type which specifies the request inside an InfoPlugin
     * @param params    optional parameters to the request
     */
    public InfoRequestData(String requestId, int type, QueryParams params) {
        mRequestId = requestId;
        mType = type;
        mQueryParams = params;
        mHttpType = HTTPTYPE_GET;
    }

    /**
     * Constructor to be used for an InfoRequestData object in a "resolve" InfoSystem request
     *
     * @param requestId           the id of the to be constructed InfoRequestData
     * @param type                the type which specifies the request inside an InfoPlugin
     * @param params              optional parameters to the request
     * @param isBackgroundRequest boolean indicating whether or not this request should be run with
     *                            the lowest priority (useful for sync operations)
     */
    public InfoRequestData(String requestId, int type, QueryParams params,
            boolean isBackgroundRequest) {
        this(requestId, type, params);

        mIsBackgroundRequest = isBackgroundRequest;
    }

    /**
     * Constructor to be used for an InfoRequestData object in a "send" InfoSystem request
     *
     * @param requestId        the id of the to be constructed InfoRequestData
     * @param type             the type which specifies the request inside an InfoPlugin
     * @param params           optional parameters to the request
     * @param loggedOpId       the id of the stored loggedOp
     * @param httpType         the http type (get, put, post, delete)
     * @param jsonStringToSend the json string which will be sent via an InfoPlugin
     */
    public InfoRequestData(String requestId, int type, QueryParams params, int loggedOpId,
            int httpType, String jsonStringToSend) {
        this(requestId, type, params, httpType, jsonStringToSend);

        mLoggedOpId = loggedOpId;
    }

    /**
     * Constructor to be used for an InfoRequestData object in a "send" InfoSystem request
     *
     * @param requestId           the id of the to be constructed InfoRequestData
     * @param type                the type which specifies the request inside an InfoPlugin
     * @param params              optional parameters to the request
     * @param loggedOpId          the id of the stored loggedOp
     * @param httpType            the http type (get, put, post, delete)
     * @param jsonStringToSend    the json string which will be sent via an InfoPlugin
     * @param isBackgroundRequest boolean indicating whether or not this request should be run with
     *                            the lowest priority (useful for sync operations)
     */
    public InfoRequestData(String requestId, int type, QueryParams params, int loggedOpId,
            int httpType, String jsonStringToSend, boolean isBackgroundRequest) {
        this(requestId, type, params, loggedOpId, httpType, jsonStringToSend);

        mIsBackgroundRequest = isBackgroundRequest;
    }

    /**
     * Constructor to be used for an InfoRequestData object in a "send" InfoSystem request
     *
     * @param requestId        the id of the to be constructed InfoRequestData
     * @param type             the type which specifies the request inside an InfoPlugin
     * @param params           optional parameters to the request
     * @param httpType         the http type (get, put, post, delete)
     * @param jsonStringToSend the json string which will be sent via an InfoPlugin
     */
    public InfoRequestData(String requestId, int type, QueryParams params, int httpType,
            String jsonStringToSend) {
        mRequestId = requestId;
        mType = type;
        mQueryParams = params;
        mHttpType = httpType;
        mJsonStringToSend = jsonStringToSend;
    }

    public String getRequestId() {
        return mRequestId;
    }

    public int getType() {
        return mType;
    }

    public int getHttpType() {
        return mHttpType;
    }

    public <T> List<T> getResultList(Class<T> clss) {
        if (mResultListMap != null) {
            List<Object> objects = mResultListMap.get(clss);
            if (objects != null && objects.size() > 0 && objects.get(0).getClass() == clss) {
                return (List<T>) objects;
            }
        }
        return new ArrayList<>();
    }

    public void setResultList(List objects) {
        if (objects.size() > 0 && objects.get(0) != null) {
            if (mResultListMap == null) {
                mResultListMap = new HashMap<>();
            }
            mResultListMap.put(objects.get(0).getClass(), objects);
        }
    }

    public QueryParams getQueryParams() {
        return mQueryParams;
    }

    public String getJsonStringToSend() {
        return mJsonStringToSend;
    }

    public void setJsonStringToSend(String jsonStringToSend) {
        mJsonStringToSend = jsonStringToSend;
    }

    public int getLoggedOpId() {
        return mLoggedOpId;
    }

    public boolean isBackgroundRequest() {
        return mIsBackgroundRequest;
    }
}
