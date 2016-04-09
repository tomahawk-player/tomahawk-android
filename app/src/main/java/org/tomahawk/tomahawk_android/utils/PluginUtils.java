/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.utils;

import com.google.android.gms.common.GooglePlayServicesUtil;

import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.mediaplayers.DeezerMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.SpotifyMediaPlayer;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class PluginUtils {

    public static boolean isPluginInstalled(String pluginName) {
        String pluginPackageName = "";
        switch (pluginName) {
            case TomahawkApp.PLUGINNAME_SPOTIFY:
                pluginPackageName = SpotifyMediaPlayer.PACKAGE_NAME;
                break;
            case TomahawkApp.PLUGINNAME_DEEZER:
                pluginPackageName = DeezerMediaPlayer.PACKAGE_NAME;
                break;
        }
        try {
            TomahawkApp.getContext().getPackageManager()
                    .getPackageInfo(pluginPackageName, PackageManager.GET_SERVICES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return false;
    }

    public static boolean isPluginUpToDate(String pluginName) {
        String pluginPackageName = "";
        int pluginMinVersionCode = 0;
        switch (pluginName) {
            case TomahawkApp.PLUGINNAME_SPOTIFY:
                pluginPackageName = SpotifyMediaPlayer.PACKAGE_NAME;
                pluginMinVersionCode = SpotifyMediaPlayer.MIN_VERSION;
                break;
            case TomahawkApp.PLUGINNAME_DEEZER:
                pluginPackageName = DeezerMediaPlayer.PACKAGE_NAME;
                pluginMinVersionCode = DeezerMediaPlayer.MIN_VERSION;
                break;
        }
        try {
            PackageInfo info = TomahawkApp.getContext().getPackageManager()
                    .getPackageInfo(pluginPackageName, PackageManager.GET_SERVICES);
            // Remove the first digit that identifies the architecture type
            String versionCodeString = String.valueOf(info.versionCode);
            versionCodeString = versionCodeString.substring(1, versionCodeString.length());
            int versionCode = Integer.valueOf(versionCodeString);
            if (versionCode >= pluginMinVersionCode) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return false;
    }

    public static boolean isPlayStoreInstalled() {
        try {
            TomahawkApp.getContext().getPackageManager()
                    .getPackageInfo(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
