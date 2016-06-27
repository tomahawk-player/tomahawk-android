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
package org.tomahawk.tomahawk_android.views;

import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.resolver.UserCollectionStubResolver;
import org.tomahawk.libtomahawk.utils.ViewUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.listeners.OnSizeChangedListener;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FancyDropDown extends FrameLayout {

    private int mSelection;

    private List<DropDownItemInfo> mItemInfos;

    private DropDownListener mListener;

    private boolean mShowing;

    private boolean mIsAnimating;

    private int mItemHeight;

    private SparseArray<FrameLayout> mItemFrames;

    public String mText;

    private OnSizeChangedListener mOnSizeChangedListener;

    private boolean mCanBeVisible = false;

    public static class DropDownItemInfo {

        public String mText;

        public Resolver mResolver;

        public boolean equals(DropDownItemInfo itemInfo) {
            return (mText != null && mText.equals(itemInfo.mText))
                    || (mText == null && itemInfo.mText == null)
                    && mResolver == itemInfo.mResolver;
        }
    }

    public interface DropDownListener {

        void onDropDownItemSelected(int position);

        void onCancel();
    }

    public FancyDropDown(Context context) {
        super(context);
        inflate(getContext(), R.layout.fancydropdown, this);
    }

    public FancyDropDown(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(getContext(), R.layout.fancydropdown, this);
    }

    public void setup(int initialSelection, String selectedText,
            List<DropDownItemInfo> dropDownItemInfos, DropDownListener dropDownListener) {
        mListener = dropDownListener;
        mText = selectedText;
        ((TextView) findViewById(R.id.textview_selected)).setText(mText);
        mCanBeVisible = true;

        if (dropDownItemInfos != null && dropDownItemInfos.size() > 0) {
            // Do we really need to update? Do the new infos differ from the old ones?
            boolean differingInfos = mItemInfos == null
                    || mItemInfos.size() != dropDownItemInfos.size();
            for (int i = 0; !differingInfos && i < mItemInfos.size(); i++) {
                if (!mItemInfos.get(i).equals(dropDownItemInfos.get(i))) {
                    differingInfos = true;
                }
            }
            if (differingInfos) {
                mItemInfos = dropDownItemInfos;
                mItemFrames = new SparseArray<>();
                LinearLayout itemsContainer =
                        (LinearLayout) findViewById(R.id.dropdown_items_container);
                itemsContainer.removeAllViews();
                updateSelectedItem(initialSelection);
                LayoutInflater inflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                for (int i = 0; i < mItemInfos.size(); i++) {
                    final LinearLayout item = (LinearLayout) inflater
                            .inflate(R.layout.fancydropdown_item, this, false);
                    final TextView textView = (TextView) item.findViewById(R.id.textview);
                    textView.setText(mItemInfos.get(i).mText.toUpperCase());
                    ImageView imageView = (ImageView) item.findViewById(R.id.imageview);
                    if (mItemInfos.get(i).mResolver != null) {
                        mItemInfos.get(i).mResolver.loadIconWhite(imageView, 0);
                    }

                    final int position = i;
                    item.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            hideDropDownList(position);
                            if (mListener != null) {
                                mListener.onDropDownItemSelected(position);
                            }
                        }
                    });
                    // We need a FrameLayout to hide the item when it's out of the FrameLayout's bounds
                    FrameLayout frameLayout = new FrameLayout(getContext());
                    frameLayout.setLayoutParams(
                            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT));
                    frameLayout.addView(item);
                    itemsContainer.addView(frameLayout);
                    mItemFrames.put(position, frameLayout);
                }
                ViewUtils.afterViewGlobalLayout(new ViewUtils.ViewRunnable(mItemFrames.get(0)) {
                    @Override
                    public void run() {
                        mItemHeight = mItemFrames.get(0).getHeight();
                        for (int i = 0; i < mItemFrames.size(); i++) {
                            mItemFrames.get(i).getChildAt(0).setY(mItemHeight * -1);
                        }
                    }
                });
            }
        }
        findViewById(R.id.selected_item_container).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemFrames != null && mItemFrames.size() > 1) {
                    if (mShowing) {
                        mListener.onCancel();
                        hideDropDownList(mSelection);
                    } else {
                        showDropDownList();
                    }
                }
            }
        });
    }

    private void updateSelectedItem(int newSelection) {
        mSelection = newSelection;
        ImageView imageView = (ImageView) findViewById(R.id.imageview_selected);
        if (mItemInfos != null) {
            if (mItemInfos.get(mSelection).mResolver != null) {
                mItemInfos.get(mSelection).mResolver.loadIconWhite(imageView, 0);
                imageView.setVisibility(VISIBLE);
            } else {
                imageView.setVisibility(GONE);
            }
        } else {
            imageView.setVisibility(GONE);
        }
    }

    public void showDropDownList() {
        if (!mIsAnimating && !isShowing()) {
            animateDropDown(false, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mShowing = true;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }
    }

    public void hideDropDownList(int selectedItem) {
        if (!mIsAnimating && isShowing()) {
            updateSelectedItem(selectedItem);
            animateDropDown(true, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mShowing = false;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }
    }

    public boolean isShowing() {
        return mShowing;
    }

    private void animateDropDown(boolean reverse, Animation.AnimationListener listener) {
        int startPos = reverse ? mItemFrames.size() - 1 : 0;
        animateDropDownItem(startPos, reverse, 120, listener);
    }

    private void animateDropDownItem(final int position, final boolean reverse, final int duration,
            final Animation.AnimationListener listener) {
        if (reverse ? position >= 0
                : mItemFrames != null && position < mItemFrames.size()) {
            View item = mItemFrames.get(position).getChildAt(0);
            item.setY(reverse ? 0 : mItemHeight * -1);
            final ValueAnimator animator =
                    ObjectAnimator.ofFloat(item, "y", reverse ? mItemHeight * -1 : 0);
            animator.setDuration(duration / mItemFrames.size());
            animator.setInterpolator(new LinearInterpolator());
            animator.start();
            mIsAnimating = true;
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    int newPosition = reverse ? position - 1 : position + 1;
                    animateDropDownItem(newPosition, reverse, duration, listener);
                    animator.removeListener(this);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        } else {
            mIsAnimating = false;
            listener.onAnimationEnd(null);
        }
    }

    public static List<DropDownItemInfo> convertToDropDownItemInfo(List<Collection> collections) {
        List<FancyDropDown.DropDownItemInfo> dropDownItemInfos
                = new ArrayList<>();
        for (Collection collection : collections) {
            FancyDropDown.DropDownItemInfo dropDownItemInfo =
                    new FancyDropDown.DropDownItemInfo();
            if (TomahawkApp.PLUGINNAME_HATCHET.equals(collection.getId())) {
                dropDownItemInfo.mText = TomahawkApp.getContext().getString(R.string.all);
            } else if (TomahawkApp.PLUGINNAME_USERCOLLECTION.equals(collection.getId())) {
                dropDownItemInfo.mText = TomahawkApp.getContext().getString(R.string.local);
                dropDownItemInfo.mResolver = UserCollectionStubResolver.get();
            } else {
                Resolver resolver = PipeLine.get().getResolver(collection.getId());
                dropDownItemInfo.mText = resolver.getId();
                dropDownItemInfo.mResolver = resolver;
            }
            dropDownItemInfos.add(dropDownItemInfo);
        }
        return dropDownItemInfos;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (mCanBeVisible) {
            setVisibility(VISIBLE);
        }

        if (mOnSizeChangedListener != null) {
            mOnSizeChangedListener.onSizeChanged(w, h, oldw, oldh);
        }
    }

    public void setOnSizeChangedListener(OnSizeChangedListener listener) {
        mOnSizeChangedListener = listener;
    }
}
