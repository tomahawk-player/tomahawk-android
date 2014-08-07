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

    public static final int INFOREQUESTDATA_TYPE_PLAYLISTS = 1001;

    public static final int INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES = 1002;

    public static final int INFOREQUESTDATA_TYPE_SEARCHES = 1100;

    public static final int INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES = 1200;

    public static final int INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES_NOWPLAYING = 1201;

    public static final int INFOREQUESTDATA_TYPE_SOCIALACTIONS = 1300;

    public static final int HTTPTYPE_GET = 0;

    public static final int HTTPTYPE_POST = 1;

    public static final int HTTPTYPE_PUT = 2;

    public static final int HTTPTYPE_DELETE = 3;

    private String mRequestId;

    private int mType;

    private int mHttpType;

    private QueryParams mQueryParams;

    private String mJsonStringToSend;

    private int mLoggedOpId;

    /**
     * Storage member-variable. Used if a single object is the result.
     */
    private Map<Class, Object> mResultMap;

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
        mRequestId = requestId;
        mType = type;
        mQueryParams = params;
        mLoggedOpId = loggedOpId;
        mHttpType = httpType;
        mJsonStringToSend = jsonStringToSend;
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

    public <T> T getResult(Class<T> clss) {
        if (mResultMap != null) {
            Object object = mResultMap.get(clss);
            if (object != null && object.getClass() == clss) {
                return (T) object;
            }
        }
        return null;
    }

    public void setResult(Object object) {
        if (mResultMap == null) {
            mResultMap = new HashMap<Class, Object>();
        }
        mResultMap.put(object.getClass(), object);
    }

    public <T> List<T> getResultList(Class<T> clss) {
        if (mResultListMap != null) {
            List<Object> objects = mResultListMap.get(clss);
            if (objects != null && objects.size() > 0 && objects.get(0).getClass() == clss) {
                return (List<T>) objects;
            }
        }
        return new ArrayList<T>();
    }

    public void setResultList(List<Object> objects) {
        if (mResultListMap == null) {
            mResultListMap = new HashMap<Class, List<Object>>();
        }
        if (objects.size() > 0) {
            mResultListMap.put(objects.get(0).getClass(), objects);
        }
    }

    public QueryParams getQueryParams() {
        return mQueryParams;
    }

    public String getJsonStringToSend() {
        return mJsonStringToSend;
    }

    public int getLoggedOpId() {
        return mLoggedOpId;
    }
}
