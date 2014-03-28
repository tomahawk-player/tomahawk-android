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

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.TomahawkMenuAdapter;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * {@link TomahawkListFragment} which shows a simple listview menu to the user, so that he can
 * choose between a {@link TracksFragment}, an {@link AlbumsFragment} and an {@link
 * ArtistsFragment}, which display the {@link org.tomahawk.libtomahawk.collection.UserCollection}'s
 * content to the user.
 */
public class UserCollectionFragment extends TomahawkListFragment implements OnItemClickListener {

    /**
     * Called, when this {@link UserCollectionFragment}'s {@link View} has been created
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTomahawkMainActivity.setTitle(getString(R.string.usercollectionfragment_title_string));

        TomahawkMenuAdapter tomahawkMenuAdapter = new TomahawkMenuAdapter(getActivity(),
                getResources().getStringArray(R.array.local_collection_menu_items),
                getResources().obtainTypedArray(R.array.local_collection_menu_items_icons),
                getResources().obtainTypedArray(R.array.local_collection_menu_items_colors));
        setListAdapter(tomahawkMenuAdapter);
        getListView().setOnItemClickListener(this);
    }

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
        switch ((int) id) {
            case 0:
                mTomahawkMainActivity.getContentViewer()
                        .replace(TracksFragment.class, "", null, true, false);
                mTomahawkMainActivity.setTitle(getString(R.string.tracksfragment_title_string));
                break;
            case 1:
                mTomahawkMainActivity.getContentViewer()
                        .replace(AlbumsFragment.class, "", null, true, false);
                mTomahawkMainActivity.setTitle(getString(R.string.albumsfragment_title_string));
                break;
            case 2:
                mTomahawkMainActivity.getContentViewer()
                        .replace(ArtistsFragment.class, "", null, true, false);
                mTomahawkMainActivity.setTitle(getString(R.string.artistsfragment_title_string));
                break;
        }
    }
}
