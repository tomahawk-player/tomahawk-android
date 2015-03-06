/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * A {@link android.widget.LinearLayout} which will always show as a scare {@link
 * android.view.View}, because its width is also used as its height.
 */
public class SquareWidthFrameLayout extends FrameLayout {

    /**
     * Constructs a new {@link org.tomahawk.tomahawk_android.ui.widgets.SquareWidthFrameLayout}
     */
    public SquareWidthFrameLayout(Context context) {
        super(context);
    }

    /**
     * Constructs a new {@link org.tomahawk.tomahawk_android.ui.widgets.SquareWidthFrameLayout}
     */
    public SquareWidthFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
