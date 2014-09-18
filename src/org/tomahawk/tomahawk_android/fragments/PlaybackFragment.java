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

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.AlbumArtSwipeAdapter;
import org.tomahawk.tomahawk_android.adapters.PlaybackPagerAdapter;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.views.PlaybackSeekBar;
import org.tomahawk.tomahawk_android.views.TomahawkVerticalViewPager;

import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * This {@link android.support.v4.app.Fragment} represents our Playback view in which the user can
 * play/stop/pause. It is being shown as the topmost fragment in the {@link PlaybackFragment}'s
 * {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}.
 */
public class PlaybackFragment extends TomahawkFragment {

    private AlbumArtSwipeAdapter mAlbumArtSwipeAdapter;

    private PlaybackPagerAdapter mPlaybackPagerAdapter;

    private TomahawkVerticalViewPager mTomahawkVerticalViewPager;

    private View mQueueButton;

    private View mViewPagerFrame;

    private View mListViewFrame;

    private Menu mMenu;

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

        setRestoreScrollPosition(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playback_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mContainerFragmentClass == null) {
            getActivity().setTitle("");
        }

        //Set listeners on our buttons
        view.findViewById(R.id.imageButton_shuffle).setOnClickListener(mButtonClickListener);
        view.findViewById(R.id.imageButton_previous).setOnClickListener(mButtonClickListener);
        view.findViewById(R.id.imageButton_playpause).setOnClickListener(mButtonClickListener);
        view.findViewById(R.id.imageButton_next).setOnClickListener(mButtonClickListener);
        view.findViewById(R.id.imageButton_repeat).setOnClickListener(mButtonClickListener);
        mViewPagerFrame = getActivity().getLayoutInflater()
                .inflate(R.layout.album_art_view_pager, null);
        mListViewFrame = getActivity().getLayoutInflater()
                .inflate(R.layout.listview_with_queue_button, null);
        FrameLayout listViewFrame = (FrameLayout) mListViewFrame.findViewById(R.id.listview_frame);
        listViewFrame.addView(getListView(), new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void onResume() {
        super.onResume();

        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        PlaybackService playbackService = activity.getPlaybackService();

        onPlaylistChanged();

        //Setup SlidingUpPanelLayout
        SlidingUpPanelLayout slidingLayout =
                (SlidingUpPanelLayout) activity.findViewById(R.id.sliding_layout);
        View dragView = mViewPagerFrame.findViewById(R.id.sliding_layout_drag_view);
        slidingLayout.setDragView(dragView);

        ViewPager viewPager = (ViewPager) mViewPagerFrame.findViewById(R.id.album_art_view_pager);
        mAlbumArtSwipeAdapter = new AlbumArtSwipeAdapter(activity,
                activity.getSupportFragmentManager(), activity.getLayoutInflater(), viewPager,
                this, slidingLayout);
        mAlbumArtSwipeAdapter.setPlaybackService(playbackService);
        viewPager.setAdapter(mAlbumArtSwipeAdapter);
        viewPager.setOnPageChangeListener(mAlbumArtSwipeAdapter);

        mQueueButton = mListViewFrame.findViewById(R.id.button_open_queue);
        mQueueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTomahawkVerticalViewPager.getCurrentItem() == 0) {
                    mTomahawkVerticalViewPager.setCurrentItem(1, true);
                } else {
                    mTomahawkVerticalViewPager.setCurrentItem(0, true);
                }
            }
        });

        mPlaybackPagerAdapter = new PlaybackPagerAdapter(mViewPagerFrame, mListViewFrame);
        mTomahawkVerticalViewPager = (TomahawkVerticalViewPager) getView()
                .findViewById(R.id.playback_view_pager);
        mTomahawkVerticalViewPager.setAdapter(mPlaybackPagerAdapter);
        mTomahawkVerticalViewPager.setStickyListHeadersListView(getListView());
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mTomahawkVerticalViewPager.setPageMargin(TomahawkUtils.convertDpToPixel(-24));
        }

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
        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (playbackService != null) {
            if (item instanceof PlaylistEntry) {
                PlaylistEntry entry = (PlaylistEntry) item;
                if (entry.getQuery().isPlayable()) {
                    // if the user clicked on an already playing track
                    if (playbackService.getCurrentEntry() == entry) {
                        playbackService.playPause();
                    } else {
                        playbackService.setCurrentEntry(entry);
                    }
                }
            }
        }
    }

    /**
     * If the PlaybackService signals, that it is ready, this method is being called
     */
    @Override
    public void onPlaybackServiceReady() {
        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
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
        super.onTrackChanged();

        refreshTrackInfo();
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver received a Broadcast indicating that the
     * playlist has changed inside our PlaybackService
     */
    @Override
    public void onPlaylistChanged() {
        super.onPlaylistChanged();

        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();

        if (playbackService != null) {
            mShownQueries = playbackService.getQueue().getQueries();
            mResolveQueriesHandler.removeCallbacksAndMessages(null);
            mResolveQueriesHandler.sendEmptyMessage(RESOLVE_QUERIES_REPORTER_MSG);
        }
        if (getListAdapter() != null) {
            if (playbackService != null) {
                ArrayList<TomahawkListItem> tracks = new ArrayList<TomahawkListItem>();
                tracks.addAll(playbackService.getQueue().getQueries());
                getListAdapter().setSegments(new Segment(tracks));
                getListAdapter().notifyDataSetChanged();
            }
        } else {
            updateAdapter();
        }
        refreshRepeatButtonState();
        refreshShuffleButtonState();
        if (mAlbumArtSwipeAdapter != null) {
            mAlbumArtSwipeAdapter.updatePlaylist();
        }
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver in PlaybackFragment received a Broadcast
     * indicating that the playState (playing or paused) has changed inside our PlaybackService
     */
    @Override
    public void onPlaystateChanged() {
        super.onPlaystateChanged();

        refreshPlayPauseButtonState();
        if (mPlaybackSeekBar != null) {
            PlaybackService playbackService =
                    ((TomahawkMainActivity) getActivity()).getPlaybackService();
            if (playbackService != null && playbackService.isPlaying()) {
                mPlaybackSeekBar.updateSeekBarPosition();
            } else {
                mPlaybackSeekBar.stopUpdates();
            }
        }
        if (mAlbumArtSwipeAdapter != null) {
            mAlbumArtSwipeAdapter.updatePlaylist();
        }
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    @Override
    protected void updateAdapter() {
        if (!mIsResumed) {
            return;
        }

        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        PlaybackService playbackService = activity.getPlaybackService();
        if (playbackService != null) {
            List<TomahawkListItem> entries = new ArrayList<TomahawkListItem>();
            entries.addAll(playbackService.getQueue().getEntries());
            Segment segment = new Segment(entries);
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(activity,
                        layoutInflater, segment, this);
                tomahawkListAdapter.setShowPlaystate(true);
                tomahawkListAdapter.setShowResolvedBy(true);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segment);
            }
        }

        updateShowPlaystate();
    }

    /**
     * Called when the play/pause button is clicked.
     */
    public void onPlayPauseClicked() {
        final PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
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
        final PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
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
        final PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (playbackService != null) {
            playbackService.previous();
        }
    }

    /**
     * Called when the shuffle button is clicked.
     */
    public void onShuffleClicked() {
        final PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (playbackService != null) {
            playbackService.setShuffled(!playbackService.isShuffled());

            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(getActivity(), getString(playbackService.isShuffled()
                    ? R.string.playbackactivity_toastshuffleon_string
                    : R.string.playbackactivity_toastshuffleoff_string), Toast.LENGTH_SHORT);
            mToast.show();
        }
    }

    /**
     * Called when the repeat button is clicked.
     */
    public void onRepeatClicked() {
        final PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (playbackService != null) {
            playbackService.setRepeating(!playbackService.isRepeating());

            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(getActivity(), getString(playbackService.isRepeating()
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
        final PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (playbackService != null) {
            refreshTrackInfo(playbackService.getCurrentQuery());
        } else {
            refreshTrackInfo(null);
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the given Track.
     *
     * @param query the query to which the track info view stuff should be updated to
     */
    protected void refreshTrackInfo(final Query query) {
        if (getView() != null) {
            final TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
            final PlaybackService playbackService = activity.getPlaybackService();
            if (query != null && playbackService != null) {
                if (playbackService.getQueue().size() > 0) {
                    mQueueButton.setVisibility(View.VISIBLE);
                    mTomahawkVerticalViewPager.setPagingEnabled(true);
                } else if (mTomahawkVerticalViewPager.isPagingEnabled()) {
                    mTomahawkVerticalViewPager.setCurrentItem(0);
                    mQueueButton.setVisibility(View.GONE);
                    mTomahawkVerticalViewPager.setPagingEnabled(false);
                }
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
                    mAlbumArtSwipeAdapter.setCurrentItem(playbackService.getCurrentEntry(), true);
                    mAlbumArtSwipeAdapter.setByUser(true);
                }

                // Make all buttons clickable
                getView().findViewById(R.id.imageButton_playpause).setClickable(true);
                getView().findViewById(R.id.imageButton_next).setClickable(true);
                getView().findViewById(R.id.imageButton_previous).setClickable(true);
                getView().findViewById(R.id.imageButton_shuffle).setClickable(true);
                getView().findViewById(R.id.imageButton_repeat).setClickable(true);

                // Update the PlaybackSeekBar
                mPlaybackSeekBar.setPlaybackService(playbackService);
                mPlaybackSeekBar.setMax();
                mPlaybackSeekBar.updateSeekBarPosition();
                mPlaybackSeekBar.updateTextViewCompleteTime();

            } else {
                // Make all buttons not clickable
                getView().findViewById(R.id.imageButton_playpause).setClickable(false);
                getView().findViewById(R.id.imageButton_next).setClickable(false);
                getView().findViewById(R.id.imageButton_previous).setClickable(false);
                getView().findViewById(R.id.imageButton_shuffle).setClickable(false);
                getView().findViewById(R.id.imageButton_repeat).setClickable(false);

                // Update the PlaybackSeekBar
                if (mPlaybackSeekBar != null) {
                    mPlaybackSeekBar.setEnabled(false);
                    mPlaybackSeekBar.updateSeekBarPosition();
                    mPlaybackSeekBar.updateTextViewCompleteTime();
                }
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
                PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                        .getPlaybackService();
                if (playbackService != null && playbackService.isPlaying()) {
                    TomahawkUtils.loadDrawableIntoImageView(getActivity(), imageButton,
                            R.drawable.ic_player_pause);
                } else {
                    TomahawkUtils.loadDrawableIntoImageView(getActivity(), imageButton,
                            R.drawable.ic_player_play);
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
                PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                        .getPlaybackService();
                if (playbackService != null && playbackService.isRepeating()) {
                    imageButton.getDrawable()
                            .setColorFilter(getResources().getColor(R.color.tomahawk_red),
                                    PorterDuff.Mode.SRC_IN);
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
                PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                        .getPlaybackService();
                if (playbackService != null && playbackService.isShuffled()) {
                    imageButton.getDrawable()
                            .setColorFilter(getResources().getColor(R.color.tomahawk_red),
                                    PorterDuff.Mode.SRC_IN);
                } else {
                    imageButton.getDrawable().clearColorFilter();
                }
            }
        }
    }

    @Override
    public void onPanelCollapsed() {
        mAlbumArtSwipeAdapter.notifyDataSetChanged();
        mPlaybackSeekBar.stopUpdates();
    }

    @Override
    public void onPanelExpanded() {
        mAlbumArtSwipeAdapter.notifyDataSetChanged();
        mPlaybackSeekBar.updateSeekBarPosition();
    }
}
