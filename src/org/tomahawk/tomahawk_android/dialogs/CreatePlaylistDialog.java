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
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigEdittext;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * A {@link DialogFragment} which is presented for the user so that he can choose a name for the
 * {@link org.tomahawk.libtomahawk.collection.Playlist} he intends to create
 */
public class CreatePlaylistDialog extends ConfigDialog {

    private Playlist mPlaylist;

    private EditText mNameEditText;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Check if there is a playlist key in the provided arguments
        if (getArguments() != null && getArguments()
                .containsKey(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY)) {
            mPlaylist = Playlist.getPlaylistById(
                    getArguments().getString(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY));
            if (mPlaylist == null) {
                dismiss();
            }
        }

        //set the proper flags for our edittext
        LayoutInflater inflater = getActivity().getLayoutInflater();
        LinearLayout textLayout = (LinearLayout) inflater.inflate(R.layout.config_text, null);
        mNameEditText = (ConfigEdittext) textLayout.findViewById(R.id.config_edittext);
        mNameEditText.setHint(R.string.name_playlist);
        mNameEditText.setOnEditorActionListener(mOnKeyboardEnterListener);
        addScrollingViewToFrame(textLayout);

        showSoftKeyboard(mNameEditText);

        //Set the textview's text to the proper title
        setDialogTitle(getString(R.string.save_playlist));

        hideEnabledCheckbox();
        hideStatusImage();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getDialogView());
        return builder.create();
    }

    /**
     * Persist a {@link org.tomahawk.libtomahawk.collection.Playlist} as a {@link
     * org.tomahawk.libtomahawk.collection.Playlist} in our database
     */
    private void savePlaylist() {
        String playlistName = TextUtils.isEmpty(mNameEditText.getText().toString())
                ? getString(R.string.playlist)
                : mNameEditText.getText().toString();
        if (mPlaylist != null) {
            CollectionManager.getInstance().createPlaylist(Playlist.fromQueryList(playlistName,
                    mPlaylist.getQueries()));
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
