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

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PageIndicator extends LinearLayout implements ViewPager.OnPageChangeListener {

    private ViewPager mViewPager;

    private List<TextView> mTextViews = new ArrayList<TextView>();

    public PageIndicator(Context context) {
        super(context);
    }

    public PageIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setViewPager(ViewPager viewPager) {
        mViewPager = viewPager;
        populate();
    }

    private void populate() {
        removeAllViews();
        mTextViews.clear();
        for (int i = 0; i < mViewPager.getAdapter().getCount(); i++) {
            TextView textView = new TextView(getContext());
            textView.setText(mViewPager.getAdapter().getPageTitle(i));
            textView.setLayoutParams(new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            if (i == mViewPager.getAdapter().getCount() - 1) {
                textView.setGravity(Gravity.RIGHT);
            } else if (i != 0) {
                textView.setGravity(Gravity.CENTER_HORIZONTAL);
            }
            addView(textView);
            mTextViews.add(textView);
            mViewPager.setOnPageChangeListener(this);
            updateColors(mViewPager.getCurrentItem());
        }
    }

    private void updateColors(int position) {
        for (int i = 0; i < mTextViews.size(); i++) {
            TextView textView = mTextViews.get(i);
            if (i == position) {
                textView.setTextColor(
                        getResources().getColor(R.color.primary_textcolor_inverted));
            } else {
                textView.setTextColor(
                        getResources().getColor(R.color.secondary_textcolor_inverted));
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
}
