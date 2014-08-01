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

import java.util.List;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedInput;

public interface Hatchet {

    @GET("/users")
    HatchetUsers users(
            @Query("ids[]") List<String> ids,
            @Query("name") String name,
            @Query("random") String random,
            @Query("count") String count
    );

    @GET("/users/{user-id}/playlists")
    HatchetPlaylists usersPlaylists(
            @Path("user-id") String user_id
    );

    @GET("/users/{user-id}/lovedItems")
    HatchetPlaylistEntries usersLovedItems(
            @Path("user-id") String user_id
    );

    @GET("/users/{user-id}/socialActions")
    HatchetSocialActionResponse usersSocialActions(
            @Path("user-id") String user_id,
            @Query("offset") String offset,
            @Query("limit") String limit
    );

    @GET("/users/{user-id}/friendsFeed")
    HatchetSocialActionResponse usersFriendsFeed(
            @Path("user-id") String user_id,
            @Query("offset") String offset,
            @Query("limit") String limit
    );

    @GET("/users/{user-id}/playbackLog")
    HatchetPlaybackLogsResponse usersPlaybackLog(
            @Path("user-id") String user_id
    );

    @GET("/playlists/{playlist-id}/entries")
    HatchetPlaylistEntries playlistsEntries(
            @Path("playlist-id") String playlist_id
    );

    @GET("/artists")
    HatchetArtists artists(
            @Query("ids[]") List<String> ids,
            @Query("name") String name
    );

    @GET("/artists/{artist-id}/albums")
    HatchetCharts artistsAlbums(
            @Path("artist-id") String artist_id
    );

    @GET("/artists/{artist-id}/topHits")
    HatchetCharts artistsTopHits(
            @Path("artist-id") String artist_id
    );

    @GET("/tracks")
    HatchetTracks tracks(
            @Query("ids[]") List<String> ids,
            @Query("name") String name,
            @Query("artist_name") String artist_name
    );

    @GET("/albums")
    HatchetAlbums albums(
            @Query("ids[]") List<String> ids,
            @Query("name") String name,
            @Query("artist_name") String artist_name
    );

    @GET("/searches")
    HatchetSearch searches(
            @Query("term") String term
    );

    @POST("/playbackLogEntries")
    Response playbackLogEntries(
            @Header("Authorization") String accesstoken,
            @Body TypedInput rawBody
    );

    @POST("/playbackLogEntries/nowplaying")
    Response playbackLogEntriesNowPlaying(
            @Header("Authorization") String accesstoken,
            @Body TypedInput rawBody
    );

    @POST("/socialActions")
    Response socialActions(
            @Header("Authorization") String accesstoken,
            @Body TypedInput rawBody
    );

    @GET("/relationships")
    HatchetRelationshipsStruct relationships(
            @Query("ids[]") List<String> ids,
            @Query("user_id") String user_id,
            @Query("target_type") String target_type,
            @Query("target_user_id") String target_user_id,
            @Query("target_artist_id") String target_artist_id,
            @Query("target_album_id") String target_album_id,
            @Query("filter") String filter,
            @Query("type") String type
    );

}