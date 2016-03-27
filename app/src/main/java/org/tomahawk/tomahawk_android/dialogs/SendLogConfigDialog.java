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

import org.acra.ACRA;
import org.tomahawk.libtomahawk.utils.ViewUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigEdittext;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A {@link ConfigDialog} which shows a textfield to enter the reason why the user wants to send us
 * a log. A click on "OK" will dispatch an email intent with the generated data.
 */
public class SendLogConfigDialog extends ConfigDialog {

    public final static String TAG = SendLogConfigDialog.class.getSimpleName();

    public static String mLastEmail;

    public static String mLastUsermessage;

    private EditText mEmailEditText;

    private EditText mUserMessageEditText;

    public static class SendLogException extends Throwable {

        @Override
        public String toString() {
            return getDefaultString();
        }

        public static String getDefaultString() {
            return SendLogException.class.getSimpleName()
                    + ": User manually requested to send a log";
        }

    }

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        TextView headerTextView = (TextView) addScrollingViewToFrame(R.layout.config_textview);
        headerTextView.setText(R.string.preferences_app_sendlog_dialog_text);
        mEmailEditText = (ConfigEdittext) addScrollingViewToFrame(R.layout.config_edittext);
        mEmailEditText.setHint(R.string.preferences_app_sendlog_email);
        mUserMessageEditText =
                (ConfigEdittext) addScrollingViewToFrame(R.layout.config_edittext_multiplelines);
        mUserMessageEditText.setHint(R.string.preferences_app_sendlog_issue);

        ViewUtils.showSoftKeyboard(mEmailEditText);

        setDialogTitle(getString(R.string.preferences_app_sendlog));
        hideConnectImage();

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
        } else {
            mLastEmail = email;
            mLastUsermessage = userMessage;

            ACRA.getErrorReporter().handleSilentException(new SendLogException());
            Toast.makeText(TomahawkApp.getContext(), R.string.crash_dialog_ok_toast,
                    Toast.LENGTH_LONG).show();
            dismiss();
        }
    }

    @Override
    protected void onNegativeAction() {
        dismiss();
    }
}
