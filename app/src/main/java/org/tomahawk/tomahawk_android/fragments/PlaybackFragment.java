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
import com.squareup.picasso.Callback;

import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.StationPlaylist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.ImageUtils;
import org.tomahawk.libtomahawk.utils.ViewUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.AlbumArtSwipeAdapter;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.AnimationUtils;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.PlaybackManager;
import org.tomahawk.tomahawk_android.views.AlbumArtViewPager;
import org.tomahawk.tomahawk_android.views.PlaybackFragmentFrame;

import android.animation.Animator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * This {@link android.support.v4.app.Fragment} represents our Playback view in which the user can
 * play/stop/pause. It is being shown as the topmost fragment in the {@link PlaybackFragment}'s
 * {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}.
 */
public class PlaybackFragment extends TomahawkFragment {

    private static final String TAG = PlaybackFragment.class.getSimpleName();

    private AlbumArtSwipeAdapter mAlbumArtSwipeAdapter;

    private AlbumArtViewPager mAlbumArtViewPager;

    private FrameLayout mAlbumArtViewPagerFrame;

    private ImageView mSwipeHintLeft;

    private ImageView mSwipeHintRight;

    private ImageView mSwipeHintBottom;

    private boolean mSwipeHintsShown = false;

    private boolean mShouldShowSwipeHints = false;

    private int mOriginalViewPagerHeight;

    private Image mCurrentBlurredImage;

