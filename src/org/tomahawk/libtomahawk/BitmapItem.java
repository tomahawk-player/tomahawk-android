/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 09.05.13
 */
public class BitmapItem {

    private static final String TAG = BitmapItem.class.getName();

    public static final String BITMAPITEM_BITMAPLOADED = "bitmapitem_bitmaploaded";

    public static final String BITMAPITEM_BITMAPLOADED_PATH = "bitmapitem_bitmaploaded_path";

    public static final int BITMAP_MAXSIZE = 512;

    private static final int sCacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024) / 8;

    private static AlbumArtCache sAlbumArtCache;

    /**
     * Cache album cover art.
     */
    private static class AlbumArtCache extends LruCache<String, Bitmap> {

        public AlbumArtCache() {
            super(sCacheSize);
        }

        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount() / 1024;
        }

        private void addAlbumArtToCache(String key, Bitmap bitmap) {
            if (getAlbumArtFromCache(key) == null) {
                put(key, bitmap);
            }
        }

        private Bitmap getAlbumArtFromCache(String key) {
            return get(key);
        }
    }

    static class AsyncDrawable extends BitmapDrawable {

        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    public static class AsyncBitmap {

        public Bitmap bitmap;

        private WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncBitmap(BitmapWorkerTask bitmapWorkerTask) {
            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }

        void setBitmapWorkerTaskReference(
                WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference) {
            this.bitmapWorkerTaskReference = bitmapWorkerTaskReference;
        }
    }

    public class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

        private Context context;

        private WeakReference<ImageView> imageViewReference = null;

        private AsyncBitmap bitMapToFill = null;

        private String path;

        private BitmapFactory.Options opts = new BitmapFactory.Options();

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
        }

        public BitmapWorkerTask(Context context, AsyncBitmap asyncBitmap) {
            this.context = context;
            bitMapToFill = asyncBitmap;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            path = params[0];
            if (sAlbumArtCache == null) {
                sAlbumArtCache = new AlbumArtCache();
            }
            Bitmap cachedBitMap = sAlbumArtCache.getAlbumArtFromCache(path);
            if (cachedBitMap != null) {
                return resizeBitmap(cachedBitMap);
            } else if (path.contains("http://")) {
                try {
                    URL url = new URL(path);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    return resizeBitmap(BitmapFactory.decodeStream(input, null, opts));
                } catch (IOException e) {
                    Log.e(TAG, "doInBackground: " + e.getClass() + ": " + e.getLocalizedMessage());
                    return null;
                }
            } else {
                return resizeBitmap(BitmapFactory.decodeFile(path, opts));
            }
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (sAlbumArtCache == null) {
                sAlbumArtCache = new AlbumArtCache();
            }
            sAlbumArtCache.addAlbumArtToCache(path, bitmap);

            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            } else if (bitMapToFill != null && bitmap != null) {
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(bitMapToFill);
                if (this == bitmapWorkerTask && bitMapToFill != null) {
                    bitMapToFill.bitmap = bitmap;
                    Intent intent = new Intent(BITMAPITEM_BITMAPLOADED);
                    intent.putExtra(BITMAPITEM_BITMAPLOADED_PATH, path);
                    context.sendBroadcast(intent);
                }
            }
        }

        private Bitmap resizeBitmap(Bitmap bitmap) {
            int scaledHeight, scaledWidth;
            if (bitmap.getHeight() > BITMAP_MAXSIZE || bitmap.getWidth() > BITMAP_MAXSIZE) {
                if (bitmap.getHeight() > bitmap.getWidth()) {
                    scaledWidth = bitmap.getWidth() * BITMAP_MAXSIZE / bitmap.getHeight();
                    scaledHeight = BITMAP_MAXSIZE;
                } else {
                    scaledWidth = BITMAP_MAXSIZE;
                    scaledHeight = bitmap.getHeight() * BITMAP_MAXSIZE / bitmap.getWidth();
                }
            } else {
                scaledHeight = BITMAP_MAXSIZE;
                scaledWidth = BITMAP_MAXSIZE;
            }
            return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false);
        }
    }

    /**
     * Checks if another running task is already associated with the {@link ImageView}
     */
    public static boolean cancelPotentialWork(String data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.path;
            if (bitmapData != data) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    /**
     * Checks if another running task is already associated with the {@link ImageView}
     */
    public static boolean cancelPotentialWork(String data, AsyncBitmap asyncBitmap) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(asyncBitmap);

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.path;
            if (bitmapData != data) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    /**
     * Used to get the {@link BitmapWorkerTask}, which is used to asynchronously load a {@link
     * Bitmap} into to {@link ImageView}
     */
    public static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    /**
     * Used to get the {@link BitmapWorkerTask}, which is used to asynchronously load a {@link
     * Bitmap} into to {@link ImageView}
     */
    public static BitmapWorkerTask getBitmapWorkerTask(AsyncBitmap asyncBitmap) {
        if (asyncBitmap != null) {
            return asyncBitmap.getBitmapWorkerTask();
        }
        return null;
    }

}
