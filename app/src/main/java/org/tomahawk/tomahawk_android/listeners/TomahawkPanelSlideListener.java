/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.listeners;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.utils.AnimationUtils;
import org.tomahawk.tomahawk_android.utils.PreferenceUtils;
import org.tomahawk.tomahawk_android.views.PlaybackPanel;

import android.animation.Animator;
import android.view.View;

import de.greenrobot.event.EventBus;

public class TomahawkPanelSlideListener implements SlidingUpPanelLayout.PanelSlideListener {

    private TomahawkMainActivity mActivity;

    private float mSlidingOffset = -1f;

    private SlidingUpPanelLayout.PanelState mLastSlidingState;

    private SlidingUpPanelLayout mSlidingUpPanelLayout;

    private View mTopPanel;

    private PlaybackPanel mPlaybackPanel;

    public static class SlidingLayoutChangedEvent {

        public SlidingUpPanelLayout.PanelState mSlideState;

    }

    public TomahawkPanelSlideListener(TomahawkMainActivity activity, SlidingUpPanelLayout layout,
            PlaybackPanel playbackPanel) {
        mActivity = activity;
        mSlidingUpPanelLayout = layout;
        mPlaybackPanel = playbackPanel;
    }

    @Override
    public void onPanelSlide(View view, float v) {
        mSlidingOffset = v;
        mActivity.updateActionBarState(false);
        if (mTopPanel == null) {
            mTopPanel = mSlidingUpPanelLayout.findViewById(R.id.top_buttonpanel);
        }
        if (mTopPanel != null) {
            if (v > 0.15f) {
                AnimationUtils
                        .fade(mTopPanel, 0f, 1f, AnimationUtils.DURATION_PLAYBACKTOPPANEL, true,
                                new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animation) {
                                        mTopPanel.findViewById(R.id.imageButton_repeat)
                                                .setVisibility(View.VISIBLE);
                                        mTopPanel.findViewById(R.id.close_button)
                                                .setVisibility(View.VISIBLE);
                                        mTopPanel.findViewById(R.id.imageButton_shuffle)
                                                .setVisibility(View.VISIBLE);
                                        animation.removeListener(this);
                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animation) {
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animation) {
                                    }
                                });
            } else if (v < 0.15f) {
                AnimationUtils
                        .fade(mTopPanel, 1f, 0f, AnimationUtils.DURATION_PLAYBACKTOPPANEL, false,
                                new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animation) {
                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        mTopPanel.findViewById(R.id.imageButton_repeat)
                                                .setVisibility(View.GONE);
                                        mTopPanel.findViewById(R.id.close_button)
                                                .setVisibility(View.GONE);
                                        mTopPanel.findViewById(R.id.imageButton_shuffle)
                                                .setVisibility(View.GONE);
                                        animation.removeListener(this);
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animation) {
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animation) {
                                    }
                                });
            }
        }
        int position = Math.min(10000, Math.max(0, (int) ((v - 0.8f) * 10000f / (1f - 0.8f))));
        mPlaybackPanel.animate(position);
        sendSlidingLayoutChangedEvent();
    }

    @Override
    public void onPanelCollapsed(View view) {
        sendSlidingLayoutChangedEvent();
    }

    @Override
    public void onPanelExpanded(View view) {
        PreferenceUtils.edit().putBoolean(
                PreferenceUtils.COACHMARK_PLAYBACKFRAGMENT_NAVIGATION_DISABLED, true)
                .apply();
        sendSlidingLayoutChangedEvent();
    }

    @Override
    public void onPanelAnchored(View view) {
        sendSlidingLayoutChangedEvent();
    }

    @Override
    public void onPanelHidden(View view) {
        sendSlidingLayoutChangedEvent();
    }

    public float getSlidingOffset() {
        return mSlidingOffset;
    }

    private void sendSlidingLayoutChangedEvent() {
        if (mSlidingUpPanelLayout.getPanelState() != mLastSlidingState) {
            mLastSlidingState = mSlidingUpPanelLayout.getPanelState();

            SlidingLayoutChangedEvent event = new SlidingLayoutChangedEvent();
            event.mSlideState = mSlidingUpPanelLayout.getPanelState();
            EventBus.getDefault().post(event);
        }
    }
}
