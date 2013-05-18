/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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

import com.emilsjolander.components.stickylistheaders.StickyListHeadersListView;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 29.01.13
 */
public class TomahawkStickyListHeadersListView extends StickyListHeadersListView {

    private final static String TAG = TomahawkStickyListHeadersListView.class.getName();

    private GestureDetector gestureDetector;

    public TomahawkStickyListHeadersListView(Context context) {
        super(context);
        gestureDetector = new GestureDetector(new YScrollDetector());
    }

    public TomahawkStickyListHeadersListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestureDetector = new GestureDetector(new YScrollDetector());
    }

    public TomahawkStickyListHeadersListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        gestureDetector = new GestureDetector(new YScrollDetector());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean result = super.onInterceptTouchEvent(ev);
        if (gestureDetector.onTouchEvent(ev)) {
            return result;
        } else {
            return false;
        }
    }

    class YScrollDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            try {
                if (Math.abs(distanceY) > Math.abs(distanceX)) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
            return false;
        }
    }
}
