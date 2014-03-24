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

import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.database.UserPlaylistsDataSource;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TomahawkFragment} which shows a set of {@link Track}s inside its {@link
 * se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class TracksFragment extends TomahawkFragment implements OnItemClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mAlbum != null) {
            menu.findItem(R.id.action_gotoartist_item).setVisible(true);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * If the user clicks on a menuItem, handle what should be done here
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null) {
            if (item.getItemId() == R.id.action_gotoartist_item) {
                Bundle bundle = new Bundle();
                String key = TomahawkUtils.getCacheKey(mAlbum.getArtist());
                bundle.putString(TOMAHAWK_ARTIST_KEY, key);
                mTomahawkApp.getContentViewer()
                        .replace(AlbumsFragment.class, key, TOMAHAWK_ARTIST_KEY, false, false);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called every time an item inside the {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
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
                Query query = (Query) getListAdapter().getItem(position);
                if (query.isPlayable()) {
                    ArrayList<Query> queries = new ArrayList<Query>();
                    if (mAlbum != null) {
                        queries = mAlbum.getQueries(mIsLocal);
                    } else if (mArtist != null) {
                        queries = mArtist.getQueries(mIsLocal);
                    } else if (mUserPlaylist != null) {
                        queries = mUserPlaylist.getQueries();
                    } else {
                        queries.addAll(mTomahawkMainActivity.getUserCollection().getQueries());
                    }
                    PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
                    if (playbackService != null && shouldShowPlaystate() && mQueryPositions
                            .get(playbackService.getCurrentPlaylist().getCurrentQueryIndex())
                            == position) {
                        playbackService.playPause();
                    } else {
                        UserPlaylist playlist = UserPlaylist.fromQueryList(
                                UserPlaylistsDataSource.CACHED_PLAYLIST_ID,
                                UserPlaylistsDataSource.CACHED_PLAYLIST_NAME, queries,
                                queries.get(position));
                        if (playbackService != null) {
                            playbackService.setCurrentPlaylist(playlist);
                            playbackService.start();
                        }
                    }
                }
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
        resolveVisibleQueries();
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    @Override
    protected void updateAdapter() {
        ArrayList<TomahawkListItem> queries = new ArrayList<TomahawkListItem>();
        TomahawkListAdapter tomahawkListAdapter;
        Collection coll = mTomahawkMainActivity.getUserCollection();
        if (mAlbum != null) {
            mTomahawkMainActivity.setTitle(mAlbum.getName());
            queries.addAll(mAlbum.getQueries(mIsLocal));
            List<List<TomahawkListItem>> listArray = new ArrayList<List<TomahawkListItem>>();
            listArray.add(queries);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mTomahawkMainActivity, listArray);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true, false);
                tomahawkListAdapter.showContentHeader(getListView(), mAlbum, mIsLocal);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
                ((TomahawkListAdapter) getListAdapter()).updateContentHeader(mAlbum, mIsLocal);
            }
        } else if (mArtist != null) {
            mTomahawkMainActivity.setTitle(mArtist.getName());
            queries.addAll(mArtist.getQueries(mIsLocal));
            List<List<TomahawkListItem>> listArray = new ArrayList<List<TomahawkListItem>>();
            listArray.add(queries);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mTomahawkMainActivity, listArray);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true, false);
                tomahawkListAdapter.showContentHeader(getListView(), mArtist, mIsLocal);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
            }
        } else if (mUserPlaylist != null) {
            mTomahawkMainActivity.setTitle(mUserPlaylist.getName());
            queries.addAll(mUserPlaylist.getQueries());
            List<List<TomahawkListItem>> listArray = new ArrayList<List<TomahawkListItem>>();
            listArray.add(queries);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mTomahawkMainActivity, listArray);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true, false);
                tomahawkListAdapter.showContentHeader(getListView(), mUserPlaylist, mIsLocal);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
                ((TomahawkListAdapter) getListAdapter())
                        .updateContentHeader(mUserPlaylist, mIsLocal);
            }
        } else {
            mTomahawkMainActivity.setTitle(getString(R.string.tracksfragment_title_string));
            queries.addAll(coll.getQueries());
            List<List<TomahawkListItem>> listArray = new ArrayList<List<TomahawkListItem>>();
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
        int i = 0;
        for (TomahawkListItem item : queries) {
            mShownQueries.add((Query) item);
            mQueryPositions.put(i, i);
            i++;
        }

        getListView().setOnItemClickListener(this);

        updateShowPlaystate();
    }
}
