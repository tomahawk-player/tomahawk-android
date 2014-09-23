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
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.ViewHolder;
import org.tomahawk.tomahawk_android.utils.AdapterUtils;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import java.util.ArrayList;

public abstract class ContentHeaderFragment extends SlidingPanelFragment {

    private ValueAnimator mTextViewAnim;

    private ValueAnimator mButtonAnim;

    private ValueAnimator mImageViewAnim;

    private ValueAnimator mPageIndicatorAnim;

    private boolean mShowFakeFollowing = false;

    private boolean mShowFakeNotFollowing = false;

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
            final Object item, boolean dynamic, int headerHeightResid,
            View.OnClickListener followListener) {
        View actionBarBg = getView().findViewById(R.id.action_bar_background);
        if (actionBarBg != null) {
            actionBarBg.setVisibility(View.GONE);
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
        if (finalHeaderImage != null && dynamic) {
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
                setupTextViewAnimation(header);
                setupButtonAnimation(header);
                setupPageIndicatorAnimation(header);
            }
        }

        //Now we fill the added views with data
        ViewHolder viewHolder = new ViewHolder(imageFrame, headerFrame, layoutId);
        if (dynamic) {
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentUtils.showContextMenu((TomahawkMainActivity) getActivity(),
                            getFragmentManager(), (TomahawkListItem) item, null, false);
                }
            };
            if (item instanceof Album) {
                AdapterUtils.fillContentHeader(TomahawkApp.getContext(), viewHolder, (Album) item,
                        listener);
            } else if (item instanceof Artist) {
                AdapterUtils.fillContentHeader(TomahawkApp.getContext(), viewHolder, (Artist) item,
                        listener);
            } else if (item instanceof Playlist) {
                AdapterUtils.fillContentHeader(TomahawkApp.getContext(), viewHolder,
                        (Playlist) item, artistImages);
            } else if (item instanceof Query) {
                AdapterUtils.fillContentHeader(TomahawkApp.getContext(), viewHolder, (Query) item);
            }
        } else {
            if (item instanceof Image) {
                AdapterUtils.fillContentHeader(TomahawkApp.getContext(), viewHolder, (Image) item);
            } else if (item instanceof Integer) {
                TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(),
                        viewHolder.getImageView1(), (Integer) item);
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
                AdapterUtils.fillContentHeader(TomahawkApp.getContext(), viewHolder, (User) item,
                        showFollowing, showNotFollowing);
                viewHolder.getButton4().setOnClickListener(followListener);
            }
        }
    }

    private void setupTextViewAnimation(View view) {
        if (view != null) {
            View textView = view.findViewById(R.id.content_header_textview);
            if (textView != null) {
                Resources resources = TomahawkApp.getContext().getResources();
                int smallPadding = resources.getDimensionPixelSize(R.dimen.padding_small);
                int x = resources.getDimensionPixelSize(R.dimen.padding_superlarge);
                int actionBarHeight = 0;
                TypedValue tv = new TypedValue();
                if (getActivity().getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                    actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,
                            getResources().getDisplayMetrics());
                }
                int y = actionBarHeight + smallPadding;
                PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("x", x);
                PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("y", y);
                mTextViewAnim = ObjectAnimator.ofPropertyValuesHolder(textView, pvhX, pvhY)
                        .setDuration(10000);
                mTextViewAnim.setInterpolator(new LinearInterpolator());
            }
        }
    }

    private void setupButtonAnimation(View view) {
        if (view != null) {
            View buttonView = view.findViewById(R.id.content_header_more_button);
            if (buttonView != null) {
                Resources resources = TomahawkApp.getContext().getResources();
                int smallPadding = resources.getDimensionPixelSize(R.dimen.padding_small);
                int actionBarHeight = 0;
                TypedValue tv = new TypedValue();
                if (getActivity().getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                    actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,
                            getResources().getDisplayMetrics());
                }
                int y = actionBarHeight + smallPadding;
                mButtonAnim = ObjectAnimator.ofFloat(buttonView, "y", y).setDuration(10000);
                mButtonAnim.setInterpolator(new LinearInterpolator());
            }
        }
    }

    private void setupImageViewAnimation(View view) {
        if (view != null) {
            View imageView = view.findViewById(R.id.content_header_imagesingle);
            if (imageView == null) {
                imageView = view.findViewById(R.id.content_header_imagegrid);
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
                int offset = resources.getDimensionPixelSize(
                        R.dimen.header_clear_space_nonscrollable_pager)
                        - resources.getDimensionPixelSize(R.dimen.pager_indicator_height);
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
