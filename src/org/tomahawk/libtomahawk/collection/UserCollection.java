/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
package org.tomahawk.libtomahawk.collection;

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class represents a user's local {@link UserCollection}.
 */
public class UserCollection extends Collection {

    private HandlerThread mCollectionUpdateHandlerThread;

    private Handler mHandler;

    private Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            update();
            mCollectionUpdateHandlerThread.getLooper().quit();
        }
    };

    public UserCollection() {
        super(TomahawkApp.PLUGINNAME_USERCOLLECTION,
                PipeLine.getInstance().getResolver(TomahawkApp.PLUGINNAME_USERCOLLECTION)
                        .getCollectionName(), true);

        //This ContentObserver watches for changes in the Media db.
        ContentObserver localMediaObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                mCollectionUpdateHandlerThread.start();
                mHandler.post(mUpdateRunnable);
            }
        };
        TomahawkApp.getContext().getContentResolver().registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, false, localMediaObserver);

        mCollectionUpdateHandlerThread = new HandlerThread("CollectionUpdate",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mCollectionUpdateHandlerThread.start();

        mHandler = new Handler(mCollectionUpdateHandlerThread.getLooper());
        mHandler.postDelayed(mUpdateRunnable, 300);
    }

    /**
     * Initialize this {@link UserCollection}. Pull all local tracks/albums/artists from the {@link
     * MediaStore} and add them to our {@link UserCollection}.
     */
    private void initializeCollection() {
        Resolver userCollectionResolver = PipeLine.getInstance().getResolver(
                TomahawkApp.PLUGINNAME_USERCOLLECTION);
        if (userCollectionResolver == null) {
            return;
        }

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DATE_ADDED};

        ContentResolver resolver = TomahawkApp.getContext().getContentResolver();

        Cursor cursor = resolver
                .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
                        null);
        HashMap<Long, Album> idAlbumsMap = new HashMap<Long, Album>();
        HashMap<Album, List<Query>> albumTracksMap = new HashMap<Album, List<Query>>();

        // Go through the complete set of data in the MediaStore
        while (cursor != null && cursor.moveToNext()) {
            Artist artist = Artist.get(cursor.getString(6));

            Album album = Album.get(cursor.getString(8), artist);
            String albumsel = MediaStore.Audio.Albums._ID + " == " + String.valueOf(
                    cursor.getLong(7));
            String[] albumproj = {MediaStore.Audio.Albums.ALBUM_ART,
                    MediaStore.Audio.Albums.FIRST_YEAR, MediaStore.Audio.Albums.LAST_YEAR};
            Cursor albumcursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    albumproj, albumsel, null, null);
            if (albumcursor != null && albumcursor.moveToNext()) {
                if (!TextUtils.isEmpty(albumcursor.getString(0))) {
                    album.setImage(Image.get(albumcursor.getString(0), false));
                }
                album.setFirstYear(albumcursor.getString(1));
                album.setLastYear(albumcursor.getString(2));
            }
            if (albumcursor != null) {
                albumcursor.close();
            }
            if (!idAlbumsMap.containsKey(cursor.getLong(7))) {
                idAlbumsMap.put(cursor.getLong(7), album);
            } else {
                album = idAlbumsMap.get(cursor.getLong(7));
            }

            Track track = Track.get(cursor.getString(2), album, artist);
            track.setDuration(cursor.getLong(3));
            track.setAlbumPos(cursor.getInt(4));

            Query query = Query.get(track.getName(), album.getName(), artist.getName(), true);
            Result result = Result.get(cursor.getString(1), track, userCollectionResolver,
                    query.getCacheKey());
            result.setTrackScore(1f);
            query.addTrackResult(result);

            addQuery(query, cursor.getInt(9));
            addAlbum(album);
            addArtist(artist);
            addArtistTracks(artist, query);
            addArtistAlbum(artist, album);
            if (albumTracksMap.get(album) == null) {
                albumTracksMap.put(album, new ArrayList<Query>());
            }
            albumTracksMap.get(album).add(query);
        }
        for (Album album : albumTracksMap.keySet()) {
            addAlbumTracks(album, albumTracksMap.get(album));
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * Reinitalize this {@link UserCollection} and send a broadcast letting everybody know.
     */
    public void update() {
        initializeCollection();
        CollectionManager.sendCollectionUpdatedBroadcast(null, null);
    }
}
