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

import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link Resolver} which resolves {@link org.tomahawk.libtomahawk.collection.Track}s via
 * libspotify
 */
public class SpotifyResolver implements Resolver {

    private final static String TAG = SpotifyResolver.class.getName();

    private TomahawkApp mTomahawkApp;

    private int mId;

    private Drawable mIcon;

    private int mWeight;

    private boolean mReady;

    private boolean mAuthenticated;

    private boolean mStopped;

    private ConcurrentHashMap<String, Query> mWaitingQueries
            = new ConcurrentHashMap<String, Query>();

    private static int SPOTIFY_RESOLVER_WORKER_COUNT = 10;

    /**
     * Construct a new {@link SpotifyResolver}
     *
     * @param id          the id of this {@link Resolver}
     * @param tomahawkApp reference needed to {@link TomahawkApp}, so that we have access to the
     *                    {@link org.tomahawk.libtomahawk.resolver.PipeLine} to report our results
     */
    public SpotifyResolver(int id, TomahawkApp tomahawkApp) {
        mTomahawkApp = tomahawkApp;
        mId = id;
        mIcon = mTomahawkApp.getResources().getDrawable(R.drawable.spotify_icon);
        mWeight = 90;
        mReady = true;
        mTomahawkApp.getPipeLine().onResolverReady();
        mStopped = true;
    }

    /**
     * @return whether or not this resolver is currently resolving
     */
    @Override
    public boolean isResolving() {
        return mReady && mAuthenticated && !mStopped;
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
        mStopped = false;
        if (mAuthenticated) {
            String queryKey = TomahawkUtils.getCacheKey(query);
            if (mWaitingQueries.size() >= SPOTIFY_RESOLVER_WORKER_COUNT) {
                mWaitingQueries.put(queryKey, query);
            } else {
                LibSpotifyWrapper.resolve(queryKey, query, this, mTomahawkApp);
            }
        }
        return mAuthenticated;
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
    public void onResolved(String queryKey, ArrayList<Result> results) {
        mStopped = true;
        mWaitingQueries.remove(queryKey);
        // report our results to the pipeline
        if (results != null && !results.isEmpty()) {
            mTomahawkApp.getPipeLine().reportResults(queryKey, results);
        }
    }

    public void onError(String message) {
        Log.d(TAG, "Spotify encountered an error during the resolving process: '" + message + "'");
    }

    /**
     * @return whether or not this {@link Resolver} is ready
     */
    @Override
    public boolean isReady() {
        return mReady;
    }

    /**
     * Set whether or not this {@link Resolver} is authenticated
     */
    public void setAuthenticated(boolean authenticated) {
        mAuthenticated = authenticated;
    }
}