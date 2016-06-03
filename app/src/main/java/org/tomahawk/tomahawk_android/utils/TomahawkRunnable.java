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

import android.support.annotation.NonNull;

public abstract class TomahawkRunnable implements Runnable, Comparable<TomahawkRunnable> {

    public static final int PRIORITY_IS_NOTIFICATION = 500;

    public static final int PRIORITY_IS_VERYHIGH = 200;

    public static final int PRIORITY_IS_INFOSYSTEM_HIGH = 100;

    public static final int PRIORITY_IS_INFOSYSTEM_MEDIUM = 90;

    public static final int PRIORITY_IS_AUTHENTICATING = 50;

    public static final int PRIORITY_IS_REPORTING_LOCALSOURCE = 40;

    public static final int PRIORITY_IS_REPORTING_SUBSCRIPTION = 25;

    public static final int PRIORITY_IS_REPORTING = 20;

    public static final int PRIORITY_IS_RESOLVING = 10;

    public static final int PRIORITY_IS_DATABASEACTION = 5;

    public static final int PRIORITY_IS_INFOSYSTEM_LOW = 4;

    public static final int PRIORITY_IS_REPORTING_WITH_HEADERREQUEST = 0;

    private final int mPriority;

    public TomahawkRunnable(int priority) {
        mPriority = priority;
    }

    public int getPriority() {
        return mPriority;
    }

    @Override
    public int compareTo(@NonNull TomahawkRunnable other) {
        return other.getPriority() - mPriority;
    }
}
