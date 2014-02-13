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

import com.google.common.collect.Multimap;

import java.util.List;
import java.util.Map;

/**
 * The parameter-object which is being used in the class InfoSystem to define the request and later
 * on store results.
 */
public class InfoRequestData {

    public static final int INFOREQUESTDATA_TYPE_TRACKS = 400;

    public static final int INFOREQUESTDATA_TYPE_ARTISTS = 600;

    public static final int INFOREQUESTDATA_TYPE_ARTISTS_TOPHITS = 601;

    public static final int INFOREQUESTDATA_TYPE_ARTISTS_ALBUMS = 602;

    public static final int INFOREQUESTDATA_TYPE_ALBUMS = 700;

    public static final int INFOREQUESTDATA_TYPE_USERS = 800;

    public static final int INFOREQUESTDATA_TYPE_USERS_PLAYLISTS = 801;

    public static final int INFOREQUESTDATA_TYPE_USERS_PLAYLISTS_ALL = 802;

    public static final int INFOREQUESTDATA_TYPE_PLAYLISTS_ENTRIES = 1000;

    public static final int INFOREQUESTDATA_TYPE_SEARCHES = 1100;

    private String mRequestId;

    private int mType;

    private Multimap<String, String> mParams;

    /**
     * Storage member-variable. Used if a single Info object is the result.
     */
    private Object mInfoResult;

    /**
     * A Map of several Info objects.
     */
    private Map<String, Map> mInfoResultMap;

    /**
     * In the case, that we search for a given keyword, we are directly converting the results
     * inside the InfoSystem and storing them in this Map
     */
    private Map<String, List> mConvertedResultMap;

    public InfoRequestData(String requestId, int type, Multimap<String, String> params) {
        mRequestId = requestId;
        mType = type;
        mParams = params;
    }

    public String getRequestId() {
        return mRequestId;
    }

    public int getType() {
        return mType;
    }

    public Object getInfoResult() {
        return mInfoResult;
    }

    public void setInfoResult(Object infoResult) {
        mInfoResult = infoResult;
    }

    public Map<String, Map> getInfoResultMap() {
        return mInfoResultMap;
    }

    public void setInfoResultMap(Map<String, Map> infoResultMap) {
        mInfoResultMap = infoResultMap;
    }

    public Map<String, List> getConvertedResultMap() {
        return mConvertedResultMap;
    }

    public void setConvertedResultMap(Map<String, List> convertedResultMap) {
        mConvertedResultMap = convertedResultMap;
    }

    public Multimap<String, String> getParams() {
        return mParams;
    }
}
