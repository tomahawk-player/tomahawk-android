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
package org.tomahawk.libtomahawk.collection;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Every {@link org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}, which
 * needs async {@link Bitmap} loading functionality (like e.g. AlbumArt) extends from this class
 */
public class BitmapItem {

    private static final String TAG = BitmapItem.class.getName();

    public static final String BITMAPITEM_BITMAPLOADED
            = "org.tomahawk.tomahawk_android.bitmapitem_bitmaploaded";

    public static final String BITMAPITEM_BITMAPLOADED_PATH
            = "org.tomahawk.tomahawk_android.bitmapitem_bitmaploaded_path";

    private static final int BITMAP_MAXSIZE = 512;

    private static final int sCacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024) / 8;

    private static AlbumArtCache sAlbumArtCache;

    /**
     * Cache {@link Album} cover art.
     */
    private static class AlbumArtCache extends LruCache<String, Bitmap> {

        /**
         * Construct this {@link AlbumArtCache} with sCacheSize as the cache's size
         */
        public AlbumArtCache() {
            super(sCacheSize);
        }

        /**
         * Returns the size of the entry for key and value in user-defined units.
         */
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
                return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
            }
            return bitmap.getByteCount() / 1024;
        }

        /**
         * Add the given {@link Bitmap} with the given key to this {@link AlbumArtCache}
         */
        private void addAlbumArtToCache(String key, Bitmap bitmap) {
            if (getAlbumArtFromCache(key) == null) {
                put(key, bitmap);
            }
        }

        /**
         * Return a cached {@link Bitmap} by providing its key
         */
        private Bitmap getAlbumArtFromCache(String key) {
            return get(key);
        }
    }

    /**
     * An {@link AsyncDrawable}, which will show a default placeholder {@link Bitmap} until the
     * content has been loaded.
     */
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

    /**
     * An {@link AsyncBitmap}, which will contain a default placeholder {@link Bitmap} until the
     * content has been loaded.
     */
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

    /**
     * This {@link AsyncTask} contains all the code, which will asynchronously fetch a {@link
     * Bitmap}.
     */
    public class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

        //The Context is needed to send broadcasts
        private Context context;

        //A WeakReference to the ImageView which this AsyncTask works on
        private WeakReference<ImageView> imageViewReference = null;

        //The placeHolder bitmap to show up until the actual image has been loaded
        private Bitmap placeHolderBitmap;

        private AsyncBitmap bitMapToFill = null;

        //The path to the image which should be loaded
        private String path;

        private BitmapFactory.Options opts = new BitmapFactory.Options();

        /**
         * Construct a new {@link BitmapWorkerTask}
         *
         * @param imageView         the {@link ImageView} to be asynchronously filled
         * @param placeHolderBitmap the placeHolderBitmap which should be shown before the actual
         *                          image has been loaded
         */
        public BitmapWorkerTask(ImageView imageView, Bitmap placeHolderBitmap) {
            this.placeHolderBitmap = placeHolderBitmap;
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
        }

        /**
         * Construct a new {@link BitmapWorkerTask}
         *
         * @param context           The {@link Context}, which is needed to be able to send
         *                          broadcasts
         * @param asyncBitmap       the {@link AsyncBitmap} to be asynchronously filled
         * @param placeHolderBitmap the placeHolderBitmap which should be shown before the actual
         *                          image has been loaded
         */
        public BitmapWorkerTask(Context context, AsyncBitmap asyncBitmap,
                Bitmap placeHolderBitmap) {
            this.context = context;
            bitMapToFill = asyncBitmap;
            this.placeHolderBitmap = placeHolderBitmap;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
        }

        /**
         * Decodes image in background
         *
         * @param params params should contain the path at params[0] to the image which should be
         *               loaded
         * @return The decoded {@link Bitmap}
         */
        @Override
        protected Bitmap doInBackground(String... params) {
            //Get the path to the image which should be loaded
            path = params[0];
            if (sAlbumArtCache == null) {
                //If we don't yet have a working AlbumArtCache, construct a new one
                sAlbumArtCache = new AlbumArtCache();
            }
            // Try to get the image from our cache
            Bitmap cachedBitMap = sAlbumArtCache.getAlbumArtFromCache(path);
            if (cachedBitMap != null) {
                // we found our image in the cache, so we can use it right away
                return resizeBitmap(cachedBitMap);
            } else if (path.contains("http://") || path.contains("https://")) {
                //We need to resolve our bitmap with a HTTP request
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
                //We simply decode the bitmap from a local file
                return resizeBitmap(BitmapFactory.decodeFile(path, opts));
            }
        }

        /**
         * Once complete, see if {@link ImageView} is still around and set {@link Bitmap}.
         *
         * @param bitmap {@link Bitmap} containing the decoded/loaded {@link Bitmap}
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                //If we've cancelled our loading process in the meantime
                bitmap = null;
            }

            if (sAlbumArtCache == null) {
                //If we don't yet have a working AlbumArtCache, construct a new one
                sAlbumArtCache = new AlbumArtCache();
            }
            if (path != null && bitmap != null) {
                //Add the loaded/decoded bitmap to our AlbumArtCache
                sAlbumArtCache.addAlbumArtToCache(path, bitmap);
            }

            if (imageViewReference != null) {
                //Because the imageViewReference is set, we need to fill this one
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    } else {
                        imageView.setImageBitmap(placeHolderBitmap);
                    }
                }
            } else if (bitMapToFill != null) {
                //Because the bitMapToFill is set, we need to fill this one
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(bitMapToFill);
                if (this == bitmapWorkerTask && bitMapToFill != null) {
                    if (bitmap != null) {
                        bitMapToFill.bitmap = bitmap;
                    } else {
                        bitMapToFill.bitmap = placeHolderBitmap;
                    }
                }
                //After we have finished we broadcast the bitmap's path, so anybody listening can
                //associate the loaded bitmap to the broadcast
                Intent intent = new Intent(BITMAPITEM_BITMAPLOADED);
                intent.putExtra(BITMAPITEM_BITMAPLOADED_PATH, path);
                context.sendBroadcast(intent);
            }
        }

        /**
         * Resize the given {@link Bitmap}, so that its ratio is conserved, but neither its width
         * nor its height is greate than BITMAP_MAXSIZE
         *
         * @param bitmap the {@link Bitmap} to resize
         * @return the resized {@link Bitmap}
         */
        private Bitmap resizeBitmap(Bitmap bitmap) {
            int scaledHeight, scaledWidth;
            if (bitmap == null) {
                return null;
            }
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
     * Checks if another running {@link BitmapWorkerTask} is already associated with the {@link
     * ImageView}
     *
     * @param path             the path to the image to be loaded
     * @param bitmapWorkerTask the {@link BitmapWorkerTask} to be cancelled
     */
    public static boolean cancelPotentialWork(String path, BitmapWorkerTask bitmapWorkerTask) {
        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.path;
            if (bitmapData != null && bitmapData.equals(path)) {
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
     * Used to get the {@link BitmapWorkerTask}, which is used to asynchronously fill a {@link
     * Bitmap}
     */
    public static BitmapWorkerTask getBitmapWorkerTask(AsyncBitmap asyncBitmap) {
        if (asyncBitmap != null) {
            return asyncBitmap.getBitmapWorkerTask();
        }
        return null;
    }

}
