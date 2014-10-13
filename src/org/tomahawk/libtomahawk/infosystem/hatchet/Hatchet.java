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
package org.tomahawk.libtomahawk.infosystem.hatchet;

import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetAlbums;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetArtists;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetCharts;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaybackLogsResponse;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistEntries;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetRelationshipsStruct;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetSearch;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetSocialActionResponse;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetTracks;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetUsers;

import java.util.List;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedInput;

public interface Hatchet {

    @GET("/users")
    HatchetUsers getUsers(
            @Query("ids[]") List<String> ids,
            @Query("name") String name,
            @Query("random") String random,
            @Query("count") String count
    );

    @GET("/users/{user-id}/playlists")
    HatchetPlaylistEntries getUsersPlaylists(
            @Path("user-id") String user_id
    );

    @GET("/users/{user-id}/lovedItems")
    HatchetPlaylistEntries getUsersLovedItems(
            @Path("user-id") String user_id
    );

    @GET("/users/{user-id}/socialActions")
    HatchetSocialActionResponse getUsersSocialActions(
            @Path("user-id") String user_id,
            @Query("offset") String offset,
            @Query("limit") String limit
    );

    @GET("/users/{user-id}/friendsFeed")
    HatchetSocialActionResponse getUsersFriendsFeed(
            @Path("user-id") String user_id,
            @Query("offset") String offset,
            @Query("limit") String limit
    );

    @GET("/users/{user-id}/playbackLog")
    HatchetPlaybackLogsResponse getUsersPlaybackLog(
            @Path("user-id") String user_id
    );

    @GET("/playlists/{playlist-id}/entries")
    HatchetPlaylistEntries getPlaylistsEntries(
            @Path("playlist-id") String playlist_id
    );

    @GET("/playlists/{playlist-id}")
    HatchetPlaylistEntries getPlaylists(
            @Path("playlist-id") String playlist_id
    );

    @GET("/artists")
    HatchetArtists getArtists(
            @Query("ids[]") List<String> ids,
            @Query("name") String name
    );

    @GET("/artists/{artist-id}/albums")
    HatchetCharts getArtistsAlbums(
            @Path("artist-id") String artist_id
    );

    @GET("/artists/{artist-id}/topHits")
    HatchetCharts getArtistsTopHits(
            @Path("artist-id") String artist_id
    );

    @GET("/tracks")
    HatchetTracks getTracks(
            @Query("ids[]") List<String> ids,
            @Query("name") String name,
            @Query("artist_name") String artist_name
    );

    @GET("/albums")
    HatchetAlbums getAlbums(
            @Query("ids[]") List<String> ids,
            @Query("name") String name,
            @Query("artist_name") String artist_name
    );

    @GET("/searches")
    HatchetSearch getSearches(
            @Query("term") String term
    );

    @GET("/relationships")
    HatchetRelationshipsStruct getRelationships(
            @Query("ids[]") List<String> ids,
            @Query("user_id") String user_id,
            @Query("target_type") String target_type,
            @Query("target_user_id") String target_user_id,
            @Query("target_artist_id") String target_artist_id,
            @Query("target_album_id") String target_album_id,
            @Query("filter") String filter,
            @Query("type") String type
    );

    @POST("/playbackLogEntries")
    Response postPlaybackLogEntries(
            @Header("Authorization") String accesstoken,
            @Body TypedInput rawBody
    );

    @POST("/playbackLogEntries/nowplaying")
    Response postPlaybackLogEntriesNowPlaying(
            @Header("Authorization") String accesstoken,
            @Body TypedInput rawBody
    );

    @POST("/socialActions")
    Response postSocialActions(
            @Header("Authorization") String accesstoken,
            @Body TypedInput rawBody
    );

    @POST("/playlists")
    HatchetPlaylistEntries postPlaylists(
            @Header("Authorization") String accesstoken,
            @Body TypedInput rawBody
    );

    @POST("/playlists/{playlist-id}/playlistEntries")
    HatchetPlaylistEntries postPlaylistsPlaylistEntries(
            @Header("Authorization") String accesstoken,
            @Path("playlist-id") String playlist_id,
            @Body TypedInput rawBody
    );

    @POST("/relationships")
    HatchetPlaylistEntries postRelationship(
            @Header("Authorization") String accesstoken,
            @Body TypedInput rawBody
    );

    @PUT("/playlists/{playlist-id}")
    Response putPlaylists(
            @Header("Authorization") String accesstoken,
            @Path("playlist-id") String playlist_id,
            @Body TypedInput rawBody
    );

    @DELETE("/playlists/{playlist-id}")
    Response deletePlaylists(
            @Header("Authorization") String accesstoken,
            @Path("playlist-id") String playlist_id
    );

    @DELETE("/playlists/{playlist-id}/playlistEntries/{entry-id}")
    Response deletePlaylistsPlaylistEntries(
            @Header("Authorization") String accesstoken,
            @Path("playlist-id") String playlist_id,
            @Path("entry-id") String entry_id
    );

    @DELETE("/relationships/{relationship-id}")
    Response deleteRelationShip(
            @Header("Authorization") String accesstoken,
            @Path("relationship-id") String relationship_id
    );

}