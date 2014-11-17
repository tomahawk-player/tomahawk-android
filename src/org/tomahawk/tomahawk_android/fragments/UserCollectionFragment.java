/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserCollectionFragment extends TomahawkFragment {

    public static final String USER_COLLECTION_SPINNER_POSITION
            = "org.tomahawk.tomahawk_android.user_collection_spinner_position";

    @Override
    public void onResume() {
        super.onResume();

        updateAdapter();
        if (mUser == null) {
            getActivity().setTitle(getString(R.string.drawer_title_collection).toUpperCase());
        } else {
            mCurrentRequestIds.add(InfoSystem.getInstance().resolveStarredAlbums(mUser));
        }

        if (!mDontShowHeader) {
            showContentHeader(R.drawable.collection_header,
                    R.dimen.header_clear_space_nonscrollable_static);
        }
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param view the clicked view
     * @param item the TomahawkListItem which corresponds to the click
     */
    @Override
    public void onItemClick(View view, TomahawkListItem item) {
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        if (item instanceof Album) {
            Collection userCollection = CollectionManager.getInstance()
                    .getCollection(TomahawkApp.PLUGINNAME_USERCOLLECTION);
            if (userCollection.getAlbumTracks((Album) item, false).size() > 0) {
                FragmentUtils.replace(activity, getActivity().getSupportFragmentManager(),
                        TracksFragment.class, item.getCacheKey(),
                        TomahawkFragment.TOMAHAWK_ALBUM_KEY, userCollection);
            } else {
                FragmentUtils.replace(activity, getActivity().getSupportFragmentManager(),
                        TracksFragment.class, item.getCacheKey(),
                        TomahawkFragment.TOMAHAWK_ALBUM_KEY, CollectionManager.getInstance()
                                .getCollection(TomahawkApp.PLUGINNAME_HATCHET));
            }
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

        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        List<Segment> segments = new ArrayList<Segment>();
        ArrayList<TomahawkListItem> items = new ArrayList<TomahawkListItem>();
        if (mUser != null) {
            items.addAll(mUser.getStarredAlbums());
        } else {
            items.addAll(CollectionManager.getInstance()
                    .getCollection(TomahawkApp.PLUGINNAME_USERCOLLECTION).getAlbums());
            for (Album album : DatabaseHelper.getInstance().getStarredAlbums()) {
                if (!items.contains(album)) {
                    items.add(album);
                }
            }
        }

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext());
        List<Integer> dropDownItems = new ArrayList<Integer>();
        dropDownItems.add(R.string.collection_dropdown_recently_added);
        dropDownItems.add(R.string.collection_dropdown_alpha);
        dropDownItems.add(R.string.collection_dropdown_alpha_artists);
        AdapterView.OnItemSelectedListener spinnerClickListener
                = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences preferences =
                        PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext());
                int initialPos = preferences.getInt(USER_COLLECTION_SPINNER_POSITION, 0);
                if (initialPos != position) {
                    preferences.edit().putInt(USER_COLLECTION_SPINNER_POSITION, position).commit();
                    updateAdapter();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        int initialPos = preferences.getInt(USER_COLLECTION_SPINNER_POSITION, 0);
        if (initialPos == 0) {
            Collection userColl = CollectionManager.getInstance().getCollection(
                    TomahawkApp.PLUGINNAME_USERCOLLECTION);
            Collections.sort(items, new TomahawkListItemComparator(
                    TomahawkListItemComparator.COMPARE_RECENTLY_ADDED,
                    userColl.getAddedTimeStamps()));
        } else if (initialPos == 1) {
            Collections.sort(items, new TomahawkListItemComparator(
                    TomahawkListItemComparator.COMPARE_ALPHA));
        } else if (initialPos == 2) {
            Collections.sort(items, new TomahawkListItemComparator(
                    TomahawkListItemComparator.COMPARE_ARTIST_ALPHA));
        }
        segments.add(new Segment(initialPos, dropDownItems, spinnerClickListener, items,
                R.integer.grid_column_count, R.dimen.padding_superlarge,
                R.dimen.padding_superlarge));
        if (getListAdapter() == null) {
            TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(activity,
                    layoutInflater, segments, this);
            if (!mDontShowHeader) {
                int actionBarHeight = getResources().getDimensionPixelSize(
                        R.dimen.abc_action_bar_default_height_material);
                int headerHeight = getResources().getDimensionPixelSize(
                        R.dimen.header_clear_space_nonscrollable_static);
                tomahawkListAdapter.setShowContentHeaderSpacer(headerHeight - actionBarHeight,
                        getListView());
            }
            setListAdapter(tomahawkListAdapter);
        } else {
            getListAdapter().setSegments(segments, getListView());
        }
        if (!getResources().getBoolean(R.bool.is_landscape)) {
            getListView().setAreHeadersSticky(true);
        }
        forceAutoResolve();
    }
}
