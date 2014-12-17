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

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.animation.ValueAnimator;
import com.pascalwelsch.holocircularprogressbar.HoloCircularProgressBar;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.AnimationUtils;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class PlaybackPanel extends FrameLayout {

    private PlaybackService mPlaybackService;

    public static final String COMPLETION_STRING_DEFAULT = "-:--";

    private boolean mIsSeeking;

    private SlidingUpPanelLayout mSlidingUpPanelView;

    private LinearLayout mTextViewContainer;

    private Point mStartingPoint;

    private Point mExpandedPanelPoint;

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

    private ValueAnimator mTextViewContainerAnimation;

    private int mLastPlayTime = 0;

    private boolean mInitialized = false;

    private static final int MSG_UPDATE_PROGRESS = 0x1;

    private static class Point {

        Point(float x, float y) {
            this.x = x;
            this.y = y;
        }

        float x;

        float y;
    }

    private Handler mProgressHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_PROGRESS:
                    updateSeekBarPosition();
                    break;
            }
            return true;
        }
    });

    public PlaybackPanel(Context context) {
        super(context);
        inflate(getContext(), R.layout.playback_panel, this);
    }

    public PlaybackPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(getContext(), R.layout.playback_panel, this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    public void setup(SlidingUpPanelLayout slidingUpPanelView) {
        mInitialized = true;
        mSlidingUpPanelView = slidingUpPanelView;

        mTextViewContainer = (LinearLayout) findViewById(R.id.textview_container);
        mArtistTextView = (TextView) mTextViewContainer.findViewById(R.id.artist_textview);
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
                mIsSeeking = true;
                if (!mSlidingUpPanelView.isPanelExpanded()
                        || getResources().getBoolean(R.bool.is_landscape)) {
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
                            mIsSeeking = false;
                            if (!mSlidingUpPanelView.isPanelExpanded()
                                    || getResources().getBoolean(R.bool.is_landscape)) {
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
                                mAbortSeeking = false;
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
    }

    public void update(PlaybackService playbackService) {
        if (mInitialized) {
            mPlaybackService = playbackService;
            updateSeekBarPosition();
            updateTextViewCompleteTime();
            updateText();
            updateResolverIconImageView();
            if (mPlaybackService != null) {
                updatePlayPauseState(mPlaybackService.isPlaying());
            }

            calculateAnimationPoints();
        }
    }

    private void calculateAnimationPoints() {
        final View content = mSlidingUpPanelView.findViewById(R.id.content);
        if (content != null) {
            content.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            Resources resources = TomahawkApp.getContext().getResources();
                            int panelHeight = resources
                                    .getDimensionPixelSize(R.dimen.playback_panel_height);
                            int resolverIconSize = resources.getDimensionPixelSize(
                                    R.dimen.playback_panel_resolver_icon_size);
                            int paddingSmall =
                                    resources.getDimensionPixelSize(R.dimen.padding_small);
                            mStartingPoint = new Point(
                                    resolverIconSize + panelHeight + paddingSmall,
                                    content.getHeight() - mTextViewContainer.getHeight() / 2
                                            - panelHeight / 2);

                            int padding = resources.getDimensionPixelSize(R.dimen.padding_medium);
                            int panelBottom = resources
                                    .getDimensionPixelSize(R.dimen.playback_clear_space_bottom);
                            float textViewWidthSum =
                                    mTextViewContainer.findViewById(R.id.artist_textview).getWidth()
                                            + mTextViewContainer.findViewById(R.id.hyphen_textview)
                                            .getWidth()
                                            + mTextViewContainer.findViewById(R.id.track_textview)
                                            .getWidth();
                            mExpandedPanelPoint = new Point(
                                    content.getHeight() + padding - panelBottom,
                                    (int) (content.getWidth() - textViewWidthSum * 1.5f) / 2);

                            setupAnimations();

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                content.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } else {
                                content.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            }
                        }
                    });
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
    public void updateSeekBarPosition() {
        if (mPlaybackService != null && mPlaybackService.getCurrentTrack() != null
                && mPlaybackService.getCurrentTrack().getDuration() != 0) {
            mProgressHandler.removeMessages(MSG_UPDATE_PROGRESS);
            mProgressHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 500);
            mCircularProgressBar.setProgress((float) mPlaybackService.getPosition()
                    / mPlaybackService.getCurrentTrack().getDuration());
            mProgressBar.setProgress((int) ((float) mPlaybackService.getPosition()
                    / mPlaybackService.getCurrentTrack().getDuration() * 10000));
            mCurrentTimeTextView
                    .setText(TomahawkUtils.durationToString(mPlaybackService.getPosition()));
        } else {
            mProgressBar.setProgress(0);
            mCircularProgressBar.setProgress(0);
            mCurrentTimeTextView.setText(TomahawkUtils.durationToString(0));
        }
    }

    public void stopUpdates() {
        mProgressHandler.removeMessages(MSG_UPDATE_PROGRESS);
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
        if (mTextViewContainer != null) {
            mTextViewContainer.setX(mStartingPoint.x);
            mTextViewContainer.setY(mStartingPoint.y);
            mTextViewContainer.setScaleX(1f);
            mTextViewContainer.setScaleY(1f);
            mTextViewContainer.setPivotX(0f);
            mTextViewContainer.setPivotY(0f);
            if (!getResources().getBoolean(R.bool.is_landscape)) {
                View content = mSlidingUpPanelView.findViewById(R.id.content);
                if (content != null) {
                    PropertyValuesHolder pvhY =
                            PropertyValuesHolder.ofFloat("y", mExpandedPanelPoint.x);
                    PropertyValuesHolder pvhX =
                            PropertyValuesHolder.ofFloat("x", mExpandedPanelPoint.y);
                    PropertyValuesHolder pvhScaleX = PropertyValuesHolder.ofFloat("scaleX", 1.5f);
                    PropertyValuesHolder pvhScaleY = PropertyValuesHolder.ofFloat("scaleY", 1.5f);
                    mTextViewContainerAnimation =
                            ObjectAnimator.ofPropertyValuesHolder(mTextViewContainer, pvhX, pvhY,
                                    pvhScaleX, pvhScaleY).setDuration(10000);
                    mTextViewContainerAnimation.setInterpolator(new LinearInterpolator());
                    mTextViewContainerAnimation.setCurrentPlayTime(mLastPlayTime);
                }
            }
        }
    }

    public void onPanelSlide(final View view, float f) {
        if (f > 0.15f) {
            AnimationUtils.fade(view.findViewById(R.id.top_buttonpanel), 0f, 1f,
                    AnimationUtils.DURATION_PLAYBACKTOPPANEL, true,
                    new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            if (view.findViewById(R.id.imageButton_repeat) != null) {
                                view.findViewById(R.id.imageButton_repeat)
                                        .setVisibility(View.VISIBLE);
                            }
                            if (view.findViewById(R.id.close_button) != null) {
                                view.findViewById(R.id.close_button)
                                        .setVisibility(View.VISIBLE);
                            }
                            if (view.findViewById(R.id.imageButton_shuffle) != null) {
                                view.findViewById(R.id.imageButton_shuffle)
                                        .setVisibility(View.VISIBLE);
                            }
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
        } else if (f < 0.15f) {
            AnimationUtils.fade(view.findViewById(R.id.top_buttonpanel), 1f, 0f,
                    AnimationUtils.DURATION_PLAYBACKTOPPANEL, false,
                    new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (view.findViewById(R.id.imageButton_repeat) != null) {
                                view.findViewById(R.id.imageButton_repeat)
                                        .setVisibility(View.GONE);
                            }
                            if (view.findViewById(R.id.close_button) != null) {
                                view.findViewById(R.id.close_button)
                                        .setVisibility(View.GONE);
                            }
                            if (view.findViewById(R.id.imageButton_shuffle) != null) {
                                view.findViewById(R.id.imageButton_shuffle)
                                        .setVisibility(View.GONE);
                            }
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
        int position = Math.min(10000, Math.max(0, (int) ((f - 0.8f) * 10000f / (1f - 0.8f))));
        mLastPlayTime = position;
        if (mTextViewContainerAnimation != null
                && position != mTextViewContainerAnimation.getCurrentPlayTime()) {
            mTextViewContainerAnimation.setCurrentPlayTime(position);
        }
    }
}
