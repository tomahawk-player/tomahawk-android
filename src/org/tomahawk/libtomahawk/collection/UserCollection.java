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

import org.tomahawk.libtomahawk.database.UserPlaylistsDataSource;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.QueryComparator;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a user's local {@link Collection} of all his {@link Track}s.
 */
public class UserCollection extends Collection {

    public static final int Id = 0;

    private TomahawkApp mTomahawkApp;

    private HandlerThread mCollectionUpdateHandlerThread;

    private Handler mHandler;

    private ConcurrentHashMap<String, Artist> mArtists = new ConcurrentHashMap<String, Artist>();

    private Artist mCachedArtist;

    private ConcurrentHashMap<String, Album> mAlbums = new ConcurrentHashMap<String, Album>();

    private Album mCachedAlbum;

    private ConcurrentHashMap<String, Query> mQueries = new ConcurrentHashMap<String, Query>();

    private UserPlaylist mCachedUserPlaylist;

    private ConcurrentHashMap<Long, UserPlaylist> mUserPlaylists
            = new ConcurrentHashMap<Long, UserPlaylist>();

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
        @Override
        public void onChange(boolean selfChange) {
            mCollectionUpdateHandlerThread.start();
            mHandler.post(mUpdateRunnable);
        }
    };

    /**
     * Construct a new {@link UserCollection} and initializes it.
     */
    public UserCollection(TomahawkApp tomahawkApp) {
        mTomahawkApp = tomahawkApp;

        TomahawkApp.getContext().getContentResolver()
                .registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, false,
                        mLocalMediaObserver);

        mCollectionUpdateHandlerThread = new HandlerThread("CollectionUpdate",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mCollectionUpdateHandlerThread.start();

        mHandler = new Handler(mCollectionUpdateHandlerThread.getLooper());
        mHandler.postDelayed(mUpdateRunnable, 300);
    }

    /**
     * @return A {@link List} of all {@link Artist}s in this {@link UserCollection}
     */
    @Override
    public ArrayList<Artist> getArtists() {
        ArrayList<Artist> artists = new ArrayList<Artist>(mArtists.values());
        Collections.sort(artists, new ArtistComparator(ArtistComparator.COMPARE_ALPHA));
        return artists;
    }

    /**
     * Get an {@link Artist} from this {@link UserCollection} by providing an id
     */
    @Override
    public Artist getArtistByKey(String key) {
        return mArtists.get(key);
    }

    /**
     * Caches an {@link Artist} in a private member variable
     */
    @Override
    public void setCachedArtist(Artist artist) {
        mCachedArtist = artist;
    }

    /**
     * @return the cached {@link Artist}
     */
    @Override
    public Artist getCachedArtist() {
        return mCachedArtist;
    }

    /**
     * @return A {@link java.util.ArrayList} of all {@link Album}s in this {@link UserCollection}
     */
    @Override
    public ArrayList<Album> getAlbums() {
        ArrayList<Album> albums = new ArrayList<Album>(mAlbums.values());
        Collections.sort(albums, new AlbumComparator(AlbumComparator.COMPARE_ALPHA));
        return albums;
    }

    /**
     * Get an {@link Album} from this {@link UserCollection} by providing an id
     */
    @Override
    public Album getAlbumByKey(String key) {
        return mAlbums.get(key);
    }


    /**
     * Caches an {@link Album} in a private member variable
     */
    @Override
    public void setCachedAlbum(Album album) {
        mCachedAlbum = album;
    }

    /**
     * @return the cached {@link Album}
     */
    @Override
    public Album getCachedAlbum() {
        return mCachedAlbum;
    }

    /**
     * @return A {@link List} of all {@link UserPlaylist}s in this {@link UserCollection}
     */
    @Override
    public ArrayList<UserPlaylist> getUserPlaylists() {
        ArrayList<UserPlaylist> userPlaylists = new ArrayList<UserPlaylist>(
                mUserPlaylists.values());
        Collections.sort(userPlaylists,
                new UserPlaylistComparator(UserPlaylistComparator.COMPARE_ALPHA));
        return userPlaylists;
    }

    /**
     * Get an {@link UserPlaylist} from this {@link UserCollection} by providing an id
     */
    @Override
    public UserPlaylist getCustomPlaylistByKey(String key) {
        return mUserPlaylists.get(key);
    }

    /**
     * Add a {@link UserPlaylist} to this {@link UserCollection}
     */
    public void addCustomPlaylist(long playlistId, UserPlaylist userPlaylist) {
        mUserPlaylists.put(playlistId, userPlaylist);
    }

    /**
     * Store the PlaybackService's currentPlaylist
     */
    public void setCachedPlaylist(UserPlaylist userPlaylist) {
        mCachedUserPlaylist = userPlaylist;
    }

    /**
     * @return the previously cached {@link UserPlaylist}
     */
    public UserPlaylist getCachedUserPlaylist() {
        return mCachedUserPlaylist;
    }

    /**
     * @return A {@link List} of all {@link Track}s in this {@link UserCollection}
     */
    @Override
    public ArrayList<Query> getQueries() {
        ArrayList<Query> queries = new ArrayList<Query>(mQueries.values());
        Collections.sort(queries, new QueryComparator(QueryComparator.COMPARE_ALPHA));
        return queries;
    }

    /**
     * @return always true
     */
    @Override
    public boolean isLocal() {
        return true;
    }

    /**
     * Initialize this {@link UserCollection}. Pull all local tracks from the {@link MediaStore} and
     * add them to our {@link UserCollection}
     */
    private void initializeCollection() {
        Resolver userCollectionResolver = mTomahawkApp.getPipeLine().getResolver(
                TomahawkApp.RESOLVER_ID_USERCOLLECTION);

        updateUserPlaylists();

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM};

        ContentResolver resolver = TomahawkApp.getContext().getContentResolver();

        Cursor cursor = resolver
                .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
                        null);

        // Go through the complete set of data in the MediaStore
        while (cursor != null && cursor.moveToNext()) {
            Artist artist = Artist.get(cursor.getString(6));
            String key = TomahawkUtils.getCacheKey(artist);
            mArtists.put(key, artist);

            Album album = Album.get(cursor.getString(8), artist);
            String albumsel = MediaStore.Audio.Albums._ID + " == " + Long.toString(
                    cursor.getLong(7));
            String[] albumproj = {MediaStore.Audio.Albums.ALBUM_ART,
                    MediaStore.Audio.Albums.FIRST_YEAR, MediaStore.Audio.Albums.LAST_YEAR};
            Cursor albumcursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    albumproj, albumsel, null, null);
            if (albumcursor != null && albumcursor.moveToNext()) {
                album.setAlbumArtPath(albumcursor.getString(0));
                album.setFirstYear(albumcursor.getString(1));
                album.setLastYear(albumcursor.getString(2));
            }
            if (albumcursor != null) {
                albumcursor.close();
            }
            key = TomahawkUtils.getCacheKey(album);
            mAlbums.put(key, album);

            artist.addAlbum(album);

            Track track = Track.get(cursor.getString(2), album, artist);
            track.setDuration(cursor.getLong(3));
            track.setAlbumPos(cursor.getInt(4));

            Query query = new Query(track.getName(), album.getName(), artist.getName(), true);
            Result result = Result.get(cursor.getString(1), track);
            result.setResolvedBy(userCollectionResolver);
            query.addTrackResult(result);
            mQueries.put(query.getQid(), query);

            artist.addQuery(query);
            album.addQuery(query);
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * Fetch all user {@link UserPlaylist} from the app's database via our helper class {@link
     * UserPlaylistsDataSource}
     */
    public void updateUserPlaylists() {
        mUserPlaylists.clear();
        ArrayList<UserPlaylist> userPlayListList = mTomahawkApp.getUserPlaylistsDataSource()
                .getAllUserPlaylists();
        for (UserPlaylist userPlaylist : userPlayListList) {
            if (userPlaylist.getId() == UserPlaylistsDataSource.CACHED_PLAYLIST_ID) {
                setCachedPlaylist(userPlaylist);
            } else {
                mUserPlaylists.put(userPlaylist.getId(), userPlaylist);
            }
        }
        TomahawkApp.getContext().sendBroadcast(new Intent(COLLECTION_UPDATED));
    }

    /**
     * Reinitalize this {@link UserCollection} and send a broadcast letting everybody know.
     */
    @Override
    public void update() {
        initializeCollection();

        TomahawkApp.getContext().sendBroadcast(new Intent(COLLECTION_UPDATED));
    }

    /**
     * @return this {@link UserCollection}'s id
     */
    @Override
    public int getId() {
        return Id;
    }
}
