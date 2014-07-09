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

import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigEdittext;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * A {@link DialogFragment} which is presented for the user so that he can choose a name for the
 * {@link UserPlaylist} he intends to create
 */
public class CreateUserPlaylistDialog extends ConfigDialog {

    private Playlist mUserPlaylist;

    private EditText mNameEditText;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Check if there is a playlist key in the provided arguments
        if (getArguments() != null && getArguments()
                .containsKey(TomahawkFragment.TOMAHAWK_USERPLAYLIST_KEY)) {
            mUserPlaylist = UserPlaylist.getUserPlaylistById(
                    getArguments().getString(TomahawkFragment.TOMAHAWK_USERPLAYLIST_KEY));
            if (mUserPlaylist == null) {
                dismiss();
            }
        }

        //set the proper flags for our edittext
        LayoutInflater inflater = getActivity().getLayoutInflater();
        LinearLayout textLayout = (LinearLayout) inflater.inflate(R.layout.config_text, null);
        mNameEditText = (ConfigEdittext) textLayout.findViewById(R.id.config_edittext);
        mNameEditText.setHint(R.string.playbackactivity_playlist_dialog_name_hint);
        mNameEditText.setOnEditorActionListener(mOnKeyboardEnterListener);
        addViewToFrame(textLayout);

        showSoftKeyboard(mNameEditText);

        //Set the textview's text to the proper title
        setDialogTitle(getString(R.string.playbackactivity_save_playlist_dialog_title));

        hideEnabledCheckbox();
        hideStatusImage();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getDialogView());
        return builder.create();
    }

    /**
     * Persist a {@link org.tomahawk.libtomahawk.collection.Playlist} as a {@link
     * org.tomahawk.libtomahawk.collection.UserPlaylist} in our database
     */
    private void savePlaylist() {
        String playlistName = TextUtils.isEmpty(mNameEditText.getText().toString())
                ? getString(R.string.playbackplaylistfragment_title_string)
                : mNameEditText.getText().toString();
        if (mUserPlaylist != null) {
            DatabaseHelper.getInstance().storeUserPlaylist(UserPlaylist
                    .fromQueryList(TomahawkMainActivity.getLifetimeUniqueStringId(), playlistName,
                            mUserPlaylist.getQueries()));
        }
    }

    @Override
    protected void onEnabledCheckedChange(boolean checked) {
    }

    @Override
    protected void onConfigTestResult(String componentId, int type, String message) {
    }

    @Override
    protected void onPositiveAction() {
        savePlaylist();
        dismiss();
    }

    @Override
    protected void onNegativeAction() {
        dismiss();
    }
}
