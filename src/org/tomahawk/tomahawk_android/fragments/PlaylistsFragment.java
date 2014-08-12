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

import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.dialogs.CreatePlaylistDialog;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.content.Context;
import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TomahawkFragment} which shows a set of {@link org.tomahawk.libtomahawk.collection.Playlist}s
 * inside its {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class PlaylistsFragment extends TomahawkFragment {

    @Override
    public void onResume() {
        super.onResume();

        CollectionManager.getInstance().fetchPlaylists();

        updateAdapter();
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param item the TomahawkListItem which corresponds to the click
     */
    @Override
    public void onItemClick(TomahawkListItem item) {
        if (item instanceof Playlist) {
            String key = ((Playlist) item).getId();
            FragmentUtils.replace(getActivity(), getActivity().getSupportFragmentManager(),
                    PlaylistEntriesFragment.class, key, TomahawkFragment.TOMAHAWK_PLAYLIST_KEY);
        } else {
            new CreatePlaylistDialog().show(getFragmentManager(),
                    getString(R.string.playbackactivity_create_playlist_dialog_title));
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

        getActivity().setTitle(getString(R.string.playlistsfragment_title_string));

        List<TomahawkListItem> playlists = new ArrayList<TomahawkListItem>();
        playlists.addAll(CollectionManager.getInstance().getPlaylists());
        if (getListAdapter() == null) {
            TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(context,
                    layoutInflater, playlists, this);
            setListAdapter(tomahawkListAdapter);
            tomahawkListAdapter.setShowCategoryHeaders(true);
        } else {
            ((TomahawkListAdapter) getListAdapter()).setListItems(playlists);
        }
    }

    @Override
    public void onPanelCollapsed() {
        getActivity().setTitle(getString(R.string.playlistsfragment_title_string));
    }

    @Override
    public void onPanelExpanded() {
    }
}
