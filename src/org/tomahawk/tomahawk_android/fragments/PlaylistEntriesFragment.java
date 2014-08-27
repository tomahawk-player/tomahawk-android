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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mPlaylist != null) {
            MenuItem shuffleItem = menu.findItem(R.id.action_playshuffled_item);
            shuffleItem.setVisible(true);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * If the user clicks on a menuItem, handle what should be done here
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null) {
            if (item.getItemId() == R.id.action_playshuffled_item) {
                ArrayList<PlaylistEntry> entries = new ArrayList<PlaylistEntry>();
                if (mPlaylist != null) {
                    entries = mPlaylist.getEntries();
                }
                PlaybackService playbackService =
                        ((TomahawkMainActivity) getActivity()).getPlaybackService();
                Playlist playlist = Playlist.fromEntriesList(
                        DatabaseHelper.CACHED_PLAYLIST_NAME, "", entries);
                playlist.setId(DatabaseHelper.CACHED_PLAYLIST_ID);
                if (playbackService != null) {
                    playbackService.setPlaylist(playlist,
                            playlist.getEntryAtPos((int) (Math.random() * playlist.size())));
                    Class clss = mContainerFragmentClass != null ? mContainerFragmentClass
                            : ((Object) this).getClass();
                    playbackService.setReturnFragment(clss, getArguments());
                    playbackService.setShuffled(true);
                    playbackService.start();
                }
            }
            ((TomahawkMainActivity) getActivity()).closeDrawer();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        CollectionManager.getInstance().fetchPlaylists();
        if (mUser != null) {
            mCurrentRequestIds.add(InfoSystem.getInstance().resolvePlaybackLog(mUser));
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
        Context context = getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View rootView = getView();
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
                mPlaylist.setFilled(true);
                refreshCurrentPlaylist();
            } else {
                if (!mPlaylist.getId().equals(DatabaseHelper.LOVEDITEMS_PLAYLIST_ID)) {
                    activity.setTitle(mPlaylist.getName());
                }
                playlistEntries.addAll(mPlaylist.getEntries());
                Segment segment = new Segment(R.string.segmentheader_playlist, playlistEntries);
                if (getListAdapter() == null) {
                    tomahawkListAdapter = new TomahawkListAdapter(activity, layoutInflater,
                            segment, this);
                    tomahawkListAdapter.setShowResolvedBy(true);
                    tomahawkListAdapter.showContentHeader(rootView, mPlaylist, mCollection,
                            mStarLoveButtonListener);
                    setListAdapter(tomahawkListAdapter);
                } else {
                    getListAdapter().setSegments(segment);
                    getListAdapter().showContentHeader(rootView,
                            mPlaylist, mCollection, mStarLoveButtonListener);
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

    @Override
    public void onPanelCollapsed() {
        if (mPlaylist != null && !mPlaylist.getId().equals(DatabaseHelper.LOVEDITEMS_PLAYLIST_ID)) {
            getActivity().setTitle(mPlaylist.getName());
        }
    }

    @Override
    public void onPanelExpanded() {
    }
}
