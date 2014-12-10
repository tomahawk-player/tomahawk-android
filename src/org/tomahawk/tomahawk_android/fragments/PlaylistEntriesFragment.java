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

import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment} which shows a set of {@link
 * org.tomahawk.libtomahawk.collection.Track}s inside its {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class PlaylistEntriesFragment extends TomahawkFragment {

    @Override
    public void onResume() {
        super.onResume();

        CollectionManager.getInstance().fetchPlaylists();
        if (mUser != null) {
            if (mPlaylist == mUser.getPlaybackLog()) {
                mCurrentRequestIds.add(InfoSystem.getInstance().resolvePlaybackLog(mUser));
            } else if (mPlaylist == mUser.getFavorites()) {
                mCurrentRequestIds.add(InfoSystem.getInstance().resolveFavorites(mUser));
            }
        }
        if (mPlaylist != null
                && DatabaseHelper.LOVEDITEMS_PLAYLIST_ID.equals(mPlaylist.getId())) {
            CollectionManager.getInstance().fetchLovedItemsPlaylist();
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
     * @param item the TomahawkListItem which corresponds to the click
     */
    @Override
    public void onItemClick(View view, TomahawkListItem item) {
        if (item instanceof PlaylistEntry) {
            PlaylistEntry entry = (PlaylistEntry) item;
            if (entry.getQuery().isPlayable()) {
                ArrayList<PlaylistEntry> entries = new ArrayList<PlaylistEntry>();
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

        mResolveQueriesHandler.removeCallbacksAndMessages(null);
        mResolveQueriesHandler.sendEmptyMessage(RESOLVE_QUERIES_REPORTER_MSG);
        ArrayList<TomahawkListItem> playlistEntries = new ArrayList<TomahawkListItem>();
        TomahawkListAdapter tomahawkListAdapter;
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        if (mPlaylist != null) {
            ThreadManager.getInstance().execute(
                    new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_INFOSYSTEM_MEDIUM) {
                        @Override
                        public void run() {
                            getPlaylistArtists(mPlaylist);
                        }
                    }
            );
            if (!mPlaylist.isFilled()) {
                refreshCurrentPlaylist();
            } else {
                playlistEntries.addAll(mPlaylist.getEntries());
                Segment segment = new Segment(R.string.playlist_details, playlistEntries);
                if (getListAdapter() == null) {
                    tomahawkListAdapter = new TomahawkListAdapter(activity, layoutInflater,
                            segment, this);
                    tomahawkListAdapter.setShowNumeration(true);
                    if (!mDontShowHeader) {
                        tomahawkListAdapter.setShowContentHeaderSpacerResId(
                                R.dimen.header_clear_space_scrollable, getListView());
                    }
                    setListAdapter(tomahawkListAdapter);
                } else {
                    getListAdapter().setSegments(segment, getListView());
                }
                if (!mDontShowHeader) {
                    showContentHeader(mPlaylist, R.dimen.header_clear_space_nonscrollable);
                    showFancyDropDown(mPlaylist);
                }
            }
        }

        mShownQueries.clear();
        mShownPlaylistEntries.clear();
        for (TomahawkListItem playlistEntry : playlistEntries) {
            mShownQueries.add(((PlaylistEntry) playlistEntry).getQuery());
            mShownPlaylistEntries.add((PlaylistEntry) playlistEntry);
        }

        updateShowPlaystate();
    }

    private void getPlaylistArtists(Playlist playlist) {
        if (playlist.getContentHeaderArtists().size() < 6) {
            final HashMap<Artist, Integer> countMap = new HashMap<Artist, Integer>();
            for (Query query : playlist.getQueries()) {
                Artist artist = query.getArtist();
                if (countMap.containsKey(artist)) {
                    countMap.put(artist, countMap.get(artist) + 1);
                } else {
                    countMap.put(artist, 1);
                }
            }
            TreeMap<Artist, Integer> sortedCountMap = new TreeMap<Artist, Integer>(
                    new Comparator<Artist>() {
                        @Override
                        public int compare(Artist lhs, Artist rhs) {
                            return countMap.get(lhs) >= countMap.get(rhs) ? -1 : 1;
                        }
                    }
            );
            sortedCountMap.putAll(countMap);
            for (Artist artist : sortedCountMap.keySet()) {
                synchronized (playlist) {
                    playlist.addContentHeaderArtists(artist);
                }
                ArrayList<String> requestIds = InfoSystem.getInstance().resolve(artist, true);
                for (String requestId : requestIds) {
                    mCurrentRequestIds.add(requestId);
                }
                if (playlist.getContentHeaderArtists().size() == 6) {
                    break;
                }
            }
        }
    }
}
