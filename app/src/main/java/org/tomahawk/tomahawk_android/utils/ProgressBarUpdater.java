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
package org.tomahawk.tomahawk_android.utils;

import android.os.Handler;
import android.support.v4.media.session.PlaybackStateCompat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ProgressBarUpdater {

    private static final long PROGRESS_UPDATE_INTERNAL = 500;

    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

    private final ScheduledExecutorService mExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> mScheduleFuture;

    private final Handler mHandler = new Handler();

    private PlaybackStateCompat mPlaybackState;

    private long mCurrentDuration;

    private UpdateProgressRunnable mUpdateProgressRunnable;

    public interface UpdateProgressRunnable {

        void updateProgress(PlaybackStateCompat playbackState, long duration);
    }

    public ProgressBarUpdater(UpdateProgressRunnable updateProgressRunnable) {
        mUpdateProgressRunnable = updateProgressRunnable;
    }

    public void setPlaybackState(PlaybackStateCompat playbackState) {
        mPlaybackState = playbackState;
    }

    public void setCurrentDuration(long currentDuration) {
        mCurrentDuration = currentDuration;
    }

    public void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mPlaybackState != null) {
                                        mUpdateProgressRunnable.updateProgress(mPlaybackState,
                                                mCurrentDuration);
                                    }
                                }
                            });
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    public void stopSeekbarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

}
