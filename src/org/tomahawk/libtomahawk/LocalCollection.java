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
package org.tomahawk.libtomahawk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;

public class LocalCollection extends Collection {

    private static final String TAG = LocalCollection.class.getName();

    private HandlerThread mCollectionUpdateHandlerThread;
    private Handler mHandler;

    private Map<Long, Artist> mArtists;
    private Map<Long, Album> mAlbums;
    private Map<Long, Track> mTracks;

    private Runnable mUpdateRunnable = new Runnable() {
        /* 
         * (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            update();
            mCollectionUpdateHandlerThread.getLooper().quit();
        }
    };

    /**
     * This class watches for changes in the Media db.
     */
    private final ContentObserver mLocalMediaObserver = new ContentObserver(null) {
        /* 
         * (non-Javadoc)
         * @see android.database.ContentObserver#onChange(boolean)
         */
        @Override
        public void onChange(boolean selfChange) {
            mCollectionUpdateHandlerThread.start();
            mHandler.post(mUpdateRunnable);
        }
    };

    /**
     * Construct a new LocalCollection and initialize.
     * 
     * @param resolver
     */
    public LocalCollection() {
        mArtists = new HashMap<Long, Artist>();
        mAlbums = new HashMap<Long, Album>();
        mTracks = new HashMap<Long, Track>();

        TomahawkApp.getContext().getContentResolver().registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, false, mLocalMediaObserver);

        mCollectionUpdateHandlerThread = new HandlerThread("CollectionUpdate",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mCollectionUpdateHandlerThread.start();

        mHandler = new Handler(mCollectionUpdateHandlerThread.getLooper());
        mHandler.postDelayed(mUpdateRunnable, 300);
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.Collection#getArtists()
     */
    @Override
    public List<Artist> getArtists() {
        ArrayList<Artist> artists = new ArrayList<Artist>(mArtists.values());
        Collections.sort(artists, new ArtistComparator());
        return artists;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.Collection#getArtistById(java.lang.Long)
     */
    @Override
    public Artist getArtistById(Long id) {
        return mArtists.get(id);
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.Collection#getAlbums()
     */
    @Override
    public List<Album> getAlbums() {
        ArrayList<Album> albums = new ArrayList<Album>(mAlbums.values());
        Collections.sort(albums, new AlbumComparator());
        return albums;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.Collection#getAlbumById(java.lang.Long)
     */
    @Override
    public Album getAlbumById(Long id) {
        return mAlbums.get(id);
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.Collection#getTracks()
     */
    @Override
    public List<Track> getTracks() {
        ArrayList<Track> tracks = new ArrayList<Track>(mTracks.values());
        Collections.sort(tracks, new TrackComparator(TrackComparator.COMPARE_ALPHA));
        return tracks;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.Collection#getTrackById(java.lang.Long)
     */
    @Override
    public Track getTrackById(Long id) {
        return mTracks.get(id);
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.Collection#isLocal()
     */
    @Override
    public boolean isLocal() {
        return true;
    }

    /**
     * Initialize Tracks.
     */
    private void initializeCollection() {
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ALBUM };

        ContentResolver resolver = TomahawkApp.getContext().getContentResolver();

        Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, null);

        while (cursor != null && cursor.moveToNext()) {
            Artist artist = mArtists.get(cursor.getLong(5));
            if (artist == null) {
                artist = Artist.get(cursor.getLong(5));
                artist.setName(cursor.getString(6));

                mArtists.put(artist.getId(), artist);
                Log.d(TAG, "New Artist: " + artist.toString());
            }

            Album album = mAlbums.get(cursor.getLong(7));
            if (album == null) {
                album = Album.get(cursor.getLong(7));
                album.setName(cursor.getString(8));

                String albumsel = MediaStore.Audio.Albums._ID + " == " + Long.toString(album.getId());

                String[] albumproj = { MediaStore.Audio.Albums.ALBUM_ART, MediaStore.Audio.Albums.FIRST_YEAR,
                        MediaStore.Audio.Albums.LAST_YEAR };

                Cursor albumcursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumproj, albumsel,
                        null, null);

                if (albumcursor != null && albumcursor.moveToNext()) {

                    album.setAlbumArt(albumcursor.getString(0));
                    album.setFirstYear(albumcursor.getString(1));
                    album.setLastYear(albumcursor.getString(2));

                    mAlbums.put(album.getId(), album);
                    Log.d(TAG, "New Album: " + album.toString());
                }

                if (albumcursor != null)
                    albumcursor.close();
            }

            Track track = mTracks.get(cursor.getLong(0));
            if (track == null) {
                track = Track.get(cursor.getLong(0));
                track.setPath(cursor.getString(1));
                track.setName(cursor.getString(2));
                track.setDuration(cursor.getLong(3));
                track.setTrackNumber(cursor.getInt(4));

                mTracks.put(track.getId(), track);
                Log.d(TAG, "New Track: " + track.toString());
            }

            artist.addAlbum(album);
            artist.addTrack(track);

            album.addTrack(track);
            album.setArtist(artist);

            track.setAlbum(album);
            track.setArtist(artist);
        }

        if (cursor != null)
            cursor.close();
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.Collection#update()
     */
    @Override
    public void update() {
        initializeCollection();

        TomahawkApp.getContext().sendBroadcast(new Intent(COLLECTION_UPDATED));
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.Collection#getId()
     */
    @Override
    public int getId() {
        return 0;
    }
}
