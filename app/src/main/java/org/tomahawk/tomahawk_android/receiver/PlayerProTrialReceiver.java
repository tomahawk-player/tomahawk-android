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
package org.tomahawk.tomahawk_android.receiver;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.tomahawk_android.services.MicroService;

import android.content.Context;
import android.os.Bundle;

public class PlayerProTrialReceiver extends AbstractPlayStatusReceiver {

    static final String APP_PACKAGE = "com.tbig.playerprotrial";

    static final String APP_NAME = "Player Pro Trial";

    static final String ACTION_PLAYERPROTRIAL_METACHANGED = "com.tbig.playerprotrial.metachanged";

    static final String ACTION_PLAYERPROTRIAL_PLAYSTATECHANGED
            = "com.tbig.playerprotrial.playstatechanged";

    static final String ACTION_PLAYERPROTRIAL_PLAYBACKCOMPLETE
            = "com.tbig.playerprotrial.playbackcomplete";

    static final String TAG = PlayerProTrialReceiver.class.getSimpleName();

    @Override
    protected void parseIntent(Context ctx, String action, Bundle bundle) {

        setTimestamp(System.currentTimeMillis());

        if (ACTION_PLAYERPROTRIAL_PLAYSTATECHANGED.equals(action)) {
            if (bundle.getBoolean("playing")) {
                setState(MicroService.State.RESUME);
            } else {
                setState(MicroService.State.PAUSE);
            }
        } else if (ACTION_PLAYERPROTRIAL_METACHANGED.equals(action)) {
            if (bundle.getBoolean("playing")) {
                setState(MicroService.State.START);
            } else {
                setState(MicroService.State.PAUSE);
            }
        } else if (ACTION_PLAYERPROTRIAL_PLAYBACKCOMPLETE.equals(action)) {
            setState(MicroService.State.COMPLETE);
        }
        Artist artist = Artist.get(bundle.getString("artist"));
        Album album = null;
        if (bundle.getString("album") != null) {
            album = Album.get(bundle.getString("album"), artist);
        }
        Track track = Track.get(bundle.getString("track"), album, artist);
        track.setDuration(bundle.getInt("duration"));
        // throws on bad data
        setTrack(track);
    }
}
