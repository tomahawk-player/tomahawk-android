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

import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;

import java.util.ArrayList;
import java.util.HashMap;

public class InfoRequestData {

    private final static String TAG = InfoRequestData.class.getName();

    public static final int INFOREQUESTDATA_TYPE_ALBUMINFO = 0;

    public static final int INFOREQUESTDATA_TYPE_ARTISTALBUMS = 1;

    public static final int INFOREQUESTDATA_TYPE_ARTISTINFO = 2;

    public static final int INFOREQUESTDATA_TYPE_USERSINFO = 3;

    public static final int INFOREQUESTDATA_TYPE_PERSONINFO = 4;

    public static final int INFOREQUESTDATA_TYPE_USERPLAYLISTS = 5;

    public static final int INFOREQUESTDATA_TYPE_PLAYLISTINFO = 10;

    public static final int INFOREQUESTDATA_TYPE_PLAYLISTSENTRYINFO = 10;

    public static final int INFOREQUESTDATA_TYPE_PLAYLISTSINFO = 11;

    public static final int INFOREQUESTDATA_TYPE_USERPLAYBACKLOG = 12;

    public static final int INFOREQUESTDATA_TYPE_USERLOVED = 13;

    public static final int INFOREQUESTDATA_TYPE_USERFEED = 14;

    public static final int INFOREQUESTDATA_TYPE_TRACKCHARTS = 20;

    public static final int INFOREQUESTDATA_TYPE_ARTISTCHARTS = 21;

    public static final int INFOREQUESTDATA_TYPE_USERTRACKCHARTS = 22;

    public static final int INFOREQUESTDATA_TYPE_USERARTISTCHARTS = 23;

    public static final int INFOREQUESTDATA_TYPE_ALL_PLAYLISTS_FROM_USER = 30;

    private String mRequestId;

    private int mType;

    private String mQueryString;

    private HashMap<String, ArrayList<Info>> mInfoResults;

    private ArrayList<TomahawkBaseAdapter.TomahawkListItem> mConvertedResults;

    public InfoRequestData(String requestId, int type, String queryString) {
        mRequestId = requestId;
        mType = type;
        mQueryString = queryString;
    }

    public String getRequestId() {
        return mRequestId;
    }

    public int getType() {
        return mType;
    }

    public String getQueryString() {
        return mQueryString;
    }

    public HashMap<String, ArrayList<Info>> getInfoResults() {
        return mInfoResults;
    }

    public void setInfoResults(HashMap<String, ArrayList<Info>> infoResults) {
        mInfoResults = infoResults;
    }

    public ArrayList<TomahawkBaseAdapter.TomahawkListItem> getConvertedResults() {
        return mConvertedResults;
    }

    public void setConvertedResults(
            ArrayList<TomahawkBaseAdapter.TomahawkListItem> convertedResults) {
        mConvertedResults = convertedResults;
    }

    /*
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
        Artist artist;
        if (albumInfo.getArtist() != null) {
            artist = artistInfoToArtist(albumInfo.getArtist(),
                    Artist.get());
        } else {

        }
        if (album == null) {
            album = Album.get(albumInfo.getName());
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
            int trackNumCounter = 0;
            for (TrackInfo trackInfo : albumInfo.getTracks()) {
                Track track = trackInfoToTrack(trackInfo);
                track.setAlbum(album);
                track.setAlbumPos(trackNumCounter++);
                //album.addQuery(track);
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
            track = new Track(TomahawkApp.getUniqueId());
        }
        if (trackInfo.getArtist() != null) {
            track.setArtist(artistInfoToArtist(trackInfo.getArtist(),
                    new Artist(TomahawkApp.getUniqueId())));
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
            artist = new Artist(TomahawkApp.getUniqueId());
        }
        if (artistInfo.getId() != null) {
        }
        if (artistInfo.getName() != null) {
            artist.setName(artistInfo.getName());
        }
        return artist;
    }*/
}
