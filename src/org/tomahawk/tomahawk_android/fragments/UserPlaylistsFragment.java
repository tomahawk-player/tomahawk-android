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

import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.dialogs.CreateUserPlaylistDialog;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TomahawkFragment} which shows a set of {@link UserPlaylist}s inside its {@link
 * se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class UserPlaylistsFragment extends TomahawkFragment implements OnItemClickListener {

    @Override
    public void onResume() {
        super.onResume();

        updateAdapter();
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
        position -= getListView().getHeaderViewsCount();
        if (position >= 0) {
            if (getListAdapter().getItem(position) instanceof UserPlaylist) {
                String key = ((UserPlaylist) getListAdapter().getItem(position)).getId();
                FragmentUtils.replace(getActivity(), getActivity().getSupportFragmentManager(),
                        TracksFragment.class, key, TomahawkFragment.TOMAHAWK_USERPLAYLIST_KEY,
                        true);
            } else {
                new CreateUserPlaylistDialog().show(getFragmentManager(),
                        getString(R.string.playbackactivity_create_playlist_dialog_title));
            }
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

        Context context = getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();

        getActivity().setTitle(getString(R.string.userplaylistsfragment_title_string));

        List<TomahawkListItem> userPlaylists = new ArrayList<TomahawkListItem>();
        userPlaylists.addAll(UserCollection.getInstance().getLocalUserPlaylists());
        userPlaylists.addAll(UserCollection.getInstance().getHatchetUserPlaylists());
        if (getListAdapter() == null) {
            TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(context,
                    layoutInflater, userPlaylists);
            tomahawkListAdapter.setShowAddButton(getListView(),
                    getString(R.string.playbackactivity_create_playlist_dialog_title));
            setListAdapter(tomahawkListAdapter);
            tomahawkListAdapter.setShowCategoryHeaders(true, false);
        } else {
            ((TomahawkListAdapter) getListAdapter()).setListItems(userPlaylists);
        }

        getListView().setOnItemClickListener(this);
    }
}
