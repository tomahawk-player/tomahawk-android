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

import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.AlbumArtSwipeAdapter;
import org.tomahawk.tomahawk_android.adapters.PlaybackPagerAdapter;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.views.TomahawkVerticalViewPager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
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

    private TextView mQueueButton;

    private ViewPager mViewPager;

    private View mListViewFrame;

    private Toast mToast;

    private PlaybackFragmentReceiver mPlaybackFragmentReceiver;

    /**
     * Handles incoming broadcasts.
     */
    private class PlaybackFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (TomahawkMainActivity.SLIDING_LAYOUT_EXPANDED.equals(intent.getAction())) {
                mAlbumArtSwipeAdapter.notifyDataSetChanged();
            } else if (TomahawkMainActivity.SLIDING_LAYOUT_COLLAPSED.equals(intent.getAction())) {
                mAlbumArtSwipeAdapter.notifyDataSetChanged();
            }
        }

    }

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
        view.findViewById(R.id.imageButton_shuffle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onShuffleClicked();
            }
        });
        view.findViewById(R.id.imageButton_repeat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRepeatClicked();
            }
        });
        View closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View contextMenu = getView().findViewById(R.id.context_menu_framelayout);
                if (contextMenu.getVisibility() == View.VISIBLE) {
                    contextMenu.setVisibility(View.GONE);
                    getView().findViewById(R.id.view_album_button).setVisibility(View.GONE);
                    ((TomahawkMainActivity) getActivity()).getPlaybackPanel()
                            .findViewById(R.id.textview_container).setVisibility(View.VISIBLE);
                } else {
                    SlidingUpPanelLayout slidingLayout =
                            (SlidingUpPanelLayout) getActivity().findViewById(R.id.sliding_layout);
                    slidingLayout.collapsePanel();
                }
            }
        });
        TextView closeButtonText = (TextView) closeButton.findViewById(R.id.close_button_text);
        closeButtonText.setText(getString(R.string.button_close).toUpperCase());
        mViewPager = new ViewPager(TomahawkApp.getContext());
        mListViewFrame = getActivity().getLayoutInflater()
                .inflate(R.layout.listview_with_queue_button, null);
        FrameLayout listViewFrame = (FrameLayout) mListViewFrame.findViewById(R.id.listview_frame);
        listViewFrame.addView(getListView(), new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void onResume() {
        super.onResume();

        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();

        onPlaylistChanged();

        // Initialize and register Receiver
        if (mPlaybackFragmentReceiver == null) {
            mPlaybackFragmentReceiver = new PlaybackFragmentReceiver();
            IntentFilter intentFilter =
                    new IntentFilter(TomahawkMainActivity.SLIDING_LAYOUT_COLLAPSED);
            getActivity().registerReceiver(mPlaybackFragmentReceiver, intentFilter);
            intentFilter =
                    new IntentFilter(TomahawkMainActivity.SLIDING_LAYOUT_EXPANDED);
            getActivity().registerReceiver(mPlaybackFragmentReceiver, intentFilter);
        }

        mAlbumArtSwipeAdapter = new AlbumArtSwipeAdapter((TomahawkMainActivity) getActivity(),
                getActivity().getSupportFragmentManager(), getActivity().getLayoutInflater(),
                mViewPager, new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                getView().findViewById(R.id.context_menu_framelayout)
                        .setVisibility(View.VISIBLE);
                getView().findViewById(R.id.view_album_button).setVisibility(View.VISIBLE);
                if (getResources().getBoolean(R.bool.is_landscape)) {
                    ((TomahawkMainActivity) getActivity()).getPlaybackPanel()
                            .findViewById(R.id.textview_container).setVisibility(View.GONE);
                }
                return true;
            }
        });
        mAlbumArtSwipeAdapter.setPlaybackService(playbackService);
        mViewPager.setAdapter(mAlbumArtSwipeAdapter);
        mViewPager.setOnPageChangeListener(mAlbumArtSwipeAdapter);

        mQueueButton = (TextView) mListViewFrame.findViewById(R.id.button_open_queue);
        mQueueButton.setText(getString(R.string.button_open_queue_text).toUpperCase());
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

        mPlaybackPagerAdapter = new PlaybackPagerAdapter(mViewPager, mListViewFrame);
        mTomahawkVerticalViewPager = (TomahawkVerticalViewPager) getView()
                .findViewById(R.id.playback_view_pager);
        mTomahawkVerticalViewPager.setAdapter(mPlaybackPagerAdapter);
        mTomahawkVerticalViewPager.setStickyListHeadersListView(getListView());
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mTomahawkVerticalViewPager.setPageMargin(TomahawkUtils.convertDpToPixel(-24));
        }

        refreshTrackInfo();
        refreshRepeatButtonState();
        refreshShuffleButtonState();
        updateAdapter();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mPlaybackFragmentReceiver != null) {
            getActivity().unregisterReceiver(mPlaybackFragmentReceiver);
            mPlaybackFragmentReceiver = null;
        }
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
     * Called every time an item inside a ListView or GridView is long-clicked
     *
     * @param item the TomahawkListItem which corresponds to the long-click
     */
    @Override
    public boolean onItemLongClick(View view, TomahawkListItem item) {
        TomahawkListItem contextItem = null;
        if (mAlbum != null) {
            contextItem = mAlbum;
        } else if (mArtist != null) {
            contextItem = mArtist;
        } else if (mPlaylist != null) {
            contextItem = mPlaylist;
        }
        return FragmentUtils.showContextMenu((TomahawkMainActivity) getActivity(),
                getActivity().getSupportFragmentManager(), item, contextItem, true);
    }

    /**
     * If the PlaybackService signals, that it is ready, this method is being called
     */
    @Override
    public void onPlaybackServiceReady() {
        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (playbackService != null) {
            if (mAlbumArtSwipeAdapter != null) {
                mAlbumArtSwipeAdapter.setPlaybackService(playbackService);
                refreshTrackInfo();
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
                getListAdapter().setSegments(new Segment(tracks), getListView());
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
                tomahawkListAdapter.setShowDuration(true);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segment, getListView());
            }
        }

        updateShowPlaystate();
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
                    ? R.string.shuffle_on_label
                    : R.string.shuffle_off_label), Toast.LENGTH_SHORT);
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
                    ? R.string.repeat_on_label
                    : R.string.repeat_off_label), Toast.LENGTH_SHORT);
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
                getView().findViewById(R.id.imageButton_shuffle).setClickable(true);
                getView().findViewById(R.id.imageButton_repeat).setClickable(true);

                ImageView bgImageView =
                        (ImageView) getView().findViewById(R.id.background);
                TomahawkUtils.loadBlurredImageIntoImageView(TomahawkApp.getContext(), bgImageView,
                        playbackService.getCurrentQuery().getImage(),
                        Image.getSmallImageSize(), R.drawable.album_placeholder_grid);

                ContextMenuFragment.setupClickListeners((TomahawkMainActivity) getActivity(),
                        getView(), query, null, new ContextMenuFragment.Action() {
                            @Override
                            public void run() {
                                getView().findViewById(R.id.context_menu_framelayout)
                                        .setVisibility(View.GONE);
                                getView().findViewById(R.id.view_album_button)
                                        .setVisibility(View.GONE);
                                activity.getPlaybackPanel().findViewById(R.id.textview_container)
                                        .setVisibility(View.VISIBLE);
                            }
                        });
            } else {
                // Make all buttons not clickable
                getView().findViewById(R.id.imageButton_shuffle).setClickable(false);
                getView().findViewById(R.id.imageButton_repeat).setClickable(false);
            }
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the current repeatButton state.
     */
    protected void refreshRepeatButtonState() {
        if (getView() != null) {
            ImageButton imageButton = (ImageButton) getView().findViewById(R.id.imageButton_repeat);
            if (imageButton != null) {
                PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                        .getPlaybackService();
                if (playbackService != null && playbackService.isRepeating()) {
                    TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageButton,
                            R.drawable.ic_player_repeat_light);
                } else {
                    TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageButton,
                            R.drawable.ic_player_repeat);
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
            if (imageButton != null) {
                PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                        .getPlaybackService();
                if (playbackService != null && playbackService.isShuffled()) {
                    TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageButton,
                            R.drawable.ic_player_shuffle_light);
                } else {
                    TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageButton,
                            R.drawable.ic_player_shuffle);
                }
            }
        }
    }
}
