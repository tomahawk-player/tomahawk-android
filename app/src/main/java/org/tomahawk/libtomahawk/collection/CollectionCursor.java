/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.tomahawk_android.utils.IdGenerator;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

public class CollectionCursor<T> {

    private final static String TAG = CollectionCursor.class.getSimpleName();

    private SparseArray<T> mCursorCache = new SparseArray<>();

    private Cursor mCursor;

    private int mCursorCount;

    private List<T> mItems;

    private Class<T> mClass;

    private Resolver mResolver;

    private Playlist mPlaylist;

    public CollectionCursor(Cursor cursor, Class<T> clss, Resolver resolver, Playlist playlist) {
        mCursor = cursor;
        mCursorCount = cursor.getCount();
        mClass = clss;
        if (clss == PlaylistEntry.class || clss == Result.class) {
            if (resolver != null) {
                mResolver = resolver;
            } else {
                throw new RuntimeException("Resolver is required for "
                        + "CollectionCursor<PlaylistEntry> or CollectionCursor<Result>!");
            }
        }
        if (clss == PlaylistEntry.class) {
            if (playlist != null) {
                mPlaylist = playlist;
            } else {
                throw new RuntimeException("Playlist is required for "
                        + "CollectionCursor<PlaylistEntry>!");
            }
        }
    }

    public CollectionCursor(List<T> items, Class<T> clss) {
        mItems = items;
        mClass = clss;
    }

    public CollectionCursor<T> copy() {
        CollectionCursor<T> copy;
        if (mCursor != null) {
            copy = new CollectionCursor<>(mCursor, mClass, mResolver, mPlaylist);
            SparseArray<T> cacheCopy = mCursorCache.clone();
            copy.setCursorCache(cacheCopy);
        } else {
            List<T> itemsCopy = new ArrayList<>();
            for (T item : mItems) {
                itemsCopy.add(item);
            }
            copy = new CollectionCursor<>(itemsCopy, mClass);
        }
        return copy;
    }

    public void close() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public void setCursorCache(SparseArray<T> cursorCache) {
        mCursorCache = cursorCache;
    }

    public T get(int location) {
        if (mCursor != null) {
            if (mCursor.isClosed()) {
                Log.d(TAG, "rawGet - Cursor has been closed.");
                return null;
            }
            T cachedItem = mCursorCache.get(location);
            if (cachedItem == null) {
                mCursor.moveToPosition(location);
                if (mClass == PlaylistEntry.class) {
                    Artist artist = Artist.get(mCursor.getString(0));
                    Album album = Album.get(mCursor.getString(2), artist);
                    Track track = Track.get(mCursor.getString(3), album, artist);
                    track.setDuration(mCursor.getInt(4) * 1000);
                    track.setAlbumPos(mCursor.getInt(7));
                    Result result = Result.get(mCursor.getString(5), track, mResolver);
                    Query query = Query.get(result, false);
                    query.addTrackResult(result, 1.0f);
                    PlaylistEntry entry = PlaylistEntry.get(mPlaylist.getId(), query,
                            IdGenerator.getLifetimeUniqueStringId());
                    cachedItem = (T) entry;
                } else if (mClass == Result.class) {
                    Artist artist = Artist.get(mCursor.getString(0));
                    Album album = Album.get(mCursor.getString(2), artist);
                    Track track = Track.get(mCursor.getString(3), album, artist);
                    track.setDuration(mCursor.getInt(4) * 1000);
                    track.setAlbumPos(mCursor.getInt(7));
                    Result result = Result.get(mCursor.getString(5), track, mResolver);
                    cachedItem = (T) result;
                } else if (mClass == Album.class) {
                    Artist artist = Artist.get(mCursor.getString(1));
                    Album album = Album.get(mCursor.getString(0), artist);
                    String imagePath = mCursor.getString(3);
                    if (!TextUtils.isEmpty(imagePath)) {
                        album.setImage(Image.get(imagePath, false));
                    }
                    cachedItem = (T) album;
                } else if (mClass == Artist.class) {
                    Artist artist = Artist.get(mCursor.getString(0));
                    cachedItem = (T) artist;
                }
                mCursorCache.put(location, cachedItem);
            }
            return cachedItem;
        } else {
            return mItems.get(location);
        }
    }

    public int size() {
        if (mCursor != null) {
            return mCursorCount;
        } else {
            return mItems.size();
        }
    }

    public String getArtistName(int location) {
        if (mCursor != null) {
            mCursor.moveToPosition(location);
            if (mClass == PlaylistEntry.class || mClass == Result.class || mClass == Artist.class) {
                return mCursor.getString(0);
            } else if (mClass == Album.class) {
                return mCursor.getString(1);
            }
        } else {
            Object o = mItems.get(location);
            if (o instanceof PlaylistEntry) {
                return ((PlaylistEntry) o).getArtist().getName();
            } else if (o instanceof Result) {
                return ((Result) o).getArtist().getName();
            } else if (o instanceof Album) {
                return ((Album) o).getArtist().getName();
            } else if (o instanceof Artist) {
                return ((Artist) o).getName();
            }
            return ((ArtistAlphaComparable) mItems.get(location)).getArtist().getName();
        }
        Log.e(TAG, "getArtistName(int location) - Couldn't return a string");
        return null;
    }
}
