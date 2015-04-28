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

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.adapters.ViewHolder;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.views.FancyDropDown;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.greenrobot.event.EventBus;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ContentHeaderFragment extends Fragment {

    private static final String TAG = ContentHeaderFragment.class.getSimpleName();

    public static final int MODE_HEADER_DYNAMIC = 0;

    public static final int MODE_HEADER_DYNAMIC_PAGER = 1;

    public static final int MODE_HEADER_STATIC = 2;

    public static final int MODE_HEADER_STATIC_USER = 3;

    public static final int MODE_ACTIONBAR_FILLED = 4;

    public static final int MODE_HEADER_STATIC_SMALL = 5;

    public static final int MODE_HEADER_PLAYBACK = 6;

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

    private final Set<ValueAnimator> mAnimators = new HashSet<>();

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
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mCurrentMode == MODE_HEADER_PLAYBACK) {
            TomahawkUtils.afterViewGlobalLayout(new TomahawkUtils.ViewRunnable(view) {
                @Override
                public void run() {
                    mHeaderScrollableHeight =
                            getLayedOutView().getHeight() - mHeaderNonscrollableHeight;
                    onHeaderHeightChanged();
                }
            });
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
    }

    @Override
    public void onPause() {
        super.onPause();

        ((TomahawkMainActivity) getActivity()).showGradientActionBar();
    }

    public void onHeaderHeightChanged() {
    }

    public boolean isDynamicHeader() {
        return mHeaderScrollableHeight > 0;
    }

    protected void showFancyDropDown(String text) {
        if (getView() == null) {
            Log.e(TAG, "Couldn't setup FancyDropDown, because getView() is null!");
            return;
        }

        FancyDropDown fancyDropDown = (FancyDropDown) getView().findViewById(R.id.fancydropdown);
        fancyDropDown.setup(text);
    }

    protected void showFancyDropDown(int initialSelection, String text,
            List<FancyDropDown.DropDownItemInfo> dropDownItemInfos,
            FancyDropDown.DropDownListener dropDownListener) {
        if (getView() == null) {
            Log.e(TAG, "Couldn't setup FancyDropDown, because getView() is null!");
            return;
        }

        FancyDropDown fancyDropDown = (FancyDropDown) getView().findViewById(R.id.fancydropdown);
        fancyDropDown.setup(initialSelection, text, dropDownItemInfos, dropDownListener);
    }

    /**
     * Show a content header. A content header provides information about the current {@link
     * org.tomahawk.tomahawk_android.utils.TomahawkListItem} that the user has navigated to. Like an
     * AlbumArt image with the {@link org.tomahawk.libtomahawk.collection.Album}s name, which is
     * shown at the top of the listview, if the user browses to a particular {@link
     * org.tomahawk.libtomahawk.collection.Album} in his {@link org.tomahawk.libtomahawk.collection.UserCollection}.
     *
     * @param item the {@link org.tomahawk.tomahawk_android.utils.TomahawkListItem}'s information to
     *             show in the header view
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
        View contentHeader = TomahawkUtils.ensureInflation(getView(), stubResId, inflatedId);
        contentHeader.getLayoutParams().height =
                mHeaderNonscrollableHeight + mHeaderScrollableHeight;

        //Now we fill the added views with data and inflate the correct imageview_grid depending on
        //what we need
        int gridOneStubId = isPagerFragment ? R.id.imageview_grid_one_pager_stub
                : R.id.imageview_grid_one_stub;
        int gridOneResId = isPagerFragment ? R.id.imageview_grid_one_pager
                : R.id.imageview_grid_one;
        if (item instanceof Integer) {
            View v = TomahawkUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
            v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
            ImageView imageView = (ImageView) v.findViewById(R.id.imageview1);
            TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                    (Integer) item);
        } else if (item instanceof ColorDrawable) {
            View v = TomahawkUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
            v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
            ImageView imageView = (ImageView) v.findViewById(R.id.imageview1);
            imageView.setImageDrawable((ColorDrawable) item);
        } else if (mHeaderScrollableHeight > 0) {
            View.OnClickListener moreButtonListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String collectionId = mCollection != null ? mCollection.getId() : null;
                    FragmentUtils.showContextMenu((TomahawkMainActivity) getActivity(), item,
                            collectionId, false);
                }
            };
            if (item instanceof Album) {
                View v = TomahawkUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
                v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
                ImageView imageView = (ImageView) v.findViewById(R.id.imageview1);
                TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), imageView,
                        ((Album) item).getImage(), Image.getLargeImageSize(), false);
                View moreButton = getView().findViewById(R.id.more_button);
                moreButton.setOnClickListener(moreButtonListener);
            } else if (item instanceof Artist) {
                View v = TomahawkUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
                v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
                ImageView imageView = (ImageView) v.findViewById(R.id.imageview1);
                TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), imageView,
                        ((Artist) item).getImage(), Image.getLargeImageSize(), true);
                View moreButton = getView().findViewById(R.id.more_button);
                moreButton.setOnClickListener(moreButtonListener);
            } else if (item instanceof Playlist) {
                ViewHolder.fillView(getView(), (Playlist) item,
                        mHeaderNonscrollableHeight + mHeaderScrollableHeight, isPagerFragment);
                View moreButton = getView().findViewById(R.id.more_button);
                moreButton.setOnClickListener(moreButtonListener);
            } else if (item instanceof Query) {
                View v = TomahawkUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
                v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
                ImageView imageView = (ImageView) v.findViewById(R.id.imageview1);
                TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), imageView,
                        ((Query) item).getImage(), Image.getLargeImageSize(),
                        ((Query) item).hasArtistImage());
                View moreButton = getView().findViewById(R.id.more_button);
                moreButton.setOnClickListener(moreButtonListener);
            }
        } else {
            if (item == null) {
                View v = TomahawkUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
                v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
                ImageView imageView = (ImageView) v.findViewById(R.id.imageview1);
                imageView.setImageDrawable(new ColorDrawable(
                        getResources().getColor(R.color.userpage_default_background)));
            } else if (item instanceof Image) {
                View v = TomahawkUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
                v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
                TomahawkUtils.loadBlurredImageIntoImageView(TomahawkApp.getContext(),
                        (ImageView) v.findViewById(R.id.imageview1), (Image) item,
                        Image.getSmallImageSize(), R.color.userpage_default_background);
            } else if (item instanceof User) {
                HatchetAuthenticatorUtils authUtils =
                        (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                boolean showFollowing = false;
                boolean showNotFollowing = false;
                if (mShowFakeFollowing || mShowFakeNotFollowing) {
                    showFollowing = mShowFakeFollowing;
                    showNotFollowing = mShowFakeNotFollowing;
                } else if (authUtils.getLoggedInUser() != null) {
                    User user = authUtils.getLoggedInUser();
                    showFollowing = item != user && user.getFollowings() != null
                            && user.getFollowings().containsKey(item);
                    showNotFollowing = item != user && (user.getFollowings() == null
                            || !user.getFollowings().containsKey(item));
                }
                View v = TomahawkUtils.ensureInflation(getView(), gridOneStubId, gridOneResId);
                v.getLayoutParams().height = mHeaderNonscrollableHeight + mHeaderScrollableHeight;
                TomahawkUtils.loadBlurredImageIntoImageView(TomahawkApp.getContext(),
                        (ImageView) v.findViewById(R.id.imageview1),
                        ((User) item).getImage(), Image.getSmallImageSize(),
                        R.color.userpage_default_background);
                TomahawkUtils.loadUserImageIntoImageView(TomahawkApp.getContext(),
                        (ImageView) contentHeader.findViewById(R.id.userimageview1),
                        (User) item, Image.getSmallImageSize(),
                        (TextView) contentHeader.findViewById(R.id.usertextview1));
                TextView textView = (TextView) contentHeader.findViewById(R.id.textview1);
                textView.setText(((User) item).getName().toUpperCase());
                TextView followButton = (TextView) contentHeader.findViewById(R.id.followbutton1);
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

    protected void addAnimator(ValueAnimator animator) {
        mAnimators.add(animator);
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

            if (mContainerFragmentId >= 0) {
                AnimateEvent event = new AnimateEvent();
                event.mContainerFragmentId = mContainerFragmentId;
                event.mContainerFragmentPage = mContainerFragmentPage;
                event.mPlayTime = mLastPlayTime;
                EventBus.getDefault().post(event);
            } else {
                animate(mLastPlayTime);
            }
        }
    }

    private void setupFancyDropDownAnimation(final View view) {
        if (view != null) {
            View fancyDropDown = view.findViewById(R.id.fancydropdown);
            if (fancyDropDown != null) {
                TomahawkUtils.afterViewGlobalLayout(new TomahawkUtils.ViewRunnable(fancyDropDown) {
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
                        int initialX = view.getWidth() / 2 - getLayedOutView().getWidth() / 2;
                        int initialY = view.getHeight() / 2 - dropDownHeight / 2;
                        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("x", initialX,
                                superLargePadding);
                        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("y", initialY,
                                actionBarHeight + smallPadding);
                        ValueAnimator animator = ObjectAnimator
                                .ofPropertyValuesHolder(getLayedOutView(), pvhX, pvhY)
                                .setDuration(10000);
                        animator.setInterpolator(new LinearInterpolator());
                        addAnimator(animator);
                    }
                });
            }
        }
    }

    private void setupButtonAnimation(final View view) {
        if (view != null) {
            View moreButton = view.findViewById(R.id.more_button);
            if (moreButton != null) {
                TomahawkUtils.afterViewGlobalLayout(new TomahawkUtils.ViewRunnable(moreButton) {
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
                        addAnimator(animator);
                    }
                });
            }
        }
    }

    private void setupImageViewAnimation(final View view) {
        if (view != null) {
            TomahawkUtils.afterViewGlobalLayout(new TomahawkUtils.ViewRunnable(view) {
                @Override
                public void run() {
                    // now calculate the animation goal and instantiate the animation
                    int initialY = 0;
                    ValueAnimator animator = ObjectAnimator.ofFloat(getLayedOutView(), "y",
                            initialY, view.getHeight() / -3)
                            .setDuration(10000);
                    animator.setInterpolator(new LinearInterpolator());
                    addAnimator(animator);
                }
            });
        }
    }

    private void setupPageIndicatorAnimation(final View view) {
        if (view != null) {
            View indicatorView = view.findViewById(R.id.page_indicator);
            if (indicatorView != null) {
                TomahawkUtils.afterViewGlobalLayout(new TomahawkUtils.ViewRunnable(indicatorView) {
                    @Override
                    public void run() {
                        // now calculate the animation goal and instantiate the animation
                        int initialY = view.getHeight() - getLayedOutView().getHeight();
                        ValueAnimator animator = ObjectAnimator.ofFloat(getLayedOutView(), "y",
                                initialY,
                                mHeaderNonscrollableHeight - getLayedOutView().getHeight())
                                .setDuration(10000);
                        animator.setInterpolator(new LinearInterpolator());
                        addAnimator(animator);
                    }
                });
            }
        }
    }

    public void animate(int position) {
        mLastPlayTime = position;
        for (ValueAnimator animator : mAnimators) {
            if (animator != null) {
                animator.setCurrentPlayTime(position);
            }
        }
    }
}
