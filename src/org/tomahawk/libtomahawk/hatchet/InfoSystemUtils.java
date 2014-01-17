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

import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.resolver.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InfoSystemUtils {

    public static UserPlaylist playlistInfoToUserPlaylist(PlaylistInfo playlistInfo,
            PlaylistEntries playlistEntries) {
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

    public static Artist fillArtistWithArtistInfo(Artist artist, ArtistInfo artistInfo,
            Image image) {
        if (artist.getImage() == null && image != null) {
            artist.setImage(image.squareurl);
        }
        return artist;
    }
}
