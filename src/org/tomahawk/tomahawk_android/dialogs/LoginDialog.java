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
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A {@link DialogFragment} which shows a textfield to enter a username and password, and provides
 * button for cancel/logout and ok/login, depending on whether or not the user is logged in
 */
public class LoginDialog extends DialogFragment {

    public final static String TAG = LoginDialog.class.getName();

    private AuthenticatorUtils mAuthenticatorUtils;

    private EditText mUsernameEditText;

    private EditText mPasswordEditText;

    private TextView mPositiveButton;

    private TextView mNegativeButton;

    private Drawable mProgressDrawable;

    private ImageView mStatusImageView;

    //Used to handle the animation of our progress animation, while we try to login
    private Handler mAnimationHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_ANIMATION:
                    if (mAuthenticatorUtils.isAuthenticating()) {
                        mProgressDrawable.setLevel(mProgressDrawable.getLevel() + 500);
                        mStatusImageView.setImageDrawable(mProgressDrawable);
                        mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
                        mAnimationHandler.sendEmptyMessageDelayed(MSG_UPDATE_ANIMATION, 50);
                    } else {
                        stopLoadingAnimation();
                        boolean isLoggedIn = AuthenticatorUtils
                                .isLoggedIn(getActivity().getApplicationContext(),
                                        mAuthenticatorUtils.getAuthenticatorUtilsName(),
                                        mAuthenticatorUtils.getAuthenticatorUtilsTokenType());
                        if (isLoggedIn) {
                            dismiss();
                        } else {
                            updateButtonTexts(isLoggedIn);
                        }
                    }
                    break;
            }
            return true;
        }
    });

    private static final int MSG_UPDATE_ANIMATION = 0x20;

    //So that the user can login by pressing "Enter" or something similar on his keyboard
    private TextView.OnEditorActionListener mOnLoginActionListener
            = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (event == null || actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                attemptLogin();
            }
            return false;
        }
    };

    private View.OnClickListener mPositiveButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (AuthenticatorUtils.isLoggedIn(getActivity().getApplicationContext(),
                    mAuthenticatorUtils.getAuthenticatorUtilsName(),
                    mAuthenticatorUtils.getAuthenticatorUtilsTokenType())) {
                hideSoftKeyboard();
                getDialog().dismiss();
            } else {
                attemptLogin();
            }
        }
    };

    private View.OnClickListener mNegativeButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (AuthenticatorUtils.isLoggedIn(getActivity().getApplicationContext(),
                    mAuthenticatorUtils.getAuthenticatorUtilsName(),
                    mAuthenticatorUtils.getAuthenticatorUtilsTokenType())) {
                startLoadingAnimation();
                mAuthenticatorUtils.logout();
            } else {
                hideSoftKeyboard();
                getDialog().cancel();
            }
        }
    };

    /**
     * Called when this {@link DialogFragment} is being created
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null && getArguments()
                .containsKey(TomahawkFragment.TOMAHAWK_AUTHENTICATORID_KEY)) {
            String authenticatorId = getArguments().getString(
                    TomahawkFragment.TOMAHAWK_AUTHENTICATORID_KEY);
            mAuthenticatorUtils = AuthenticatorManager.getInstance().getAuthenticatorUtils(
                    authenticatorId);
            if (mAuthenticatorUtils.isAuthenticating()) {
                startLoadingAnimation();
            }
        }

        InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.login_dialog, null);
        boolean isLoggedIn = AuthenticatorUtils.isLoggedIn(getActivity().getApplicationContext(),
                mAuthenticatorUtils.getAuthenticatorUtilsName(),
                mAuthenticatorUtils.getAuthenticatorUtilsTokenType());
        mUsernameEditText = (EditText) view.findViewById(R.id.login_dialog_username_edittext);
        mUsernameEditText.setHint(mAuthenticatorUtils.getUserIdEditTextHintResId());
        mUsernameEditText.setOnEditorActionListener(mOnLoginActionListener);
        mUsernameEditText.setText(isLoggedIn
                ? AuthenticatorUtils.getUserName(getActivity().getApplicationContext(),
                mAuthenticatorUtils.getAuthenticatorUtilsName()) : "");
        mPasswordEditText = (EditText) view.findViewById(R.id.login_dialog_password_edittext);
        mPasswordEditText.setTypeface(Typeface.DEFAULT);
        mPasswordEditText.setTransformationMethod(new PasswordTransformationMethod());
        mPasswordEditText.setOnEditorActionListener(mOnLoginActionListener);

        TextView textView = (TextView) view.findViewById(R.id.login_dialog_title_textview);
        textView.setText(mAuthenticatorUtils.getTitleResourceId());

        mPositiveButton = (TextView) view.findViewById(R.id.login_dialog_ok_button);
        mPositiveButton.setOnClickListener(mPositiveButtonListener);
        mNegativeButton = (TextView) view.findViewById(R.id.login_dialog_cancel_button);
        mNegativeButton.setOnClickListener(mNegativeButtonListener);
        mProgressDrawable = getResources().getDrawable(R.drawable.progress_indeterminate_tomahawk);
        mStatusImageView = (ImageView) view.findViewById(R.id.login_dialog_status_imageview);
        TomahawkUtils.loadDrawableIntoImageView(getActivity(), mStatusImageView,
                mAuthenticatorUtils.getIconResourceId(), !isLoggedIn);
        updateButtonTexts(isLoggedIn);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        return builder.create();
    }

    @Override
    public void onPause() {
        super.onPause();

        stopLoadingAnimation();
    }

    /**
     * Attempts to sign in or register the account specified by the login form. If there are form
     * errors (invalid email, missing fields, etc.), the errors are presented and no actual login
     * attempt is made.
     */
    public void attemptLogin() {

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
            hideSoftKeyboard();
            // Tell the service to login
            mAuthenticatorUtils.login(mEmail, mPassword);
            startLoadingAnimation();
        }
    }

    /**
     * Hide the soft keyboard
     */
    private void hideSoftKeyboard() {
        if (getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mUsernameEditText.getWindowToken(), 0);
        }
    }

    /**
     * Update the texts of all buttons. Depends on whether or not the user is logged in.
     */
    private void updateButtonTexts(boolean isLoggedIn) {
        if (isLoggedIn) {
            mPositiveButton.setText(R.string.ok);
            mNegativeButton.setText(R.string.logout);
        } else {
            mPositiveButton.setText(R.string.login);
            mNegativeButton.setText(R.string.cancel);
        }
    }

    /**
     * Start the loading animation. Called when beginning login process.
     */
    public void startLoadingAnimation() {
        mAnimationHandler.sendEmptyMessageDelayed(MSG_UPDATE_ANIMATION, 50);
    }

    /**
     * Stop the loading animation. Called when login/logout process has finished.
     */
    public void stopLoadingAnimation() {
        mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
        boolean isLoggedIn = AuthenticatorUtils.isLoggedIn(getActivity().getApplicationContext(),
                mAuthenticatorUtils.getAuthenticatorUtilsName(),
                mAuthenticatorUtils.getAuthenticatorUtilsTokenType());
        TomahawkUtils.loadDrawableIntoImageView(getActivity(), mStatusImageView,
                mAuthenticatorUtils.getIconResourceId(), !isLoggedIn);
    }
}
