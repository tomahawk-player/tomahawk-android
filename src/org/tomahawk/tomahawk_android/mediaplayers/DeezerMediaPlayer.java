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

import org.tomahawk.aidl.IPluginService;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.ScriptJob;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverAccessTokenResult;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.app.Application;
import android.os.RemoteException;
import android.util.Log;

public class DeezerMediaPlayer extends PluginMediaPlayer implements TomahawkMediaPlayer {

    private static final String TAG = DeezerMediaPlayer.class.getSimpleName();

    private static class Holder {

        private static final DeezerMediaPlayer instance = new DeezerMediaPlayer();

    }

    private DeezerMediaPlayer() {
        super(TomahawkApp.PLUGINNAME_DEEZER, "org.tomahawk.deezerplugin");
    }

    public static DeezerMediaPlayer get() {
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
                                String strippedPath = query.getPreferredTrackResult().getPath()
                                        .replace("deezer://track/", "");
                                String[] parts = strippedPath.split("/");
                                try {
                                    pluginService.prepare(parts[0], results.accessToken, null,
                                            results.accessTokenExpires);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "prepare: " + e.getClass() + ": "
                                            + e.getLocalizedMessage());
                                }
                            }
                        });
            }
        };
    }
}
