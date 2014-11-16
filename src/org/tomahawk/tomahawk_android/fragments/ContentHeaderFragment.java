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
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.ViewHolder;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.views.FancyDropDown;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

public abstract class ContentHeaderFragment extends Fragment {

    public static final String DONT_SHOW_HEADER
            = "org.tomahawk.tomahawk_android.dont_show_header";

    private ValueAnimator mTextViewAnim;

    private ValueAnimator mButtonAnim;

    private ValueAnimator mImageViewAnim;

    private ValueAnimator mPageIndicatorAnim;

    private boolean mShowFakeFollowing = false;

    private boolean mShowFakeNotFollowing = false;

    protected boolean mDontShowHeader = false;

    @Override
    public void onResume() {
        super.onResume();

        if (getArguments() != null) {
            if (getArguments().containsKey(DONT_SHOW_HEADER)) {
                mDontShowHeader = getArguments().getBoolean(DONT_SHOW_HEADER);
            }
        }
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
            View actionBarGradient, final Object item, boolean dynamic, int headerHeightResid,
            View.OnClickListener followListener) {
        if (actionBarGradient != null) {
            actionBarGradient.setVisibility(View.VISIBLE);
        }
        //Inflate views and add them into our frames
        LayoutInflater inflater = (LayoutInflater)
                TomahawkApp.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ArrayList<Image> artistImages = new ArrayList<Image>();
        if (item instanceof Playlist) {
            synchronized (item) {
                ArrayList<Artist> artists = ((Playlist) item).getContentHeaderArtists();
                for (Artist artist : artists) {
                    if (artist.getImage() != null) {
                        artistImages.add(artist.getImage());
                    }
                }
            }
        }
        View headerImage = null;
        int layoutId;
        int viewId;
        if (artistImages.size() > 3) {
            if (dynamic) {
                layoutId = R.layout.content_header_imagegrid;
                viewId = R.id.content_header_imagegrid;
            } else {
                layoutId = R.layout.content_header_imagegrid_static;
                viewId = R.id.content_header_imagegrid_static;
            }
        } else {
            if (dynamic) {
                layoutId = R.layout.content_header_imagesingle;
                viewId = R.id.content_header_imagesingle;
            } else {
                layoutId = R.layout.content_header_imagesingle_static;
                viewId = R.id.content_header_imagesingle_static;
            }
        }
        if (imageFrame.findViewById(viewId) == null) {
            imageFrame.removeAllViews();
            headerImage = inflater.inflate(layoutId, imageFrame, false);
            if (!dynamic) {
                int headerHeight = getResources().getDimensionPixelSize(headerHeightResid);
                headerImage.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, headerHeight));
            }
            imageFrame.addView(headerImage);
        }
        final View finalHeaderImage = headerImage;
        if (finalHeaderImage != null) {
            headerImage.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            setupImageViewAnimation(finalHeaderImage);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                finalHeaderImage.getViewTreeObserver()
                                        .removeOnGlobalLayoutListener(this);
                            } else {
                                finalHeaderImage.getViewTreeObserver()
                                        .removeGlobalOnLayoutListener(this);
                            }
                        }
                    });
        }

        if (item instanceof User) {
            layoutId = R.layout.content_header_user;
            viewId = R.id.content_header_user;
        } else {
            if (dynamic) {
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
            if (dynamic) {
                setupFancyDropDownAnimation(header);
                setupButtonAnimation(header);
                setupPageIndicatorAnimation(header);

                //calculate the needed height for the content header
                int headerHeight = getResources()
                        .getDimensionPixelSize(R.dimen.header_clear_space_scrollable)
                        + getResources()
                        .getDimensionPixelSize(R.dimen.header_clear_space_nonscrollable);
                header.setLayoutParams(
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                headerHeight));
            }
        }

        //Now we fill the added views with data
        ViewHolder viewHolder = new ViewHolder(imageFrame, headerFrame, layoutId);
        if (item instanceof Integer) {
            viewHolder.fillContentHeader((Integer) item);
        } else if (dynamic) {
            View.OnClickListener moreButtonListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentUtils.showContextMenu((TomahawkMainActivity) getActivity(),
                            getFragmentManager(), (TomahawkListItem) item, null, false);
                }
            };
            if (item instanceof Album) {
                viewHolder.fillContentHeader((Album) item, moreButtonListener);
            } else if (item instanceof Artist) {
                viewHolder.fillContentHeader((Artist) item, moreButtonListener);
            } else if (item instanceof Playlist) {
                viewHolder.fillContentHeader((Playlist) item, artistImages);
            } else if (item instanceof Query) {
                viewHolder.fillContentHeader((Query) item);
            }
        } else {
            if (item instanceof Image) {
                viewHolder.fillContentHeader((Image) item);
            } else if (item instanceof User) {
                HatchetAuthenticatorUtils authUtils =
                        (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                boolean showFollowing, showNotFollowing;
                if (mShowFakeFollowing || mShowFakeNotFollowing) {
                    showFollowing = mShowFakeFollowing;
                    showNotFollowing = mShowFakeNotFollowing;
                } else {
                    showFollowing = item != authUtils.getLoggedInUser()
                            && authUtils.getLoggedInUser().getFollowings().containsKey(item);
                    showNotFollowing = item != authUtils.getLoggedInUser()
                            && !authUtils.getLoggedInUser().getFollowings().containsKey(item);
                }
                viewHolder.fillContentHeader((User) item, showFollowing, showNotFollowing,
                        followListener);
            }
        }
    }

    private void setupFancyDropDownAnimation(final View view) {
        if (view != null) {
            final View fancyDropDown = view.findViewById(R.id.fancydropdown);
            if (fancyDropDown != null) {
                view.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                // correctly position fancyDropDown first
                                int dropDownHeight = TomahawkApp.getContext().getResources()
                                        .getDimensionPixelSize(
                                                R.dimen.show_context_menu_icon_height);
                                fancyDropDown.setY(view.getHeight() / 2 - dropDownHeight / 2);

                                // now calculate the animation goal and instantiate the animation
                                Resources resources = TomahawkApp.getContext().getResources();
                                int smallPadding = resources
                                        .getDimensionPixelSize(R.dimen.padding_small);
                                int x = resources.getDimensionPixelSize(R.dimen.padding_superlarge);
                                int actionBarHeight = resources.getDimensionPixelSize(
                                        R.dimen.abc_action_bar_default_height_material);
                                int y = actionBarHeight + smallPadding;
                                PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("x", x);
                                PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("y", y);
                                mTextViewAnim = ObjectAnimator
                                        .ofPropertyValuesHolder(fancyDropDown, pvhX, pvhY)
                                        .setDuration(10000);
                                mTextViewAnim.setInterpolator(new LinearInterpolator());
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    view.getViewTreeObserver()
                                            .removeOnGlobalLayoutListener(this);
                                } else {
                                    view.getViewTreeObserver()
                                            .removeGlobalOnLayoutListener(this);
                                }
                            }
                        });
            }
        }
    }

    private void setupButtonAnimation(final View view) {
        if (view != null) {
            final View buttonView = view.findViewById(R.id.morebutton1);
            if (buttonView != null) {
                view.getViewTreeObserver().addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                // correctly position fancyDropDown first
                                Resources resources = TomahawkApp.getContext().getResources();
                                int buttonHeight = TomahawkApp.getContext().getResources()
                                        .getDimensionPixelSize(
                                                R.dimen.show_context_menu_icon_height);
                                int largePadding = TomahawkApp.getContext().getResources()
                                        .getDimensionPixelSize(R.dimen.padding_large);
                                int pageIndicatorHeight = 0;
                                View pageIndicator =
                                        view.findViewById(R.id.page_indicator_container);
                                if (pageIndicator != null
                                        && pageIndicator.getVisibility() == View.VISIBLE) {
                                    pageIndicatorHeight = TomahawkApp.getContext().getResources()
                                            .getDimensionPixelSize(R.dimen.pager_indicator_height);
                                }
                                buttonView.setY(view.getHeight() - buttonHeight - largePadding
                                        - pageIndicatorHeight);

                                // now calculate the animation goal and instantiate the animation
                                int smallPadding = resources
                                        .getDimensionPixelSize(R.dimen.padding_small);
                                int actionBarHeight = resources.getDimensionPixelSize(
                                        R.dimen.abc_action_bar_default_height_material);
                                int y = actionBarHeight + smallPadding;
                                mButtonAnim = ObjectAnimator.ofFloat(buttonView, "y", y)
                                        .setDuration(10000);
                                mButtonAnim.setInterpolator(new LinearInterpolator());
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    view.getViewTreeObserver()
                                            .removeOnGlobalLayoutListener(this);
                                } else {
                                    view.getViewTreeObserver()
                                            .removeGlobalOnLayoutListener(this);
                                }
                            }
                        });
            }
        }
    }

    private void setupImageViewAnimation(View view) {
        if (view != null) {
            View imageView = view.findViewById(R.id.content_header_imagesingle);
            if (imageView == null) {
                imageView = view.findViewById(R.id.content_header_imagegrid);
            }
            if (imageView == null) {
                imageView = view.findViewById(R.id.content_header_imagesingle_static);
            }
            if (imageView != null) {
                mImageViewAnim = ObjectAnimator.ofFloat(imageView, "y", view.getHeight() / -3)
                        .setDuration(10000);
                mImageViewAnim.setInterpolator(new LinearInterpolator());
            }
        }
    }

    private void setupPageIndicatorAnimation(View view) {
        if (view != null) {
            View indicatorView = view.findViewById(R.id.page_indicator_container);
            if (indicatorView != null) {
                Resources resources = TomahawkApp.getContext().getResources();
                int offset =
                        resources.getDimensionPixelSize(R.dimen.header_clear_space_nonscrollable);
                mPageIndicatorAnim = ObjectAnimator.ofFloat(indicatorView, "y", offset)
                        .setDuration(10000);
                mPageIndicatorAnim.setInterpolator(new LinearInterpolator());
            }
        }
    }

    public void animateContentHeader(int position) {
        if (mTextViewAnim != null && position != mTextViewAnim.getCurrentPlayTime()) {
            mTextViewAnim.setCurrentPlayTime(position);
        }
        if (mButtonAnim != null && position != mButtonAnim.getCurrentPlayTime()) {
            mButtonAnim.setCurrentPlayTime(position);
        }
        if (mImageViewAnim != null && position != mImageViewAnim.getCurrentPlayTime()) {
            mImageViewAnim.setCurrentPlayTime(position);
        }
        if (mPageIndicatorAnim != null && position != mPageIndicatorAnim.getCurrentPlayTime()) {
            mPageIndicatorAnim.setCurrentPlayTime(position);
        }
    }

    public void setShowFakeNotFollowing(boolean showFakeNotFollowing) {
        mShowFakeNotFollowing = showFakeNotFollowing;
    }

    public void setShowFakeFollowing(boolean showFakeFollowing) {
        mShowFakeFollowing = showFakeFollowing;
    }
}
