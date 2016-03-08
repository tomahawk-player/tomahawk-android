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
package org.tomahawk.tomahawk_android.mediaplayers;

import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.ScriptJob;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverAccessTokenResult;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.os.Build;
import android.os.Bundle;

public class DeezerMediaPlayer extends PluginMediaPlayer {

    private static final String TAG = DeezerMediaPlayer.class.getSimpleName();

    public static final String PACKAGE_NAME = "org.tomahawk.deezerplugin";

    public static final int MIN_VERSION = 20;

    public DeezerMediaPlayer() {
        super(TomahawkApp.PLUGINNAME_DEEZER, PACKAGE_NAME);
    }

    public static String getPluginDownloadLink() {
        if (Build.CPU_ABI.equals("x86")) {
            return "http://download.tomahawk-player.org/android-plugins/"
                    + "tomahawk-android-deezer-x86-release-" + MIN_VERSION + ".apk";
        } else {
            return "http://download.tomahawk-player.org/android-plugins/"
                    + "tomahawk-android-deezer-armv7a-release-" + MIN_VERSION + ".apk";
        }
    }

    @Override
    public String getUri(Query query) {
        String strippedPath = query.getPreferredTrackResult().getPath()
                .replace("deezer://track/", "");
        String[] parts = strippedPath.split("/");
        return parts[0];
    }

    @Override
    public void prepare(final String uri) {
        getScriptResolver().getAccessToken(
                new ScriptJob.ResultsCallback<ScriptResolverAccessTokenResult>(
                        ScriptResolverAccessTokenResult.class) {
                    @Override
                    public void onReportResults(ScriptResolverAccessTokenResult results) {
                        Bundle args = new Bundle();
                        args.putString(MSG_PREPARE_ARG_URI, uri);
                        args.putString(MSG_PREPARE_ARG_ACCESSTOKEN, results.accessToken);
                        args.putLong(MSG_PREPARE_ARG_ACCESSTOKENEXPIRES,
                                results.accessTokenExpires);
                        callService(MSG_PREPARE, args);
                    }
                });
    }

    @Override
    public void setBitrate(int bitrateMode) {
        // The Deezer Android SDK doesn't support setting a bitrate
    }
}
