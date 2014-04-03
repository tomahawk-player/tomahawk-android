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

import org.tomahawk.tomahawk_android.services.SpotifyService;

/**
 * Wrapper class around libspotify. Provides functionality to talk to the c library.
 */
public class LibSpotifyWrapper {

    private static boolean sIsAuthenticated = false;

    private static SpotifyService sSpotifyService;

    native public static void nativeinit(ClassLoader loader, String storagePath);

    native public static void nativedestroy();

    native public static void nativelogin(String username, String password, String blob);

    native public static void nativerelogin();

    native public static void nativelogout();

    native public static void nativeresolve(String qid, String query);

    native public static void nativeprepare(String uri);

    native public static void nativeplay();

    native public static void nativepause();

    native public static void nativeseek(int position);

    native public static void nativestar();

    native public static void nativeunstar();

    native public static void nativesetbitrate(int bitratemode);

    public static void setSpotifyService(SpotifyService sSpotifyService) {
        LibSpotifyWrapper.sSpotifyService = sSpotifyService;
    }

    /**
     * Called by libspotify, when a track has been prepared
     */
    public static void onPrepared() {
        sSpotifyService.onPrepared();
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
            final String didYouMean, final int count, final int[] trackDurations,
            final int[] trackDiscnumbers, final int[] trackIndexes, final int[] albumYears,
            final String[] trackNames, final String[] trackUris, final String[] albumNames,
            final String[] artistNames) {
        sSpotifyService.onResolved(qid, success, message, didYouMean, count, trackDurations,
                trackDiscnumbers, trackIndexes, albumYears, trackNames, trackUris, albumNames,
                artistNames);
    }

    /**
     * Called by libspotify when initialized
     */
    public static void onInit() {
        sSpotifyService.onInit();
        if (sIsAuthenticated) {
            onLogin(true, "Spotify user was already logged in", "");
        }
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
        sSpotifyService.onLogin(success, message, username);
    }

    /**
     * Called by libspotify on logout
     */
    public static void onLogout() {
        sIsAuthenticated = false;
        sSpotifyService.onLogout();
    }

    /**
     * Called by libspotify, when the credential blob has been updated
     *
     * @param username {@link String} containing the username
     * @param blob     {@link String} containing the blob
     */
    public static void onCredentialsBlobUpdated(final String username, final String blob) {
        sIsAuthenticated = true;
        sSpotifyService.onCredentialsBlobUpdated(username, blob);
    }

    /**
     * Called by libspotify, when the OpenSLES player has finished playing a track
     */
    public static void onPlayerEndOfTrack() {
        sSpotifyService.onPlayerEndOfTrack();
    }

    /**
     * Called by libspotify, when the OpenSLES player signals that the current position has changed
     */
    public static void onPlayerPositionChanged(final int position) {
        sSpotifyService.onPlayerPositionChanged(position);
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

}
