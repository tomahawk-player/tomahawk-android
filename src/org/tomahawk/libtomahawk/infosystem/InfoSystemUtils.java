/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.HatchetCollection;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetAlbumInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetArtistInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetChartItem;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetImage;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetPlaybackItemResponse;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetPlaybackLogResponse;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetPlaylistEntries;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetPlaylistEntryInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetPlaylistInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetSocialAction;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetTrackInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetTracks;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetUserInfo;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfoSystemUtils {

    public static Query convertToQuery(HatchetTrackInfo trackInfo, HatchetAlbumInfo albumInfo,
            HatchetArtistInfo artistInfo) {
        String trackName;
        String artistName = "";
        String albumName = "";
        if (trackInfo != null) {
            trackName = trackInfo.name;
            if (artistInfo != null) {
                artistName = artistInfo.name;
            }
            if (albumInfo != null) {
                albumName = albumInfo.name;
            }
            return Query.get(trackName, albumName, artistName, false, true);
        }
        return null;
    }

    /**
     * Convert the given playlist entry data, add it to a Playlist object and return that.
     *
     * @param playlist        the Playlist to fill with entries(queries)
     * @param playlistEntries Object containing info about each entry of the playlist
     * @return the filled Playlist object
     */
    public static Playlist fillPlaylist(Playlist playlist,
            HatchetPlaylistEntries playlistEntries) {
        if (playlist != null && playlistEntries != null) {
            ArrayList<Query> queries = new ArrayList<Query>();
            // Convert our Lists to Maps containing the id as the key, so we can efficiently build the
            // list of Queries afterwards
            Map<String, HatchetTrackInfo> trackInfoMap = new HashMap<String, HatchetTrackInfo>();
            if (playlistEntries.tracks != null) {
                for (HatchetTrackInfo trackInfo : playlistEntries.tracks) {
                    trackInfoMap.put(trackInfo.id, trackInfo);
                }
            }
            Map<String, HatchetArtistInfo> artistInfoMap = new HashMap<String, HatchetArtistInfo>();
            if (playlistEntries.artists != null) {
                for (HatchetArtistInfo artistInfo : playlistEntries.artists) {
                    artistInfoMap.put(artistInfo.id, artistInfo);
                }
            }
            Map<String, HatchetAlbumInfo> albumInfoMap = new HashMap<String, HatchetAlbumInfo>();
            if (playlistEntries.albums != null) {
                for (HatchetAlbumInfo albumInfo : playlistEntries.albums) {
                    albumInfoMap.put(albumInfo.id, albumInfo);
                }
            }
            for (HatchetPlaylistEntryInfo playlistEntryInfo : playlistEntries.playlistEntries) {
                HatchetTrackInfo trackInfo = trackInfoMap.get(playlistEntryInfo.track);
                if (trackInfo != null) {
                    HatchetArtistInfo artistInfo = artistInfoMap.get(trackInfo.artist);
                    HatchetAlbumInfo albumInfo = albumInfoMap.get(playlistEntryInfo.album);
                    queries.add(convertToQuery(trackInfo, albumInfo, artistInfo));
                }
            }
            playlist.setQueries(queries);
        }
        return playlist;
    }

    /**
     * Convert the given data into a Playlist object and return that.
     *
     * @param playlistInfo Object containing basic playlist info like title etc...
     * @return the converted Playlist object
     */
    public static Playlist convertToPlaylist(HatchetPlaylistInfo playlistInfo) {
        if (playlistInfo != null) {
            return Playlist.fromQueryList(playlistInfo.id, playlistInfo.title,
                    playlistInfo.currentrevision, new ArrayList<Query>());
        }
        return null;
    }

    /**
     * Fill the given artist with the given artistinfo, without overriding any values that have
     * already been set
     */
    public static Artist fillArtist(Artist artist, HatchetArtistInfo artistInfo,
            HatchetImage image) {
        if (artist.getImage() == null && image != null && !TextUtils.isEmpty(image.squareurl)) {
            artist.setImage(org.tomahawk.libtomahawk.collection.Image.get(image.squareurl, true,
                    image.width, image.height));
        }
        return artist;
    }

    /**
     * Fill the given artist's albums with the given list of albums
     */
    public static List<Album> convertToAlbumList(Artist artist,
            Map<HatchetAlbumInfo, HatchetTracks> tracksMap,
            Map<HatchetAlbumInfo, HatchetImage> imageMap) {
        ArrayList<Album> albums = new ArrayList<Album>();
        if (tracksMap != null && imageMap != null) {
            for (HatchetAlbumInfo albumInfo : tracksMap.keySet()) {
                HatchetImage image = imageMap.get(albumInfo);
                List<HatchetTrackInfo> trackInfos = tracksMap.get(albumInfo).tracks;
                Album album = convertToAlbum(albumInfo, artist.getName(), trackInfos,
                        image);
                albums.add(album);
            }
        }
        return albums;
    }

    /**
     * Fill the given artist with the given list of top-hit tracks
     */
    public static Artist fillArtist(Artist artist,
            Map<HatchetChartItem, HatchetTrackInfo> trackInfoMap) {
        HatchetCollection hatchetCollection = (HatchetCollection) CollectionManager
                .getInstance().getCollection(TomahawkApp.PLUGINNAME_HATCHET);
        if (trackInfoMap != null) {
            ArrayList<Query> tophits = new ArrayList<Query>();
            for (HatchetChartItem chartItem : trackInfoMap.keySet()) {
                HatchetTrackInfo trackInfos = trackInfoMap.get(chartItem);
                Query query = Query
                        .get(trackInfos.name, "", artist.getName(), false, true);
                tophits.add(query);
            }
            hatchetCollection.addArtistTopHits(artist, tophits);
        }
        return artist;
    }

    /**
     * Convert the given artistInfo into an Artist object
     */
    public static Artist convertToArtist(HatchetArtistInfo artistInfo, HatchetImage image) {
        Artist artist = Artist.get(artistInfo.name);
        fillArtist(artist, artistInfo, image);
        return artist;
    }

    /**
     * Fill the given album with the given albuminfo, without overriding any values that have
     * already been set
     */
    public static Album fillAlbum(Album album, HatchetAlbumInfo albumInfo, HatchetImage image) {
        if (album.getImage() == null && image != null && !TextUtils.isEmpty(image.squareurl)) {
            album.setImage(org.tomahawk.libtomahawk.collection.Image.get(image.squareurl, true,
                    image.width, image.height));
        }
        return album;
    }

    /**
     * Fill the given album's tracks with the given list of trackinfos
     */
    public static Album fillAlbum(Album album, List<HatchetTrackInfo> trackInfos) {
        if (trackInfos != null) {
            ArrayList<Query> queries = new ArrayList<Query>();
            for (HatchetTrackInfo trackInfo : trackInfos) {
                Query query = Query.get(trackInfo.name, album.getName(),
                        album.getArtist().getName(), false, true);
                album.addQuery(query);
                queries.add(query);
            }
        }
        return album;
    }

    /**
     * Convert the given albuminfo to an album
     */
    public static Album convertToAlbum(HatchetAlbumInfo albumInfo, String artistName,
            List<HatchetTrackInfo> trackInfos, HatchetImage image) {
        Album album = Album.get(albumInfo.name, Artist.get(artistName));
        fillAlbum(album, albumInfo, image);
        fillAlbum(album, trackInfos);
        return album;
    }

    /**
     * Fill the given User with the given HatchetUserInfo
     */
    public static User fillUser(User user, HatchetUserInfo userInfo,
            Map<String, HatchetTrackInfo> trackInfoMap,
            Map<String, HatchetArtistInfo> artistInfoMap,
            Map<String, HatchetImage> imageMap) {
        if (userInfo != null) {
            HatchetImage image = null;
            if (imageMap != null && userInfo.images != null && userInfo.images.size() > 0) {
                image = imageMap.get(userInfo.images.get(0));
            }
            if (userInfo.nowplaying != null) {
                HatchetTrackInfo trackInfo = trackInfoMap.get(userInfo.nowplaying);
                if (trackInfo != null) {
                    HatchetArtistInfo artistInfo = artistInfoMap.get(trackInfo.artist);
                    Query nowPlayingQuery = convertToQuery(trackInfo, null, artistInfo);
                    user.setNowPlaying(nowPlayingQuery);
                }
            }
            if (userInfo.about != null) {
                user.setAbout(userInfo.about);
            }
            if (userInfo.followCount >= 0) {
                user.setFollowCount(userInfo.followCount);
            }
            if (userInfo.followersCount >= 0) {
                user.setFollowersCount(userInfo.followersCount);
            }
            if (userInfo.totalPlays >= 0) {
                user.setTotalPlays(userInfo.totalPlays);
            }
            user.setName(userInfo.name);
            user.setNowPlayingTimeStamp(userInfo.nowplayingtimestamp);
            if (user.getImage() == null && image != null && !TextUtils.isEmpty(image.squareurl)) {
                user.setImage(org.tomahawk.libtomahawk.collection.Image.get(image.squareurl, true,
                        image.width, image.height));
            }
        }
        return user;
    }

    /**
     * Convert the given HatchetUserInfo into a User object
     */
    public static User convertToUser(HatchetUserInfo userInfo,
            Map<String, HatchetTrackInfo> trackInfoMap,
            Map<String, HatchetArtistInfo> artistInfoMap, Map<String, HatchetImage> imageMap) {
        User user = User.get(userInfo.id);
        fillUser(user, userInfo, trackInfoMap, artistInfoMap, imageMap);
        return user;
    }

    /**
     * Fill the given SocialAction with the given HatchetSocialAction
     */
    public static SocialAction fillSocialAction(SocialAction socialAction,
            HatchetSocialAction hatchetSocialAction, Map<String, HatchetTrackInfo> trackInfoMap,
            Map<String, HatchetArtistInfo> artistInfoMap,
            Map<String, HatchetAlbumInfo> albumInfoMap,
            Map<String, HatchetUserInfo> userInfoMap,
            Map<String, HatchetPlaylistInfo> playlistInfoMap) {
        if (hatchetSocialAction != null) {
            socialAction.setAction(hatchetSocialAction.action);
            socialAction.setDate(hatchetSocialAction.date);
            socialAction.setTimeStamp(hatchetSocialAction.timestamp);
            socialAction.setType(hatchetSocialAction.type);
            if (hatchetSocialAction.track != null) {
                HatchetTrackInfo trackInfo = trackInfoMap.get(hatchetSocialAction.track);
                HatchetArtistInfo artistInfo = artistInfoMap.get(trackInfo.artist);
                socialAction.setQuery(convertToQuery(trackInfo, null, artistInfo));
            }
            if (hatchetSocialAction.artist != null) {
                HatchetArtistInfo artistInfo = artistInfoMap.get(hatchetSocialAction.artist);
                socialAction.setArtist(convertToArtist(artistInfo, null));
            }
            if (hatchetSocialAction.album != null) {
                HatchetAlbumInfo albumInfo = albumInfoMap.get(hatchetSocialAction.album);
                socialAction.setAlbum(convertToAlbum(albumInfo,
                        artistInfoMap.get(albumInfo.artist).name, null, null));
            }
            if (hatchetSocialAction.user != null) {
                HatchetUserInfo userInfo = userInfoMap.get(hatchetSocialAction.user);
                socialAction.setUser(convertToUser(userInfo, trackInfoMap, artistInfoMap, null));
            }
            if (hatchetSocialAction.target != null) {
                HatchetUserInfo userInfo = userInfoMap.get(hatchetSocialAction.target);
                socialAction.setTarget(convertToUser(userInfo, trackInfoMap, artistInfoMap, null));
            }
            if (hatchetSocialAction.playlist != null) {
                HatchetPlaylistInfo playlistInfo = playlistInfoMap
                        .get(hatchetSocialAction.playlist);
                socialAction.setPlaylist(convertToPlaylist(playlistInfo));
            }
        }
        return socialAction;
    }

    /**
     * Convert the given HatchetSocialAction into a SocialAction object
     */
    public static SocialAction convertToSocialAction(HatchetSocialAction hatchetSocialAction,
            Map<String, HatchetTrackInfo> trackInfoMap,
            Map<String, HatchetArtistInfo> artistInfoMap,
            Map<String, HatchetAlbumInfo> albumInfoMap,
            Map<String, HatchetUserInfo> userInfoMap,
            Map<String, HatchetPlaylistInfo> playlistInfoMap) {
        SocialAction socialAction = SocialAction.get(hatchetSocialAction.id);
        fillSocialAction(socialAction, hatchetSocialAction, trackInfoMap, artistInfoMap,
                albumInfoMap, userInfoMap, playlistInfoMap);
        return socialAction;
    }

    /**
     * Convert the given HatchetPlaybackLogResponse into a List of Queries
     */
    public static ArrayList<Query> convertToQueryList(HatchetPlaybackLogResponse playbackLog,
            Map<String, HatchetPlaybackItemResponse> playbackitemMap,
            Map<String, HatchetTrackInfo> trackInfoMap,
            Map<String, HatchetArtistInfo> artistInfoMap) {
        ArrayList<Query> queries = new ArrayList<Query>();
        for (String playbackItemId : playbackLog.playbackLogEntries) {
            HatchetPlaybackItemResponse playbackitem = playbackitemMap.get(playbackItemId);
            HatchetTrackInfo trackInfo = trackInfoMap.get(playbackitem.track);
            HatchetArtistInfo artistInfo = artistInfoMap.get(trackInfo.artist);
            Query q = Query.get(trackInfo.name, "", artistInfo.name, false, true);
            queries.add(q);
        }
        return queries;
    }

    public static ObjectMapper constructObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setDateFormat(new org.tomahawk.libtomahawk.utils.ISO8601DateFormat());
        return objectMapper;
    }
}
