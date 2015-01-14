/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.dialogs;

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigEdittext;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkExceptionReporter;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Pattern;

/**
 * A {@link ConfigDialog} which shows a textfield to enter the reason why the user wants to send us
 * a log. A click on "OK" will dispatch an email intent with the generated data.
 */
public class SendLogConfigDialog extends ConfigDialog {

    public final static String TAG = SendLogConfigDialog.class.getSimpleName();

    private EditText mEmailEditText;

    private EditText mUserMessageEditText;

    private String mLogData;

    private boolean mSendingLog;

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null && getArguments().containsKey(TomahawkFragment.LOG_DATA)) {
            mLogData = getArguments().getString(TomahawkFragment.LOG_DATA);
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        LinearLayout headerTextLayout =
                (LinearLayout) inflater.inflate(R.layout.config_textview, null);
        TextView headerTextView = (TextView) headerTextLayout.findViewById(R.id.config_textview);
        headerTextView.setText(R.string.preferences_app_sendlog_dialog_text);
        addScrollingViewToFrame(headerTextLayout);
        LinearLayout emailLayout =
                (LinearLayout) inflater.inflate(R.layout.config_edittext, null);
        mEmailEditText = (ConfigEdittext) emailLayout.findViewById(R.id.config_edittext);
        mEmailEditText.setHint(R.string.preferences_app_sendlog_email);
        addScrollingViewToFrame(emailLayout);
        LinearLayout usernameLayout =
                (LinearLayout) inflater.inflate(R.layout.config_edittext_multiplelines, null);
        mUserMessageEditText = (ConfigEdittext) usernameLayout.findViewById(R.id.config_edittext);
        mUserMessageEditText.setHint(R.string.preferences_app_sendlog_issue);
        addScrollingViewToFrame(usernameLayout);

        showSoftKeyboard(mEmailEditText);

        setDialogTitle(getString(R.string.preferences_app_sendlog));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getDialogView());
        return builder.create();
    }

    @Override
    protected void onEnabledCheckedChange(boolean checked) {
        // We don't care about this since we don't offer a checkbox in a LoginConfigDialog
    }

    @Override
    protected void onConfigTestResult(Object component, int type, String message) {
    }

    @Override
    protected void onPositiveAction() {
        // Reset errors.
        mUserMessageEditText.setError(null);

        // Store values at the time of the login attempt.
        final String email = mEmailEditText.getText().toString();
        final String userMessage = mUserMessageEditText.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check that the text field isn't empty
        if (TextUtils.isEmpty(email)) {
            mEmailEditText.setError(getString(R.string.error_field_required));
            focusView = mEmailEditText;
            cancel = true;
        }

        // Check that the text field isn't empty
        if (TextUtils.isEmpty(userMessage)) {
            mUserMessageEditText.setError(getString(R.string.error_field_required));
            focusView = mUserMessageEditText;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt to send the log and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else if (!mSendingLog) {
            mSendingLog = true;
            startLoadingAnimation();
            ThreadManager.getInstance()
                    .execute(new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_VERYHIGH) {
                        @Override
                        public void run() {
                            mLogData = Pattern
                                    .compile("User Email:.*\\r\\nUser Comment:.*\\r\\n")
                                    .matcher(mLogData).replaceFirst("User Email: " + email
                                            + "\r\nUser Comment: " + userMessage + "\r\n");
                            TomahawkExceptionReporter.sendCrashReport(mLogData);
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    dismiss();
                                    Toast.makeText(TomahawkApp.getContext(),
                                            R.string.crash_dialog_ok_toast, Toast.LENGTH_LONG)
                                            .show();
                                }
                            });
                            mSendingLog = false;
                        }
                    });
        }
    }

    @Override
    protected void onNegativeAction() {
        dismiss();
    }
}
