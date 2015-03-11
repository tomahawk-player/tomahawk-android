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

import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.tomahawk_android.services.MicroService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

/**
 * Base class for play status receivers.
 *
 * @author tgwizard
 * @author mrmaffen
 */
public abstract class AbstractPlayStatusReceiver extends BroadcastReceiver {

    private static final String TAG = AbstractPlayStatusReceiver.class.getSimpleName();

    private Intent mServiceIntent = null;

    private Track mTrack;

    @Override
    public final void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            String action = intent.getAction();
            Bundle bundle = intent.getExtras();

            Log.v(TAG, "Action received was: " + action);

            // check to make sure we actually got something
            if (action == null) {
                Log.w(TAG, "Got null action");
                return;
            }

            if (bundle == null) {
                bundle = Bundle.EMPTY;
            }

            mServiceIntent = new Intent(MicroService.ACTION_PLAYSTATECHANGED);

            try {
                parseIntent(context, action, bundle); // might throw

                // parseIntent must have called setTrack with non-null values
                if (mTrack == null) {
                    throw new IllegalArgumentException(
                            "Track was null, not starting/calling MicroService");
                } else {
                    mServiceIntent.putExtra(MicroService.EXTRA_TRACKKEY, mTrack.getCacheKey());
                    // start/call the Scrobbling Service
                    context.startService(mServiceIntent);
                }
            } catch (IllegalArgumentException e) {
                Log.i(TAG, "onReceive: Got a bad track, ignoring it (" + e.getMessage() + ")");
            }
        }
    }

    /**
     * Sets a boolean indicating, that the received track is the same as the current one.
     */
    protected final void setIsSameAsCurrentTrack() {
        mServiceIntent.putExtra(MicroService.EXTRA_IS_SAME_AS_CURRENT_TRACK, true);
    }

    /**
     * Sets the music brains id of the track in the received broadcast.
     */
    protected final void setMbid(String mbid) {
        mServiceIntent.putExtra(MicroService.EXTRA_MBID, mbid);
    }

    /**
     * Sets the source that the broadcast has been received from.
     */
    protected final void setSource(String source) {
        mServiceIntent.putExtra(MicroService.EXTRA_SOURCE, source);
    }

    /**
     * Sets the timestamp that the broadcast has been received at.
     */
    protected final void setTimestamp(long timestamp) {
        mServiceIntent.putExtra(MicroService.EXTRA_TIMESTAMP, timestamp);
    }

    /**
     * Sets the {@link org.tomahawk.tomahawk_android.services.MicroService.State} that the received
     * broadcast represents.
     */
    protected final void setState(MicroService.State state) {
        mServiceIntent.putExtra(MicroService.EXTRA_STATE, state.name());
    }

    /**
     * Sets the {@link Track} for this scrobble request
     *
     * @param track the Track for this scrobble request
     */
    protected final void setTrack(Track track) {
        mTrack = track;
    }

    /**
     * Parses the API / music app specific parts of the received broadcast.
     *
     * @param ctx    to be able to create {@code MusicAPIs}
     * @param action the action/intent used for this scrobble request
     * @param bundle the data sent with this request
     * @throws IllegalArgumentException when the data received is invalid
     * @see #setTrack(Track)
     */
    protected abstract void parseIntent(Context ctx, String action,
            Bundle bundle) throws IllegalArgumentException;

}
