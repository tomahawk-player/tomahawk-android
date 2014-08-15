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
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.AdapterUtils;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;

/**
 * {@link TomahawkFragment} which shows a set of {@link Track}s inside its {@link
 * se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class TracksFragment extends TomahawkFragment {

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
                FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                        getActivity().getSupportFragmentManager(), AlbumsFragment.class,
                        mAlbum.getArtist().getCacheKey(), TomahawkFragment.TOMAHAWK_ARTIST_KEY,
                        mCollection);
            }
            ((TomahawkMainActivity) getActivity()).closeDrawer();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateAdapter();
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param item the TomahawkListItem which corresponds to the click
     */
    @Override
    public void onItemClick(TomahawkListItem item) {
        if (item instanceof Query) {
            Query query = (Query) item;
            if (query.isPlayable()) {
                ArrayList<Query> queries = new ArrayList<Query>();
                TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
                if (mAlbum != null) {
                    queries = AdapterUtils.getAlbumTracks(mAlbum, mCollection);
                } else if (mArtist != null) {
                    queries = AdapterUtils.getArtistTracks(mArtist, mCollection);
                } else {
                    Collection userCollection = CollectionManager.getInstance()
                            .getCollection(TomahawkApp.PLUGINNAME_USERCOLLECTION);
                    queries.addAll(userCollection.getQueries());
                }
                PlaybackService playbackService = activity.getPlaybackService();
                if (playbackService != null && playbackService.getCurrentQuery() == query) {
                    playbackService.playPause();
                } else {
                    Playlist playlist = Playlist.fromQueryList(DatabaseHelper.CACHED_PLAYLIST_NAME,
                            queries);
                    playlist.setId(DatabaseHelper.CACHED_PLAYLIST_ID);
                    if (playbackService != null) {
                        playbackService.setPlaylist(playlist, playlist.getEntryWithQuery(query));
                        playbackService.start();
                    }
                }
            }
        }
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    @Override
    protected void updateAdapter() {
        if (!mIsResumed) {
            return;
        }

        mResolveQueriesHandler.removeCallbacksAndMessages(null);
        mResolveQueriesHandler.sendEmptyMessage(RESOLVE_QUERIES_REPORTER_MSG);
        ArrayList<TomahawkListItem> queries = new ArrayList<TomahawkListItem>();
        TomahawkListAdapter tomahawkListAdapter;
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View rootView = getView();
        if (mAlbum != null) {
            activity.setTitle(mAlbum.getName());
            queries.addAll(AdapterUtils.getAlbumTracks(mAlbum, mCollection));
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(activity, layoutInflater, queries,
                        this);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true);
                tomahawkListAdapter.showContentHeader(rootView, mAlbum, mCollection);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListItems(queries);
                ((TomahawkListAdapter) getListAdapter())
                        .showContentHeader(rootView, mAlbum, mCollection);
            }
        } else if (mArtist != null) {
            activity.setTitle(mArtist.getName());
            queries.addAll(AdapterUtils.getArtistTracks(mArtist, mCollection));
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(activity, layoutInflater, queries,
                        this);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true);
                tomahawkListAdapter.showContentHeader(rootView, mArtist, mCollection);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListItems(queries);
                ((TomahawkListAdapter) getListAdapter())
                        .showContentHeader(rootView, mArtist, mCollection);
            }
        } else {
            queries.addAll(mCollection.getQueries());
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(activity, layoutInflater, queries,
                        this);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListItems(queries);
            }
        }

        mShownQueries.clear();
        for (TomahawkListItem query : queries) {
            mShownQueries.add((Query) query);
        }

        updateShowPlaystate();
    }

    @Override
    public void onPanelCollapsed() {
        if (mAlbum != null) {
            getActivity().setTitle(mAlbum.getName());
        } else if (mArtist != null) {
            getActivity().setTitle(mArtist.getName());
        }
    }

    @Override
    public void onPanelExpanded() {
    }
}
