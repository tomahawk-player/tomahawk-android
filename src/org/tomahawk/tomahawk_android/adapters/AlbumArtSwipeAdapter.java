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

import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.services.PlaybackService;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;

/**
 * {@link PagerAdapter} which provides functionality to swipe an AlbumArt image. Used in {@link
 * org.tomahawk.tomahawk_android.fragments.PlaybackFragment}
 */
public class AlbumArtSwipeAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener {

    //Used to provide fake infinite swiping behaviour, if current Playlist is repeating
    private static final int FAKE_INFINITY_COUNT = 20000;

    private Context mContext;

    private int mFakeInfinityOffset;

    private boolean mByUser;

    private boolean mSwiped;

    //The ViewPager used to display the AlbumArt images
    private ViewPager mViewPager;

    private PlaybackService mPlaybackService;

    private Playlist mPlaylist;

    private int mCurrentViewPage = 0;

    /**
     * Constructs a new AlbumArtSwipeAdapter.
     *
     * @param context   the {@link Context} needed to call .loadBitmap in {@link
     *                  org.tomahawk.libtomahawk.collection.Album}
     * @param viewPager {@link ViewPager} used to display the AlbumArt images
     */
    public AlbumArtSwipeAdapter(Context context, ViewPager viewPager) {
        this.mContext = context;
        this.mByUser = true;
        this.mSwiped = false;
        this.mViewPager = viewPager;
        this.mViewPager.setAdapter(this);
        this.mViewPager.setOnPageChangeListener(this);
    }

    /**
     * Instantiate an item in the {@link PagerAdapter}. Fill it async with the correct AlbumArt
     * image.
     */
    @Override
    public Object instantiateItem(View collection, int position) {
        ImageView albumArtImageView = new ImageView(mContext);
        if (mPlaylist != null && mPlaylist.getCount() > 0) {
            if (mPlaylist.isRepeating()
                    && mPlaylist.peekTrackAtPos((position) % mPlaylist.getCount()).getAlbum()
                    != null) {
                mPlaylist.peekTrackAtPos((position) % mPlaylist.getCount()).getAlbum()
                        .loadBitmap(mContext, albumArtImageView);
            } else if (!mPlaylist.isRepeating()
                    && mPlaylist.peekTrackAtPos(position).getAlbum() != null) {
                mPlaylist.peekTrackAtPos(position).getAlbum()
                        .loadBitmap(mContext, albumArtImageView);
            } else {
                albumArtImageView.setImageResource(R.drawable.no_album_art_placeholder);
            }
        } else {
            albumArtImageView.setImageResource(R.drawable.no_album_art_placeholder);
        }
        ((ViewPager) collection).addView(albumArtImageView);
        return albumArtImageView;
    }

    /**
     * @return If current {@link Playlist} is empty or null, return 1. If current {@link Playlist}
     *         is repeating, return FAKE_INFINITY_COUNT. Else return the current {@link Playlist}'s
     *         length.
     */
    @Override
    public int getCount() {
        if (mPlaylist == null || mPlaylist.getCount() == 0) {
            return 1;
        }
        if (mPlaylist.isRepeating()) {
            return FAKE_INFINITY_COUNT;
        }
        return mPlaylist.getCount();
    }

    /**
     * @return the offset by which the position should be shifted, when {@link Playlist} is
     *         repeating
     */
    public int getFakeInfinityOffset() {
        return mFakeInfinityOffset;
    }

    /**
     * Remove the given {@link View} from the {@link ViewPager}
     */
    @Override
    public void destroyItem(View arg0, int arg1, Object arg2) {
        ((ViewPager) arg0).removeView((View) arg2);
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

    /**
     * @return POSITION_NONE
     */
    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    /**
     * Is being called, whenever a new Page in our {@link AlbumArtSwipeAdapter} has been selected/
     * swiped to
     */
    @Override
    public void onPageSelected(int arg0) {
        if (mPlaybackService != null && isByUser()) {
            setSwiped(true);
            if (arg0 == mCurrentViewPage - 1) {
                mPlaybackService.previous();
            } else if (arg0 == mCurrentViewPage + 1) {
                mPlaybackService.next();
            }
        }
        mCurrentViewPage = arg0;
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    /**
     * @param position     to set the current item to
     * @param smoothScroll boolean to determine whether or not to show a scrolling animation
     */
    public void setCurrentItem(int position, boolean smoothScroll) {
        if (position != mCurrentViewPage) {
            if (mPlaylist.isRepeating()) {
                if (position == (mCurrentViewPage % mPlaylist.getCount()) + 1 || (
                        (mCurrentViewPage % mPlaylist.getCount()) == mPlaylist.getCount() - 1
                                && position == 0)) {
                    setCurrentToNextItem(smoothScroll);
                } else if (position == (mCurrentViewPage % mPlaylist.getCount()) - 1 || (
                        (mCurrentViewPage % mPlaylist.getCount()) == 0
                                && position == mPlaylist.getCount() - 1)) {
                    setCurrentToPreviousItem(smoothScroll);
                } else {
                    mViewPager.setCurrentItem(position, false);
                }
            } else {
                mViewPager.setCurrentItem(position, smoothScroll);
            }
            mCurrentViewPage = mViewPager.getCurrentItem();
        }
    }

    /**
     * @param smoothScroll boolean to determine whether or not to show a scrolling animation
     */
    public void setCurrentToNextItem(boolean smoothScroll) {
        mViewPager.setCurrentItem(mCurrentViewPage + 1, smoothScroll);
    }

    /**
     * @param smoothScroll boolean to determine whether or not to show a scrolling animation
     */
    public void setCurrentToPreviousItem(boolean smoothScroll) {
        mViewPager.setCurrentItem(mCurrentViewPage - 1, smoothScroll);
    }

    /**
     * update the {@link Playlist} of the {@link AlbumArtSwipeAdapter} to the given {@link
     * Playlist}
     */
    public void updatePlaylist() {
        if (mPlaybackService != null) {
            mPlaylist = mPlaybackService.getCurrentPlaylist();
            notifyDataSetChanged();
        }
        if (mPlaylist != null && mPlaylist.getCount() > 0) {
            mFakeInfinityOffset = mPlaylist.getCount() * ((FAKE_INFINITY_COUNT / 2) / mPlaylist
                    .getCount());
            setByUser(false);
            if (mPlaylist.isRepeating()) {
                setCurrentItem(mPlaylist.getCurrentTrackIndex() + getFakeInfinityOffset(), false);
            } else {
                setCurrentItem(mPlaylist.getCurrentTrackIndex(), false);
            }
            setByUser(true);
        }
    }

    /**
     * @return whether or not previous swipe was done by user
     */
    public boolean isByUser() {
        return mByUser;
    }

    /**
     * Set whether or not previous swipe was done by user
     */
    public void setByUser(boolean byUser) {
        this.mByUser = byUser;
    }

    /**
     * @return whether or not previous skipping to next/previous {@link org.tomahawk.libtomahawk.collection.Track}
     *         was induced by swiping
     */
    public boolean isSwiped() {
        return mSwiped;
    }

    /**
     * Set whether or not previous skipping to next/previous {@link org.tomahawk.libtomahawk.collection.Track}
     * was induced by swiping
     */
    public void setSwiped(boolean isSwiped) {
        this.mSwiped = isSwiped;
    }

    /**
     * Set this {@link AlbumArtSwipeAdapter}'s {@link PlaybackService} reference
     */
    public void setPlaybackService(PlaybackService mPlaybackService) {
        this.mPlaybackService = mPlaybackService;
        updatePlaylist();
    }

}
