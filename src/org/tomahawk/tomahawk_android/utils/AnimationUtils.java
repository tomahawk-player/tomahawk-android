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

import org.tomahawk.tomahawk_android.R;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public class AnimationUtils {

    public static void fade(final View view, float from, float to, int duration,
            final boolean isFadeIn, Animation.AnimationListener listener) {
        if (view != null && !(view.getTag(R.id.animation_type_fade) instanceof Boolean)
                || (!(Boolean) view.getTag(R.id.animation_type_fade) && isFadeIn
                || (Boolean) view.getTag(R.id.animation_type_fade) && !isFadeIn)) {
            view.setTag(R.id.animation_type_fade, isFadeIn);
            AlphaAnimation animation = new AlphaAnimation(from, to);
            animation.setDuration(duration);
            view.startAnimation(animation);
            animation.setFillAfter(true);
            animation.setAnimationListener(listener);
        }
    }
}
