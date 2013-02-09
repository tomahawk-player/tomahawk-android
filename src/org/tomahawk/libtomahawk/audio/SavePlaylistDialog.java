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
package org.tomahawk.libtomahawk.audio;

import org.tomahawk.libtomahawk.UserCollection;
import org.tomahawk.libtomahawk.database.UserPlaylistsDataSource;
import org.tomahawk.libtomahawk.playlist.Playlist;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.EditText;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com>
 * Date: 20.02.13
 */

public class SavePlaylistDialog extends DialogFragment {
    Playlist mPlaylist;

    public SavePlaylistDialog(Playlist playlist) {
        setRetainInstance(true);
        mPlaylist = playlist;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.save_playlist_dialog, null)).setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText editText = (EditText) getDialog().findViewById(R.id.save_playlist_dialog_name_textview);
                        UserPlaylistsDataSource userPlaylistsDataSource = new UserPlaylistsDataSource(getActivity(),
                                ((TomahawkApp) getActivity().getApplication()).getPipeLine());
                        userPlaylistsDataSource.open();
                        userPlaylistsDataSource.storeUserPlaylist(-1, TextUtils.isEmpty(editText.getText().toString())
                                ? getString(R.string.playlistsfragment_title_string) : editText.getText().toString(),
                                mPlaylist);
                        ((UserCollection) ((TomahawkApp) getActivity().getApplication()).getSourceList().getCollectionFromId(
                                UserCollection.Id)).updateUserPlaylists();
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                getDialog().cancel();
            }
        });
        return builder.create();
    }
}
