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

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.AlbumArtSwipeAdapter;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.AnimationUtils;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.views.AlbumArtViewPager;
import org.tomahawk.tomahawk_android.views.PlaybackFragmentFrame;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
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

    private AlbumArtViewPager mAlbumArtViewPager;

    private int mOriginalViewPagerHeight;

    private Toast mToast;

    public abstract static class ShowContextMenuListener {

        public abstract void onShowContextMenu(Query query);

        public abstract void onDoubleTap(Query query);
    }

    private final ShowContextMenuListener mShowContextMenuListener = new ShowContextMenuListener() {

        @Override
        public void onShowContextMenu(Query query) {
            FragmentUtils.showContextMenu((TomahawkMainActivity) getActivity(), query, null, true);
        }

        @Override
        public void onDoubleTap(Query query) {
            final ImageView imageView =
                    (ImageView) getView().findViewById(R.id.imageview_favorite_doubletap);
            if (DatabaseHelper.getInstance().isItemLoved(query)) {
                TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                        R.drawable.ic_action_unfavorite_large);
            } else {
                TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                        R.drawable.ic_action_favorite_large);
            }
            AnimationUtils.fade(imageView, AnimationUtils.DURATION_CONTEXTMENU, true);
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    AnimationUtils.fade(imageView, AnimationUtils.DURATION_CONTEXTMENU, false);
                }
            };
            new Handler().postDelayed(r, 2000);
            CollectionManager.getInstance().toggleLovedItem(query);
        }
    };

    @SuppressWarnings("unused")
    public void onEventMainThread(TomahawkMainActivity.SlidingLayoutChangedEvent event) {
        switch (event.mSlideState) {
            case COLLAPSED:
            case EXPANDED:
                if (mAlbumArtSwipeAdapter != null) {
                    mAlbumArtSwipeAdapter.notifyDataSetChanged();
                }
                if (getListView() != null) {
                    getListView().smoothScrollToPosition(0);
                }
                break;
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

        getListView().setFastScrollEnabled(false);

        mAlbumArtViewPager = (AlbumArtViewPager) view.findViewById(R.id.albumart_viewpager);
        mAlbumArtViewPager.setShowContextMenuListener(mShowContextMenuListener);
        mAlbumArtViewPager
                .setPlaybackService(((TomahawkMainActivity) getActivity()).getPlaybackService());

        PlaybackFragmentFrame playbackFragmentFrame = (PlaybackFragmentFrame) view.getParent();
        playbackFragmentFrame.setListView(getListView());
        playbackFragmentFrame.setPanelLayout(
                ((TomahawkMainActivity) getActivity()).getSlidingUpPanelLayout());

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
                SlidingUpPanelLayout slidingLayout =
                        ((TomahawkMainActivity) getActivity()).getSlidingUpPanelLayout();
                slidingLayout.collapsePanel();
            }
        });
        TextView closeButtonText = (TextView) closeButton.findViewById(R.id.close_button_text);
        closeButtonText.setText(getString(R.string.button_close).toUpperCase());

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (!preferences.getBoolean(
                TomahawkMainActivity.COACHMARK_PLAYBACKFRAGMENT_NAVIGATION_DISABLED, false)) {
            final View coachMark = TomahawkUtils.ensureInflation(view,
                    R.id.playbackfragment_navigation_coachmark_stub,
                    R.id.playbackfragment_navigation_coachmark);
            coachMark.findViewById(R.id.close_button).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            coachMark.setVisibility(View.GONE);
                        }
                    });
            coachMark.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();

        onPlaylistChanged();

        mAlbumArtSwipeAdapter = new AlbumArtSwipeAdapter((TomahawkMainActivity) getActivity(),
                getActivity().getLayoutInflater(), mAlbumArtViewPager);
        mAlbumArtSwipeAdapter.setPlaybackService(playbackService);
        mAlbumArtViewPager.setAdapter(mAlbumArtSwipeAdapter);

        setupAlbumArtAnimation();

        refreshTrackInfo();
        refreshRepeatButtonState();
        refreshShuffleButtonState();
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
     * @param item the Object which corresponds to the long-click
     */
    @Override
    public boolean onItemLongClick(View view, Object item) {
        if (item != null) {
            TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
            AnimationUtils
                    .fade(activity.getPlaybackPanel(), AnimationUtils.DURATION_CONTEXTMENU, false);
            return FragmentUtils
                    .showContextMenu((TomahawkMainActivity) getActivity(), item, null, false);
        }
        return false;
    }

    /**
     * If the PlaybackService signals, that it is ready, this method is being called
     */
    @Override
    public void onPlaybackServiceReady() {
        super.onPlaybackServiceReady();

        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (playbackService != null) {
            if (mAlbumArtSwipeAdapter != null) {
                mAlbumArtSwipeAdapter.setPlaybackService(playbackService);
                refreshTrackInfo();
                refreshRepeatButtonState();
                refreshShuffleButtonState();
            }
            mAlbumArtViewPager.setPlaybackService(playbackService);
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
        updateAdapter();
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
            mShownQueries = playbackService.getMergedPlaylist().getQueries();
            mResolveQueriesHandler.removeCallbacksAndMessages(null);
            mResolveQueriesHandler.sendEmptyMessage(RESOLVE_QUERIES_REPORTER_MSG);
        }
        updateAdapter();

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

    @Override
    public void onHeaderHeightChanged() {
        setupScrollableSpacer(mAlbumArtViewPager);
        setupNonScrollableSpacer();
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
            List<Segment> segments = new ArrayList<>();
            List entries = new ArrayList();
            entries.add(playbackService.getCurrentEntry());
            Segment segment = new Segment(entries);
            segment.setShowNumeration(true, 0);
            segments.add(segment);

            entries = new ArrayList();
            entries.addAll(playbackService.getQueue().getEntries());
            entries.remove(playbackService
                    .getCurrentEntry()); // don't show queue entry if currently playing
            segment = new Segment(entries);
            segment.setShowAsQueued(true);
            segments.add(segment);

            if (playbackService.getPlaylist().size() > 1) {
                entries = new ArrayList();
                int currentIndex;
                if (playbackService.getQueue().getEntries()
                        .contains(playbackService.getCurrentEntry())) {
                    currentIndex = playbackService.getQueueStartPos();
                } else {
                    currentIndex = playbackService.getPlaylist()
                            .getIndexOfEntry(playbackService.getCurrentEntry());
                }
                entries.addAll(playbackService.getPlaylist().getEntries()
                        .subList(Math.max(1, currentIndex + 1),
                                playbackService.getPlaylist().size()));
                segment = new Segment(entries);
                segment.setShowNumeration(true, 1);
                segments.add(segment);
            }
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(activity,
                        layoutInflater, segments, getListView(), this);
                tomahawkListAdapter.setShowPlaystate(true);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segments, getListView());
            }
        }

        updateShowPlaystate();
        forceAutoResolve();
        setupNonScrollableSpacer();
        setupScrollableSpacer(mAlbumArtViewPager);
    }

    private void setupAlbumArtAnimation() {
        if (mAlbumArtViewPager != null) {
            TomahawkUtils.afterViewGlobalLayout(new TomahawkUtils.ViewRunnable(mAlbumArtViewPager) {
                @Override
                public void run() {
                    if (mOriginalViewPagerHeight <= 0) {
                        mOriginalViewPagerHeight = mAlbumArtViewPager.getHeight();
                    }

                    // now calculate the animation goal and instantiate the animation
                    int playbackPanelHeight = TomahawkApp.getContext().getResources()
                            .getDimensionPixelSize(R.dimen.playback_panel_height);
                    ValueAnimator animator = ObjectAnimator
                            .ofFloat(getLayedOutView(), "y", playbackPanelHeight,
                                    getLayedOutView().getHeight() / -4)
                            .setDuration(10000);
                    animator.setInterpolator(new AccelerateDecelerateInterpolator());
                    addAnimator(animator);
                }
            });
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
                /*
                This logic makes sure, that if a track is being skipped by the user, it doesn't do this
                for eternity. Because a press of the next button would cause the AlbumArtSwipeAdapter
                to display a swipe to the next track, which would then cause another skipping to the
                next track. That's why we have to make a difference between a swipe by the user, and a
                programmatically called swipe.
                */
                mAlbumArtViewPager.setPlaybackService(playbackService);
                mAlbumArtSwipeAdapter.setPlaybackService(playbackService);

                // Make all buttons clickable
                getView().findViewById(R.id.imageButton_shuffle).setClickable(true);
                getView().findViewById(R.id.imageButton_repeat).setClickable(true);

                ImageView bgImageView =
                        (ImageView) getView().findViewById(R.id.background);
                TomahawkUtils.loadBlurredImageIntoImageView(TomahawkApp.getContext(), bgImageView,
                        playbackService.getCurrentQuery().getImage(),
                        Image.getSmallImageSize(), R.color.playerview_default_bg);
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
                    TomahawkUtils.setTint(imageButton.getDrawable(), R.color.tomahawk_red);
                } else {
                    TomahawkUtils.clearTint(imageButton.getDrawable());
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
                    TomahawkUtils.setTint(imageButton.getDrawable(), R.color.tomahawk_red);
                } else {
                    TomahawkUtils.clearTint(imageButton.getDrawable());
                }
            }
        }
    }

    @Override
    public void animate(int position) {
        super.animate(position);
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        if (activity.getSlidingOffset() > 0f) {
            activity.getPlaybackPanel().animate(position + 10000);
        }
    }
}
