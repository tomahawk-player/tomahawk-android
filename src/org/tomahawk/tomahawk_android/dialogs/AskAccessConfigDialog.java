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

import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.resolver.HatchetStubResolver;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.widget.Toast;

/**
 * A {@link android.support.v4.app.DialogFragment} which is being shown to the user to ask him to
 * give us the notification listener permission, so that we can also scrobble to hatchet when music
 * is being played in other apps.
 */
public class AskAccessConfigDialog extends ConfigDialog {

    public final static String TAG = AskAccessConfigDialog.class.getSimpleName();

    public static final String ASKED_FOR_ACCESS = "org.tomahawk.tomahawk_android.asked_for_access";

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        addScrollingViewToFrame(R.layout.config_ask_access);
        setDialogTitle(HatchetAuthenticatorUtils.HATCHET_PRETTY_NAME);
        setStatus(HatchetStubResolver.get());
        hideConnectImage();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getDialogView());
        return builder.create();
    }

    @Override
    protected void onEnabledCheckedChange(boolean checked) {
    }

    @Override
    protected void onConfigTestResult(Object component, int type, String message) {
    }

    @Override
    protected void onPositiveAction() {
        try {
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException e) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(TomahawkApp.getContext(),
                            R.string.notification_settings_activity_not_found, Toast.LENGTH_LONG)
                            .show();
                }
            });
        }
        dismiss();
    }

    @Override
    protected void onNegativeAction() {
        dismiss();
    }
}
