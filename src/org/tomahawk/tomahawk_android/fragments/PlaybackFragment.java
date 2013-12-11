/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
 *   Copyright 2012, Hugo Lindström <hugolm84@gmail.com>
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.dialogs.CreateUserPlaylistDialog;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FakeContextMenu;
import org.tomahawk.tomahawk_android.views.TomahawkStickyListHeadersListView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * This activity represents our Playback view in which the user can play/stop/pause and show/edit
 * the current playlist.
 */
public class PlaybackFragment extends TomahawkFragment
        implements AdapterView.OnItemClickListener, StickyListHeadersListView.OnHeaderClickListener,
        ViewTreeObserver.OnGlobalLayoutListener, FakeContextMenu {

    // Used for debug logging
    public static final String TAG = PlaybackFragment.class.getName();

    // Used to manually assign the correct height to the PlaybackControlsFragment
    private int mFragmentLayoutHeight;

    // The playback fragment at the top of the shown listview
    private PlaybackControlsFragment mPlaybackControlsFragment;

    private PlaybackFragmentBroadcastReceiver mPlaybackFragmentBroadcastReceiver;

    /**
     * Identifier for passing a Track as an extra in an Intent.
     */
    public static final String PLAYLIST_EXTRA = "org.tomahawk.tomahawk_android.playlist_extra";

    public static final String PLAYLIST_ALBUM_ID
            = "org.tomahawk.tomahawk_android.playlist_album_id";

    public static final String PLAYLIST_ARTIST_ID
            = "org.tomahawk.tomahawk_android.playlist_artist_id";

    public static final String PLAYLIST_TRACK_ID
            = "org.tomahawk.tomahawk_android.playlist_track_id";

    public static final String PLAYLIST_COLLECTION_ID
            = "org.tomahawk.tomahawk_android.playlist_collection_id";

    public static final String PLAYLIST_PLAYLIST_ID
            = "org.tomahawk.tomahawk_android.playlist_playlist_id";

    /**
     * Handles incoming broadcasts.
     */
    private class PlaybackFragmentBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (TomahawkMainActivity.PLAYBACKSERVICE_READY.equals(intent.getAction())) {
                onPlaybackServiceReady();
            }
            if (PlaybackService.BROADCAST_NEWTRACK.equals(intent.getAction())) {
                if (mPlaybackControlsFragment != null) {
                    mPlaybackControlsFragment.onTrackChanged();
                }
                onTrackChanged();
                mTomahawkMainActivity.startLoadingAnimation();
            }
            if (PlaybackService.BROADCAST_PLAYLISTCHANGED.equals(intent.getAction())) {
                if (mPlaybackControlsFragment != null) {
                    mPlaybackControlsFragment.onPlaylistChanged();
                }
                onPlaylistChanged();
            }
            if (PlaybackService.BROADCAST_PLAYSTATECHANGED.equals(intent.getAction())) {
                if (mPlaybackControlsFragment != null) {
                    mPlaybackControlsFragment.onPlaystateChanged();
                }
                onPlaystateChanged();
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTomahawkMainActivity.getWindow().getDecorView().getViewTreeObserver()
                .addOnGlobalLayoutListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        onPlaylistChanged();

        if (mPlaybackFragmentBroadcastReceiver == null) {
            mPlaybackFragmentBroadcastReceiver = new PlaybackFragmentBroadcastReceiver();
        }
        // Register intents that mPlaybackFragmentBroadcastReceiver should listen to
        IntentFilter intentFilter = new IntentFilter(PlaybackService.BROADCAST_NEWTRACK);
        mTomahawkMainActivity.registerReceiver(mPlaybackFragmentBroadcastReceiver, intentFilter);
        intentFilter = new IntentFilter(PlaybackService.BROADCAST_PLAYLISTCHANGED);
        mTomahawkMainActivity.registerReceiver(mPlaybackFragmentBroadcastReceiver, intentFilter);
        intentFilter = new IntentFilter(PlaybackService.BROADCAST_PLAYSTATECHANGED);
        mTomahawkMainActivity.registerReceiver(mPlaybackFragmentBroadcastReceiver, intentFilter);
        intentFilter = new IntentFilter(TomahawkMainActivity.PLAYBACKSERVICE_READY);
        mTomahawkMainActivity.registerReceiver(mPlaybackFragmentBroadcastReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mPlaybackFragmentBroadcastReceiver != null) {
            mTomahawkMainActivity.unregisterReceiver(mPlaybackFragmentBroadcastReceiver);
            mPlaybackFragmentBroadcastReceiver = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.playback_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * If the user clicks on a menuItem, handle what should be done here
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        if (playbackService != null && item != null) {
            if (item.getItemId() == R.id.action_clearplaylist_item) {
                while (playbackService.getCurrentPlaylist().getCount() > 0) {
                    playbackService.deleteQueryAtPos(0);
                }
                return true;
            } else if (item.getItemId() == R.id.action_saveplaylist_item) {
                new CreateUserPlaylistDialog(playbackService.getCurrentPlaylist())
                        .show(mTomahawkMainActivity.getSupportFragmentManager(),
                                getString(R.string.playbackactivity_save_playlist_dialog_title));
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        TomahawkListAdapter tomahawkListAdapter = (TomahawkListAdapter) getListAdapter();
        if (playbackService != null && tomahawkListAdapter != null) {
            Object obj = tomahawkListAdapter.getItem(idx - 1);
            if (obj instanceof Query) {
                // if the user clicked on an already playing track
                if (playbackService.getCurrentPlaylist().getCurrentQueryIndex() == idx - 1) {
                    playbackService.playPause();
                } else {
                    playbackService.setCurrentQuery(
                            playbackService.getCurrentPlaylist().getQueryAtPos(idx - 1));
                }
            }
        }
    }

    /**
     * If the listview header is clicked, scroll back to the top. Uses different ways of achieving
     * this depending on api level.
     */
    @Override
    public void onHeaderClick(StickyListHeadersListView list, View header, int itemPosition,
            long headerId, boolean currentlySticky) {
        list = getListView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            list.smoothScrollToPositionFromTop(0, 0, 200);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            int firstVisible = list.getFirstVisiblePosition();
            int lastVisible = list.getLastVisiblePosition();
            if (0 < firstVisible) {
                list.smoothScrollToPosition(0);
            } else {
                list.smoothScrollToPosition(lastVisible - firstVisible - 2);
            }
        } else {
            list.setSelectionFromTop(0, 0);
        }
    }

    /**
     * Workaround to assign the correct height to the PlaybackControlsFragment inside the listview
     */
    @Override
    public void onGlobalLayout() {
        if (mTomahawkMainActivity != null) {
            View activityRootView = mTomahawkMainActivity.getWindow().getDecorView()
                    .findViewById(android.R.id.content);
            mFragmentLayoutHeight = activityRootView.getHeight() - (int) TomahawkUtils
                    .convertDpToPixel(32f, mTomahawkMainActivity);
            mPlaybackControlsFragment = (PlaybackControlsFragment) mTomahawkMainActivity
                    .getSupportFragmentManager()
                    .findFragmentById(R.id.playbackControlsFragment);
            if (mPlaybackControlsFragment != null && mPlaybackControlsFragment.getView() != null) {
                mPlaybackControlsFragment.getView().setLayoutParams(
                        new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                                mFragmentLayoutHeight));
            }
            //is softkeyboard shown hack
            int heightdiff = activityRootView.getRootView().getHeight() - activityRootView
                    .getHeight();
            if (heightdiff < 220) {
                mTomahawkMainActivity.getWindow().getDecorView().getViewTreeObserver()
                        .removeGlobalOnLayoutListener(this);
            }
        }
    }

    /**
     * If the PlaybackService signals, that it is ready, this method is being called
     */
    public void onPlaybackServiceReady() {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        if (mPlaybackControlsFragment != null) {
            mPlaybackControlsFragment.setPlaybackService(playbackService);
        }
        onPlaylistChanged();
    }

    /**
     * Initialize our listview adapter. Adds the current playlist's tracks, sets boolean variables
     * to customize the listview's appearance. Adds the PlaybackControlsFragment to the top of the
     * listview.
     */
    private void initAdapter() {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        if (playbackService != null && playbackService.getCurrentPlaylist() != null) {
            List<TomahawkBaseAdapter.TomahawkListItem> tracks
                    = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
            tracks.addAll(playbackService.getCurrentPlaylist().getQueries());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(tracks);
            TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(mTomahawkMainActivity,
                    listArray);
            tomahawkListAdapter.setShowHighlightingAndPlaystate(true);
            tomahawkListAdapter.setShowResolvedBy(true);
            tomahawkListAdapter.setShowPlaylistHeader(true);
            tomahawkListAdapter.setHighlightedItem(
                    playbackService.getCurrentPlaylist().getCurrentQueryIndex());
            tomahawkListAdapter.setHighlightedItemIsPlaying(playbackService.isPlaying());
            TomahawkStickyListHeadersListView list = getListView();
            list.setOnItemClickListener(this);
            list.setOnHeaderClickListener(this);
            if (list.getHeaderViewsCount() == 0) {
                mPlaybackControlsFragment = (PlaybackControlsFragment) mTomahawkMainActivity
                        .getSupportFragmentManager()
                        .findFragmentById(R.id.playbackControlsFragment);
                View headerView;
                if (mPlaybackControlsFragment == null
                        || mPlaybackControlsFragment.getView() == null) {
                    headerView = mTomahawkMainActivity.getLayoutInflater()
                            .inflate(R.layout.fragment_container_list_item, null);
                    mPlaybackControlsFragment = (PlaybackControlsFragment) mTomahawkMainActivity
                            .getSupportFragmentManager()
                            .findFragmentById(R.id.playbackControlsFragment);
                } else {
                    headerView = (View) mPlaybackControlsFragment.getView().getParent();
                }
                mPlaybackControlsFragment.getView().setLayoutParams(
                        new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                                mFragmentLayoutHeight));
                mPlaybackControlsFragment.setPlaybackService(playbackService);
                list.addHeaderView(headerView);
            }
            mPlaybackControlsFragment.init();
            setListAdapter(tomahawkListAdapter);
        }
    }

    /**
     * Called when the PlaybackFragmentBroadcastReceiver received a Broadcast indicating that the
     * track has changed inside our PlaybackService
     */
    public void onTrackChanged() {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        TomahawkListAdapter tomahawkListAdapter = (TomahawkListAdapter) getListAdapter();
        if (tomahawkListAdapter != null && playbackService != null
                && playbackService.getCurrentPlaylist() != null) {
            tomahawkListAdapter.setHighlightedItem(
                    playbackService.getCurrentPlaylist().getCurrentQueryIndex());
            tomahawkListAdapter.setHighlightedItemIsPlaying(playbackService.isPlaying());
            tomahawkListAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Called when the PlaybackFragmentBroadcastReceiver received a Broadcast indicating that the
     * playState (playing or paused) has changed inside our PlaybackService
     */
    public void onPlaystateChanged() {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        TomahawkListAdapter tomahawkListAdapter = (TomahawkListAdapter) getListAdapter();
        if (tomahawkListAdapter != null && playbackService != null
                && playbackService.getCurrentPlaylist() != null) {
            tomahawkListAdapter.setHighlightedItem(
                    playbackService.getCurrentPlaylist().getCurrentQueryIndex());
            tomahawkListAdapter.setHighlightedItemIsPlaying(playbackService.isPlaying());
            tomahawkListAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Called when the PlaybackFragmentBroadcastReceiver received a Broadcast indicating that the
     * playlist has changed inside our PlaybackService
     */
    public void onPlaylistChanged() {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        TomahawkListAdapter tomahawkListAdapter = (TomahawkListAdapter) getListAdapter();

        if (playbackService != null && playbackService.getCurrentPlaylist() != null) {
            ArrayList<Query> qs = new ArrayList<Query>();
            for (Query query : playbackService.getCurrentPlaylist().getQueries()) {
                if (!query.isSolved() && !mPipeline.hasQuery(query.getQid())) {
                    qs.add(query);
                }
            }
            if (!qs.isEmpty()) {
                HashSet<String> qids = mPipeline.resolve(qs);
                mCorrespondingQueryIds.addAll(qids);
                mTomahawkMainActivity.startLoadingAnimation();
            }
        }

        if (tomahawkListAdapter != null && playbackService != null
                && playbackService.getCurrentPlaylist() != null
                && playbackService.getCurrentPlaylist().getCount() > 0) {
            ArrayList<TomahawkBaseAdapter.TomahawkListItem> tracks
                    = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
            tracks.addAll(playbackService.getCurrentPlaylist().getQueries());
            tomahawkListAdapter.setListWithIndex(0, tracks);
            tomahawkListAdapter.setHighlightedItem(
                    playbackService.getCurrentPlaylist().getCurrentQueryIndex());
            tomahawkListAdapter.setHighlightedItemIsPlaying(playbackService.isPlaying());
            tomahawkListAdapter.notifyDataSetChanged();
        } else {
            initAdapter();
        }
    }

    @Override
    protected void onPipeLineResultsReported(String qId) {
        if (mCorrespondingQueryIds.contains(qId)) {
            onPlaylistChanged();
        }
    }
}
