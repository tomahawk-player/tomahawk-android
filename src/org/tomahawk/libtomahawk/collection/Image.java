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

import org.tomahawk.libtomahawk.utils.TomahawkUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Class which represents a Tomahawk {@link org.tomahawk.libtomahawk.collection.Image}.
 */
public class Image {

    public static final int IMAGE_SIZE_SMALL = 128;

    public static final int IMAGE_SIZE_LARGE = 360;

    private static ConcurrentHashMap<String, Image> sImages
            = new ConcurrentHashMap<String, Image>();

    private String mImagePath;

    private boolean mIsHatchetImage;

    private int mWidth = -1;

    private int mHeight = -1;

    /**
     * Construct a new {@link org.tomahawk.libtomahawk.collection.Image}
     */
    private Image(String imagePath, boolean isHatchetImage) {
        mImagePath = imagePath;
        mIsHatchetImage = isHatchetImage;
    }

    /**
     * Construct a new {@link org.tomahawk.libtomahawk.collection.Image}
     */
    private Image(String imagePath, boolean isHatchetImage, int width, int height) {
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
        Image image = new Image(imagePath, scaleItDown);
        String key = TomahawkUtils.getCacheKey(image);
        if (!sImages.containsKey(key)) {
            sImages.put(key, image);
        }
        return sImages.get(key);
    }

    /**
     * Returns the {@link org.tomahawk.libtomahawk.collection.Image} with the given image path and
     * boolean to determine whether or not this image should be scaled down. If none exists in our
     * static {@link java.util.concurrent.ConcurrentHashMap} yet, construct and add it.
     */
    public static Image get(String imagePath, boolean scaleItDown, int width, int height) {
        Image image = new Image(imagePath, scaleItDown, width, height);
        String key = TomahawkUtils.getCacheKey(image);
        if (!sImages.containsKey(key)) {
            sImages.put(key, image);
        }
        return sImages.get(key);
    }

    /**
     * Get the {@link org.tomahawk.libtomahawk.collection.Image} by providing its cache key
     */
    public static Image getImageByKey(String key) {
        return sImages.get(key);
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
}
