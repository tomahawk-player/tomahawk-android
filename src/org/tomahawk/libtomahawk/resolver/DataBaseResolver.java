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
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 25.01.13
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
     * @return wether or not this resolver is currently resolving
     */
    public boolean isResolving() {
        return mReady && !mStopped;
    }

    /**
     * @return the icon of this resolver as a drawable
     */
    public Drawable getIcon() {
        return mIcon;
    }

    /**
     * resolve the given Query.
     *
     * @param query the query which should be resolved
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

    private class TomahawkListItemFilter extends Filter {

        private String mQid;

        private Resolver mResolver;

        private String mFullTextQuery = "";

        private String mTrackName = "";

        private String mAlbumName = "";

        private String mArtistName = "";

        public TomahawkListItemFilter(final String qid, final Resolver resolver,
                final String fullTextQuery) {
            mQid = qid;
            mResolver = resolver;
            mFullTextQuery = fullTextQuery.toLowerCase().trim();
        }

        public TomahawkListItemFilter(final String qid, final Resolver resolver,
                final String trackName, final String albumName, final String artistName) {
            mQid = qid;
            mResolver = resolver;
            mTrackName = trackName.toLowerCase().trim();
            mAlbumName = albumName.toLowerCase().trim();
            mArtistName = artistName.toLowerCase().trim();
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            ArrayList<Result> resultList = (ArrayList<Result>) results.values;
            mStopped = true;
            mTomahawkApp.getPipeLine().reportResults(mQid, resultList);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<Result> filteredResults = getFilteredResults();

            FilterResults results = new FilterResults();
            synchronized (this) {
                results.values = filteredResults;
            }

            return results;
        }

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

    public int getId() {
        return mId;
    }

    public int getWeight() {
        return mWeight;
    }
}
