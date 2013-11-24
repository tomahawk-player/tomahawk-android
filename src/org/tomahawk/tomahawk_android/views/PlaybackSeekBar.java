/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
 *   Copyright 2012, Hugo Lindström <hugolm84@gmail.com>
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

import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.services.PlaybackService;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Customized {@link SeekBar} for the {@link org.tomahawk.tomahawk_android.fragments.PlaybackControlsFragment}
 */
public class PlaybackSeekBar extends SeekBar implements Handler.Callback {

    private boolean mIsSeeking;

    private PlaybackService mPlaybackService;

    private Handler mUiHandler;

    private TextView mTextViewCurrentTime;

    private TextView mTextViewCompletionTime;

    private int mUpdateInterval;

    private static final int MSG_UPDATE_PROGRESS = 0x1;

    /**
     * Construct a {@link PlaybackSeekBar}
     */
    public PlaybackSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mUiHandler = new Handler(this);
        OnSeekBarChangeListener onSeekBarChangeListener = new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                if (isIsSeeking()) {
                    updateTextViewCurrentTime(mPlaybackService.getPosition());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                setIsSeeking(true);
            }

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
                setIsSeeking(false);
                mPlaybackService.seekTo(getProgress());
                updateSeekBarPosition();
            }
        };
        setOnSeekBarChangeListener(onSeekBarChangeListener);
        setIsSeeking(false);
    }

    /**
     * If the message is MSG_UPDATE_PROGRESS, update the seekbar position
     */
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_UPDATE_PROGRESS:
                updateSeekBarPosition();
                break;
        }
        return true;
    }

    /**
     * Set max to current {@link org.tomahawk.libtomahawk.collection.Track}'s duration
     */
    public void setMax() {
        setMax((int) mPlaybackService.getCurrentTrack().getDuration());
    }

    /**
     * Set the interval in which to update the seekbar position
     */
    public void setUpdateInterval() {
        mUpdateInterval = (int) (mPlaybackService.getCurrentTrack().getDuration() / 300);
        mUpdateInterval = Math.min(mUpdateInterval, 250);
        mUpdateInterval = Math.max(mUpdateInterval, 20);
    }

    /**
     * Updates the position on seekbar and the related textviews
     */
    public void updateSeekBarPosition() {
        if (!isIsSeeking()) {
            if (mPlaybackService.isPreparing() ||
                    mPlaybackService.getCurrentTrack() == null
                    || mPlaybackService.getCurrentTrack().getDuration() == 0) {
                setEnabled(false);
            } else {
                setEnabled(true);
            }
            if (!mPlaybackService.isPreparing()) {
                setProgress(mPlaybackService.getPosition());
                updateTextViewCurrentTime(mPlaybackService.getPosition());
            } else {
                setProgress(0);
                updateTextViewCurrentTime(0);
            }
        }
        mUiHandler.removeMessages(MSG_UPDATE_PROGRESS);
        mUiHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, mUpdateInterval);
    }

    /**
     * Updates the textview that shows the current time the track is at
     */
    public void updateTextViewCurrentTime(int position) {
        if (mTextViewCurrentTime != null) {
            if (!isIsSeeking() && mPlaybackService.getCurrentPlaylist().getCount() > 0) {
                mTextViewCurrentTime.setText(TomahawkUtils.durationToString(position));
            } else if (mPlaybackService.getCurrentPlaylist().getCount() > 0) {
                mTextViewCurrentTime.setText(TomahawkUtils.durationToString(getProgress()));
            } else {
                mTextViewCurrentTime.setText(TomahawkUtils.durationToString(0));
            }
        }
    }

    /**
     * Updates the textview that shows the duration of the current track
     */
    public void updateTextViewCompleteTime() {
        if (mTextViewCompletionTime != null) {
            if (mPlaybackService.getCurrentTrack() != null
                    && mPlaybackService.getCurrentTrack().getDuration() > 0) {
                mTextViewCompletionTime.setText(TomahawkUtils
                        .durationToString(mPlaybackService.getCurrentTrack().getDuration()));
            } else {
                mTextViewCompletionTime.setText(getResources()
                        .getString(R.string.playbackactivity_seekbar_completion_time_string));
            }
        }
    }

    /**
     * @return mIsSeeking showing whether or not the user is currently seeking
     */
    public boolean isIsSeeking() {
        return mIsSeeking;
    }

    /**
     * @param mIsSeeking showing whether or not the user is currently seeking
     */
    public void setIsSeeking(boolean mIsSeeking) {
        this.mIsSeeking = mIsSeeking;
    }

    public void setPlaybackService(PlaybackService mPlaybackService) {
        this.mPlaybackService = mPlaybackService;
    }

    /**
     * @param mTextViewCurrentTime displaying the current time
     */
    public void setTextViewCurrentTime(TextView mTextViewCurrentTime) {
        this.mTextViewCurrentTime = mTextViewCurrentTime;
    }

    /**
     * @param mTextViewCompletionTime displaying the completion time
     */
    public void setTextViewCompletionTime(TextView mTextViewCompletionTime) {
        this.mTextViewCompletionTime = mTextViewCompletionTime;
    }

}
