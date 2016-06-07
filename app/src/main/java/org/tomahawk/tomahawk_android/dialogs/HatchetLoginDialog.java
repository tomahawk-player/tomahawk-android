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

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.resolver.HatchetStubResolver;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.views.HatchetLoginRegisterView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * A {@link org.tomahawk.tomahawk_android.dialogs.ConfigDialog} which shows a textfield to enter a
 * username and password, and provides button for cancel/logout and ok/login, depending on whether
 * or not the user is logged in.
 */
public class HatchetLoginDialog extends ConfigDialog {

    public final static String TAG = HatchetLoginDialog.class.getSimpleName();

    private AuthenticatorUtils mAuthenticatorUtils;

    private HatchetLoginRegisterView mHatchetLoginRegisterView;

    /**
     * Called when this {@link DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null && getArguments()
                .containsKey(TomahawkFragment.PREFERENCEID)) {
            String authenticatorId = getArguments().getString(
                    TomahawkFragment.PREFERENCEID);
            mAuthenticatorUtils = AuthenticatorManager.get().getAuthenticatorUtils(
                    authenticatorId);
        }

        TextView headerTextView = (TextView) addScrollingViewToFrame(R.layout.config_textview);
        headerTextView.setText(mAuthenticatorUtils.getDescription());
        mHatchetLoginRegisterView = (HatchetLoginRegisterView) addScrollingViewToFrame(
                R.layout.config_hatchetloginregister);
        mHatchetLoginRegisterView.setup(mAuthenticatorUtils, mProgressBar);

        setDialogTitle(mAuthenticatorUtils.getPrettyName());
        if (TomahawkApp.PLUGINNAME_HATCHET.equals(mAuthenticatorUtils.getId())) {
            onResolverStateUpdated(HatchetStubResolver.get());
        } else {
            onResolverStateUpdated(PipeLine.get().getResolver(mAuthenticatorUtils.getId()));
        }
        hideNegativeButton();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getDialogView());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        return alertDialog;
    }

    @Override
    protected void onConfigTestResult(Object component, int type, String message) {
        mHatchetLoginRegisterView.onConfigTestResult(component, type, message);
    }

    @Override
    protected void onPositiveAction() {
        dismiss();
    }
}
