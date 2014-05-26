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
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
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

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        DatabaseHelper.getInstance().ensureInit();
        InfoSystem.getInstance().ensureInit();
        AuthenticatorManager.getInstance().ensureInit();
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

                    Track track = Track.getTrackByKey(extras.getString(EXTRA_TRACKKEY));
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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                TomahawkApp.getContext());
        boolean scrobbleEverything = preferences.getBoolean(
                FakePreferenceFragment.FAKEPREFERENCEFRAGMENT_KEY_SCROBBLEEVERYTHING, false);
        if (scrobbleEverything && !TextUtils.isEmpty(trackName) && (!TextUtils.isEmpty(artistName)
                || !TextUtils.isEmpty(albumArtistName))) {
            Artist artist;
            if (!TextUtils.isEmpty(artistName)) {
                artist = Artist.get(artistName);
            } else {
                artist = Artist.get(albumArtistName);
            }
            Album album = null;
            if (!TextUtils.isEmpty(albumName)) {
                album = Album.get(albumName, artist);
            }
            Track track = Track.get(trackName, album, artist);
            if (sCurrentTrack != track) {
                sCurrentTrack = track;
                AuthenticatorUtils utils = AuthenticatorManager.getInstance()
                        .getAuthenticatorUtils(
                                AuthenticatorManager.AUTHENTICATOR_ID_HATCHET);
                InfoSystem.getInstance()
                        .sendNowPlayingPostStruct(utils, Query.get(track, false));
                Log.d(TAG, "Scrobbling track: '" + track.getName() + "' - '"
                        + track.getArtist().getName() + "' - '"
                        + track.getAlbum().getName());
            }
        }
    }
}
