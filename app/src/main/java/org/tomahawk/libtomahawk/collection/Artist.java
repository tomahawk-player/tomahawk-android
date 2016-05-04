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

/**
 * This class represents an {@link Artist}.
 */
public class Artist extends Cacheable implements AlphaComparable {

    public static final Artist COMPILATION_ARTIST = new Artist("Various Artists");

    private final String mName;

    private ListItemString mBio;

    private Image mImage;

    /**
     * Construct a new {@link Artist}
     */
    private Artist(String artistName) {
        super(Artist.class, getCacheKey(artistName));

        mName = artistName != null ? artistName : "";
    }

    /**
     * Returns the {@link Artist} with the given album name. If none exists in the cache yet,
     * construct and add it.
     */
    public static Artist get(String artistName) {
        Cacheable cacheable = get(Artist.class, getCacheKey(artistName));
        return cacheable != null ? (Artist) cacheable : new Artist(artistName);
    }

    public static Artist getByKey(String cacheKey) {
        return (Artist) get(Artist.class, cacheKey);
    }

    /**
     * @return this object's name
     */
    public String getName() {
        return mName;
    }

    /**
     * @return the name that should be displayed
     */
    public String getPrettyName() {
        return getName().isEmpty() ?
                TomahawkApp.getContext().getResources().getString(R.string.unknown_artist)
                : getName();
    }

    public Image getImage() {
        return mImage;
    }

    public void setImage(Image image) {
        mImage = image;
    }

    public ListItemString getBio() {
        return mBio;
    }

    public void setBio(ListItemString bio) {
        mBio = bio;
    }

    public String toShortString() {
        return "'" + getName() + "'";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "( " + toShortString() + " )@"
                + Integer.toHexString(hashCode());
    }
}
