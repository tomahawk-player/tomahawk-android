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
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Resolver} which resolves {@link Track}s via our local database. Or in other words:
 * Fetches {@link Track}s from the local {@link UserCollection}. Can also be used to resolve from
 * remote {@link UserCollection}s.
 */
public class DataBaseResolver implements Resolver {

    private Context mContext;

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
     * @param id the id of this {@link Resolver}
     */
    public DataBaseResolver(int id, Context context) {
        mId = id;
        mContext = context;
        mWeight = 100;
        mReady = false;
        mStopped = true;
        mName = String.valueOf(PipeLine.RESOLVER_ID_USERCOLLECTION);
        if (id == PipeLine.RESOLVER_ID_USERCOLLECTION) {
            mIcon = context.getResources().getDrawable(R.drawable.ic_action_sd_storage);
        } else {
            mIcon = context.getResources().getDrawable(R.drawable.ic_resolver_default);
        }

        mReady = true;
        PipeLine.getInstance().onResolverReady();
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
     * @param queryToSearchFor the {@link Query} which should be resolved
     * @return whether or not the Resolver is ready to resolve
     */
    @Override
    public boolean resolve(final Query queryToSearchFor) {
        if (mReady) {
            mStopped = false;
            ArrayList<Result> results = new ArrayList<Result>();
            List<Query> inputList = UserCollection.getInstance().getQueries(false);

            for (Query existingQuery : inputList) {
                String existingTrackName = existingQuery.getName();
                String existingArtistName = existingQuery.getArtist().getName();
                String existingAlbumName = existingQuery.getAlbum().getName();
                if (queryToSearchFor.isFullTextQuery()) {
                    if (!TextUtils.isEmpty(queryToSearchFor.getFullTextQuery())) {
                        String toSearchForFullText = queryToSearchFor.getFullTextQuery();
                        if (TomahawkUtils.containsIgnoreCase(existingTrackName, toSearchForFullText)
                                || TomahawkUtils.containsIgnoreCase(existingArtistName,
                                toSearchForFullText)
                                || TomahawkUtils.containsIgnoreCase(existingAlbumName,
                                toSearchForFullText)
                                || TomahawkUtils.containsIgnoreCase(toSearchForFullText,
                                existingTrackName)
                                || TomahawkUtils.containsIgnoreCase(toSearchForFullText,
                                existingArtistName)
                                || TomahawkUtils.containsIgnoreCase(toSearchForFullText,
                                existingAlbumName)) {
                            results.add(existingQuery.getPreferredTrackResult());
                        }

                    }
                } else {
                    if (!TextUtils.isEmpty(queryToSearchFor.getName()) &&
                            !TextUtils.isEmpty(queryToSearchFor.getArtist().getName())) {
                        String toSearchTrackName = queryToSearchFor.getName();
                        String toSearchArtistName = queryToSearchFor.getArtist().getName();
                        if ((TomahawkUtils.containsIgnoreCase(existingTrackName, toSearchTrackName)
                                || TomahawkUtils.containsIgnoreCase(toSearchTrackName,
                                existingTrackName))
                                && (TomahawkUtils.containsIgnoreCase(existingArtistName,
                                toSearchArtistName)
                                || TomahawkUtils
                                .containsIgnoreCase(toSearchArtistName, existingArtistName))) {
                            results.add(existingQuery.getPreferredTrackResult());
                        }
                    }
                }
            }
            PipeLine.getInstance().reportResults(queryToSearchFor.getCacheKey(), results, mId);
            mStopped = true;
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
