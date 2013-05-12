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
import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.Track;
import org.tomahawk.tomahawk_android.TomahawkApp;

import java.util.ArrayList;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 04.05.13
 */
public class InfoRequestData {

    private final static String TAG = InfoRequestData.class.getName();

    public static final int INFOREQUESTDATA_TYPE_ALBUMINFO = 0;

    public static final int INFOREQUESTDATA_TYPE_ARTISTALBUMS = 1;

    public static final int INFOREQUESTDATA_TYPE_ARTISTINFO = 2;

    public static final int INFOREQUESTDATA_TYPE_USERINFO = 3;

    public static final int INFOREQUESTDATA_TYPE_PERSONINFO = 4;

    public static final int INFOREQUESTDATA_TYPE_USERPLAYLISTS = 5;

    public static final int INFOREQUESTDATA_TYPE_PLAYLISTINFO = 10;

    public static final int INFOREQUESTDATA_TYPE_USERPLAYBACKLOG = 11;

    public static final int INFOREQUESTDATA_TYPE_USERLOVED = 12;

    public static final int INFOREQUESTDATA_TYPE_USERFEED = 13;

    public static final int INFOREQUESTDATA_TYPE_TRACKCHARTS = 20;

    public static final int INFOREQUESTDATA_TYPE_ARTISTCHARTS = 21;

    public static final int INFOREQUESTDATA_TYPE_USERTRACKCHARTS = 22;

    public static final int INFOREQUESTDATA_TYPE_USERARTISTCHARTS = 23;

    static TomahawkApp mTomahawkApp;

    private String mRequestId;

    private int mType;

    private boolean mUseCache;

    private String mFirstParam;

    private String mSecondParam;

    private String mCacheKey;

    private HttpGet mRequestGet;

    public Info mResult;

    public InfoRequestData(TomahawkApp tomahawkApp, String requestId, int type, boolean useCache) {
        mTomahawkApp = tomahawkApp;
        mRequestId = requestId;
        mType = type;
        mUseCache = useCache;
        mCacheKey = "" + mType;
        mRequestGet = buildRequestGet();
    }

    public InfoRequestData(TomahawkApp tomahawkApp, String requestId, int type, boolean useCache,
            String firstParam) {
        mTomahawkApp = tomahawkApp;
        mRequestId = requestId;
        mType = type;
        mUseCache = useCache;
        mFirstParam = prepareString(firstParam);
        mCacheKey = mType + "/" + mFirstParam;
        mRequestGet = buildRequestGet(mFirstParam);
    }

    public InfoRequestData(TomahawkApp tomahawkApp, String requestId, int type, boolean useCache,
            String firstParam, String secondParam) {
        mTomahawkApp = tomahawkApp;
        mRequestId = requestId;
        mType = type;
        mUseCache = useCache;
        mFirstParam = prepareString(firstParam);
        mSecondParam = prepareString(secondParam);
        mCacheKey = mType + "/" + mFirstParam + "/" + mSecondParam;
        mRequestGet = buildRequestGet(mFirstParam, mSecondParam);
    }

    private String prepareString(String in) {
        return in.replace(" ", "%20");
    }

    public static ArrayList<Album> albumInfoListToAlbumList(ArrayList<AlbumInfo> albumInfos) {
        ArrayList<Album> albums = new ArrayList<Album>();
        for (AlbumInfo albumInfo : albumInfos) {
            albums.add(albumInfoToAlbum(albumInfo));
        }
        return albums;
    }

    public static Album albumInfoToAlbum(AlbumInfo albumInfo) {
        return albumInfoToAlbum(albumInfo, null);
    }

    public static Album albumInfoToAlbum(AlbumInfo albumInfo, Album album) {
        if (album == null) {
            album = new Album();
        }
        album.setId(mTomahawkApp.getUniqueAlbumId());
        if (albumInfo.getArtist() != null) {
            album.setArtist(artistInfoToArtist(albumInfo.getArtist(), new Artist()));
        }
        if (albumInfo.getId() != null) {
        }
        if (albumInfo.getImages() != null && albumInfo.getImages().size() > 0) {
            album.setAlbumArtPath(albumInfo.getImages().get(0).getUrl());
        }
        if (albumInfo.getName() != null) {
            album.setName(albumInfo.getName());
        }
        if (albumInfo.getReleaseDate() != null) {
            album.setFirstYear(String.valueOf(albumInfo.getReleaseDate().getYear()));
            album.setLastYear(String.valueOf(albumInfo.getReleaseDate().getYear()));
        }
        if (albumInfo.getTracks() != null) {
            for (TrackInfo trackInfo : albumInfo.getTracks()) {
                Track track = trackInfoToTrack(trackInfo);
                track.setAlbum(album);
                album.addTrack(track);
            }
        }
        return album;
    }

    public static ArrayList<Track> trackInfoListToTrackList(ArrayList<TrackInfo> trackInfos) {
        ArrayList<Track> tracks = new ArrayList<Track>();
        for (TrackInfo trackInfo : trackInfos) {
            tracks.add(trackInfoToTrack(trackInfo));
        }
        return tracks;
    }

    public static Track trackInfoToTrack(TrackInfo trackInfo) {
        return trackInfoToTrack(trackInfo, null);
    }

    public static Track trackInfoToTrack(TrackInfo trackInfo, Track track) {
        if (track == null) {
            track = new Track();
        }
        track.setId(mTomahawkApp.getUniqueTrackId());
        track.setLocal(false);
        if (trackInfo.getArtist() != null) {
            track.setArtist(artistInfoToArtist(trackInfo.getArtist(), new Artist()));
        }
        if (trackInfo.getId() != null) {
        }
        if (trackInfo.getName() != null) {
            track.setName(trackInfo.getName());
        }
        if (trackInfo.getUrl() != null) {
            track.setLinkUrl(trackInfo.getUrl());
        }
        return track;
    }

    public static Artist artistInfoToArtist(ArtistInfo artistInfo) {
        return artistInfoToArtist(artistInfo, null);
    }

    public static Artist artistInfoToArtist(ArtistInfo artistInfo, Artist artist) {
        if (artist == null) {
            artist = new Artist();
        }
        artist.setId(mTomahawkApp.getUniqueArtistId());
        if (artistInfo.getId() != null) {
        }
        if (artistInfo.getName() != null) {
            artist.setName(artistInfo.getName());
        }
        return artist;
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
            case INFOREQUESTDATA_TYPE_ARTISTALBUMS:
                httpGet = new HttpGet(
                        InfoSystem.HATCHET_BASE_URL + "/" + InfoSystem.HATCHET_ARTIST_PATH + "/"
                                + InfoSystem.HATCHET_NAME_PATH + "/" + firstParam + "/"
                                + InfoSystem.HATCHET_ALBUMS_PATH);
                break;
            case INFOREQUESTDATA_TYPE_ARTISTINFO:
                httpGet = new HttpGet(
                        InfoSystem.HATCHET_BASE_URL + "/" + InfoSystem.HATCHET_ARTIST_PATH + "/"
                                + "/" + InfoSystem.HATCHET_NAME_PATH + "/" + firstParam + "/"
                                + InfoSystem.HATCHET_INFO_PATH);
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
                                + "/" + InfoSystem.HATCHET_NAME_PATH + "/" + firstParam + "/"
                                + secondParam + "/" + InfoSystem.HATCHET_INFO_PATH);
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
}
