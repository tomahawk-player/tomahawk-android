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

import android.os.Message;

public abstract class DelayedHandler<T> extends WeakReferenceHandler<T> {

    private int mDelay;

    private long mStartingTime = 0L;

    private int mDelayReduction = 0;

    public DelayedHandler(T referencedObject, int delay) {
        super(referencedObject);

        mDelay = delay;
    }

    public abstract void handleMessage(Message msg);

    public void reset() {
        mDelayReduction = 0;
    }

    public void start() {
        if (mDelay - mDelayReduction > 0) {
            mStartingTime = System.currentTimeMillis();
            removeCallbacksAndMessages(null);
            Message message = obtainMessage();
            sendMessageDelayed(message, mDelay - mDelayReduction);
        }
    }

    public void stop() {
        int delayReduction = (int) (System.currentTimeMillis() - mStartingTime);
        if (delayReduction > 0) {
            mDelayReduction += delayReduction;
        }
        removeCallbacksAndMessages(null);
    }
}
