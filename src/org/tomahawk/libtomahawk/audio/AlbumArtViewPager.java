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
package org.tomahawk.libtomahawk.audio;

import org.tomahawk.libtomahawk.playlist.Playlist;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 * 
 */
public class AlbumArtViewPager extends ViewPager {

    private AlbumArtSwipeAdapter mAlbumArtSwipeAdapter;
    private PlaybackService mPlaybackService;
    private OnPageChangeListener mOnPageChangeListener;

    private int mCurrentViewPage = 0;
    private boolean mByUser;
    private boolean mSwiped;

    public AlbumArtViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        setByUser(true);
        setSwiped(false);
        mAlbumArtSwipeAdapter = new AlbumArtSwipeAdapter(getContext(), null);
        setAdapter(mAlbumArtSwipeAdapter);
        setCurrentItem(0, false);
        mOnPageChangeListener = new OnPageChangeListener() {

            /* (non-Javadoc)
             * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageSelected(int)
             */
            @Override
            public void onPageSelected(int arg0) {
                if (isByUser() && mPlaybackService != null) {
                    setSwiped(true);
                    if (arg0 == mCurrentViewPage - 1)
                        mPlaybackService.previous();
                    else if (arg0 == mCurrentViewPage + 1)
                        mPlaybackService.next();
                }
                if (mAlbumArtSwipeAdapter.getPlaylist().isRepeating())
                    mCurrentViewPage = mAlbumArtSwipeAdapter.getPlaylist().getPosition() + mAlbumArtSwipeAdapter.getFakeInfinityOffset();
                else
                    mCurrentViewPage = mAlbumArtSwipeAdapter.getPlaylist().getPosition();
                Log.d("test",
                        "onPageSelected(int): currentViewPage = " + mCurrentViewPage + " arg0 = " + arg0);
            }

            /* (non-Javadoc)
             * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrolled(int, float, int)
             */
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            /* (non-Javadoc)
             * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrollStateChanged(int)
             */
            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        };
        setOnPageChangeListener(mOnPageChangeListener);
    }

    /**
     * update the playlist of the AlbumArtSwipeAdapter to the given Playlist
     * 
     * @param playList
     */
    public void updatePlaylist(Playlist playList) {
        mAlbumArtSwipeAdapter = new AlbumArtSwipeAdapter(getContext(), playList);
        setAdapter(mAlbumArtSwipeAdapter);
        if (mAlbumArtSwipeAdapter.getPlaylist().isRepeating()) {
            setCurrentItem(playList.getPosition() + mAlbumArtSwipeAdapter.getFakeInfinityOffset(), false);
            mCurrentViewPage = mAlbumArtSwipeAdapter.getPlaylist().getPosition() + mAlbumArtSwipeAdapter.getFakeInfinityOffset();
        } else {
            setCurrentItem(playList.getPosition(), false);
            mCurrentViewPage = mAlbumArtSwipeAdapter.getPlaylist().getPosition();
        }
    }

    public boolean isByUser() {
        return mByUser;
    }

    public void setByUser(boolean byUser) {
        this.mByUser = byUser;
    }

    public boolean isSwiped() {
        return mSwiped;
    }

    public void setSwiped(boolean isSwiped) {
        this.mSwiped = isSwiped;
    }

    public boolean isPlaylistNull() {
        return mAlbumArtSwipeAdapter.getPlaylist() == null;
    }

    public void setPlaybackService(PlaybackService mPlaybackService) {
        this.mPlaybackService = mPlaybackService;
    }

    public void notifyDataSetChanged() {
        mAlbumArtSwipeAdapter.notifyDataSetChanged();
    }
}