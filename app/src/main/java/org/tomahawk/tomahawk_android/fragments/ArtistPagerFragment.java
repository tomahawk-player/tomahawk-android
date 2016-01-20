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

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.utils.FragmentInfo;
import org.tomahawk.tomahawk_android.views.FancyDropDown;

import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ArtistPagerFragment extends PagerFragment {

    private static final String TAG = ArtistPagerFragment.class.getSimpleName();

    private Artist mArtist;

    private int mInitialPage = -1;

    @SuppressWarnings("unused")
    public void onEventMainThread(CollectionManager.UpdatedEvent event) {
        if (event.mUpdatedItemIds != null
                && event.mUpdatedItemIds.contains(mArtist.getCacheKey())) {
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
        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkFragment.CONTAINER_FRAGMENT_PAGE)) {
                mInitialPage = getArguments()
                        .getInt(TomahawkFragment.CONTAINER_FRAGMENT_PAGE);
            }
            if (getArguments().containsKey(TomahawkFragment.ARTIST)) {
                mArtist = Artist.getByKey(getArguments().getString(TomahawkFragment.ARTIST));
                if (mArtist == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else {
                    String requestId = InfoSystem.get().resolve(mArtist, false);
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            }
        }

        updatePager();
    }

    private void updatePager() {
        showContentHeader(mArtist);

        setupPager(getFragmentInfoLists(), mInitialPage, null, 1);
        CollectionManager.get().getAvailableCollections(mArtist)
                .done(new DoneCallback<List<Collection>>() {
                    @Override
                    public void onDone(final List<Collection> result) {
                        int initialSelection = 0;
                        for (int i = 0; i < result.size(); i++) {
                            if (result.get(i).getId().equals(
                                    getArguments().getString(TomahawkFragment.COLLECTION_ID))) {
                                initialSelection = i;
                                break;
                            }
                        }
                        getArguments().putString(TomahawkFragment.COLLECTION_ID,
                                result.get(initialSelection).getId());
                        showFancyDropDown(initialSelection, mArtist.getPrettyName().toUpperCase(),
                                FancyDropDown.convertToDropDownItemInfo(result),
                                new FancyDropDown.DropDownListener() {
                                    @Override
                                    public void onDropDownItemSelected(int position) {
                                        getArguments().putString(TomahawkFragment.COLLECTION_ID,
                                                result.get(position).getId());
                                        fillAdapter(getFragmentInfoLists(), 0, 1);
                                    }

                                    @Override
                                    public void onCancel() {
                                    }
                                });
                        setupAnimations();
                    }
                });
    }

    private List<FragmentInfoList> getFragmentInfoLists() {
        List<FragmentInfoList> fragmentInfoLists = new ArrayList<>();
        FragmentInfoList fragmentInfoList = new FragmentInfoList();
        FragmentInfo fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = AlbumsFragment.class;
        fragmentInfo.mTitle = getString(R.string.music);
        fragmentInfo.mBundle = getChildFragmentBundle();
        fragmentInfo.mBundle
                .putString(TomahawkFragment.ARTIST, mArtist.getCacheKey());
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfoLists.add(fragmentInfoList);

        fragmentInfoList = new FragmentInfoList();
        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = BiographyFragment.class;
        fragmentInfo.mTitle = getString(R.string.biography);
        fragmentInfo.mBundle = getChildFragmentBundle();
        fragmentInfo.mBundle
                .putString(TomahawkFragment.ARTIST, mArtist.getCacheKey());
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfoLists.add(fragmentInfoList);
        return fragmentInfoLists;
    }

    @Override
    protected void onInfoSystemResultsReported(InfoRequestData infoRequestData) {
        if (mCorrespondingRequestIds.contains(infoRequestData.getRequestId())) {
            showContentHeader(mArtist);
        }
    }
}
