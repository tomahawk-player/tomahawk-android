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

import org.tomahawk.libtomahawk.collection.CustomPlaylist;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.database.UserPlaylistsDataSource;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 20.02.13
 */

public class PlaylistDialog extends DialogFragment {

    Playlist mPlaylist;

    public PlaylistDialog() {
        setRetainInstance(true);
    }

    public PlaylistDialog(Playlist playlist) {
        setRetainInstance(true);
        mPlaylist = playlist;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.playlist_dialog, null);
        EditText editText = (EditText) view.findViewById(R.id.playlist_dialog_name_textview);
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        editText.setSingleLine(true);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event == null || actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    hideSoftKeyboard();
                    savePlaylist();
                    dismiss();
                }
                return false;
            }
        });
        TextView textView = (TextView) view.findViewById(R.id.playlist_dialog_title_textview);
        DialogInterface.OnClickListener onPositiveButtonClickedListener
                = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                hideSoftKeyboard();
                savePlaylist();
            }
        };
        builder.setPositiveButton(R.string.ok, onPositiveButtonClickedListener);
        if (mPlaylist != null) {
            textView.setText(R.string.playbackactivity_save_playlist_dialog_title);
        } else {
            textView.setText(R.string.playbackactivity_create_playlist_dialog_title);
        }
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                hideSoftKeyboard();
                getDialog().cancel();
            }
        });
        builder.setView(view);
        return builder.create();
    }

    private void savePlaylist() {
        EditText editText = (EditText) getDialog().findViewById(R.id.playlist_dialog_name_textview);
        String playlistName = TextUtils.isEmpty(editText.getText().toString()) ? getString(
                R.string.playbackplaylistfragment_title_string) : editText.getText().toString();
        UserPlaylistsDataSource userPlaylistsDataSource = new UserPlaylistsDataSource(getActivity(),
                ((TomahawkApp) getActivity().getApplication()).getPipeLine());
        userPlaylistsDataSource.open();
        if (mPlaylist != null) {
            userPlaylistsDataSource.storeUserPlaylist(-1, playlistName, mPlaylist);
        } else {
            userPlaylistsDataSource.storeUserPlaylist(-1, playlistName,
                    CustomPlaylist.fromTrackList(playlistName, new ArrayList<Track>(), -1));
        }
        userPlaylistsDataSource.close();
        ((UserCollection) ((TomahawkApp) getActivity().getApplication()).getSourceList()
                .getCollectionFromId(UserCollection.Id)).updateUserPlaylists();
    }

    private void hideSoftKeyboard() {
        EditText editText = (EditText) getDialog().findViewById(R.id.playlist_dialog_name_textview);
        InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }
}
