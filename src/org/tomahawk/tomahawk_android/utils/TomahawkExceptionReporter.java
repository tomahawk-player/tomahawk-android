/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.PlaybackService;

import android.annotation.TargetApi;
import android.os.Debug;
import android.os.StrictMode;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class uploads a Tomahawk Exception Report to oops.tomahawk-player.org.
 */
public class TomahawkExceptionReporter implements ReportSender {

    private static final String TAG = TomahawkExceptionReporter.class.getSimpleName();

    /**
     * Construct a new TomahawkExceptionReporter
     *
     * Use init() to create a TomahawkExceptionReporter publicly.
     */
    private TomahawkExceptionReporter() {
    }

    /**
     * Initialize the TomahawkExceptionReporter.
     */
    public static void init(TomahawkApp app) {
        if (!Debug.isDebuggerConnected()) {
            ACRA.init(app);
            TomahawkExceptionReporter reporter = new TomahawkExceptionReporter();
            ACRA.getErrorReporter().setReportSender(reporter);
        }
        initStrictMode();
    }

    /**
     * Pull information from the given {@link CrashReportData} and send it via HTTP to
     * oops.tomahawk-player.org
     */
    @Override
    public void send(CrashReportData data) throws ReportSenderException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://oops.tomahawk-player.org/addreport.php");

        StringBuilder body = new StringBuilder();

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("Version",
                data.getProperty(ReportField.APP_VERSION_NAME)));

        nameValuePairs.add(new BasicNameValuePair("BuildID", data.getProperty(ReportField.BUILD)));
        nameValuePairs.add(new BasicNameValuePair("ProductName", "tomahawk-android"));
        nameValuePairs.add(new BasicNameValuePair("Vendor", "Tomahawk"));
        nameValuePairs.add(new BasicNameValuePair("timestamp",
                data.getProperty(ReportField.USER_CRASH_DATE)));

        for (NameValuePair pair : nameValuePairs) {
            body.append("--thkboundary\r\n");
            body.append("Content-Disposition: form-data; name=\"");
            body.append(pair.getName()).append("\"\r\n\r\n").append(pair.getValue()).append("\r\n");
        }

        body.append("--thkboundary\r\n");
        body.append("Content-Disposition: form-data; name=\"upload_file_minidump\"; filename=\"")
                .append(data.getProperty(ReportField.REPORT_ID)).append("\"\r\n");
        body.append("Content-Type: application/octet-stream\r\n\r\n");

        body.append("============== Tomahawk Exception Report ==============\r\n\r\n");
        body.append("Report ID: ").append(data.getProperty(ReportField.REPORT_ID)).append("\r\n");
        body.append("App Start Date: ").append(data.getProperty(ReportField.USER_APP_START_DATE))
                .append("\r\n");
        body.append("Crash Date: ").append(data.getProperty(ReportField.USER_CRASH_DATE))
                .append("\r\n\r\n");

        body.append("--------- Phone Details  ----------\r\n");
        body.append("Phone Model: ").append(data.getProperty(ReportField.PHONE_MODEL))
                .append("\r\n");
        body.append("Brand: ").append(data.getProperty(ReportField.BRAND)).append("\r\n");
        body.append("Product: ").append(data.getProperty(ReportField.PRODUCT)).append("\r\n");
        body.append("Display: ").append(data.getProperty(ReportField.DISPLAY)).append("\r\n");
        body.append("-----------------------------------\r\n\r\n");

        body.append("----------- Stack Trace -----------\r\n");
        body.append(data.getProperty(ReportField.STACK_TRACE)).append("\r\n");
        body.append("-----------------------------------\r\n\r\n");

        body.append("------- Operating System  ---------\r\n");
        body.append("App Version Name: ").append(data.getProperty(ReportField.APP_VERSION_NAME))
                .append("\r\n");
        body.append("Total Mem Size: ").append(data.getProperty(ReportField.TOTAL_MEM_SIZE))
                .append("\r\n");
        body.append("Available Mem Size: ").append(data.getProperty(ReportField.AVAILABLE_MEM_SIZE))
                .append("\r\n");
        body.append("Dumpsys Meminfo: ").append(data.getProperty(ReportField.DUMPSYS_MEMINFO))
                .append("\r\n");
        body.append("-----------------------------------\r\n\r\n");

        body.append("-------------- Misc ---------------\r\n");
        body.append("Package Name: ").append(data.getProperty(ReportField.PACKAGE_NAME))
                .append("\r\n");
        body.append("File Path: ").append(data.getProperty(ReportField.FILE_PATH)).append("\r\n");

        body.append("Android Version: ").append(data.getProperty(ReportField.ANDROID_VERSION))
                .append("\r\n");
        body.append("Build: ").append(data.getProperty(ReportField.BUILD)).append("\r\n");
        body.append("Initial Configuration:  ")
                .append(data.getProperty(ReportField.INITIAL_CONFIGURATION)).append("\r\n");
        body.append("Crash Configuration: ")
                .append(data.getProperty(ReportField.CRASH_CONFIGURATION)).append("\r\n");
        body.append("Settings Secure: ").append(data.getProperty(ReportField.SETTINGS_SECURE))
                .append("\r\n");
        body.append("User Email: ").append(data.getProperty(ReportField.USER_EMAIL)).append("\r\n");
        body.append("User Comment: ").append(data.getProperty(ReportField.USER_COMMENT))
                .append("\r\n");
        body.append("-----------------------------------\r\n\r\n");

        body.append("---------------- Logs -------------\r\n");
        body.append("Logcat: ").append(data.getProperty(ReportField.LOGCAT)).append("\r\n\r\n");
        body.append("Events Log: ").append(data.getProperty(ReportField.EVENTSLOG))
                .append("\r\n\r\n");
        body.append("Radio Log: ").append(data.getProperty(ReportField.RADIOLOG)).append("\r\n");
        body.append("-----------------------------------\r\n\r\n");

        body.append("=======================================================\r\n\r\n");
        body.append("--thkboundary\r\n");
        body.append(
                "Content-Disposition: form-data; name=\"upload_file_tomahawklog\"; filename=\"Tomahawk.log\"\r\n");
        body.append("Content-Type: text/plain\r\n\r\n");
        body.append(data.getProperty(ReportField.LOGCAT));
        body.append("\r\n--thkboundary--\r\n");

        httppost.setHeader("Content-type", "multipart/form-data; boundary=thkboundary");

        try {

            httppost.setEntity(new StringEntity(body.toString()));
            httpclient.execute(httppost);

        } catch (ClientProtocolException e) {
            Log.e(TAG, "send: " + e.getClass() + ": " + e.getLocalizedMessage());
        } catch (IOException e) {
            Log.e(TAG, "send: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    /**
     * Use strict mode to determine app bottlenecks.
     *
     * Does nothing if api version is less than 11.
     */
    @TargetApi(11)
    private static void initStrictMode() {

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            return;
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
    }
}
