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
package org.tomahawk.tomahawk_android.services;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.Query;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class MicroService extends Service {

    private static final String TAG = MicroService.class.getSimpleName();

    public static final String ACTION_PLAYSTATECHANGED
            = "org.tomahawk.tomahawk_android.playstatechanged";

    public static final String EXTRA_TRACKKEY = "org.tomahawk.tomahawk_android.track_key";

    public static final String EXTRA_STATE = "org.tomahawk.tomahawk_android.extra_state";

    public static final String EXTRA_TIMESTAMP = "org.tomahawk.tomahawk_android.extra_timestamp";

    public static final String EXTRA_SOURCE = "org.tomahawk.tomahawk_android.extra_source";

    public static final String EXTRA_MBID = "org.tomahawk.tomahawk_android.extra_mbid";

    public static final String EXTRA_IS_SAME_AS_CURRENT_TRACK
            = "org.tomahawk.tomahawk_android.is_same_as_current_track";

    public enum State {
        START, RESUME, PAUSE, COMPLETE, PLAYLIST_FINISHED, UNKNOWN_NONPLAYING
    }

    private Track mCurrentTrack = null;

    @Override
    public void onCreate() {
        super.onCreate();

        DatabaseHelper.getInstance().ensureInit();
        InfoSystem.getInstance().ensureInit();
        AuthenticatorManager.getInstance().ensureInit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (i != null) {
            String action = i.getAction();
            Bundle extras = i.getExtras();
            if (ACTION_PLAYSTATECHANGED.equals(action)) {
                if (extras != null) {
                    MicroService.State state = MicroService.State
                            .valueOf(extras.getString(EXTRA_STATE));

                    Track track = Track.getTrackByKey(extras.getString(EXTRA_TRACKKEY));
                    boolean isSameAsCurrentTrack = extras
                            .containsKey(EXTRA_IS_SAME_AS_CURRENT_TRACK);
                    String source = extras.getString(EXTRA_SOURCE);
                    if (track != null || isSameAsCurrentTrack) {
                        onPlayStateChanged(track, state, isSameAsCurrentTrack, source);
                    }
                }
            }
        }
        return Service.START_NOT_STICKY;
    }

    private synchronized void onPlayStateChanged(Track track, MicroService.State state,
            boolean isSameAsCurrentTrack, String source) {
        if (isSameAsCurrentTrack) {
            // this only happens for apps implementing Scrobble Droid's API
            Log.d(TAG, "Got a SAME_AS_CURRENT track");
            if (mCurrentTrack != null) {
                track = mCurrentTrack;
            } else {
                Log.e(TAG, "Got a SAME_AS_CURRENT track, but current was null!");
                return;
            }
        }
        Log.d(TAG, "Track data: '" + track.getName() + "' - '" + track.getArtist().getName()
                + "' - '" + track.getAlbum().getName() + "' ,source: " + source + ",state: "
                + state);
        if (mCurrentTrack != track) {
            mCurrentTrack = track;
            AuthenticatorUtils utils = AuthenticatorManager.getInstance()
                    .getAuthenticatorUtils(AuthenticatorManager.AUTHENTICATOR_ID_HATCHET);
            InfoSystem.getInstance().sendNowPlayingPostStruct(utils, Query.get(track, false));
        }
    }
}
