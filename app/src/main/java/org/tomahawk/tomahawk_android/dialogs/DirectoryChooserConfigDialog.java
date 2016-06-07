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

import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.resolver.UserCollectionStubResolver;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.views.DirectoryChooser;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;

/**
 * A {@link org.tomahawk.tomahawk_android.dialogs.ConfigDialog} which shows a textfield to enter a
 * username and password, and provides button for cancel/logout and ok/login, depending on whether
 * or not the user is logged in.
 */
public class DirectoryChooserConfigDialog extends ConfigDialog {

    public final static String TAG = DirectoryChooserConfigDialog.class.getSimpleName();

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DirectoryChooser directoryChooser =
                (DirectoryChooser) addViewToFrame(R.layout.config_directorychooser);
        directoryChooser.setup();

        setDialogTitle(getString(R.string.local_collection_pretty_name));
        onResolverStateUpdated(UserCollectionStubResolver.get());
        setPositiveButtonText(R.string.rescan);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getDialogView());
        return builder.create();
    }

    @Override
    protected void onPositiveAction() {
        CollectionManager.get().getUserCollection().loadMediaItems(true);
        dismiss();
    }
}
