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

import com.pascalwelsch.holocircularprogressbar.HoloCircularProgressBar;

import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.AnimationUtils;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.TransitionDrawable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

public class PlaybackPanel extends FrameLayout {

    private PlaybackService mPlaybackService;

    public static final String COMPLETION_STRING_DEFAULT = "-:--";

    private FrameLayout mTextViewContainer;

    private View mPanelContainer;

    private FrameLayout mArtistNameButton;

    private TextView mArtistTextView;

    private TextView mTrackTextView;

    private TextView mCompletionTimeTextView;

    private TextView mCurrentTimeTextView;

    private TextView mSeekTimeTextView;

    private ImageView mResolverImageView;

    private ImageView mPlayButton;

    private ImageView mPauseButton;

    private ProgressBar mProgressBar;

    private View mProgressBarThumb;

    private float mLastThumbPosition = -1f;

    private boolean mAbortSeeking;

    private FrameLayout mCircularProgressBarContainer;

    private HoloCircularProgressBar mCircularProgressBar;

    private final Set<ValueAnimator> mAnimators = new HashSet<>();

    private int mLastPlayTime = 0;

    private boolean mInitialized = false;

    public PlaybackPanel(Context context) {
        super(context);
        inflate(getContext(), R.layout.playback_panel, this);
    }

