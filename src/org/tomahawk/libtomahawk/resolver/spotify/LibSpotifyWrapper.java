/*
 Copyright (c) 2012, Spotify AB
 All rights reserved.
 
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of Spotify AB nor the names of its contributors may 
 be used to endorse or promote products derived from this software 
 without specific prior written permission.
 
 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL SPOTIFY AB BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Interface to the JNI. All communication goes through this class.
 */

package org.tomahawk.libtomahawk.resolver.spotify;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.tomahawk_android.services.TomahawkService;
import org.tomahawk.tomahawk_android.utils.TomahawkMediaPlayer;

import android.os.Handler;
import android.util.Log;

public class LibSpotifyWrapper {

    private final static String TAG = LibSpotifyWrapper.class.getName();

    private static TomahawkService.OnLoginListener sOnLoginListener;

    private static TomahawkService.OnCredBlobUpdatedListener sOnCredBlobUpdatedListener;

    private static SpotifyResolver sSpotifyResolver;

    private static TomahawkMediaPlayer sTomahawkMediaPlayer;

    private static int mCurrentPosition;

    private static boolean mInitialized;

    native public static void nativeinit(ClassLoader loader, String storagePath);

    native public static void nativedestroy();

    native private static void nativelogin(String username, String password, String blob);

    native private static void nativerelogin();

    native private static void nativelogout();

    native private static void nativeresolve(String qid, String query);

    native public static void nativeprepare(String uri);

    native public static void nativeplay();

    native public static void nativepause();

    native public static void nativeseek(int position);

    native public static void nativestar();

    native public static void nativeunstar();

    public static void init(ClassLoader loader, String storagePath) {
        nativeinit(loader, storagePath);
        mInitialized = true;
    }

    public static void destroy() {
        if (mInitialized) {
            nativedestroy();
            mInitialized = false;
        }
    }

    public static void login(String username, String password, String blob) {
        if (mInitialized) {
            nativelogin(username, password, blob);
        }
    }

    public static void relogin() {
        if (mInitialized) {
            nativerelogin();
        }
    }

    public static void logout() {
        if (mInitialized) {
            nativelogout();
        }
    }

    public static void resolve(String qid, String query) {
        if (mInitialized) {
            nativeresolve(qid, query);
        }
    }

    public static void prepare(String uri) {
        if (mInitialized) {
            nativeprepare(uri);
        }
    }


    public static void play() {
        if (mInitialized) {
            nativeplay();
        }
    }

    public static void pause() {
        if (mInitialized) {
            nativepause();
        }
    }

    public static void seek(int position) {
        if (mInitialized) {
            nativeseek(position);
        }
    }

    public static void star() {
        if (mInitialized) {
            nativestar();
        }
    }

    public static void unstar() {
        if (mInitialized) {
            nativeunstar();
        }
    }

    public static void loginUser(String username, String password, String blob,
            TomahawkService.OnLoginListener onLoginListener,
            TomahawkService.OnCredBlobUpdatedListener onCredBlobUpdatedListener) {
        sOnLoginListener = onLoginListener;
        sOnCredBlobUpdatedListener = onCredBlobUpdatedListener;
        login(username, password, blob);
    }

    public static void reloginUser(TomahawkService.OnLoginListener onLoginListener) {
        sOnLoginListener = onLoginListener;
        relogin();
    }

    public static void logoutUser(TomahawkService.OnLoginListener onLoginListener) {
        sOnLoginListener = onLoginListener;
        logout();
    }

    public static void onPrepared() {
        sTomahawkMediaPlayer.onPrepared();
    }

    public static void prepare(String uri, TomahawkMediaPlayer tomahawkMediaPlayer) {
        sTomahawkMediaPlayer = tomahawkMediaPlayer;
        prepare(uri);
    }

    public static void onResolved(final String qid, final boolean success, final String message,
            final String didYouMean) {
        if (success) {
            sSpotifyResolver.onResolved(qid);
        } else {
            Log.d(TAG, "onResolved: ERROR: " + message);
        }
    }

    public static void addResult(final String trackName, final int trackDuration,
            final int trackDiscnumber, final int trackIndex, final String trackUri,
            final String albumName, final int albumYear, final String artistName) {
        Result result = new Result();
        Track track = new Track();
        track.setName(trackName);
        track.setDuration(trackDuration);
        track.setTrackNumber(trackIndex);
        track.setPath(trackUri);
        track.setLocal(false);
        track.setResolver(sSpotifyResolver);
        Album album = new Album();
        album.setName(albumName);
        album.setFirstYear("" + albumYear);
        album.setLastYear("" + albumYear);
        Artist artist = new Artist();
        artist.setName(artistName);
        track.setAlbum(album);
        track.setArtist(artist);
        result.setTrack(track);
        result.setArtist(artist);
        result.setAlbum(album);
        result.setResolver(sSpotifyResolver);
        sSpotifyResolver.addResult(result);
    }

    public static void resolve(String qid, String query, SpotifyResolver spotifyResolver) {
        sSpotifyResolver = spotifyResolver;
        resolve(qid, query);
    }

    public static void onLogin(final boolean success, final String message, final String username) {
        if (success) {
            sOnLoginListener.onLogin(username);
        } else {
            sOnLoginListener.onLoginFailed(message);
        }
    }

    public static void onLogout() {
        sOnLoginListener.onLogout();
    }

    public static void onCredentialsBlobUpdated(final String blob) {
        sOnCredBlobUpdatedListener.onCredBlobUpdated(blob);
    }

    public static void onPlayerEndOfTrack() {
        sTomahawkMediaPlayer.onCompletion();
    }

    public static void onPlayerPositionChanged(final int position) {
        mCurrentPosition = position;
    }

    public static void onPlayerPause() {
    }

    public static void onPlayerPlay() {
    }

    public static void onTrackStarred() {
    }

    public static void onTrackUnStarred() {
    }

    public static int getCurrentPosition() {
        return mCurrentPosition;
    }

}
