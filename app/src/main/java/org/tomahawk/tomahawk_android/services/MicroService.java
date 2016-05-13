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

import org.electricwisdom.unifiedremotemetadataprovider.media.RemoteMetadataProvider;
import org.electricwisdom.unifiedremotemetadataprovider.media.enums.PlayState;
import org.electricwisdom.unifiedremotemetadataprovider.media.enums.RemoteControlFeature;
import org.electricwisdom.unifiedremotemetadataprovider.media.listeners.OnArtworkChangeListener;
import org.electricwisdom.unifiedremotemetadataprovider.media.listeners.OnMetadataChangeListener;
import org.electricwisdom.unifiedremotemetadataprovider.media.listeners.OnPlaybackStateChangeListener;
import org.electricwisdom.unifiedremotemetadataprovider.media.listeners.OnRemoteControlFeaturesChangeListener;
import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.PreferenceUtils;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

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

    private static Track sCurrentTrack = null;

    private RemoteMetadataProvider mMetadataProvider;

    private MicroServiceBroadcastReceiver mMicroServiceBroadcastReceiver;

    /**
     * Handles incoming broadcasts
     */
    private class MicroServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction()) && intent
                    .hasExtra("state") && intent.getIntExtra("state", 0) == 1) {
                Log.d(TAG, "Headset has been plugged in");

                if (PreferenceUtils.getBoolean(PreferenceUtils.PLUG_IN_TO_PLAY)) {
                    //resume playback, if user has set the "resume on headset plugin" preference
                    context.startService(new Intent(PlaybackService.ACTION_PLAY, null, context,
                            PlaybackService.class));
                }
            }
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                //Acquiring instance of RemoteMetadataProvider
                mMetadataProvider = RemoteMetadataProvider.getInstance(this);
                //setting up metadata listener
                mMetadataProvider.setOnMetadataChangeListener(new OnMetadataChangeListener() {
                    @Override
                    public void onMetadataChanged(String artistName, String trackName,
                            String albumName,
                            String albumArtistName, long duration) {
                        Log.d(TAG, "onMetadataChanged");
                        scrobbleTrack(trackName, artistName, albumName, albumArtistName);
                    }
                });

                //setting up artwork listener
                mMetadataProvider.setOnArtworkChangeListener(new OnArtworkChangeListener() {
                    @Override
                    public void onArtworkChanged(Bitmap artwork) {
                        Log.d(TAG, "onArtworkChanged");
                    }
                });

                //setting up remote control flags listener
                mMetadataProvider.setOnRemoteControlFeaturesChangeListener(
                        new OnRemoteControlFeaturesChangeListener() {
                            @Override
                            public void onFeaturesChanged(List<RemoteControlFeature> usesFeatures) {
                                Log.d(TAG, "onFeaturesChanged");
                            }
                        }
                );

                //setting up playback state change listener
                mMetadataProvider
                        .setOnPlaybackStateChangeListener(new OnPlaybackStateChangeListener() {
                            @Override
                            public void onPlaybackStateChanged(PlayState playbackState) {
                                Log.d(TAG, "onPlaybackStateChanged");
                            }
                        });
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mMetadataProvider.acquireRemoteControls();
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                mMetadataProvider.acquireRemoteControls(256, 256);
            }
        }

        mMicroServiceBroadcastReceiver = new MicroServiceBroadcastReceiver();
        registerReceiver(mMicroServiceBroadcastReceiver,
                new IntentFilter(Intent.ACTION_HEADSET_PLUG));
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

                    Track track = Track.getByKey(extras.getString(EXTRA_TRACKKEY));
                    boolean isSameAsCurrentTrack = extras
                            .containsKey(EXTRA_IS_SAME_AS_CURRENT_TRACK);
                    String source = extras.getString(EXTRA_SOURCE);
                    if (track != null || isSameAsCurrentTrack) {
                        onPlayStateChanged(track, state, isSameAsCurrentTrack, source);
                    }
                }
            }
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mMicroServiceBroadcastReceiver);
        mMicroServiceBroadcastReceiver = null;

        Log.d(TAG, "MicroService has been destroyed");
    }

    private synchronized void onPlayStateChanged(Track track, MicroService.State state,
            boolean isSameAsCurrentTrack, String source) {
        if (isSameAsCurrentTrack) {
            // this only happens for apps implementing Scrobble Droid's API
            Log.d(TAG, "Got a SAME_AS_CURRENT track");
            if (sCurrentTrack != null) {
                track = sCurrentTrack;
            } else {
                Log.e(TAG, "Got a SAME_AS_CURRENT track, but current was null!");
                return;
            }
        }
        if (State.RESUME.equals(state) || State.START.equals(state)) {
            scrobbleTrack(track.getName(), track.getArtist().getName(), track.getAlbum().getName(),
                    null);
        }
    }

    public static void scrobbleTrack(String trackName, String artistName, String albumName,
            String albumArtistName) {
        boolean scrobbleEverything = PreferenceUtils.getBoolean(PreferenceUtils.SCROBBLE_EVERYTHING)
                || Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if (scrobbleEverything && !TextUtils.isEmpty(trackName) && (!TextUtils.isEmpty(artistName)
                || !TextUtils.isEmpty(albumArtistName) || !TextUtils.isEmpty(albumName))) {
            Artist artist;
            Album album;
            if (!TextUtils.isEmpty(artistName)) {
                artist = Artist.get(artistName);
                album = Album.get(albumName, artist);
            } else if (!TextUtils.isEmpty(albumArtistName)) {
                artist = Artist.get(albumArtistName);
                album = Album.get(albumName, artist);
            } else {
                // Since the artistName is empty and the albumName isn't, we just assume that
                // something got switched up. So we use the albumName as the artistName instead.
                artist = Artist.get(albumName);
                album = Album.get(null, artist);
            }
            Track track = Track.get(trackName, album, artist);
            if (sCurrentTrack != track) {
                sCurrentTrack = track;
                AuthenticatorUtils utils = AuthenticatorManager.get()
                        .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                InfoSystem.get().sendNowPlayingPostStruct(utils, Query.get(track, false));
                Log.d(TAG, "Scrobbling " + track);
            }
        } else {
            Log.d(TAG, "Didn't scrobble track: '" + trackName + "' - '"
                    + artistName + "' - '" + albumName + "' - '" + albumArtistName);
        }
    }
}
