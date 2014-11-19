/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.utils;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

public class MediaWithDate extends Media {

    private long mDateAdded;

    public MediaWithDate(LibVLC libVLC, String URI, long dateAdded) {
        super(libVLC, URI);

        mDateAdded = dateAdded;
    }

    public MediaWithDate(java.lang.String location, long time, long length, int type,
            android.graphics.Bitmap picture, java.lang.String title, java.lang.String artist,
            java.lang.String genre, java.lang.String album, int width, int height,
            java.lang.String artworkURL, int audio, int spu, long dateAdded) {
        super(location, time, length, type, picture, title, artist, genre, album, width, height,
                artworkURL, audio, spu);

        mDateAdded = dateAdded;
    }

    public long getDateAdded() {
        return mDateAdded;
    }
}