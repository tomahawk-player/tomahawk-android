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

import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;

import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment} which shows a set of {@link
 * org.tomahawk.libtomahawk.collection.Artist}s inside its {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class CloudCollectionFragment extends TomahawkFragment {

    @Override
    public void onResume() {
        super.onResume();

        if (mCollection != null) {
            getActivity().setTitle(mCollection.getName());
        }

        updateAdapter();
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param view the clicked view
     * @param item the TomahawkListItem which corresponds to the click
     */
    @Override
    public void onItemClick(View view, Object item) {
        if (item instanceof Artist) {
            Bundle bundle = new Bundle();
            bundle.putString(TomahawkFragment.ARTIST, ((Artist) item).getCacheKey());
            bundle.putString(TomahawkFragment.COLLECTION_ID, mCollection.getId());
            bundle.putInt(CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_DYNAMIC_PAGER);
            bundle.putLong(CONTAINER_FRAGMENT_ID,
                    TomahawkMainActivity.getSessionUniqueId());
            FragmentUtils.replace((TomahawkMainActivity) getActivity(), ArtistPagerFragment.class,
                    bundle);
        }
    }

    /**
     * Update this {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment}'s {@link
     * org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter} content
     */
    @Override
    protected void updateAdapter() {
        if (!mIsResumed) {
            return;
        }

        if (mCollection != null) {
            List artists = new ArrayList<Object>();
            artists.addAll(mCollection.getArtists());
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter =
                        new TomahawkListAdapter((TomahawkMainActivity) getActivity(),
                                getActivity().getLayoutInflater(), new Segment(artists), this);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(new Segment(artists), getListView());
            }
            showContentHeader(R.drawable.collection_header);
        }

        onUpdateAdapterFinished();
    }
}
