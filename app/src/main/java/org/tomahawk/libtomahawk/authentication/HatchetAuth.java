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
package org.tomahawk.libtomahawk.authentication;

import org.tomahawk.libtomahawk.authentication.models.HatchetAuthResponse;

import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.Path;

public interface HatchetAuth {

    @FormUrlEncoded
    @POST("/authentication/password")
    HatchetAuthResponse login(
            @Field("username") String username,
            @Field("password") String password,
            @Field("grant_type") String grant_type
    );

    @FormUrlEncoded
    @POST("/tokens/refresh/bearer")
    HatchetAuthResponse getBearerAccessToken(
            @Field("refresh_token") String refresh_token,
            @Field("grant_type") String grant_type
    );

    @GET("/tokens/fetch/{tokentype}")
    HatchetAuthResponse getAccessToken(
            @Header("Authorization") String bearerAccessToken,
            @Path("tokentype") String tokentype
    );

    @FormUrlEncoded
    @POST("/registration/direct")
    HatchetAuthResponse registerDirectly(
            @Field("username") String username,
            @Field("password") String password,
            @Field("email") String email
    );

}