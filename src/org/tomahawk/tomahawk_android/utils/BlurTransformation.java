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
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;

public class BlurTransformation implements Transformation {

    private static RenderScript mRenderScript = RenderScript.create(TomahawkApp.getContext());

    @Override
    public Bitmap transform(Bitmap source) {
        return staticTransform(source, 16f);
    }

    @Override
    public String key() {
        return "BlurTransformation";
    }

    public static Bitmap staticTransform(Bitmap source, float radius) {
        final Allocation input = Allocation.createFromBitmap(mRenderScript, source);
        // Use this constructor for best performance, because it uses USAGE_SHARED mode which reuses
        // memory
        final Allocation output = Allocation.createTyped(mRenderScript, input.getType());
        final ScriptIntrinsicBlur script =
                ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript));
        script.setRadius(radius);
        script.setInput(input);
        script.forEach(output);
        output.copyTo(source);
        return source;
    }
}