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
package org.tomahawk.libtomahawk;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class LocalCollection extends Collection {

    private static final String TAG = LocalCollection.class.getName();

    private ContentResolver mResolver;
    private ArrayList<Track> mTracks;

    /**
     * Construct a new LocalCollection and initialize.
     * 
     * @param resolver
     */
    public LocalCollection(ContentResolver resolver) {
        super();
        mResolver = resolver;
        mTracks = new ArrayList<Track>();

        initializeCollection();
    }

    /**
     * Add newTracks to the list of all tracks.
     */
    @Override
    public void addTracks(ArrayList<Track> newTracks) {
        mTracks.addAll(newTracks);
    }

    /**
     * Return a list of all Tracks.
     */
    @Override
    public ArrayList<Track> getTracks() {
        return mTracks;
    }

    /**
     * Returns whether this Collection is a local collection.
     */
    @Override
    public boolean isLocal() {
        return true;
    }

    /**
     * Initialize the LocalCollection of all music files on the device.
     */
    private void initializeCollection() {

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.Audio.Media._ID,
                         MediaStore.Audio.Media.DATA,
                         MediaStore.Audio.Media.TITLE,
                         MediaStore.Audio.Media.ALBUM,
                         MediaStore.Audio.Media.ARTIST,
                         MediaStore.Audio.Media.ALBUM_ID,
                         MediaStore.Audio.Media.ARTIST_ID,
                         MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK };

        Cursor cursor = mResolver.query(uri, projection, selection, null, null);

        while (cursor != null && cursor.moveToNext()) {
            Log.d(TAG, "New Track : " + cursor.getString(2));
            Track track = new Track(cursor.getLong(0));
            track.populate(cursor);

            mTracks.add(track);
        }
    }

}
