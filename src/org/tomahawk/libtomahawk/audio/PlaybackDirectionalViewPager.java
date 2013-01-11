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
package org.tomahawk.libtomahawk.audio;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.DirectionalViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com>
 * Date: 12.01.13
 */
public class PlaybackDirectionalViewPager extends DirectionalViewPager {

    private boolean mTouchEventHandlingEnabled = true;

    private class PlaybackOnPageChangeListener implements OnPageChangeListener {
        @Override
        public void onPageScrolled(int i, float v, int i2) {
        }

        @Override
        public void onPageSelected(int i) {
            if (i == 0)
                mTouchEventHandlingEnabled = true;
            else
                mTouchEventHandlingEnabled = false;
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }

    }

    public PlaybackDirectionalViewPager(Context context) {
        super(context);
        setOnPageChangeListener(new PlaybackOnPageChangeListener());
    }

    public PlaybackDirectionalViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnPageChangeListener(new PlaybackOnPageChangeListener());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mTouchEventHandlingEnabled)
            return super.onInterceptTouchEvent(ev);
        return false;
    }

    public void setTouchEventHandlingEnabled(boolean TouchEventHandlingEnabled) {
        this.mTouchEventHandlingEnabled = TouchEventHandlingEnabled;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        super.onSaveInstanceState();
        return null;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
    }
}
