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

import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.AlbumArtSwipeAdapter;
import org.tomahawk.tomahawk_android.adapters.PlaybackPagerAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.dialogs.CreateUserPlaylistDialog;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FakeContextMenu;
import org.tomahawk.tomahawk_android.views.PlaybackSeekBar;
import org.tomahawk.tomahawk_android.views.TomahawkVerticalViewPager;

import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * This {@link android.support.v4.app.Fragment} represents our Playback view in which the user can
 * play/stop/pause. It is being shown as the topmost fragment in the {@link PlaybackFragment}'s
 * {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}.
 */
public class PlaybackFragment extends TomahawkFragment
        implements AdapterView.OnItemClickListener,
        se.emilsjolander.stickylistheaders.StickyListHeadersListView.OnHeaderClickListener,
        FakeContextMenu {

    private AlbumArtSwipeAdapter mAlbumArtSwipeAdapter;

    private PlaybackPagerAdapter mPlaybackPagerAdapter;

    private PlaybackSeekBar mPlaybackSeekBar;

    private Toast mToast;

    /**
     * This listener handles our button clicks
     */
    private View.OnClickListener mButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.imageButton_shuffle:
                    onShuffleClicked();
                    break;
                case R.id.imageButton_previous:
                    onPreviousClicked();
                    break;
                case R.id.imageButton_playpause:
                    onPlayPauseClicked();
                    break;
                case R.id.imageButton_next:
                    onNextClicked();
                    break;
                case R.id.imageButton_repeat:
                    onRepeatClicked();
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playback_fragment, null, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Set listeners on our buttons
        view.findViewById(R.id.imageButton_shuffle).setOnClickListener(mButtonClickListener);
        view.findViewById(R.id.imageButton_previous).setOnClickListener(mButtonClickListener);
        view.findViewById(R.id.imageButton_playpause).setOnClickListener(mButtonClickListener);
        view.findViewById(R.id.imageButton_next).setOnClickListener(mButtonClickListener);
        view.findViewById(R.id.imageButton_repeat).setOnClickListener(mButtonClickListener);
    }

    @Override
    public void onResume() {
        super.onResume();

        onPlaylistChanged();

        mTomahawkMainActivity.setTitle(getString(R.string.playbackfragment_title_string));

        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        ViewPager viewPager = (ViewPager) mTomahawkMainActivity.getLayoutInflater()
                .inflate(R.layout.album_art_view_pager, null, false);
        mAlbumArtSwipeAdapter = new AlbumArtSwipeAdapter((ActionBarActivity) getActivity(),
                viewPager);
        mAlbumArtSwipeAdapter.setPlaybackService(playbackService);
        viewPager.setAdapter(mAlbumArtSwipeAdapter);
        viewPager.setOnPageChangeListener(mAlbumArtSwipeAdapter);

        TomahawkVerticalViewPager verticalViewPager = (TomahawkVerticalViewPager) getView()
                .findViewById(R.id.playback_view_pager);
        mPlaybackPagerAdapter = new PlaybackPagerAdapter(viewPager, getListView());
        verticalViewPager.setAdapter(mPlaybackPagerAdapter);
        verticalViewPager.setStickyListHeadersListView(getListView());

        mPlaybackSeekBar = (PlaybackSeekBar) getView().findViewById(R.id.seekBar_track);
        mPlaybackSeekBar.setTextViewCurrentTime((TextView) getView().findViewById(
                R.id.textView_currentTime));
        mPlaybackSeekBar.setTextViewCompletionTime((TextView) getView().findViewById(
                R.id.textView_completionTime));
        mPlaybackSeekBar.setPlaybackService(playbackService);

        refreshTrackInfo();
        refreshPlayPauseButtonState();
        refreshRepeatButtonState();
        refreshShuffleButtonState();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.findItem(R.id.action_saveplaylist_item).setVisible(true);

        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * If the user clicks on a menuItem, handle what should be done here
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        if (playbackService != null && item != null) {
            if (item.getItemId() == R.id.action_saveplaylist_item) {
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
            Object obj = tomahawkListAdapter.getItem(idx);
            if (obj instanceof Query) {
                // if the user clicked on an already playing track
                if (playbackService.getCurrentPlaylist().getCurrentQueryIndex() == idx) {
                    playbackService.playPause();
                } else {
                    playbackService.setCurrentQuery(
                            playbackService.getCurrentPlaylist().getQueryAtPos(idx));
                }
            }
        }
    }

    /**
     * If the listview header is clicked, scroll back to the top. Uses different ways of achieving
     * this depending on api level.
     */
    @Override
    public void onHeaderClick(se.emilsjolander.stickylistheaders.StickyListHeadersListView list,
            View header, int itemPosition,
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
     * If the PlaybackService signals, that it is ready, this method is being called
     */
    @Override
    public void onPlaybackServiceReady() {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        if (playbackService != null) {
            if (mAlbumArtSwipeAdapter != null && mPlaybackSeekBar != null) {
                mAlbumArtSwipeAdapter.setPlaybackService(playbackService);
                mPlaybackSeekBar.setPlaybackService(playbackService);
                refreshTrackInfo();
                refreshPlayPauseButtonState();
                refreshRepeatButtonState();
                refreshShuffleButtonState();
            }
        }
        onPlaylistChanged();
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver received a Broadcast indicating that the
     * track has changed inside our PlaybackService
     */
    @Override
    public void onTrackChanged() {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        TomahawkListAdapter tomahawkListAdapter = (TomahawkListAdapter) getListAdapter();
        if (tomahawkListAdapter != null && playbackService != null
                && playbackService.getCurrentPlaylist() != null
                && playbackService.getCurrentPlaylist().getCurrentQuery() != null) {
            tomahawkListAdapter.setHighlightedItem(
                    playbackService.getCurrentPlaylist().getCurrentQueryIndex());
            tomahawkListAdapter.setHighlightedItemIsPlaying(playbackService.isPlaying());
            tomahawkListAdapter.notifyDataSetChanged();
            Track currentTrack = playbackService.getCurrentQuery().getPreferredTrack();
            if (TextUtils.isEmpty(currentTrack.getAlbum().getAlbumArtPath())
                    && TextUtils.isEmpty(currentTrack.getArtist().getImage())) {
                ArrayList<String> requestIds = mInfoSystem.resolve(currentTrack.getArtist(), true);
                for (String requestId : requestIds) {
                    mCurrentRequestIds.add(requestId);
                }
                mCurrentRequestIds.add(mInfoSystem.resolve(currentTrack.getAlbum()));
            }
        }
        refreshTrackInfo();
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver received a Broadcast indicating that the
     * playlist has changed inside our PlaybackService
     */
    @Override
    public void onPlaylistChanged() {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        TomahawkListAdapter tomahawkListAdapter = (TomahawkListAdapter) getListAdapter();

        if (playbackService != null
                && playbackService.getCurrentPlaylist() != null
                && playbackService.getCurrentPlaylist().getCount() > 0) {
            mShownQueries = playbackService.getCurrentPlaylist().getQueries();
            resolveQueriesFromTo(getListView().getFirstVisiblePosition(),
                    getListView().getLastVisiblePosition() + 2);
        }
        if (tomahawkListAdapter != null) {
            if (playbackService != null && playbackService.getCurrentPlaylist() != null
                    && playbackService.getCurrentPlaylist().getCount() > 0) {
                ArrayList<TomahawkBaseAdapter.TomahawkListItem> tracks
                        = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
                tracks.addAll(playbackService.getCurrentPlaylist().getQueries());
                tomahawkListAdapter.setListWithIndex(0, tracks);
                tomahawkListAdapter.setHighlightedItem(
                        playbackService.getCurrentPlaylist().getCurrentQueryIndex());
                tomahawkListAdapter.setHighlightedItemIsPlaying(playbackService.isPlaying());
                tomahawkListAdapter.notifyDataSetChanged();
            }
        } else {
            initAdapter();
        }
        if (mAlbumArtSwipeAdapter != null) {
            mAlbumArtSwipeAdapter.updatePlaylist();
        }
        refreshRepeatButtonState();
        refreshShuffleButtonState();
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver in PlaybackFragment received a Broadcast
     * indicating that the playState (playing or paused) has changed inside our PlaybackService
     */
    @Override
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
        refreshPlayPauseButtonState();
        if (mPlaybackSeekBar != null) {
            mPlaybackSeekBar.updateSeekBarPosition();
        }
    }

    @Override
    protected void onPipeLineResultsReported(String qId) {
        if (mCorrespondingQueryIds.contains(qId)) {
            onPlaylistChanged();
        }
    }

    @Override
    protected void onInfoSystemResultsReported(String requestId) {
        if (mCurrentRequestIds.contains(requestId)) {
            mTomahawkMainActivity.getPlaybackService().updatePlayingNotification();
        }
    }

    /**
     * Initialize our listview adapter. Adds the current playlist's tracks, sets boolean variables
     * to customize the listview's appearance. Adds the PlaybackFragment to the top of the
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
            StickyListHeadersListView list = getListView();
            list.setOnItemClickListener(this);
            list.setOnHeaderClickListener(this);
            setListAdapter(tomahawkListAdapter);
        }
    }

    /**
     * Called when the play/pause button is clicked.
     */
    public void onPlayPauseClicked() {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        if (playbackService != null) {
            playbackService.playPause(true);
        }
    }

    /**
     * Called when the next button is clicked.
     */
    public void onNextClicked() {
        if (mAlbumArtSwipeAdapter != null) {
            mAlbumArtSwipeAdapter.setSwiped(false);
        }
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        if (playbackService != null) {
            playbackService.next();
        }
    }

    /**
     * Called when the previous button is clicked.
     */
    public void onPreviousClicked() {
        if (mAlbumArtSwipeAdapter != null) {
            mAlbumArtSwipeAdapter.setSwiped(false);
        }
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        if (playbackService != null) {
            playbackService.previous();
        }
    }

    /**
     * Called when the shuffle button is clicked.
     */
    public void onShuffleClicked() {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        if (playbackService != null) {
            playbackService.setShuffled(!playbackService.getCurrentPlaylist().isShuffled());

            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(getActivity(), getString(
                    playbackService.getCurrentPlaylist().isShuffled()
                            ? R.string.playbackactivity_toastshuffleon_string
                            : R.string.playbackactivity_toastshuffleoff_string),
                    Toast.LENGTH_SHORT);
            mToast.show();
        }
    }

    /**
     * Called when the repeat button is clicked.
     */
    public void onRepeatClicked() {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        if (playbackService != null) {
            playbackService.setRepeating(!playbackService.getCurrentPlaylist().isRepeating());

            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(getActivity(), getString(
                    playbackService.getCurrentPlaylist().isRepeating()
                            ? R.string.playbackactivity_toastrepeaton_string
                            : R.string.playbackactivity_toastrepeatoff_string), Toast.LENGTH_SHORT);
            mToast.show();
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the current Track, if possible
     * (meaning mPlaybackService is not null).
     */
    protected void refreshTrackInfo() {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        if (playbackService != null) {
            refreshTrackInfo(playbackService.getCurrentTrack());
        } else {
            refreshTrackInfo(null);
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the given Track.
     *
     * @param track the track to which the track info view stuff should be updated to
     */
    protected void refreshTrackInfo(Track track) {
        if (getView() != null) {
            PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
            if (track != null && playbackService != null) {
                /*
                This logic makes sure, that if a track is being skipped by the user, it doesn't do this
                for eternity. Because a press of the next button would cause the AlbumArtSwipeAdapter
                to display a swipe to the next track, which would then cause another skipping to the
                next track. That's why we have to make a difference between a swipe by the user, and a
                programmatically called swipe.
                */
                mAlbumArtSwipeAdapter.setPlaybackService(playbackService);
                if (!mAlbumArtSwipeAdapter.isSwiped()) {
                    mAlbumArtSwipeAdapter.setByUser(false);
                    if (playbackService.getCurrentPlaylist().getCurrentQueryIndex() >= 0) {
                        mAlbumArtSwipeAdapter.setCurrentItem(
                                playbackService.getCurrentPlaylist().getCurrentQueryIndex(), true);
                    }
                    mAlbumArtSwipeAdapter.setByUser(true);
                }
                mAlbumArtSwipeAdapter.setSwiped(false);

                // Make all buttons clickable
                getView().findViewById(R.id.imageButton_playpause).setClickable(true);
                getView().findViewById(R.id.imageButton_next).setClickable(true);
                getView().findViewById(R.id.imageButton_previous).setClickable(true);
                getView().findViewById(R.id.imageButton_shuffle).setClickable(true);
                getView().findViewById(R.id.imageButton_repeat).setClickable(true);

                // Update the PlaybackSeekBar
                mPlaybackSeekBar.setPlaybackService(playbackService);
                mPlaybackSeekBar.setMax();
                mPlaybackSeekBar.setUpdateInterval();
                mPlaybackSeekBar.updateSeekBarPosition();
                mPlaybackSeekBar.updateTextViewCompleteTime();
            } else {
                //No track has been given, so we update the view state accordingly

                // Make all buttons not clickable
                getView().findViewById(R.id.imageButton_playpause).setClickable(false);
                getView().findViewById(R.id.imageButton_next).setClickable(false);
                getView().findViewById(R.id.imageButton_previous).setClickable(false);
                getView().findViewById(R.id.imageButton_shuffle).setClickable(false);
                getView().findViewById(R.id.imageButton_repeat).setClickable(false);

                // Update the PlaybackSeekBar
                mPlaybackSeekBar.setEnabled(false);
                mPlaybackSeekBar.updateSeekBarPosition();
                mPlaybackSeekBar.updateTextViewCompleteTime();
            }
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the current play/pause-button
     * state.
     */
    protected void refreshPlayPauseButtonState() {
        if (getView() != null) {
            ImageButton imageButton = (ImageButton) getView()
                    .findViewById(R.id.imageButton_playpause);
            if (imageButton != null) {
                PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
                if (playbackService != null && playbackService.isPlaying()) {
                    imageButton
                            .setImageDrawable(
                                    getResources().getDrawable(R.drawable.ic_player_pause));
                } else {
                    imageButton.setImageDrawable(
                            getResources().getDrawable(R.drawable.ic_player_play));
                }
            }
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the current repeatButton state.
     */
    protected void refreshRepeatButtonState() {
        if (getView() != null) {
            ImageButton imageButton = (ImageButton) getView().findViewById(R.id.imageButton_repeat);
            if (imageButton != null && imageButton.getDrawable() != null) {
                PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
                if (playbackService != null && playbackService.getCurrentPlaylist() != null
                        && playbackService.getCurrentPlaylist().isRepeating()) {
                    imageButton.getDrawable()
                            .setColorFilter(getResources().getColor(R.color.pressed_tomahawk),
                                    PorterDuff.Mode.MULTIPLY);
                } else {
                    imageButton.getDrawable().clearColorFilter();
                }
            }
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the current shuffleButton state.
     */
    protected void refreshShuffleButtonState() {
        if (getView() != null) {
            ImageButton imageButton = (ImageButton) getView()
                    .findViewById(R.id.imageButton_shuffle);
            if (imageButton != null && imageButton.getDrawable() != null) {
                PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
                if (playbackService != null && playbackService.getCurrentPlaylist() != null
                        && playbackService.getCurrentPlaylist().isShuffled()) {
                    imageButton.getDrawable()
                            .setColorFilter(getResources().getColor(R.color.pressed_tomahawk),
                                    PorterDuff.Mode.MULTIPLY);
                } else {
                    imageButton.getDrawable().clearColorFilter();
                }
            }
        }
    }
}
