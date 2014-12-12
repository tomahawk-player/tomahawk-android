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
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.ui.widgets.BoundedLinearLayout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A {@link android.support.v4.app.DialogFragment} which is the base class for all config dialogs
 * (ResolverConfigDialog, RedirectConfigDialog, LoginConfigDialog)
 */
public abstract class ConfigDialog extends DialogFragment {

    public final static String TAG = ConfigDialog.class.getSimpleName();

    private View mDialogView;

    private View mShowKeyboardView;

    private CheckBox mEnabledCheckbox;

    private TextView mTitleTextView;

    protected LinearLayout mScrollingDialogFrame;

    protected BoundedLinearLayout mDialogFrame;

    private TextView mPositiveButton;

    private TextView mNegativeButton;

    private Drawable mProgressDrawable;

    private ImageView mStatusImageView;

    private int mStatusImageResId;

    private String mStatusImagePath;

    private ConfigDialogReceiver mConfigDialogReceiver;

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

    //Used to handle the animation of our progress animation, while we try to login
    private Handler mAnimationHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_ANIMATION:
                    mProgressDrawable.setLevel(mProgressDrawable.getLevel() + 500);
                    mStatusImageView.setImageDrawable(mProgressDrawable);
                    mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
                    mAnimationHandler.sendEmptyMessageDelayed(MSG_UPDATE_ANIMATION, 50);
                    break;
            }
            return true;
        }
    });

    private static final int MSG_UPDATE_ANIMATION = 0x20;

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

    private CompoundButton.OnCheckedChangeListener mEnabledCheckboxListener
            = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            onEnabledCheckedChange(isChecked);
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
        mEnabledCheckbox = (CheckBox) mDialogView
                .findViewById(R.id.config_dialog_enable_checkbox);
        mEnabledCheckbox.setOnCheckedChangeListener(mEnabledCheckboxListener);
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
        mProgressDrawable = getResources()
                .getDrawable(R.drawable.tomahawk_progress_indeterminate_circular_holo_light);
        mStatusImageView = (ImageView) mDialogView
                .findViewById(R.id.config_dialog_status_imageview);
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

        mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
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

    protected void hideEnabledCheckbox() {
        mEnabledCheckbox.setVisibility(View.GONE);
    }

    protected void hideStatusImage() {
        mStatusImageView.setVisibility(View.GONE);
    }

    protected void setStatusImage(int statusImageResId, boolean enabled) {
        mStatusImageResId = statusImageResId;
        TomahawkUtils.loadDrawableIntoImageView(getActivity(), mStatusImageView,
                statusImageResId, !enabled);
    }

    protected void setStatusImage(String path, boolean enabled) {
        mStatusImagePath = path;
        TomahawkUtils.loadDrawableIntoImageView(getActivity(), mStatusImageView,
                mStatusImagePath, !enabled);
    }

    protected void setDialogTitle(String title) {
        if (mTitleTextView != null) {
            mTitleTextView.setText(title);
        }
    }

    protected void setPositiveButtonText(int stringResId) {
        if (mPositiveButton != null) {
            mPositiveButton.setText(stringResId);
        }
    }

    protected void setNegativeButtonText(int stringResId) {
        if (mNegativeButton != null) {
            mNegativeButton.setText(stringResId);
        }
    }

    protected void setEnabledCheckboxState(boolean state) {
        if (mEnabledCheckbox != null) {
            mEnabledCheckbox.setChecked(state);
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
        mAnimationHandler.sendEmptyMessageDelayed(MSG_UPDATE_ANIMATION, 50);
    }

    /**
     * Stop the loading animation. Called when login/logout process has finished.
     *
     * @param loggedIn determines whether or not the status image drawable will be greyed out.
     */
    protected void stopLoadingAnimation(boolean loggedIn) {
        mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
        if (mStatusImagePath != null) {
            TomahawkUtils.loadDrawableIntoImageView(getActivity(), mStatusImageView,
                    mStatusImagePath, !loggedIn);
        } else {
            TomahawkUtils.loadDrawableIntoImageView(getActivity(), mStatusImageView,
                    mStatusImageResId, !loggedIn);
        }
    }

    private void updateContainerHeight() {
        final int buttonPanelHeight =
                getResources().getDimensionPixelSize(R.dimen.row_height_large);
        final int topPanelheight =
                getResources().getDimensionPixelSize(R.dimen.row_height_verylarge);
        final int dividerThick =
                getResources().getDimensionPixelSize(R.dimen.divider_height_thick);
        if (getDialogView().getHeight() > 0) {
            // we subtract 1 here because of the divider line which is 1px thick
            setContainerHeight(getDialogView().getHeight() - buttonPanelHeight - topPanelheight
                    - dividerThick - 1);
        } else {
            getDialogView().getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            // we subtract 1 here because of the divider line which is 1px thick
                            setContainerHeight(getDialogView().getHeight() - buttonPanelHeight
                                    - topPanelheight - dividerThick - 1);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                getDialogView().getViewTreeObserver()
                                        .removeOnGlobalLayoutListener(this);
                            } else {
                                getDialogView().getViewTreeObserver()
                                        .removeGlobalOnLayoutListener(this);
                            }
                        }
                    });
        }
    }

    private void setContainerHeight(final int height) {
        if (mDialogFrame.getHeight() > 0) {
            mDialogFrame.setMaxHeight(height);
        } else {
            mDialogFrame.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            mDialogFrame.setMaxHeight(height);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                getDialogView().getViewTreeObserver()
                                        .removeOnGlobalLayoutListener(this);
                            } else {
                                getDialogView().getViewTreeObserver()
                                        .removeGlobalOnLayoutListener(this);
                            }
                        }
                    });
        }
    }
}
