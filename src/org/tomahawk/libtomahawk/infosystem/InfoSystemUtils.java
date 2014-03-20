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

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.util.ISO8601DateFormat;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.infosystem.hatchet.AlbumInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.ArtistInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.ChartItem;
import org.tomahawk.libtomahawk.infosystem.hatchet.Image;
import org.tomahawk.libtomahawk.infosystem.hatchet.PlaylistEntries;
import org.tomahawk.libtomahawk.infosystem.hatchet.PlaylistEntryInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.PlaylistInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.TrackInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.Tracks;
import org.tomahawk.libtomahawk.infosystem.hatchet.UserInfo;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InfoSystemUtils {

    public static Query trackInfoToQuery(TrackInfo trackInfo, AlbumInfo albumInfo,
            ArtistInfo artistInfo) {
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
    public static UserPlaylist fillUserPlaylistWithPlaylistEntries(UserPlaylist userPlaylist,
            PlaylistEntries playlistEntries) {
        if (userPlaylist != null && playlistEntries != null) {
            ArrayList<Query> queries = new ArrayList<Query>();
            // Convert our Lists to Maps containing the id as the key, so we can efficiently build the
            // list of Queries afterwards
            Map<String, TrackInfo> trackInfoMap = new HashMap<String, TrackInfo>();
            if (playlistEntries.tracks != null) {
                for (TrackInfo trackInfo : playlistEntries.tracks) {
                    trackInfoMap.put(trackInfo.id, trackInfo);
                }
            }
            Map<String, ArtistInfo> artistInfoMap = new HashMap<String, ArtistInfo>();
            if (playlistEntries.artists != null) {
                for (ArtistInfo artistInfo : playlistEntries.artists) {
                    artistInfoMap.put(artistInfo.id, artistInfo);
                }
            }
            Map<String, AlbumInfo> albumInfoMap = new HashMap<String, AlbumInfo>();
            if (playlistEntries.albums != null) {
                for (AlbumInfo albumInfo : playlistEntries.albums) {
                    albumInfoMap.put(albumInfo.id, albumInfo);
                }
            }
            for (PlaylistEntryInfo playlistEntryInfo : playlistEntries.playlistEntries) {
                TrackInfo trackInfo = trackInfoMap.get(playlistEntryInfo.track);
                if (trackInfo != null) {
                    ArtistInfo artistInfo = artistInfoMap.get(trackInfo.artist);
                    AlbumInfo albumInfo = albumInfoMap.get(playlistEntryInfo.album);
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
    public static UserPlaylist playlistInfoToUserPlaylist(PlaylistInfo playlistInfo) {
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
    public static Artist fillArtistWithArtistInfo(Artist artist, ArtistInfo artistInfo,
            Image image) {
        if (artist.getImage() == null && image != null && !TextUtils.isEmpty(image.squareurl)) {
            artist.setImage(org.tomahawk.libtomahawk.collection.Image.get(image.squareurl, true,
                    image.width, image.height));
        }
        return artist;
    }

    /**
     * Fill the given artist's albums with the given list of albums
     */
    public static Artist fillArtistWithAlbums(Artist artist, Map<AlbumInfo, Tracks> tracksMap,
            Map<AlbumInfo, Image> imageMap) {
        ConcurrentHashMap<String, Album> albumMap = new ConcurrentHashMap<String, Album>();
        if (tracksMap != null && imageMap != null && !artist.hasAlbumsFetchedViaHatchet()) {
            for (AlbumInfo albumInfo : tracksMap.keySet()) {
                Image image = imageMap.get(albumInfo);
                List<TrackInfo> trackInfos = tracksMap.get(albumInfo).tracks;
                Album album = albumInfoToAlbum(albumInfo, artist.getName(), trackInfos, image);
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
    public static Artist fillArtistWithTopHits(Artist artist, Map<ChartItem, TrackInfo> tracksMap) {
        if (tracksMap != null && artist.getTopHits().size() == 0) {
            ArrayList<Query> tophits = new ArrayList<Query>();
            for (ChartItem chartItem : tracksMap.keySet()) {
                TrackInfo trackInfos = tracksMap.get(chartItem);
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
    public static Artist artistInfoToArtist(ArtistInfo artistInfo, Image image) {
        Artist artist = Artist.get(artistInfo.name);
        fillArtistWithArtistInfo(artist, artistInfo, image);
        return artist;
    }

    /**
     * Fill the given album with the given albuminfo, without overriding any values that have
     * already been set
     */
    public static Album fillAlbumWithAlbumInfo(Album album, AlbumInfo albumInfo,
            Image image) {
        if (album.getImage() == null && image != null && !TextUtils.isEmpty(image.squareurl)) {
            album.setImage(org.tomahawk.libtomahawk.collection.Image.get(image.squareurl, true,
                    image.width, image.height));
        }
        return album;
    }

    /**
     * Fill the given album's tracks with the given list of trackinfos
     */
    public static Album fillAlbumWithTracks(Album album, List<TrackInfo> trackInfos) {
        ArrayList<Query> queries = new ArrayList<Query>();
        if (trackInfos != null && !album.hasQueriesFetchedViaHatchet()) {
            for (TrackInfo trackInfo : trackInfos) {
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
    public static Album albumInfoToAlbum(AlbumInfo albumInfo, String artistName,
            List<TrackInfo> trackInfos, Image image) {
        Album album = Album.get(albumInfo.name, Artist.get(artistName));
        fillAlbumWithAlbumInfo(album, albumInfo, image);
        fillAlbumWithTracks(album, trackInfos);
        return album;
    }

    /**
     * Fill the given User with the given UserInfo
     */
    public static User fillUserWithUserInfo(User user, UserInfo userInfo, Image image,
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
     * Convert the given UserInfo into a User object
     */
    public static User userInfoToUser(UserInfo userInfo, Image image, TrackInfo nowPlayingTrackInfo,
            ArtistInfo nowPlayingArtistInfo) {
        User user = User.get(userInfo.id);
        Query nowPlayingQuery = trackInfoToQuery(nowPlayingTrackInfo, null, nowPlayingArtistInfo);
        fillUserWithUserInfo(user, userInfo, image, nowPlayingQuery);
        return user;
    }

    public static ObjectMapper constructObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setDateFormat(new ISO8601DateFormat());
        return objectMapper;
    }
}
