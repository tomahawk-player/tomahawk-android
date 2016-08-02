/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.utils.ImageUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.listeners.MediaImageLoadedListener;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import java.util.ArrayList;
import java.util.List;

public class MediaImageHelper {

    private static final String TAG = MediaImageHelper.class.getSimpleName();

    private static final int MAX_ALBUM_ART_CACHE_SIZE = 5 * 1024 * 1024;

    private static class Holder {

        private static final MediaImageHelper instance = new MediaImageHelper();

    }

    private List<MediaImageLoadedListener> mListeners = new ArrayList<>();

    private Bitmap mCachedPlaceHolder;

    private final LruCache<Image, Bitmap> mMediaImageCache =
            new LruCache<Image, Bitmap>(MAX_ALBUM_ART_CACHE_SIZE) {
                @Override
                protected int sizeOf(Image key, Bitmap value) {
                    return value.getByteCount();
                }
            };

    private MediaImageTarget mMediaImageTarget;

    private class MediaImageTarget implements Target {

        private Image mImageToLoad;

        public MediaImageTarget(Image imageToLoad) {
            mImageToLoad = imageToLoad;
        }

        @Override
        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            new Runnable() {
                @Override
                public void run() {
                    Bitmap copy = bitmap.copy(bitmap.getConfig(), false);
                    if (mImageToLoad != null) {
                        mMediaImageCache.put(mImageToLoad, copy);
                    }
                    for (MediaImageLoadedListener listener : mListeners) {
                        listener.onMediaImageLoaded();
                    }
                    Log.d(TAG, "Setting lockscreen bitmap");
                }
            }.run();
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
        }

        @Override
        public void onPrepareLoad(Drawable drawable) {
        }
    }

    private MediaImageHelper() {
        Drawable drawable =
                TomahawkApp.getContext().getResources().getDrawable(R.drawable.album_placeholder);
        mCachedPlaceHolder = ImageUtils.drawableToBitmap(drawable);
    }

    public static MediaImageHelper get() {
        return Holder.instance;
    }

    public void addListener(MediaImageLoadedListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(MediaImageLoadedListener listener) {
        mListeners.remove(listener);
    }

    public void loadMediaImage(final Image image) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mMediaImageTarget == null || mMediaImageTarget.mImageToLoad != image) {
                    mMediaImageTarget = new MediaImageTarget(image);
                    ImageUtils.loadImageIntoBitmap(TomahawkApp.getContext(), image,
                            mMediaImageTarget, Image.getLargeImageSize(), false);
                }
            }
        });
    }

    public Bitmap getCachedPlaceHolder() {
        return mCachedPlaceHolder;
    }

    public LruCache<Image, Bitmap> getMediaImageCache() {
        return mMediaImageCache;
    }

}
