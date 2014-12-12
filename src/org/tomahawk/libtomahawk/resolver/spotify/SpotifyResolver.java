/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.libtomahawk.resolver.spotify;

import org.tomahawk.libtomahawk.authentication.SpotifyAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.infosystem.InfoSystemUtils;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.resolver.spotify.models.SpotifyQuery;
import org.tomahawk.libtomahawk.resolver.spotify.models.SpotifyResult;
import org.tomahawk.libtomahawk.resolver.spotify.models.SpotifyResults;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.SpotifyService;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A {@link Resolver} which resolves {@link org.tomahawk.libtomahawk.collection.Track}s via
 * libspotify
 */
public class SpotifyResolver extends Resolver {

    private final static String TAG = SpotifyResolver.class.getSimpleName();

    private Messenger mToSpotifyMessenger = null;

    private final Messenger mFromSpotifyMessenger =
            new Messenger(new FromSpotifyHandler(Looper.getMainLooper()));

    private String mId;

    private int mIconResId;

    private int mWeight = 90;

    private boolean mAuthenticated;

    private boolean mInitialized;

    // In case we currently don't have a connection to the SpotifyService, we cache Queries here
    private ArrayList<Query> mCachedQueries = new ArrayList<Query>();

    /**
     * Handler of incoming messages from the SpotifyService's messenger.
     */
    private class FromSpotifyHandler extends Handler {

        private FromSpotifyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case SpotifyService.MSG_ONINIT:
                        mInitialized = true;
                        resolveWaitingQueries();
                        break;
                    case SpotifyService.MSG_ONRESOLVED:
                        SpotifyResults spotifyResults = InfoSystemUtils.getObjectMapper()
                                .readValue(msg.getData().getString(SpotifyService.STRING_KEY),
                                        SpotifyResults.class);
                        onResolved(spotifyResults);
                        break;
                    case SpotifyService.MSG_ONLOGIN:
                        mAuthenticated = true;
                        break;
                    default:
                        super.handleMessage(msg);
                }
            } catch (IOException e) {
                Log.e(TAG, "handleMessage: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Construct a new {@link SpotifyResolver}
     */
    public SpotifyResolver(OnResolverReadyListener onResolverReadyListener) {
        super(SpotifyAuthenticatorUtils.SPOTIFY_PRETTY_NAME, onResolverReadyListener);

        mId = TomahawkApp.PLUGINNAME_SPOTIFY;
        mIconResId = R.drawable.ic_spotify;
        onResolverReady();
    }

    public void setToSpotifyMessenger(Messenger toSpotifyMessenger) {
        mToSpotifyMessenger = toSpotifyMessenger;
        if (mToSpotifyMessenger != null) {
            SpotifyServiceUtils.registerMsg(mToSpotifyMessenger, mFromSpotifyMessenger);
        }
    }

    /**
     * @return whether or not this resolver is currently resolving
     */
    @Override
    public boolean isResolving() {
        return mAuthenticated && mInitialized;
    }

    @Override
    public String getIconPath() {
        return null;
    }

    @Override
    public String getCollectionName() {
        return SpotifyAuthenticatorUtils.SPOTIFY_PRETTY_NAME;
    }

    @Override
    public int getIconResId() {
        return mIconResId;
    }

    /**
     * Resolve the given {@link Query}
     *
     * @return whether or not the Resolver is ready to resolve
     */
    @Override
    public boolean resolve(Query query) {
        if (mToSpotifyMessenger != null) {
            if (mAuthenticated && mInitialized) {
                sendResolveMsg(query);
                return true;
            }
        } else {
            mCachedQueries.add(query);
            TomahawkApp.getContext()
                    .sendBroadcast(new Intent(SpotifyService.REQUEST_SPOTIFYSERVICE));
        }
        return false;
    }

    private void resolveWaitingQueries() {
        if (mToSpotifyMessenger != null) {
            if (mAuthenticated && mInitialized) {
                for (int i = 0; i < mCachedQueries.size(); i++) {
                    sendResolveMsg(mCachedQueries.remove(i));
                }
            }
        }
    }

    private void sendResolveMsg(Query query) {
        try {
            SpotifyQuery spotifyQuery = new SpotifyQuery();
            spotifyQuery.queryKey = query.getCacheKey();
            if (query.isFullTextQuery()) {
                spotifyQuery.queryString = query.getFullTextQuery();
            } else {
                spotifyQuery.queryString = query.getArtist().getName() + " " + query
                        .getName();
            }
            String jsonString = InfoSystemUtils.getObjectMapper().writeValueAsString(spotifyQuery);
            SpotifyServiceUtils.sendMsg(mToSpotifyMessenger, SpotifyService.MSG_RESOLVE,
                    jsonString);
        } catch (IOException e) {
            Log.e(TAG, "SpotifyResolver: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    /**
     * @return this {@link Resolver}'s id
     */
    @Override
    public String getId() {
        return mId;
    }

    /**
     * @return this {@link Resolver}'s weight
     */
    @Override
    public int getWeight() {
        return mWeight;
    }

    /**
     * Called by {@link LibSpotifyWrapper}, which has been called by libspotify. Signals that the
     * {@link org.tomahawk.libtomahawk.resolver.Query} with the given query key has been resolved.
     */
    public void onResolved(final SpotifyResults spotifyResults) {
        ThreadManager.getInstance().execute(
                new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_REPORTING_SUBSCRIPTION) {
                    @Override
                    public void run() {
                        // report our results to the pipeline
                        if (spotifyResults != null && !spotifyResults.results.isEmpty()) {
                            ArrayList<Result> results = new ArrayList<Result>();
                            for (SpotifyResult spotifyResult : spotifyResults.results) {
                                Artist artist = Artist.get(spotifyResult.artistName);
                                Album album = Album.get(spotifyResult.albumName, artist);
                                album.setFirstYear("" + spotifyResult.albumYear);
                                album.setLastYear("" + spotifyResult.albumYear);
                                Track track = Track.get(spotifyResult.trackName, album, artist);
                                track.setDiscNumber(spotifyResult.trackDiscnumber);
                                track.setDuration(spotifyResult.trackDuration);
                                track.setAlbumPos(spotifyResult.trackIndex);
                                Result result = Result.get(spotifyResult.trackUri, track,
                                        SpotifyResolver.this, spotifyResults.qid);
                                result.setTrack(track);
                                result.setArtist(artist);
                                result.setAlbum(album);
                                results.add(result);
                            }
                            PipeLine.getInstance().reportResults(spotifyResults.qid, results, mId);
                        }
                    }
                }
        );
    }

    /**
     * @return whether or not this {@link Resolver} is ready
     */
    @Override
    public boolean isReady() {
        return mToSpotifyMessenger != null && mInitialized;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}