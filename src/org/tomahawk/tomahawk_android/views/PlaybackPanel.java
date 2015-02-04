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

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.animation.ValueAnimator;
import com.pascalwelsch.holocircularprogressbar.HoloCircularProgressBar;

import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.AnimationUtils;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class PlaybackPanel extends FrameLayout {

    private PlaybackService mPlaybackService;

    public static final String COMPLETION_STRING_DEFAULT = "-:--";

    private FrameLayout mTextViewContainer;

    private LinearLayout mPanelContainer;

    private boolean mIsPanelExpanded;

    private Map<View, AnimationGoal> mStartingPoints = new HashMap<>();

    private Map<View, AnimationGoal> mExpandedPanelPoints = new HashMap<>();

    private Map<View, AnimationGoal> mScrolledDownPanelPoints = new HashMap<>();

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

    private ValueAnimator mPanelContainerAnimation;

    private int mLastPlayTime = 0;

    private boolean mInitialized = false;

    private static final int MSG_UPDATE_PROGRESS = 0x1;

    private static class AnimationGoal {

        AnimationGoal(int x, int y, float scaleX, float scaleY) {
            this.x = x;
            this.y = y;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
        }

        int x;

        int y;

        float scaleX;

        float scaleY;
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

    public void setup(final boolean isPanelExpanded) {
        mInitialized = true;

        mTextViewContainer = (FrameLayout) findViewById(R.id.textview_container);
        mPanelContainer = (LinearLayout) findViewById(R.id.panel_container);
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

    public void setPanelExpanded(boolean isPanelExpanded) {
        mIsPanelExpanded = isPanelExpanded;
        mLastPlayTime = 0;
        setupAnimations();
    }

    private void calculateAnimationPoints() {
        TomahawkUtils.afterViewGlobalLayout(new TomahawkUtils.ViewRunnable(this) {
            @Override
            public void run() {
                Resources resources = TomahawkApp.getContext().getResources();
                int panelHeight =
                        resources.getDimensionPixelSize(R.dimen.playback_panel_height);
                mStartingPoints.put(mTextViewContainer, new AnimationGoal(
                        (int) mTextViewContainer.getX(),
                        getHeight() - mTextViewContainer.getHeight() / 2 - panelHeight / 2,
                        1f, 1f));
                mStartingPoints.put(mPanelContainer, new AnimationGoal(
                        0, getHeight() - mPanelContainer.getHeight(), 1f, 1f));

                int padding =
                        resources.getDimensionPixelSize(R.dimen.padding_medium);
                int panelBottom =
                        resources.getDimensionPixelSize(R.dimen.playback_clear_space_bottom);
                mExpandedPanelPoints.put(mTextViewContainer, new AnimationGoal(
                        (int) mTextViewContainer.getX(),
                        getHeight() + padding - panelBottom, 1.5f, 1.5f));
                mExpandedPanelPoints.put(mPanelContainer, new AnimationGoal(
                        0, getHeight() - mPanelContainer.getHeight(), 1f, 1f));

                int headerClearSpace = resources.getDimensionPixelSize(
                        R.dimen.header_clear_space_nonscrollable_playback);
                mScrolledDownPanelPoints.put(mTextViewContainer, new AnimationGoal(
                        (int) mTextViewContainer.getX(),
                        headerClearSpace / 2 - mTextViewContainer.getHeight() / 2, 1.5f, 1.5f));
                mScrolledDownPanelPoints.put(mPanelContainer, new AnimationGoal(
                        0, headerClearSpace - mPanelContainer.getHeight(), 1f, 1f));

                setupAnimations();
            }
        });
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
        mTextViewContainerAnimation = setupAnimation(mTextViewContainer);
        mPanelContainerAnimation = setupAnimation(mPanelContainer);
    }

    private ValueAnimator setupAnimation(View view) {
        AnimationGoal firstAnimationGoal, secondAnimationGoal;
        if (mIsPanelExpanded) {
            firstAnimationGoal = mExpandedPanelPoints.get(view);
            secondAnimationGoal = mScrolledDownPanelPoints.get(view);
        } else {
            firstAnimationGoal = mStartingPoints.get(view);
            secondAnimationGoal = mExpandedPanelPoints.get(view);
        }
        ValueAnimator animator = null;
        if (firstAnimationGoal != null && secondAnimationGoal != null) {
            view.setX(firstAnimationGoal.x);
            view.setY(firstAnimationGoal.y);
            view.setScaleX(firstAnimationGoal.scaleX);
            view.setScaleY(firstAnimationGoal.scaleY);
            if (!getResources().getBoolean(R.bool.is_landscape)) {
                PropertyValuesHolder pvhX =
                        PropertyValuesHolder.ofFloat("x", secondAnimationGoal.x);
                PropertyValuesHolder pvhY =
                        PropertyValuesHolder.ofFloat("y", secondAnimationGoal.y);
                PropertyValuesHolder pvhScaleX =
                        PropertyValuesHolder.ofFloat("scaleX", secondAnimationGoal.scaleX);
                PropertyValuesHolder pvhScaleY =
                        PropertyValuesHolder.ofFloat("scaleY", secondAnimationGoal.scaleY);
                animator = ObjectAnimator.ofPropertyValuesHolder(view, pvhX, pvhY,
                        pvhScaleX, pvhScaleY).setDuration(10000);
                animator.setInterpolator(new LinearInterpolator());
                animator.setCurrentPlayTime(mLastPlayTime);
            }
        }
        return animator;
    }

    public void animate(int position) {
        mLastPlayTime = position;
        if (mTextViewContainerAnimation != null
                && position != mTextViewContainerAnimation.getCurrentPlayTime()) {
            mTextViewContainerAnimation.setCurrentPlayTime(position);
        }
        if (mPanelContainerAnimation != null
                && position != mPanelContainerAnimation.getCurrentPlayTime()) {
            mPanelContainerAnimation.setCurrentPlayTime(position);
        }
    }
}
