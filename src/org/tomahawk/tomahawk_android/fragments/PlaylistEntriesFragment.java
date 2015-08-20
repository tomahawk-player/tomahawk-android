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
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.util.Log;
import android.view.View;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment} which shows a set of {@link
 * org.tomahawk.libtomahawk.collection.Track}s inside its {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class PlaylistEntriesFragment extends TomahawkFragment {

    public static final int SHOW_MODE_LOVEDITEMS = 0;

    public static final int SHOW_MODE_PLAYBACKLOG = 1;

    private Set<String> mResolvingTopArtistNames = new HashSet<>();

    @SuppressWarnings("unused")
    public void onEvent(DatabaseHelper.PlaylistsUpdatedEvent event) {
        if (mPlaylist != null && mPlaylist.getId().equals(event.mPlaylistId)) {
            refreshUserPlaylists();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        CollectionManager.getInstance().fetchPlaylists();
        User.getSelf().done(new DoneCallback<User>() {
            @Override
            public void onDone(User user) {
                if (mUser != null) {
                    if (mShowMode == SHOW_MODE_PLAYBACKLOG) {
                        String requestId = InfoSystem.getInstance().resolvePlaybackLog(mUser);
                        if (requestId != null) {
                            mCorrespondingRequestIds.add(requestId);
                        }
                    } else if (mShowMode == SHOW_MODE_LOVEDITEMS) {
                        mHideRemoveButton = true;
                        if (mUser == user) {
                            CollectionManager.getInstance().fetchLovedItemsPlaylist();
                            refreshUserPlaylists();
                        } else {
                            String requestId = InfoSystem.getInstance().resolveLovedItems(mUser);
                            if (requestId != null) {
                                mCorrespondingRequestIds.add(requestId);
                            }
                        }
                    }
                    if (mUser != user) {
                        mHideRemoveButton = true;
                    }
                }
                if (mContainerFragmentClass == null) {
                    getActivity().setTitle("");
                }
                updateAdapter();
            }
        });
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

        if (getPlaylist() != null) {
            if (!getPlaylist().isFilled()) {
                User.getSelf().done(new DoneCallback<User>() {
                    @Override
                    public void onDone(User user) {
                        if (mUser != user && mShowMode < 0) {
                            String requestId = InfoSystem.getInstance().resolve(mPlaylist);
                            if (requestId != null) {
                                mCorrespondingRequestIds.add(requestId);
                            }
                        }
                    }
                });
            } else {
                Segment segment = new Segment(R.string.playlist_details,
                        getPlaylist().getEntries());
                segment.setShowNumeration(true, 1);
                fillAdapter(segment);
                showContentHeader(getPlaylist());
                showFancyDropDown(0, getPlaylist().getName(), null, null);
                ThreadManager.getInstance()
                        .execute(new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_INFOSYSTEM_LOW) {
                            @Override
                            public void run() {
                                if (getPlaylist().getTopArtistNames() == null
                                        || getPlaylist().getTopArtistNames().length == 0) {
                                    getPlaylist().updateTopArtistNames();
                                } else {
                                    for (int i = 0;
                                            i < getPlaylist().getTopArtistNames().length && i < 5;
                                            i++) {
                                        String artistName = getPlaylist().getTopArtistNames()[i];
                                        if (mResolvingTopArtistNames.contains(artistName)) {
                                            mCorrespondingRequestIds.addAll(InfoSystem.getInstance()
                                                    .resolve(Artist.get(artistName), false));
                                            mResolvingTopArtistNames.add(artistName);
                                        }
                                    }
                                }
                            }
                        });
            }
        }
    }

    private Playlist getPlaylist() {
        if (mUser != null) {
            if (mShowMode == SHOW_MODE_PLAYBACKLOG) {
                return mUser.getPlaybackLog();
            } else if (mShowMode == SHOW_MODE_LOVEDITEMS) {
                return mUser.getFavorites();
            }
        } else if (mPlaylist != null) {
            return mPlaylist;
        }
        Log.e(TAG, "mUser and mShowMode null or mPlaylist null.");
        return null;
    }

    private void refreshUserPlaylists() {
        CollectionManager.getInstance().refreshUserPlaylists().done(new DoneCallback<Void>() {
            @Override
            public void onDone(Void result) {
                if (!mAdapterUpdateHandler.hasMessages(ADAPTER_UPDATE_MSG)) {
                    mAdapterUpdateHandler.sendEmptyMessageDelayed(
                            ADAPTER_UPDATE_MSG, ADAPTER_UPDATE_DELAY);
                }
            }
        });
    }
}
