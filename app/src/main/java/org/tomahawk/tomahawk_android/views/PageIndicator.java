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

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.TomahawkPagerAdapter;
import org.tomahawk.tomahawk_android.fragments.PagerFragment;
import org.tomahawk.tomahawk_android.utils.AnimationUtils;
import org.tomahawk.tomahawk_android.utils.FragmentInfo;
import org.tomahawk.tomahawk_android.listeners.OnSizeChangedListener;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PageIndicator extends LinearLayout implements ViewPager.OnPageChangeListener {

    private ViewPager mViewPager;

    private List<PagerFragment.FragmentInfoList> mFragmentInfosList;

    private final List<View> mItems = new ArrayList<>();

    private View mRootview;

    private Selector mSelector;

    private String mSelectorPosStorageKey;

    private OnSizeChangedListener mOnSizeChangedListener;

    public PageIndicator(Context context) {
        super(context);
    }

    public PageIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setup(ViewPager viewPager, List<PagerFragment.FragmentInfoList> fragmentInfosList,
            View rootView, Selector selector, String selectorPosStorageKey) {
        mViewPager = viewPager;
        mFragmentInfosList = fragmentInfosList;
        mRootview = rootView;
        mSelector = selector;
        mSelectorPosStorageKey = selectorPosStorageKey;
        populate();
    }

    private void populate() {
        removeAllViews();
        mItems.clear();
        for (int i = 0; i < mViewPager.getAdapter().getCount(); i++) {
            LayoutInflater inflater =
                    (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View item = inflater.inflate(R.layout.page_indicator_item, this, false);
            final TextView textView = (TextView) item.findViewById(R.id.textview);
            textView.setText(mViewPager.getAdapter().getPageTitle(i));
            final int j = i;
            if (mFragmentInfosList.get(i).size() > 1) {
                final ImageView arrow = (ImageView) item.findViewById(R.id.arrow);
                arrow.setVisibility(VISIBLE);
                item.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!mSelector.isListShowing()) {
                            rotateArrow(arrow, false);

                            mViewPager.setCurrentItem(j);

                            final List<FragmentInfo> fragmentInfos =
                                    mFragmentInfosList.get(j).getFragmentInfos();
                            Selector.SelectorListener selectorListener
                                    = new Selector.SelectorListener() {
                                @Override
                                public void onSelectorItemSelected(int position) {
                                    rotateArrow(arrow, true);

                                    FragmentInfo selectedItem = fragmentInfos.get(position);
                                    ((TomahawkPagerAdapter) mViewPager.getAdapter()).changeFragment(
                                            j, selectedItem);
                                    TextView textView =
                                            (TextView) mItems.get(j).findViewById(R.id.textview);
                                    textView.setText(selectedItem.mTitle);
                                    ImageView imageView =
                                            (ImageView) item.findViewById(R.id.imageview);
                                    imageView.setImageResource(selectedItem.mIconResId);
                                }

                                @Override
                                public void onCancel() {
                                    rotateArrow(arrow, true);
                                }
                            };
                            mSelector.setup(fragmentInfos, selectorListener, mRootview,
                                    mSelectorPosStorageKey);
                            mSelector.showSelectorList();
                        } else {
                            rotateArrow(arrow, true);

                            mSelector.hideSelectorList();
                        }
                    }
                });
            } else {
                item.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSelector.hideSelectorList();
                        mViewPager.setCurrentItem(j);
                    }
                });
            }
            if (mFragmentInfosList.get(i).getCurrentFragmentInfo().mIconResId > 0) {
                ImageView imageView = (ImageView) item.findViewById(R.id.imageview);
                imageView.setVisibility(VISIBLE);
                imageView.setImageResource(
                        mFragmentInfosList.get(i).getCurrentFragmentInfo().mIconResId);
            }
            if (i != 0) {
                View spacer = inflater.inflate(R.layout.page_indicator_spacer, this, false);
                addView(spacer);
            }
            addView(item);
            mItems.add(item);
            updateColors(mViewPager.getCurrentItem());
        }
    }

    private void rotateArrow(View arrow, boolean reverse) {
        RotateAnimation rotate;
        if (reverse) {
            rotate = new RotateAnimation(180, 360,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
        } else {
            rotate = new RotateAnimation(360, 180,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
        }
        rotate.setDuration(AnimationUtils.DURATION_ARROWROTATE);
        arrow.startAnimation(rotate);
        rotate.setFillAfter(true);
    }

    private void updateColors(int position) {
        for (int i = 0; i < mItems.size(); i++) {
            TextView textView = (TextView) mItems.get(i).findViewById(R.id.textview);
            ImageView imageView = (ImageView) mItems.get(i).findViewById(R.id.imageview);
            ImageView arrow = (ImageView) mItems.get(i).findViewById(R.id.arrow);
            if (i == position) {
                textView.setTextColor(
                        getResources().getColor(R.color.primary_textcolor_inverted));
                imageView.clearColorFilter();
                arrow.clearColorFilter();
            } else {
                textView.setTextColor(
                        getResources().getColor(R.color.tertiary_textcolor_inverted));
                ColorFilter grayOutFilter = new PorterDuffColorFilter(
                        TomahawkApp.getContext().getResources()
                                .getColor(R.color.tertiary_textcolor_inverted),
                        PorterDuff.Mode.MULTIPLY);
                imageView.setColorFilter(grayOutFilter);
                arrow.setColorFilter(grayOutFilter);
            }
        }
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {
    }

    @Override
    public void onPageSelected(int position) {
        updateColors(position);
    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (mOnSizeChangedListener != null) {
            mOnSizeChangedListener.onSizeChanged(w, h, oldw, oldh);
        }
    }

    public void setOnSizeChangedListener(OnSizeChangedListener listener) {
        mOnSizeChangedListener = listener;
    }
}
