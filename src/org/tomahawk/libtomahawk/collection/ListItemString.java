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
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import java.util.ArrayList;

/**
 * This class represents an {@link ListItemString}.
 */
public class ListItemString implements TomahawkListItem {

    private final String mText;

    private boolean mHighlighted;

    /**
     * Construct a new {@link ListItemString} with the given text
     */
    public ListItemString(String text, boolean highlighted) {
        this(text);

        mHighlighted = highlighted;
    }

    /**
     * Construct a new {@link ListItemString} with the given text
     */
    public ListItemString(String text) {
        if (text == null) {
            mText = "";
        } else {
            mText = text;
        }
    }

    @Override
    public String getCacheKey() {
        return mText;
    }

    @Override
    public String getName() {
        return mText;
    }

    @Override
    public Artist getArtist() {
        return null;
    }

    @Override
    public Album getAlbum() {
        return null;
    }

    @Override
    public ArrayList<Query> getQueries() {
        return null;
    }

    @Override
    public Image getImage() {
        return null;
    }

    public boolean isHighlighted() {
        return mHighlighted;
    }
}
