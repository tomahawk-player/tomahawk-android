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
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment} which shows a set of {@link
 * org.tomahawk.libtomahawk.collection.Track}s inside its {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class PlaylistEntriesFragment extends TomahawkFragment implements OnItemClickListener {

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
        if (getListAdapter().getItem(position) instanceof PlaylistEntry) {
            PlaylistEntry entry = (PlaylistEntry) getListAdapter().getItem(position);
            if (entry.getQuery().isPlayable()) {
                ArrayList<Query> queries = new ArrayList<Query>();
                if (mPlaylist != null) {
                    queries = mPlaylist.getQueries();
                }
                PlaybackService playbackService =
                        ((TomahawkMainActivity) getActivity()).getPlaybackService();
                if (playbackService != null && shouldShowPlaystate() && mQueryPositions
                        .get(playbackService.getCurrentPlaylist().getCurrentQueryIndex())
                        == position) {
                    playbackService.playPause();
                } else {
                    Playlist playlist = Playlist.fromQueryList(
                            DatabaseHelper.CACHED_PLAYLIST_ID,
                            DatabaseHelper.CACHED_PLAYLIST_NAME, queries,
                            mQueryPositions.keyAt(mQueryPositions.indexOfValue(position)));
                    if (playbackService != null) {
                        playbackService.setCurrentPlaylist(playlist);
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
                ThreadManager.getInstance().execute(
                        new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_INFOSYSTEM_HIGH) {
                            @Override
                            public void run() {
                                mPlaylist = DatabaseHelper.getInstance()
                                        .getPlaylist(mPlaylist.getId());
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateAdapter();
                                    }
                                });
                            }
                        }
                );
            } else {
                if (!mPlaylist.getId().equals(DatabaseHelper.LOVEDITEMS_PLAYLIST_ID)) {
                    activity.setTitle(mPlaylist.getName());
                }
                playlistEntries.addAll(mPlaylist.getEntries());
                if (getListAdapter() == null) {
                    tomahawkListAdapter = new TomahawkListAdapter(context, layoutInflater,
                            playlistEntries);
                    tomahawkListAdapter.setShowResolvedBy(true);
                    tomahawkListAdapter.showContentHeader(rootView, mPlaylist, mCollection);
                    setListAdapter(tomahawkListAdapter);
                } else {
                    ((TomahawkListAdapter) getListAdapter()).setListItems(playlistEntries);
                    ((TomahawkListAdapter) getListAdapter()).showContentHeader(rootView,
                            mPlaylist, mCollection);
                }
            }
        }

        int precedingItemCount = 0;
        if (getListAdapter() != null
                && ((TomahawkListAdapter) getListAdapter()).isShowingContentHeader()) {
            precedingItemCount++;
        }
        mShownQueries.clear();
        mShownPlaylistEntries.clear();
        mQueryPositions.clear();
        for (int i = 0; i < playlistEntries.size(); i++) {
            mShownQueries.add(((PlaylistEntry) playlistEntries.get(i)).getQuery());
            mShownPlaylistEntries.add((PlaylistEntry) playlistEntries.get(i));
            mQueryPositions.put(i, i + precedingItemCount);
        }

        getListView().setOnItemClickListener(this);

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
