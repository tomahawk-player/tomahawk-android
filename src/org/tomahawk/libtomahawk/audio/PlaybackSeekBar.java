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
package org.tomahawk.libtomahawk.audio;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.TextView;

public class PlaybackSeekBar extends SeekBar
        implements Handler.Callback {

    private boolean mIsSeeking;
    private PlaybackService mPlaybackService;
    private OnSeekBarChangeListener mOnSeekBarChangeListener;
    private Handler mUiHandler;
    private TextView mTextViewCurrentTime;
    private TextView mTextViewCompletionTime;

    private static final int MSG_UPDATE_PROGRESS = 0x1;

    /** @param context
     * @param attrs */
    public PlaybackSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mUiHandler = new Handler(this);
        mOnSeekBarChangeListener = new OnSeekBarChangeListener() {
            /*
             * (non-Javadoc )
             * @see android .widget .SeekBar. OnSeekBarChangeListener # onProgressChanged (android .widget .SeekBar,
             * int, boolean)
             */
            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                updateTextViewCurrentTime();
            }

            /*
             * (non-Javadoc)
             * @see android.widget.SeekBar.OnSeekBarChangeListener#onStartTrackingTouch (android .widget.SeekBar)
             */
            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                setIsSeeking(true);
            }

            /*
             * (non-Javadoc)
             * @see android.widget.SeekBar.OnSeekBarChangeListener#onStopTrackingTouch (android .widget.SeekBar)
             */
            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
                setIsSeeking(false);
                mPlaybackService.seekTo(getProgress());
                updateSeekBarPosition();
            }
        };
        setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        setIsSeeking(false);
    }

    /*
     * (non-Javadoc)
     * @see android.os.Handler.Callback#handleMessage(android.os.Message)
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

    /** Updates the position on seekbar and the related textviews */
    public void updateSeekBarPosition() {
        if (!mPlaybackService.isPlaying() && !isIsSeeking())
            return;
        if (!isIsSeeking()) {
            setProgress(mPlaybackService.getPosition());
            updateTextViewCurrentTime();
        }
        mUiHandler.removeMessages(MSG_UPDATE_PROGRESS);
        mUiHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 100);
    }

    /** Updates the textview that shows the current time the track is at */
    protected void updateTextViewCurrentTime() {
        if (mTextViewCurrentTime != null)
            mTextViewCurrentTime.setText(String.format("%02d", mPlaybackService.getPosition() / 60000)
                    + ":" + String.format("%02.0f", (double) ((mPlaybackService.getPosition() / 1000) % 60)));
    }

    /** Updates the textview that shows the duration of the current track */
    protected void updateTextViewCompleteTime() {
        if (mTextViewCompletionTime != null)
            mTextViewCompletionTime.setText(String.format("%02d", mPlaybackService.getCurrentTrack().getDuration() / 60000)
                    + ":" + String.format("%02.0f", (double) ((mPlaybackService.getCurrentTrack().getDuration() / 1000) % 60)));
    }

    /** @return mIsSeeking showing whether or not the user is currently seeking */
    public boolean isIsSeeking() {
        return mIsSeeking;
    }

    /** @param mIsSeeking showing whether or not the user is currently seeking */
    public void setIsSeeking(boolean mIsSeeking) {
        this.mIsSeeking = mIsSeeking;
    }

    /** @return mUiHandler to handle the updating process of the PlaybackSeekBar */
    public Handler getUiHandler() {
        return mUiHandler;
    }

    /** @param mUiHandler to handle the updating process of the PlaybackSeekBar */
    public void setUiHandler(Handler mUiHandler) {
        this.mUiHandler = mUiHandler;
    }

    /** @return MSG_UPDATE_PROGRESS */
    public static int getMsgUpdateProgress() {
        return MSG_UPDATE_PROGRESS;
    }

    /** @param mPlaybackService */
    public void setPlaybackService(PlaybackService mPlaybackService) {
        this.mPlaybackService = mPlaybackService;
    }

    /** @param mTextViewCurrentTime displaying the current time */
    public void setTextViewCurrentTime(TextView mTextViewCurrentTime) {
        this.mTextViewCurrentTime = mTextViewCurrentTime;
    }

    /** @param mTextViewCompletionTime displaying the completion time */
    public void setTextViewCompletionTime(TextView mTextViewCompletionTime) {
        this.mTextViewCompletionTime = mTextViewCompletionTime;
    }

}
