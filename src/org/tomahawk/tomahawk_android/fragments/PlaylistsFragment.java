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
import org.tomahawk.libtomahawk.collection.ListItemString;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.dialogs.CreatePlaylistDialog;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

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

    private final HashSet<User> mResolvingUsers = new HashSet<>();

    private List<Playlist> mPlaylists = new ArrayList<>();

    @SuppressWarnings("unused")
    public void onEventAsync(DatabaseHelper.PlaylistsUpdatedEvent event) {
        HatchetAuthenticatorUtils authenticatorUtils
                = (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        if (mUser == null || mUser == authenticatorUtils.getLoggedInUser()) {
            refreshPlaylists();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        CollectionManager.getInstance().fetchPlaylists();
        refreshPlaylists();

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
            String playlistId = ((Playlist) item).getId();
            if (mQueryArray != null) {
                ArrayList<PlaylistEntry> entries = new ArrayList<>();
                for (Query query : mQueryArray) {
                    entries.add(PlaylistEntry.get(playlistId, query,
                            TomahawkMainActivity.getLifetimeUniqueStringId()));
                }
                CollectionManager.getInstance().addPlaylistEntries(playlistId, entries);
            }
            Bundle bundle = new Bundle();
            bundle.putString(TomahawkFragment.PLAYLIST, playlistId);
            if (mUser != null) {
                bundle.putString(TomahawkFragment.USER, mUser.getId());
            }
            bundle.putInt(CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_DYNAMIC);
            FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                    PlaylistEntriesFragment.class, bundle);
        } else {
            ArrayList<Query> queries = mQueryArray != null ? mQueryArray : new ArrayList<Query>();
            Playlist playlist = Playlist.fromQueryList("", queries);
            CreatePlaylistDialog dialog = new CreatePlaylistDialog();
            Bundle args = new Bundle();
            args.putString(TomahawkFragment.PLAYLIST, playlist.getId());
            dialog.setArguments(args);
            dialog.show(getFragmentManager(), null);
        }
        getArguments().remove(QUERYARRAY);
        mQueryArray = null;
    }

    /**
     * Called every time an item inside a ListView or GridView is long-clicked
     *
     * @param item the Object which corresponds to the long-click
     */
    @Override
    public boolean onItemLongClick(View view, Object item) {
        return item.equals(CREATE_PLAYLIST_BUTTON_ID) || FragmentUtils
                .showContextMenu((TomahawkMainActivity) getActivity(), item, null, false);
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
        List<Segment> segments = new ArrayList<>();

        if (mQueryArray != null) {
            // Add the header text item
            List textItems = new ArrayList();
            textItems.add(new ListItemString(
                    getResources().getQuantityString(R.plurals.add_to_playlist_headertext,
                            mQueryArray.size(), mQueryArray.size()), true));
            segments.add(new Segment(textItems));
        }

        if (mUser != null && mUser != authenticatorUtils.getLoggedInUser()) {
            if (mUser.getPlaylists() == null) {
                if (!mResolvingUsers.contains(mUser)) {
                    mCorrespondingRequestIds.add(InfoSystem.getInstance().resolvePlaylists(mUser));
                    mResolvingUsers.add(mUser);
                }
            } else {
                playlists.addAll(mUser.getPlaylists());
            }
            segments.add(new Segment(playlists));
        } else {
            playlists.add(CREATE_PLAYLIST_BUTTON_ID);
            mPlaylists = DatabaseHelper.getInstance().getCachedPlaylists();
            playlists.addAll(mPlaylists);
            segments.add(new Segment(playlists, R.integer.grid_column_count,
                    R.dimen.padding_superlarge, R.dimen.padding_superlarge));
        }
        if (getListAdapter() == null) {
            TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(
                    (TomahawkMainActivity) getActivity(), layoutInflater, segments, getListView(),
                    this);
            setListAdapter(tomahawkListAdapter);
        } else {
            getListAdapter().setSegments(segments, getListView());
        }
        showContentHeader(R.drawable.playlists_header);

        onUpdateAdapterFinished();
    }

    @Override
    protected void resolveItem(final TomahawkListItem item) {
        if (item instanceof Playlist) {
            HatchetAuthenticatorUtils authenticatorUtils
                    = (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                    .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
            if (mUser == null || mUser == authenticatorUtils.getLoggedInUser()) {
                TomahawkRunnable r = new TomahawkRunnable(
                        TomahawkRunnable.PRIORITY_IS_DATABASEACTION) {
                    @Override
                    public void run() {
                        if (!mResolvingItems.contains(item)) {
                            mResolvingItems.add(item);
                            Playlist pl = (Playlist) item;
                            if (pl.getEntries().size() == 0) {
                                pl = DatabaseHelper.getInstance().getPlaylist(pl.getId());
                            }
                            if (pl != null && pl.getEntries().size() > 0) {
                                pl.updateTopArtistNames();
                                DatabaseHelper.getInstance().updatePlaylist(pl);
                                if (pl.getTopArtistNames() != null) {
                                    for (int i = 0; i < pl.getTopArtistNames().length && i < 5;
                                            i++) {
                                        PlaylistsFragment.super
                                                .resolveItem(Artist.get(pl.getTopArtistNames()[i]));
                                    }
                                }
                            } else {
                                mResolvingItems.remove(item);
                            }
                        }
                    }
                };
                ThreadManager.getInstance().execute(r);
            }
        }
    }

    private void refreshPlaylists() {
        ThreadManager.getInstance().execute(
                new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_VERYHIGH) {
                    @Override
                    public void run() {
                        mPlaylists = DatabaseHelper.getInstance().getPlaylists();
                        if (!mAdapterUpdateHandler.hasMessages(ADAPTER_UPDATE_MSG)) {
                            mAdapterUpdateHandler.sendEmptyMessageDelayed(
                                    ADAPTER_UPDATE_MSG, ADAPTER_UPDATE_DELAY);
                        }
                    }
                });
    }

}
