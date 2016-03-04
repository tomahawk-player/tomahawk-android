/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.libtomahawk.resolver;

import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;

/**
 * Used to provide output in the debug log. Especially in the case of an error.
 */
public class TomahawkWebChromeClient extends WebChromeClient {

    private final static String TAG = TomahawkWebChromeClient.class.getSimpleName();

    @Override
    public boolean onConsoleMessage(@NonNull ConsoleMessage cm) {
        String msg = cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId();
        if (cm.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
            Log.e(TAG, msg);
        } else {
            Log.d(TAG, msg);
        }
        return true;
    }
}
