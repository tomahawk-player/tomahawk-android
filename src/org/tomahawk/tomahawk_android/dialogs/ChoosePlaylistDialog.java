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
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkContextMenuAdapter;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DialogFragment} which shows a list of all {@link org.tomahawk.libtomahawk.collection.Playlist}s
 * to choose from.
 */
public class ChoosePlaylistDialog extends DialogFragment {

    /**
     * Called when this {@link DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.choose_playlist_dialog, null);

        // Get all query keys from this fragment's arguments and through get the actual queries.
        final ArrayList<Query> queries = new ArrayList<Query>();
        if (getArguments() != null && getArguments()
                .containsKey(TomahawkFragment.TOMAHAWK_QUERYARRAY_KEY)) {
            ArrayList<String> queryKeys = getArguments()
                    .getStringArrayList(TomahawkFragment.TOMAHAWK_QUERYARRAY_KEY);
            if (queryKeys != null) {
                for (String queryKey : queryKeys) {
                    queries.add(Query.getQueryByKey(queryKey));
                }
            }
        }

        final ArrayList<Playlist> playlists = CollectionManager.getInstance().getPlaylists();

        ListView listView = (ListView) view.findViewById(R.id.playlist_dialog_playlists_listview);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<PlaylistEntry> entries = new ArrayList<PlaylistEntry>();
                Playlist playlist = playlists.get(position);
                for (Query query : queries) {
                    entries.add(PlaylistEntry.get(playlist.getId(), query,
                            TomahawkMainActivity.getLifetimeUniqueStringId()));
                }
                CollectionManager.getInstance().addPlaylistEntries(playlist.getId(), entries);
                getDialog().dismiss();
            }
        });
        List<String> playlistNames = new ArrayList<String>();
        for (Playlist playlist : playlists) {
            playlistNames.add(playlist.getName());
        }
        listView.setAdapter(
                new TomahawkContextMenuAdapter(getActivity().getLayoutInflater(), playlistNames));
        LinearLayout linearLayout = (LinearLayout) view
                .findViewById(R.id.playlist_dialog_addplaylist_layout);
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Playlist playlist = Playlist.fromQueryList("", queries);
                CreatePlaylistDialog dialog = new CreatePlaylistDialog();
                Bundle args = new Bundle();
                args.putString(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY, playlist.getId());
                dialog.setArguments(args);
                dialog.show(getFragmentManager(), null);
                getDialog().dismiss();
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        return builder.create();
    }
}
