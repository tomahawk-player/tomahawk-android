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
import org.tomahawk.tomahawk_android.TomahawkApp;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Class which represents a Tomahawk {@link Album}.
 */
public class Album extends Cacheable implements AlphaComparable, ArtistAlphaComparable {

    public final static String RELEASETYPE_UNKNOWN = "unknown";

    public final static String RELEASETYPE_ALBUM = "album";

    public final static String RELEASETYPE_EPS = "ep";

    private final String mName;

    private final Artist mArtist;

    private Image mImage;

    private String mReleaseType;

    /**
     * Construct a new {@link Album}
     */
    private Album(String albumName, Artist artist) {
        super(Album.class, getCacheKey(albumName, artist.getName()));

        mName = albumName != null ? albumName : "";
        mArtist = artist;
    }

    /**
     * Returns the {@link Album} with the given album name and {@link org.tomahawk.libtomahawk.collection.Artist}.
     * If none exists in our static {@link ConcurrentHashMap} yet, construct and add it.
     */
    public static Album get(String albumName, Artist artist) {
        Cacheable cacheable = get(Album.class, getCacheKey(albumName, artist.getName()));
        return cacheable != null ? (Album) cacheable : new Album(albumName, artist);
    }

    public static Album getByKey(String cacheKey) {
        return (Album) get(Album.class, cacheKey);
    }

    /**
     * @return the {@link Album}'s name
     */
    public String getName() {
        return mName;
    }

    /**
     * @return the name that should be displayed
     */
    public String getPrettyName() {
        return getName().isEmpty() ?
                TomahawkApp.getContext().getResources().getString(R.string.unknown_album)
                : getName();
    }

    /**
     * @return the filePath/url to this {@link Album}'s albumArt
     */
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
     * @return the {@link Album}'s {@link Artist}
     */
    public Artist getArtist() {
        return mArtist;
    }

    public String getReleaseType() {
        return mReleaseType;
    }

    public void setReleaseType(String releaseType) {
        mReleaseType = releaseType;
    }

    public String toShortString() {
        return "'" + getName() + "'";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "( " + toShortString() + " by "
                + getArtist().toShortString() + " )@" + Integer.toHexString(hashCode());
    }

}
