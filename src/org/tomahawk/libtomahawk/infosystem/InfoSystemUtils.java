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

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.HatchetCollection;
import org.tomahawk.libtomahawk.collection.ListItemString;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetAlbumInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetArtistInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetChartItem;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetImage;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaybackItemResponse;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaybackLogsResponse;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistEntries;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistEntryInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetSocialAction;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetTrackInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetUserInfo;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.Mappable;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfoSystemUtils {

    private final static String TAG = InfoSystemUtils.class.getSimpleName();

    /**
     * Convert the given playlist entry data, add it to a Playlist object and return that.
     *
     * @param playlist        the Playlist to fill with entries(queries)
     * @param playlistEntries Object containing info about each entry of the playlist
     * @return the filled Playlist object
     */
    public static Playlist fillPlaylist(Playlist playlist, HatchetPlaylistEntries playlistEntries) {
        if (playlist != null && playlistEntries != null
                && playlistEntries.playlistEntries != null) {
            ArrayList<PlaylistEntry> entries = new ArrayList<>();
            Map<String, HatchetTrackInfo> tracksMap = listToMap(playlistEntries.tracks);
            Map<String, HatchetArtistInfo> artistsMap = listToMap(playlistEntries.artists);
            Map<String, HatchetAlbumInfo> albumsMap = listToMap(playlistEntries.albums);
            for (HatchetPlaylistEntryInfo entryInfo : playlistEntries.playlistEntries) {
                HatchetTrackInfo trackInfo = tracksMap.get(entryInfo.track);
                if (trackInfo != null) {
                    HatchetArtistInfo artistInfo = artistsMap.get(trackInfo.artist);
                    String albumName = null;
                    if (playlistEntries.albums != null && entryInfo.album != null) {
                        albumName = albumsMap.get(entryInfo.album).name;
                    }
                    String artistName = null;
                    if (artistInfo != null) {
                        artistName = artistInfo.name;
                    }
                    Query query = Query.get(trackInfo.name, albumName, artistName, false);
                    entries.add(PlaylistEntry.get(playlist.getId(), query, entryInfo.id));
                }
            }
            playlist.setEntries(entries);
            playlist.setFilled(true);
        } else {
            Log.e(TAG, "fillPlaylist - Unable to parse.");
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
            Playlist playlist = Playlist.fromQueryList(playlistInfo.title,
                    playlistInfo.currentrevision, new ArrayList<Query>());
            playlist.setHatchetId(playlistInfo.id);
            return playlist;
        }
        return null;
    }

    /**
     * Fill the given artist with the given artistinfo, without overriding any values that have
     * already been set
     */
    public static Artist fillArtist(Artist artist, HatchetImage image, String wikiabstract) {
        if (artist.getImage() == null && image != null && !TextUtils.isEmpty(image.squareurl)) {
            artist.setImage(org.tomahawk.libtomahawk.collection.Image.get(image.squareurl, true,
                    image.width, image.height));
        }
        if (wikiabstract != null) {
            artist.setBio(new ListItemString(wikiabstract));
        }
        return artist;
    }

    /**
     * Fill the given artist with the given list of top-hit tracks
     */
    public static Artist fillArtist(Artist artist, List<HatchetChartItem> chartItems,
            Map<String, HatchetTrackInfo> tracks) {
        HatchetCollection hatchetCollection = (HatchetCollection) CollectionManager
                .getInstance().getCollection(TomahawkApp.PLUGINNAME_HATCHET);
        if (tracks != null) {
            ArrayList<Query> tophits = new ArrayList<>();
            for (HatchetChartItem chartItem : chartItems) {
                HatchetTrackInfo trackInfos = tracks.get(chartItem.track);
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
        fillArtist(artist, image, artistInfo.wikiabstract);
        return artist;
    }

    /**
     * Fill the given album with the given albuminfo, without overriding any values that have
     * already been set
     */
    public static Album fillAlbum(Album album, HatchetImage image) {
        if (album.getImage() == null && image != null && !TextUtils.isEmpty(image.squareurl)) {
            album.setImage(org.tomahawk.libtomahawk.collection.Image.get(image.squareurl, true,
                    image.width, image.height));
        }
        return album;
    }

    /**
     * Convert the given albuminfo to an album
     */
    public static Album convertToAlbum(HatchetAlbumInfo albumInfo, String artistName,
            HatchetImage image) {
        Album album = Album.get(albumInfo.name, Artist.get(artistName));
        fillAlbum(album, image);
        return album;
    }

    /**
     * Convert the given list of trackinfos to a list of queries
     */
    public static List<Query> convertToQueries(List<HatchetTrackInfo> tracks, String albumName,
            Map<String, HatchetArtistInfo> artists) {
        List<Query> queries = new ArrayList<>();
        if (tracks != null) {
            for (HatchetTrackInfo trackInfo : tracks) {
                Query query = Query.get(trackInfo.name, albumName,
                        artists.get(trackInfo.artist).name, false, true);
                queries.add(query);
            }
        }
        return queries;
    }

    /**
     * Convert the given HatchetUserInfo into a User object
     */
    public static User convertToUser(HatchetUserInfo userInfo, HatchetTrackInfo track,
            HatchetArtistInfo artist, HatchetImage image) {
        User user = User.get(userInfo.id);
        user.setName(userInfo.name);
        if (track != null && artist != null) {
            user.setNowPlaying(Query.get(track.name, null, artist.name, false));
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
        user.setNowPlayingTimeStamp(userInfo.nowplayingtimestamp);
        if (user.getImage() == null && image != null && !TextUtils.isEmpty(image.squareurl)) {
            user.setImage(org.tomahawk.libtomahawk.collection.Image.get(image.squareurl, true,
                    image.width, image.height));
        }
        return user;
    }

    /**
     * Convert the given HatchetSocialAction into a SocialAction object
     */
    public static SocialAction convertToSocialAction(HatchetSocialAction hatchetSocialAction,
            HatchetTrackInfo track, HatchetArtistInfo artist, HatchetAlbumInfo album,
            HatchetUserInfo user, HatchetUserInfo target, HatchetPlaylistInfo playlist) {
        SocialAction socialAction = SocialAction.get(hatchetSocialAction.id);
        socialAction.setAction(hatchetSocialAction.action);
        socialAction.setDate(hatchetSocialAction.date);
        socialAction.setTimeStamp(hatchetSocialAction.timestamp);
        socialAction.setType(hatchetSocialAction.type);
        if (hatchetSocialAction.track != null) {
            socialAction.setQuery(Query.get(track.name, null, artist.name, false));
        }
        if (hatchetSocialAction.artist != null) {
            socialAction.setArtist(convertToArtist(artist, null));
        }
        if (hatchetSocialAction.album != null) {
            socialAction.setAlbum(convertToAlbum(album, artist.name, null));
        }
        if (hatchetSocialAction.user != null) {
            socialAction.setUser(convertToUser(user, null, null, null));
        }
        if (hatchetSocialAction.target != null) {
            socialAction.setTarget(convertToUser(target, null, null, null));
        }
        if (hatchetSocialAction.playlist != null) {
            socialAction.setPlaylist(convertToPlaylist(playlist));
        }
        return socialAction;
    }

    /**
     * Convert the given HatchetPlaybackLogResponse into a List of Queries
     */
    public static ArrayList<Query> convertToQueryList(HatchetPlaybackLogsResponse playbackLogs) {
        ArrayList<Query> queries = new ArrayList<>();
        if (playbackLogs.playbackLog != null && playbackLogs.playbackLogEntries != null) {
            Map<String, HatchetTrackInfo> tracksMap = listToMap(playbackLogs.tracks);
            Map<String, HatchetArtistInfo> artistsMap = listToMap(playbackLogs.artists);
            Map<String, HatchetPlaybackItemResponse> entriesMap =
                    listToMap(playbackLogs.playbackLogEntries);
            for (String playbackItemId : playbackLogs.playbackLog.playbackLogEntries) {
                HatchetPlaybackItemResponse playbackitem = entriesMap.get(playbackItemId);
                HatchetTrackInfo trackInfo = tracksMap.get(playbackitem.track);
                HatchetArtistInfo artistInfo = artistsMap.get(trackInfo.artist);
                Query q = Query.get(trackInfo.name, null, artistInfo.name, false, true);
                queries.add(q);
            }
        } else {
            Log.e(TAG, "convertToQueryList - Unable to parse.");
        }
        return queries;
    }

    public static <T extends Mappable> Map<String, T> listToMap(List<T> list) {
        if (list != null) {
            Map<String, T> map = new HashMap<>();
            for (T item : list) {
                map.put(item.id, item);
            }
            return map;
        }
        return null;
    }

    public static <T> T carelessGet(List<T> list, int position) {
        if (list == null || position >= list.size()) {
            return null;
        } else {
            return list.get(position);
        }
    }

    public static <T extends Mappable> T carelessGet(List<T> list, String id) {
        if (list != null && id != null) {
            for (T item : list) {
                if (item.id.equals(id)) {
                    return item;
                }
            }
        }
        return null;
    }

    public static <T> T carelessGet(Map<String, T> map, String key) {
        if (map == null || key == null) {
            return null;
        } else {
            return map.get(key);
        }
    }
}
