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

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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

    private ImageView mRemoveButton;

    protected SmoothProgressBar mProgressBar;

    private Resolver mResolver;

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
        onResolverStateUpdated(mResolver);
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

    protected void onConfigTestResult(Object component, int type, String message) {
    }

    protected abstract void onPositiveAction();

    private void onNegativeAction() {
        dismiss();
    }

    protected void hideNegativeButton() {
        mNegativeButton.setVisibility(View.GONE);
    }

    protected void onResolverStateUpdated(Resolver resolver) {
        mResolver = resolver;
        if (!(resolver instanceof ScriptResolver) ||
                ((ScriptResolver) resolver).getScriptAccount().getMetaData()
                        .manifest.iconBackground != null) {
            resolver.loadIconBackground(mHeaderBackground, !resolver.isEnabled());
        } else {
            int color;
            if (resolver.isEnabled()) {
                color = android.R.color.black;
            } else {
                color = R.color.disabled_resolver;
            }
            mHeaderBackground.setImageDrawable(new ColorDrawable(getResources().getColor(color)));
        }
        if (!(resolver instanceof ScriptResolver) ||
                ((ScriptResolver) resolver).getScriptAccount().getMetaData()
                        .manifest.iconWhite != null) {
            resolver.loadIconWhite(mStatusImageView, 0);
        } else {
            resolver.loadIcon(mStatusImageView, false);
        }

        View button = getDialogView().findViewById(R.id.config_enable_button);
        if (button != null) {
            ImageView buttonImage =
                    (ImageView) button.findViewById(R.id.config_enable_button_image);
            TextView buttonText = (TextView) button.findViewById(R.id.config_enable_button_text);
            if (resolver.isEnabled()) {
                button.setBackgroundResource(R.drawable.selectable_background_tomahawk_red_filled);
                resolver.loadIconWhite(buttonImage, 0);
                buttonText.setText(R.string.resolver_config_enable_button_disable);
                buttonText.setTextColor(
                        getResources().getColor(R.color.primary_textcolor_inverted));
            } else {
                button.setBackgroundResource(R.drawable.selectable_background_tomahawk_red);
                resolver.loadIconWhite(buttonImage, R.color.tomahawk_red);
                buttonText.setText(R.string.resolver_config_enable_button_enable);
                buttonText.setTextColor(getResources().getColor(R.color.tomahawk_red));
            }
        }
    }

    protected void showEnableButton(View.OnClickListener onClickListener) {
        View button = addScrollingViewToFrame(R.layout.config_enable_button);
        button.setOnClickListener(onClickListener);
    }

    protected void showRemoveButton(View.OnClickListener onClickListener) {
        ImageUtils.setTint(mRemoveButton.getDrawable(), R.color.tomahawk_red);
        mRemoveButton.setVisibility(View.VISIBLE);
        mRemoveButton.setOnClickListener(onClickListener);
    }

    protected void setStatusImage(int drawableResId) {
        mStatusImageView.setImageResource(drawableResId);
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
