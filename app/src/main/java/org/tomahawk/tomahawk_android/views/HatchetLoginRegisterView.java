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
package org.tomahawk.tomahawk_android.views;

import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.utils.ViewUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigEdittext;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class HatchetLoginRegisterView extends LinearLayout {

    private AuthenticatorUtils mAuthenticatorUtils;

    private ProgressBar mProgressBar;

    //So that the user can login by pressing "Enter" or something similar on his keyboard
    private final TextView.OnEditorActionListener mOnKeyboardEnterListener
            = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (event == null || actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                loginButtonClick();
            }
            return false;
        }
    };

    private ViewPager mViewPager;

    private TextView mLoginButton;

    private EditText mLoginUsernameEditText;

    private EditText mLoginPasswordEditText;

    private EditText mRegisterUsernameEditText;

    private EditText mRegisterPasswordEditText;

    private EditText mPasswordConfirmationEditText;

    private EditText mMailEditText;

    private ViewPager.OnPageChangeListener mOnPageChangeListener
            = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            updateButtonTexts();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            switch (mViewPager.getCurrentItem()) {
                case 0:
                    mRegisterUsernameEditText.setText(mLoginUsernameEditText.getText());
                    mRegisterPasswordEditText.setText(mLoginPasswordEditText.getText());
                    break;
                case 1:
                    mLoginUsernameEditText.setText(mRegisterUsernameEditText.getText());
                    mLoginPasswordEditText.setText(mRegisterPasswordEditText.getText());
                    break;
            }
        }
    };

    private class LoginButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            loginButtonClick();
        }
    }

    private class LoginRegisterPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            switch (position) {
                case 0:
                    LinearLayout loginContainer = new LinearLayout(getContext());
                    loginContainer.setOrientation(LinearLayout.VERTICAL);
                    loginContainer.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));

                    mLoginUsernameEditText = (ConfigEdittext)
                            inflater.inflate(R.layout.config_edittext, container, false);
                    mLoginUsernameEditText
                            .setHint(mAuthenticatorUtils.getUserIdEditTextHintResId());
                    mLoginUsernameEditText.setText(mAuthenticatorUtils.isLoggedIn()
                            ? mAuthenticatorUtils.getUserName() : "");
                    loginContainer.addView(mLoginUsernameEditText);

                    mLoginPasswordEditText = (ConfigEdittext)
                            inflater.inflate(R.layout.config_edittext, container, false);
                    mLoginPasswordEditText.setHint(R.string.login_password);
                    mLoginPasswordEditText.setTypeface(Typeface.DEFAULT);
                    mLoginPasswordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    mLoginPasswordEditText
                            .setTransformationMethod(new PasswordTransformationMethod());
                    mLoginPasswordEditText.setOnEditorActionListener(mOnKeyboardEnterListener);
                    loginContainer.addView(mLoginPasswordEditText);

                    if (mAuthenticatorUtils.isLoggedIn()) {
                        mLoginUsernameEditText.setEnabled(false);
                        mLoginPasswordEditText.setEnabled(false);
                    } else {
                        mLoginUsernameEditText.setEnabled(true);
                        mLoginPasswordEditText.setEnabled(true);
                    }

                    FrameLayout frameContainer = new FrameLayout(getContext());
                    frameContainer.addView(loginContainer);
                    FrameLayout.LayoutParams frameParams =
                            (FrameLayout.LayoutParams) loginContainer.getLayoutParams();
                    frameParams.gravity = Gravity.CENTER_VERTICAL;
                    container.addView(frameContainer);
                    ViewUtils.showSoftKeyboard(mLoginUsernameEditText);
                    return frameContainer;
                case 1:
                    LinearLayout registerContainer = new LinearLayout(getContext());
                    registerContainer.setOrientation(LinearLayout.VERTICAL);
                    registerContainer.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));

                    mRegisterUsernameEditText = (ConfigEdittext)
                            inflater.inflate(R.layout.config_edittext, container, false);
                    mRegisterUsernameEditText
                            .setHint(mAuthenticatorUtils.getUserIdEditTextHintResId());
                    mRegisterUsernameEditText.setText(mAuthenticatorUtils.isLoggedIn()
                            ? mAuthenticatorUtils.getUserName() : "");
                    registerContainer.addView(mRegisterUsernameEditText);

                    mRegisterPasswordEditText = (ConfigEdittext)
                            inflater.inflate(R.layout.config_edittext, container, false);
                    mRegisterPasswordEditText.setHint(R.string.login_password);
                    mRegisterPasswordEditText.setTypeface(Typeface.DEFAULT);
                    mRegisterPasswordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    mRegisterPasswordEditText
                            .setTransformationMethod(new PasswordTransformationMethod());
                    mRegisterPasswordEditText.setOnEditorActionListener(mOnKeyboardEnterListener);
                    registerContainer.addView(mRegisterPasswordEditText);

                    mPasswordConfirmationEditText = (ConfigEdittext)
                            inflater.inflate(R.layout.config_edittext, container, false);
                    mPasswordConfirmationEditText.setHint(R.string.login_password_confirmation);
                    mPasswordConfirmationEditText.setTypeface(Typeface.DEFAULT);
                    mPasswordConfirmationEditText.setInputType(
                            InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    mPasswordConfirmationEditText
                            .setTransformationMethod(new PasswordTransformationMethod());
                    registerContainer.addView(mPasswordConfirmationEditText);

                    mMailEditText = (ConfigEdittext)
                            inflater.inflate(R.layout.config_edittext, container, false);
                    mMailEditText.setHint(R.string.account_email_label);
                    mMailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    mMailEditText.setOnEditorActionListener(mOnKeyboardEnterListener);
                    registerContainer.addView(mMailEditText);

                    if (mAuthenticatorUtils.isLoggedIn()) {
                        mRegisterUsernameEditText.setEnabled(false);
                        mRegisterPasswordEditText.setEnabled(false);
                        mPasswordConfirmationEditText.setEnabled(false);
                        mMailEditText.setEnabled(false);
                    } else {
                        mRegisterUsernameEditText.setEnabled(true);
                        mRegisterPasswordEditText.setEnabled(true);
                        mPasswordConfirmationEditText.setEnabled(true);
                        mMailEditText.setEnabled(true);
                    }

                    frameContainer = new FrameLayout(getContext());
                    frameContainer.addView(registerContainer);
                    frameParams = (FrameLayout.LayoutParams) registerContainer.getLayoutParams();
                    frameParams.gravity = Gravity.CENTER_VERTICAL;
                    container.addView(frameContainer);
                    return frameContainer;
            }
            return null;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public String getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getContext().getString(R.string.login);
                case 1:
                    return getContext().getString(R.string.register);
            }
            return "";
        }

        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

    public HatchetLoginRegisterView(Context context) {
        super(context);
        inflate(getContext(), R.layout.hatchet_login_register, this);
    }

    public HatchetLoginRegisterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(getContext(), R.layout.hatchet_login_register, this);
    }

    public void setup(AuthenticatorUtils authenticatorUtils, ProgressBar progressBar) {
        mAuthenticatorUtils = authenticatorUtils;
        mProgressBar = progressBar;

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(new LoginRegisterPagerAdapter());
        SimplePagerTabs pagerTabs =
                (SimplePagerTabs) findViewById(R.id.simplepagertabs);
        pagerTabs.setViewPager(mViewPager);
        pagerTabs.setOnPageChangeListener(mOnPageChangeListener);
        mLoginButton = (TextView) findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new LoginButtonListener());

        updateButtonTexts();
    }

    private void loginButtonClick() {
        if (mAuthenticatorUtils.isLoggedIn()) {
            mProgressBar.setVisibility(VISIBLE);
            mAuthenticatorUtils.logout();
        } else {
            switch (mViewPager.getCurrentItem()) {
                case 0:
                    attemptLogin();
                    break;
                case 1:
                    attemptRegister();
                    break;
            }
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form. If there are form
     * errors (invalid email, missing fields, etc.), the errors are presented and no actual login
     * attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mLoginUsernameEditText.setError(null);
        mLoginPasswordEditText.setError(null);

        // Store values at the time of the login attempt.
        String mEmail = mLoginUsernameEditText.getText().toString();
        String mPassword = mLoginPasswordEditText.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(mEmail)) {
            mLoginUsernameEditText.setError(getContext().getString(R.string.error_field_required));
            focusView = mLoginUsernameEditText;
            cancel = true;
        }

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mLoginPasswordEditText.setError(getContext().getString(R.string.error_field_required));
            focusView = mLoginPasswordEditText;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Tell the service to login
            mAuthenticatorUtils.login(mEmail, mPassword);
            mProgressBar.setVisibility(VISIBLE);
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form. If there are form
     * errors (invalid email, missing fields, etc.), the errors are presented and no actual login
     * attempt is made.
     */
    private void attemptRegister() {
        // Reset errors.
        mRegisterUsernameEditText.setError(null);
        mRegisterPasswordEditText.setError(null);
        mPasswordConfirmationEditText.setError(null);

        // Store values at the time of the register attempt.
        String username = mRegisterUsernameEditText.getText().toString();
        String password = mRegisterPasswordEditText.getText().toString();
        String passwordConfirmation = mPasswordConfirmationEditText.getText().toString();
        String email = null;
        if (!TextUtils.isEmpty(mMailEditText.getText().toString())) {
            email = mMailEditText.getText().toString();
        }

        boolean cancel = false;
        View focusView = null;

        // Check for a valid username
        if (TextUtils.isEmpty(username)) {
            mRegisterUsernameEditText
                    .setError(getContext().getString(R.string.error_field_required));
            focusView = mRegisterUsernameEditText;
            cancel = true;
        }

        // Check for a valid password.
        if (TextUtils.isEmpty(password)) {
            mRegisterPasswordEditText
                    .setError(getContext().getString(R.string.error_field_required));
            focusView = mRegisterPasswordEditText;
            cancel = true;
        }

        // Check for a valid password confirmation.
        if (TextUtils.isEmpty(passwordConfirmation)) {
            mPasswordConfirmationEditText.setError(getContext().getString(
                    R.string.error_field_required));
            focusView = mPasswordConfirmationEditText;
            cancel = true;
        }
        if (!password.equals(passwordConfirmation)) {
            mPasswordConfirmationEditText.setError(getContext().getString(
                    R.string.error_passwords_dont_match));
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
            mProgressBar.setVisibility(VISIBLE);
        }
    }

    /**
     * Update the texts of all buttons. Depends on whether or not the user is logged in.
     */
    public void updateButtonTexts() {
        if (mAuthenticatorUtils.isLoggedIn()) {
            mLoginButton.setTextColor(getResources().getColor(R.color.primary_textcolor_inverted));
            mLoginButton.setBackgroundResource(
                    R.drawable.selectable_background_tomahawk_red_filled);
            mLoginButton.setText(R.string.logout);
        } else {
            mLoginButton.setTextColor(getResources().getColor(R.color.tomahawk_red));
            mLoginButton.setBackgroundResource(R.drawable.selectable_background_tomahawk_red);
            switch (mViewPager.getCurrentItem()) {
                case 0:
                    mLoginButton.setText(R.string.login);
                    break;
                case 1:
                    mLoginButton.setText(R.string.register_and_login);
                    break;
            }
        }
    }

    public void onConfigTestResult(Object component, int type, String message) {
        if (mAuthenticatorUtils == component) {
            updateButtonTexts();
            mViewPager.getAdapter().notifyDataSetChanged();
            mProgressBar.setVisibility(GONE);
        }
    }
}
