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
 * A BroadcastReceiver for the <a href="http://code.google.com/p/scrobbledroid/wiki/DeveloperAPI">
 * Scrobbler Droid API</a>. New music apps are recommended to use the <a
 * href="http://code.google.com/p/a-simple-lastfm-scrobbler/wiki/Developers"> SLS API</a> instead.
 *
 * @author tgwizard
 * @see AbstractPlayStatusReceiver
 * @since 1.2
 */
public class ScrobbleDroidMusicReceiver extends AbstractPlayStatusReceiver {

    private static final String TAG = ScrobbleDroidMusicReceiver.class.getSimpleName();

    public static final String SCROBBLE_DROID_MUSIC_STATUS
            = "net.jjc1138.android.scrobbler.action.MUSIC_STATUS";

    @Override
    protected void parseIntent(Context ctx, String action, Bundle bundle)
            throws IllegalArgumentException {

        boolean playing = bundle.getBoolean("playing", false);

        if (!playing) {
            // if not playing, there is no guarantee the bundle will contain any track info
            setState(MicroService.State.UNKNOWN_NONPLAYING);
            setIsSameAsCurrentTrack();
            return;
        }
        Artist artist = Artist.get(bundle.getString("artist"));
        Album album = null;
        if (bundle.getString("album") != null) {
            album = Album.get(bundle.getString("album"), artist);
        }
        Track track = Track.get(bundle.getString("track"), album, artist);

        String source = bundle.getString("source");
        if (source == null || source.length() > 1) {
            source = "P";
        }
        setSource(source);

        setTimestamp(System.currentTimeMillis());

        String mbid = bundle.getString("mb-trackid"); // optional
        setMbid(mbid);

        int duration = bundle.getInt("secs", -1); // optional unless source
        // is P, but we don't care
        if (duration != -1) {
            track.setDuration(duration * 1000);
        }

        int tnr = bundle.getInt("tracknumber", -1); // optional
        if (tnr != -1) {
            track.setAlbumPos(tnr);
        }

        // we've handled stopping/pausing at the top
        setState(MicroService.State.RESUME);

        setTrack(track);
    }
}
