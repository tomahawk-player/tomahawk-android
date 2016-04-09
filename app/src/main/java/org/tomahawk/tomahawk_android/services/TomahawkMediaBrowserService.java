/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class TomahawkMediaBrowserService extends MediaBrowserServiceCompat {

    private static final String TAG = TomahawkMediaBrowserService.class.getSimpleName();

    public static final String MEDIA_ID_ROOT = "__ROOT__";

    public static final String MEDIA_ID_ALBUMS = "__ALBUMS__";

    MediaSessionCompat mSession;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "oncreate");
        // Start a new MediaSession
        mSession = new MediaSessionCompat(this, "TomahawkMediaBrowserService");
        setSessionToken(mSession.getSessionToken());
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
            Bundle rootHints) {
        Log.d(TAG, "onGetRoot");
        return new BrowserRoot(MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull final String parentMediaId,
            @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "OnLoadChildren.ROOT");
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId("__ALBUMS__")
                .setTitle("test")
                .build();
        mediaItems.add(new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
        result.sendResult(mediaItems);
    }
}
