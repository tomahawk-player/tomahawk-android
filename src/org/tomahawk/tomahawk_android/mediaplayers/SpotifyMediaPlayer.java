/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.mediaplayers;

import org.tomahawk.aidl.IPluginService;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.ScriptJob;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverAccessTokenResult;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This class wraps all functionality to be able to directly playback spotify-resolved tracks with
 * OpenSLES .
 */
public class SpotifyMediaPlayer extends PluginMediaPlayer {

    private static final String TAG = SpotifyMediaPlayer.class.getSimpleName();

    // String tags used to store Spotify's preferred bitrate
    private static final String SPOTIFY_PREF_BITRATE
            = "org.tomahawk.tomahawk_android.spotify_pref_bitrate";

    public static final int SPOTIFY_PREF_BITRATE_MODE_LOW = 0;

    public static final int SPOTIFY_PREF_BITRATE_MODE_MEDIUM = 1;

    public static final int SPOTIFY_PREF_BITRATE_MODE_HIGH = 2;

    private static class Holder {

        private static final SpotifyMediaPlayer instance = new SpotifyMediaPlayer();

    }

    private SpotifyMediaPlayer() {
        super(TomahawkApp.PLUGINNAME_SPOTIFY, "org.tomahawk.spotifyplugin", 10);
    }

    public static SpotifyMediaPlayer get() {
        return Holder.instance;
    }

    @Override
    public ServiceCall getPrepareServiceCall(Application application, final Query query) {
        return new ServiceCall() {
            @Override
            public void call(final IPluginService pluginService) {
                getScriptResolver().getAccessToken(
                        new ScriptJob.ResultsCallback<ScriptResolverAccessTokenResult>(
                                ScriptResolverAccessTokenResult.class) {
                            @Override
                            public void onReportResults(ScriptResolverAccessTokenResult results) {
                                String[] pathParts =
                                        query.getPreferredTrackResult().getPath().split("/");
                                String uri = "spotify:track:" + pathParts[pathParts.length - 1];
                                try {
                                    pluginService.prepare(uri, results.accessToken, null, -1);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "prepare: " + e.getClass() + ": "
                                            + e.getLocalizedMessage());
                                }
                            }
                        });
            }
        };
    }

    public void setBitRate(final int bitrateMode) {
        callService(new ServiceCall() {
            @Override
            public void call(IPluginService pluginService) {
                try {
                    pluginService.setBitRate(bitrateMode);
                } catch (RemoteException e) {
                    Log.e(TAG, "setBitRate: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
            }
        });
    }

    public void updateBitrate() {
        ConnectivityManager conMan = (ConnectivityManager) TomahawkApp.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Log.d(TAG, "Updating bitrate to HIGH, because we have a Wifi connection");
            setBitRate(SpotifyMediaPlayer.SPOTIFY_PREF_BITRATE_MODE_HIGH);
        } else {
            Log.d(TAG, "Updating bitrate to user setting, because we don't have a Wifi connection");
            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(TomahawkApp.getContext());
            int prefbitrate = preferences.getInt(
                    SpotifyMediaPlayer.SPOTIFY_PREF_BITRATE,
                    SpotifyMediaPlayer.SPOTIFY_PREF_BITRATE_MODE_MEDIUM);
            setBitRate(prefbitrate);
        }
    }
}
