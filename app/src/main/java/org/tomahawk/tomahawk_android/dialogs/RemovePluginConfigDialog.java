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

import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.libtomahawk.utils.VariousUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;

public class RemovePluginConfigDialog extends ConfigDialog {

    public final static String TAG = RemovePluginConfigDialog.class.getSimpleName();

    private ScriptResolver mScriptResolver;

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null && getArguments().containsKey(TomahawkFragment.PREFERENCEID)) {
            String resolverId = getArguments().getString(
                    TomahawkFragment.PREFERENCEID);
            mScriptResolver = PipeLine.get().getResolver(resolverId);
        }

        TextView headerTextView = (TextView) addScrollingViewToFrame(R.layout.config_textview);
        headerTextView.setText(R.string.uninstall_plugin_warning);
        setDialogTitle(getString(R.string.uninstall_plugin_title));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getDialogView());
        return builder.create();
    }

    @Override
    protected void onPositiveAction() {
        File destDir =
                new File(mScriptResolver.getScriptAccount().getPath().replaceFirst("file:", ""));
        try {
            VariousUtils.deleteRecursive(destDir);
            mScriptResolver.getScriptAccount().unregisterAllPlugins();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "onPositiveAction: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        PipeLine.get().removeResolver(mScriptResolver);
        dismiss();
    }
}
