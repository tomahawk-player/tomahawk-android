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
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.Filter;

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

    private Collection mCollection;

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
     * @param collection  reference to the {@link Collection} which we want to resolve {@link
     *                    Track}s from
     */
    public DataBaseResolver(int id, TomahawkApp tomahawkApp, Collection collection) {
        mWeight = 1000;
        mReady = false;
        mStopped = true;
        mTomahawkApp = tomahawkApp;
        mId = id;
        mName = String.valueOf(collection.getId());
        if (collection.getId() == UserCollection.Id) {
            mIcon = mTomahawkApp.getResources().getDrawable(R.drawable.ic_action_collection);
        } else {
            mIcon = mTomahawkApp.getResources().getDrawable(R.drawable.ic_resolver_default);
        }
        mCollection = collection;

        mReady = true;
    }

    /**
     * @return whether or not this {@link Resolver} is currently resolving
     */
    public boolean isResolving() {
        return mReady && !mStopped;
    }

    /**
     * @return the icon of this {@link Resolver} as a {@link Drawable}
     */
    public Drawable getIcon() {
        return mIcon;
    }

    /**
     * Resolve the given {@link Query}.
     *
     * @param query the {@link Query} which should be resolved
     */
    public void resolve(Query query) {
        mStopped = false;
        if (query.isFullTextQuery()) {
            new TomahawkListItemFilter(query.getQid(), this, query.getFullTextQuery()).filter(null);
        } else {
            new TomahawkListItemFilter(query.getQid(), this, query.getTrackName(),
                    query.getAlbumName(), query.getArtistName()).filter(null);
        }
    }

    /**
     * We use a {@link Filter} to resolve our {@link Track}s from the {@link UserCollection}
     */
    private class TomahawkListItemFilter extends Filter {

        private String mQid;

        private Resolver mResolver;

        private String mFullTextQuery = "";

        private String mTrackName = "";

        private String mAlbumName = "";

        private String mArtistName = "";

        /**
         * Construct this {@link TomahawkListItemFilter}, if you want to do a fullTextQuery search.
         *
         * @param qid           the id of the {@link Query} to be resolved
         * @param resolver      the {@link Resolver} that we will associate with the resolved {@link
         *                      Result}s
         * @param fullTextQuery {@link String}  containing the fullTextQuery to be searched for
         */
        public TomahawkListItemFilter(final String qid, final Resolver resolver,
                final String fullTextQuery) {
            mQid = qid;
            mResolver = resolver;
            mFullTextQuery = fullTextQuery.toLowerCase().trim();
        }

        /**
         * Construct this {@link TomahawkListItemFilter}, if you want to do a fullTextQuery search.
         *
         * @param qid        the id of the {@link Query} to be resolved
         * @param resolver   the {@link Resolver} that we will associate with the resolved {@link
         *                   Result}s
         * @param trackName  {@link String}  containing the {@link Track}s name
         * @param albumName  {@link String}  containing the {@link org.tomahawk.libtomahawk.collection.Album}s
         *                   name
         * @param artistName {@link String}  containing the {@link org.tomahawk.libtomahawk.collection.Artist}s
         *                   name
         */
        public TomahawkListItemFilter(final String qid, final Resolver resolver,
                final String trackName, final String albumName, final String artistName) {
            mQid = qid;
            mResolver = resolver;
            mTrackName = trackName.toLowerCase().trim();
            mAlbumName = albumName.toLowerCase().trim();
            mArtistName = artistName.toLowerCase().trim();
        }

        /**
         * Called when this {@link TomahawkListItemFilter} is done with performFiltering(...)
         *
         * @param constraint can be ignored in our case
         * @param results    all found {@link FilterResults}
         */
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            ArrayList<Result> resultList = (ArrayList<Result>) results.values;
            mStopped = true;
            mTomahawkApp.getPipeLine().reportResults(mQid, resultList);
        }

        /**
         * Perform the filtering process. Automatically called whenever this {@link
         * TomahawkListItemFilter} is constructed
         *
         * @param constraint can be ignored in our case
         * @return all found {@link FilterResults}
         */
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<Result> filteredResults = getFilteredResults();

            FilterResults results = new FilterResults();
            synchronized (this) {
                results.values = filteredResults;
            }

            return results;
        }

        /**
         * The actual resolving process.
         *
         * @return an {@link ArrayList} of all found {@link Result}s
         */
        protected ArrayList<Result> getFilteredResults() {
            ArrayList<Result> filteredResults = new ArrayList<Result>();
            if (TextUtils.isEmpty(mFullTextQuery) && TextUtils.isEmpty(mTrackName) && TextUtils
                    .isEmpty(mAlbumName) && TextUtils.isEmpty(mArtistName)) {
                return filteredResults;
            }
            List<TomahawkBaseAdapter.TomahawkListItem> inputList
                    = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
            inputList.addAll(mCollection.getTracks());

            for (TomahawkBaseAdapter.TomahawkListItem item : inputList) {
                if (!TextUtils.isEmpty(mFullTextQuery)) {
                    if (item.getName().toLowerCase().contains(mFullTextQuery) || (
                            item.getArtist() != null && item.getArtist().getName().toLowerCase()
                                    .contains(mFullTextQuery)) || (item.getAlbum() != null && item
                            .getAlbum().getName().toLowerCase().contains(mFullTextQuery))) {
                        Result r;
                        r = new Result((Track) item);
                        r.setResolver(mResolver);
                        filteredResults.add(r);
                    }
                } else {
                    if (item.getName().toLowerCase().contains(mTrackName) || (
                            item.getArtist() != null && item.getArtist().getName().toLowerCase()
                                    .contains(mArtistName)) || (item.getAlbum() != null && item
                            .getAlbum().getName().toLowerCase().contains(mAlbumName))) {
                        Result r;
                        r = new Result((Track) item);
                        r.setResolver(mResolver);
                        filteredResults.add(r);
                    }
                }
            }
            return filteredResults;
        }
    }

    /**
     * @return this {@link DataBaseResolver}'s id
     */
    public int getId() {
        return mId;
    }

    /**
     * @return this {@link DataBaseResolver}'s weight
     */
    public int getWeight() {
        return mWeight;
    }
}
