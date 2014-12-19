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
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A {@link android.support.v4.app.DialogFragment} which redirects the user to an external login
 * activity.
 */
public class RedirectConfigDialog extends ConfigDialog {

    public final static String TAG = RedirectConfigDialog.class.getSimpleName();

    private String mResolverId;

    private TextView mRedirectButtonTextView;

    private class RedirectButtonListener implements View.OnClickListener {

        String mPluginName;

        public RedirectButtonListener(String pluginName) {
            mPluginName = pluginName;
        }

        @Override
        public void onClick(View v) {
            AuthenticatorUtils authenticatorUtils = AuthenticatorManager.getInstance()
                    .getAuthenticatorUtils(mPluginName);
            boolean rdioLoggedIn = authenticatorUtils.isLoggedIn();
            if (rdioLoggedIn) {
                authenticatorUtils.logout(getActivity());
            } else {
                authenticatorUtils.login(getActivity(), null, null);
            }
        }
    }

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null && getArguments()
                .containsKey(TomahawkFragment.TOMAHAWK_PREFERENCEID_KEY)) {
            mResolverId = getArguments().getString(TomahawkFragment.TOMAHAWK_PREFERENCEID_KEY);
        }

        ScriptResolver scriptResolver = (ScriptResolver) PipeLine.getInstance()
                .getResolver(mResolverId);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        LinearLayout headerTextLayout =
                (LinearLayout) inflater.inflate(R.layout.config_textview, null);
        TextView headerTextView = (TextView) headerTextLayout.findViewById(R.id.config_textview);
        headerTextView.setText(scriptResolver.getDescription());
        addScrollingViewToFrame(headerTextLayout);

        int buttonBackgroundResId;
        int buttonImageResId;
        int buttonTextColor;
        View.OnClickListener onClickListener;
        if (mResolverId.equals(TomahawkApp.PLUGINNAME_RDIO)) {
            buttonBackgroundResId = R.drawable.selectable_background_tomahawk;
            buttonImageResId = R.drawable.logo_rdio;
            buttonTextColor = getResources().getColor(R.color.primary_textcolor);
            onClickListener = new RedirectButtonListener(TomahawkApp.PLUGINNAME_RDIO);
        } else {
            buttonBackgroundResId = R.drawable.selectable_background_deezer_button;
            buttonImageResId = R.drawable.logo_deezer;
            buttonTextColor = getResources().getColor(R.color.primary_textcolor_inverted);
            onClickListener = new RedirectButtonListener(TomahawkApp.PLUGINNAME_DEEZER);
        }

        View buttonLayout = inflater.inflate(R.layout.config_redirect_button, null);
        addScrollingViewToFrame(buttonLayout);
        AuthenticatorUtils utils = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(mResolverId);
        boolean loggedIn = utils.isLoggedIn();
        LinearLayout button = ((LinearLayout) buttonLayout
                .findViewById(R.id.config_redirect_button));
        button.setBackgroundResource(buttonBackgroundResId);
        ImageView buttonImage = (ImageView) buttonLayout
                .findViewById(R.id.config_redirect_button_image);
        buttonImage.setImageResource(buttonImageResId);
        mRedirectButtonTextView = (TextView) button
                .findViewById(R.id.config_redirect_button_text);
        mRedirectButtonTextView.setTextColor(buttonTextColor);
        mRedirectButtonTextView.setText(loggedIn
                ? getString(R.string.resolver_config_redirect_button_text_log_out_of)
                : getString(R.string.resolver_config_redirect_button_text_log_into));
        button.setOnClickListener(onClickListener);
        setDialogTitle(scriptResolver.getName());
        hideNegativeButton();
        setStatus(scriptResolver);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getDialogView());
        return builder.create();
    }

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
            mRedirectButtonTextView.setText(loggedIn
                    ? getString(R.string.resolver_config_redirect_button_text_log_out_of)
                    : getString(R.string.resolver_config_redirect_button_text_log_into));
        }
    }

    @Override
    protected void onEnabledCheckedChange(boolean checked) {
        // We don't care about this since we don't offer a checkbox in a LoginConfigDialog
    }

    @Override
    protected void onConfigTestResult(String componentId, int type, String message) {
        if (mResolverId.equals(componentId)) {
            if (type == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS) {
                mRedirectButtonTextView.setText(
                        getString(R.string.resolver_config_redirect_button_text_log_out_of));
            } else if (type == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_LOGOUT) {
                mRedirectButtonTextView.setText(
                        getString(R.string.resolver_config_redirect_button_text_log_into));
            }
        }
    }

    @Override
    protected void onPositiveAction() {
        getDialog().cancel();
    }

    @Override
    protected void onNegativeAction() {
        // We don't have a negative button
    }
}
