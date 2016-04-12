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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ImageUtils {

    public static final String TAG = ImageUtils.class.getSimpleName();

    private static final int BLURRED_IMAGE_TRANSITION_TIME = 500;

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
        int placeHolder = isArtistImage ? R.drawable.artist_placeholder_grid
                : R.drawable.album_placeholder_grid;
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
        int placeHolder = isArtistImage ? R.drawable.artist_placeholder_grid
                : R.drawable.album_placeholder_grid;
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

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1,
                    Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
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
}
