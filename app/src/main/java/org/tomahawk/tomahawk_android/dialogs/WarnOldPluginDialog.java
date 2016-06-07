/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.TextView;

/**
 * A {@link android.support.v4.app.DialogFragment} which is being shown to the user to warn him that
 * he's using an old plugin version.
 */
public class WarnOldPluginDialog extends ConfigDialog {

    public final static String TAG = WarnOldPluginDialog.class.getSimpleName();

    private ScriptResolver mScriptResolver;

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String message = "";
        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkFragment.PREFERENCEID)) {
                String id = getArguments().getString(TomahawkFragment.PREFERENCEID);
                mScriptResolver = PipeLine.get().getResolver(id);
            }
            if (getArguments().containsKey(TomahawkFragment.MESSAGE)) {
                message = getArguments().getString(TomahawkFragment.MESSAGE);
            }
        }

        TextView textview = (TextView) addScrollingViewToFrame(R.layout.config_textview);
        textview.setText(message);
        setDialogTitle(getString(android.R.string.dialog_alert_title));
        onResolverStateUpdated(mScriptResolver);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getDialogView());
        return builder.create();
    }

    @Override
    protected void onPositiveAction() {
        ResolverRedirectConfigDialog dialog = new ResolverRedirectConfigDialog();
        Bundle args = new Bundle();
        args.putString(TomahawkFragment.PREFERENCEID, mScriptResolver.getId());
        dialog.setArguments(args);
        dialog.show(getFragmentManager(), null);
        dismiss();
    }
}
