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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;

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
        LayoutInflater inflater = getActivity().getLayoutInflater();
        addScrollingViewToFrame(inflater.inflate(R.layout.config_ask_access, null));
        setDialogTitle(HatchetAuthenticatorUtils.HATCHET_PRETTY_NAME);
        setStatus(new HatchetStubResolver(HatchetAuthenticatorUtils.HATCHET_PRETTY_NAME, null));
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
        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        dismiss();
    }

    @Override
    protected void onNegativeAction() {
        dismiss();
    }
}
