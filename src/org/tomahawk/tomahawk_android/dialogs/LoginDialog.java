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

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.services.TomahawkService;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 14.06.13
 */

public class LoginDialog extends DialogFragment {

    public final static String TAG = LoginDialog.class.getName();

    EditText mUsernameEditText;

    EditText mPasswordEditText;

    private TomahawkService mTomahawkService;

    public LoginDialog(TomahawkService tomahawkService) {
        setRetainInstance(true);
        mTomahawkService = tomahawkService;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.login_dialog, null);
        mUsernameEditText = (EditText) view.findViewById(R.id.login_dialog_username_edittext);
        mUsernameEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mUsernameEditText.setSingleLine(true);
        mUsernameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
        });
        mUsernameEditText.setText(
                mTomahawkService.getSpotifyUserId() != null ? mTomahawkService.getSpotifyUserId()
                        : "");
        mPasswordEditText = (EditText) view.findViewById(R.id.login_dialog_password_edittext);
        mPasswordEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mPasswordEditText.setSingleLine(true);
        mPasswordEditText.setTypeface(Typeface.DEFAULT);
        mPasswordEditText.setTransformationMethod(new PasswordTransformationMethod());
        mPasswordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
        });
        TextView textView = (TextView) view.findViewById(R.id.login_dialog_title_textview);
        DialogInterface.OnClickListener onPositiveButtonClickedListener
                = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                attemptLogin();
            }
        };
        builder.setPositiveButton(R.string.ok, onPositiveButtonClickedListener);
        textView.setText(R.string.logindialog_spotify_dialog_title);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                hideSoftKeyboard();
                getDialog().cancel();
            }
        });
        builder.setView(view);
        return builder.create();
    }

    /**
     * Attempts to sign in or register the account specified by the loginSpotify form. If there are
     * form errors (invalid email, missing fields, etc.), the errors are presented and no actual
     * loginSpotify attempt is made.
     */
    public void attemptLogin() {

        // Reset errors.
        mUsernameEditText.setError(null);
        mPasswordEditText.setError(null);

        // Store values at the time of the login Spotify attempt.
        String mEmail = mUsernameEditText.getText().toString();
        String mPassword = mPasswordEditText.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordEditText.setError(getString(R.string.error_field_required));
            if (focusView == null) {
                focusView = mPasswordEditText;
            }
            cancel = true;
        } else if (mPassword.length() < 1) {
            mPasswordEditText.setError(getString(R.string.error_invalid_password));
            if (focusView == null) {
                focusView = mPasswordEditText;
            }
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(mEmail)) {
            mUsernameEditText.setError(getString(R.string.error_field_required));
            if (focusView == null) {
                focusView = mUsernameEditText;
            }
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login Spotify and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {

            // Tell the service to login Spotify
            mTomahawkService.loginSpotify(mEmail, mPassword);
        }
    }

    private void hideSoftKeyboard() {
        if (getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mUsernameEditText.getWindowToken(), 0);
        }
    }
}
