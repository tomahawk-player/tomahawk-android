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
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TomahawkFragment} which shows a set of {@link Artist}s inside its {@link
 * se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class ArtistsFragment extends TomahawkFragment {

    public static final int SHOW_MODE_STARREDARTISTS = 1;

    @Override
    public void onResume() {
        super.onResume();

        if (getArguments() != null) {
            if (getArguments().containsKey(SHOW_MODE)) {
                mShowMode = getArguments().getInt(SHOW_MODE);
            }
        }
        if (mContainerFragmentClass == null) {
            getActivity().setTitle("");
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
    public void onItemClick(View view, TomahawkListItem item) {
        if (item instanceof Artist) {
            FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                    getActivity().getSupportFragmentManager(), ArtistPagerFragment.class,
                    item.getCacheKey(), TomahawkFragment.TOMAHAWK_ARTIST_KEY, mCollection);
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

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();

        List<TomahawkListItem> artists = new ArrayList<TomahawkListItem>();
        if (mShowMode == SHOW_MODE_STARREDARTISTS) {
            ArrayList<Artist> starredArtists = DatabaseHelper.getInstance().getStarredArtists();
            for (Artist artist : starredArtists) {
                ArrayList<String> requestIds = InfoSystem.getInstance().resolve(artist, false);
                for (String requestId : requestIds) {
                    mCurrentRequestIds.add(requestId);
                }
            }
            artists.addAll(starredArtists);
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter =
                        new TomahawkListAdapter((TomahawkMainActivity) getActivity(),
                                layoutInflater, new Segment(artists), this);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(new Segment(artists));
            }
        } else if (mSearchArtists != null) {
            ArrayList<TomahawkListItem> items = new ArrayList<TomahawkListItem>();
            items.addAll(mSearchArtists);
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter =
                        new TomahawkListAdapter((TomahawkMainActivity) getActivity(),
                                layoutInflater, new Segment(items), this);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(new Segment(items));
            }
        } else {
            artists.addAll(mCollection.getArtists());
            Segment segment = new Segment(artists);
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter =
                        new TomahawkListAdapter((TomahawkMainActivity) getActivity(),
                                layoutInflater, segment, this);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segment);
            }
        }
    }
}
