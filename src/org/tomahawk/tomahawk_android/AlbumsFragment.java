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
package org.tomahawk.tomahawk_android;

import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.TomahawkBaseAdapter;
import org.tomahawk.libtomahawk.TomahawkGridAdapter;
import org.tomahawk.libtomahawk.TomahawkListAdapter;
import org.tomahawk.libtomahawk.UserCollection;
import org.tomahawk.libtomahawk.hatchet.AlbumsInfo;
import org.tomahawk.libtomahawk.hatchet.ArtistInfo;
import org.tomahawk.libtomahawk.hatchet.InfoRequestData;
import org.tomahawk.libtomahawk.hatchet.InfoSystem;

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
 * Fragment which represents the "Album" tabview.
 */
public class AlbumsFragment extends TomahawkFragment implements OnItemClickListener {

    private AlbumFragmentReceiver mAlbumFragmentReceiver;

    /**
     * Handles incoming {@link Collection} updated broadcasts.
     */
    private class AlbumFragmentReceiver extends BroadcastReceiver {

        /*
         * (non-Javadoc)
         *
         * @see
         * android.content.BroadcastReceiver#onReceive(android.content.Context,
         * android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(InfoSystem.INFOSYSTEM_RESULTSREPORTED)) {
                String requestId = intent
                        .getStringExtra(InfoSystem.INFOSYSTEM_RESULTSREPORTED_REQUESTID);

                if (mCurrentRequestIds.contains(requestId)) {
                    mCurrentRequestIds.remove(requestId);
                    InfoRequestData infoRequestData = mInfoSystem.getInfoRequestById(requestId);
                    if (infoRequestData.getType()
                            == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTALBUMS) {
                        if (mArtist == null) {
                            mArtist = new Artist();
                        }
                        ArrayList<Album> albums = InfoRequestData.albumInfoListToAlbumList(
                                ((AlbumsInfo) infoRequestData.mResult).getAlbums());
                        for (Album album : albums) {
                            mArtist.addAlbum(album);
                        }
                        updateAdapter();
                    } else if (infoRequestData.getType()
                            == InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTINFO) {
                        if (mArtist == null) {
                            mArtist = new Artist();
                        }
                        mArtist = InfoRequestData
                                .artistInfoToArtist((ArtistInfo) infoRequestData.mResult, mArtist);
                        updateAdapter();
                    }
                }
            }
        }
    }

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

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.Fragment#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

        if (mAlbumFragmentReceiver != null) {
            getActivity().unregisterReceiver(mAlbumFragmentReceiver);
            mAlbumFragmentReceiver = null;
        }
    }

    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);
        if (mActivity.getCollection() != null && getArguments() != null && getArguments()
                .containsKey(TOMAHAWK_ARTIST_ID)
                && getArguments().getLong(TOMAHAWK_ARTIST_ID) > 0) {
            mArtist = mActivity.getCollection()
                    .getArtistById(getArguments().getLong(TOMAHAWK_ARTIST_ID));
        } else if (getArguments().containsKey(UserCollection.USERCOLLECTION_ARTISTCACHED)) {
            mArtist = mActivity.getCollection().getCachedArtist();
            String requestId = mTomahawkApp.getUniqueInfoRequestId();
            mCurrentRequestIds.add(requestId);
            mInfoSystem.resolve(new InfoRequestData(mTomahawkApp, requestId,
                    InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTALBUMS, true,
                    mActivity.getCollection().getCachedArtist().getName()));
            requestId = mTomahawkApp.getUniqueInfoRequestId();
            mCurrentRequestIds.add(requestId);
            mInfoSystem.resolve(new InfoRequestData(mTomahawkApp, requestId,
                    InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTINFO, true,
                    mActivity.getCollection().getCachedArtist().getName()));
        }
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        idx -= mList.getHeaderViewsCount();
        if (idx >= 0) {
            if (getListAdapter().getItem(idx) instanceof Album) {
                mActivity.getCollection().setCachedAlbum((Album) getListAdapter().getItem(idx));
                mActivity.getContentViewer().
                        replace(mCorrespondingStackId, TracksFragment.class, -1,
                                UserCollection.USERCOLLECTION_ALBUMCACHED, false);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#onLoadFinished(android.support.v4.content.Loader, org.tomahawk.libtomahawk.Collection)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        super.onLoadFinished(loader, coll);
        updateAdapter();
    }

    private void updateAdapter() {
        List<TomahawkBaseAdapter.TomahawkListItem> albums
                = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        if (mArtist != null) {
            albums.addAll(mArtist.getAlbums());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(albums);
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(mActivity,
                        listArray);
                tomahawkListAdapter.setShowCategoryHeaders(true);
                tomahawkListAdapter.setShowContentHeader(true, mList, mArtist);
                tomahawkListAdapter.setShowResolvedBy(true);
                setListAdapter(tomahawkListAdapter);
            } else {
                mTomahawkBaseAdapter.setListArray(listArray);
            }
            getListView().setOnItemClickListener(this);
        } else {
            albums.addAll(mActivity.getCollection().getAlbums());
            setListAdapter(new TomahawkGridAdapter(getActivity(), R.layout.album_art_grid_item,
                    R.id.album_art_grid_image, R.id.album_art_grid_textView1,
                    R.id.album_art_grid_textView2, albums));
            getGridView().setOnItemClickListener(this);
            adaptColumnCount();
        }
    }

    public Artist getArtist() {
        return mArtist;
    }
}
