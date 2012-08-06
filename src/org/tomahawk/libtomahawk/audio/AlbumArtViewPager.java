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

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 * 
 */
public class AlbumArtViewPager extends ViewPager {

    private AlbumArtSwipeAdapter mAlbumArtSwipeAdapter;
    private PlaybackService mPlaybackService;
    private OnPageChangeListener mOnPageChangeListener;

    private int currentViewPage = 0;
    private boolean byUser;
    private boolean swiped;

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
                    if (arg0 == currentViewPage - 1) {
                        mPlaybackService.previous();
                    } else if (arg0 == currentViewPage + 1) {
                        mPlaybackService.next();
                    }
                }
                currentViewPage = mAlbumArtSwipeAdapter.getPlaylist().getPosition();
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
        setCurrentItem(playList.getPosition(), false);
    }

    public boolean isByUser() {
        return byUser;
    }

    public void setByUser(boolean byUser) {
        this.byUser = byUser;
    }

    public boolean isSwiped() {
        return swiped;
    }

    public void setSwiped(boolean isSwiped) {
        this.swiped = isSwiped;
    }

    public boolean isPlaylistNull() {
        return mAlbumArtSwipeAdapter.getPlaylist() == null;
    }

    public void setPlaybackService(PlaybackService mPlaybackService) {
        this.mPlaybackService = mPlaybackService;
    }
}