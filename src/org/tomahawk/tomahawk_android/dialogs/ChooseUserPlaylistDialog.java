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

import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.database.UserPlaylistsDataSource;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.TomahawkContextMenuAdapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * A {@link DialogFragment} which shows a list of all {@link UserPlaylist}s to choose from.
 */
public class ChooseUserPlaylistDialog extends DialogFragment {

    private UserCollection mUserCollection;

    private ArrayList<Query> mQueries;

    private int mCustomPlaylistCount;

    /**
     * Construct a {@link ChooseUserPlaylistDialog}
     *
     * @param userCollection a reference to the {@link UserCollection}
     * @param queries        an {@link ArrayList} of {@link org.tomahawk.libtomahawk.resolver.Query}s
     *                       in case we want to add them to a {@link UserPlaylist}, which the user
     *                       chooses through this {@link ChooseUserPlaylistDialog}
     */
    public ChooseUserPlaylistDialog(UserCollection userCollection, ArrayList<Query> queries) {
        setRetainInstance(true);
        mUserCollection = userCollection;
        mQueries = queries;
    }

    /**
     * Called when this {@link DialogFragment} is being created
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.choose_playlist_dialog, null);
        ListView listView = (ListView) view.findViewById(R.id.playlist_dialog_playlists_listview);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UserPlaylistsDataSource userPlaylistsDataSource = ((TomahawkApp) getActivity()
                        .getApplication()).getUserPlaylistsDataSource();
                userPlaylistsDataSource.addQueriesToUserPlaylist(
                        mUserCollection.getLocalUserPlaylists().get(position).getId(), mQueries);
                ((UserCollection) ((TomahawkApp) getActivity().getApplication()).getSourceList()
                        .getCollectionFromId(UserCollection.Id)).updateUserPlaylists();
                getDialog().dismiss();
            }
        });
        mCustomPlaylistCount = mUserCollection.getLocalUserPlaylists().size();
        String[] playlistNames = new String[mCustomPlaylistCount];
        for (int i = 0; i < mCustomPlaylistCount; i++) {
            playlistNames[i] = mUserCollection.getLocalUserPlaylists().get(i).getName();
        }
        listView.setAdapter(
                new TomahawkContextMenuAdapter(getActivity().getLayoutInflater(), playlistNames));
        LinearLayout linearLayout = (LinearLayout) view
                .findViewById(R.id.playlist_dialog_addplaylist_layout);
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CreateUserPlaylistDialog(
                        UserPlaylist.fromQueryList(TomahawkApp.getLifetimeUniqueStringId(), "",
                                mQueries))
                        .show(getFragmentManager(),
                                getString(R.string.playbackactivity_create_playlist_dialog_title));
                getDialog().dismiss();
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        return builder.create();
    }
}
