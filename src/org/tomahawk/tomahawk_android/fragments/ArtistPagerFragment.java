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
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.utils.FragmentInfo;
import org.tomahawk.tomahawk_android.views.FancyDropDown;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ArtistPagerFragment extends PagerFragment {

    private Artist mArtist;

    @SuppressWarnings("unused")
    public void onEventMainThread(CollectionManager.UpdatedEvent event) {
        if (mArtist != null && mArtist == event.mUpdatedItem) {
            updatePager();
        }
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
                    ArrayList<String> requestIds = InfoSystem.getInstance().resolve(mArtist, true);
                    for (String requestId : requestIds) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            }
        }

        showContentHeader(mArtist);
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
        fragmentInfo.mBundle = getChildFragmentBundle();
        fragmentInfo.mBundle.putString(TomahawkFragment.TOMAHAWK_ARTIST_KEY, mArtist.getCacheKey());
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfoLists.add(fragmentInfoList);

        fragmentInfoList = new FragmentInfoList();
        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = BiographyFragment.class;
        fragmentInfo.mTitle = getString(R.string.biography);
        fragmentInfo.mBundle = getChildFragmentBundle();
        fragmentInfo.mBundle.putString(TomahawkFragment.TOMAHAWK_ARTIST_KEY, mArtist.getCacheKey());
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfoLists.add(fragmentInfoList);

        setupPager(fragmentInfoLists, initialPage, null);
    }

    @Override
    protected void onInfoSystemResultsReported(InfoRequestData infoRequestData) {
        showContentHeader(mArtist);
    }
}
