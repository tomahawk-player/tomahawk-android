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
import org.tomahawk.libtomahawk.resolver.HatchetStubResolver;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigEdittext;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A {@link ConfigDialog} which shows a textfield to enter a username and password, and provides
 * button for cancel/logout and ok/login, depending on whether or not the user is logged in.
 */
public class RegisterConfigDialog extends ConfigDialog {

    public final static String TAG = RegisterConfigDialog.class.getSimpleName();

    private AuthenticatorUtils mAuthenticatorUtils;

    private EditText mUsernameEditText;

    private EditText mPasswordEditText;

    private EditText mPasswordConfirmationEditText;

    private EditText mMailEditText;

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String username = "";
        String password = "";
        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkFragment.PREFERENCEID)) {
                String authenticatorId = getArguments().getString(
                        TomahawkFragment.PREFERENCEID);
                mAuthenticatorUtils = AuthenticatorManager.getInstance().getAuthenticatorUtils(
                        authenticatorId);
            }
            if (getArguments().containsKey(TomahawkFragment.USERNAME_STRING)) {
                username = getArguments().getString(TomahawkFragment.USERNAME_STRING);
            }
            if (getArguments().containsKey(TomahawkFragment.PASSWORD_STRING)) {
                password = getArguments().getString(TomahawkFragment.PASSWORD_STRING);
            }
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        LinearLayout headerTextLayout =
                (LinearLayout) inflater.inflate(R.layout.config_textview, null);
        TextView headerTextView = (TextView) headerTextLayout.findViewById(R.id.config_textview);
        headerTextView.setText(mAuthenticatorUtils.getDescription());
        addScrollingViewToFrame(headerTextLayout);
        LinearLayout usernameLayout =
                (LinearLayout) inflater.inflate(R.layout.config_edittext, null);
        mUsernameEditText = (ConfigEdittext) usernameLayout.findViewById(R.id.config_edittext);
        mUsernameEditText.setHint(mAuthenticatorUtils.getUserIdEditTextHintResId());
        mUsernameEditText.setText(username);
        addScrollingViewToFrame(usernameLayout);
        LinearLayout passwordLayout =
                (LinearLayout) inflater.inflate(R.layout.config_edittext, null);
        mPasswordEditText = (ConfigEdittext) passwordLayout.findViewById(R.id.config_edittext);
        mPasswordEditText.setHint(R.string.login_password);
        mPasswordEditText.setTypeface(Typeface.DEFAULT);
        mPasswordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mPasswordEditText.setTransformationMethod(new PasswordTransformationMethod());
        mPasswordEditText.setText(password);
        addScrollingViewToFrame(passwordLayout);
        LinearLayout passwordConfirmationLayout =
                (LinearLayout) inflater.inflate(R.layout.config_edittext, null);
        mPasswordConfirmationEditText =
                (ConfigEdittext) passwordConfirmationLayout.findViewById(R.id.config_edittext);
        mPasswordConfirmationEditText.setHint(
                R.string.login_password_confirmation);
        mPasswordConfirmationEditText.setTypeface(Typeface.DEFAULT);
        mPasswordConfirmationEditText.setTransformationMethod(new PasswordTransformationMethod());
        addScrollingViewToFrame(passwordConfirmationLayout);
        LinearLayout emailLayout = (LinearLayout) inflater.inflate(R.layout.config_edittext, null);
        mMailEditText = (ConfigEdittext) emailLayout.findViewById(R.id.config_edittext);
        mMailEditText.setHint(R.string.account_email_label);
        mMailEditText.setOnEditorActionListener(mOnKeyboardEnterListener);
        addScrollingViewToFrame(emailLayout);

        showSoftKeyboard(mUsernameEditText);

        setDialogTitle(mAuthenticatorUtils.getPrettyName() + ": " + getString(R.string.register));
        setStatus(
                new HatchetStubResolver(HatchetAuthenticatorUtils.HATCHET_PRETTY_NAME, null));
        setPositiveButtonText(R.string.register);
        setNegativeButtonText(R.string.cancel);
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
        if (mAuthenticatorUtils == component) {
            if (type == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS) {
                dismiss();
            }
            stopLoadingAnimation();
        }
    }

    @Override
    protected void onPositiveAction() {
        attemptRegister();
    }

    @Override
    protected void onNegativeAction() {
        dismiss();
    }

    /**
     * Attempts to sign in or register the account specified by the login form. If there are form
     * errors (invalid email, missing fields, etc.), the errors are presented and no actual login
     * attempt is made.
     */
    private void attemptRegister() {
        // Reset errors.
        mUsernameEditText.setError(null);
        mPasswordEditText.setError(null);
        mPasswordConfirmationEditText.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();
        String passwordConfirmation = mPasswordConfirmationEditText.getText().toString();
        String email = null;
        if (!TextUtils.isEmpty(mMailEditText.getText().toString())) {
            email = mMailEditText.getText().toString();
        }

        boolean cancel = false;
        View focusView = null;

        // Check for a valid username
        if (TextUtils.isEmpty(username)) {
            mUsernameEditText.setError(getString(R.string.error_field_required));
            focusView = mUsernameEditText;
            cancel = true;
        }

        // Check for a valid password.
        if (TextUtils.isEmpty(password)) {
            mPasswordEditText.setError(getString(R.string.error_field_required));
            focusView = mPasswordEditText;
            cancel = true;
        }

        // Check for a valid password confirmation.
        if (TextUtils.isEmpty(passwordConfirmation)) {
            mPasswordConfirmationEditText.setError(getString(R.string.error_field_required));
            focusView = mPasswordConfirmationEditText;
            cancel = true;
        }
        if (!password.equals(passwordConfirmation)) {
            mPasswordConfirmationEditText.setError(getString(R.string.error_passwords_dont_match));
            focusView = mPasswordConfirmationEditText;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt register and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Tell the service to register
            mAuthenticatorUtils.register(username, password, email);
            startLoadingAnimation();
        }
    }
}
