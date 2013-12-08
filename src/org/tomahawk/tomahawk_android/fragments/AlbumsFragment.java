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
import org.tomahawk.libtomahawk.hatchet.InfoRequestData;
import org.tomahawk.libtomahawk.hatchet.InfoSystem;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkGridAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TomahawkFragment} which shows a set of {@link Album}s inside its {@link
 * org.tomahawk.tomahawk_android.views.TomahawkStickyListHeadersListView}
 */
public class AlbumsFragment extends TomahawkFragment implements OnItemClickListener {

    private AlbumFragmentReceiver mAlbumFragmentReceiver;

    /**
     * Handles incoming broadcasts.
     */
    private class AlbumFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (InfoSystem.INFOSYSTEM_RESULTSREPORTED.equals(intent.getAction())) {
                String requestId = intent
                        .getStringExtra(InfoSystem.INFOSYSTEM_RESULTSREPORTED_REQUESTID);

                /*if (mCurrentRequestIds.contains(requestId)) {
                    mCurrentRequestIds.remove(requestId);
                    InfoRequestData infoRequestData = mInfoSystem.getInfoRequestById(requestId);
                    if (infoRequestData.getType()
                            == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTALBUMS) {
                        if (mArtist == null) {
                            mArtist = new Artist(TomahawkApp.getUniqueId());
                        }
                        ArrayList<Album> albums = InfoRequestData.albumInfoListToAlbumList(
                                ((AlbumsInfo) infoRequestData.mResult).getAlbums());
                        mArtist.clearAlbums();
                        for (Album album : albums) {
                            mArtist.addAlbum(album);
                        }
                        updateAdapter();
                    } else if (infoRequestData.getType()
                            == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTINFO) {
                        if (mArtist == null) {
                            mArtist = new Artist(TomahawkApp.getUniqueId());
                        }
                        mArtist = InfoRequestData
                                .artistInfoToArtist((ArtistInfo) infoRequestData.mResult,
                                        mArtist);
                        updateAdapter();
                    }
                }*/
            }
        }
    }

    /**
     * Pulls all the necessary information from the {@link Bundle}s that are being sent, when this
     * {@link AlbumsFragment} is created. We can access the information through getArguments().
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut
     *                           down then this {@link Bundle} contains the data it most recently
     *                           supplied in onSaveInstanceState({@link Bundle}). Note: Otherwise it
     *                           is null.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mTomahawkMainActivity.getUserCollection() != null && getArguments() != null
                && getArguments().containsKey(TOMAHAWK_ARTIST_KEY)
                && !TextUtils.isEmpty(getArguments().getString(TOMAHAWK_ARTIST_KEY))) {
            mArtist = mTomahawkMainActivity.getUserCollection()
                    .getArtistByKey(getArguments().getString(TOMAHAWK_ARTIST_KEY));
        }
    }

    /**
     * Initialize and register {@link AlbumFragmentReceiver}
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mAlbumFragmentReceiver == null) {
            mAlbumFragmentReceiver = new AlbumFragmentReceiver();
            IntentFilter intentFilter = new IntentFilter(Collection.COLLECTION_UPDATED);
            getActivity().registerReceiver(mAlbumFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(InfoSystem.INFOSYSTEM_RESULTSREPORTED);
            getActivity().registerReceiver(mAlbumFragmentReceiver, intentFilter);
        }
    }

    /**
     * Unregister {@link AlbumFragmentReceiver} and delete reference
     */
    @Override
    public void onPause() {
        super.onPause();

        if (mAlbumFragmentReceiver != null) {
            getActivity().unregisterReceiver(mAlbumFragmentReceiver);
            mAlbumFragmentReceiver = null;
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
            Object item;
            if (mArtist != null) {
                item = getListAdapter().getItem(position);
            } else {
                item = getGridAdapter().getItem(position);
            }
            if (item instanceof Album) {
                Bundle bundle = new Bundle();
                String key = TomahawkUtils.getCacheKey((Album) item);
                bundle.putString(TOMAHAWK_ALBUM_KEY, key);
                mTomahawkMainActivity.getContentViewer().replace(mCorrespondingHubId,
                        TracksFragment.class, key, TOMAHAWK_ALBUM_KEY, false);
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
    private void updateAdapter() {
        List<TomahawkBaseAdapter.TomahawkListItem> albums
                = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        if (mArtist != null) {
            albums.addAll(mArtist.getAlbums());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(albums);
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(
                        mTomahawkMainActivity, listArray);
                tomahawkListAdapter.setShowCategoryHeaders(true);
                tomahawkListAdapter.setShowContentHeader(true, getListView(), mArtist);
                tomahawkListAdapter.setShowResolvedBy(true);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
            }
            getListView().setOnItemClickListener(this);
        } else {
            albums.addAll(mTomahawkMainActivity.getUserCollection().getAlbums());
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
}
