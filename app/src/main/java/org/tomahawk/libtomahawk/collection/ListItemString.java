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

/**
 * This class represents an {@link ListItemString}.
 */
public class ListItemString {

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

    public String getText() {
        return mText;
    }

    public boolean isHighlighted() {
        return mHighlighted;
    }
}
