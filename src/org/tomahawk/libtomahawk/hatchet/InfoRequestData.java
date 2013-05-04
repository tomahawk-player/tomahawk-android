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

import org.apache.http.client.methods.HttpGet;

import java.util.ArrayList;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 04.05.13
 */
public class InfoRequestData {

    private final static String TAG = InfoRequestData.class.getName();

    public static final int INFOREQUESTDATA_TYPE_ALBUMINFO = 0;

    public static final int INFOREQUESTDATA_TYPE_ARTISTINFO = 1;

    public static final int INFOREQUESTDATA_TYPE_USERINFO = 2;

    public static final int INFOREQUESTDATA_TYPE_PERSONINFO = 3;

    public static final int INFOREQUESTDATA_TYPE_USERPLAYLISTS = 4;

    public static final int INFOREQUESTDATA_TYPE_PLAYLISTINFO = 10;

    public static final int INFOREQUESTDATA_TYPE_USERPLAYBACKLOG = 11;

    public static final int INFOREQUESTDATA_TYPE_USERLOVED = 12;

    public static final int INFOREQUESTDATA_TYPE_USERFEED = 13;

    public static final int INFOREQUESTDATA_TYPE_TRACKCHARTS = 20;

    public static final int INFOREQUESTDATA_TYPE_ARTISTCHARTS = 21;

    public static final int INFOREQUESTDATA_TYPE_USERTRACKCHARTS = 22;

    public static final int INFOREQUESTDATA_TYPE_USERARTISTCHARTS = 23;

    private String mRequestId;

    private int mType;

    private boolean mUseCache;

    private String mFirstParam;

    private String mSecondParam;

    private String mCacheKey;

    private HttpGet mRequestGet;

    public Info mResult;

    public ArrayList<Info> mResultList;

    public String getRequestId() {
        return mRequestId;
    }

    public int getType() {
        return mType;
    }

    public boolean isUseCache() {
        return mUseCache;
    }

    public String getFirstParam() {
        return mFirstParam;
    }

    public String getSecondParam() {
        return mSecondParam;
    }

    public String getCacheKey() {
        return mCacheKey;
    }

    public HttpGet getRequestGet() {
        return mRequestGet;
    }

    public InfoRequestData(String requestId, int type, boolean useCache) {
        mRequestId = requestId;
        mType = type;
        mUseCache = useCache;
        mCacheKey = "" + mType;
        mRequestGet = buildRequestGet();
    }

    public InfoRequestData(String requestId, int type, boolean useCache, String firstParam) {
        mRequestId = requestId;
        mType = type;
        mUseCache = useCache;
        mFirstParam = firstParam;
        mCacheKey = mType + "/" + mFirstParam;
        mRequestGet = buildRequestGet(mFirstParam);
    }

    public InfoRequestData(String requestId, int type, boolean useCache, String firstParam,
            String secondParam) {
        mRequestId = requestId;
        mType = type;
        mUseCache = useCache;
        mFirstParam = firstParam;
        mSecondParam = secondParam;
        mCacheKey = mType + "/" + mFirstParam + "/" + mSecondParam;
        mRequestGet = buildRequestGet(mFirstParam, mSecondParam);
    }

    private HttpGet buildRequestGet() {
        HttpGet httpGet = null;
        switch (mType) {
            case INFOREQUESTDATA_TYPE_TRACKCHARTS:
                httpGet = new HttpGet(
                        InfoSystem.HATCHET_BASE_URL + "/" + InfoSystem.HATCHET_TRACK_PATH + "/"
                                + InfoSystem.HATCHET_CHARTS_PATH);
                break;
            case INFOREQUESTDATA_TYPE_ARTISTCHARTS:
                httpGet = new HttpGet(
                        InfoSystem.HATCHET_BASE_URL + "/" + InfoSystem.HATCHET_ARTIST_PATH + "/"
                                + InfoSystem.HATCHET_CHARTS_PATH);
                break;
        }
        return httpGet;
    }

