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
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.TomahawkListItemComparator;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link TomahawkFragment} which shows a set of {@link Artist}s inside its {@link
 * se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class ArtistsFragment extends TomahawkFragment {

    public static final String COLLECTION_ARTISTS_SPINNER_POSITION
            = "org.tomahawk.tomahawk_android.collection_artists_spinner_position";

    public static final int SHOW_MODE_STARREDARTISTS = 1;

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
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param view the clicked view
     * @param item the TomahawkListItem which corresponds to the click
     */
    @Override
    public void onItemClick(View view, TomahawkListItem item) {
        if (item instanceof Artist) {
            Bundle bundle = new Bundle();
            bundle.putString(TomahawkFragment.TOMAHAWK_ARTIST_KEY, item.getCacheKey());
            if (mCollection != null
                    && mCollection.getArtistAlbums((Artist) item, false).size() > 0) {
                bundle.putString(CollectionManager.COLLECTION_ID, mCollection.getId());
            } else {
                bundle.putString(CollectionManager.COLLECTION_ID, TomahawkApp.PLUGINNAME_HATCHET);
            }
            FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                    getActivity().getSupportFragmentManager(), ArtistPagerFragment.class, bundle);
        }
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter}
     * content
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
                getListAdapter().setSegments(new Segment(artists), getListView());
            }
        } else if (mSearchArtists != null) {
            artists.addAll(mSearchArtists);
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter =
                        new TomahawkListAdapter((TomahawkMainActivity) getActivity(),
                                layoutInflater, new Segment(artists), this);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(new Segment(artists), getListView());
            }
        } else {
            artists.addAll(CollectionManager.getInstance()
                    .getCollection(TomahawkApp.PLUGINNAME_USERCOLLECTION).getArtists());
            for (Artist artist : DatabaseHelper.getInstance().getStarredArtists()) {
                if (!artists.contains(artist)) {
                    artists.add(artist);
                }
            }
            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext());
            List<Integer> dropDownItems = new ArrayList<Integer>();
            dropDownItems.add(R.string.collection_dropdown_recently_added);
            dropDownItems.add(R.string.collection_dropdown_alpha);
            AdapterView.OnItemSelectedListener spinnerClickListener
                    = new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position,
                        long id) {
                    SharedPreferences preferences =
                            PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext());
                    int initialPos = preferences.getInt(COLLECTION_ARTISTS_SPINNER_POSITION, 0);
                    if (initialPos != position) {
                        preferences.edit().putInt(COLLECTION_ARTISTS_SPINNER_POSITION, position)
                                .commit();
                        updateAdapter();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            };
            int initialPos = preferences.getInt(COLLECTION_ARTISTS_SPINNER_POSITION, 0);
            if (initialPos == 0) {
                Collection userColl = CollectionManager.getInstance().getCollection(
                        TomahawkApp.PLUGINNAME_USERCOLLECTION);
                Collections.sort(artists, new TomahawkListItemComparator(
                        TomahawkListItemComparator.COMPARE_RECENTLY_ADDED,
                        userColl.getAddedTimeStamps()));
            } else if (initialPos == 1) {
                Collections.sort(artists, new TomahawkListItemComparator(
                        TomahawkListItemComparator.COMPARE_ALPHA));
            }
            List<Segment> segments = new ArrayList<Segment>();
            segments.add(new Segment(initialPos, dropDownItems, spinnerClickListener, artists,
                    R.integer.grid_column_count, R.dimen.padding_superlarge,
                    R.dimen.padding_superlarge));
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter =
                        new TomahawkListAdapter((TomahawkMainActivity) getActivity(),
                                layoutInflater, segments, this);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segments, getListView());
            }
        }
        forceAutoResolve();
    }
}
