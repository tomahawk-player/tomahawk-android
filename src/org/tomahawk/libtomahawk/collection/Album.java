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

import org.tomahawk.libtomahawk.resolver.DataBaseResolver;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.QueryComparator;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
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
 * Class which represents a Tomahawk {@link Album}.
 */
public class Album extends BitmapItem implements TomahawkBaseAdapter.TomahawkListItem {

    private static ConcurrentHashMap<String, Album> sAlbums
            = new ConcurrentHashMap<String, Album>();

    private ConcurrentHashMap<String, Query> mQueries;

    private String mName;

    private Artist mArtist;

    private String mAlbumArtPath;

    private static Bitmap sAlbumPlaceHolderBitmap;

    private String mFirstYear;

    private String mLastYear;

    private boolean mContainsLocalQueries = false;

    /**
     * Construct a new {@link Album}
     */
    private Album(String albumName, Artist artist) {
        mName = albumName;
        mArtist = artist;
        mQueries = new ConcurrentHashMap<String, Query>();
    }

    /**
     * Returns the {@link Album} with the given album name and {@link org.tomahawk.libtomahawk.collection.Artist}.
     * If none exists in our static {@link ConcurrentHashMap} yet, construct and add it.
     */
    public static Album get(String albumName, Artist artist) {
        if (artist == null) {
            artist = Artist.get("");
        }
        Album album = new Album(albumName, artist);
        String key = TomahawkUtils.getCacheKey(album);
        if (!sAlbums.containsKey(key)) {
            sAlbums.put(key, album);
        }
        return sAlbums.get(key);
    }

    /**
     * Get the {@link org.tomahawk.libtomahawk.collection.Album} by providing its cache key
     */
    public static Album getAlbumByKey(String key) {
        return sAlbums.get(key);
    }

    /**
     * @return A {@link java.util.ArrayList} of all {@link Album}s
     */
    public static ArrayList<Album> getAlbums() {
        ArrayList<Album> albums = new ArrayList<Album>(sAlbums.values());
        Collections.sort(albums, new AlbumComparator(AlbumComparator.COMPARE_ALPHA));
        return albums;
    }

    /**
     * @return A {@link java.util.ArrayList} of all local {@link Album}s
     */
    public static ArrayList<Album> getLocalAlbums() {
        ArrayList<Album> albums = new ArrayList<Album>();
        for (Album album : sAlbums.values()) {
            if (album.containsLocalQueries()) {
                albums.add(album);
            }
        }
        Collections.sort(albums, new AlbumComparator(AlbumComparator.COMPARE_ALPHA));
        return albums;
    }

    /**
     * @return the {@link Album}'s name
     */
    @Override
    public String toString() {
        return mName;
    }

    /**
     * @return the {@link Album}'s name
     */
    @Override
    public String getName() {
        return mName;
    }

    /**
     * @return the {@link Album}'s {@link Artist}
     */
    @Override
    public Artist getArtist() {
        return mArtist;
    }

    /**
     * @return this {@link Album}
     */
    @Override
    public Album getAlbum() {
        return this;
    }

    /**
     * Add a {@link Track} to this {@link Album}.
     *
     * @param query the {@link Track} to be added
     */
    public void addQuery(Query query) {
        if (query.getPreferredTrackResult().getResolvedBy() instanceof DataBaseResolver) {
            mContainsLocalQueries = true;
        }
        mQueries.put(TomahawkUtils.getCacheKey(query), query);
    }

    /**
     * Get a list of all {@link org.tomahawk.libtomahawk.resolver.Query}s from this {@link Album}.
     *
     * @return list of all {@link org.tomahawk.libtomahawk.resolver.Query}s from this {@link Album}.
     */
    public ArrayList<Query> getQueries() {
        ArrayList<Query> queries = new ArrayList<Query>(mQueries.values());
        Collections.sort(queries, new QueryComparator(QueryComparator.COMPARE_ALBUMPOS));
        return queries;
    }

    /**
     * Get a list of all local {@link org.tomahawk.libtomahawk.resolver.Query}s from this {@link
     * Album}.
     *
     * @return list of all local {@link org.tomahawk.libtomahawk.resolver.Query}s from this {@link
     * Album}.
     */
    public ArrayList<Query> getLocalQueries() {
        ArrayList<Query> queries = new ArrayList<Query>();
        for (Query query : mQueries.values()) {
            if (query.getPreferredTrackResult().isLocal()) {
                queries.add(query);
            }
        }
        Collections.sort(queries, new QueryComparator(QueryComparator.COMPARE_ALBUMPOS));
        return queries;
    }

    /**
     * @return the filePath/url to this {@link Album}'s albumArt
     */
    public String getAlbumArtPath() {
        return mAlbumArtPath;
    }

    /**
     * Set filePath/url to albumArt of this {@link Album}
     *
     * @param albumArt filePath/url to albumArt of this {@link Album}
     */
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
            if (cancelPotentialWork(pathToBitmap, getBitmapWorkerTask(asyncBitmap))) {
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
            if (cancelPotentialWork(pathToBitmap, getBitmapWorkerTask(imageView))) {
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

    /**
     * @return {@link String} containing the first year info
     */
    public String getFirstYear() {
        return mFirstYear;
    }

    /**
     * Set the first year info
     *
     * @param firstYear {@link String} containing first year info
     */
    public void setFirstYear(String firstYear) {
        mFirstYear = firstYear;
    }

    /**
     * @return {@link String} containing the last year info
     */
    public String getLastYear() {
        return mLastYear;
    }

    /**
     * Set the last year info
     *
     * @param lastYear {@link String} containing last year info
     */
    public void setLastYear(String lastYear) {
        mLastYear = lastYear;
    }

    /**
     * @return whether or not this {@link Album} only contains non local queries
     */
    public boolean containsLocalQueries() {
        return mContainsLocalQueries;
    }

}
