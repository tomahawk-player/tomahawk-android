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

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.ListItemString;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.dialogs.CreatePlaylistDialog;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.IdGenerator;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * {@link TomahawkFragment} which shows a set of {@link org.tomahawk.libtomahawk.collection.Playlist}s
 * inside its {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class PlaylistsFragment extends TomahawkFragment {

    private final HashSet<User> mResolvingUsers = new HashSet<>();

    @SuppressWarnings("unused")
    public void onEventAsync(DatabaseHelper.PlaylistsUpdatedEvent event) {
        scheduleUpdateAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playlistsfragment_layout, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        User.getSelf().done(new DoneCallback<User>() {
            @Override
            public void onDone(User user) {
                if (mUser == user) {
                    CollectionManager.get().fetchPlaylists();
                } else {
                    mHideRemoveButton = true;
                }
            }
        });

        if (mContainerFragmentClass == null) {
            getActivity().setTitle(getString(R.string.drawer_title_playlists).toUpperCase());
            if (getView() != null) {
                View newButton = getView().findViewById(R.id.create_new_button);
                newButton.setVisibility(View.VISIBLE);
                newButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCreateDialog();
                    }
                });
            }
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
                            IdGenerator.getLifetimeUniqueStringId()));
                }
                CollectionManager.get().addPlaylistEntries(playlistId, entries);
                // invalidate the current list of entries
                ((Playlist) item).setFilled(false);
            }
            Bundle bundle = new Bundle();
            bundle.putString(TomahawkFragment.PLAYLIST, ((Playlist) item).getCacheKey());
            if (mUser != null) {
                bundle.putString(TomahawkFragment.USER, mUser.getId());
            }
            bundle.putInt(CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_DYNAMIC);
            FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                    PlaylistEntriesFragment.class, bundle);
        }
        getArguments().remove(QUERYARRAY);
        mQueryArray = null;
    }

    public void showCreateDialog() {
        ArrayList<Query> queries = mQueryArray != null ? mQueryArray : new ArrayList<Query>();
        Playlist playlist = Playlist.fromQueryList(
                IdGenerator.getLifetimeUniqueStringId(), "", null, queries);
        CreatePlaylistDialog dialog = new CreatePlaylistDialog();
        Bundle args = new Bundle();
        args.putString(TomahawkFragment.PLAYLIST, playlist.getCacheKey());
        args.putString(TomahawkFragment.USER, mUser.getCacheKey());
        dialog.setArguments(args);
        dialog.show(getFragmentManager(), null);
    }

    /**
     * Called every time an item inside a ListView or GridView is long-clicked
     *
     * @param item the Object which corresponds to the long-click
     */
    @Override
    public boolean onItemLongClick(View view, Object item) {
        return FragmentUtils.showContextMenu((TomahawkMainActivity) getActivity(), item, null,
                false, mHideRemoveButton);
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    @Override
    protected void updateAdapter() {
        if (!mIsResumed) {
            return;
        }

        final List<Segment> segments = new ArrayList<>();

        if (mQueryArray != null) {
            // Add the header text item
            List textItems = new ArrayList();
            textItems.add(new ListItemString(
                    getResources().getQuantityString(R.plurals.add_to_playlist_headertext,
                            mQueryArray.size(), mQueryArray.size()), true));
            segments.add(new Segment.Builder(textItems).build());
        }

        User.getSelf().done(new DoneCallback<User>() {
            @Override
            public void onDone(User user) {
                List playlists = new ArrayList();
                if (mUser.getPlaylists() == null) {
                    if (mUser != user && !mResolvingUsers.contains(mUser)) {
                        String requestId = InfoSystem.get().resolvePlaylists(mUser, false);
                        if (requestId != null) {
                            mCorrespondingRequestIds.add(requestId);
                        }
                        mResolvingUsers.add(mUser);
                    }
                } else {
                    playlists.addAll(mUser.getPlaylists());
                }
                segments.add(new Segment.Builder(playlists)
                        .showAsGrid(R.integer.grid_column_count, R.dimen.padding_superlarge,
                                R.dimen.padding_superlarge)
                        .build());
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        fillAdapter(segments);
                        showContentHeader(R.drawable.playlists_header);
                        if (getView() != null) {
                            View newButton = getView().findViewById(R.id.create_new_button);
                            int y = mHeaderNonscrollableHeight
                                    - getResources().getDimensionPixelSize(
                                    R.dimen.row_height_medium)
                                    - getResources().getDimensionPixelSize(R.dimen.padding_small);
                            newButton.setY(y);
                        }
                    }
                });
            }
        });
    }

}
