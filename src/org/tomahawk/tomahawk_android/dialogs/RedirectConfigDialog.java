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
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A {@link android.support.v4.app.DialogFragment} which redirects the user to an external login
 * activity.
 */
public class RedirectConfigDialog extends DialogFragment {

    public final static String TAG = RedirectConfigDialog.class.getSimpleName();

    private String mResolverId;

    private TextView mButtonText;

    private RedirectConfigDialogReceiver mRedirectConfigDialogReceiver;

    private class RedirectConfigDialogReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AuthenticatorManager.AUTHENTICATOR_LOGGED_IN.equals(intent.getAction())) {
                final boolean loggedIn = intent
                        .getBooleanExtra(AuthenticatorManager.AUTHENTICATOR_LOGGED_IN_STATE, false);
                String correspondingResolverId = intent
                        .getStringExtra(AuthenticatorManager.AUTHENTICATOR_LOGGED_IN_RESOLVERID);
                if (mResolverId.equals(correspondingResolverId)) {
                    mButtonText.setText(loggedIn
                            ? getString(R.string.resolver_config_redirect_button_text_log_out_of)
                            : getString(R.string.resolver_config_redirect_button_text_log_into));
                }
            }
        }
    }

    private View.OnClickListener mPositiveButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            getDialog().cancel();
        }
    };

    /**
     * Initialize
     */
    @Override
    public void onResume() {
        super.onResume();

        AuthenticatorUtils utils = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(mResolverId);
        if (utils != null) {
            boolean loggedIn = utils.isLoggedIn();
            mButtonText.setText(loggedIn
                    ? getString(R.string.resolver_config_redirect_button_text_log_out_of)
                    : getString(R.string.resolver_config_redirect_button_text_log_into));
        }

        if (mRedirectConfigDialogReceiver == null) {
            mRedirectConfigDialogReceiver = new RedirectConfigDialogReceiver();
        }

        // Register intents that the BroadcastReceiver should listen to
        getActivity().registerReceiver(mRedirectConfigDialogReceiver,
                new IntentFilter(AuthenticatorManager.AUTHENTICATOR_LOGGED_IN));
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mRedirectConfigDialogReceiver != null) {
            getActivity().unregisterReceiver(mRedirectConfigDialogReceiver);
            mRedirectConfigDialogReceiver = null;
        }
    }

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null && getArguments()
                .containsKey(TomahawkFragment.TOMAHAWK_PREFERENCEID_KEY)) {
            mResolverId = getArguments().getString(TomahawkFragment.TOMAHAWK_PREFERENCEID_KEY);
        }
        int buttonBackgroundResId;
        int buttonImageResId;
        int buttonTextColor;
        View.OnClickListener onClickListener;
        if (mResolverId.equals(TomahawkApp.PLUGINNAME_RDIO)) {
            buttonBackgroundResId = R.drawable.selectable_background_tomahawk_opaque;
            buttonImageResId = R.drawable.logo_rdio;
            buttonTextColor = getResources().getColor(R.color.primary_textcolor);
            onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AuthenticatorUtils authenticatorUtils = AuthenticatorManager.getInstance()
                            .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_RDIO);
                    boolean rdioLoggedIn = authenticatorUtils.isLoggedIn();
                    if (rdioLoggedIn) {
                        authenticatorUtils.logout();
                    } else {
                        authenticatorUtils.login(getActivity(), null, null);
                    }
                }
            };
        } else {
            buttonBackgroundResId = R.drawable.selectable_background_tomahawk_opaque_inverted;
            buttonImageResId = R.drawable.logo_deezer;
            buttonTextColor = getResources().getColor(R.color.primary_textcolor_inverted);
            onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AuthenticatorUtils authenticatorUtils = AuthenticatorManager.getInstance()
                            .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_DEEZER);
                    boolean deezerLoggedIn = authenticatorUtils.isLoggedIn();
                    if (deezerLoggedIn) {
                        authenticatorUtils.logout(getActivity());
                    } else {
                        authenticatorUtils.login(getActivity(), null, null);
                    }
                }
            };
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.resolver_config_dialog, null);
        LinearLayout frame = (LinearLayout) view.findViewById(R.id.resolver_config_dialog_frame);
        FrameLayout buttonLayout = (FrameLayout) inflater
                .inflate(R.layout.resolver_config_redirect_button, null);
        frame.addView(buttonLayout);
        ScriptResolver scriptResolver = (ScriptResolver) PipeLine.getInstance()
                .getResolver(mResolverId);
        AuthenticatorUtils utils = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(mResolverId);
        if (utils != null) {
            boolean loggedIn = utils.isLoggedIn();
            LinearLayout button = ((LinearLayout) buttonLayout
                    .findViewById(R.id.resolver_config_redirect_button));
            button.setBackgroundResource(buttonBackgroundResId);
            ImageView buttonImage = (ImageView) buttonLayout
                    .findViewById(R.id.resolver_config_redirect_button_image);
            buttonImage.setImageResource(buttonImageResId);
            mButtonText = (TextView) button.findViewById(R.id.resolver_config_redirect_button_text);
            mButtonText.setTextColor(buttonTextColor);
            mButtonText.setText(loggedIn
                    ? getString(R.string.resolver_config_redirect_button_text_log_out_of)
                    : getString(R.string.resolver_config_redirect_button_text_log_into));
            button.setOnClickListener(onClickListener);
            TextView textView = (TextView) view
                    .findViewById(R.id.resolver_config_dialog_title_textview);
            textView.setText(scriptResolver.getName());
            CheckBox checkBox = (CheckBox) view
                    .findViewById(R.id.resolver_config_dialog_enable_checkbox);
            checkBox.setVisibility(View.GONE);
        }

        TextView positiveButton = (TextView) view
                .findViewById(R.id.resolver_config_dialog_ok_button);
        positiveButton.setOnClickListener(mPositiveButtonListener);
        TextView negativeButton = (TextView) view
                .findViewById(R.id.resolver_config_dialog_cancel_button);
        negativeButton.setVisibility(View.GONE);
        ImageView statusImageView = (ImageView) view
                .findViewById(R.id.resolver_config_dialog_status_imageview);
        TomahawkUtils.loadResolverIconIntoImageView(TomahawkApp.getContext(), statusImageView,
                scriptResolver, !scriptResolver.isEnabled());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        return builder.create();
    }
}
