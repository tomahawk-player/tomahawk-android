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
package org.tomahawk.tomahawk_android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.acra.ACRA;
import org.acra.CrashReportData;
import org.acra.ErrorReporter;
import org.acra.ReportField;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.tomahawk.libtomahawk.audio.PlaybackService;

import android.annotation.TargetApi;
import android.os.Debug;
import android.os.StrictMode;
import android.util.Log;

/**
 * This class uploads a Tomahawk Exception Report to oops.tomahawk-player.org.
 */
class TomahawkExceptionReporter implements ReportSender {

    private static final String TAG = TomahawkExceptionReporter.class.getName();

    /**
     * Construct a new TomahawkExceptionReporter
     * 
     * Use init() to create a TomahawkExceptionReporter publicly.
     */
    protected TomahawkExceptionReporter() {
    }

    /**
     * Initialize the TomahawkExceptionReporter.
     */
    static void init(TomahawkApp app) {
        if (!Debug.isDebuggerConnected()) {
            ACRA.init(app);
            TomahawkExceptionReporter reporter = new TomahawkExceptionReporter();
            ErrorReporter.getInstance().setReportSender(reporter);
        }
        initStrictMode();
    }

    @Override
    public void send(CrashReportData data) throws ReportSenderException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://oops.tomahawk-player.org/addreport.php");

        StringBuilder body = new StringBuilder();

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("Version", data
                .getProperty(ReportField.APP_VERSION_NAME)));

        nameValuePairs.add(new BasicNameValuePair("BuildID", data.getProperty(ReportField.BUILD)));
        nameValuePairs.add(new BasicNameValuePair("ProductName", "tomahawk-android"));
        nameValuePairs.add(new BasicNameValuePair("Vendor", "Tomahawk"));
        nameValuePairs.add(new BasicNameValuePair("timestamp", data
                .getProperty(ReportField.USER_CRASH_DATE)));

        for (NameValuePair pair : nameValuePairs) {
            body.append("--thkboundary\r\n");
            body.append("Content-Disposition: form-data; name=\"");
            body.append(pair.getName() + "\"\r\n\r\n" + pair.getValue() + "\r\n");
        }

        body.append("--thkboundary\r\n");
        body.append("Content-Disposition: form-data; name=\"upload_file_minidump\"; filename=\""
                + data.getProperty(ReportField.REPORT_ID) + "\"\r\n");
        body.append("Content-Type: application/octet-stream\r\n\r\n");

        body.append("============== Tomahawk Exception Report ==============\r\n\r\n");
        body.append("Report ID: " + data.getProperty(ReportField.REPORT_ID) + "\r\n");
        body.append("App Start Date: " + data.getProperty(ReportField.USER_APP_START_DATE) + "\r\n");
        body.append("Crash Date: " + data.getProperty(ReportField.USER_CRASH_DATE) + "\r\n\r\n");

        body.append("--------- Phone Details  ----------\r\n");
        body.append("Phone Model: " + data.getProperty(ReportField.PHONE_MODEL) + "\r\n");
        body.append("Brand: " + data.getProperty(ReportField.BRAND) + "\r\n");
        body.append("Product: " + data.getProperty(ReportField.PRODUCT) + "\r\n");
        body.append("Display: " + data.getProperty(ReportField.DISPLAY) + "\r\n");
        body.append("-----------------------------------\r\n\r\n");

        body.append("----------- Stack Trace -----------\r\n");
        body.append(data.getProperty(ReportField.STACK_TRACE) + "\r\n");
        body.append("-----------------------------------\r\n\r\n");

        body.append("------- Operating System  ---------\r\n");
        body.append("App Version Name: " + data.getProperty(ReportField.APP_VERSION_NAME) + "\r\n");
        body.append("Total Mem Size: " + data.getProperty(ReportField.TOTAL_MEM_SIZE) + "\r\n");
        body.append("Available Mem Size: " + data.getProperty(ReportField.AVAILABLE_MEM_SIZE)
                + "\r\n");
        body.append("Dumpsys Meminfo: " + data.getProperty(ReportField.DUMPSYS_MEMINFO) + "\r\n");
        body.append("-----------------------------------\r\n\r\n");

        body.append("-------------- Misc ---------------\r\n");
        body.append("Package Name: " + data.getProperty(ReportField.PACKAGE_NAME) + "\r\n");
        body.append("File Path: " + data.getProperty(ReportField.FILE_PATH) + "\r\n");

        body.append("Android Version: " + data.getProperty(ReportField.ANDROID_VERSION) + "\r\n");
        body.append("Build: " + data.getProperty(ReportField.BUILD) + "\r\n");
        body.append("Initial Configuration:  "
                + data.getProperty(ReportField.INITIAL_CONFIGURATION) + "\r\n");
        body.append("Crash Configuration: " + data.getProperty(ReportField.CRASH_CONFIGURATION)
                + "\r\n");
        body.append("Settings Secure: " + data.getProperty(ReportField.SETTINGS_SECURE) + "\r\n");
        body.append("User Email: " + data.getProperty(ReportField.USER_EMAIL) + "\r\n");
        body.append("User Comment: " + data.getProperty(ReportField.USER_COMMENT) + "\r\n");
        body.append("-----------------------------------\r\n\r\n");

        body.append("---------------- Logs -------------\r\n");
        body.append("Logcat: " + data.getProperty(ReportField.LOGCAT) + "\r\n\r\n");
        body.append("Events Log: " + data.getProperty(ReportField.EVENTSLOG) + "\r\n\r\n");
        body.append("Radio Log: " + data.getProperty(ReportField.RADIOLOG) + "\r\n");
        body.append("-----------------------------------\r\n\r\n");

        body.append("=======================================================\r\n\r\n");
        body.append("--thkboundary\r\n");
        body.append("Content-Disposition: form-data; name=\"upload_file_tomahawklog\"; filename=\"Tomahawk.log\"\r\n");
        body.append("Content-Type: text/plain\r\n\r\n");
        body.append(data.getProperty(ReportField.LOGCAT));
        body.append("\r\n--thkboundary--\r\n");

        httppost.setHeader("Content-type", "multipart/form-data; boundary=thkboundary");

        try {

            httppost.setEntity(new StringEntity(body.toString()));
            httpclient.execute(httppost);

        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }
    }

    /**
     * Use strict mode to determine app bottlenecks.
     * 
     * Does nothing if api version is less than 11.
     */
    @TargetApi(11)
    private static void initStrictMode() {

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB)
            return;

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectCustomSlowCalls()
                .detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog()
                .penaltyFlashScreen().build());

        try {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .setClassInstanceLimit(Class.forName(PlaybackService.class.getName()), 1)
                    .penaltyLog().build());
        } catch (ClassNotFoundException e) {
            Log.e(TAG, e.toString());
        }
    }
}
