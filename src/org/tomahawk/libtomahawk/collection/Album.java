/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
package org.tomahawk.libtomahawk.collection;

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class which represents a Tomahawk Album.
 */
public class Album extends BitmapItem implements TomahawkBaseAdapter.TomahawkListItem {

    private static final String TAG = Album.class.getName();

    private static ConcurrentHashMap<Long, Album> sAlbums = new ConcurrentHashMap<Long, Album>();

    private ConcurrentHashMap<Long, Track> mTracks;

    private long mId;

    private String mName;

    private String mAlbumArtPath;

    private static Bitmap sAlbumPlaceHolderBitmap;

    private String mFirstYear;

    private String mLastYear;

    private Artist mArtist;

    private float mScore;

    public Album() {
        mTracks = new ConcurrentHashMap<Long, Track>();
    }

    public Album(long id) {
        setId(id);
        mTracks = new ConcurrentHashMap<Long, Track>();
    }

    /**
     * Construct a new Album from the id
     */
    public static Album get(long id) {

        if (!sAlbums.containsKey(id)) {
            sAlbums.put(id, new Album(id));
        }

        return sAlbums.get(id);
    }

    /* 
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return mName;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.TomahawkListItem#getName()
     */
    @Override
    public String getName() {
        return mName;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.TomahawkListItem#getArtist()
     */
    @Override
    public Artist getArtist() {
        return mArtist;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.TomahawkListItem#getAlbum()
     */
    @Override
    public Album getAlbum() {
        return this;
    }

    /**
     * Add a Track to this Album.
     */
    public void addTrack(Track track) {
        mTracks.put(track.getId(), track);
    }

    /**
     * Get a list of all Tracks from this Album.
     */
    public ArrayList<Track> getTracks() {
        ArrayList<Track> tracks = new ArrayList<Track>(mTracks.values());
        Collections.sort(tracks, new TrackComparator(TrackComparator.COMPARE_DISCNUM));
        return tracks;
    }

    /**
     * Return the Album id.
     */
    public long getId() {
        return mId;
    }

    /**
     * Set the Album id.
     */
    public void setId(long id) {
        this.mId = id;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getAlbumArtPath() {
        return mAlbumArtPath;
    }

    public void setAlbumArtPath(String albumArt) {
        mAlbumArtPath = albumArt;
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context     the context needed for fetching resources
     * @param asyncBitmap the {@link android.widget.ImageView}, which will be used to show the
     *                    {@link android.graphics.Bitmap}
     */
    public void loadBitmap(Context context, AsyncBitmap asyncBitmap) {
        Bitmap placeHolderBitmap;
        String pathToBitmap;
        if (sAlbumPlaceHolderBitmap == null) {
            sAlbumPlaceHolderBitmap = BitmapFactory
                    .decodeResource(context.getResources(), R.drawable.no_album_art_placeholder);
        }
        placeHolderBitmap = sAlbumPlaceHolderBitmap;
        pathToBitmap = getAlbumArtPath();
        if (pathToBitmap != null) {
            if (cancelPotentialWork(pathToBitmap, asyncBitmap)) {
                final BitmapWorkerTask task = new BitmapWorkerTask(context, asyncBitmap,
                        sAlbumPlaceHolderBitmap);
                asyncBitmap.setBitmapWorkerTaskReference(new WeakReference<BitmapWorkerTask>(task));
                task.execute(getAlbumArtPath());
            }
        } else {
            asyncBitmap.bitmap = placeHolderBitmap;
        }
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context   the context needed for fetching resources
     * @param imageView the {@link android.widget.ImageView}, which will be used to show the {@link
     *                  android.graphics.Bitmap}
     */
    public void loadBitmap(Context context, ImageView imageView) {
        Bitmap placeHolderBitmap;
        String pathToBitmap;
        if (sAlbumPlaceHolderBitmap == null) {
            sAlbumPlaceHolderBitmap = BitmapFactory
                    .decodeResource(context.getResources(), R.drawable.no_album_art_placeholder);
        }
        placeHolderBitmap = sAlbumPlaceHolderBitmap;
        pathToBitmap = getAlbumArtPath();
        if (pathToBitmap != null) {
            if (cancelPotentialWork(pathToBitmap, imageView)) {
                final BitmapWorkerTask task = new BitmapWorkerTask(imageView,
                        sAlbumPlaceHolderBitmap);
                final AsyncDrawable asyncDrawable = new AsyncDrawable(context.getResources(),
                        placeHolderBitmap, task);
                imageView.setImageDrawable(asyncDrawable);
                task.execute(getAlbumArtPath());
            }
        } else {
            imageView.setImageBitmap(placeHolderBitmap);
        }
    }

    public String getFirstYear() {
        return mFirstYear;
    }

    public void setFirstYear(String firstYear) {
        mFirstYear = firstYear;
    }

    public String getLastYear() {
        return mLastYear;
    }

    public void setLastYear(String lastYear) {
        mLastYear = lastYear;
    }

    public void setArtist(Artist artist) {
        mArtist = artist;
    }

    public float getScore() {
        return mScore;
    }

    public void setScore(float score) {
        this.mScore = score;
    }

}
