/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.fragments;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.database.UserPlaylistsDataSource;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;

import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TomahawkFragment} which shows a set of {@link Track}s inside its {@link
 * org.tomahawk.tomahawk_android.views.TomahawkStickyListHeadersListView}
 */
public class TracksFragment extends TomahawkFragment implements OnItemClickListener {

    private boolean mShouldShowLoadingAnimation = false;

    /**
     * Initialize
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mShouldShowLoadingAnimation) {
            mTomahawkMainActivity.startLoadingAnimation();
        }
    }

    /**
     * Called every time an item inside the {@link org.tomahawk.tomahawk_android.views.TomahawkStickyListHeadersListView}
     * is clicked
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this will be a view
     *                 provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        position -= getListView().getHeaderViewsCount();
        if (position >= 0) {
            if (getListAdapter().getItem(position) instanceof Query) {
                ArrayList<Query> queries = new ArrayList<Query>();
                if (mAlbum != null) {
                    if (mIsLocal) {
                        queries = mAlbum.getLocalQueries();
                    } else {
                        queries = mAlbum.getQueries();
                    }
                } else if (mArtist != null) {
                    if (mIsLocal) {
                        queries = mArtist.getLocalQueries();
                    } else {
                        queries = mArtist.getQueries();
                    }
                } else if (mUserPlaylist != null) {
                    queries = mUserPlaylist.getQueries();
                } else if (mUserPlaylist != null) {
                    queries = mUserPlaylist.getQueries();
                } else {
                    queries.addAll(mTomahawkMainActivity.getUserCollection().getQueries());
                }
                UserPlaylist playlist = UserPlaylist
                        .fromQueryList(UserPlaylistsDataSource.CACHED_PLAYLIST_ID,
                                UserPlaylistsDataSource.CACHED_PLAYLIST_NAME, queries,
                                queries.get(position));
                PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
                if (playbackService != null) {
                    playbackService.setCurrentPlaylist(playlist);
                    playbackService.start();
                }
                mTomahawkMainActivity.getContentViewer()
                        .setCurrentHubId(TomahawkMainActivity.HUB_ID_PLAYBACK);
            }
        }
    }

    /**
     * Called whenever the {@link UserCollection} {@link Loader} has finished
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        super.onLoadFinished(loader, coll);
        updateAdapter();
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkBaseAdapter} content
     */
    public void updateAdapter() {
        ArrayList<TomahawkBaseAdapter.TomahawkListItem> queries
                = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        TomahawkListAdapter tomahawkListAdapter;
        Collection coll = mTomahawkMainActivity.getUserCollection();
        if (mAlbum != null) {
            if (mIsLocal) {
                queries.addAll(mAlbum.getLocalQueries());
            } else {
                queries.addAll(mAlbum.getQueries());
            }
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(queries);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mTomahawkMainActivity, listArray);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true);
                tomahawkListAdapter.setShowContentHeader(true, getListView(), mAlbum);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
                ((TomahawkListAdapter) getListAdapter()).updateContentHeader(mAlbum);
            }
        } else if (mArtist != null) {
            if (mIsLocal) {
                queries.addAll(mArtist.getLocalQueries());
            } else {
                queries.addAll(mArtist.getQueries());
            }
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(queries);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mTomahawkMainActivity, listArray);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true);
                tomahawkListAdapter.setShowContentHeader(true, getListView(), mArtist);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
            }
        } else if (mUserPlaylist != null) {
            queries.addAll(mUserPlaylist.getQueries());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(queries);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mTomahawkMainActivity, listArray);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true);
                tomahawkListAdapter.setShowContentHeader(true, getListView(), mUserPlaylist);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
            }
        } else if (mUserPlaylist != null) {
            queries.addAll(mUserPlaylist.getQueries());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(queries);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mTomahawkMainActivity, listArray);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true);
                tomahawkListAdapter.setShowContentHeader(true, getListView(), mUserPlaylist);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
            }
        } else {
            queries.addAll(coll.getQueries());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(queries);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mTomahawkMainActivity, listArray);
                getListView().setAreHeadersSticky(false);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
            }
        }

        mShownQueries.clear();
        for (TomahawkBaseAdapter.TomahawkListItem item : queries) {
            mShownQueries.add((Query) item);
        }
        resolveQueriesFromTo(getListView().getFirstVisiblePosition(),
                getListView().getLastVisiblePosition() + 2);

        getListView().setOnItemClickListener(this);
    }

    /**
     * @return the {@link Album} associated with this {@link TracksFragment}
     */
    public Album getAlbum() {
        return mAlbum;
    }

    @Override
    protected void onPipeLineResultsReported(String qId) {
        if (mCorrespondingQueryIds.contains(qId)) {
            updateAdapter();
        }
    }

    @Override
    protected void onInfoSystemResultsReported(String requestId) {
        if (mCurrentRequestIds.contains(requestId)) {
            updateAdapter();
        }
    }
}
