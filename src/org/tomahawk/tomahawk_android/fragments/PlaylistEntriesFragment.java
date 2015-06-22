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
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment} which shows a set of {@link
 * org.tomahawk.libtomahawk.collection.Track}s inside its {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class PlaylistEntriesFragment extends TomahawkFragment {

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
                String requestId = InfoSystem.getInstance().resolveFavorites(mUser);
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
            PlaylistEntry entry = (PlaylistEntry) item;
            if (entry.getQuery().isPlayable()) {
                ArrayList<PlaylistEntry> entries = new ArrayList<>();
                if (mPlaylist != null) {
                    entries = mPlaylist.getEntries();
                }
                PlaybackService playbackService =
                        ((TomahawkMainActivity) getActivity()).getPlaybackService();
                if (playbackService != null && playbackService.getCurrentEntry() == entry) {
                    playbackService.playPause();
                } else {
                    Playlist playlist = Playlist.fromEntriesList(
                            DatabaseHelper.CACHED_PLAYLIST_NAME, "", entries);
                    playlist.setId(DatabaseHelper.CACHED_PLAYLIST_ID);
                    if (playbackService != null) {
                        playbackService.setPlaylist(playlist, entry);
                        Class clss = mContainerFragmentClass != null ? mContainerFragmentClass
                                : ((Object) this).getClass();
                        playbackService.setReturnFragment(clss, getArguments());
                        playbackService.start();
                    }
                }
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

        List playlistEntries = new ArrayList();
        if (mPlaylist != null) {
            if (!mPlaylist.isFilled()) {
                refreshCurrentPlaylist();
            } else {
                playlistEntries.addAll(mPlaylist.getEntries());
                Segment segment = new Segment(R.string.playlist_details, playlistEntries);
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
                                    mCorrespondingRequestIds.addAll(InfoSystem.getInstance()
                                            .resolve(Artist.get(mPlaylist.getTopArtistNames()[i]),
                                                    false));
                                }
                            }
                        });
            }
        }

        mShownQueries.clear();
        mShownPlaylistEntries.clear();
        for (Object playlistEntry : playlistEntries) {
            mShownQueries.add(((PlaylistEntry) playlistEntry).getQuery());
            mShownPlaylistEntries.add((PlaylistEntry) playlistEntry);
        }
    }

    protected void refreshCurrentPlaylist() {
        ThreadManager.getInstance().execute(
                new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_VERYHIGH) {
                    @Override
                    public void run() {
                        HatchetAuthenticatorUtils authenticatorUtils
                                = (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                        if (mUser != authenticatorUtils.getLoggedInUser()) {
                            String requestId = InfoSystem.getInstance().resolve(mPlaylist);
                            if (requestId != null) {
                                mCorrespondingRequestIds.add(requestId);
                            }
                        } else {
                            Playlist playlist =
                                    DatabaseHelper.getInstance().getPlaylist(mPlaylist.getId());
                            if (playlist != null) {
                                mPlaylist = playlist;
                                if (!mAdapterUpdateHandler.hasMessages(ADAPTER_UPDATE_MSG)) {
                                    mAdapterUpdateHandler.sendEmptyMessageDelayed(
                                            ADAPTER_UPDATE_MSG, ADAPTER_UPDATE_DELAY);
                                }
                            }
                        }
                    }
                }
        );
    }
}
