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

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * This class forwards TouchEvents to mForwardView if the GestureDetector has detected a vertical
 * scroll.
 */
public class BiDirectionalFrame extends FrameLayout {

    private final GestureDetector mGestureDetector;

    private boolean mVerticallyScrolled;

    private boolean mHorizontallyScrolled;

    private MotionEvent mDownMotionEvent;

    private boolean mTouchCancelled;

    private View mForwardView;

    private class ShouldSwipeDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mVerticallyScrolled = Math.abs(distanceY) >= Math.abs(distanceX);
            mHorizontallyScrolled = Math.abs(distanceX) >= Math.abs(distanceY);
            return false;
        }
    }

    public BiDirectionalFrame(Context context) {
        super(context);
        mGestureDetector = new GestureDetector(context, new ShouldSwipeDetector());
    }

    public BiDirectionalFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(context, new ShouldSwipeDetector());
    }

    public void setForwardView(View forwardView) {
        mForwardView = forwardView;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (mForwardView != null) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mDownMotionEvent = MotionEvent.obtain(event);
                    mVerticallyScrolled = false;
                    mHorizontallyScrolled = false;
                    mTouchCancelled = false;
                    break;
            }

            if (!mVerticallyScrolled && !mHorizontallyScrolled) {
                mGestureDetector.onTouchEvent(event);
            }

            if (mVerticallyScrolled) {
                getParent().requestDisallowInterceptTouchEvent(false);
                if (mDownMotionEvent != null) {
                    super.onTouchEvent(mDownMotionEvent);
                    mDownMotionEvent = null;
                }
                super.onTouchEvent(event);
                if (!mTouchCancelled) {
                    mTouchCancelled = true;
                    MotionEvent cancel = MotionEvent.obtain(event);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    mForwardView.dispatchTouchEvent(cancel);
                    cancel.recycle();
                }
            } else {
                getParent().requestDisallowInterceptTouchEvent(true);
                mForwardView.dispatchTouchEvent(event);
                getParent().requestDisallowInterceptTouchEvent(true);
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    getParent().requestDisallowInterceptTouchEvent(false);
            }
            return true;
        }

        return false;
    }
}
