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

import org.tomahawk.tomahawk_android.fragments.PlaybackFragment;
import org.tomahawk.tomahawk_android.services.PlaybackService;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * A {@link AlbumArtViewPager} extended by a {@link android.view.GestureDetector}, so that we can
 * use it inside our {@link org.tomahawk.tomahawk_android.fragments.PlaybackFragment}. The {@link
 * android.view.GestureDetector} is being used, so that the user is able to scroll up to the
 * AlbumArt, if the listview in this vertical viewpager is at the topmost position and the user
 * swipes his finger from the bottom to the top of the screen. Otherwise we want to scroll inside
 * our listview, so we don't intercept the touchevent in that case.
 */
public class AlbumArtViewPager extends ViewPager {

    private final GestureDetector mGestureDetector;

    private PlaybackFragment.ShowContextMenuListener mShowContextMenuListener;

    private PlaybackService mPlaybackService;

    /**
     * Class to extend a {@link android.view.GestureDetector.SimpleOnGestureListener}, so that we
     * can apply our logic to manually solve the TouchEvent conflict.
     */
    private class ShouldSwipeDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public void onLongPress(MotionEvent e) {
            mShowContextMenuListener.onShowContextMenu(mPlaybackService.getCurrentQuery());
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            mShowContextMenuListener.onDoubleTap(mPlaybackService.getCurrentQuery());
            return false;
        }
    }

    public AlbumArtViewPager(Context context) {
        super(context);
        mGestureDetector = new GestureDetector(context, new ShouldSwipeDetector());
    }

    public AlbumArtViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(context, new ShouldSwipeDetector());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        super.onTouchEvent(event);

        return true;
    }

    public void setShowContextMenuListener(
            PlaybackFragment.ShowContextMenuListener showContextMenuListener) {
        mShowContextMenuListener = showContextMenuListener;
    }

    public void setPlaybackService(PlaybackService playbackService) {
        mPlaybackService = playbackService;
    }
}