    private final GestureDetector.SimpleOnGestureListener mGestureListener
            = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public void onLongPress(MotionEvent e) {
            if (getMediaController() == null) {
                Log.d(TAG, "onLongPress failed because getMediaController() is null");
                return;
            }
            FragmentUtils.showContextMenu((TomahawkMainActivity) getActivity(),
                    getPlaybackManager().getCurrentEntry().getQuery(), null, true, true);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (getMediaController() == null) {
                Log.d(TAG, "onDoubleTap failed because getMediaController() is null");
                return false;
            }
            final ImageView imageView =
                    (ImageView) getView().findViewById(R.id.imageview_favorite_doubletap);
            if (DatabaseHelper.get().isItemLoved(getPlaybackManager().getCurrentQuery())) {
                ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                        R.drawable.ic_action_unfavorite_large);
            } else {
                ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
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
            CollectionManager.get().toggleLovedItem(getPlaybackManager().getCurrentQuery());
            return false;
        }
    };

    @SuppressWarnings("unused")
    public void onEventMainThread(InfoSystem.ResultsEvent event) {
        if (mCorrespondingRequestIds.contains(event.mInfoRequestData.getRequestId())) {
            mAlbumArtSwipeAdapter.updatePlaylist();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(TomahawkMainActivity.SlidingLayoutChangedEvent event) {
        if (event.mSlideState == SlidingUpPanelLayout.PanelState.EXPANDED
                || event.mSlideState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            if (mAlbumArtSwipeAdapter != null) {
                mAlbumArtSwipeAdapter.notifyDataSetChanged();
            }
            if (getListView() != null) {
                getListView().smoothScrollToPosition(0);
            }
        }
        if (event.mSlideState == SlidingUpPanelLayout.PanelState.EXPANDED) {
            if (mShouldShowSwipeHints) {
                mShouldShowSwipeHints = false;
                if (getMediaController() != null) {
                    showSwipeHints(getMediaController().getPlaybackState());
                }
            }
        }
        if (event.mSlideState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            mShouldShowSwipeHints = true;
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

        ViewUtils.afterViewGlobalLayout(new ViewUtils.ViewRunnable(view) {
            @Override
            public void run() {
                if (getListView() != null) {
                    mHeaderScrollableHeight =
                            getLayedOutView().getHeight() - mHeaderNonscrollableHeight;
                    setupScrollableSpacer(getListAdapter(), getListView(), mAlbumArtViewPager);
                    setupNonScrollableSpacer(getListView());
                }
            }
        });

        getListView().setFastScrollEnabled(false);

        mSwipeHintLeft = (ImageView) view.findViewById(R.id.swipe_hint_left);
        mSwipeHintRight = (ImageView) view.findViewById(R.id.swipe_hint_right);
        mSwipeHintBottom = (ImageView) view.findViewById(R.id.swipe_hint_bottom);

        mAlbumArtViewPagerFrame = (FrameLayout) view.findViewById(R.id.albumart_viewpager_frame);

        mAlbumArtViewPager = (AlbumArtViewPager)
                mAlbumArtViewPagerFrame.findViewById(R.id.albumart_viewpager);
        int padding = getResources().getDimensionPixelSize(R.dimen.padding_large);
        mAlbumArtViewPager.setPadding(padding, 0, padding, 0);
        mAlbumArtViewPager.setClipToPadding(false);
        padding = getResources().getDimensionPixelSize(R.dimen.padding_large);
        mAlbumArtViewPager.setPageMargin(padding);
        mAlbumArtViewPager.setOnGestureListener(mGestureListener);

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
                slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });
        TextView closeButtonText = (TextView) closeButton.findViewById(R.id.close_button_text);
        closeButtonText.setText(getString(R.string.button_close).toUpperCase());

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (!preferences.getBoolean(
                TomahawkMainActivity.COACHMARK_PLAYBACKFRAGMENT_NAVIGATION_DISABLED, false)) {
            final View coachMark = ViewUtils.ensureInflation(view,
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

        mAlbumArtSwipeAdapter = new AlbumArtSwipeAdapter(mAlbumArtViewPager);
        mAlbumArtViewPager.setAdapter(mAlbumArtSwipeAdapter);

        setupAlbumArtAnimation();

        refreshAll();
    }

    private void refreshAll() {
        if (getMediaController() != null && mAlbumArtSwipeAdapter != null) {
            mAlbumArtSwipeAdapter.setMediaController(getMediaController());
            mAlbumArtSwipeAdapter.setPlaybackManager(getPlaybackManager());
            mAlbumArtSwipeAdapter.updatePlaylist();
            refreshTrackInfo(getMediaController().getMetadata());
            refreshRepeatButtonState(getMediaController().getPlaybackState());
            refreshShuffleButtonState(getMediaController().getPlaybackState());
            updateAdapter();
        }
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param view the clicked view
     * @param item the Object which corresponds to the click
     */
    @Override
    public void onItemClick(View view, Object item) {
        if (getMediaController() == null) {
            Log.d(TAG, "onItemClick failed because getMediaController() is null");
            return;
        }
        if (item instanceof PlaylistEntry) {
            PlaylistEntry entry = (PlaylistEntry) item;
            if (entry.getQuery().isPlayable()) {
                if (getPlaybackManager().getCurrentEntry() == entry) {
                    // if the user clicked on an already playing track
                    int playState = getMediaController().getPlaybackState().getState();
                    if (playState == PlaybackStateCompat.STATE_PLAYING) {
                        getMediaController().getTransportControls().pause();
                    } else if (playState == PlaybackStateCompat.STATE_PAUSED) {
                        getMediaController().getTransportControls().play();
                    }
                } else {
                    getPlaybackManager().setCurrentEntry(entry);
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
                    .showContextMenu((TomahawkMainActivity) getActivity(), item, null, false, true);
        }
        return false;
    }

    @Override
    public void onMediaControllerConnected() {
        super.onMediaControllerConnected();

        refreshAll();
    }

    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {
        refreshTrackInfo(metadata);
        scheduleUpdateAdapter();
    }

    @Override
    public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
        forceResolveVisibleItems(false);
        scheduleUpdateAdapter();

        if (getMediaController() != null) {
            refreshRepeatButtonState(getMediaController().getPlaybackState());
            refreshShuffleButtonState(getMediaController().getPlaybackState());
        }
        if (mAlbumArtSwipeAdapter != null) {
            mAlbumArtSwipeAdapter.updatePlaylist();
        }
    }

    @Override
    public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
        if (getMediaController() != null) {
            refreshRepeatButtonState(state);
            refreshShuffleButtonState(state);
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
            Log.e(TAG, "updateAdapter failed because Fragment is not resumed");
            return;
        }
        if (getMediaController() == null) {
            Log.e(TAG, "updateAdapter failed because getMediaController() is null");
            return;
        }

        if (getPlaybackManager() != null) {
            List<Segment> segments = new ArrayList<>();
            Segment segment = new Segment.Builder(getPlaybackManager())
                    .showNumeration(true, 0)
                    .build();
            segments.add(segment);
            fillAdapter(segments, mAlbumArtViewPager, null);
        }
    }

    private void setupAlbumArtAnimation() {
        if (mAlbumArtViewPagerFrame != null) {
            ViewUtils.afterViewGlobalLayout(new ViewUtils.ViewRunnable(mAlbumArtViewPagerFrame) {
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
                    addAnimator(ANIM_ALBUMART_ID, animator);

                    refreshAnimations();
                }
            });
        }
    }

    /**
     * Called when the shuffle button is clicked.
     */
    public void onShuffleClicked() {
        if (getMediaController() == null) {
            Log.e(TAG, "onShuffleClicked failed because getMediaController() is null");
            return;
        }
        Bundle playbackStateExtras = getMediaController().getPlaybackState().getExtras();
        if (playbackStateExtras != null) {
            int shuffleMode = playbackStateExtras.getInt(PlaybackService.EXTRAS_KEY_SHUFFLE_MODE);
            int newShuffleMode = shuffleMode == PlaybackManager.SHUFFLED
                    ? PlaybackManager.NOT_SHUFFLED : PlaybackManager.SHUFFLED;
            Bundle extras = new Bundle();
            extras.putInt(PlaybackService.EXTRAS_KEY_SHUFFLE_MODE, newShuffleMode);
            getMediaController().getTransportControls()
                    .sendCustomAction(PlaybackService.ACTION_SET_SHUFFLE_MODE, extras);
        }
    }

    /**
     * Called when the repeat button is clicked.
     */
    public void onRepeatClicked() {
        if (getMediaController() == null) {
            Log.e(TAG, "onRepeatClicked failed because getMediaController() is null");
            return;
        }
        Bundle playbackStateExtras = getMediaController().getPlaybackState().getExtras();
        if (playbackStateExtras != null) {
            int repeatMode = playbackStateExtras.getInt(PlaybackService.EXTRAS_KEY_REPEAT_MODE);
            int newRepeatMode = PlaybackManager.NOT_REPEATING;
            if (repeatMode == PlaybackManager.NOT_REPEATING) {
                newRepeatMode = PlaybackManager.REPEAT_ALL;
            } else if (repeatMode == PlaybackManager.REPEAT_ALL) {
                newRepeatMode = PlaybackManager.REPEAT_ONE;
            } else if (repeatMode == PlaybackManager.REPEAT_ONE) {
                newRepeatMode = PlaybackManager.NOT_REPEATING;
            }
            Bundle extras = new Bundle();
            extras.putInt(PlaybackService.EXTRAS_KEY_REPEAT_MODE, newRepeatMode);
            getMediaController().getTransportControls()
                    .sendCustomAction(PlaybackService.ACTION_SET_REPEAT_MODE, extras);
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the given Track.
     */
    protected void refreshTrackInfo(MediaMetadataCompat metadata) {
        if (getView() != null && metadata != null
                && getPlaybackManager().getCurrentQuery() != null) {
            if (getPlaybackManager().getPreviousEntry() != null) {
                resolveImages(getPlaybackManager().getPreviousEntry().getQuery());
            }
            if (getPlaybackManager().getNextEntry() != null) {
                resolveImages(getPlaybackManager().getNextEntry().getQuery());
            }
            if (mCurrentBlurredImage != getPlaybackManager().getCurrentQuery().getImage()) {
                mCurrentBlurredImage = getPlaybackManager().getCurrentQuery().getImage();
                ImageView bgImageView = (ImageView) getView().findViewById(R.id.background);
                ImageView bgAltImageView = (ImageView) getView().findViewById(R.id.background_alt);
                final ImageView imageViewToFadeIn;
                final ImageView imageViewToFadeOut;
                if (bgAltImageView.getAlpha() < bgImageView.getAlpha()) {
                    imageViewToFadeIn = bgAltImageView;
                    imageViewToFadeOut = bgImageView;
                } else {
                    imageViewToFadeIn = bgImageView;
                    imageViewToFadeOut = bgAltImageView;
                }
                Callback fadeCallback = new Callback() {
                    @Override
                    public void onSuccess() {
                        AnimationUtils.fade(imageViewToFadeIn, imageViewToFadeIn.getAlpha(),
                                1f, AnimationUtils.DURATION_PLAYBACKFRAGMENT_BG, true, null);
                        AnimationUtils.fade(imageViewToFadeOut, imageViewToFadeOut.getAlpha(),
                                0f, AnimationUtils.DURATION_PLAYBACKFRAGMENT_BG, false,
                                new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animation) {
                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        imageViewToFadeOut.setImageDrawable(null);
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animation) {
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animation) {
                                    }
                                });
                    }

                    @Override
                    public void onError() {
                    }
                };
                ImageUtils.loadBlurredImageIntoImageView(TomahawkApp.getContext(),
                        imageViewToFadeIn, mCurrentBlurredImage, Image.getSmallImageSize(),
                        R.color.playerview_default_bg, fadeCallback);
            }
        }
    }

    private void resolveImages(Query query) {
        if (query.getImage() == null) {
            String requestId = InfoSystem.get().resolve(query.getArtist(), false);
            if (requestId != null) {
                mCorrespondingRequestIds.add(requestId);
            }
            requestId = InfoSystem.get().resolve(query.getAlbum());
            if (requestId != null) {
                mCorrespondingRequestIds.add(requestId);
            }
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the current repeatButton state.
     */
    protected void refreshRepeatButtonState(PlaybackStateCompat playbackState) {
        if (getView() != null) {
            ImageButton imageButton = (ImageButton) getView().findViewById(R.id.imageButton_repeat);
            if (imageButton != null) {
                if (playbackState.getExtras() != null
                        && !(getPlaybackManager().getPlaylist() instanceof StationPlaylist)
                        && getPlaybackManager().getCurrentEntry() != null) {
                    imageButton.setAlpha(1f);
                    imageButton.setClickable(true);
                    int repeatMode = playbackState.getExtras()
                            .getInt(PlaybackService.EXTRAS_KEY_REPEAT_MODE);
                    if (repeatMode == PlaybackManager.REPEAT_ALL) {
                        ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(),
                                imageButton, R.drawable.repeat_all, R.color.tomahawk_red);
                    } else if (repeatMode == PlaybackManager.REPEAT_ONE) {
                        ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(),
                                imageButton, R.drawable.repeat_one, R.color.tomahawk_red);
                    } else if (repeatMode == PlaybackManager.NOT_REPEATING) {
                        ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(),
                                imageButton, R.drawable.repeat_all);
                    }
                } else {
                    imageButton.setAlpha(0.2f);
                    imageButton.setClickable(false);
                }
            }
        }
    }

    /**
     * Refresh the information in this fragment to reflect that of the current shuffleButton state.
     */
    protected void refreshShuffleButtonState(PlaybackStateCompat playbackState) {
        if (getView() != null) {
            ImageButton imageButton =
                    (ImageButton) getView().findViewById(R.id.imageButton_shuffle);
            if (imageButton != null) {
                if (playbackState.getExtras() != null
                        && !(getPlaybackManager().getPlaylist() instanceof StationPlaylist)
                        && getPlaybackManager().getCurrentEntry() != null) {
                    imageButton.setAlpha(1f);
                    imageButton.setClickable(true);
                    int repeatMode = playbackState.getExtras()
                            .getInt(PlaybackService.EXTRAS_KEY_SHUFFLE_MODE);
                    if (repeatMode == PlaybackManager.SHUFFLED) {
                        ImageUtils.setTint(imageButton.getDrawable(), R.color.tomahawk_red);
                    } else if (repeatMode == PlaybackManager.NOT_SHUFFLED) {
                        ImageUtils.clearTint(imageButton.getDrawable());
                    }
                } else {
                    imageButton.setAlpha(0.2f);
                    imageButton.setClickable(false);
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

    public void showSwipeHints(PlaybackStateCompat playbackState) {
        if (!mSwipeHintsShown && mSwipeHintLeft != null && mSwipeHintRight != null
                && mSwipeHintBottom != null) {
            mSwipeHintsShown = true;
            AnimationUtils.fade(mSwipeHintBottom, AnimationUtils.DURATION_PLAYBACKTOPPANEL, true);
            long actions = playbackState.getActions();
            final boolean hasPreviousEntry =
                    (actions & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0;
            final boolean hasNextEntry =
                    (actions & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0;
            if (hasPreviousEntry) {
                AnimationUtils.fade(mSwipeHintLeft, AnimationUtils.DURATION_PLAYBACKTOPPANEL, true);
            }
            if (hasNextEntry) {
                AnimationUtils.fade(
                        mSwipeHintRight, AnimationUtils.DURATION_PLAYBACKTOPPANEL, true);
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSwipeHintsShown = false;
                    if (hasPreviousEntry) {
                        AnimationUtils.fade(
                                mSwipeHintLeft, AnimationUtils.DURATION_PLAYBACKTOPPANEL, false);
                    }
                    if (hasNextEntry) {
                        AnimationUtils.fade(
                                mSwipeHintRight, AnimationUtils.DURATION_PLAYBACKTOPPANEL, false);
                    }
                    AnimationUtils.fade(
                            mSwipeHintBottom, AnimationUtils.DURATION_PLAYBACKTOPPANEL, false);
                }
            }, 1500);
        }
    }
}
