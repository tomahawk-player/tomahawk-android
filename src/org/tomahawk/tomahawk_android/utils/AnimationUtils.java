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

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;

import org.tomahawk.tomahawk_android.R;

import android.view.View;

public class AnimationUtils {

    public static final int DURATION_CONTEXTMENU = 120;

    public static final int DURATION_PLAYBACKTOPPANEL = 200;

    public static final int DURATION_ARROWROTATE = 200;

    public static void fade(final View view, int duration, final boolean isFadeIn) {
        float from = isFadeIn ? 0f : 1f;
        float to = isFadeIn ? 1f : 0f;
        fade(view, from, to, duration, isFadeIn, new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (isFadeIn) {
                    view.setVisibility(View.VISIBLE);
                    animation.removeListener(this);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isFadeIn) {
                    view.setVisibility(View.GONE);
                    animation.removeListener(this);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }

    public static void fade(final View view, float from, float to, int duration,
            final boolean isFadeIn, Animator.AnimatorListener listener) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
            if (!(view.getTag(R.id.animation_type_fade) instanceof Boolean)
                    || (Boolean) view.getTag(R.id.animation_type_fade) != isFadeIn) {
                view.setTag(R.id.animation_type_fade, isFadeIn);
                ValueAnimator animator = ObjectAnimator.ofFloat(view, "alpha", from, to);
                animator.setDuration(duration);
                animator.addListener(listener);
                animator.start();
            }
        }
    }
}
