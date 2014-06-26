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
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkGridAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.AdapterUtils;
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

    public static final int SHOW_MODE_STARREDALBUMS = 1;

    @Override
    public void onResume() {
        super.onResume();

        if (getArguments() != null) {
            if (getArguments().containsKey(SHOW_MODE)) {
                mShowMode = getArguments().getInt(SHOW_MODE);
            }
        }
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
                                mShownQueries,
                                mQueryPositions.keyAt(mQueryPositions.indexOfValue(position)));
                if (playbackService != null) {
                    playbackService.setCurrentPlaylist(playlist);
                    playbackService.start();
                }
            }
        } else if (item instanceof Album) {
            FragmentUtils.replace(getActivity(), getActivity().getSupportFragmentManager(),
                    TracksFragment.class, ((Album) item).getCacheKey(),
                    TomahawkFragment.TOMAHAWK_ALBUM_KEY, mCollection);
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
        View rootView = getView();
        if (mArtist != null) {
            activity.setTitle(mArtist.getName());
            if (mCollection != null) {
                albumsAndTopHits.addAll(mCollection.getArtistAlbums(mArtist, true));
            } else {
                ArrayList<Query> topHits = AdapterUtils.getArtistTopHits(mArtist);
                albumsAndTopHits.addAll(topHits);
                albumsAndTopHits.addAll(AdapterUtils.getArtistAlbums(mArtist, null));
                mShownQueries = topHits;
                int precedingItemCount = 0;
                if (getListAdapter() != null
                        && ((TomahawkListAdapter) getListAdapter()).isShowingContentHeader()) {
                    precedingItemCount++;
                }
                mQueryPositions.clear();
                for (int i = 0; i < mShownQueries.size(); i++) {
                    mQueryPositions.put(i, i + precedingItemCount);
                }
            }
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(context,
                        layoutInflater, albumsAndTopHits);
                tomahawkListAdapter
                        .setShowCategoryHeaders(true,
                                TomahawkListAdapter.SHOW_QUERIES_AS_TOPHITS);
                tomahawkListAdapter.showContentHeader(rootView, mArtist, mCollection);
                tomahawkListAdapter.setShowResolvedBy(true);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListItems(albumsAndTopHits);
                ((TomahawkListAdapter) getListAdapter())
                        .showContentHeader(rootView, mArtist, mCollection);
            }
            getListView().setOnItemClickListener(this);
        } else if (mShowMode == SHOW_MODE_STARREDALBUMS) {
            ArrayList<Album> albums = DatabaseHelper.getInstance().getStarredAlbums();
            for (Album album : albums) {
                mCurrentRequestIds.add(InfoSystem.getInstance().resolve(album));
            }
            albumsAndTopHits.addAll(albums);
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(context,
                        layoutInflater, albumsAndTopHits);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListItems(albumsAndTopHits);
            }
            getListView().setOnItemClickListener(this);
        } else {
            albumsAndTopHits.addAll(mCollection.getAlbums());
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
