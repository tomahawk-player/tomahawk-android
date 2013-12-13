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
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.tomahawk_android.services.TomahawkService;
import org.tomahawk.tomahawk_android.utils.TomahawkMediaPlayer;

import android.util.Log;

/**
 * Wrapper class around libspotify. Provides functionality to talk to the c library.
 */
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

    native public static void nativesetbitrate(int bitratemode);

    /**
     * Initialize libspotify
     *
     * @param loader      {@link ClassLoader} needed to initialize libspotify
     * @param storagePath {@link String} containing the path to where libspotify stores its stuff
     */
    public static void init(ClassLoader loader, String storagePath) {
        if (!mInitialized) {
            nativeinit(loader, storagePath);
            mInitialized = true;
        }
    }

    /**
     * Destroy libspotify session
     */
    public static void destroy() {
        if (mInitialized) {
            //nativedestroy();
            //mInitialized = false;
        }
    }

    /**
     * Use loginUser(...) instead, if you want a proper callback. Login Spotify account. Does only
     * need blob OR password. Not both
     *
     * @param username {@link String} containing the username
     * @param password {@link String} containing the password
     * @param blob     {@link String} containing the blob
     */
    private static void login(String username, String password, String blob) {
        if (mInitialized) {
            nativelogin(username, password, blob);
        }
    }

    /**
     * Use reloginUser(...) instead, if you want a proper callback. Relogin, in case session
     * expired
     */
    private static void relogin() {
        if (mInitialized) {
            nativerelogin();
        }
    }

    /**
     * Use logoutUser(...) instead, if you want a proper callback. Logout Spotify account
     */
    private static void logout() {
        if (mInitialized) {
            nativelogout();
        }
    }

    /**
     * Resolve a {@link org.tomahawk.libtomahawk.resolver.Query} via libspotify
     *
     * @param qid   {@link String} containg the query id
     * @param query {@link org.tomahawk.libtomahawk.resolver.Query} to be resolved
     */
    private static void resolve(String qid, Query query) {
        if (mInitialized) {
            String queryString;
            if (query.isFullTextQuery()) {
                queryString = query.getFullTextQuery();
            } else {
                queryString = query.getArtist() + " " + query.getName();
            }
            nativeresolve(qid, queryString);
            Log.d("test", "nativeresolve " + qid + ", " + queryString);
        }
    }

    /**
     * Prepare a track via our native OpenSLES layer
     *
     * @param uri {@link String} containing the previously resolved Spotify URI
     */
    private static void prepare(String uri) {
        if (mInitialized) {
            nativeprepare(uri);
        }
    }

    /**
     * Start playing the previously prepared track via our native OpenSLES layer
     */
    public static void play() {
        if (mInitialized) {
            nativeplay();
        }
    }

    /**
     * Pause playing the previously prepared track via our native OpenSLES layer
     */
    public static void pause() {
        if (mInitialized) {
            nativepause();
        }
    }

    /**
     * Seek to position (in ms) in the currently prepared track via our native OpenSLES layer
     *
     * @param position position to be seeked to (in ms)
     */
    public static void seek(int position) {
        if (mInitialized) {
            nativeseek(position);
        }
    }

    /**
     * Star the current track within Spotify
     */
    public static void star() {
        if (mInitialized) {
            nativestar();
        }
    }

    /**
     * Unstar the current track within Spotify
     */
    public static void unstar() {
        if (mInitialized) {
            nativeunstar();
        }
    }

    /**
     * Set the preferred bitrate mode
     *
     * @param bitratemode int containing values '1'(96 kbit/s), '2'(160 kbit/s) or '3'(320 kbit/s)
     */
    public static void setbitrate(int bitratemode) {
        if (mInitialized) {
            nativesetbitrate(bitratemode);
        }
    }

    /**
     * Login Spotify user. Does only need blob OR password. Not both.
     *
     * @param username                  {@link String} containing the username
     * @param password                  {@link String} containing the password
     * @param blob                      {@link String} containing the blob
     * @param onLoginListener           {@link org.tomahawk.tomahawk_android.services.TomahawkService.OnLoginListener}
     *                                  used to get a callback on login
     * @param onCredBlobUpdatedListener {@link org.tomahawk.tomahawk_android.services.TomahawkService.OnCredBlobUpdatedListener}
     *                                  used to get callback when the credential blob inside Spotify
     *                                  has been updated
     */
    public static void loginUser(String username, String password, String blob,
            TomahawkService.OnLoginListener onLoginListener,
            TomahawkService.OnCredBlobUpdatedListener onCredBlobUpdatedListener) {
        sOnLoginListener = onLoginListener;
        sOnCredBlobUpdatedListener = onCredBlobUpdatedListener;
        login(username, password, blob);
    }

    /**
     * Relogin user
     *
     * @param onLoginListener {@link org.tomahawk.tomahawk_android.services.TomahawkService.OnLoginListener}
     *                        used to get a callback on login
     */
    public static void reloginUser(TomahawkService.OnLoginListener onLoginListener) {
        sOnLoginListener = onLoginListener;
        relogin();
    }

    /**
     * Logout user
     *
     * @param onLoginListener {@link org.tomahawk.tomahawk_android.services.TomahawkService.OnLoginListener}
     *                        used to get a callback on login
     */
    public static void logoutUser(TomahawkService.OnLoginListener onLoginListener) {
        sOnLoginListener = onLoginListener;
        logout();
    }

    /**
     * Resolve a {@link org.tomahawk.libtomahawk.resolver.Query} via Spotify
     *
     * @param qid             {@link String} containing the {@link org.tomahawk.libtomahawk.resolver.Query}'s
     *                        id
     * @param query           {@link org.tomahawk.libtomahawk.resolver.Query} to be resolved
     * @param spotifyResolver reference to the {@link SpotifyResolver}
     */
    public static void resolve(String qid, Query query, SpotifyResolver spotifyResolver) {
        sSpotifyResolver = spotifyResolver;
        resolve(qid, query);
    }

    /**
     * Prepare a track via our native OpenSLES layer
     *
     * @param uri                 {@link String} containing the previously resolved Spotify URI
     * @param tomahawkMediaPlayer reference to {@link TomahawkMediaPlayer}, so that we are able to
     *                            callback on certain events
     */
    public static void prepare(String uri, TomahawkMediaPlayer tomahawkMediaPlayer) {
        sTomahawkMediaPlayer = tomahawkMediaPlayer;
        prepare(uri);
    }

    /**
     * Called by libspotify, when a track has been prepared
     */
    public static void onPrepared() {
        sTomahawkMediaPlayer.onPrepared();
    }

    /**
     * Called by libspotify, when a {@link org.tomahawk.libtomahawk.resolver.Query} has been
     * resolved
     *
     * @param qid        {@link String} containing the {@link org.tomahawk.libtomahawk.resolver.Query}'s
     *                   id
     * @param success    boolean signaling whether or not the {@link org.tomahawk.libtomahawk.resolver.Query}
     *                   has been successfully resolved
     * @param message    {@link String} containing libspotify's message
     * @param didYouMean {@link String} containing libspotify's "Did you mean?" suggestion
     */
    public static void onResolved(final String qid, final boolean success, final String message,
            final String didYouMean) {
        sSpotifyResolver.onResolved(qid);
        Log.d("test", "onResolved " + qid);
    }

    /**
     * Called by libspotify. Add a result to the static {@link SpotifyResolver}
     */
    public static void addResult(final String trackName, final int trackDuration,
            final int trackDiscnumber, final int trackIndex, final String trackUri,
            final String albumName, final int albumYear, final String artistName) {
        Artist artist = Artist.get(artistName);
        Album album = Album.get(albumName, artist);
        album.setFirstYear("" + albumYear);
        album.setLastYear("" + albumYear);
        Track track = Track.get(trackName, album, artist);
        track.setDuration(trackDuration);
        track.setAlbumPos(trackIndex);
        Result result = new Result(trackUri, track);
        result.setTrack(track);
        result.setArtist(artist);
        result.setAlbum(album);
        result.setResolvedBy(sSpotifyResolver);
        sSpotifyResolver.addResult(result);
    }

    /**
     * Called by libspotify on login
     *
     * @param success  boolean signaling whether or not the login process was successful
     * @param message  {@link String} containing libspotify's message
     * @param username {@link String} containing the username, which has been used to login to
     *                 Spotify
     */
    public static void onLogin(final boolean success, final String message, final String username) {
        if (success) {
            sOnLoginListener.onLogin(username);
        } else {
            sOnLoginListener.onLoginFailed(message);
        }
    }

    /**
     * Called by libspotify on logout
     */
    public static void onLogout() {
        sOnLoginListener.onLogout();
    }

    /**
     * Called by libspotify, when the credential blob has been updated
     *
     * @param blob {@link String} containing the blob
     */
    public static void onCredentialsBlobUpdated(final String blob) {
        sOnCredBlobUpdatedListener.onCredBlobUpdated(blob);
    }

    /**
     * Called by libspotify, when the OpenSLES player has finished playing a track
     */
    public static void onPlayerEndOfTrack() {
        sTomahawkMediaPlayer.onCompletion();
    }

    /**
     * Called by libspotify, when the OpenSLES player signals that the current position has changed
     */
    public static void onPlayerPositionChanged(final int position) {
        mCurrentPosition = position;
    }

    /**
     * Called by libspotify, when the OpenSLES player has paused
     */
    public static void onPlayerPause() {
    }

    /**
     * Called by libspotify, when the OpenSLES player has started playing
     */
    public static void onPlayerPlay() {
    }

    /**
     * Called by libspotify, when a track has been starred
     */
    public static void onTrackStarred() {
    }

    /**
     * Called by libspotify, when a track has been unstarred
     */
    public static void onTrackUnStarred() {
    }

    /**
     * @return the current position of playback inside our OpenSLES player
     */
    public static int getCurrentPosition() {
        return mCurrentPosition;
    }

}
