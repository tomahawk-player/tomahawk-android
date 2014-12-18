/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.ui.widgets.BoundedLinearLayout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

/**
 * A {@link android.support.v4.app.DialogFragment} which is the base class for all config dialogs
 * (ResolverConfigDialog, RedirectConfigDialog, LoginConfigDialog)
 */
public abstract class ConfigDialog extends DialogFragment {

    public final static String TAG = ConfigDialog.class.getSimpleName();

    private View mDialogView;

    private ImageView mHeaderBackground;

    private TextView mTitleTextView;

    protected LinearLayout mScrollingDialogFrame;

    protected BoundedLinearLayout mDialogFrame;

    private TextView mPositiveButton;

    private TextView mNegativeButton;

    private ImageView mStatusImageView;

    private ImageView mConnectImageView;

    private ImageView mConnectBgImageView;

    private SmoothProgressBar mProgressBar;

    private ConfigDialogReceiver mConfigDialogReceiver;

    private boolean mResolverEnabled;

    private class ConfigDialogReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AuthenticatorManager.CONFIG_TEST_RESULT.equals(intent.getAction())) {
                String componentId = intent
                        .getStringExtra(AuthenticatorManager.CONFIG_TEST_RESULT_PLUGINNAME);
                int type = intent
                        .getIntExtra(AuthenticatorManager.CONFIG_TEST_RESULT_TYPE, 0);
                String message = intent
                        .getStringExtra(AuthenticatorManager.CONFIG_TEST_RESULT_MESSAGE);
                onConfigTestResult(componentId, type, message);
            }
        }
    }

    //So that the user can login by pressing "Enter" or something similar on his keyboard
    protected TextView.OnEditorActionListener mOnKeyboardEnterListener
            = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (event == null || actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                onPositiveAction();
            }
            return false;
        }
    };

    private View.OnClickListener mPositiveButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onPositiveAction();
        }
    };

    private View.OnClickListener mNegativeButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onNegativeAction();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        mDialogView = inflater.inflate(R.layout.config_dialog, null);
        mHeaderBackground = (ImageView) mDialogView
                .findViewById(R.id.config_dialog_header_background);
        mTitleTextView = (TextView) mDialogView
                .findViewById(R.id.config_dialog_title_textview);
        mDialogFrame = (BoundedLinearLayout) mDialogView
                .findViewById(R.id.config_dialog_frame);
        mScrollingDialogFrame = (LinearLayout) mDialogView
                .findViewById(R.id.scrolling_config_dialog_frame);
        mPositiveButton = (TextView) mDialogView
                .findViewById(R.id.config_dialog_positive_button);
        mPositiveButton.setOnClickListener(mPositiveButtonListener);
        mNegativeButton = (TextView) mDialogView
                .findViewById(R.id.config_dialog_negative_button);
        mNegativeButton.setOnClickListener(mNegativeButtonListener);
        mStatusImageView = (ImageView) mDialogView
                .findViewById(R.id.config_dialog_status_imageview);
        mConnectImageView = (ImageView) mDialogView
                .findViewById(R.id.config_dialog_connect_imageview);
        mConnectBgImageView = (ImageView) mDialogView
                .findViewById(R.id.config_dialog_connect_bg_imageview);
        mProgressBar = (SmoothProgressBar) mDialogView
                .findViewById(R.id.smoothprogressbar);

        mPositiveButton.setText(getString(R.string.ok).toUpperCase());
        mNegativeButton.setText(getString(R.string.cancel).toUpperCase());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mConfigDialogReceiver == null) {
            mConfigDialogReceiver = new ConfigDialogReceiver();
        }

        // Register intents that the BroadcastReceiver should listen to
        getActivity().registerReceiver(mConfigDialogReceiver,
                new IntentFilter(AuthenticatorManager.CONFIG_TEST_RESULT));
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mConfigDialogReceiver != null) {
            getActivity().unregisterReceiver(mConfigDialogReceiver);
            mConfigDialogReceiver = null;
        }
    }

    public View getDialogView() {
        return mDialogView;
    }

    protected void addScrollingViewToFrame(View view) {
        mScrollingDialogFrame.addView(view);
        updateContainerHeight();
    }

    protected void addViewToFrame(View view) {
        mDialogFrame.addView(view);
        mDialogFrame.setVisibility(View.VISIBLE);
        updateContainerHeight();
    }

    protected abstract void onEnabledCheckedChange(boolean checked);

    protected abstract void onConfigTestResult(String componentId, int type, String message);

    protected abstract void onPositiveAction();

    protected abstract void onNegativeAction();

    protected void hideNegativeButton() {
        mNegativeButton.setVisibility(View.GONE);
    }

    protected void hideStatusImage() {
        mStatusImageView.setVisibility(View.GONE);
    }

    protected void setConnectImageViewClickable() {
        mConnectBgImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEnabledCheckedChange(!mResolverEnabled);
            }
        });
        mConnectBgImageView.setVisibility(View.VISIBLE);
    }

    protected void setStatus(Resolver resolver) {
        mResolverEnabled = resolver.isEnabled();
        resolver.loadIconWhite(mStatusImageView);
        resolver.loadIconBackground(mHeaderBackground, false);
        int resId = resolver.isEnabled() ? R.drawable.ic_connected : R.drawable.ic_connect;
        TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), mConnectImageView, resId);
        int bgResId = resolver.isEnabled() ? R.drawable.selectable_background_button_green
                : R.drawable.selectable_background_button_white;
        mConnectBgImageView.setImageResource(bgResId);
    }

    protected void setDialogTitle(String title) {
        if (mTitleTextView != null) {
            mTitleTextView.setText(title);
        }
    }

    protected void setPositiveButtonText(int stringResId) {
        if (mPositiveButton != null) {
            String string = getString(stringResId).toUpperCase();
            mPositiveButton.setText(string);
        }
    }

    protected void setNegativeButtonText(int stringResId) {
        if (mNegativeButton != null) {
            String string = getString(stringResId).toUpperCase();
            mNegativeButton.setText(string);
        }
    }

    protected void showSoftKeyboard(final EditText editText) {
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, final boolean hasFocus) {
                editText.post(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() != null) {
                            InputMethodManager imm = (InputMethodManager) getActivity()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }
                });
                editText.setOnFocusChangeListener(null);
            }
        });
        editText.requestFocus();
    }

    /**
     * Start the loading animation. Called when beginning login process.
     */
    protected void startLoadingAnimation() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Stop the loading animation. Called when login/logout process has finished.
     */
    protected void stopLoadingAnimation() {
        mProgressBar.setVisibility(View.GONE);
    }

    private void updateContainerHeight() {
        final int panelHeight = getResources().getDimensionPixelSize(R.dimen.row_height_verylarge);

        TomahawkUtils.afterViewGlobalLayout(new TomahawkUtils.ViewRunnable(getDialogView()) {
            @Override
            public void run() {
                setContainerHeight(getDialogView().getHeight() - panelHeight * 2);
            }
        });
    }

    private void setContainerHeight(final int height) {
        TomahawkUtils.afterViewGlobalLayout(new TomahawkUtils.ViewRunnable(getDialogView()) {
            @Override
            public void run() {
                mDialogFrame.setMaxHeight(height);
            }
        });
    }
}
