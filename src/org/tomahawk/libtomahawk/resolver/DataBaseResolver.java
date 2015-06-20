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

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.GrayOutTransformation;

import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Set;

/**
 * A {@link Resolver} which resolves {@link Track}s via our local database. Or in other words:
 * Fetches {@link Track}s from the local {@link UserCollection}. Can also be used to resolve from
 * remote {@link UserCollection}s.
 */
public class DataBaseResolver implements Resolver {

    private final String mId;

    private final int mWeight;

    private boolean mReady;

    private boolean mStopped;

    /**
     * Construct this {@link DataBaseResolver}
     */
    public DataBaseResolver() {
        mId = TomahawkApp.PLUGINNAME_USERCOLLECTION;
        mWeight = 100;
        mReady = false;
        mStopped = true;
        mReady = true;
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

    @Override
    public void loadIcon(ImageView imageView, boolean grayOut) {
        TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                R.drawable.ic_action_sd_storage, grayOut);
    }

    @Override
    public void loadIconWhite(ImageView imageView) {
        TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                R.drawable.ic_action_sd_storage_light);
    }

    @Override
    public void loadIconBackground(ImageView imageView, boolean grayOut) {
        imageView.setImageDrawable(new ColorDrawable(
                TomahawkApp.getContext().getResources()
                        .getColor(R.color.local_collection_resolver_bg)));
        if (grayOut) {
            imageView.setColorFilter(GrayOutTransformation.getColorFilter());
        } else {
            imageView.clearColorFilter();
        }
    }

    @Override
    public String getPrettyName() {
        return TomahawkApp.getContext().getString(R.string.local_collection_pretty_name);
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
            CollectionManager.getInstance().getCollection(mId).getQueries(
                    false).done(new DoneCallback<Set<Query>>() {
                @Override
                public void onDone(Set<Query> result) {
                    ArrayList<Result> results = new ArrayList<>();
                    for (Query existingQuery : result) {
                        String existingTrackName = existingQuery.getName();
                        String existingArtistName = existingQuery.getArtist().getName();
                        String existingAlbumName = existingQuery.getAlbum().getName();
                        if (queryToSearchFor.isFullTextQuery()) {
                            if (!TextUtils.isEmpty(queryToSearchFor.getFullTextQuery())) {
                                String toSearchForFullText = queryToSearchFor.getFullTextQuery();
                                if (TomahawkUtils
                                        .containsIgnoreCase(existingTrackName, toSearchForFullText)
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
                                if ((TomahawkUtils
                                        .containsIgnoreCase(existingTrackName, toSearchTrackName)
                                        || TomahawkUtils.containsIgnoreCase(toSearchTrackName,
                                        existingTrackName))
                                        && (TomahawkUtils.containsIgnoreCase(existingArtistName,
                                        toSearchArtistName)
                                        || TomahawkUtils
                                        .containsIgnoreCase(toSearchArtistName,
                                                existingArtistName))) {
                                    results.add(existingQuery.getPreferredTrackResult());
                                }
                            }
                        }
                    }
                    PipeLine.getInstance().reportResults(queryToSearchFor, results, mId);
                    mStopped = true;
                }
            });

        }
        return mReady;
    }

    /**
     * @return this {@link DataBaseResolver}'s id
     */
    @Override
    public String getId() {
        return mId;
    }

    /**
     * @return this {@link DataBaseResolver}'s weight
     */
    @Override
    public int getWeight() {
        return mWeight;
    }

    @Override
    public boolean isEnabled() {
        UserCollection userCollection = (UserCollection) CollectionManager.getInstance()
                .getCollection(TomahawkApp.PLUGINNAME_USERCOLLECTION);
        return userCollection.hasAudioItems();
    }
}
