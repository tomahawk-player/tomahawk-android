/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.animation.ValueAnimator;

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.StationPlaylist;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.ImageUtils;
import org.tomahawk.libtomahawk.utils.ViewUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.adapters.ViewHolder;
import org.tomahawk.tomahawk_android.listeners.OnSizeChangedListener;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.PlaybackManager;
import org.tomahawk.tomahawk_android.views.FancyDropDown;
import org.tomahawk.tomahawk_android.views.PageIndicator;

import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.util.Pair;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ContentHeaderFragment extends Fragment {

    private static final String TAG = ContentHeaderFragment.class.getSimpleName();

    public static final String COLLECTION_ID = "collection_id";

    public static final String CONTENT_HEADER_MODE = "content_header_mode";

    public static final String CONTAINER_FRAGMENT_ID = "container_fragment_id";

    public static final String CONTAINER_FRAGMENT_PAGE = "container_fragment_page";

    public static final int MODE_HEADER_DYNAMIC = 0;

    public static final int MODE_HEADER_DYNAMIC_PAGER = 1;

    public static final int MODE_HEADER_STATIC = 2;

    public static final int MODE_HEADER_STATIC_USER = 3;

    public static final int MODE_ACTIONBAR_FILLED = 4;

    public static final int MODE_HEADER_STATIC_SMALL = 5;

    public static final int MODE_HEADER_PLAYBACK = 6;

    public static final int MODE_HEADER_STATIC_CHARTS = 7;

    public static final int ANIM_BUTTON_ID = 0;

    public static final int ANIM_FANCYDROPDOWN_ID = 1;

    public static final int ANIM_IMAGEVIEW_ID = 2;

    public static final int ANIM_ALBUMART_ID = 3;

    public static final int ANIM_PAGEINDICATOR_ID = 4;

    public static class AnimateEvent {

        public int mPlayTime;

        public long mContainerFragmentId;

        public int mContainerFragmentPage;
    }

    public static class PerformSyncEvent {

        public int mContainerFragmentPage;

        public long mContainerFragmentId;

        public int mFirstVisiblePosition;

        public int mTop;
    }

    public static class RequestSyncEvent {

        public long mContainerFragmentId;

        public int mPerformerFragmentPage;

        public int mReceiverFragmentPage;
    }

    public static class MediaControllerConnectedEvent {

    }

    private final SparseArray<ValueAnimator> mAnimators = new SparseArray<>();

    protected boolean mShowFakeFollowing = false;

    protected boolean mShowFakeNotFollowing = false;

    protected int mHeaderScrollableHeight = 0;

    protected int mHeaderNonscrollableHeight = 0;

    private int mCurrentMode = -1;

    protected View.OnClickListener mFollowButtonListener;

    private int mLastPlayTime;

    protected long mContainerFragmentId = -1;

    protected int mContainerFragmentPage = -1;

    protected Collection mCollection;

    protected boolean mHideRemoveButton;

    private PlaybackManager mPlaybackManager;

    @SuppressWarnings("unused")
    public void onEventMainThread(MediaControllerConnectedEvent event) {
        if (getMediaController() != null) {
            String playbackManagerId = getMediaController().getExtras()
                    .getString(PlaybackService.EXTRAS_KEY_PLAYBACKMANAGER);
            mPlaybackManager = PlaybackManager.getByKey(playbackManagerId);
            onMediaControllerConnected();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources res = getResources();
        if (getArguments() != null) {
            mCurrentMode = getArguments().getInt(TomahawkFragment.CONTENT_HEADER_MODE, -1);

            switch (mCurrentMode) {
                case MODE_HEADER_DYNAMIC:
                    mHeaderScrollableHeight = res.getDimensionPixelSize(
                            R.dimen.header_clear_space_scrollable);
                    mHeaderNonscrollableHeight = res.getDimensionPixelSize(
                            R.dimen.header_clear_space_nonscrollable);
                    break;
                case MODE_HEADER_DYNAMIC_PAGER:
                    mHeaderScrollableHeight = res.getDimensionPixelSize(
                            R.dimen.header_clear_space_scrollable);
                    mHeaderNonscrollableHeight =
                            res.getDimensionPixelSize(R.dimen.header_clear_space_nonscrollable)
                                    + res.getDimensionPixelSize(R.dimen.pager_indicator_height);
                    break;
                case MODE_HEADER_STATIC:
                    mHeaderNonscrollableHeight = res.getDimensionPixelSize(
                            R.dimen.header_clear_space_nonscrollable_static);
                    break;
                case MODE_HEADER_STATIC_USER:
                    mHeaderNonscrollableHeight = res.getDimensionPixelSize(
                            R.dimen.header_clear_space_nonscrollable_static_user);
                    break;
                case MODE_HEADER_STATIC_SMALL:
                    mHeaderNonscrollableHeight = res.getDimensionPixelSize(
                            R.dimen.abc_action_bar_default_height_material)
                            + res.getDimensionPixelSize(R.dimen.pager_indicator_height);
                    break;
                case MODE_HEADER_STATIC_CHARTS:
                    mHeaderNonscrollableHeight = res.getDimensionPixelSize(
                            R.dimen.header_clear_space_nonscrollable);
                    break;
                case MODE_ACTIONBAR_FILLED:
                    mHeaderNonscrollableHeight = res.getDimensionPixelSize(
                            R.dimen.abc_action_bar_default_height_material);
                    break;
                case MODE_HEADER_PLAYBACK:
                    mHeaderNonscrollableHeight = res.getDimensionPixelSize(
                            R.dimen.header_clear_space_nonscrollable_playback);
                    break;
                default:
                    throw new RuntimeException("Missing or invalid ContentHeaderFragment mode");
            }
            if (getArguments().containsKey(TomahawkFragment.CONTAINER_FRAGMENT_ID)) {
                mContainerFragmentId = getArguments()
                        .getLong(TomahawkFragment.CONTAINER_FRAGMENT_ID);
            }
            if (getArguments().containsKey(TomahawkFragment.CONTAINER_FRAGMENT_PAGE)) {
                mContainerFragmentPage = getArguments().getInt(
                        TomahawkFragment.CONTAINER_FRAGMENT_PAGE);
            }
        }

        if (getMediaController() != null) {
            String playbackManagerId = getMediaController().getExtras().getString(
                    PlaybackService.EXTRAS_KEY_PLAYBACKMANAGER);
            mPlaybackManager = PlaybackManager.getByKey(playbackManagerId);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCurrentMode == MODE_ACTIONBAR_FILLED) {
            ((TomahawkMainActivity) getActivity()).showFilledActionBar();
        } else if (mCurrentMode == MODE_HEADER_STATIC_SMALL) {
            ((TomahawkMainActivity) getActivity()).showGradientActionBar();
        }

        if (getArguments() != null) {
            if (getArguments().containsKey(COLLECTION_ID)) {
                String collectionId = getArguments().getString(TomahawkFragment.COLLECTION_ID);
                mCollection = CollectionManager.get().getCollection(collectionId);
            } else {
                mCollection = CollectionManager.get().getHatchetCollection();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        ((TomahawkMainActivity) getActivity()).showGradientActionBar();
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);

        super.onStop();
    }

    protected void onMediaControllerConnected() {
    }

    public MediaControllerCompat getMediaController() {
        if (getActivity() != null) {
            return getActivity().getSupportMediaController();
        }
        Log.e(TAG, "getActivity() was null, couldn't get MediaController!");
        return null;
    }

    public PlaybackManager getPlaybackManager() {
        return mPlaybackManager;
    }

    public boolean isDynamicHeader() {
        return mHeaderScrollableHeight > 0;
    }

    protected void showFancyDropDown(int initialSelection, String text,
            List<FancyDropDown.DropDownItemInfo> dropDownItemInfos,
            FancyDropDown.DropDownListener dropDownListener) {
        if (getView() == null) {
            Log.e(TAG, "Couldn't setup FancyDropDown, because getView() is null!");
            return;
        }

        FancyDropDown fancyDropDown = (FancyDropDown) getView().findViewById(R.id.fancydropdown);
        if (fancyDropDown != null) {
            fancyDropDown.setup(initialSelection, text.toUpperCase(), dropDownItemInfos,
                    dropDownListener);
        } else {
            Log.e(TAG, "Couldn't setup FancyDropDown, because there is no FancyDropDown in the view"
                    + " hierarchy");
        }
    }

    /**
     * Show a content header. A content header provides information about the current Collection
     * object that the user has navigated to. Like an AlbumArt image with the {@link
     * org.tomahawk.libtomahawk.collection.Album}s name, which is shown at the top of the listview,
     * if the user browses to a particular {@link org.tomahawk.libtomahawk.collection.Album} in his
     * {@link org.tomahawk.libtomahawk.collection.UserCollection}.
     *
     * @param item the Collection object's information to show in the header view
     */
    protected void showContentHeader(final Object item) {
        if (getView() == null) {
            Log.e(TAG, "Couldn't setup content header, because getView() is null!");
            return;
        }

        boolean isPagerFragment = this instanceof PagerFragment;

        //Inflate content header
        int stubResId;
        int inflatedId;
        if (item instanceof User) {
            stubResId = isPagerFragment ? R.id.content_header_user_pager_stub
                    : R.id.content_header_user_stub;
            inflatedId = isPagerFragment ? R.id.content_header_user_pager
                    : R.id.content_header_user;
        } else {
            if (mHeaderScrollableHeight > 0) {
                stubResId = isPagerFragment ? R.id.content_header_pager_stub
                        : R.id.content_header_stub;
                inflatedId = isPagerFragment ? R.id.content_header_pager
                        : R.id.content_header;
            } else {
                stubResId = isPagerFragment ? R.id.content_header_static_pager_stub
                        : R.id.content_header_static_stub;
                inflatedId = isPagerFragment ? R.id.content_header_static_pager
                        : R.id.content_header_static;
            }
        }
        final View contentHeader = ViewUtils.ensureInflation(getView(), stubResId, inflatedId);
        contentHeader.getLayoutParams().height =
                mHeaderNonscrollableHeight + mHeaderScrollableHeight;

        //Now we fill the added views with data and inflate the correct imageview_grid depending on
        //what we need
        final int gridOneStubId = isPagerFragment ? R.id.imageview_grid_one_pager_stub
                : R.id.imageview_grid_one_stub;
        final int gridOneResId = isPagerFragment ? R.id.imageview_grid_one_pager
                : R.id.imageview_grid_one;
        if (item instanceof Integer) {
            View v = ViewUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
            v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
            ImageView imageView = (ImageView) v.findViewById(R.id.imageview1);
            ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                    (Integer) item);
        } else if (item instanceof String) {
            View v = ViewUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
            v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
            ImageView imageView = (ImageView) v.findViewById(R.id.imageview1);
            ImageUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                    (String) item);
        } else if (item instanceof ColorDrawable) {
            View v = ViewUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
            v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
            ImageView imageView = (ImageView) v.findViewById(R.id.imageview1);
            imageView.setImageDrawable((ColorDrawable) item);
        } else if (mHeaderScrollableHeight > 0) {
            View moreButton = getView().findViewById(R.id.more_button);
            moreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentUtils.showContextMenu((TomahawkMainActivity) getActivity(), item,
                            mCollection.getId(), false, mHideRemoveButton);
                }
            });

            if (item instanceof Album) {
                View v = ViewUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
                v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
                ImageView imageView = (ImageView) v.findViewById(R.id.imageview1);
                ImageUtils.loadImageIntoImageView(TomahawkApp.getContext(), imageView,
                        ((Album) item).getImage(), Image.getLargeImageSize(), false);
                View stationButton = getView().findViewById(R.id.station_button);
                stationButton.setVisibility(View.VISIBLE);
                stationButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getMediaController() != null) {
                            if (item != getPlaybackManager().getPlaylist()) {
                                List<Pair<Artist, String>> artists = new ArrayList<>();
                                artists.add(new Pair<>(((Album) item).getArtist(), ""));
                                StationPlaylist stationPlaylist =
                                        StationPlaylist.get(artists, null, null);
                                getPlaybackManager().setPlaylist(stationPlaylist);
                                getMediaController().getTransportControls().play();
                            }
                        }
                    }
                });
            } else if (item instanceof Artist) {
                View v = ViewUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
                v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
                ImageView imageView = (ImageView) v.findViewById(R.id.imageview1);
                ImageUtils.loadImageIntoImageView(TomahawkApp.getContext(), imageView,
                        ((Artist) item).getImage(), Image.getLargeImageSize(), true);
                View stationButton = getView().findViewById(R.id.station_button);
                stationButton.setVisibility(View.VISIBLE);
                stationButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getMediaController() != null) {
                            if (item != getPlaybackManager().getPlaylist()) {
                                List<Pair<Artist, String>> artists = new ArrayList<>();
                                artists.add(new Pair<>((Artist) item, ""));
                                StationPlaylist stationPlaylist =
                                        StationPlaylist.get(artists, null, null);
                                getPlaybackManager().setPlaylist(stationPlaylist);
                                getMediaController().getTransportControls().play();
                            }
                        }
                    }
                });
            } else if (item instanceof Playlist) {
                ViewHolder.fillView(getView(), (Playlist) item,
                        mHeaderNonscrollableHeight + mHeaderScrollableHeight, isPagerFragment);
                View stationButton = getView().findViewById(R.id.station_button);
                stationButton.setVisibility(View.VISIBLE);
                stationButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getMediaController() != null) {
                            if (item != getPlaybackManager().getPlaylist()) {
                                StationPlaylist stationPlaylist =
                                        StationPlaylist.get((Playlist) item);
                                getPlaybackManager().setPlaylist(stationPlaylist);
                                getMediaController().getTransportControls().play();
                            }
                        }
                    }
                });
            } else if (item instanceof Query) {
                View v = ViewUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
                v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
                ImageView imageView = (ImageView) v.findViewById(R.id.imageview1);
                ImageUtils.loadImageIntoImageView(TomahawkApp.getContext(), imageView,
                        ((Query) item).getImage(), Image.getLargeImageSize(),
                        ((Query) item).hasArtistImage());
            }
        } else {
            if (item == null) {
                View v = ViewUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
                v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
                ImageView imageView = (ImageView) v.findViewById(R.id.imageview1);
                imageView.setImageDrawable(new ColorDrawable(
                        getResources().getColor(R.color.userpage_default_background)));
            } else if (item instanceof Image) {
                View v = ViewUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
                v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
                ImageUtils.loadBlurredImageIntoImageView(TomahawkApp.getContext(),
                        (ImageView) v.findViewById(R.id.imageview1), (Image) item,
                        Image.getSmallImageSize(), R.color.userpage_default_background);
            } else if (item instanceof User) {
                User.getSelf().done(new DoneCallback<User>() {
                    @Override
                    public void onDone(final User user) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                boolean showFollowing = false;
                                boolean showNotFollowing = false;
                                if (mShowFakeFollowing || mShowFakeNotFollowing) {
                                    showFollowing = mShowFakeFollowing;
                                    showNotFollowing = mShowFakeNotFollowing;
                                } else if (!user.isOffline()) {
                                    showFollowing = item != user && user.getFollowings() != null
                                            && user.getFollowings().containsKey(item);
                                    showNotFollowing = item != user && (user.getFollowings() == null
                                            || !user.getFollowings().containsKey(item));
                                }
                                View v = ViewUtils.ensureInflation(
                                        getView(), gridOneStubId, gridOneResId);
                                v.getLayoutParams().height = mHeaderNonscrollableHeight
                                        + mHeaderScrollableHeight;
                                ImageUtils.loadBlurredImageIntoImageView(
                                        TomahawkApp.getContext(),
                                        (ImageView) v.findViewById(R.id.imageview1),
                                        ((User) item).getImage(), Image.getSmallImageSize(),
                                        R.color.userpage_default_background);
                                ImageUtils.loadUserImageIntoImageView(TomahawkApp.getContext(),
                                        (ImageView) contentHeader.findViewById(R.id.userimageview1),
                                        (User) item, Image.getSmallImageSize(),
                                        (TextView) contentHeader.findViewById(R.id.usertextview1));
                                TextView textView =
                                        (TextView) contentHeader.findViewById(R.id.textview1);
                                textView.setText(((User) item).getName().toUpperCase());
                                TextView followButton =
                                        (TextView) contentHeader.findViewById(R.id.followbutton1);
                                if (showFollowing) {
                                    followButton.setVisibility(View.VISIBLE);
                                    followButton.setBackgroundResource(
                                            R.drawable.selectable_background_button_green_filled);
                                    followButton.setOnClickListener(mFollowButtonListener);
                                    followButton.setText(TomahawkApp.getContext().getString(
                                            R.string.content_header_following).toUpperCase());
                                } else if (showNotFollowing) {
                                    followButton.setVisibility(View.VISIBLE);
                                    followButton.setBackgroundResource(
                                            R.drawable.selectable_background_button_green);
                                    followButton.setOnClickListener(mFollowButtonListener);
                                    followButton.setText(TomahawkApp.getContext().getString(
                                            R.string.content_header_follow).toUpperCase());
                                } else {
                                    followButton.setVisibility(View.GONE);
                                }
                            }
                        });
                    }
                });
            }
        }
    }

    /**
     * Add a non-scrollable spacer to the top of the given view
     */
    protected void setupNonScrollableSpacer(View view) {
        //Add a non-scrollable spacer to the top of the given view
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.topMargin = mHeaderNonscrollableHeight;
    }

    protected void setupScrollableSpacer(TomahawkListAdapter adapter,
            StickyListHeadersListView listView, View headerSpacerForwardView) {
        if (adapter != null) {
            adapter.setShowContentHeaderSpacer(mHeaderScrollableHeight, listView,
                    headerSpacerForwardView);
        } else {
            Log.d(TAG, "setupScrollableSpacer - Can't call setShowContentHeaderSpacer,"
                    + " Adapter is null");
        }
    }

    protected void addAnimator(int id, ValueAnimator animator) {
        mAnimators.put(id, animator);
    }

    protected void setupAnimations() {
        if (getView() == null) {
            Log.e(TAG, "Couldn't setup animations, because getView() is null!");
            return;
        }

        mAnimators.clear();
        if (isDynamicHeader()) {
            boolean isPagerFragment = this instanceof PagerFragment;
            View header = getView().findViewById(isPagerFragment ? R.id.content_header_pager
                    : R.id.content_header);
            if (header == null) {
                header = getView().findViewById(isPagerFragment ? R.id.content_header_user_pager
                        : R.id.content_header_user);
            }
            setupFancyDropDownAnimation(header);
            setupButtonAnimation(header);
            setupPageIndicatorAnimation(header);

            View headerImage = getView()
                    .findViewById(isPagerFragment ? R.id.imageview_grid_one_pager
                            : R.id.imageview_grid_one);
            if (headerImage == null || headerImage.getVisibility() == View.GONE) {
                headerImage = getView()
                        .findViewById(isPagerFragment ? R.id.imageview_grid_two_pager
                                : R.id.imageview_grid_two);
            }
            if (headerImage == null || headerImage.getVisibility() == View.GONE) {
                headerImage = getView()
                        .findViewById(isPagerFragment ? R.id.imageview_grid_three_pager
                                : R.id.imageview_grid_three);
            }

            setupImageViewAnimation(headerImage);
        }
    }

    protected void refreshAnimations() {
        animate(mLastPlayTime);
    }

    private void setupFancyDropDownAnimation(final View view) {
        if (view != null) {
            final FancyDropDown fancyDropDown =
                    (FancyDropDown) view.findViewById(R.id.fancydropdown);
            if (fancyDropDown != null) {
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        // get resources first
                        Resources resources = TomahawkApp.getContext().getResources();
                        int dropDownHeight = resources.getDimensionPixelSize(
                                R.dimen.show_context_menu_icon_height);
                        int actionBarHeight = resources.getDimensionPixelSize(
                                R.dimen.abc_action_bar_default_height_material);
                        int smallPadding = resources.getDimensionPixelSize(
                                R.dimen.padding_small);
                        int superLargePadding = resources.getDimensionPixelSize(
                                R.dimen.padding_superlarge);

                        // now calculate the animation goal and instantiate the animation
                        int initialX = view.getWidth() / 2 - fancyDropDown.getWidth() / 2;
                        int initialY = view.getHeight() / 2 - dropDownHeight / 2;
                        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("x", initialX,
                                superLargePadding);
                        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("y", initialY,
                                actionBarHeight + smallPadding);
                        ValueAnimator animator = ObjectAnimator
                                .ofPropertyValuesHolder(fancyDropDown, pvhX, pvhY)
                                .setDuration(10000);
                        animator.setInterpolator(new LinearInterpolator());
                        addAnimator(ANIM_FANCYDROPDOWN_ID, animator);

                        refreshAnimations();
                    }
                };
                r.run();
                fancyDropDown.setOnSizeChangedListener(new OnSizeChangedListener() {
                    @Override
                    public void onSizeChanged(int w, int h, int oldw, int oldh) {
                        r.run();
                    }
                });
            }
        }
    }

    private void setupButtonAnimation(final View view) {
        if (view != null) {
            View moreButton = view.findViewById(R.id.button_panel);
            if (moreButton != null) {
                ViewUtils.afterViewGlobalLayout(new ViewUtils.ViewRunnable(moreButton) {
                    @Override
                    public void run() {
                        // get resources first
                        Resources resources = TomahawkApp.getContext().getResources();
                        int buttonHeight = TomahawkApp.getContext().getResources()
                                .getDimensionPixelSize(
                                        R.dimen.show_context_menu_icon_height);
                        int largePadding = TomahawkApp.getContext().getResources()
                                .getDimensionPixelSize(R.dimen.padding_large);
                        int smallPadding = resources
                                .getDimensionPixelSize(R.dimen.padding_small);
                        int actionBarHeight = resources.getDimensionPixelSize(
                                R.dimen.abc_action_bar_default_height_material);
                        View pageIndicator = view.findViewById(R.id.page_indicator);
                        int pageIndicatorHeight = 0;
                        if (pageIndicator != null
                                && pageIndicator.getVisibility() == View.VISIBLE) {
                            pageIndicatorHeight = TomahawkApp.getContext().getResources()
                                    .getDimensionPixelSize(R.dimen.pager_indicator_height);
                        }

                        // now calculate the animation goal and instantiate the animation
                        int initialY = view.getHeight() - buttonHeight - largePadding
                                - pageIndicatorHeight;
                        ValueAnimator animator = ObjectAnimator.ofFloat(getLayedOutView(), "y",
                                initialY, actionBarHeight + smallPadding)
                                .setDuration(10000);
                        animator.setInterpolator(new LinearInterpolator());
                        addAnimator(ANIM_BUTTON_ID, animator);

                        refreshAnimations();
                    }
                });
            }
        }
    }

    private void setupImageViewAnimation(final View view) {
        if (view != null) {
            ViewUtils.afterViewGlobalLayout(new ViewUtils.ViewRunnable(view) {
                @Override
                public void run() {
                    // now calculate the animation goal and instantiate the animation
                    int initialY = 0;
                    ValueAnimator animator = ObjectAnimator.ofFloat(getLayedOutView(), "y",
                            initialY, view.getHeight() / -3)
                            .setDuration(10000);
                    animator.setInterpolator(new LinearInterpolator());
                    addAnimator(ANIM_IMAGEVIEW_ID, animator);

                    refreshAnimations();
                }
            });
        }
    }

    private void setupPageIndicatorAnimation(final View view) {
        if (view != null) {
            final PageIndicator indicatorView =
                    (PageIndicator) view.findViewById(R.id.page_indicator);
            if (indicatorView != null) {
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        // now calculate the animation goal and instantiate the animation
                        int initialY = view.getHeight() - indicatorView.getHeight();
                        ValueAnimator animator = ObjectAnimator.ofFloat(indicatorView, "y",
                                initialY,
                                mHeaderNonscrollableHeight - indicatorView.getHeight())
                                .setDuration(10000);
                        animator.setInterpolator(new LinearInterpolator());
                        addAnimator(ANIM_PAGEINDICATOR_ID, animator);

                        refreshAnimations();
                    }
                };
                r.run();
                indicatorView.setOnSizeChangedListener(new OnSizeChangedListener() {
                    @Override
                    public void onSizeChanged(int w, int h, int oldw, int oldh) {
                        r.run();
                    }
                });
            }
        }
    }

    public void animate(int position) {
        mLastPlayTime = position;
        for (int i = 0; i < mAnimators.size(); i++) {
            mAnimators.valueAt(i).setCurrentPlayTime(position);
        }
    }
}
