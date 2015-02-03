/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class PlaybackFragmentFrame extends FrameLayout {

    private SlidingUpPanelLayout mPanelLayout;

    private StickyListHeadersListView mListView;

    private GestureDetector mGestureDetector;

    private boolean mHasDetectedScroll;

    private boolean mVerticallyScrolled;

    private MotionEvent mDownMotionEvent;

    /**
     * Class to extend a {@link android.view.GestureDetector.SimpleOnGestureListener}, so that we
     * can apply our logic to manually solve the TouchEvent conflict.
     */
    private class ShouldSwipeDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!mHasDetectedScroll) {
                mHasDetectedScroll = true;
                mVerticallyScrolled = Math.abs(distanceY) >= Math.abs(distanceX) && distanceY < 0;
            }
            return true;
        }
    }

    public PlaybackFragmentFrame(Context context) {
        super(context);
        mGestureDetector = new GestureDetector(context, new ShouldSwipeDetector());
    }

    public PlaybackFragmentFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(context, new ShouldSwipeDetector());
    }

    public void setPanelLayout(SlidingUpPanelLayout panelLayout) {
        mPanelLayout = panelLayout;
    }

    public void setListView(StickyListHeadersListView listView) {
        mListView = listView;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        mGestureDetector.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownMotionEvent = MotionEvent.obtain(event);
                mHasDetectedScroll = false;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                forwardTouchEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mHasDetectedScroll) {
                    forwardTouchEvent(event);
                }
                break;
        }

        return true;
    }

    private void forwardTouchEvent(MotionEvent event) {
        if (!mPanelLayout.isPanelExpanded()
                || (mVerticallyScrolled && isListViewScrolledUp())) {
            getParent().requestDisallowInterceptTouchEvent(false);
            if (mDownMotionEvent != null) {
                super.onTouchEvent(mDownMotionEvent);
                mDownMotionEvent = null;
            }
            super.onTouchEvent(event);
        } else {
            if (mDownMotionEvent != null) {
                getChildAt(0).dispatchTouchEvent(mDownMotionEvent);
                mDownMotionEvent = null;
            }
            getChildAt(0).dispatchTouchEvent(event);
        }
    }

    private boolean isListViewScrolledUp() {
        return mListView.getFirstVisiblePosition() == 0
                && (mListView.getListChildAt(0) == null
                || mListView.getListChildAt(0).getTop() >= 0);
    }
}
