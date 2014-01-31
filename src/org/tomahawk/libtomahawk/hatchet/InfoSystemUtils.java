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
package org.tomahawk.libtomahawk.hatchet;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InfoSystemUtils {

    public static UserPlaylist playlistInfoToUserPlaylist(PlaylistInfo playlistInfo,
            PlaylistEntries playlistEntries) {
        if (playlistInfo != null && playlistEntries != null) {
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
                String trackName;
                String artistName = "";
                String albumName = "";
                TrackInfo trackInfo = trackInfoMap.get(playlistEntryInfo.track);
                if (trackInfo != null) {
                    trackName = trackInfo.name;
                    ArtistInfo artistInfo = artistInfoMap.get(trackInfo.artist);
                    if (artistInfo != null) {
                        artistName = artistInfo.name;
                    }
                    AlbumInfo albumInfo = albumInfoMap.get(playlistEntryInfo.album);
                    if (albumInfo != null) {
                        albumName = albumInfo.name;
                    }
                    queries.add(new Query(trackName, albumName, artistName, false));
                }
            }
            return UserPlaylist.fromQueryList(playlistInfo.id, playlistInfo.title,
                    playlistInfo.currentrevision, queries);
        }
        return null;
    }

    public static Artist fillArtistWithArtistInfo(Artist artist, ArtistInfo artistInfo,
            Image image) {
        if (artist.getImage() == null && image != null) {
            artist.setImage(image.squareurl);
        }
        return artist;
    }

    public static Artist artistInfoToArtist(ArtistInfo artistInfo, Image image) {
        Artist artist = Artist.get(artistInfo.name);
        if (artist.getImage() == null && image != null) {
            artist.setImage(image.squareurl);
        }
        return artist;
    }

    public static Album fillAlbumWithAlbumInfo(Album album, AlbumInfo albumInfo,
            Image image) {
        if (album.getAlbumArtPath() == null && image != null) {
            album.setAlbumArtPath(image.squareurl);
        }
        return album;
    }

    public static Album fillAlbumWithTracks(Album album, List<TrackInfo> trackInfos) {
        if (trackInfos != null) {
            for (TrackInfo trackInfo : trackInfos) {
                album.addQuery(
                        new Query(trackInfo.name, album.getName(), album.getArtist().getName(),
                                false));
            }
        }
        return album;
    }

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

    public static Artist fillArtistWithTopHits(Artist artist, Map<ChartItem, TrackInfo> tracksMap) {
        if (tracksMap != null && artist.getTopHits().size() == 0) {
            ArrayList<Query> tophits = new ArrayList<Query>();
            for (ChartItem chartItem : tracksMap.keySet()) {
                TrackInfo trackInfos = tracksMap.get(chartItem);
                Query query = new Query(trackInfos.name, "", artist.getName(), false);
                tophits.add(query);
            }
            artist.setTopHits(tophits);
        }
        return artist;
    }

    public static Album albumInfoToAlbum(AlbumInfo albumInfo, String artistName,
            List<TrackInfo> trackInfos, Image image) {
        Album album = Album.get(albumInfo.name, Artist.get(artistName));
        ArrayList<Query> queries = new ArrayList<Query>();
        if (album.getAlbumArtPath() == null && image != null) {
            album.setAlbumArtPath(image.squareurl);
        }
        if (trackInfos != null && !album.hasQueriesFetchedViaHatchet()) {
            for (TrackInfo trackInfo : trackInfos) {
                Query query = new Query(trackInfo.name, album.getName(), artistName, false);
                queries.add(query);
                album.addQuery(query);
            }
        }
        album.setQueriesFetchedViaHatchet(queries);
        return album;
    }
}
