/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.services;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * This Activity is only needed because the Spotify Android SDK calls back from its LoginActivity in
 * onActivityResult. This callback is handled here and the access token is passed to SpotifyService,
 * which then sets up the Player object and stuff.
 */
public class SpotifyActivity extends Activity {

    public static final String ACCESS_TOKEN = "access_token";

    public static final String CLIENT_ID = "d3e9c989687c496ab2f92c9e84f779dc";

    private static final String REDIRECT_URI = "tomahawkspotifyresolver://callback";

    // Request code that will be used to verify if the result comes from correct activity
    private static final int REQUEST_CODE = 0xB00B1E5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Intent serviceIntent = new Intent(this, SpotifyService.class);
                serviceIntent.putExtra(ACCESS_TOKEN, response.getAccessToken());
                startService(serviceIntent);
                finish();
            }
        }
    }
}