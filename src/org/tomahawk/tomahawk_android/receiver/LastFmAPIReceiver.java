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
 * A BroadcastReceiver for the Last.fm Android API. More info at <a href="https://github.com/c99koder/lastfm-android/wiki/scrobbler-interface"
 * >their dev page</a>
 *
 * @author tgwizard
 * @see AbstractPlayStatusReceiver
 * @since 1.3.2
 */
public class LastFmAPIReceiver extends AbstractPlayStatusReceiver {

    public static final String ACTION_LASTFMAPI_START = "fm.last.android.metachanged";

    public static final String ACTION_LASTFMAPI_PAUSERESUME = "fm.last.android.playbackpaused";

    public static final String ACTION_LASTFMAPI_STOP = "fm.last.android.playbackcomplete";

    @Override
    protected void parseIntent(Context ctx, String action, Bundle bundle)
            throws IllegalArgumentException {

        switch (action) {
            case ACTION_LASTFMAPI_START:
                setState(MicroService.State.START);
                setTimestamp(System.currentTimeMillis());

                Artist artist = Artist.get(bundle.getString("artist"));
                Album album = null;
                if (bundle.getString("album") != null) {
                    album = Album.get(bundle.getString("album"), artist);
                }
                Track track = Track.get(bundle.getString("track"), album, artist);
                track.setDuration(bundle.getInt("duration"));
                // throws on bad data
                setTrack(track);
                break;
            case ACTION_LASTFMAPI_PAUSERESUME:
                if (bundle.containsKey("position")) {
                    setState(MicroService.State.RESUME);
                } else {
                    setState(MicroService.State.PAUSE);
                }
                setIsSameAsCurrentTrack();
                break;
            case ACTION_LASTFMAPI_STOP:
                setState(MicroService.State.COMPLETE);
                setIsSameAsCurrentTrack();
                break;
        }
    }

}
