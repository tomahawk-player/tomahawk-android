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
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetAlbumInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetArtistInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetChartItem;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetImage;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetPlaylistEntries;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetPlaylistEntryInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetPlaylistInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetTrackInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetTracks;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetUserInfo;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InfoSystemUtils {

    public static Query trackInfoToQuery(HatchetTrackInfo trackInfo, HatchetAlbumInfo albumInfo,
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
     * Convert the given playlist entry data, add it to a UserPlaylist object and return that.
     *
     * @param userPlaylist    the UserPlaylist to fill with entries(queries)
     * @param playlistEntries Object containing info about each entry of the playlist
     * @return the filled UserPlaylist object
     */
    public static UserPlaylist fillUserPlaylist(UserPlaylist userPlaylist,
            HatchetPlaylistEntries playlistEntries) {
        if (userPlaylist != null && playlistEntries != null) {
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
                    queries.add(trackInfoToQuery(trackInfo, albumInfo, artistInfo));
                }
            }
            userPlaylist.setQueries(queries);
        }
        return userPlaylist;
    }

    /**
     * Convert the given data into a UserPlaylist object and return that.
     *
     * @param playlistInfo Object containing basic playlist info like title etc...
     * @return the converted UserPlaylist object
     */
    public static UserPlaylist convertToUserPlaylist(HatchetPlaylistInfo playlistInfo) {
        if (playlistInfo != null) {
            return UserPlaylist.fromQueryList(playlistInfo.id, playlistInfo.title,
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
    public static Artist fillArtist(Artist artist, Map<HatchetAlbumInfo, HatchetTracks> tracksMap,
            Map<HatchetAlbumInfo, HatchetImage> imageMap) {
        ConcurrentHashMap<String, Album> albumMap = new ConcurrentHashMap<String, Album>();
        if (tracksMap != null && imageMap != null && !artist.hasAlbumsFetchedViaHatchet()) {
            for (HatchetAlbumInfo albumInfo : tracksMap.keySet()) {
                HatchetImage image = imageMap.get(albumInfo);
                List<HatchetTrackInfo> trackInfos = tracksMap.get(albumInfo).tracks;
                Album album = convertToAlbum(albumInfo, artist.getName(), trackInfos, image);
                String key = TomahawkUtils.getCacheKey(album);
                albumMap.put(key, album);
                artist.addAlbum(album);
            }
        }
        artist.setAlbumsFetchedViaHatchet(albumMap);
        return artist;
    }

    /**
     * Fill the given artist with the given list of top-hit tracks
     */
    public static Artist fillArtist(Artist artist,
            Map<HatchetChartItem, HatchetTrackInfo> tracksMap) {
        if (tracksMap != null && artist.getTopHits().size() == 0) {
            ArrayList<Query> tophits = new ArrayList<Query>();
            for (HatchetChartItem chartItem : tracksMap.keySet()) {
                HatchetTrackInfo trackInfos = tracksMap.get(chartItem);
                Query query = Query.get(trackInfos.name, "", artist.getName(), false, true);
                tophits.add(query);
            }
            artist.setTopHits(tophits);
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
        ArrayList<Query> queries = new ArrayList<Query>();
        if (trackInfos != null && !album.hasQueriesFetchedViaHatchet()) {
            for (HatchetTrackInfo trackInfo : trackInfos) {
                Query query = Query.get(trackInfo.name, album.getName(),
                        album.getArtist().getName(), false, true);
                album.addQuery(query);
                queries.add(query);
            }
            album.setQueriesFetchedViaHatchet(queries);
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
    public static User fillUser(User user, HatchetUserInfo userInfo, HatchetImage image,
            Query nowPlaying) {
        user.setAbout(userInfo.about);
        user.setFollowCount(userInfo.followCount);
        user.setFollowersCount(userInfo.followersCount);
        user.setName(userInfo.name);
        user.setNowPlaying(nowPlaying);
        user.setNowPlayingTimeStamp(userInfo.nowplayingtimestamp);
        user.setTotalPlays(userInfo.totalPlays);
        if (user.getImage() == null && image != null && !TextUtils.isEmpty(image.squareurl)) {
            user.setImage(org.tomahawk.libtomahawk.collection.Image.get(image.squareurl, true,
                    image.width, image.height));
        }
        return user;
    }

    /**
     * Convert the given HatchetUserInfo into a User object
     */
    public static User userInfoToUser(HatchetUserInfo userInfo, HatchetImage image,
            HatchetTrackInfo nowPlayingTrackInfo,
            HatchetArtistInfo nowPlayingArtistInfo) {
        User user = User.get(userInfo.id);
        Query nowPlayingQuery = trackInfoToQuery(nowPlayingTrackInfo, null, nowPlayingArtistInfo);
        fillUser(user, userInfo, image, nowPlayingQuery);
        return user;
    }

    public static ObjectMapper constructObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setDateFormat(new org.tomahawk.libtomahawk.utils.ISO8601DateFormat());
        return objectMapper;
    }
}
