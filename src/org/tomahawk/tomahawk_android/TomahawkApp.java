/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
package org.tomahawk.tomahawk_android;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.TomahawkExceptionReporter;

import android.app.Application;
import android.content.Context;
import android.os.Debug;
import android.os.StrictMode;
import android.util.Log;

/**
 * This class represents the Application core.
 */
@ReportsCrashes(formKey = "",
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_info,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        resDialogOkToast = R.string.crash_dialog_ok_toast)
public class TomahawkApp extends Application {

    private static final String TAG = TomahawkApp.class.getSimpleName();

    public final static String PLUGINNAME_HATCHET = "hatchet";

    public final static String PLUGINNAME_USERCOLLECTION = "usercollection";

    public final static String PLUGINNAME_SPOTIFY = "spotify";

    public final static String PLUGINNAME_DEEZER = "deezer";

    public final static String PLUGINNAME_BEATSMUSIC = "beatsmusic";

    public final static String PLUGINNAME_RDIO = "rdio";

    public final static String PLUGINNAME_JAMENDO = "jamendo";

    public final static String PLUGINNAME_OFFICIALFM = "officialfm";

    public final static String PLUGINNAME_SOUNDCLOUD = "soundcloud";

    private static Context sApplicationContext;

    @Override
    public void onCreate() {
        if (!Debug.isDebuggerConnected()) {
            ACRA.init(this);
            ACRA.getErrorReporter().setReportSender(new TomahawkExceptionReporter());
        }
        StrictMode.setThreadPolicy(
                new StrictMode.ThreadPolicy.Builder().detectCustomSlowCalls().detectDiskReads()
                        .detectDiskWrites().detectNetwork().penaltyLog().penaltyFlashScreen()
                        .build());
        try {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .setClassInstanceLimit(Class.forName(PlaybackService.class.getName()), 1)
                    .penaltyLog().build());
        } catch (ClassNotFoundException e) {
            Log.e(TAG, e.toString());
        }

        super.onCreate();

        sApplicationContext = getApplicationContext();
    }

    public static Context getContext() {
        return sApplicationContext;
    }

}
