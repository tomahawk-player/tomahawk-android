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
package org.tomahawk.tomahawk_android.dialogs;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigEdittext;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A {@link org.tomahawk.tomahawk_android.dialogs.ConfigDialog} which shows a textfield to enter a
 * username and password, and provides button for cancel/logout and ok/login, depending on whether
 * or not the user is logged in.
 */
public class LoginConfigDialog extends ConfigDialog {

    public final static String TAG = LoginConfigDialog.class.getSimpleName();

    private AuthenticatorUtils mAuthenticatorUtils;

    private EditText mUsernameEditText;

    private EditText mPasswordEditText;

    private class RegisterButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            dismiss();
            RegisterConfigDialog dialog = new RegisterConfigDialog();
            Bundle args = new Bundle();
            args.putString(TomahawkFragment.TOMAHAWK_PREFERENCEID_KEY, mAuthenticatorUtils.getId());
            args.putString(TomahawkFragment.TOMAHAWK_USERNAME_STRING,
                    mUsernameEditText.getText().toString());
            args.putString(TomahawkFragment.TOMAHAWK_PASSWORD_STRING,
                    mPasswordEditText.getText().toString());
            dialog.setArguments(args);
            dialog.show(getFragmentManager(), null);
        }
    }

    /**
     * Called when this {@link DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null && getArguments()
                .containsKey(TomahawkFragment.TOMAHAWK_PREFERENCEID_KEY)) {
            String authenticatorId = getArguments().getString(
                    TomahawkFragment.TOMAHAWK_PREFERENCEID_KEY);
            mAuthenticatorUtils = AuthenticatorManager.getInstance().getAuthenticatorUtils(
                    authenticatorId);
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        boolean isLoggedIn = mAuthenticatorUtils.isLoggedIn();
        LinearLayout usernameLayout = (LinearLayout) inflater.inflate(R.layout.config_text, null);
        mUsernameEditText = (ConfigEdittext) usernameLayout.findViewById(R.id.config_edittext);
        mUsernameEditText.setHint(mAuthenticatorUtils.getUserIdEditTextHintResId());
        mUsernameEditText.setText(isLoggedIn ? mAuthenticatorUtils.getUserName() : "");
        addScrollingViewToFrame(usernameLayout);
        LinearLayout passwordLayout = (LinearLayout) inflater.inflate(R.layout.config_text, null);
        mPasswordEditText = (ConfigEdittext) passwordLayout.findViewById(R.id.config_edittext);
        mPasswordEditText.setHint(R.string.login_password);
        mPasswordEditText.setTypeface(Typeface.DEFAULT);
        mPasswordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mPasswordEditText.setTransformationMethod(new PasswordTransformationMethod());
        mPasswordEditText.setOnEditorActionListener(mOnKeyboardEnterListener);
        addScrollingViewToFrame(passwordLayout);
        if (mAuthenticatorUtils.doesAllowRegistration() && !mAuthenticatorUtils.isLoggedIn()) {
            FrameLayout buttonLayout =
                    (FrameLayout) inflater.inflate(R.layout.config_button, null);
            LinearLayout button =
                    (LinearLayout) buttonLayout.findViewById(R.id.config_button);
            button.setOnClickListener(new RegisterButtonListener());
            TextView buttonText =
                    (TextView) buttonLayout.findViewById(R.id.config_button_text);
            buttonText.setText(R.string.register);
            addScrollingViewToFrame(buttonLayout);
        }

        showSoftKeyboard(mUsernameEditText);

        hideEnabledCheckbox();
        setDialogTitle(mAuthenticatorUtils.getPrettyName() + ": " + getString(R.string.login));
        setStatusImage(mAuthenticatorUtils.getIconResourceId(), isLoggedIn);
        updateButtonTexts(isLoggedIn);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getDialogView());
        return builder.create();
    }

    @Override
    protected void onEnabledCheckedChange(boolean checked) {
        // We don't care about this since we don't offer a checkbox in a LoginConfigDialog
    }

    @Override
    protected void onConfigTestResult(String componentId, int type, String message) {
        if (componentId.equals(mAuthenticatorUtils.getId())) {
            if (type == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS) {
                dismiss();
            } else if (type == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_LOGOUT) {
                updateButtonTexts(false);
            }
            stopLoadingAnimation(false);
        }
    }

    @Override
    protected void onPositiveAction() {
        if (mAuthenticatorUtils.isLoggedIn()) {
            dismiss();
        } else {
            attemptLogin();
        }
    }

    @Override
    protected void onNegativeAction() {
        if (mAuthenticatorUtils.isLoggedIn()) {
            startLoadingAnimation();
            mAuthenticatorUtils.logout(getActivity());
        } else {
            dismiss();
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form. If there are form
     * errors (invalid email, missing fields, etc.), the errors are presented and no actual login
     * attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mUsernameEditText.setError(null);
        mPasswordEditText.setError(null);

        // Store values at the time of the login attempt.
        String mEmail = mUsernameEditText.getText().toString();
        String mPassword = mPasswordEditText.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(mEmail)) {
            mUsernameEditText.setError(getString(R.string.error_field_required));
            focusView = mUsernameEditText;
            cancel = true;
        }

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordEditText.setError(getString(R.string.error_field_required));
            focusView = mPasswordEditText;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Tell the service to login
            mAuthenticatorUtils.login(getActivity(), mEmail, mPassword);
            startLoadingAnimation();
        }
    }

    /**
     * Update the texts of all buttons. Depends on whether or not the user is logged in.
     */
    private void updateButtonTexts(boolean isLoggedIn) {
        if (isLoggedIn) {
            setPositiveButtonText(R.string.ok);
            setNegativeButtonText(R.string.logout);
        } else {
            setPositiveButtonText(R.string.login);
            setNegativeButtonText(R.string.cancel);
        }
    }
}
