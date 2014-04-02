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

import org.codehaus.jackson.map.ObjectMapper;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.infosystem.InfoSystemUtils;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.services.SpotifyService;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A {@link Resolver} which resolves {@link org.tomahawk.libtomahawk.collection.Track}s via
 * libspotify
 */
public class SpotifyResolver implements Resolver {

    private final static String TAG = SpotifyResolver.class.getName();

    private Messenger mToSpotifyMessenger = null;

    private final Messenger mFromSpotifyMessenger = new Messenger(new FromSpotifyHandler());

    private ObjectMapper mObjectMapper = InfoSystemUtils.constructObjectMapper();

    private int mId;

    private Drawable mIcon;

    private int mWeight = 90;

    private boolean mReady = true;

    private boolean mAuthenticated;

    private boolean mInitialized;

    /**
     * Handler of incoming messages from the SpotifyService's messenger.
     */
    private class FromSpotifyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case SpotifyService.MSG_ONINIT:
                        mInitialized = true;
                        break;
                    case SpotifyService.MSG_ONRESOLVED:
                        SpotifyResults spotifyResults = mObjectMapper
                                .readValue(msg.getData().getString(SpotifyService.STRING_KEY),
                                        SpotifyResults.class);
                        onResolved(spotifyResults);
                        break;
                    case SpotifyService.MSG_ONCREDBLOBUPDATED:
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
     *
     * @param id the id of this {@link Resolver}
     */
    public SpotifyResolver(int id, Context context) {
        mId = id;
        mIcon = context.getResources().getDrawable(R.drawable.spotify_icon);
        PipeLine.getInstance().onResolverReady();
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
        return mReady && mAuthenticated && mInitialized;
    }

    /**
     * @return this {@link Resolver}'s icon
     */
    @Override
    public Drawable getIcon() {
        return mIcon;
    }

    /**
     * Resolve the given {@link Query}
     *
     * @return whether or not the Resolver is ready to resolve
     */
    @Override
    public boolean resolve(Query query) {
        if (mToSpotifyMessenger != null && mAuthenticated && mInitialized) {
            try {
                SpotifyQuery spotifyQuery = new SpotifyQuery();
                spotifyQuery.queryKey = TomahawkUtils.getCacheKey(query);
                if (query.isFullTextQuery()) {
                    spotifyQuery.queryString = query.getFullTextQuery();
                } else {
                    spotifyQuery.queryString = query.getArtist().getName() + " " + query.getName();
                }
                String jsonString = mObjectMapper.writeValueAsString(spotifyQuery);
                SpotifyServiceUtils.sendMsg(mToSpotifyMessenger, SpotifyService.MSG_RESOLVE,
                        jsonString);
            } catch (IOException e) {
                Log.e(TAG, "SpotifyResolver: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
            return true;
        }
        return false;
    }

    /**
     * @return this {@link Resolver}'s id
     */
    @Override
    public int getId() {
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
                new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_REPORTING) {
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
                                Result result = new Result(spotifyResult.trackUri, track);
                                result.setTrack(track);
                                result.setArtist(artist);
                                result.setAlbum(album);
                                result.setResolvedBy(SpotifyResolver.this);
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
        return mReady && mInitialized;
    }
}