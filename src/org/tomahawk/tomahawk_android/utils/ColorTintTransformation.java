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
package org.tomahawk.tomahawk_android.utils;

import com.squareup.picasso.Transformation;

import org.tomahawk.tomahawk_android.TomahawkApp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import static android.graphics.Bitmap.createBitmap;
import static android.graphics.Paint.ANTI_ALIAS_FLAG;

public class ColorTintTransformation implements Transformation {

    private int mColorResId;

    public ColorTintTransformation(int colorResId) {
        mColorResId = colorResId;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        Bitmap result = createBitmap(source.getWidth(), source.getHeight(), source.getConfig());

        ColorFilter filter = getColorFilter(mColorResId);

        Paint paint = new Paint(ANTI_ALIAS_FLAG);
        paint.setColorFilter(filter);

        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(source, 0, 0, paint);

        source.recycle();

        return result;
    }

    @Override
    public String key() {
        return "ColorTintTransformation";
    }

    public static ColorFilter getColorFilter(int colorResId) {
        return new PorterDuffColorFilter(
                TomahawkApp.getContext().getResources().getColor(colorResId),
                PorterDuff.Mode.MULTIPLY);
    }
}