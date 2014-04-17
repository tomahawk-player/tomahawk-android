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
package org.tomahawk.tomahawk_android.views;

import com.squareup.picasso.Transformation;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class CircularImageTransformation implements Transformation {

    @Override
    public Bitmap transform(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());

        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
        if (squaredBitmap != source) {
            source.recycle();
        }

        Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        BitmapShader shader = new BitmapShader(squaredBitmap, BitmapShader.TileMode.CLAMP,
                BitmapShader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setAntiAlias(true);

        float r = size / 2f;

        // Draw an underlying frame with a shadow
        Paint framePaint = new Paint();
        framePaint.setStrokeWidth(4);
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setColor(Color.WHITE);
        framePaint.setAntiAlias(true);
        framePaint.setShadowLayer(5, 0, 0, Color.BLACK);
        canvas.drawCircle(r, r, r - 6, framePaint);

        // Draw the actual round bitmap
        canvas.drawCircle(r, r, r - 5, paint);

        // Draw the white frame
        framePaint.clearShadowLayer();
        canvas.drawCircle(r, r, r - 6, framePaint);

        squaredBitmap.recycle();
        return bitmap;
    }

    @Override
    public String key() {
        return "CircularImageTransformation";
    }
}