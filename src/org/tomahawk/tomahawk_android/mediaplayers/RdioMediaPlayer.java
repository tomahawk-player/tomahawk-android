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
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

/**
 * This class wraps a standard {@link android.media.MediaPlayer} object.
 */
public class RdioMediaPlayer extends PluginMediaPlayer implements TomahawkMediaPlayer {

    private static final String TAG = RdioMediaPlayer.class.getSimpleName();

    private static class Holder {

        private static final RdioMediaPlayer instance = new RdioMediaPlayer();

    }

    private RdioMediaPlayer() {
        super(TomahawkApp.PLUGINNAME_RDIO, "org.tomahawk.rdioplugin");
    }

    public static RdioMediaPlayer getInstance() {
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
                            public void onReportResults(
                                    ScriptResolverAccessTokenResult accessTokenResult) {
                                String uri = query.getPreferredTrackResult().getPath()
                                        .replace("rdio://track/", "");
                                try {
                                    pluginService.prepare(uri, accessTokenResult.accessToken,
                                            accessTokenResult.accessTokenSecret, -1);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "prepare: " + e.getClass() + ": "
                                            + e.getLocalizedMessage());
                                }
                            }
                        });

            }
        };
    }

    @Override
    public void seekTo(int msec) {
        // Override seekTo since we can't seek in RdioMediaPlayer
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // Notify user
                Toast.makeText(TomahawkApp.getContext(),
                        TomahawkApp.getContext().getString(R.string.seeking_not_supported),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
