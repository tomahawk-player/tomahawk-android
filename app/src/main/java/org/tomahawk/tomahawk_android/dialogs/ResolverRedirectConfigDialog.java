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
package org.tomahawk.tomahawk_android.dialogs;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.libtomahawk.utils.VariousUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.mediaplayers.DeezerMediaPlayer;
import org.tomahawk.tomahawk_android.mediaplayers.SpotifyMediaPlayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ResolverRedirectConfigDialog extends ConfigDialog {

    public final static String TAG = ResolverRedirectConfigDialog.class.getSimpleName();

    private ScriptResolver mScriptResolver;

    private TextView mRedirectButtonTextView;

    private class RedirectButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            startLoadingAnimation();
            if (isPluginInstalled()) {
                if (mScriptResolver.isEnabled()) {
                    mScriptResolver.logout();
                } else {
                    mScriptResolver.login();
                }
            } else {
                String url = null;
                boolean isPlayStoreInstalled = VariousUtils.isPlayStoreInstalled();
                switch (mScriptResolver.getId()) {
                    case TomahawkApp.PLUGINNAME_SPOTIFY:
                        if (isPlayStoreInstalled) {
                            url = "market://details?id=" + SpotifyMediaPlayer.PACKAGE_NAME;
                        } else {
                            url = SpotifyMediaPlayer.getPluginDownloadLink();
                        }
                        break;
                    case TomahawkApp.PLUGINNAME_DEEZER:
                        if (isPlayStoreInstalled) {
                            url = "market://details?id=" + DeezerMediaPlayer.PACKAGE_NAME;
                        } else {
                            url = DeezerMediaPlayer.getPluginDownloadLink();
                        }
                        break;
                }
                if (url != null) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }
            }
        }
    }

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null && getArguments().containsKey(TomahawkFragment.PREFERENCEID)) {
            String id = getArguments().getString(TomahawkFragment.PREFERENCEID);
            mScriptResolver = (ScriptResolver) PipeLine.get().getResolver(id);
        }

        TextView headerTextView = (TextView) addScrollingViewToFrame(R.layout.config_textview);
        headerTextView.setText(mScriptResolver.getDescription());

        int buttonBackgroundResId = R.drawable.selectable_background_tomahawk_rectangle_gray;
        int buttonTextColor = getResources().getColor(R.color.primary_textcolor);
        View.OnClickListener onClickListener = new RedirectButtonListener();

        View buttonLayout = addScrollingViewToFrame(R.layout.config_redirect_button);
        LinearLayout button = ((LinearLayout) buttonLayout
                .findViewById(R.id.config_redirect_button));
        button.setBackgroundResource(buttonBackgroundResId);
        ImageView buttonImage = (ImageView) buttonLayout
                .findViewById(R.id.config_redirect_button_image);
        mScriptResolver.loadIcon(buttonImage, false);
        mRedirectButtonTextView = (TextView) button
                .findViewById(R.id.config_redirect_button_text);
        mRedirectButtonTextView.setTextColor(buttonTextColor);

        if (isPluginInstalled()) {
            mRedirectButtonTextView.setText(mScriptResolver.isEnabled()
                    ? getString(R.string.resolver_config_redirect_button_text_log_out_of)
                    : getString(R.string.resolver_config_redirect_button_text_log_into));
        } else {
            mRedirectButtonTextView.setText(
                    getString(R.string.resolver_config_redirect_button_text_download_plugin));
        }

        button.setOnClickListener(onClickListener);
        setDialogTitle(mScriptResolver.getName());
        hideNegativeButton();
        setStatus(mScriptResolver);
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

        if (mScriptResolver != null) {
            if (isPluginInstalled()) {
                mRedirectButtonTextView.setText(mScriptResolver.isEnabled()
                        ? getString(R.string.resolver_config_redirect_button_text_log_out_of)
                        : getString(R.string.resolver_config_redirect_button_text_log_into));
            } else {
                mRedirectButtonTextView.setText(
                        getString(R.string.resolver_config_redirect_button_text_download_plugin));
            }
        }
    }

    @Override
    protected void onEnabledCheckedChange(boolean checked) {
        // We don't care about this since we don't offer a checkbox
    }

    @Override
    protected void onConfigTestResult(Object component, int type, String message) {
        if (mScriptResolver == component && mScriptResolver.isConfigTestable()) {
            if (type == AuthenticatorManager.CONFIG_TEST_RESULT_TYPE_SUCCESS) {
                mRedirectButtonTextView.setText(
                        getString(R.string.resolver_config_redirect_button_text_log_out_of));
            } else {
                mRedirectButtonTextView.setText(
                        getString(R.string.resolver_config_redirect_button_text_log_into));
            }
            stopLoadingAnimation();
        }
    }

    @Override
    protected void onPositiveAction() {
        getDialog().cancel();
    }

    @Override
    protected void onNegativeAction() {
    }

    private boolean isPluginInstalled() {
        String pluginPackageName = "";
        int pluginMinVersionCode = 0;
        switch (mScriptResolver.getId()) {
            case TomahawkApp.PLUGINNAME_SPOTIFY:
                pluginPackageName = SpotifyMediaPlayer.PACKAGE_NAME;
                pluginMinVersionCode = SpotifyMediaPlayer.MIN_VERSION;
                break;
            case TomahawkApp.PLUGINNAME_DEEZER:
                pluginPackageName = DeezerMediaPlayer.PACKAGE_NAME;
                pluginMinVersionCode = DeezerMediaPlayer.MIN_VERSION;
                break;
        }
        try {
            PackageInfo info = getActivity().getPackageManager()
                    .getPackageInfo(pluginPackageName, PackageManager.GET_SERVICES);
            // Remove the first digit that identifies the architecture type
            String versionCodeString = String.valueOf(info.versionCode);
            versionCodeString = versionCodeString.substring(1, versionCodeString.length());
            int versionCode = Integer.valueOf(versionCodeString);
            if (versionCode >= pluginMinVersionCode) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return false;
    }
}
