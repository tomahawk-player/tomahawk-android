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
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.libtomahawk.utils.ImageUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigCheckbox;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.greenrobot.event.EventBus;
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

    private LinearLayout mScrollingDialogFrame;

    private LinearLayout mDialogFrame;

    private TextView mPositiveButton;

    private TextView mNegativeButton;

    private ImageView mStatusImageView;

    private ConfigCheckbox mEnableCheckbox;

    private ImageView mRemoveButton;

    protected SmoothProgressBar mProgressBar;

    private boolean mResolverEnabled;

    //So that the user can login by pressing "Enter" or something similar on his keyboard
    protected final TextView.OnEditorActionListener mOnKeyboardEnterListener
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

    private final View.OnClickListener mPositiveButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onPositiveAction();
        }
    };

    private final View.OnClickListener mNegativeButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onNegativeAction();
        }
    };

    @SuppressWarnings("unused")
    public void onEventMainThread(AuthenticatorManager.ConfigTestResultEvent event) {
        onConfigTestResult(event.mComponent, event.mType, event.mMessage);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        mDialogView = inflater.inflate(R.layout.config_dialog, null);
        mHeaderBackground = (ImageView) mDialogView
                .findViewById(R.id.config_dialog_header_background);
        mTitleTextView = (TextView) mDialogView
                .findViewById(R.id.config_dialog_title_textview);
        mDialogFrame = (LinearLayout) mDialogView
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
        mEnableCheckbox = (ConfigCheckbox) mDialogView
                .findViewById(R.id.config_dialog_enable_checkbox);
        mProgressBar = (SmoothProgressBar) mDialogView
                .findViewById(R.id.smoothprogressbar);
        mRemoveButton = (ImageView) mDialogView
                .findViewById(R.id.config_dialog_remove_button);

        mPositiveButton.setText(getString(R.string.ok).toUpperCase());
        mNegativeButton.setText(getString(R.string.cancel).toUpperCase());
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);

        super.onStop();
    }

    public View getDialogView() {
        return mDialogView;
    }

    protected void addScrollingViewToFrame(View view) {
        mScrollingDialogFrame.addView(view);
    }

    protected View addScrollingViewToFrame(int layoutId) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(layoutId, mScrollingDialogFrame, false);
        mScrollingDialogFrame.addView(view);
        return view;
    }

    protected View addViewToFrame(int layoutId) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(layoutId, mScrollingDialogFrame, false);
        mDialogFrame.addView(view);
        mDialogFrame.setVisibility(View.VISIBLE);
        return view;
    }

    protected abstract void onEnabledCheckedChange(boolean checked);

    protected abstract void onConfigTestResult(Object component, int type, String message);

    protected abstract void onPositiveAction();

    protected abstract void onNegativeAction();

    protected void onRemoveAction() {
    }

    protected void hideNegativeButton() {
        mNegativeButton.setVisibility(View.GONE);
    }

    protected void hideStatusImage() {
        mStatusImageView.setVisibility(View.GONE);
    }

    protected void hideConnectImage() {
        mEnableCheckbox.setVisibility(View.GONE);
    }

    protected void showRemoveButton() {
        ImageUtils.setTint(mRemoveButton.getDrawable(), R.color.tomahawk_red);
        mRemoveButton.setVisibility(View.VISIBLE);
        mRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRemoveAction();
            }
        });
    }

    protected void setConnectImageViewClickable() {
        mEnableCheckbox.setChecked(mResolverEnabled);
        mEnableCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onEnabledCheckedChange(isChecked);
            }
        });
        mEnableCheckbox.setEnabled(true);
    }

    protected void setStatus(Resolver resolver) {
        mResolverEnabled = resolver.isEnabled();
        if (!(resolver instanceof ScriptResolver) ||
                ((ScriptResolver) resolver).getScriptAccount().getMetaData()
                        .manifest.iconBackground != null) {
            resolver.loadIconBackground(mHeaderBackground, false);
        }
        if (!(resolver instanceof ScriptResolver) ||
                ((ScriptResolver) resolver).getScriptAccount().getMetaData()
                        .manifest.iconWhite != null) {
            resolver.loadIconWhite(mStatusImageView);
        } else {
            resolver.loadIcon(mStatusImageView, false);
        }
        mEnableCheckbox.setChecked(resolver.isEnabled());
    }

    protected void setStatusImage(int drawableResId) {
        mStatusImageView.setImageResource(drawableResId);
    }

    protected void setStatusImageClickListener(View.OnClickListener clickListener) {
        mStatusImageView.setOnClickListener(clickListener);
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

    /**
     * Start the loading animation. Called when beginning login process.
     */
    public void startLoadingAnimation() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Stop the loading animation. Called when login/logout process has finished.
     */
    public void stopLoadingAnimation() {
        mProgressBar.setVisibility(View.GONE);
    }
}
