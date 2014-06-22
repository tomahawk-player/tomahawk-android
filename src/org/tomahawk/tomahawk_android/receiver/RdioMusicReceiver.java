/**
 *  This file is part of Simple Last.fm Scrobbler.
 *
 *  Simple Last.fm Scrobbler is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Simple Last.fm Scrobbler is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Simple Last.fm Scrobbler.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  See http://code.google.com/p/a-simple-lastfm-scrobbler/ for the latest version.
 */

package org.tomahawk.tomahawk_android.receiver;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.tomahawk_android.services.MicroService;

import android.content.Context;
import android.os.Bundle;

/**
 * A BroadcastReceiver for intents sent by the Rdio Music Player.
 *
 * @author tgwizard
 * @see BuiltInMusicAppReceiver
 * @since 1.3.7
 */
public class RdioMusicReceiver extends AbstractPlayStatusReceiver {

    static final String TAG = RdioMusicReceiver.class.getSimpleName();

    static final String APP_PACKAGE = "com.rdio.android.ui";

    static final String APP_NAME = "Rdio";

    @Override
    protected void parseIntent(Context ctx, String action, Bundle bundle)
            throws IllegalArgumentException {

        // state, required
        boolean isPaused = bundle.getBoolean("isPaused");
        boolean isPlaying = bundle.getBoolean("isPlaying");

        if (isPlaying) {
            setState(MicroService.State.RESUME);
        } else if (isPaused) {
            setState(MicroService.State.PAUSE);
        } else {
            setState(MicroService.State.COMPLETE);
        }

        Artist artist = Artist.get(bundle.getString("artist"));
        Album album = null;
        if (bundle.getString("album") != null) {
            album = Album.get(bundle.getString("album"), artist);
        }
        Track track = Track.get(bundle.getString("track"), album, artist);

        setTimestamp(System.currentTimeMillis());

        long duration = -1;
        Object obj = bundle.get("duration");
        if (obj instanceof Integer) {
            duration = (Integer) obj;
        }
        if (obj instanceof Double) {
            duration = ((Double) obj).longValue();
        }
        if (duration != -1) {
            // duration is in milliseconds
            track.setDuration(duration);
        }

        // throws on bad data
        setTrack(track);
    }

}
