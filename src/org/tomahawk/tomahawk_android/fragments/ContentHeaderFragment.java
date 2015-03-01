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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

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

    private Set<ValueAnimator> mAnimators = new HashSet<>();

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

    protected void showFancyDropDown(FrameLayout headerFrame, String text) {
        ViewHolder viewHolder = new ViewHolder(null, headerFrame, R.layout.content_header);
        viewHolder.setupFancyDropDown(text);
    }

    protected void showFancyDropDown(FrameLayout headerFrame, int initialSelection, String text,
            List<FancyDropDown.DropDownItemInfo> dropDownItemInfos,
            FancyDropDown.DropDownListener dropDownListener) {
        ViewHolder viewHolder = new ViewHolder(null, headerFrame, R.layout.content_header);
        viewHolder.setupFancyDropDown(initialSelection, text, dropDownItemInfos, dropDownListener);
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
    protected void showContentHeader(FrameLayout imageFrame, FrameLayout headerFrame,
            final Object item) {
        //Inflate views and add them into our frames
        LayoutInflater inflater = (LayoutInflater)
                TomahawkApp.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int layoutId;
        int viewId;
        if (item instanceof Playlist) {
            layoutId = R.layout.content_header_imagegrid;
            viewId = R.id.content_header_imagegrid;
        } else {
            layoutId = R.layout.content_header_imagesingle;
            viewId = R.id.content_header_imagesingle;
        }
        if (imageFrame.findViewById(viewId) == null) {
            imageFrame.removeAllViews();
            View headerImage = inflater.inflate(layoutId, imageFrame, false);
            headerImage.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    mHeaderNonscrollableHeight + mHeaderScrollableHeight));
            imageFrame.addView(headerImage);
        }

        if (item instanceof User) {
            layoutId = R.layout.content_header_user;
            viewId = R.id.content_header_user;
        } else {
            if (mHeaderScrollableHeight > 0) {
                layoutId = R.layout.content_header;
                viewId = R.id.content_header;
            } else {
                layoutId = R.layout.content_header_static;
                viewId = R.id.content_header_static;
            }
        }
        if (headerFrame.findViewById(viewId) == null) {
            headerFrame.removeAllViews();
            View header = inflater.inflate(layoutId, headerFrame, false);
            headerFrame.addView(header);
            header.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    mHeaderScrollableHeight + mHeaderNonscrollableHeight));
        }

        //Now we fill the added views with data
        ViewHolder viewHolder = new ViewHolder(imageFrame, headerFrame, layoutId);
        if (item instanceof Integer) {
            viewHolder.fillContentHeader((Integer) item);
        } else if (item instanceof ColorDrawable) {
            viewHolder.fillContentHeader((ColorDrawable) item);
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
                viewHolder.fillContentHeader((Album) item, moreButtonListener);
            } else if (item instanceof Artist) {
                viewHolder.fillContentHeader((Artist) item, moreButtonListener);
            } else if (item instanceof Playlist) {
                viewHolder.fillContentHeader((Playlist) item, moreButtonListener);
            } else if (item instanceof Query) {
                viewHolder.fillContentHeader((Query) item, moreButtonListener);
            }
        } else {
            if (item instanceof Image) {
                viewHolder.fillContentHeader((Image) item);
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
                    showNotFollowing = item != user && user.getFollowings() != null
                            && !user.getFollowings().containsKey(item);
                }
                viewHolder.fillContentHeader((User) item, showFollowing, showNotFollowing,
                        mFollowButtonListener);
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
            StickyListHeadersListView listView) {
        if (adapter != null) {
            adapter.setShowContentHeaderSpacer(mHeaderScrollableHeight, listView);
        } else {
            Log.d(TAG, "setupScrollableSpacer - Can't call setShowContentHeaderSpacer,"
                    + " Adapter is null");
        }
    }

    protected void addAnimator(ValueAnimator animator) {
        mAnimators.add(animator);
    }

    protected void setupAnimations(FrameLayout imageFrame, FrameLayout headerFrame) {
        mAnimators.clear();
        if (isDynamicHeader()) {
            View header = headerFrame.findViewById(R.id.content_header);
            if (header == null) {
                header = headerFrame.findViewById(R.id.content_header_user);
            }
            setupFancyDropDownAnimation(header);
            setupButtonAnimation(header);
            setupPageIndicatorAnimation(header);

            View headerImage = imageFrame.findViewById(R.id.content_header_imagegrid);
            if (headerImage == null) {
                headerImage = imageFrame.findViewById(R.id.content_header_imagesingle);
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
                        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("x",
                                view.getWidth() / 2 - getLayedOutView().getWidth() / 2,
                                superLargePadding);
                        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("y",
                                view.getHeight() / 2 - dropDownHeight / 2,
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
            View buttonView = view.findViewById(R.id.morebutton1);
            if (buttonView != null) {
                TomahawkUtils.afterViewGlobalLayout(new TomahawkUtils.ViewRunnable(buttonView) {
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
                        View pageIndicator = view.findViewById(R.id.page_indicator_container);
                        int pageIndicatorHeight = 0;
                        if (pageIndicator != null
                                && pageIndicator.getVisibility() == View.VISIBLE) {
                            pageIndicatorHeight = TomahawkApp.getContext().getResources()
                                    .getDimensionPixelSize(R.dimen.pager_indicator_height);
                        }

                        // now calculate the animation goal and instantiate the animation
                        ValueAnimator animator = ObjectAnimator.ofFloat(getLayedOutView(), "y",
                                view.getHeight() - buttonHeight - largePadding
                                        - pageIndicatorHeight,
                                actionBarHeight + smallPadding)
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
                    ValueAnimator animator = ObjectAnimator.ofFloat(getLayedOutView(), "y",
                            0, view.getHeight() / -3)
                            .setDuration(10000);
                    animator.setInterpolator(new LinearInterpolator());
                    addAnimator(animator);
                }
            });
        }
    }

    private void setupPageIndicatorAnimation(final View view) {
        if (view != null) {
            View indicatorView = view.findViewById(R.id.page_indicator_container);
            if (indicatorView != null) {
                TomahawkUtils.afterViewGlobalLayout(new TomahawkUtils.ViewRunnable(indicatorView) {
                    @Override
                    public void run() {
                        // now calculate the animation goal and instantiate the animation
                        ValueAnimator animator = ObjectAnimator
                                .ofFloat(getLayedOutView(), "y",
                                        view.getHeight() - getLayedOutView().getHeight(),
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
            if (animator != null && position != animator.getCurrentPlayTime()) {
                animator.setCurrentPlayTime(position);
            }
        }
    }
}
