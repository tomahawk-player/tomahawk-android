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
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkGridAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TomahawkFragment} which shows a set of {@link Album}s inside its {@link
 * se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class AlbumsFragment extends TomahawkFragment implements OnItemClickListener {

    @Override
    public void onResume() {
        super.onResume();

        updateAdapter();
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
        if (getListView() != null) {
            position -= getListView().getHeaderViewsCount();
        }
        if (position >= 0) {
            Object item;
            TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
            if (!isShowGridView()) {
                item = getListAdapter().getItem(position);
            } else {
                item = getGridAdapter().getItem(position);
            }
            if (item instanceof Query && ((Query) item).isPlayable()) {
                PlaybackService playbackService = activity.getPlaybackService();
                if (playbackService != null && shouldShowPlaystate() && mQueryPositions.get(
                        playbackService.getCurrentPlaylist().getCurrentQueryIndex())
                        == position) {
                    playbackService.playPause();
                } else {
                    UserPlaylist playlist = UserPlaylist
                            .fromQueryList(TomahawkMainActivity.getLifetimeUniqueStringId(), "",
                                    mShownQueries, mShownQueries.get(position));
                    if (playbackService != null) {
                        playbackService.setCurrentPlaylist(playlist);
                        playbackService.start();
                    }
                }
            } else if (item instanceof Album) {
                FragmentUtils.replace(getActivity(), getActivity().getSupportFragmentManager(),
                        TracksFragment.class, ((Album) item).getCacheKey(),
                        TomahawkFragment.TOMAHAWK_ALBUM_KEY, mIsLocal);
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

        List<TomahawkListItem> albumsAndTopHits = new ArrayList<TomahawkListItem>();
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        Context context = getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View rootView = getActivity().findViewById(android.R.id.content);
        if (!isShowGridView() && mArtist != null) {
            activity.setTitle(mArtist.getName());
            if (mIsLocal) {
                albumsAndTopHits.addAll(mArtist.getLocalAlbums());
            } else {
                albumsAndTopHits.addAll(mArtist.getTopHits());
                albumsAndTopHits.addAll(mArtist.getAlbums());
                mShownQueries = mArtist.getTopHits();
                for (int i = 0; i < mShownQueries.size(); i++) {
                    mQueryPositions.put(i, i);
                }
            }
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(context,
                        layoutInflater, albumsAndTopHits);
                tomahawkListAdapter.setShowCategoryHeaders(true, true);
                tomahawkListAdapter.showContentHeader(rootView, getListView(), mArtist, mIsLocal);
                tomahawkListAdapter.setShowResolvedBy(true);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListItems(albumsAndTopHits);
                ((TomahawkListAdapter) getListAdapter()).showContentHeader(rootView, getListView(),
                        mArtist, mIsLocal);
            }
            getListView().setOnItemClickListener(this);
        } else {
            activity.setTitle(getString(R.string.albumsfragment_title_string));
            if (mIsLocal) {
                albumsAndTopHits.addAll(Album.getLocalAlbums());
            } else {
                albumsAndTopHits.addAll(Album.getAlbums());
            }
            if (getGridAdapter() == null) {
                TomahawkGridAdapter tomahawkGridAdapter = new TomahawkGridAdapter(activity,
                        layoutInflater, albumsAndTopHits);
                setGridAdapter(tomahawkGridAdapter);
            } else {
                getGridAdapter().setListArray(albumsAndTopHits);
            }
            getGridView().setOnItemClickListener(this);
            adaptColumnCount();
        }

        updateShowPlaystate();
    }
}
