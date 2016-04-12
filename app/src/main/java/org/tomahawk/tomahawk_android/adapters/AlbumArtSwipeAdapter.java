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
import org.tomahawk.libtomahawk.utils.ImageUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.PlaybackManager;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
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

    private int mFakeInfinityOffset;

    private final ViewPager mViewPager;

    private int mLastItem;

    private MediaControllerCompat mMediaController;

    private PlaybackManager mPlaybackManager;

    private PlaybackStateCompat mPlaybackState;

    private int mPageScrollState = ViewPager.SCROLL_STATE_IDLE;

    private boolean mUpdateWhenIdle = false;

    private final ViewPager.OnPageChangeListener mOnPageChangeListener
            = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            if (position == mLastItem - 1) {
                mMediaController.getTransportControls().skipToPrevious();
            } else if (position == mLastItem + 1) {
                mMediaController.getTransportControls().skipToNext();
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            int lastState = mPageScrollState;
            mPageScrollState = state;
            if (mUpdateWhenIdle && lastState != ViewPager.SCROLL_STATE_IDLE
                    && state == ViewPager.SCROLL_STATE_IDLE) {
                mUpdateWhenIdle = false;
                updatePlaylist();
            }
        }
    };

    /**
     * Constructs a new AlbumArtSwipeAdapter.
     *
     * @param viewPager ViewPager which this adapter has been connected with
     */
    public AlbumArtSwipeAdapter(ViewPager viewPager) {
        mViewPager = viewPager;
        mViewPager.addOnPageChangeListener(mOnPageChangeListener);
    }

    public void setMediaController(MediaControllerCompat mediaController) {
        mMediaController = mediaController;
    }

    public void setPlaybackManager(PlaybackManager playbackManager) {
        mPlaybackManager = playbackManager;
    }

    /**
     * Instantiate an item in the {@link PagerAdapter}. Fill it async with the correct AlbumArt
     * image.
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = LayoutInflater.from(mViewPager.getContext()).inflate(
                org.tomahawk.tomahawk_android.R.layout.album_art_view_pager_item, container, false);
        if (mPlaybackManager != null && mPlaybackManager.getPlaybackListSize() > 0
                && mPlaybackState != null) {
            Bundle playbackStateExtras = mPlaybackState.getExtras();
            if (playbackStateExtras != null) {
                if (mPlaybackState.getExtras().getInt(PlaybackService.EXTRAS_KEY_REPEAT_MODE)
                        != PlaybackManager.NOT_REPEATING) {
                    position = position % mPlaybackManager.getPlaybackListSize();
                }
                Query query = mPlaybackManager.getPlaybackListEntry(position).getQuery();
                if (query != null) {
                    ImageView imageView = (ImageView) view.findViewById(R.id.album_art_image);
                    boolean landscapeMode = mViewPager.getResources().getConfiguration().orientation
                            == Configuration.ORIENTATION_LANDSCAPE;
                    ImageUtils.loadImageIntoImageView(mViewPager.getContext(), imageView,
                            query.getImage(), Image.getLargeImageSize(), landscapeMode,
                            query.hasArtistImage());
                }
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
        if (mPlaybackManager == null || mPlaybackManager.getPlaybackListSize() == 0) {
            return 1;
        }
        if (mPlaybackState.getExtras() != null
                && mPlaybackState.getExtras().getInt(PlaybackService.EXTRAS_KEY_REPEAT_MODE)
                != PlaybackManager.NOT_REPEATING) {
            return FAKE_INFINITY_COUNT;
        }
        return mPlaybackManager.getPlaybackListSize();
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
        Bundle playbackStateExtras = mPlaybackState.getExtras();
        if (playbackStateExtras != null) {
            int position = mPlaybackManager.getPlaybackListIndex(entry);
            if (playbackStateExtras.getInt(PlaybackService.EXTRAS_KEY_REPEAT_MODE)
                    != PlaybackManager.NOT_REPEATING) {
                position += mFakeInfinityOffset;
            }
            if (position != mViewPager.getCurrentItem()) {
                if (playbackStateExtras.getInt(PlaybackService.EXTRAS_KEY_REPEAT_MODE)
                        != PlaybackManager.NOT_REPEATING) {
                    int currentItem = mViewPager.getCurrentItem();
                    if (position == (currentItem % mPlaybackManager.getPlaybackListSize()) + 1
                            || ((currentItem % mPlaybackManager.getPlaybackListSize())
                            == mPlaybackManager.getPlaybackListSize() - 1 && position == 0)) {
                        setCurrentViewPagerItem(mViewPager.getCurrentItem() + 1, smoothScroll);
                    } else if (position
                            == (currentItem % mPlaybackManager.getPlaybackListSize()) - 1
                            || ((currentItem % mPlaybackManager.getPlaybackListSize()) == 0
                            && position == mPlaybackManager.getPlaybackListSize() - 1)) {
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
    }

    private void setCurrentViewPagerItem(int position, boolean smoothScroll) {
        mViewPager.clearOnPageChangeListeners();
        mViewPager.setCurrentItem(position, smoothScroll);
        mViewPager.addOnPageChangeListener(mOnPageChangeListener);
    }

    /**
     * Update the {@link Playlist} of the {@link AlbumArtSwipeAdapter} with the current one in
     * {@link org.tomahawk.tomahawk_android.services.PlaybackService}
     */
    public void updatePlaylist() {
        if (mPageScrollState != ViewPager.SCROLL_STATE_IDLE) {
            mUpdateWhenIdle = true;
            return;
        }
        if (mMediaController != null && mPlaybackManager != null) {
            mPlaybackState = mMediaController.getPlaybackState();
            notifyDataSetChanged();
            int size = mPlaybackManager.getPlaybackListSize();
            if (size > 0) {
                mFakeInfinityOffset = size * ((FAKE_INFINITY_COUNT / 2) / size);
                setCurrentItem(mPlaybackManager.getCurrentEntry(), false);
            }
        }
    }
}
