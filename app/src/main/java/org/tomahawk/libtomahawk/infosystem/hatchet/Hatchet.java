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

import com.google.gson.JsonObject;

import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistEntries;

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
    JsonObject getUsers(
            @Query("ids[]") List<String> ids,
            @Query("name") String name,
            @Query("random") String random,
            @Query("count") String count
    );

    @GET("/playlists")
    JsonObject getPlaylists(
            @Query("ids[]") List<String> ids
    );

    @GET("/playlists/{id}")
    JsonObject getPlaylists(
            @Path("id") String id
    );

    @GET("/artists")
    JsonObject getArtists(
            @Query("ids[]") List<String> ids,
            @Query("name") String name
    );

    @GET("/tracks")
    JsonObject getTracks(
            @Query("ids[]") List<String> ids,
            @Query("name") String name,
            @Query("artist_name") String artist_name
    );

    @GET("/albums")
    JsonObject getAlbums(
            @Query("ids[]") List<String> ids,
            @Query("name") String name,
            @Query("artist_name") String artist_name
    );

    @GET("/searches")
    JsonObject getSearches(
            @Query("term") String term
    );

    @GET("/relationships")
    JsonObject getRelationships(
            @Query("ids[]") List<String> ids,
            @Query("user_id") String user_id,
            @Query("target_type") String target_type,
            @Query("target_user_id") String target_user_id,
            @Query("target_artist_id") String target_artist_id,
            @Query("target_album_id") String target_album_id,
            @Query("filter") String filter,
            @Query("type") String type
    );

    @GET("/socialActions")
    JsonObject getSocialActions(
            @Query("ids[]") List<String> ids,
            @Query("user_id") String user_id,
            @Query("type") String type,
            @Query("before_date") String before_date,
            @Query("limit") String limit
    );

    @GET("/images")
    JsonObject getImages(
            @Query("ids[]") List<String> ids
    );

    @POST("/playbacklogEntries")
    Response postPlaybackLogEntries(
            @Header("Authorization") String accesstoken,
            @Body TypedInput rawBody
    );

    @POST("/playlists")
    HatchetPlaylistEntries postPlaylists(
            @Header("Authorization") String accesstoken,
            @Body TypedInput rawBody
    );

    @POST("/playlistEntries")
    HatchetPlaylistEntries postPlaylistsPlaylistEntries(
            @Header("Authorization") String accesstoken,
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

    @DELETE("/playlistEntries/{entry-id}")
    Response deletePlaylistsPlaylistEntries(
            @Header("Authorization") String accesstoken,
            @Path("entry-id") String entry_id,
            @Query("playlist_id") String playlist_id
    );

    @DELETE("/relationships/{relationship-id}")
    Response deleteRelationShip(
            @Header("Authorization") String accesstoken,
            @Path("relationship-id") String relationship_id
    );

}