    private HttpGet buildRequestGet(String firstParam) {
        HttpGet httpGet = null;
        switch (mType) {
            case INFOREQUESTDATA_TYPE_ARTISTINFO:
                httpGet = new HttpGet(
                        InfoSystem.HATCHET_BASE_URL + "/" + InfoSystem.HATCHET_ARTIST_PATH + "/"
                                + firstParam + "/" + InfoSystem.HATCHET_INFO_PATH);
                break;
            case INFOREQUESTDATA_TYPE_USERINFO:
                httpGet = new HttpGet(
                        InfoSystem.HATCHET_BASE_URL + "/" + InfoSystem.HATCHET_USER_PATH + "/"
                                + firstParam + "/" + InfoSystem.HATCHET_INFO_PATH);
                break;
            case INFOREQUESTDATA_TYPE_PERSONINFO:
                httpGet = new HttpGet(
                        InfoSystem.HATCHET_BASE_URL + "/" + InfoSystem.HATCHET_PERSON_PATH + "/"
                                + firstParam + "/" + InfoSystem.HATCHET_INFO_PATH);
                break;
            case INFOREQUESTDATA_TYPE_USERPLAYLISTS:
                httpGet = new HttpGet(
                        InfoSystem.HATCHET_BASE_URL + "/" + InfoSystem.HATCHET_USER_PATH + "/"
                                + firstParam + "/" + InfoSystem.HATCHET_PLAYLISTS_PATH);
                break;
            case INFOREQUESTDATA_TYPE_USERPLAYBACKLOG:
                httpGet = new HttpGet(
                        InfoSystem.HATCHET_BASE_URL + "/" + InfoSystem.HATCHET_USER_PATH + "/"
                                + firstParam + "/" + InfoSystem.HATCHET_PLAYBACKLOG_PATH);
                break;
            case INFOREQUESTDATA_TYPE_USERLOVED:
                httpGet = new HttpGet(
                        InfoSystem.HATCHET_BASE_URL + "/" + InfoSystem.HATCHET_USER_PATH + "/"
                                + firstParam + "/" + InfoSystem.HATCHET_LOVED_PATH);
                break;
            case INFOREQUESTDATA_TYPE_USERFEED:
                httpGet = new HttpGet(
                        InfoSystem.HATCHET_BASE_URL + "/" + InfoSystem.HATCHET_USER_PATH + "/"
                                + firstParam + "/" + InfoSystem.HATCHET_FEED_PATH);
                break;
            case INFOREQUESTDATA_TYPE_USERTRACKCHARTS:
                httpGet = new HttpGet(
                        InfoSystem.HATCHET_BASE_URL + "/" + InfoSystem.HATCHET_USER_PATH + "/"
                                + firstParam + "/" + InfoSystem.HATCHET_TRACKS_PATH + "/"
                                + InfoSystem.HATCHET_CHARTS_PATH);
                break;
            case INFOREQUESTDATA_TYPE_USERARTISTCHARTS:
                httpGet = new HttpGet(
                        InfoSystem.HATCHET_BASE_URL + "/" + InfoSystem.HATCHET_USER_PATH + "/"
                                + firstParam + "/" + InfoSystem.HATCHET_ARTISTS_PATH + "/"
                                + InfoSystem.HATCHET_CHARTS_PATH);
                break;
        }
        return httpGet;
    }

    private HttpGet buildRequestGet(String firstParam, String secondParam) {
        HttpGet httpGet = null;
        switch (mType) {
            case INFOREQUESTDATA_TYPE_ALBUMINFO:
                httpGet = new HttpGet(
                        InfoSystem.HATCHET_BASE_URL + "/" + InfoSystem.HATCHET_ALBUM_PATH + "/"
                                + firstParam + "/" + secondParam + "/"
                                + InfoSystem.HATCHET_INFO_PATH);
                break;
            case INFOREQUESTDATA_TYPE_PLAYLISTINFO:
                httpGet = new HttpGet(
                        InfoSystem.HATCHET_BASE_URL + "/" + InfoSystem.HATCHET_USER_PATH + "/"
                                + firstParam + "/" + InfoSystem.HATCHET_PLAYLIST_PATH + "/"
                                + secondParam + "/" + InfoSystem.HATCHET_INFO_PATH);
                break;
        }
        return httpGet;
    }
}
