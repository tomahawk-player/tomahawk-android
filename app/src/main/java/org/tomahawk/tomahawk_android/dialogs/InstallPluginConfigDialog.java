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

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.ScriptAccount;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverMetaData;
import org.tomahawk.libtomahawk.utils.GsonHelper;
import org.tomahawk.libtomahawk.utils.VariousUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.UnzipUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class InstallPluginConfigDialog extends ConfigDialog {

    public final static String TAG = InstallPluginConfigDialog.class.getSimpleName();

    public static final String PATH_TO_AXE_URI_STRING = "path_to_axe_uri_string";

    private Uri mPathToAxe;

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Check if there is a data key in the provided arguments
        if (getArguments() != null && getArguments().containsKey(PATH_TO_AXE_URI_STRING)) {
            mPathToAxe = Uri.parse(getArguments().getString(PATH_TO_AXE_URI_STRING));
        }

        addScrollingViewToFrame(R.layout.config_install_plugin);
        setDialogTitle(getString(R.string.install_plugin_title));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getDialogView());
        return builder.create();
    }

    @Override
    protected void onPositiveAction() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String destDirPath = TomahawkApp.getContext().getFilesDir().getAbsolutePath()
                        + File.separator + "manualresolvers" + File.separator + ".temp";
                File destDir = new File(destDirPath);
                try {
                    VariousUtils.deleteRecursive(destDir);
                } catch (FileNotFoundException e) {
                    Log.d(TAG,
                            "onPositiveAction: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
                try {
                    if (UnzipUtils.unzip(mPathToAxe, destDirPath)) {
                        File metadataFile = new File(destDirPath + File.separator + "content"
                                + File.separator + "metadata.json");
                        String metadataString = FileUtils.readFileToString(metadataFile,
                                Charsets.UTF_8);
                        ScriptResolverMetaData metaData = GsonHelper.get().fromJson(metadataString,
                                ScriptResolverMetaData.class);
                        final File renamedFile = new File(
                                destDir.getParent() + File.separator + metaData.pluginName
                                        + "_" + System.currentTimeMillis());
                        boolean success = destDir.renameTo(renamedFile);
                        if (!success) {
                            Log.e(TAG, "onPositiveAction - Wasn't able to rename directory: "
                                    + renamedFile.getAbsolutePath());
                        }
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                PipeLine.get().addScriptAccount(
                                        new ScriptAccount(renamedFile.getPath(), true));
                            }
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG,
                            "onPositiveAction: " + e.getClass() + ": " + e.getLocalizedMessage());
                }
            }
        }).start();
        dismiss();
    }
}
