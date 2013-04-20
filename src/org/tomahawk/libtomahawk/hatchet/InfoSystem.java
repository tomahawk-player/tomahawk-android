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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomahawk.libtomahawk.resolver.TomahawkUtils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 20.04.13
 */
public class InfoSystem {

    private final static String TAG = InfoSystem.class.getName();

    public static String HATCHET_BASE_URL = "http://api.hatchet.is";

    public static String HATCHET_ARTIST_PATH = "artist";

    public static String HATCHET_ALBUM_PATH = "album";

    public static String HATCHET_TRACK_PATH = "track";

    public static String HATCHET_USER_PATH = "user";

    public static String HATCHET_PERSON_PATH = "person";

    public static String HATCHET_INFO_PATH = "info";

    public static String HATCHET_PLAYLIST_PATH = "playlist";

    public static String HATCHET_PLAYLISTS_PATH = "playlists";

    public static String HATCHET_PLAYBACKLOG_PATH = "playbacklog";

    public static String HATCHET_LOVED_PATH = "loved";

    public static String HATCHET_FEED_PATH = "feed";

    public static String HATCHET_CHARTS_PATH = "charts";

    public static String HATCHET_SIMILARITY_PATH = "similarity";

    public static String HATCHET_ALBUMS_PATH = "albums";

    private ConcurrentHashMap<String, AlbumInfo> mCachedAlbumInfos;

    private ConcurrentHashMap<String, ArtistInfo> mCachedArtistInfos;

    private ConcurrentHashMap<String, PersonInfo> mCachedPersonInfos;

    private ConcurrentHashMap<String, PlaylistInfo> mCachedPlaylistInfos;

    private ConcurrentHashMap<String, TrackInfo> mCachedTrackInfos;

    public AlbumInfo getAlbumInfo(String artistName, String albumName) {
        HttpGet httpGet = new HttpGet(
                HATCHET_BASE_URL + "/" + HATCHET_ALBUM_PATH + "/" + artistName + "/" + albumName
                        + "/" + HATCHET_INFO_PATH);
        JSONObject rawJSON = getJSONResponse(httpGet);
        return parseAlbumInfo(rawJSON);
    }

    public ArtistInfo getArtistInfo(String artistName) {
        HttpGet httpGet = new HttpGet(
                HATCHET_BASE_URL + "/" + HATCHET_ARTIST_PATH + "/" + artistName + "/"
                        + HATCHET_INFO_PATH);
        JSONObject rawJSON = getJSONResponse(httpGet);
        return parseArtistInfo(rawJSON);
    }

