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

import com.rdio.android.api.OAuth1WebViewActivity;

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.utils.RdioMediaPlayer;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;

/**
 * A {@link android.support.v4.app.DialogFragment} which redirects the user to an external login
 * activity.
 */
public class RedirectConfigDialog extends DialogFragment {

    public final static String TAG = RedirectConfigDialog.class.getName();

    private String mResolverId;

    private TextView mButtonText;

    private View.OnClickListener mPositiveButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            getDialog().cancel();
        }
    };

    private View.OnClickListener mNegativeButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            getDialog().cancel();
        }
    };

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null && getArguments()
                .containsKey(TomahawkFragment.TOMAHAWK_AUTHENTICATORID_KEY)) {
            mResolverId = getArguments().getString(TomahawkFragment.TOMAHAWK_AUTHENTICATORID_KEY);
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.resolver_config_dialog, null);
        LinearLayout frame = (LinearLayout) view.findViewById(R.id.resolver_config_dialog_frame);
        FrameLayout buttonLayout = (FrameLayout) inflater
                .inflate(R.layout.resolver_config_redirect_button, null);
        frame.addView(buttonLayout);
        ScriptResolver scriptResolver = (ScriptResolver) PipeLine.getInstance()
                .getResolver(mResolverId);
        if (scriptResolver != null) {
            Account account = new Account("Rdio-Account",
                    TomahawkApp.getContext().getString(R.string.accounttype_string));
            AccountManager am = AccountManager.get(TomahawkApp.getContext());
            boolean rdioLoggedIn = false;
            if (am != null && am.getUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN) != null
                    && am.getUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN_SECRET) != null) {
                rdioLoggedIn = true;
            }
            LinearLayout button = ((LinearLayout) buttonLayout
                    .findViewById(R.id.resolver_config_redirect_button));
            mButtonText = (TextView) button.findViewById(R.id.resolver_config_redirect_button_text);
            mButtonText.setText(rdioLoggedIn
                    ? getString(R.string.resolver_config_redirect_button_text_log_out_of)
                    : getString(R.string.resolver_config_redirect_button_text_log_into));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Account account = new Account("Rdio-Account",
                            TomahawkApp.getContext().getString(R.string.accounttype_string));
                    AccountManager am = AccountManager.get(TomahawkApp.getContext());
                    boolean rdioLoggedIn = false;
                    if (am != null
                            && am.getUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN) != null
                            && am.getUserData(account, OAuth1WebViewActivity.EXTRA_TOKEN_SECRET)
                            != null) {
                        rdioLoggedIn = true;
                    }
                    if (rdioLoggedIn) {
                        am.removeAccount(account, null, null);
                        mButtonText.setText(
                                getString(R.string.resolver_config_redirect_button_text_log_into));
                        ((ScriptResolver) PipeLine.getInstance().getResolver(mResolverId))
                                .setEnabled(false);
                    } else {
                        try {
                            Intent myIntent = new Intent(TomahawkApp.getContext(),
                                    OAuth1WebViewActivity.class);
                            myIntent.putExtra(OAuth1WebViewActivity.EXTRA_CONSUMER_KEY,
                                    new String(Base64.decode(RdioMediaPlayer.RDIO_APPKEY,
                                            Base64.DEFAULT), "UTF-8")
                            );
                            myIntent.putExtra(OAuth1WebViewActivity.EXTRA_CONSUMER_SECRET,
                                    new String(Base64.decode(RdioMediaPlayer.RDIO_APPKEYSECRET,
                                            Base64.DEFAULT), "UTF-8")
                            );
                            startActivityForResult(myIntent, 1);
                        } catch (UnsupportedEncodingException e) {
                            Log.e(TAG,
                                    "onCreateDialog: " + e.getClass() + ": " + e
                                            .getLocalizedMessage()
                            );
                        }
                    }
                }
            });
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
        negativeButton.setOnClickListener(mNegativeButtonListener);
        ImageView statusImageView = (ImageView) view
                .findViewById(R.id.resolver_config_dialog_status_imageview);
        TomahawkUtils.loadResolverIconIntoImageView(TomahawkApp.getContext(), statusImageView,
                scriptResolver, !scriptResolver.isEnabled());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        return builder.create();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Rdio access token is served and yummy");
                if (data != null) {
                    String accessToken = data.getStringExtra(OAuth1WebViewActivity.EXTRA_TOKEN);
                    String accessTokenSecret =
                            data.getStringExtra(OAuth1WebViewActivity.EXTRA_TOKEN_SECRET);
                    RdioMediaPlayer.getInstance().onRdioAuthorised(accessToken, accessTokenSecret);
                    RdioMediaPlayer.getInstance().getRdio().setTokenAndSecret(accessToken,
                            accessTokenSecret);
                    mButtonText.setText(
                            getString(R.string.resolver_config_redirect_button_text_log_out_of));
                    ((ScriptResolver) PipeLine.getInstance().getResolver(mResolverId))
                            .setEnabled(true);
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (data != null) {
                    String errorCode = data.getStringExtra(OAuth1WebViewActivity.EXTRA_ERROR_CODE);
                    String errorDescription = data
                            .getStringExtra(OAuth1WebViewActivity.EXTRA_ERROR_DESCRIPTION);
                    Log.e(TAG, "ERROR: " + errorCode + " - " + errorDescription);
                }
            }
            RdioMediaPlayer.getInstance().getRdio().prepareForPlayback();
        }
    }
}
