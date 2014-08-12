/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.adapters;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * {@link android.support.v4.view.PagerAdapter} which provides functionality to swipe vertically
 * between the AlbumArt image and the playback listview. Used in {@link
 * org.tomahawk.tomahawk_android.fragments.PlaybackFragment}
 */
public class PlaybackPagerAdapter extends PagerAdapter {

    private FrameLayout mViewPagerFrame;

    private StickyListHeadersListView mListView;

    /**
     * Constructs a new PlaybackPagerAdapter.
     *
     * @param viewPagerFrame ViewPager frame to display as the first item of this adapter
     * @param listView       listview showing the current playback list
     */
    public PlaybackPagerAdapter(FrameLayout viewPagerFrame,
            StickyListHeadersListView listView) {
        mViewPagerFrame = viewPagerFrame;
        mListView = listView;
    }

    /**
     * Instantiate an item in the {@link PagerAdapter}.
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view;
        if (position == 0) {
            view = mViewPagerFrame;
        } else {
            view = mListView;
        }
        container.addView(view);
        return view;
    }

    /**
     * @return always 2
     */
    @Override
    public int getCount() {
        return 2;
    }

    /**
     * Remove the given {@link View} from the {@link ViewPager}
     */
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    /**
     * @return true if view is equal to object
     */
    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
}