    private AlbumInfo parseAlbumInfo(JSONObject rawAlbumInfo) {
        AlbumInfo result = new AlbumInfo();
        try {
            if (rawAlbumInfo.has(AlbumInfo.ALBUMINFO_KEY_ARTIST)) {
                JSONObject rawArtistInfo = rawAlbumInfo
                        .getJSONObject(AlbumInfo.ALBUMINFO_KEY_ARTIST);
                result.setArtist(parseArtistInfo(rawArtistInfo));
            }
            if (rawAlbumInfo.has(AlbumInfo.ALBUMINFO_KEY_ID)) {
                result.setId(rawAlbumInfo.getString(AlbumInfo.ALBUMINFO_KEY_ID));
            }
            if (rawAlbumInfo.has(AlbumInfo.ALBUMINFO_KEY_IMAGES)) {
                JSONArray rawImageInfos = rawAlbumInfo.getJSONArray(AlbumInfo.ALBUMINFO_KEY_IMAGES);
                ArrayList<ImageInfo> images = new ArrayList<ImageInfo>();
                for (int i = 0; i < rawImageInfos.length(); i++) {
                    images.add(parseImageInfo(rawImageInfos.getJSONObject(i)));
                }
                result.setImages(images);
            }
            if (rawAlbumInfo.has(AlbumInfo.ALBUMINFO_KEY_LABELS)) {
                JSONArray rawImageInfos = rawAlbumInfo.getJSONArray(AlbumInfo.ALBUMINFO_KEY_LABELS);
                ArrayList<String> labels = new ArrayList<String>();
                for (int i = 0; i < rawImageInfos.length(); i++) {
                    labels.add(rawImageInfos.getString(i));
                }
                result.setLabels(labels);
            }
            if (rawAlbumInfo.has(AlbumInfo.ALBUMINFO_KEY_LENGTH)) {
                result.setLength(rawAlbumInfo.getInt(AlbumInfo.ALBUMINFO_KEY_LENGTH));
            }
            if (rawAlbumInfo.has(AlbumInfo.ALBUMINFO_KEY_NAME)) {
                result.setName(rawAlbumInfo.getString(AlbumInfo.ALBUMINFO_KEY_NAME));
            }
            if (rawAlbumInfo.has(AlbumInfo.ALBUMINFO_KEY_NAMES)) {
                JSONArray rawImageInfos = rawAlbumInfo.getJSONArray(AlbumInfo.ALBUMINFO_KEY_NAMES);
                ArrayList<String> names = new ArrayList<String>();
                for (int i = 0; i < rawImageInfos.length(); i++) {
                    names.add(rawImageInfos.getString(i));
                }
                result.setNames(names);
            }
            if (rawAlbumInfo.has(AlbumInfo.ALBUMINFO_KEY_PRODUCERS)) {
                JSONArray rawProducerInfos = rawAlbumInfo
                        .getJSONArray(AlbumInfo.ALBUMINFO_KEY_PRODUCERS);
                ArrayList<PersonInfo> producers = new ArrayList<PersonInfo>();
                for (int i = 0; i < rawProducerInfos.length(); i++) {
                    producers.add(parsePersonInfo(rawProducerInfos.getJSONObject(i)));
                }
                result.setProducers(producers);
            }
            if (rawAlbumInfo.has(AlbumInfo.ALBUMINFO_KEY_RELEASEDATE)) {
                result.setReleaseDate(TomahawkUtils
                        .stringToDate(rawAlbumInfo.getString(AlbumInfo.ALBUMINFO_KEY_RELEASEDATE)));
            }
            if (rawAlbumInfo.has(AlbumInfo.ALBUMINFO_KEY_TRACKS)) {
                JSONArray rawTrackInfos = rawAlbumInfo.getJSONArray(AlbumInfo.ALBUMINFO_KEY_TRACKS);
                ArrayList<TrackInfo> tracks = new ArrayList<TrackInfo>();
                for (int i = 0; i < rawTrackInfos.length(); i++) {
                    tracks.add(parseTrackInfo(rawTrackInfos.getJSONObject(i)));
                }
                result.setTracks(tracks);
            }
            if (rawAlbumInfo.has(AlbumInfo.ALBUMINFO_KEY_URL)) {
                result.setUrl(rawAlbumInfo.getString(AlbumInfo.ALBUMINFO_KEY_URL));
            }
            if (rawAlbumInfo.has(AlbumInfo.ALBUMINFO_KEY_WIKIABSTRACT)) {
                result.setWikiAbstract(
                        rawAlbumInfo.getString(AlbumInfo.ALBUMINFO_KEY_WIKIABSTRACT));
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseAlbumInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return result;
    }

    private ArtistInfo parseArtistInfo(JSONObject rawArtistInfo) {
        ArtistInfo result = new ArtistInfo();
        try {
            if (rawArtistInfo.has(ArtistInfo.ARTISTINFO_KEY_DISAMBIGUATION)) {
                result.setDisambiguation(
                        rawArtistInfo.getString(ArtistInfo.ARTISTINFO_KEY_DISAMBIGUATION));
            }
            if (rawArtistInfo.has(ArtistInfo.ARTISTINFO_KEY_ID)) {
                result.setId(rawArtistInfo.getString(ArtistInfo.ARTISTINFO_KEY_ID));
            }
            if (rawArtistInfo.has(ArtistInfo.ARTISTINFO_KEY_IMAGES)) {
                JSONArray rawImageInfos = rawArtistInfo
                        .getJSONArray(ArtistInfo.ARTISTINFO_KEY_IMAGES);
                ArrayList<ImageInfo> images = new ArrayList<ImageInfo>();
                for (int i = 0; i < rawImageInfos.length(); i++) {
                    images.add(parseImageInfo(rawImageInfos.getJSONObject(i)));
                }
                result.setImages(images);
            }
            if (rawArtistInfo.has(ArtistInfo.ARTISTINFO_KEY_MEMBERS)) {
                JSONArray rawProducerInfos = rawArtistInfo
                        .getJSONArray(ArtistInfo.ARTISTINFO_KEY_MEMBERS);
                ArrayList<PersonInfo> members = new ArrayList<PersonInfo>();
                for (int i = 0; i < rawProducerInfos.length(); i++) {
                    members.add(parsePersonInfo(rawProducerInfos.getJSONObject(i)));
                }
                result.setMembers(members);
            }
            if (rawArtistInfo.has(ArtistInfo.ARTISTINFO_KEY_NAME)) {
                result.setName(rawArtistInfo.getString(ArtistInfo.ARTISTINFO_KEY_NAME));
            }
            if (rawArtistInfo.has(ArtistInfo.ARTISTINFO_KEY_NAMES)) {
                JSONArray rawImageInfos = rawArtistInfo
                        .getJSONArray(ArtistInfo.ARTISTINFO_KEY_NAMES);
                ArrayList<String> names = new ArrayList<String>();
                for (int i = 0; i < rawImageInfos.length(); i++) {
                    names.add(rawImageInfos.getString(i));
                }
                result.setNames(names);
            }
            if (rawArtistInfo.has(ArtistInfo.ARTISTINFO_KEY_RESOURCES)) {
                JSONArray rawResourceInfos = rawArtistInfo
                        .getJSONArray(ArtistInfo.ARTISTINFO_KEY_RESOURCES);
                ArrayList<ResourceInfo> resources = new ArrayList<ResourceInfo>();
                for (int i = 0; i < rawResourceInfos.length(); i++) {
                    resources.add(parseResourceInfo(rawResourceInfos.getJSONObject(i)));
                }
                result.setResources(resources);
            }
            if (rawArtistInfo.has(ArtistInfo.ARTISTINFO_KEY_URL)) {
                result.setUrl(rawArtistInfo.getString(ArtistInfo.ARTISTINFO_KEY_URL));
            }
            if (rawArtistInfo.has(ArtistInfo.ARTISTINFO_KEY_WIKIABSTRACT)) {
                result.setWikiAbstract(
                        rawArtistInfo.getString(ArtistInfo.ARTISTINFO_KEY_WIKIABSTRACT));
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseArtistInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return result;
    }

    private ImageInfo parseImageInfo(JSONObject rawImageInfo) {
        return new ImageInfo();
    }

    private PersonInfo parsePersonInfo(JSONObject rawPersonInfo) {
        return new PersonInfo();
    }

    private ResourceInfo parseResourceInfo(JSONObject rawResourceInfo) {
        return new ResourceInfo();
    }

    private TrackChartItemInfo parseTrackChartItemInfo(JSONObject rawTrackChartItemInfo) {
        return new TrackChartItemInfo();
    }

    private TrackInfo parseTrackInfo(JSONObject rawTrackInfo) {
        return new TrackInfo();
    }

    public JSONObject getJSONResponse(HttpGet httpGet) {
        JSONObject result = null;
        HttpClient httpClient = new DefaultHttpClient();
        StringBuilder builder = new StringBuilder();
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            StatusLine statusLine = httpResponse.getStatusLine();
            if (statusLine.getStatusCode() == 200) {
                HttpEntity httpEntity = httpResponse.getEntity();
                InputStream content = httpEntity.getContent();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    builder.append(line);
                }
                result = new JSONObject(builder.toString());
            } else {
                Log.e(TAG, "getAlbumInfo: Failed to download: StatusCode='" + statusLine
                        .getStatusCode() + "', URI='" + httpGet.getURI().toString() + "'");
            }
        } catch (ClientProtocolException e) {
            Log.e(TAG, "getAlbumInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG, "getAlbumInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (JSONException e) {
            Log.e(TAG, "getAlbumInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return result;
    }

}
