package org.tomahawk.libtomahawk.utils;

import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class ViewUtils {

    public static final String TAG = ViewUtils.class.getSimpleName();

    public abstract static class ViewRunnable implements Runnable {

        private final View mView;

        public ViewRunnable(View view) {
            this.mView = view;
        }

        public View getLayedOutView() {
            return mView;
        }
    }

    public static View ensureInflation(View view, int stubResId, int inflatedId) {
        View stub = view.findViewById(stubResId);
        if (stub instanceof ViewStub) {
            return ((ViewStub) stub).inflate();
        } else {
            return view.findViewById(inflatedId);
        }
    }

    /**
     * This method converts dp unit to equivalent device specific value in pixels.
     *
     * @param dp A value in dp(Device independent pixels) unit. Which we need to convert into
     *           pixels
     * @return A float value to represent Pixels equivalent to dp according to device
     */
    public static int convertDpToPixel(int dp) {
        Resources resources = TomahawkApp.getContext().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (dp * (metrics.densityDpi / 160f));
    }

    /**
     * Converts a track duration int into the proper String format
     *
     * @param duration the track's duration
     * @return the formated string
     */
    public static String durationToString(long duration) {
        return String.format("%02d", (duration / 60000)) + ":" + String
                .format("%02.0f", (double) (duration / 1000) % 60);
    }

    public static void afterViewGlobalLayout(final ViewRunnable viewRunnable) {
        if (viewRunnable.getLayedOutView().getHeight() > 0
                && viewRunnable.getLayedOutView().getWidth() > 0) {
            viewRunnable.run();
        } else {
            viewRunnable.getLayedOutView().getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            viewRunnable.run();

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                viewRunnable.getLayedOutView().getViewTreeObserver()
                                        .removeOnGlobalLayoutListener(this);
                            } else {
                                //noinspection deprecation
                                viewRunnable.getLayedOutView().getViewTreeObserver()
                                        .removeGlobalOnLayoutListener(this);
                            }
                        }
                    });
        }
    }

    public static void showSoftKeyboard(final EditText editText) {
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, final boolean hasFocus) {
                editText.post(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager imm = (InputMethodManager) TomahawkApp.getContext()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
                editText.setOnFocusChangeListener(null);
            }
        });
        editText.requestFocus();
    }
}
