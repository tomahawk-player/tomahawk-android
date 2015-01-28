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
package org.tomahawk.tomahawk_android.views;

import android.content.Context;
import android.support.v7.internal.widget.TintCheckBox;
import android.support.v7.internal.widget.TintManager;
import android.support.v7.internal.widget.TintTypedArray;
import android.util.AttributeSet;

public class TomahawkTintCheckBox extends TintCheckBox {

    private static final int[] TINT_ATTRS = {
            android.R.attr.button
    };

    private final TintManager mTintManager;

    public TomahawkTintCheckBox(Context context) {
        this(context, null);
    }

    public TomahawkTintCheckBox(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.checkboxStyle);
    }

    public TomahawkTintCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs, TINT_ATTRS,
                defStyleAttr, 0);
        setButtonDrawable(a.getDrawable(0));
        a.recycle();

        mTintManager = a.getTintManager();
    }

    @Override
    public void setButtonDrawable(int resid) {
        setButtonDrawable(mTintManager.getDrawable(resid));
    }
}
