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
import android.util.Log;

/**
 * A BroadcastReceiver for intents sent by the LG Optimus 4X P880 music player
 *
 * @author kshahar <shahar.kosti@gmail.com>
 * @see AbstractPlayStatusReceiver
 * @since 1.4.4
 */
public class LgOptimus4xReceiver extends AbstractPlayStatusReceiver {

    static final String APP_PACKAGE = "com.lge.music";

    static final String APP_NAME = "LG Music Player";

    static final String ACTION_LGE_START = "com.lge.music.metachanged";

    static final String ACTION_LGE_PAUSERESUME = "com.lge.music.playstatechanged";

    static final String ACTION_LGE_STOP = "com.lge.music.endofplayback";

    static final String TAG = LgOptimus4xReceiver.class.getSimpleName();

    @Override
    protected void parseIntent(Context ctx, String action, Bundle bundle) {

        if (ACTION_LGE_STOP.equals(action)) {
            setState(MicroService.State.COMPLETE);
            setIsSameAsCurrentTrack();
            Log.d(TAG, "Setting state to COMPLETE");
            return;
        }

        if (ACTION_LGE_START.equals(action)) {
            setState(MicroService.State.START);
            Log.d(TAG, "Setting state to START");
        } else if (ACTION_LGE_PAUSERESUME.equals(action)) {
            boolean playing = bundle.getBoolean("playing");
            MicroService.State state = playing
                    ? (MicroService.State.RESUME)
                    : (MicroService.State.PAUSE);
            setState(state);
            Log.d(TAG, "Setting state to " + state.toString());
        }
        Artist artist = Artist.get(bundle.getString("artist"));
        Album album = null;
        if (bundle.getString("album") != null) {
            album = Album.get(bundle.getString("album"), artist);
        }
        Track track = Track.get(bundle.getString("track"), album, artist);

        setTimestamp(System.currentTimeMillis());

        // set duration
        int duration = -1;
        Object obj = bundle.get("duration");
        if (obj instanceof Long) {
            duration = ((Long) obj).intValue();
        } else if (obj instanceof Integer) {
            duration = (Integer) obj;
        }
        if (duration != -1) {
            track.setDuration(duration);
        }

        // throws on bad data
        setTrack(track);
    }
}
