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

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * A {@link StickyListHeadersListView} extended by a {@link GestureDetector}, so that we can use it
 * inside our {@link org.tomahawk.tomahawk_android.fragments.PlaybackFragment}'s {@link
 * TomahawkStickyListHeadersListView}. The {@link GestureDetector} is being used, so that the user
 * is able to swipe the AlbumArt horizontally inside {@link org.tomahawk.tomahawk_android.fragments.PlaybackControlsFragment}
 * and to scroll vertically through the {@link StickyListHeadersListView}. Without manually deciding
 * which one to scroll/swipe, the two TouchEvents would conflict with each other.
 */
public class TomahawkStickyListHeadersListView extends StickyListHeadersListView {

    private final static String TAG = TomahawkStickyListHeadersListView.class.getName();

    private GestureDetector gestureDetector;

    /**
     * Construct a {@link TomahawkStickyListHeadersListView}
     */
    public TomahawkStickyListHeadersListView(Context context) {
        super(context);
        gestureDetector = new GestureDetector(context, new YScrollDetector());
    }

    /**
     * Construct a {@link TomahawkStickyListHeadersListView}
     */
    public TomahawkStickyListHeadersListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestureDetector = new GestureDetector(context, new YScrollDetector());
    }

    /**
     * Construct a {@link TomahawkStickyListHeadersListView}
     */
    public TomahawkStickyListHeadersListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        gestureDetector = new GestureDetector(context, new YScrollDetector());
    }

    /**
     * Intercept the TouchEvent, so that we can manually solve swipe horizontally/ scroll vertically
     * conflict.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean result = super.onInterceptTouchEvent(ev);
        return gestureDetector.onTouchEvent(ev) && result;
    }

    /**
     * Class to extend a {@link android.view.GestureDetector.SimpleOnGestureListener}, so that we
     * can apply our logic to manually solve the TouchEvent conflict.
     */
    private class YScrollDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            try {
                // User wants to scroll vertically
                return Math.abs(distanceY) > Math.abs(distanceX);
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
            // User wants to swipe horizontally, prohibit scrolling
            return false;
        }
    }
}
