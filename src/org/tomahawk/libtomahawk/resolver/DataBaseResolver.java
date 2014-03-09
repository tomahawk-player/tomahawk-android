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
package org.tomahawk.libtomahawk.resolver;

import org.json.JSONObject;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Resolver} which resolves {@link Track}s via our local database. Or in other words:
 * Fetches {@link Track}s from the local {@link UserCollection}. Can also be used to resolve from
 * remote {@link Collection}s.
 */
public class DataBaseResolver implements Resolver {

    private TomahawkApp mTomahawkApp;

    private int mId;

    private String mName;

    private Drawable mIcon;

    private int mWeight;

    private int mTimeout;

    private JSONObject mConfig;

    private boolean mReady;

    private boolean mStopped;

    /**
     * Construct this {@link DataBaseResolver}
     *
     * @param id          the id of this {@link Resolver}
     * @param tomahawkApp reference needed to {@link TomahawkApp}, so that we have access to the
     *                    {@link org.tomahawk.libtomahawk.resolver.PipeLine} to report our results
     */
    public DataBaseResolver(int id, TomahawkApp tomahawkApp) {
        mWeight = 100;
        mReady = false;
        mStopped = true;
        mTomahawkApp = tomahawkApp;
        mId = id;
        mName = String.valueOf(TomahawkApp.RESOLVER_ID_USERCOLLECTION);
        if (id == TomahawkApp.RESOLVER_ID_USERCOLLECTION) {
            mIcon = mTomahawkApp.getResources()
                    .getDrawable(R.drawable.ic_action_device_access_sd_storage);
        } else {
            mIcon = mTomahawkApp.getResources().getDrawable(R.drawable.ic_resolver_default);
        }

        mReady = true;
        mTomahawkApp.getPipeLine().onResolverReady();
    }

    /**
     * @return whether or not this {@link Resolver} is ready
     */
    @Override
    public boolean isReady() {
        return mReady;
    }

    /**
     * @return whether or not this {@link Resolver} is currently resolving
     */
    @Override
    public boolean isResolving() {
        return mReady && !mStopped;
    }

    /**
     * @return the icon of this {@link Resolver} as a {@link Drawable}
     */
    @Override
    public Drawable getIcon() {
        return mIcon;
    }

    /**
     * Resolve the given {@link Query}.
     *
     * @param query the {@link Query} which should be resolved
     * @return whether or not the Resolver is ready to resolve
     */
    @Override
    public boolean resolve(final Query query) {
        if (mReady) {
            mStopped = false;
            ArrayList<Result> results = new ArrayList<Result>();
            UserCollection userCollection = (UserCollection) mTomahawkApp.getSourceList()
                    .getLocalSource().getCollection();
            if (userCollection == null || (TextUtils.isEmpty(query.getFullTextQuery()) && TextUtils
                    .isEmpty(query.getName()) && TextUtils
                    .isEmpty(query.getAlbum().getName()) && TextUtils
                    .isEmpty(query.getArtist().getName()))) {
                return false;
            }
            List<Query> inputList = userCollection.getQueries(false);

            for (Query q : inputList) {
                if (!TextUtils.isEmpty(q.getFullTextQuery())) {
                    if (q.getName().toLowerCase().contains(q.getFullTextQuery())
                            || q.getArtist().getName().toLowerCase().contains(q.getFullTextQuery())
                            || q.getAlbum().getName().toLowerCase()
                            .contains(q.getFullTextQuery())) {
                        results.add(q.getPreferredTrackResult());
                    }
                } else {
                    if (q.getName().toLowerCase().contains(q.getName())
                            && q.getArtist().getName().toLowerCase()
                            .contains(q.getArtist().getName())
                            && q.getAlbum().getName().toLowerCase()
                            .contains(q.getAlbum().getName())) {
                        results.add(q.getPreferredTrackResult());
                    }
                }
            }
            mTomahawkApp.getPipeLine()
                    .reportResults(TomahawkUtils.getCacheKey(query), results);
        }
        return mReady;
    }

    /**
     * @return this {@link DataBaseResolver}'s id
     */
    @Override
    public int getId() {
        return mId;
    }

    /**
     * @return this {@link DataBaseResolver}'s weight
     */
    @Override
    public int getWeight() {
        return mWeight;
    }
}
