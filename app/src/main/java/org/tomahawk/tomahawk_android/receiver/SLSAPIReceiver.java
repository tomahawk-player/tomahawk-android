/**
 * This file is part of Simple Last.fm Scrobbler.
 *
 *     http://code.google.com/p/a-simple-lastfm-scrobbler/
 *
 * Copyright 2011 Simple Last.fm Scrobbler Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tomahawk.tomahawk_android.receiver;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.tomahawk_android.services.MicroService;

import android.content.Context;
import android.os.Bundle;

/**
 * A BroadcastReceiver for the Simple Last.fm Scrobbler API. More info available at the SLS <a
 * href="http://code.google.com/p/a-simple-lastfm-scrobbler/wiki/Developers"> dev page</a>.
 *
 * @author tgwizard
 * @see AbstractPlayStatusReceiver
 * @since 1.2.3
 */
public class SLSAPIReceiver extends AbstractPlayStatusReceiver {

    private static final String TAG = SLSAPIReceiver.class.getSimpleName();

    public static final String SLS_API_BROADCAST_INTENT = "com.adam.aslfms.notify.playstatechanged";

    public static final int STATE_START = 0;

    public static final int STATE_RESUME = 1;

    public static final int STATE_PAUSE = 2;

    public static final int STATE_COMPLETE = 3;

    private int getIntFromBundle(Bundle bundle, String key, boolean throwOnFailure)
            throws IllegalArgumentException {
        long value = -1;
        Object obj = bundle.get(key);

        if (obj instanceof Long) {
            value = (Long) obj;
        } else if (obj instanceof Integer) {
            value = (Integer) obj;
        } else if (obj instanceof String) {
            value = Long.valueOf((String) obj);
        } else if (throwOnFailure) {
            throw new IllegalArgumentException(key + "not found in intent");
        }

        return (int) value;
    }

    @Override
    protected void parseIntent(Context ctx, String action, Bundle bundle)
            throws IllegalArgumentException {
        // state, required
        int state = getIntFromBundle(bundle, "state", true);

        if (state == STATE_START) {
            setState(MicroService.State.START);
        } else if (state == STATE_RESUME) {
            setState(MicroService.State.RESUME);
        } else if (state == STATE_PAUSE) {
            setState(MicroService.State.PAUSE);
        } else if (state == STATE_COMPLETE) {
            setState(MicroService.State.COMPLETE);
        } else {
            throw new IllegalArgumentException("bad state: " + state);
        }

        setTimestamp(System.currentTimeMillis());

        Artist artist = Artist.get(bundle.getString("artist"));
        Album album = null;
        if (bundle.getString("album") != null) {
            album = Album.get(bundle.getString("album"), artist);
        }
        Track track = Track.get(bundle.getString("track"), album, artist);

        // duration, required
        int duration = getIntFromBundle(bundle, "duration", true);
        track.setDuration(duration);

        // tracknr, optional
        int tracknr = getIntFromBundle(bundle, "track-number", false);
        if (tracknr != -1) {
            track.setAlbumPos(tracknr);
        }

        // music-brainz id, optional
        String mbid = bundle.getString("mbid");
        setMbid(mbid);

        // source, optional (defaults to "P")
        String source = bundle.getString("source");
        source = (source == null) ? "P" : source;
        setSource(source);

        // throws on bad data
        setTrack(track);
    }
}
