package org.tomahawk.tomahawk_android.views;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.widget.ImageView;

public class TopCropImageView extends ImageView {

    public TopCropImageView(Context context) {
        super(context);
        setup();
    }

    public TopCropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public TopCropImageView(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        setup();
    }

    private void setup() {
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected boolean setFrame(int frameLeft, int frameTop, int frameRight, int frameBottom) {
        if (getDrawable() != null) {
            float frameWidth = frameRight - frameLeft;
            float frameHeight = frameBottom - frameTop;

            float originalImageWidth = (float) getDrawable().getIntrinsicWidth();
            float originalImageHeight = (float) getDrawable().getIntrinsicHeight();

            float usedScaleFactor = 1;

            if ((frameWidth > originalImageWidth) || (frameHeight > originalImageHeight)) {
                // If frame is bigger than image
                // => Crop it, keep aspect ratio and position it at the bottom and center horizontally

                float fitHorizontallyScaleFactor = frameWidth / originalImageWidth;
                float fitVerticallyScaleFactor = frameHeight / originalImageHeight;

                usedScaleFactor = Math.max(fitHorizontallyScaleFactor, fitVerticallyScaleFactor);
            }

            Matrix matrix = getImageMatrix();
            matrix.setScale(usedScaleFactor, usedScaleFactor, 0,
                    0); // Replaces the old matrix completly
            setImageMatrix(matrix);
        }
        return super.setFrame(frameLeft, frameTop, frameRight, frameBottom);
    }

}