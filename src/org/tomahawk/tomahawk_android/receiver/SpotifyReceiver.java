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

public class SpotifyReceiver extends AbstractPlayStatusReceiver {

    static final String APP_PACKAGE = "com.spotify.mobile.android";

    static final String APP_NAME = "Spotify Android";

    static final String ACTION_SPOTIFY_METADATACHANGED
            = "com.spotify.mobile.android.metadatachanged";

    static final String ACTION_SPOTIFY_PLAYBACKSTATECHANGED
            = "com.spotify.mobile.android.playbackstatechanged";

    static final String TAG = SpotifyReceiver.class.getSimpleName();

    @Override
    protected void parseIntent(Context ctx, String action, Bundle bundle) {

        if (ACTION_SPOTIFY_PLAYBACKSTATECHANGED.equals(action)) {
            if (bundle.getBoolean("playing")) {
                setState(MicroService.State.RESUME);
                setIsSameAsCurrentTrack();
            } else {
                setState(MicroService.State.PAUSE);
                setIsSameAsCurrentTrack();
            }
        } else if (ACTION_SPOTIFY_METADATACHANGED.equals(action)) {
            setState(MicroService.State.START);
            Artist artist = Artist.get(bundle.getString("artist"));
            Album album = null;
            if (bundle.getString("album") != null) {
                album = Album.get(bundle.getString("album"), artist);
            }
            Track track = Track.get(bundle.getString("track"), album, artist);

            setTimestamp(System.currentTimeMillis());

            // throws on bad data
            setTrack(track);
        }
    }
}
