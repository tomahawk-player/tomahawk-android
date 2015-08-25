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

import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.services.PlaybackService;

import android.content.res.Configuration;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * {@link PagerAdapter} which provides functionality to swipe an AlbumArt image. Used in {@link
 * org.tomahawk.tomahawk_android.fragments.PlaybackFragment}
 */
public class AlbumArtSwipeAdapter extends PagerAdapter {

    //Used to provide fake infinite swiping behaviour, if current Playlist is repeating
    private static final int FAKE_INFINITY_COUNT = 20000;

    private final TomahawkMainActivity mActivity;

    private final LayoutInflater mLayoutInflater;

    private int mFakeInfinityOffset;

    private final ViewPager mViewPager;

    private PlaybackService mPlaybackService;

    private int mLastItem;

    private final ViewPager.OnPageChangeListener mOnPageChangeListener
            = new ViewPager.OnPageChangeListener() {
        /**
         * Is being called, whenever a new Page in our {@link AlbumArtSwipeAdapter} has been selected/
         * swiped to
         */
        @Override
        public void onPageSelected(int position) {
            if (mPlaybackService != null) {
                if (position == mLastItem - 1) {
                    mPlaybackService.previous();
                } else if (position == mLastItem + 1) {
                    mPlaybackService.next();
                }
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    };

    /**
     * Constructs a new AlbumArtSwipeAdapter.
     *
     * @param viewPager ViewPager which this adapter has been connected with
     */
    public AlbumArtSwipeAdapter(TomahawkMainActivity activity, LayoutInflater layoutInflater,
            ViewPager viewPager) {
        mActivity = activity;
        mLayoutInflater = layoutInflater;
        mViewPager = viewPager;
        mViewPager.setOnPageChangeListener(mOnPageChangeListener);
    }

    /**
     * Instantiate an item in the {@link PagerAdapter}. Fill it async with the correct AlbumArt
     * image.
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = mLayoutInflater.inflate(
                org.tomahawk.tomahawk_android.R.layout.album_art_view_pager_item, container, false);
        if (mPlaybackService != null) {
            if (mPlaybackService.getMergedPlaylist().size() > 0) {
                if (mPlaybackService.getRepeatingMode() != PlaybackService.NOT_REPEATING) {
                    position = position % mPlaybackService.getMergedPlaylist().size();
                }
                Query query =
                        mPlaybackService.getMergedPlaylist().getEntries().get(position).getQuery();
                if (query != null) {
                    ImageView imageView = (ImageView) view.findViewById(R.id.album_art_image);
                    boolean landscapeMode = mActivity.getResources().getConfiguration().orientation
                            == Configuration.ORIENTATION_LANDSCAPE;
                    TomahawkUtils.loadImageIntoImageView(mActivity, imageView, query.getImage(),
                            Image.getLargeImageSize(), landscapeMode, query.hasArtistImage());
                }
                mActivity.showPanel();
            } else {
                mActivity.hidePanel();
            }
        }
        if (view != null) {
            container.addView(view);
        }
        return view;
    }

    /**
     * @return If current {@link Playlist} is empty or null, return 1. If current {@link Playlist}
     * is repeating, return FAKE_INFINITY_COUNT. Else return the current {@link Playlist}'s length.
     */
    @Override
    public int getCount() {
        if (mPlaybackService == null || mPlaybackService.getMergedPlaylist().size() == 0) {
            return 1;
        }
        if (mPlaybackService.getRepeatingMode() != PlaybackService.NOT_REPEATING) {
            return FAKE_INFINITY_COUNT;
        }
        return mPlaybackService.getMergedPlaylist().size();
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

    /**
     * Dummy method
     *
     * @return always null
     */
    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    /**
     * @param entry        to set the current item to
     * @param smoothScroll boolean to determine whether or not to show a scrolling animation
     */
    private void setCurrentItem(PlaylistEntry entry, boolean smoothScroll) {
        int position = mPlaybackService.getMergedPlaylist().getIndexOfEntry(entry);
        if (mPlaybackService.getRepeatingMode() != PlaybackService.NOT_REPEATING) {
            position += mFakeInfinityOffset;
        }
        if (position != mViewPager.getCurrentItem()) {
            if (mPlaybackService.getRepeatingMode() != PlaybackService.NOT_REPEATING) {
                int currentItem = mViewPager.getCurrentItem();
                if (position == (currentItem % mPlaybackService.getMergedPlaylist().size()) + 1
                        || ((currentItem % mPlaybackService.getMergedPlaylist().size())
                        == mPlaybackService.getMergedPlaylist().size() - 1 && position == 0)) {
                    setCurrentViewPagerItem(mViewPager.getCurrentItem() + 1, smoothScroll);
                } else if (position
                        == (currentItem % mPlaybackService.getMergedPlaylist().size()) - 1
                        || ((currentItem % mPlaybackService.getMergedPlaylist().size()) == 0
                        && position == mPlaybackService.getMergedPlaylist().size() - 1)) {
                    setCurrentViewPagerItem(mViewPager.getCurrentItem() - 1, smoothScroll);
                } else {
                    setCurrentViewPagerItem(position, false);
                }
            } else {
                setCurrentViewPagerItem(position, smoothScroll);
            }
        }
        mLastItem = position;
    }

    private void setCurrentViewPagerItem(int position, boolean smoothScroll) {
        mViewPager.setOnPageChangeListener(null);
        mViewPager.setCurrentItem(position, smoothScroll);
        mViewPager.setOnPageChangeListener(mOnPageChangeListener);
    }

    /**
     * Update the {@link Playlist} of the {@link AlbumArtSwipeAdapter} with the current one in
     * {@link org.tomahawk.tomahawk_android.services.PlaybackService}
     */
    public void updatePlaylist() {
        if (mPlaybackService != null) {
            notifyDataSetChanged();
            int size = mPlaybackService.getMergedPlaylist().size();
            if (size > 0) {
                mFakeInfinityOffset = size * ((FAKE_INFINITY_COUNT / 2) / size);
                setCurrentItem(mPlaybackService.getCurrentEntry(), false);
            }
        }
    }

    /**
     * Set this {@link AlbumArtSwipeAdapter}'s {@link PlaybackService} reference
     */
    public void setPlaybackService(PlaybackService playbackService) {
        mPlaybackService = playbackService;
        updatePlaylist();
    }
}
