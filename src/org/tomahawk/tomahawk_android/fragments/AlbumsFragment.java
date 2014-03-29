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
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkGridAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.Loader;
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
                Bundle bundle = new Bundle();
                String key = TomahawkUtils.getCacheKey((Album) item);
                bundle.putString(TOMAHAWK_ALBUM_KEY, key);
                activity.getContentViewer().replace(TracksFragment.class, key, TOMAHAWK_ALBUM_KEY,
                        mIsLocal, false);
            }
        }
    }

    /**
     * Called whenever the {@link UserCollection} {@link Loader} has finished
     */
    @Override
    public void onLoadFinished(Loader<UserCollection> loader, UserCollection coll) {
        super.onLoadFinished(loader, coll);
        updateAdapter();
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    @Override
    protected void updateAdapter() {
        List<TomahawkListItem> albums = new ArrayList<TomahawkListItem>();
        List<TomahawkListItem> topHits = new ArrayList<TomahawkListItem>();
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        Context context = getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View rootView = getActivity().findViewById(android.R.id.content);
        if (!isShowGridView() && mArtist != null) {
            activity.setTitle(mArtist.getName());
            if (mIsLocal) {
                albums.addAll(mArtist.getLocalAlbums());
            } else {
                albums.addAll(mArtist.getAlbums());
                topHits.addAll(mArtist.getTopHits());
                mShownQueries = mArtist.getTopHits();
                for (int i = 0; i < mShownQueries.size(); i++) {
                    mQueryPositions.put(i, i);
                }
            }
            List<List<TomahawkListItem>> listArray = new ArrayList<List<TomahawkListItem>>();
            listArray.add(topHits);
            listArray.add(albums);
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(context,
                        layoutInflater, rootView, listArray);
                tomahawkListAdapter.setShowCategoryHeaders(true, true);
                tomahawkListAdapter.showContentHeader(getListView(), mArtist, mIsLocal);
                tomahawkListAdapter.setShowResolvedBy(true);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
                ((TomahawkListAdapter) getListAdapter()).updateContentHeader(mArtist, mIsLocal);
            }
            getListView().setOnItemClickListener(this);
        } else {
            activity.setTitle(getString(R.string.albumsfragment_title_string));
            if (mIsLocal) {
                albums.addAll(Album.getLocalAlbums());
            } else {
                albums.addAll(Album.getAlbums());
            }
            List<List<TomahawkListItem>> listArray = new ArrayList<List<TomahawkListItem>>();
            listArray.add(albums);
            if (getGridAdapter() == null) {
                TomahawkGridAdapter tomahawkGridAdapter = new TomahawkGridAdapter(activity,
                        layoutInflater, listArray);
                setGridAdapter(tomahawkGridAdapter);
            } else {
                getGridAdapter().setListArray(listArray);
            }
            getGridView().setOnItemClickListener(this);
            adaptColumnCount();
        }

        updateShowPlaystate();
    }
}
