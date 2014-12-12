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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * A {@link org.tomahawk.tomahawk_android.views.TomahawkScrollView} extended by a {@link
 * android.view.GestureDetector}, so that we can use it inside a PagerFragment. The {@link
 * android.view.GestureDetector} is being used, so that the user is able to scroll up in the
 * scrollview, if the listview in this scrollview is at the topmost position and the user swipes his
 * finger from the top to the bottom of the screen. Otherwise we want to scroll inside our listview,
 * so we don't intercept the touchevent in that case.
 */
public class TomahawkScrollView extends ScrollView {

    private GestureDetector mGestureDetector;

    /**
     * Class to extend a {@link android.view.GestureDetector.SimpleOnGestureListener}, so that we
     * can apply our logic to manually solve the TouchEvent conflict.
     */
    private class ShouldSwipeDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // we return true, if the scroll movement is upwards, false otherwise
            return distanceY < -5. && distanceX < 40. && distanceX > -40.;
        }
    }

    /**
     * Construct a {@link org.tomahawk.tomahawk_android.views.TomahawkScrollView}
     */
    public TomahawkScrollView(Context context) {
        super(context);
        mGestureDetector = new GestureDetector(context, new ShouldSwipeDetector());
    }

    /**
     * Construct a {@link org.tomahawk.tomahawk_android.views.TomahawkScrollView}
     */
    public TomahawkScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(context, new ShouldSwipeDetector());
    }

    /**
     * Intercept the TouchEvent, so that we can manually control and handle the touch event conflict
     * between the scrollview and the listview inside the viewpager
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean result = super.onInterceptTouchEvent(event);
        int offset = getResources().getDimensionPixelSize(R.dimen.header_clear_space_scrollable)
                - getResources().getDimensionPixelSize(R.dimen.pager_indicator_height);
        if (getScrollY() < offset) {
            return result;
        } else {
            return isListViewScrolledUp() && mGestureDetector.onTouchEvent(event);
        }
    }

    private boolean isListViewScrolledUp() {
        if (findViewById(R.id.fragmentpager) != null) {
            ViewPager viewPager = (ViewPager) findViewById(R.id.fragmentpager);
            View view = viewPager.findViewWithTag(viewPager.getCurrentItem());
            if (view instanceof StickyListHeadersListView) {
                StickyListHeadersListView listView = (StickyListHeadersListView) view;
                //TODO fix np if listchild null
                return listView.getFirstVisiblePosition() == 0
                        && (listView.getListChildAt(0) == null
                        || listView.getListChildAt(0).getTop() >= 0);
            }
        }
        return false;
    }
}
