/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.utils.FragmentInfo;
import org.tomahawk.tomahawk_android.views.FancyDropDown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ArtistPagerFragment extends PagerFragment {

    private Artist mArtist;

    private ArtistsPagerFragmentReceiver mArtistsPagerFragmentReceiver;

    /**
     * Handles incoming broadcasts.
     */
    private class ArtistsPagerFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (CollectionManager.COLLECTION_UPDATED.equals(intent.getAction())) {
                if (intent.getStringExtra(TomahawkFragment.TOMAHAWK_ARTIST_KEY) != null) {
                    if (mArtist != null
                            && intent.getStringExtra(TomahawkFragment.TOMAHAWK_ARTIST_KEY).equals(
                            mArtist.getCacheKey())) {
                        updatePager();
                    }
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHasScrollableHeader = true;
        mStaticHeaderHeight = getResources()
                .getDimensionPixelSize(R.dimen.header_clear_space_nonscrollable_static);
    }

    /**
     * Called, when this {@link org.tomahawk.tomahawk_android.fragments.ArtistPagerFragment}'s
     * {@link android.view.View} has been created
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().setTitle("");
        updatePager();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Initialize and register Receiver
        if (mArtistsPagerFragmentReceiver == null) {
            mArtistsPagerFragmentReceiver = new ArtistsPagerFragmentReceiver();
            IntentFilter intentFilter = new IntentFilter(CollectionManager.COLLECTION_UPDATED);
            getActivity().registerReceiver(mArtistsPagerFragmentReceiver, intentFilter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mArtistsPagerFragmentReceiver != null) {
            getActivity().unregisterReceiver(mArtistsPagerFragmentReceiver);
            mArtistsPagerFragmentReceiver = null;
        }
    }

    private void updatePager() {
        int initialPage = -1;
        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkFragment.CONTAINER_FRAGMENT_PAGE)) {
                initialPage = getArguments().getInt(TomahawkFragment.CONTAINER_FRAGMENT_PAGE);
            }
            if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_ARTIST_KEY) && !TextUtils
                    .isEmpty(getArguments().getString(TomahawkFragment.TOMAHAWK_ARTIST_KEY))) {
                mArtist = Artist.getArtistByKey(
                        getArguments().getString(TomahawkFragment.TOMAHAWK_ARTIST_KEY));
                if (mArtist == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else {
                    ArrayList<String> requestIds = InfoSystem.getInstance().resolve(mArtist, false);
                    for (String requestId : requestIds) {
                        mCurrentRequestIds.add(requestId);
                    }
                }
            }
        }

        showContentHeader(mArtist, R.dimen.header_clear_space_nonscrollable_static, null);
        final List<Collection> collections =
                CollectionManager.getInstance().getAvailableCollections(mArtist);
        int initialSelection = 0;
        for (int i = 0; i < collections.size(); i++) {
            if (collections.get(i).getId().equals(
                    getArguments().getString(CollectionManager.COLLECTION_ID))) {
                initialSelection = i;
                break;
            }
        }
        showFancyDropDown(mArtist, initialSelection,
                FancyDropDown.convertToDropDownItemInfo(collections),
                new FancyDropDown.DropDownListener() {
                    @Override
                    public void onDropDownItemSelected(int position) {
                        getArguments().putString(CollectionManager.COLLECTION_ID,
                                collections.get(position).getId());
                        updatePager();
                    }

                    @Override
                    public void onCancel() {
                    }
                });
        List<FragmentInfoList> fragmentInfoLists = new ArrayList<FragmentInfoList>();
        FragmentInfoList fragmentInfoList = new FragmentInfoList();
        FragmentInfo fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = AlbumsFragment.class;
        fragmentInfo.mTitle = getString(R.string.music);
        Bundle bundle = new Bundle();
        bundle.putString(TomahawkFragment.TOMAHAWK_ARTIST_KEY, mArtist.getCacheKey());
        if (getArguments().containsKey(CollectionManager.COLLECTION_ID)) {
            bundle.putString(CollectionManager.COLLECTION_ID,
                    getArguments().getString(CollectionManager.COLLECTION_ID));
        }
        fragmentInfo.mBundle = bundle;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfoLists.add(fragmentInfoList);

        fragmentInfoList = new FragmentInfoList();
        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = BiographyFragment.class;
        fragmentInfo.mTitle = getString(R.string.biography);
        bundle = new Bundle();
        bundle.putString(TomahawkFragment.TOMAHAWK_ARTIST_KEY, mArtist.getCacheKey());
        fragmentInfo.mBundle = bundle;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfoLists.add(fragmentInfoList);

        setupPager(fragmentInfoLists, initialPage, null);
    }

    @Override
    protected void onPipeLineResultsReported(String key) {
    }

    @Override
    protected void onInfoSystemResultsReported(String requestId) {
        showContentHeader(mArtist, R.dimen.header_clear_space_nonscrollable_static, null);
    }
}
