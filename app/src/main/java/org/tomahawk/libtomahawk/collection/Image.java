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
package org.tomahawk.libtomahawk.collection;

import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.res.Resources;
import android.util.DisplayMetrics;

/**
 * Class which represents a Tomahawk {@link org.tomahawk.libtomahawk.collection.Image}.
 */
public class Image extends Cacheable {

    private static final float IMAGE_SIZE_SMALL = 0.2f;

    private static final float IMAGE_SIZE_LARGE = 0.5f;

    private static int sScreenHeightPixels = 0;

    private static int sScreenWidthPixels = 0;

    private final String mImagePath;

    private final boolean mIsHatchetImage;

    private int mWidth = -1;

    private int mHeight = -1;

    /**
     * Construct a new {@link org.tomahawk.libtomahawk.collection.Image}
     */
    private Image(String imagePath, boolean isHatchetImage) {
        super(Image.class, getCacheKey(imagePath));

        mImagePath = imagePath;
        mIsHatchetImage = isHatchetImage;
    }

    /**
     * Construct a new {@link org.tomahawk.libtomahawk.collection.Image}
     */
    private Image(String imagePath, boolean isHatchetImage, int width, int height) {
        super(Image.class, getCacheKey(imagePath));

        mImagePath = imagePath;
        mIsHatchetImage = isHatchetImage;
        mWidth = width;
        mHeight = height;
    }

    /**
     * Returns the {@link org.tomahawk.libtomahawk.collection.Image} with the given image path and
     * boolean to determine whether or not this image should be scaled down. If none exists in our
     * static {@link java.util.concurrent.ConcurrentHashMap} yet, construct and add it.
     */
    public static Image get(String imagePath, boolean scaleItDown) {
        Cacheable cacheable = get(Image.class, getCacheKey(imagePath));
        return cacheable != null ? (Image) cacheable : new Image(imagePath, scaleItDown);
    }

    /**
     * Returns the {@link org.tomahawk.libtomahawk.collection.Image} with the given image path and
     * boolean to determine whether or not this image should be scaled down. If none exists in our
     * static {@link java.util.concurrent.ConcurrentHashMap} yet, construct and add it.
     */
    public static Image get(String imagePath, boolean scaleItDown, int width, int height) {
        Cacheable cacheable = get(Image.class, getCacheKey(imagePath));
        return cacheable != null ? (Image) cacheable
                : new Image(imagePath, scaleItDown, width, height);
    }

    public static Image getByKey(String id) {
        return (Image) get(Image.class, id);
    }

    public String getImagePath() {
        return mImagePath;
    }

    public boolean isHatchetImage() {
        return mIsHatchetImage;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getWidth() {
        return mWidth;
    }

    public static int getSmallImageSize() {
        getScreenResolution();
        return (int) (sScreenHeightPixels * IMAGE_SIZE_SMALL);
    }

    public static int getLargeImageSize() {
        getScreenResolution();
        return (int) (sScreenHeightPixels * IMAGE_SIZE_LARGE);
    }

    private static void getScreenResolution() {
        if (sScreenWidthPixels == 0 || sScreenHeightPixels == 0) {
            Resources resources = TomahawkApp.getContext().getResources();
            DisplayMetrics metrics = resources.getDisplayMetrics();
            int width;
            int height;
            if (metrics.widthPixels > metrics.heightPixels) {
                width = metrics.widthPixels;
                height = metrics.heightPixels;
            } else {
                width = metrics.heightPixels;
                height = metrics.widthPixels;
            }
            sScreenWidthPixels = width;
            sScreenHeightPixels = height;
        }
    }
}
