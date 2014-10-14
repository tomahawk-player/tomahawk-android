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

import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.QueryComparator;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class which represents a Tomahawk {@link Album}.
 */
public class Album implements TomahawkListItem {

    private static ConcurrentHashMap<String, Album> sAlbums
            = new ConcurrentHashMap<String, Album>();

    private String mCacheKey;

    private HashSet<String> mQueryKeys = new HashSet<String>();

    private ArrayList<Query> mQueries = new ArrayList<Query>();

    private String mName;

    private Artist mArtist;

    private Image mImage;

    private String mFirstYear;

    private String mLastYear;

    /**
     * Construct a new {@link Album}
     */
    private Album(String albumName, Artist artist) {
        if (albumName == null) {
            mName = "";
        } else {
            mName = albumName;
        }
        mArtist = artist;
        if (mCacheKey == null) {
            mCacheKey = TomahawkUtils.getCacheKey(this);
        }
        mArtist.addAlbum(this);
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
        return ensureCache(album);
    }

    /**
     * If Album is already in our cache, return that. Otherwise add it to the cache.
     */
    private static Album ensureCache(Album album) {
        if (!sAlbums.containsKey(album.getCacheKey())) {
            sAlbums.put(album.getCacheKey(), album);
        }
        return sAlbums.get(album.getCacheKey());
    }

    public String getCacheKey() {
        return mCacheKey;
    }

    /**
     * Get the {@link org.tomahawk.libtomahawk.collection.Album} by providing its cache key
     */
    public static Album getAlbumByKey(String key) {
        return sAlbums.get(key);
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
        synchronized (this) {
            if (!mQueryKeys.contains(query.getCacheKey())) {
                mQueries.add(query);
                mQueryKeys.add(query.getCacheKey());
            }
        }
    }

    /**
     * Add a list of {@link Query}s to this {@link Album}.
     *
     * @param queries the list of {@link Query}s to be added
     */
    public void addQueries(List<Query> queries) {
        for (Query query : queries) {
            addQuery(query);
        }
    }

    /**
     * Get a list of all {@link org.tomahawk.libtomahawk.resolver.Query}s from this {@link Album}.
     *
     * @return list of all {@link org.tomahawk.libtomahawk.resolver.Query}s from this {@link Album}.
     */
    @Override
    public ArrayList<Query> getQueries() {
        ArrayList<Query> queries = new ArrayList<Query>(mQueries);
        synchronized (this) {
            Collections.sort(queries, new QueryComparator(QueryComparator.COMPARE_ALBUMPOS));
        }
        return queries;
    }

    /**
     * @return the filePath/url to this {@link Album}'s albumArt
     */
    @Override
    public Image getImage() {
        return mImage;
    }

    /**
     * Set filePath/url to albumArt of this {@link Album}
     *
     * @param image filePath/url to albumArt of this {@link Album}
     */
    public void setImage(Image image) {
        mImage = image;
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

}
