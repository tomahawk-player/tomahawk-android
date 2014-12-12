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

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import fr.castorflex.android.verticalviewpager.VerticalViewPager;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * A {@link TomahawkVerticalViewPager} extended by a {@link android.view.GestureDetector}, so that
 * we can use it inside our {@link org.tomahawk.tomahawk_android.fragments.PlaybackFragment}. The
 * {@link android.view.GestureDetector} is being used, so that the user is able to scroll up to the
 * AlbumArt, if the listview in this vertical viewpager is at the topmost position and the user
 * swipes his finger from the bottom to the top of the screen. Otherwise we want to scroll inside
 * our listview, so we don't intercept the touchevent in that case.
 */
public class TomahawkVerticalViewPager extends VerticalViewPager {

    private StickyListHeadersListView mStickyListHeadersListView;

    private GestureDetector mGestureDetector;

    private boolean mPagingEnabled = true;

    /**
     * Class to extend a {@link android.view.GestureDetector.SimpleOnGestureListener}, so that we
     * can apply our logic to manually solve the TouchEvent conflict.
     */
    private class ShouldSwipeDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // we return true, if the scroll movement is upwards, false otherwise
            return distanceY < -5.;
        }
    }

    /**
     * Construct a {@link org.tomahawk.tomahawk_android.views.TomahawkVerticalViewPager}
     */
    public TomahawkVerticalViewPager(Context context) {
        super(context);
        mGestureDetector = new GestureDetector(context, new ShouldSwipeDetector());
    }

    /**
     * Construct a {@link org.tomahawk.tomahawk_android.views.TomahawkVerticalViewPager}
     */
    public TomahawkVerticalViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(context, new ShouldSwipeDetector());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mPagingEnabled && super.onTouchEvent(event);
    }

    /**
     * Intercept the TouchEvent, so that we can manually control, when to allow swiping upwards in
     * our vertical ViewPager
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mPagingEnabled) {
            boolean result = super.onInterceptTouchEvent(event);
            if (getCurrentItem() == 0) {
                return result;
            } else {
                return isListViewScrolledUp() && mGestureDetector.onTouchEvent(event);
            }
        }
        return false;
    }

    public void setStickyListHeadersListView(StickyListHeadersListView stickyListHeadersListView) {
        mStickyListHeadersListView = stickyListHeadersListView;
    }

    private boolean isListViewScrolledUp() {
        if (mStickyListHeadersListView != null
                && mStickyListHeadersListView.getListChildAt(0) != null) {
            return mStickyListHeadersListView.getFirstVisiblePosition() == 0
                    && mStickyListHeadersListView.getListChildAt(0).getTop() >= 0;
        }
        return false;
    }

    public void setPagingEnabled(boolean pagingEnabled) {
        mPagingEnabled = pagingEnabled;
    }

    public boolean isPagingEnabled() {
        return mPagingEnabled;
    }
}