    public PlaybackPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(getContext(), R.layout.playback_panel, this);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return false;
    }

    public void setup(final boolean isPanelExpanded) {
        mInitialized = true;

        mTextViewContainer = (FrameLayout) findViewById(R.id.textview_container);
        mPanelContainer = findViewById(R.id.panel_container);
        mArtistNameButton = (FrameLayout) mTextViewContainer.findViewById(R.id.artist_name_button);
        mArtistTextView = (TextView) mArtistNameButton.findViewById(R.id.artist_textview);
        mTrackTextView = (TextView) mTextViewContainer.findViewById(R.id.track_textview);
        mCompletionTimeTextView = (TextView) findViewById(R.id.completiontime_textview);
        mCurrentTimeTextView = (TextView) findViewById(R.id.currenttime_textview);
        mSeekTimeTextView = (TextView) findViewById(R.id.seektime_textview);
        mResolverImageView = (ImageView) findViewById(R.id.resolver_imageview);
        mPlayButton = (ImageView) findViewById(R.id.play_button);
        mPauseButton = (ImageView) findViewById(R.id.pause_button);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        mProgressBarThumb = findViewById(R.id.progressbar_thumb);
        mCircularProgressBarContainer =
                (FrameLayout) findViewById(R.id.circularprogressbar_container);
        mCircularProgressBar = (HoloCircularProgressBar)
                mCircularProgressBarContainer.findViewById(R.id.circularprogressbar);

        mCircularProgressBar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlaybackService != null) {
                    mPlaybackService.playPause(true);
                }
            }
        });

        mCircularProgressBar.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                        .putBoolean(TomahawkMainActivity.COACHMARK_SEEK_DISABLED, true)
                        .apply();
                View coachMark = TomahawkUtils.ensureInflation(PlaybackPanel.this,
                        R.id.playbackpanel_seek_coachmark_stub, R.id.playbackpanel_seek_coachmark);
                coachMark.setVisibility(GONE);
                if (!isPanelExpanded || getResources().getBoolean(R.bool.is_landscape)) {
                    AnimationUtils.fade(mTextViewContainer,
                            AnimationUtils.DURATION_PLAYBACKSEEKMODE, false, true);
                }
                AnimationUtils.fade(mCircularProgressBarContainer,
                        AnimationUtils.DURATION_PLAYBACKSEEKMODE, false, true);
                AnimationUtils.fade(mResolverImageView,
                        AnimationUtils.DURATION_PLAYBACKSEEKMODE, false, true);
                AnimationUtils.fade(mCompletionTimeTextView,
                        AnimationUtils.DURATION_PLAYBACKSEEKMODE, false, true);
                AnimationUtils.fade(mProgressBarThumb,
                        AnimationUtils.DURATION_PLAYBACKSEEKMODE, true, true);
                AnimationUtils.fade(mCurrentTimeTextView,
                        AnimationUtils.DURATION_PLAYBACKSEEKMODE, true, true);
                AnimationUtils.fade(mSeekTimeTextView,
                        AnimationUtils.DURATION_PLAYBACKSEEKMODE, true, true);
                AnimationUtils.fade(mProgressBar,
                        AnimationUtils.DURATION_PLAYBACKSEEKMODE, true, true);

                mCircularProgressBar.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            if (!isPanelExpanded || getResources()
                                    .getBoolean(R.bool.is_landscape)) {
                                AnimationUtils.fade(mTextViewContainer,
                                        AnimationUtils.DURATION_PLAYBACKSEEKMODE, true, true);
                            }
                            AnimationUtils.fade(mCircularProgressBarContainer,
                                    AnimationUtils.DURATION_PLAYBACKSEEKMODE, true, true);
                            AnimationUtils.fade(mResolverImageView,
                                    AnimationUtils.DURATION_PLAYBACKSEEKMODE, true, true);
                            AnimationUtils.fade(mCompletionTimeTextView,
                                    AnimationUtils.DURATION_PLAYBACKSEEKMODE, true, true);
                            AnimationUtils.fade(mProgressBarThumb,
                                    AnimationUtils.DURATION_PLAYBACKSEEKMODE, false, true);
                            AnimationUtils.fade(mCurrentTimeTextView,
                                    AnimationUtils.DURATION_PLAYBACKSEEKMODE, false, true);
                            AnimationUtils.fade(mSeekTimeTextView,
                                    AnimationUtils.DURATION_PLAYBACKSEEKMODE, false, true);
                            AnimationUtils.fade(mProgressBar,
                                    AnimationUtils.DURATION_PLAYBACKSEEKMODE, false, true);
                            mCircularProgressBar.setOnTouchListener(null);
                            if (!mAbortSeeking) {
                                int seekTime = (int) ((mLastThumbPosition - mProgressBar.getX())
                                        / mProgressBar.getWidth()
                                        * mPlaybackService.getCurrentTrack().getDuration());
                                mPlaybackService.seekTo(seekTime);
                            }
                        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                            float eventX = event.getX();
                            float progressBarX = mProgressBar.getX();
                            float finalX;
                            if (eventX > mProgressBar.getWidth() + progressBarX) {
                                mAbortSeeking = true;
                                finalX = mProgressBar.getWidth() + progressBarX;
                            } else if (eventX < progressBarX) {
                                mAbortSeeking = true;
                                finalX = progressBarX;
                            } else {
                                mAbortSeeking = false;
                                finalX = eventX;
                            }
                            if (mAbortSeeking) {
                                AnimationUtils.fade(mProgressBarThumb,
                                        AnimationUtils.DURATION_PLAYBACKSEEKMODE_ABORT, false,
                                        true);
                            } else {
                                AnimationUtils.fade(mProgressBarThumb,
                                        AnimationUtils.DURATION_PLAYBACKSEEKMODE_ABORT, true,
                                        true);
                            }
                            mLastThumbPosition = finalX;
                            mProgressBarThumb.setX(finalX);
                            int seekTime = (int)
                                    ((finalX - mProgressBar.getX()) / mProgressBar.getWidth()
                                            * mPlaybackService.getCurrentTrack().getDuration());
                            mSeekTimeTextView.setText(TomahawkUtils.durationToString(seekTime));
                        }
                        return false;
                    }
                });
                return true;
            }
        });

        setupAnimations();
    }

    public void update(PlaybackService playbackService) {
        if (mInitialized) {
            mPlaybackService = playbackService;
            onPlayPositionChanged(0, 0);
            updateTextViewCompleteTime();
            updateText();
            updateResolverIconImageView();
            if (mPlaybackService != null) {
                updatePlayPauseState(mPlaybackService.isPlaying());
            }
        }
    }

    public void updatePlayPauseState(boolean isPlaying) {
        if (mInitialized) {
            if (isPlaying) {
                mPauseButton.setVisibility(VISIBLE);
                mPlayButton.setVisibility(GONE);
            } else {
                mPauseButton.setVisibility(GONE);
                mPlayButton.setVisibility(VISIBLE);
            }
        }
    }

    private void updateText() {
        if (mPlaybackService != null && mPlaybackService.getCurrentQuery() != null) {
            mArtistTextView.setText(mPlaybackService.getCurrentQuery().getArtist().getName());
            mTrackTextView.setText(mPlaybackService.getCurrentQuery().getName());
        }
    }

    /**
     * Updates the position on seekbar and the related textviews
     */
    public void onPlayPositionChanged(long duration, int currentPosition) {
        if (duration != 0) {
            mCircularProgressBar.setProgress((float) currentPosition / duration);
            mProgressBar.setProgress((int) ((float) currentPosition / duration * 10000));
            mCurrentTimeTextView.setText(TomahawkUtils.durationToString(currentPosition));
        } else {
            mProgressBar.setProgress(0);
            mCircularProgressBar.setProgress(0);
            mCurrentTimeTextView.setText(TomahawkUtils.durationToString(0));
        }
    }

    private void updateResolverIconImageView() {
        if (mPlaybackService != null && mPlaybackService.getCurrentQuery() != null
                && mPlaybackService.getCurrentQuery().getPreferredTrackResult() != null) {
            Resolver resolver =
                    mPlaybackService.getCurrentQuery().getPreferredTrackResult().getResolvedBy();
            resolver.loadIcon(mResolverImageView, false);
        }
    }

    /**
     * Updates the textview that shows the duration of the current track
     */
    private void updateTextViewCompleteTime() {
        if (mCompletionTimeTextView != null) {
            if (mPlaybackService != null
                    && mPlaybackService.getCurrentTrack() != null
                    && mPlaybackService.getCurrentTrack().getDuration() > 0) {
                mCompletionTimeTextView.setText(TomahawkUtils.durationToString(
                        mPlaybackService.getCurrentTrack().getDuration()));
            } else {
                mCompletionTimeTextView.setText(COMPLETION_STRING_DEFAULT);
            }
        }
    }

    private void setupAnimations() {
        TomahawkUtils.afterViewGlobalLayout(new TomahawkUtils.ViewRunnable(this) {
            @Override
            public void run() {
                mAnimators.clear();
                // get relevant dimension sizes first
                Resources resources = TomahawkApp.getContext().getResources();
                int panelHeight = resources.getDimensionPixelSize(
                        R.dimen.playback_panel_height);
                int padding = resources.getDimensionPixelSize(
                        R.dimen.padding_medium);
                int panelBottom = resources.getDimensionPixelSize(
                        R.dimen.playback_clear_space_bottom);
                int headerClearSpace = resources.getDimensionPixelSize(
                        R.dimen.header_clear_space_nonscrollable_playback);

                // Setup mTextViewContainer animation
                Keyframe kfY0 = Keyframe.ofFloat(0f,
                        getHeight() - mTextViewContainer.getHeight() / 2 - panelHeight / 2);
                Keyframe kfY1 = Keyframe.ofFloat(0.5f,
                        getHeight() + padding - panelBottom);
                Keyframe kfY2 = Keyframe.ofFloat(1f,
                        headerClearSpace / 2 - mTextViewContainer.getHeight() / 2);
                PropertyValuesHolder pvhY =
                        PropertyValuesHolder.ofKeyframe("y", kfY0, kfY1, kfY2);
                Keyframe kfScale0 = Keyframe.ofFloat(0f, 1f);
                Keyframe kfScale1 = Keyframe.ofFloat(0.5f, 1.5f);
                Keyframe kfScale2 = Keyframe.ofFloat(1f, 1.5f);
                PropertyValuesHolder pvhScaleY =
                        PropertyValuesHolder.ofKeyframe("scaleY", kfScale0, kfScale1, kfScale2);
                PropertyValuesHolder pvhScaleX =
                        PropertyValuesHolder.ofKeyframe("scaleX", kfScale0, kfScale1, kfScale2);
                ValueAnimator animator = ObjectAnimator
                        .ofPropertyValuesHolder(mTextViewContainer, pvhY,
                                pvhScaleX, pvhScaleY).setDuration(20000);
                animator.setInterpolator(new LinearInterpolator());
                animator.setCurrentPlayTime(mLastPlayTime);
                mAnimators.add(animator);

                // Setup mPanelContainer animation
                kfY0 = Keyframe.ofFloat(0f, getHeight() - mPanelContainer.getHeight());
                kfY1 = Keyframe.ofFloat(0.5f, getHeight() - mPanelContainer.getHeight());
                kfY2 = Keyframe.ofFloat(1f, headerClearSpace - mPanelContainer.getHeight());
                pvhY = PropertyValuesHolder.ofKeyframe("y", kfY0, kfY1, kfY2);
                animator = ObjectAnimator
                        .ofPropertyValuesHolder(mPanelContainer, pvhY).setDuration(20000);
                animator.setInterpolator(new LinearInterpolator());
                animator.setCurrentPlayTime(mLastPlayTime);
                mAnimators.add(animator);

                // Setup mTextViewContainer backgroundColor alpha animation
                Keyframe kfColor1 = Keyframe.ofInt(0f, 0x0);
                Keyframe kfColor2 = Keyframe.ofInt(0.5f, 0x0);
                Keyframe kfColor3 = Keyframe.ofInt(1f, 0xFF);
                PropertyValuesHolder pvhColor = PropertyValuesHolder
                        .ofKeyframe("color", kfColor1, kfColor2, kfColor3);
                animator = ValueAnimator.ofPropertyValuesHolder(pvhColor).setDuration(20000);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mTextViewContainer.findViewById(R.id.textview_container_inner)
                                .getBackground().setAlpha((Integer) animation.getAnimatedValue());
                    }
                });
                animator.setInterpolator(new LinearInterpolator());
                animator.setCurrentPlayTime(mLastPlayTime);
                mAnimators.add(animator);

                // Setup mPanelContainer background fade animation
                Keyframe kfBgAlpha0 = Keyframe.ofInt(0f, 0);
                Keyframe kfBgAlpha1 = Keyframe.ofInt(0.5f, 0);
                Keyframe kfBgAlpha2 = Keyframe.ofInt(1f, 255);
                PropertyValuesHolder pvhBgAlpha = PropertyValuesHolder
                        .ofKeyframe("alpha", kfBgAlpha0, kfBgAlpha1, kfBgAlpha2);
                animator = ObjectAnimator.ofPropertyValuesHolder(mPanelContainer.getBackground(),
                        pvhBgAlpha).setDuration(20000);
                animator.setInterpolator(new LinearInterpolator());
                animator.setCurrentPlayTime(mLastPlayTime);
                mAnimators.add(animator);
            }
        });
    }

    public void animate(int position) {
        mLastPlayTime = position;
        for (ValueAnimator animator : mAnimators) {
            if (animator != null && position != animator.getCurrentPlayTime()) {
                animator.setCurrentPlayTime(position);
            }
        }
    }

    public void showButtons() {
        mArtistNameButton.setClickable(true);
        TransitionDrawable drawable = (TransitionDrawable) mArtistNameButton.getBackground();
        drawable.startTransition(AnimationUtils.DURATION_CONTEXTMENU);
    }

    public void hideButtons() {
        mArtistNameButton.setClickable(false);
        TransitionDrawable drawable = (TransitionDrawable) mArtistNameButton.getBackground();
        drawable.reverseTransition(AnimationUtils.DURATION_CONTEXTMENU);
    }
}
