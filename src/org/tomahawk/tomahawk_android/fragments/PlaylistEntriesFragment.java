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
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.view.View;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment} which shows a set of {@link
 * org.tomahawk.libtomahawk.collection.Track}s inside its {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class PlaylistEntriesFragment extends TomahawkFragment {

    private Set<String> mResolvingTopArtistNames = new HashSet<>();

    @SuppressWarnings("unused")
    public void onEvent(InfoSystem.ResultsEvent event) {
        if (event.mInfoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES) {
            List<Playlist> playlists = event.mInfoRequestData.getResultList(Playlist.class);
            if (playlists != null && playlists.size() > 0) {
                mPlaylist = playlists.get(0);
            }
        }

        super.onEvent(event);
    }

    @SuppressWarnings("unused")
    public void onEvent(DatabaseHelper.PlaylistsUpdatedEvent event) {
        if (mPlaylist != null && mPlaylist.getId().equals(event.mPlaylistId)) {
            refreshCurrentPlaylist();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        CollectionManager.getInstance().fetchPlaylists();
        if (mUser != null) {
            if (mPlaylist == mUser.getPlaybackLog()) {
                String requestId = InfoSystem.getInstance().resolvePlaybackLog(mUser);
                if (requestId != null) {
                    mCorrespondingRequestIds.add(requestId);
                }
            } else if (mPlaylist == mUser.getFavorites()) {
                String requestId = InfoSystem.getInstance().resolveLovedItems(mUser);
                if (requestId != null) {
                    mCorrespondingRequestIds.add(requestId);
                }
            }
        }
        if (mPlaylist != null) {
            if (DatabaseHelper.LOVEDITEMS_PLAYLIST_ID.equals(mPlaylist.getId())) {
                CollectionManager.getInstance().fetchLovedItemsPlaylist();
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
     * @param item the Object which corresponds to the click
     */
    @Override
    public void onItemClick(View view, Object item) {
        if (item instanceof PlaylistEntry) {
            getListAdapter().getPlaylistEntry(item).done(new DoneCallback<PlaylistEntry>() {
                @Override
                public void onDone(final PlaylistEntry entry) {
                    if (entry.getQuery().isPlayable()) {
                        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
                        final PlaybackService playbackService = activity.getPlaybackService();
                        if (playbackService != null) {
                            if (playbackService.getCurrentEntry() == entry) {
                                playbackService.playPause();
                            } else {
                                getListAdapter().getPlaylist().done(new DoneCallback<Playlist>() {
                                    @Override
                                    public void onDone(Playlist playlist) {
                                        playbackService.setPlaylist(playlist, entry);
                                        playbackService.start();
                                    }
                                });
                            }
                        }
                    }
                }
            });
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

        if (mPlaylist != null) {
            if (!mPlaylist.isFilled()) {
                refreshCurrentPlaylist();
            } else {
                Segment segment = new Segment(R.string.playlist_details, mPlaylist.getEntries());
                segment.setShowNumeration(true, 1);
                fillAdapter(segment);
                showContentHeader(mPlaylist);
                showFancyDropDown(0, mPlaylist.getName(), null, null);
                ThreadManager.getInstance()
                        .execute(new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_INFOSYSTEM_LOW) {
                            @Override
                            public void run() {
                                if (mPlaylist.getTopArtistNames() == null
                                        || mPlaylist.getTopArtistNames().length == 0) {
                                    mPlaylist.updateTopArtistNames();
                                }
                                for (int i = 0; i < mPlaylist.getTopArtistNames().length && i < 5;
                                        i++) {
                                    String artistName = mPlaylist.getTopArtistNames()[i];
                                    if (mResolvingTopArtistNames.contains(artistName)) {
                                        mCorrespondingRequestIds.addAll(InfoSystem.getInstance()
                                                .resolve(Artist.get(artistName), false));
                                        mResolvingTopArtistNames.add(artistName);
                                    }
                                }
                            }
                        });
            }
        }
    }

    protected void refreshCurrentPlaylist() {
        ThreadManager.getInstance().execute(
                new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_VERYHIGH) {
                    @Override
                    public void run() {
                        User.getSelf().done(new DoneCallback<User>() {
                            @Override
                            public void onDone(User user) {
                                if (mUser != user) {
                                    String requestId = InfoSystem.getInstance().resolve(mPlaylist);
                                    if (requestId != null) {
                                        mCorrespondingRequestIds.add(requestId);
                                    }
                                } else {
                                    Playlist playlist = DatabaseHelper.getInstance()
                                            .getPlaylist(mPlaylist.getId());
                                    if (playlist != null) {
                                        mPlaylist = playlist;
                                        if (!mAdapterUpdateHandler
                                                .hasMessages(ADAPTER_UPDATE_MSG)) {
                                            mAdapterUpdateHandler.sendEmptyMessageDelayed(
                                                    ADAPTER_UPDATE_MSG, ADAPTER_UPDATE_DELAY);
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
        );
    }
}
