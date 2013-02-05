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

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;
import org.json.JSONObject;
import org.tomahawk.libtomahawk.*;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.graphics.drawable.Drawable;
import android.widget.Filter;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com>
 * Date: 25.01.13
 */
public class DataBaseResolver implements Resolver {

    public static final int DATABASERESOLVER_FILTER_MODE_TRACKS = 0;
    public static final int DATABASERESOLVER_FILTER_MODE_ARTISTS = 1;
    public static final int DATABASERESOLVER_FILTER_MODE_ALBUMS = 2;

    private TomahawkApp mTomahawkApp;
    private Collection mCollection;

    private String mName;
    private Drawable mIcon;
    private int mWeight;
    private int mTimeout;
    private JSONObject mConfig;

    private boolean mReady;
    private boolean mStopped;

    public DataBaseResolver(TomahawkApp tomahawkApp, Collection collection) {
        mReady = false;
        mStopped = true;
        mTomahawkApp = tomahawkApp;
        mName = String.valueOf(collection.getId());
        if (collection.getId() == UserCollection.Id)
            mIcon = mTomahawkApp.getResources().getDrawable(R.drawable.ic_action_collection);
        else
            mIcon = mTomahawkApp.getResources().getDrawable(R.drawable.ic_resolver_default);
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
     * @param query the query which should be resolved
     */
    public void resolve(Query query) {
        mStopped = false;
        getFilter(query.getQid(), this, DATABASERESOLVER_FILTER_MODE_TRACKS).filter(query.getFullTextQuery());
    }

    /** @return the {@link Filter}, which allows to filter a list of TomahawkListItems*/
    public Filter getFilter(final String qid, final Resolver resolver, final int filterMode) {
        return new Filter() {
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                ArrayList<Result> resultList = (ArrayList<Result>) results.values;
                mStopped = true;
                mTomahawkApp.getPipeLine().reportResults(qid, resultList);
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                constraint = constraint.toString().toLowerCase();
                constraint = constraint.toString().trim();
                ArrayList<Result> filteredResults = getFilteredResults(constraint);

                FilterResults results = new FilterResults();
                synchronized (this) {
                    results.values = filteredResults;
                }

                return results;
            }

            protected ArrayList<Result> getFilteredResults(CharSequence constraint) {
                ArrayList<Result> filteredResults = new ArrayList<Result>();
                if (constraint == null || TextUtils.isEmpty(constraint.toString()))
                    return filteredResults;
                List<TomahawkBaseAdapter.TomahawkListItem> inputList = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
                switch (filterMode) {
                case DATABASERESOLVER_FILTER_MODE_TRACKS:
                    inputList.addAll(mCollection.getTracks());
                    break;
                case DATABASERESOLVER_FILTER_MODE_ARTISTS:
                    inputList.addAll(mCollection.getArtists());
                    break;
                case DATABASERESOLVER_FILTER_MODE_ALBUMS:
                    inputList.addAll(mCollection.getAlbums());
                }

                for (int i = 0; i < inputList.size(); i++) {
                    TomahawkBaseAdapter.TomahawkListItem item = inputList.get(i);
                    if (item.getName().toLowerCase().contains(constraint)
                            || (item.getArtist() != null && item.getArtist().getName().toLowerCase().contains(
                                    constraint))
                            || (item.getAlbum() != null && item.getAlbum().getName().toLowerCase().contains(constraint))) {
                        Result r;
                        switch (filterMode) {
                        case DATABASERESOLVER_FILTER_MODE_TRACKS:
                            r = new Result((Track) item);
                            r.setResolver(resolver);
                            filteredResults.add(r);
                            break;
                        case DATABASERESOLVER_FILTER_MODE_ARTISTS:
                            r = new Result((Artist) item);
                            r.setResolver(resolver);
                            filteredResults.add(r);
                            break;
                        case DATABASERESOLVER_FILTER_MODE_ALBUMS:
                            r = new Result((Album) item);
                            r.setResolver(resolver);
                            filteredResults.add(r);
                        }
                    }
                }
                return filteredResults;
            }
        };
    }
}
