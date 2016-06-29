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

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.nineoldandroids.animation.Keyframe;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.animation.ValueAnimator;

import org.tomahawk.libtomahawk.collection.StationPlaylist;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.utils.ViewUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.AnimationUtils;
import org.tomahawk.tomahawk_android.utils.PlaybackManager;
import org.tomahawk.tomahawk_android.utils.PreferenceUtils;
import org.tomahawk.tomahawk_android.utils.ProgressBarUpdater;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.TransitionDrawable;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
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

    public static final String COMPLETION_STRING_DEFAULT = "-:--";

    private FrameLayout mTextViewContainer;

    private View mPanelContainer;

    private View mStationContainer;

    private View mStationContainerInner;

    private FrameLayout mArtistNameButton;

    private TextView mArtistTextView;

    private TextView mTrackTextView;

    private TextView mCompletionTimeTextView;

    private TextView mCurrentTimeTextView;

    private TextView mSeekTimeTextView;

    private TextView mStationTextView;

    private ImageView mResolverImageView;

    private ImageView mPlayButton;

    private ImageView mPauseButton;

    private ProgressBar mProgressBar;

    private View mProgressBarThumb;

    private float mLastThumbPosition = -1f;

    private boolean mAbortSeeking;

    private FrameLayout mPlayPauseButtonContainer;

    private CircularProgressView mPlayPauseButton;

    private ValueAnimator mStationContainerAnimation;

    private final Set<ValueAnimator> mAnimators = new HashSet<>();

    private int mLastPlayTime = 0;

    private boolean mInitialized = false;

    private MediaControllerCompat mMediaController;

    private PlaybackManager mPlaybackManager;

    private ProgressBarUpdater mProgressBarUpdater;

    private long mCurrentDuration;

    private OnLayoutChangeListener mStationLayoutChangeListener = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                int oldTop, int oldRight, int oldBottom) {
            setupStationContainerAnimation();
        }
    };

    public PlaybackPanel(Context context) {
        super(context);
        inflate(getContext(), R.layout.playback_panel, this);
        init();
    }

    public PlaybackPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(getContext(), R.layout.playback_panel, this);
        init();
    }

    private void init() {
        mTextViewContainer = (FrameLayout) findViewById(R.id.textview_container);
        mPanelContainer = findViewById(R.id.panel_container);
        mStationContainer = findViewById(R.id.station_container);
        mStationContainerInner = findViewById(R.id.station_container_inner);
        mStationContainerInner.addOnLayoutChangeListener(mStationLayoutChangeListener);
        mArtistNameButton = (FrameLayout) mTextViewContainer.findViewById(R.id.artist_name_button);
        mArtistTextView = (TextView) mArtistNameButton.findViewById(R.id.artist_textview);
        mTrackTextView = (TextView) mTextViewContainer.findViewById(R.id.track_textview);
        mCompletionTimeTextView = (TextView) findViewById(R.id.completiontime_textview);
        mCurrentTimeTextView = (TextView) findViewById(R.id.currenttime_textview);
        mSeekTimeTextView = (TextView) findViewById(R.id.seektime_textview);
        mStationTextView = (TextView) findViewById(R.id.station_textview);
        mResolverImageView = (ImageView) findViewById(R.id.resolver_imageview);
        mPlayButton = (ImageView) findViewById(R.id.play_button);
        mPauseButton = (ImageView) findViewById(R.id.pause_button);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        mProgressBarThumb = findViewById(R.id.progressbar_thumb);
        mPlayPauseButtonContainer =
                (FrameLayout) findViewById(R.id.circularprogressbar_container);
        mPlayPauseButton = (CircularProgressView)
                mPlayPauseButtonContainer.findViewById(R.id.circularprogressbar);

        mProgressBarUpdater = new ProgressBarUpdater(
                new ProgressBarUpdater.UpdateProgressRunnable() {
                    @Override
                    public void updateProgress(PlaybackStateCompat playbackState, long duration) {
                        if (playbackState == null) {
                            return;
                        }
                        long currentPosition = playbackState.getPosition();
                        if (playbackState.getState() != PlaybackStateCompat.STATE_PAUSED) {
                            // Calculate the elapsed time between the last position update and now and unless
                            // paused, we can assume (delta * speed) + current position is approximately the
                            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
                            // on MediaControllerCompat.
                            long timeDelta = SystemClock.elapsedRealtime() -
                                    playbackState.getLastPositionUpdateTime();
                            currentPosition += (int) timeDelta * playbackState.getPlaybackSpeed();
                        }
                        mProgressBar
                                .setProgress((int) ((float) currentPosition / duration * 10000));
                        mPlayPauseButton.setProgress((float) currentPosition / duration * 1000);
                        mCurrentTimeTextView.setText(ViewUtils.durationToString(currentPosition));
                    }
                });
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return false;
    }

    public void setup(final boolean isPanelExpanded) {
        mInitialized = true;

        mPlayPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaController != null) {
                    int playState = mMediaController.getPlaybackState().getState();
                    if (playState == PlaybackStateCompat.STATE_PAUSED
                            || playState == PlaybackStateCompat.STATE_NONE) {
                        mMediaController.getTransportControls().play();
                    } else if (playState == PlaybackStateCompat.STATE_PLAYING) {
                        mMediaController.getTransportControls().pause();
                        mMediaController.getTransportControls()
                                .sendCustomAction(PlaybackService.ACTION_STOP_NOTIFICATION, null);
                    }
                }
            }
        });

        mPlayPauseButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PreferenceUtils.edit()
                        .putBoolean(PreferenceUtils.COACHMARK_SEEK_DISABLED, true)
                        .apply();
                View coachMark = ViewUtils.ensureInflation(PlaybackPanel.this,
                        R.id.playbackpanel_seek_coachmark_stub, R.id.playbackpanel_seek_coachmark);
                coachMark.setVisibility(GONE);
                if (!isPanelExpanded || getResources().getBoolean(R.bool.is_landscape)) {
                    AnimationUtils.fade(mTextViewContainer,
                            AnimationUtils.DURATION_PLAYBACKSEEKMODE, false, true);
                }
                AnimationUtils.fade(mPlayPauseButtonContainer,
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

                mPlayPauseButton.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            if (!isPanelExpanded || getResources()
                                    .getBoolean(R.bool.is_landscape)) {
                                AnimationUtils.fade(mTextViewContainer,
                                        AnimationUtils.DURATION_PLAYBACKSEEKMODE, true, true);
                            }
                            AnimationUtils.fade(mPlayPauseButtonContainer,
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
                            mPlayPauseButton.setOnTouchListener(null);
                            if (!mAbortSeeking) {
                                int seekTime = (int) ((mLastThumbPosition - mProgressBar.getX())
                                        / mProgressBar.getWidth() * mCurrentDuration);
                                mMediaController.getTransportControls().seekTo(seekTime);
                            }
                        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                            float eventX = event.getX();
                            float progressBarX = mProgressBar.getX();
                            float finalX;
                            if (eventX > mProgressBar.getWidth() + progressBarX) {
                                // Only fade out thumb if eventX is above the threshold
                                int threshold = getResources().getDimensionPixelSize(
                                        R.dimen.playback_panel_seekbar_threshold_end);
                                mAbortSeeking = eventX > mProgressBar.getWidth() + progressBarX
                                        + threshold;
                                finalX = mProgressBar.getWidth() + progressBarX;
                            } else if (eventX < progressBarX) {
                                // Only fade out thumb if eventX is below the threshold
                                int threshold = getResources().getDimensionPixelSize(
                                        R.dimen.playback_panel_seekbar_threshold_start);
                                mAbortSeeking = eventX < progressBarX - threshold;
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
                                            * mCurrentDuration);
                            mSeekTimeTextView.setText(ViewUtils.durationToString(seekTime));
                        }
                        return false;
                    }
                });
                return true;
            }
        });

        setupAnimations();
    }

    public void setMediaController(MediaControllerCompat mediaController) {
        mMediaController = mediaController;
        String playbackManagerId = mediaController.getExtras()
                .getString(PlaybackService.EXTRAS_KEY_PLAYBACKMANAGER);
        mPlaybackManager = PlaybackManager.getByKey(playbackManagerId);
        if (mediaController.getMetadata() != null) {
            updateMetadata(mediaController.getMetadata());
        }
        updatePlaybackState(mediaController.getPlaybackState());
    }

    public void updateMetadata(MediaMetadataCompat metadata) {
        mCurrentDuration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        mProgressBarUpdater.setCurrentDuration(mCurrentDuration);
        mCurrentTimeTextView.setText(ViewUtils.durationToString(0));
        updateTextViewCompleteTime();
        updateText();
        updateImageViews();
    }

    public void updatePlaybackState(PlaybackStateCompat playbackState) {
        if (mInitialized) {
            mProgressBarUpdater.setPlaybackState(playbackState);
            if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
                if (mPlayPauseButton.isIndeterminate()) {
                    mPlayPauseButton.setIndeterminate(false);
                    mPlayPauseButton.setColor(getResources().getColor(android.R.color.white));
                    mPlayPauseButton.resetAnimation();
                }
                mPauseButton.setVisibility(VISIBLE);
                mPlayButton.setVisibility(GONE);
                mProgressBarUpdater.scheduleSeekbarUpdate();
            } else if (playbackState.getState() == PlaybackStateCompat.STATE_PAUSED) {
                if (mPlayPauseButton.isIndeterminate()) {
                    mPlayPauseButton.setIndeterminate(false);
                    mPlayPauseButton.setColor(getResources().getColor(android.R.color.white));
                    mPlayPauseButton.resetAnimation();
                }
                mPauseButton.setVisibility(GONE);
                mPlayButton.setVisibility(VISIBLE);
                mProgressBarUpdater.stopSeekbarUpdate();
            } else if (playbackState.getState() == PlaybackStateCompat.STATE_BUFFERING) {
                if (!mPlayPauseButton.isIndeterminate()) {
                    mPlayPauseButton.setIndeterminate(true);
                    mPlayPauseButton.setColor(getResources().getColor(R.color.tomahawk_red));
                    mPlayPauseButton.startAnimation();
                }
                mProgressBarUpdater.stopSeekbarUpdate();
            }
        }
    }

    private void updateText() {
        if (mPlaybackManager.getCurrentQuery() != null) {
            mArtistTextView.setText(mPlaybackManager.getCurrentQuery().getArtist().getPrettyName());
            mTrackTextView.setText(mPlaybackManager.getCurrentQuery().getPrettyName());
        } else {
            mArtistTextView.setText(null);
            mTrackTextView.setText(null);
        }
        if (mPlaybackManager.getPlaylist() instanceof StationPlaylist) {
            if (mPlaybackManager.getCurrentQuery() == null) {
                MediaMetadataCompat metadata = mMediaController.getMetadata();
                if (metadata != null) {
                    String displayTitle =
                            metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE);
                    if (displayTitle != null) {
                        mTrackTextView.setText(displayTitle);
                    }
                }
            }
            mStationContainer.setVisibility(VISIBLE);
            mStationTextView.setText(mPlaybackManager.getPlaylist().getName());
        } else {
            mStationContainer.setVisibility(INVISIBLE);
        }

    }

    private void updateImageViews() {
        if (mPlaybackManager.getCurrentQuery() != null
                && mPlaybackManager.getCurrentQuery().getPreferredTrackResult() != null) {
            mResolverImageView.setVisibility(VISIBLE);
            Resolver resolver =
                    mPlaybackManager.getCurrentQuery().getPreferredTrackResult().getResolvedBy();
            if (TomahawkApp.PLUGINNAME_USERCOLLECTION.equals(resolver.getId())) {
                resolver.loadIconWhite(mResolverImageView, 0);
            } else {
                resolver.loadIcon(mResolverImageView, false);
            }
        } else {
            mResolverImageView.setVisibility(INVISIBLE);
        }
    }

    /**
     * Updates the textview that shows the duration of the current track
     */
    private void updateTextViewCompleteTime() {
        if (mPlaybackManager.getCurrentTrack() != null
                && mPlaybackManager.getCurrentTrack().getDuration() > 0) {
            mCompletionTimeTextView.setText(
                    ViewUtils.durationToString(mPlaybackManager.getCurrentTrack().getDuration()));
        } else {
            mCompletionTimeTextView.setText(COMPLETION_STRING_DEFAULT);
        }
    }

    private void setupAnimations() {
        ViewUtils.afterViewGlobalLayout(new ViewUtils.ViewRunnable(this) {
            @Override
            public void run() {
                mAnimators.clear();
                // get relevant dimension sizes first
                Resources resources = TomahawkApp.getContext().getResources();
                int panelHeight = resources.getDimensionPixelSize(
                        R.dimen.playback_panel_height);
                int paddingSmall = resources.getDimensionPixelSize(
                        R.dimen.padding_small);
                int paddingLarge = resources.getDimensionPixelSize(
                        R.dimen.padding_large);
                int panelBottom = resources.getDimensionPixelSize(
                        R.dimen.playback_clear_space_bottom);
                int headerClearSpace = resources.getDimensionPixelSize(
                        R.dimen.header_clear_space_nonscrollable_playback);
                boolean isLandscape = resources.getBoolean(R.bool.is_landscape);

                // Setup mTextViewContainer animation
                Keyframe kfY0 = Keyframe.ofFloat(0f,
                        getHeight() - mTextViewContainer.getHeight() / 2 - panelHeight / 2);
                Keyframe kfY1 = Keyframe.ofFloat(0.5f,
                        isLandscape ?
                                getHeight() + paddingLarge - panelBottom - mTextViewContainer
                                        .getHeight()
                                : getHeight() + paddingSmall - panelBottom);
                Keyframe kfY2 = Keyframe.ofFloat(1f,
                        headerClearSpace / 2 - mTextViewContainer.getHeight() / 2);
                PropertyValuesHolder pvhY =
                        PropertyValuesHolder.ofKeyframe("y", kfY0, kfY1, kfY2);
                Keyframe kfScale0 = Keyframe.ofFloat(0f, 1f);
                Keyframe kfScale1 = Keyframe.ofFloat(0.5f, isLandscape ? 1.25f : 1.5f);
                Keyframe kfScale2 = Keyframe.ofFloat(1f, isLandscape ? 1.25f : 1.5f);
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
                Keyframe kfColor2 = Keyframe.ofInt(0.5f, isLandscape ? 0xFF : 0x0);
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

                setupStationContainerAnimation();
            }
        });
    }

    public void setupStationContainerAnimation() {
        Resources resources = TomahawkApp.getContext().getResources();
        int resolverIconSize = resources.getDimensionPixelSize(
                R.dimen.playback_panel_resolver_icon_size);
        int padding = resources.getDimensionPixelSize(
                R.dimen.padding_small);

        // Setup mStationContainer animation
        Keyframe kfX0 = Keyframe.ofFloat(0f,
                mStationContainer.getWidth() - resolverIconSize);
        Keyframe kfX1 = Keyframe.ofFloat(0.5f,
                Math.max(resolverIconSize + padding,
                        mStationContainer.getWidth() / 2 - mStationContainerInner.getWidth() / 2));
        Keyframe kfX2 = Keyframe.ofFloat(1f,
                mStationContainer.getWidth() - resolverIconSize);
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofKeyframe("x", kfX0, kfX1, kfX2);
        ValueAnimator animator = ObjectAnimator
                .ofPropertyValuesHolder(mStationContainerInner, pvhX).setDuration(20000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setCurrentPlayTime(mLastPlayTime);
        if (mStationContainerAnimation != null) {
            mAnimators.remove(mStationContainerAnimation);
        }
        mStationContainerAnimation = animator;
        mAnimators.add(mStationContainerAnimation);
    }

    public void hideStationContainer() {
        if (mPlaybackManager.getPlaylist() instanceof StationPlaylist) {
            AnimationUtils
                    .fade(mStationContainer, AnimationUtils.DURATION_PLAYBACKTOPPANEL, false, true);
        }
    }

    public void showStationContainer() {
        if (mPlaybackManager.getPlaylist() instanceof StationPlaylist) {
            AnimationUtils
                    .fade(mStationContainer, AnimationUtils.DURATION_PLAYBACKTOPPANEL, true, true);
        }
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
