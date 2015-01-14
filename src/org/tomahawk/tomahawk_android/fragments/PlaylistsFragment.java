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

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.dialogs.CreatePlaylistDialog;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * {@link TomahawkFragment} which shows a set of {@link org.tomahawk.libtomahawk.collection.Playlist}s
 * inside its {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class PlaylistsFragment extends TomahawkFragment {

    private HashSet<User> mResolvingUsers = new HashSet<>();

    @SuppressWarnings("unused")
    public void onEventMainThread(CollectionManager.PlaylistsUpdatedEvent event) {
        updateAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();

        CollectionManager.getInstance().fetchPlaylists();

        if (mContainerFragmentClass == null) {
            getActivity().setTitle(getString(R.string.drawer_title_playlists).toUpperCase());
        }
        updateAdapter();
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param view the clicked view
     * @param item the Object which corresponds to the click
     */
    @Override
    public void onItemClick(View view, Object item) {
        if (item instanceof Playlist) {
            Bundle bundle = new Bundle();
            bundle.putString(TomahawkFragment.PLAYLIST, ((Playlist) item).getId());
            if (mUser != null) {
                bundle.putString(TomahawkFragment.USER, mUser.getId());
            }
            bundle.putInt(CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_DYNAMIC);
            FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                    PlaylistEntriesFragment.class, bundle);
        } else {
            new CreatePlaylistDialog().show(getFragmentManager(),
                    getString(R.string.create_playlist));
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

        List playlists = new ArrayList();
        HatchetAuthenticatorUtils authenticatorUtils
                = (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        Segment segment;
        if (mUser != null && mUser != authenticatorUtils.getLoggedInUser()) {
            if (mUser.getPlaylists().size() == 0) {
                if (!mResolvingUsers.contains(mUser)) {
                    mCorrespondingRequestIds.add(InfoSystem.getInstance().resolvePlaylists(mUser));
                    mResolvingUsers.add(mUser);
                }
            } else {
                playlists.addAll(mUser.getPlaylists());
            }
            segment = new Segment(playlists);
        } else {
            playlists.addAll(CollectionManager.getInstance().getPlaylists());
            segment = new Segment(playlists, R.integer.grid_column_count,
                    R.dimen.padding_superlarge, R.dimen.padding_superlarge);
        }
        if (getListAdapter() == null) {
            TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(
                    (TomahawkMainActivity) getActivity(), layoutInflater, segment, this);
            setListAdapter(tomahawkListAdapter);
        } else {
            getListAdapter().setSegments(segment, getListView());
        }
        showContentHeader(R.drawable.playlists_header);

        onUpdateAdapterFinished();
    }

    @Override
    protected void resolveItem(TomahawkListItem item) {
        Playlist pl = (Playlist) item;
        if (pl.getTopArtistNames() != null) {
            for (int i = 0; i < pl.getTopArtistNames().length && i < 5; i++) {
                super.resolveItem(Artist.get(pl.getTopArtistNames()[i]));
            }
        }
    }

}
