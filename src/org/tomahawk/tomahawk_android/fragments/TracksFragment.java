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
import org.tomahawk.libtomahawk.hatchet.InfoSystem;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

    boolean mShouldShowLoadingAnimation = false;

    private TracksFragmentReceiver mTracksFragmentReceiver;

    /**
     * Handles incoming broadcasts.
     */
    private class TracksFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (PipeLine.PIPELINE_RESULTSREPORTED_NON_FULLTEXTQUERY.equals(intent.getAction())) {
                String queryId = intent.getStringExtra(PipeLine.PIPELINE_RESULTSREPORTED_QID);
                if (mCorrespondingQueryIds.contains(queryId)) {
                    /*ArrayList<Track> tracks = mPipeline.getQuery(queryId).getTrackResults();
                    if (tracks != null && tracks.size() > 0) {
                        Track track = mCorrespondingQueryIds.get(queryId);
                        if (track.getScore() < tracks.get(0).getScore()) {
                            Query.trackResultToTrack(tracks.get(0), track);
                            updateAdapter();
                        }
                    }*/
                }
                if (InfoSystem.INFOSYSTEM_RESULTSREPORTED.equals(intent.getAction())) {
                    String requestId = intent
                            .getStringExtra(InfoSystem.INFOSYSTEM_RESULTSREPORTED_REQUESTID);
                    if (mCurrentRequestIds.contains(requestId)) {
                    }
                }
            }
        }
    }

    /**
     * Pulls all the necessary information from the {@link Bundle}s that are being sent, when this
     * {@link TracksFragment} is created. We can access the information through getArguments().
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut
     *                           down then this {@link Bundle} contains the data it most recently
     *                           supplied in onSaveInstanceState({@link Bundle}). Note: Otherwise it
     *                           is null.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mTomahawkMainActivity.getUserCollection() != null && getArguments() != null) {
            if (getArguments().containsKey(TOMAHAWK_ALBUM_KEY)
                    && getArguments().getLong(TOMAHAWK_ALBUM_KEY) >= 0) {
                mAlbum = mTomahawkMainActivity.getUserCollection()
                        .getAlbumByKey(getArguments().getString(TOMAHAWK_ALBUM_KEY));
            } else if (getArguments().containsKey(TOMAHAWK_PLAYLIST_ID)
                    && getArguments().getLong(TOMAHAWK_PLAYLIST_ID) >= 0) {
                mUserPlaylist = mTomahawkMainActivity.getUserCollection()
                        .getCustomPlaylistByKey(
                                String.valueOf(getArguments().getLong(TOMAHAWK_PLAYLIST_ID)));
            } else if (getArguments().containsKey(UserCollection.USERCOLLECTION_ALBUMCACHED)) {
                // A cached album has been given. So we try to resolve it.
                mAlbum = mTomahawkMainActivity.getUserCollection().getCachedAlbum();
                mPipeline.resolve(mAlbum);
                mTomahawkMainActivity.startLoadingAnimation();
                mShouldShowLoadingAnimation = true;
            }
        }
    }

    /**
     * Initialize and register {@link org.tomahawk.tomahawk_android.fragments.TracksFragment.TracksFragmentReceiver}
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mTracksFragmentReceiver == null) {
            mTracksFragmentReceiver = new TracksFragmentReceiver();
            IntentFilter intentFilter = new IntentFilter(Collection.COLLECTION_UPDATED);
            getActivity().registerReceiver(mTracksFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(InfoSystem.INFOSYSTEM_RESULTSREPORTED);
            getActivity().registerReceiver(mTracksFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(
                    PipeLine.PIPELINE_RESULTSREPORTED_NON_FULLTEXTQUERY);
            getActivity().registerReceiver(mTracksFragmentReceiver, intentFilter);
        }
        if (mShouldShowLoadingAnimation) {
            mTomahawkMainActivity.startLoadingAnimation();
        }
    }

    /**
     * Unregister {@link org.tomahawk.tomahawk_android.fragments.TracksFragment.TracksFragmentReceiver}
     * and delete reference
     */
    @Override
    public void onPause() {
        super.onPause();

        if (mTracksFragmentReceiver != null) {
            getActivity().unregisterReceiver(mTracksFragmentReceiver);
            mTracksFragmentReceiver = null;
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
                    queries = mAlbum.getQueries();
                } else if (mArtist != null) {
                    queries = mArtist.getQueries();
                } else if (mUserPlaylist != null) {
                    queries = mUserPlaylist.getQueries();
                } else {
                    queries.addAll(mTomahawkMainActivity.getUserCollection().getQueries());
                }
                UserPlaylist playlist = UserPlaylist.fromQueryList(
                        UserPlaylistsDataSource.CACHED_PLAYLIST_NAME, queries,
                        position);
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
        List<TomahawkBaseAdapter.TomahawkListItem> items
                = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        TomahawkListAdapter tomahawkListAdapter;
        Collection coll = mTomahawkMainActivity.getUserCollection();
        if (mAlbum != null) {
            items.addAll(mAlbum.getQueries());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(items);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mTomahawkMainActivity, listArray);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true);
                tomahawkListAdapter.setShowContentHeader(true, getListView(), mAlbum);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
            }
        } else if (mArtist != null) {
            items.addAll(mArtist.getQueries());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(items);
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
            items.addAll(mUserPlaylist.getQueries());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(items);
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
            items.addAll(coll.getQueries());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(items);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mTomahawkMainActivity, listArray);
                getListView().setAreHeadersSticky(false);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
            }
        }

        getListView().setOnItemClickListener(this);
    }

    /**
     * @return the {@link Album} associated with this {@link TracksFragment}
     */
    public Album getAlbum() {
        return mAlbum;
    }
}
