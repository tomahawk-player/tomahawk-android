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
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.services.PlaybackService;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link PagerAdapter} which provides functionality to swipe an AlbumArt image. Used in {@link
 * org.tomahawk.tomahawk_android.fragments.PlaybackFragment}
 */
public class AlbumArtSwipeAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener {

    //Used to provide fake infinite swiping behaviour, if current Playlist is repeating
    private static final int FAKE_INFINITY_COUNT = 20000;

    private ActionBarActivity mActivity;

    private int mFakeInfinityOffset;

    private boolean mByUser;

    private boolean mSwiped;

    private ViewPager mViewPager;

    private View.OnLongClickListener mOnLongClickListener;

    private PlaybackService mPlaybackService;

    private Playlist mPlaylist;

    private int mCurrentViewPage = 0;

    /**
     * Constructs a new AlbumArtSwipeAdapter.
     *
     * @param activity  the {@link Context} needed to call .loadBitmap in {@link
     *                  org.tomahawk.libtomahawk.collection.Album}
     * @param viewPager ViewPager which this adapter has been connected with
     */
    public AlbumArtSwipeAdapter(ActionBarActivity activity, ViewPager viewPager,
            View.OnLongClickListener onLongClickListener) {
        mActivity = activity;
        mViewPager = viewPager;
        mOnLongClickListener = onLongClickListener;
        mByUser = true;
        mSwiped = false;
    }

    /**
     * Instantiate an item in the {@link PagerAdapter}. Fill it async with the correct AlbumArt
     * image.
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = mActivity.getLayoutInflater().inflate(
                org.tomahawk.tomahawk_android.R.layout.album_art_view_pager_item, container, false);
        if (mPlaylist != null && mPlaylist.getCount() > 0) {
            Query query;
            if (mPlaylist.isRepeating()) {
                query = mPlaylist.peekQueryAtPos((position) % mPlaylist.getCount());
            } else {
                query = mPlaylist.peekQueryAtPos(position);
            }
            refreshTrackInfo(view, query);
        } else {
            refreshTrackInfo(view, null);
        }
        if (view != null) {
            view.setOnLongClickListener(mOnLongClickListener);
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
     * repeating
     */
    public int getFakeInfinityOffset() {
        return mFakeInfinityOffset;
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
                setCurrentItem(mPlaylist.getCurrentQueryIndex() + getFakeInfinityOffset(), false);
            } else {
                setCurrentItem(mPlaylist.getCurrentQueryIndex(), false);
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
     * was induced by swiping
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

    private void refreshTrackInfo(View view, Query query) {
        TextView artistTextView = (TextView) view.findViewById(R.id.textView_artist);
        TextView albumTextView = (TextView) view.findViewById(R.id.textView_album);
        TextView titleTextView = (TextView) view.findViewById(R.id.textView_title);
        if (query != null) {
            ImageView imageView = (ImageView) view.findViewById(R.id.album_art_image);
            TomahawkUtils
                    .loadImageIntoImageView(mActivity, imageView, query, Image.IMAGE_SIZE_LARGE);

            // Update all relevant TextViews
            if (artistTextView != null) {
                if (query.getArtist() != null && query.getArtist().getName() != null) {
                    artistTextView.setText(query.getArtist().toString());
                } else {
                    artistTextView.setText(R.string.playbackactivity_unknown_string);
                }
            }
            if (albumTextView != null) {
                if (query.getAlbum() != null && query.getAlbum().getName() != null) {
                    albumTextView.setText(query.getAlbum().toString());
                } else {
                    albumTextView.setText(R.string.playbackactivity_unknown_string);
                }
            }
            if (titleTextView != null) {
                if (query.getName() != null) {
                    titleTextView.setText(query.getName());
                } else {
                    titleTextView.setText(R.string.playbackactivity_unknown_string);
                }
            }

        } else {
            //No track has been given, so we update the view state accordingly
            // Update all relevant TextViews

            if (artistTextView != null) {
                artistTextView.setText("");
            }
            if (albumTextView != null) {
                albumTextView.setText("");
            }
            if (titleTextView != null) {
                titleTextView.setText(R.string.playbackactivity_no_track);
            }
        }
    }

}
