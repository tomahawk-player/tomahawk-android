package org.tomahawk.tomahawk_android.ui.widgets;

import org.tomahawk.tomahawk_android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class BoundedLinearLayout extends LinearLayout {

    private int mMaxWidth;

    private int mMaxHeight;

    public BoundedLinearLayout(Context context) {
        super(context);
        mMaxWidth = Integer.MAX_VALUE;
        mMaxHeight = Integer.MAX_VALUE;
    }

    public BoundedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BoundedLinearLayout);
        mMaxWidth = a.getDimensionPixelSize(R.styleable.BoundedLinearLayout_max_width,
                Integer.MAX_VALUE);
        mMaxHeight = a.getDimensionPixelSize(R.styleable.BoundedLinearLayout_max_height,
                Integer.MAX_VALUE);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Adjust width as necessary
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (mMaxWidth > 0 && mMaxWidth < measuredWidth) {
            int measureMode = MeasureSpec.getMode(widthMeasureSpec);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth, measureMode);
        }
        // Adjust height as necessary
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (mMaxHeight > 0 && mMaxHeight < measuredHeight) {
            int measureMode = MeasureSpec.getMode(heightMeasureSpec);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, measureMode);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public int getMaxWidth() {
        return mMaxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        mMaxWidth = maxWidth;
        requestLayout();
        invalidate();
    }

    public int getMaxHeight() {
        return mMaxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        mMaxHeight = maxHeight;
        requestLayout();
        invalidate();
    }
}