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
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkGridAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.content.Loader;
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
            if (!isShowGridView()) {
                item = getListAdapter().getItem(position);
            } else {
                item = getGridAdapter().getItem(position);
            }
            if (item instanceof Query) {
                PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
                if (playbackService != null && shouldShowPlaystate()
                        && playbackService.getCurrentPlaylist().getCurrentQueryIndex()
                        == mShownAlbums.size() + mShownArtists.size() + position) {
                    playbackService.playPause();
                } else {
                    UserPlaylist playlist = UserPlaylist
                            .fromQueryList(TomahawkApp.getLifetimeUniqueStringId(), "",
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
                mTomahawkApp.getContentViewer()
                        .replace(TracksFragment.class, key, TOMAHAWK_ALBUM_KEY, mIsLocal, false);
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
    protected void updateAdapter() {
        List<TomahawkBaseAdapter.TomahawkListItem> albums
                = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        List<TomahawkBaseAdapter.TomahawkListItem> topHits
                = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        if (!isShowGridView() && mArtist != null) {
            mTomahawkMainActivity.setTitle(mArtist.getName());
            if (mIsLocal) {
                albums.addAll(mArtist.getLocalAlbums());
            } else {
                albums.addAll(mArtist.getAlbums());
                topHits.addAll(mArtist.getTopHits());
                mShownQueries = mArtist.getTopHits();
            }
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(topHits);
            listArray.add(albums);
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(
                        mTomahawkMainActivity, listArray);
                tomahawkListAdapter.setShowCategoryHeaders(true, true);
                tomahawkListAdapter.setShowContentHeader(true,
                        getResources().getConfiguration().orientation
                                == Configuration.ORIENTATION_LANDSCAPE, getListView(), mArtist);
                tomahawkListAdapter.setShowResolvedBy(true);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
                ((TomahawkListAdapter) getListAdapter()).updateContentHeader(mArtist);
            }
            getListView().setOnItemClickListener(this);
        } else {
            mTomahawkMainActivity.setTitle(getString(R.string.albumsfragment_title_string));
            if (mIsLocal) {
                albums.addAll(Album.getLocalAlbums());
            } else {
                albums.addAll(Album.getAlbums());
            }
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(albums);
            if (getGridAdapter() == null) {
                TomahawkGridAdapter tomahawkGridAdapter = new TomahawkGridAdapter(getActivity(),
                        listArray);
                setGridAdapter(tomahawkGridAdapter);
            } else {
                getGridAdapter().setListArray(listArray);
            }
            getGridView().setOnItemClickListener(this);
            adaptColumnCount();
        }
    }

    /**
     * @return the {@link Artist} corresponding to this instance of {@link AlbumsFragment}
     */
    public Artist getArtist() {
        return mArtist;
    }

    @Override
    protected void onPipeLineResultsReported(ArrayList<String> queryKeys) {
        for (String key : queryKeys) {
            if (mCorrespondingQueryIds.contains(key)) {
                updateAdapter();
                break;
            }
        }
    }

    @Override
    protected void onInfoSystemResultsReported(String requestId) {
        if (mCurrentRequestIds.contains(requestId)) {
            updateAdapter();
        }
    }
}
