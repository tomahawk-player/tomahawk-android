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
import org.tomahawk.tomahawk_android.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 * 
 */
public class AlbumArtSwipeAdapter extends PagerAdapter {

    private Playlist mPlaylist;
    private Context mContext;

    /**
     * Constructs a new AlbumArtSwipeAdapter with the given list of AlbumArt
     * images
     */
    public AlbumArtSwipeAdapter(Context mContext, Playlist mPlaylist) {
        this.mPlaylist = mPlaylist;
        this.mContext = mContext;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.support.v4.view.PagerAdapter#instantiateItem(android.view.View,
     * int)
     */
    @Override
    public Object instantiateItem(View collection, int position) {
        ImageView albumArt = new ImageView(mContext);
        if (mPlaylist != null) {
            Bitmap albumArtBitmap = mPlaylist.getTrackAtPos(position)
                    .getAlbum().getAlbumArt();
            if (albumArtBitmap != null)
                albumArt.setImageBitmap(albumArtBitmap);
            else
                albumArt.setImageResource(R.drawable.no_album_art_placeholder);
        } else
            albumArt.setImageResource(R.drawable.no_album_art_placeholder);
        ((ViewPager) collection).addView(albumArt);
        return albumArt;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.view.PagerAdapter#getCount()
     */
    @Override
    public int getCount() {
        if (mPlaylist != null)
            return mPlaylist.getCount();
        return 1;
    }

    /**
     * get the current playlist
     * 
     * @return
     */
    protected Playlist getPlaylist() {
        return mPlaylist;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.view.PagerAdapter#destroyItem(android.view.View,
     * int, java.lang.Object)
     */
    @Override
    public void destroyItem(View arg0, int arg1, Object arg2) {
        ((ViewPager) arg0).removeView((View) arg2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.support.v4.view.PagerAdapter#isViewFromObject(android.view.View,
     * java.lang.Object)
     */
    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == ((View) arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.view.PagerAdapter#saveState()
     */
    @Override
    public Parcelable saveState() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.support.v4.view.PagerAdapter#getItemPosition(java.lang.Object)
     */
    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

}
