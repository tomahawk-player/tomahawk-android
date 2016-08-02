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
package org.tomahawk.libtomahawk.utils;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.BlurTransformation;
import org.tomahawk.tomahawk_android.utils.ColorTintTransformation;
import org.tomahawk.tomahawk_android.utils.CropCircleTransformation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ImageUtils {

    public static final String TAG = ImageUtils.class.getSimpleName();

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context   the context needed for fetching resources
     * @param imageView the {@link ImageView}, which will be used to show the {@link
     *                  android.graphics.Bitmap}
     * @param image     the path to load the image from
     * @param width     the width in density independent pixels to scale the image down to
     */
    public static void loadImageIntoImageView(Context context, ImageView imageView, Image image,
            int width, boolean isArtistImage) {
        loadImageIntoImageView(context, imageView, image, width, true, isArtistImage);
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context   the context needed for fetching resources
     * @param imageView the {@link ImageView}, which will be used to show the {@link
     *                  android.graphics.Bitmap}
     * @param image     the path to load the image from
     * @param width     the width in density independent pixels to scale the image down to
     */
    public static void loadBlurredImageIntoImageView(Context context, ImageView imageView,
            Image image, int width, int placeHolderResId) {
        loadBlurredImageIntoImageView(context, imageView, image, width, placeHolderResId, null);
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context   the context needed for fetching resources
     * @param imageView the {@link ImageView}, which will be used to show the {@link
     *                  android.graphics.Bitmap}
     * @param image     the path to load the image from
     * @param width     the width in density independent pixels to scale the image down to
     */
    public static void loadBlurredImageIntoImageView(Context context, ImageView imageView,
            Image image, int width, int placeHolderResId, Callback callback) {
        RequestCreator creator;
        if (image != null && !TextUtils.isEmpty(image.getImagePath())) {
            String imagePath = buildImagePath(image, width);
            creator = Picasso.with(context)
                    .load(ImageUtils.preparePathForPicasso(imagePath))
                    .resize(width, width)
                    .transform(new BlurTransformation(context, 16));
        } else {
            creator = Picasso.with(context).load(placeHolderResId);
        }
        if (placeHolderResId > 0) {
            creator.error(placeHolderResId);
        }
        if (callback != null) {
            creator.noFade();
        }
        creator.into(imageView, callback);
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context   the context needed for fetching resources
     * @param imageView the {@link ImageView}, which will be used to show the {@link
     *                  android.graphics.Bitmap}
     * @param image     the path to load the image from
     * @param width     the width in pixels to scale the image down to
     */
    public static void loadImageIntoImageView(Context context, ImageView imageView, Image image,
            int width, boolean fit, boolean isArtistImage) {
        int placeHolder = isArtistImage ? R.drawable.artist_placeholder
                : R.drawable.album_placeholder;
        if (image != null && !TextUtils.isEmpty(image.getImagePath())) {
            String imagePath = buildImagePath(image, width);
            RequestCreator creator = Picasso.with(context).load(
                    ImageUtils.preparePathForPicasso(imagePath))
                    .placeholder(placeHolder)
                    .error(placeHolder);
            if (fit) {
                creator.resize(width, width);
            }
            creator.into(imageView);
        } else {
            RequestCreator creator = Picasso.with(context).load(placeHolder)
                    .placeholder(placeHolder)
                    .error(placeHolder);
            if (fit) {
                creator.resize(width, width);
            }
            creator.into(imageView);
        }
    }

    /**
     * Load a circle-shaped {@link android.graphics.Bitmap} asynchronously
     *
     * @param context   the context needed for fetching resources
     * @param imageView the {@link ImageView}, which will be used to show the {@link
     *                  android.graphics.Bitmap}
     * @param user      the user of which to load the data into the views
     * @param width     the width in pixels to scale the image down to
     * @param textView  the textview which is being used to display the first letter of the user's
     *                  name in the placeholder image
     */
    public static void loadUserImageIntoImageView(Context context, ImageView imageView,
            User user, int width, TextView textView) {
        int placeHolder = R.drawable.circle_black;
        if (user.getImage() != null && !TextUtils.isEmpty(user.getImage().getImagePath())) {
            textView.setVisibility(View.GONE);
            String imagePath = buildImagePath(user.getImage(), width);
            Picasso.with(context).load(ImageUtils.preparePathForPicasso(imagePath))
                    .transform(new CropCircleTransformation())
                    .placeholder(placeHolder)
                    .error(placeHolder)
                    .fit()
                    .into(imageView);
        } else {
            textView.setVisibility(View.VISIBLE);
            textView.setText(user.getName().substring(0, 1).toUpperCase());
            Picasso.with(context).load(placeHolder)
                    .placeholder(placeHolder)
                    .error(placeHolder)
                    .fit()
                    .into(imageView);
        }
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context   the context needed for fetching resources
     * @param imageView the {@link ImageView}, which will be used to show the {@link
     *                  android.graphics.Bitmap}
     * @param path      the path to the image
     */
    public static void loadDrawableIntoImageView(Context context, ImageView imageView,
            String path) {
        loadDrawableIntoImageView(context, imageView, path, 0);
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context    the context needed for fetching resources
     * @param imageView  the {@link ImageView}, which will be used to show the {@link
     *                   android.graphics.Bitmap}
     * @param path       the path to the image
     * @param colorResId the color with which to tint the imageview drawable
     */
    public static void loadDrawableIntoImageView(Context context, ImageView imageView,
            String path, int colorResId) {
        RequestCreator creator = Picasso.with(context).load(path);
        if (colorResId > 0) {
            creator.transform(new ColorTintTransformation(colorResId));
        }
        creator.error(R.drawable.ic_alert_error).into(imageView);
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context    the context needed for fetching resources
     * @param view       the {@link View}, which will be used to show the {@link
     *                   android.graphics.Bitmap} in its background
     * @param path       the path to the image
     * @param colorResId the color with which to tint the imageview drawable
     */
    public static void loadDrawableIntoView(final Context context, final View view,
            final String path, int colorResId) {
        RequestCreator creator = Picasso.with(context).load(path);
        if (colorResId > 0) {
            creator.transform(new ColorTintTransformation(colorResId));
        }
        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                view.setBackgroundDrawable(new BitmapDrawable(context.getResources(), bitmap));
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Log.d(TAG, "loadDrawableIntoView onBitmapFailed for path: " + path);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        };
        view.setTag(target);
        creator.error(R.drawable.ic_alert_error).into(target);
    }

    /**
     * Load a {@link Drawable} asynchronously (convenience method)
     *
     * @param context       the context needed for fetching resources
     * @param imageView     the {@link ImageView}, which will be used to show the {@link Drawable}
     * @param drawableResId the resource id of the drawable to load into the imageview
     */
    public static void loadDrawableIntoImageView(Context context, ImageView imageView,
            int drawableResId) {
        loadDrawableIntoImageView(context, imageView, drawableResId, 0);
    }

    /**
     * Load a {@link Drawable} asynchronously
     *
     * @param context       the context needed for fetching resources
     * @param imageView     the {@link ImageView}, which will be used to show the {@link Drawable}
     * @param drawableResId the resource id of the drawable to load into the imageview
     * @param colorResId    the color with which to tint the imageview drawable
     */
    public static void loadDrawableIntoImageView(Context context, ImageView imageView,
            int drawableResId, int colorResId) {
        RequestCreator creator = Picasso.with(context).load(drawableResId);
        if (colorResId > 0) {
            creator.transform(new ColorTintTransformation(colorResId));
        }
        creator.error(R.drawable.ic_alert_error).into(imageView);
    }

    /**
     * Load a {@link android.graphics.Bitmap} asynchronously
     *
     * @param context the context needed for fetching resources
     * @param image   the path to load the image from
     * @param target  the Target which the loaded image will be pushed to
     * @param width   the width in pixels to scale the image down to
     */
    public static void loadImageIntoBitmap(Context context, Image image, Target target, int width,
            boolean isArtistImage) {
        int placeHolder = isArtistImage ? R.drawable.artist_placeholder
                : R.drawable.album_placeholder;
        if (image != null && !TextUtils.isEmpty(image.getImagePath())) {
            String imagePath = buildImagePath(image, width);
            Picasso.with(context).load(ImageUtils.preparePathForPicasso(imagePath))
                    .resize(width, width)
                    .into(target);
        } else {
            Picasso.with(context).load(placeHolder)
                    .resize(width, width)
                    .into(target);
        }
    }

    public static String preparePathForPicasso(String path) {
        if (TextUtils.isEmpty(path) || path.contains("https://") || path.contains("http://")) {
            return path;
        }
        return path.startsWith("file:") ? path : "file:" + path;
    }

    private static String buildImagePath(Image image, int width) {
        if (image.isHatchetImage()) {
            int imageSize = Math.min(image.getHeight(), image.getWidth());
            int actualWidth;
            if (NetworkUtils.isWifiAvailable()) {
                actualWidth = Math.min(imageSize, width);
            } else {
                actualWidth = Math.min(imageSize, width * 2 / 3);
            }
            return image.getImagePath() + "?width=" + actualWidth + "&height=" + actualWidth;
        }
        return image.getImagePath();
    }

    @SuppressLint("NewApi")
    public static void setTint(final Drawable drawable, final int colorResId) {
        int color = TomahawkApp.getContext().getResources().getColor(colorResId);
        drawable.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_ATOP);
    }

    @SuppressLint("NewApi")
    public static void clearTint(final Drawable drawable) {
        drawable.clearColorFilter();
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
