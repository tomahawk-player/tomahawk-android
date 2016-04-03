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

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.ListItemString;
import org.tomahawk.libtomahawk.collection.StationPlaylist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.stations.ScriptPlaylistGenerator;
import org.tomahawk.libtomahawk.infosystem.stations.ScriptPlaylistGeneratorManager;
import org.tomahawk.libtomahawk.infosystem.stations.ScriptPlaylistGeneratorSearchResult;
import org.tomahawk.libtomahawk.utils.ViewUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.ui.widgets.ConfigEdittext;
import org.tomahawk.tomahawk_android.utils.MultiColumnClickListener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * A {@link ConfigDialog} which shows a textfield to enter a username and password, and provides
 * button for cancel/logout and ok/login, depending on whether or not the user is logged in.
 */
public class CreateStationDialog extends ConfigDialog {

    public final static String TAG = CreateStationDialog.class.getSimpleName();

    private TomahawkListAdapter mAdapter;

    private StickyListHeadersListView mListView;

    private EditText mSearchEditText;

    private Map<Track, String> mSongIds = new ConcurrentHashMap<>();

    private ClickListener mClickListener = new ClickListener();

    private class ClickListener implements MultiColumnClickListener {

        @Override
        public void onItemClick(View view, Object item) {
            if (item instanceof Artist) {
                List<Artist> artists = new ArrayList<>();
                artists.add((Artist) item);
                StationPlaylist stationPlaylist = StationPlaylist.get(artists, null, null);
                DatabaseHelper.get().storeStation(stationPlaylist);
            } else if (item instanceof Track) {
                List<Pair<Track, String>> tracks = new ArrayList<>();
                tracks.add(new Pair<>((Track) item, mSongIds.get(item)));
                StationPlaylist stationPlaylist = StationPlaylist.get(null, tracks, null);
                DatabaseHelper.get().storeStation(stationPlaylist);
            } else if (item instanceof ListItemString) {
                List<String> genres = new ArrayList<>();
                genres.add(((ListItemString) item).getText());
                StationPlaylist stationPlaylist = StationPlaylist.get(null, null, genres);
                DatabaseHelper.get().storeStation(stationPlaylist);
            }
            CreateStationDialog.this.dismiss();
        }

        @Override
        public boolean onItemLongClick(View view, Object item) {
            return false;
        }
    }

    /**
     * Called when this {@link android.support.v4.app.DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LinearLayout linearLayout = (LinearLayout) addViewToFrame(R.layout.config_create_station);
        mListView =
                (StickyListHeadersListView) linearLayout.findViewById(R.id.create_station_listview);

        mSearchEditText = (ConfigEdittext) linearLayout.findViewById(R.id.create_station_edittext);
        mSearchEditText.setOnEditorActionListener(mOnKeyboardEnterListener);

        ViewUtils.showSoftKeyboard(mSearchEditText);

        setDialogTitle(getString(R.string.create_station));

        hideStatusImage();
        hideConnectImage();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(getDialogView());
        return builder.create();
    }

    @Override
    protected void onEnabledCheckedChange(boolean checked) {
        // We don't care about this since we don't offer a checkbox in a DirectoryChooserConfigDialog
    }

    @Override
    protected void onConfigTestResult(Object component, int type, String message) {
        // We don't care about this since we don't offer a checkbox in a DirectoryChooserConfigDialog
    }

    @Override
    protected void onPositiveAction() {
        ScriptPlaylistGenerator generator =
                ScriptPlaylistGeneratorManager.get().getDefaultPlaylistGenerator();
        generator.search(mSearchEditText.getText().toString())
                .done(new DoneCallback<ScriptPlaylistGeneratorSearchResult>() {
                    @Override
                    public void onDone(ScriptPlaylistGeneratorSearchResult result) {
                        List<Segment> segments = new ArrayList<>();
                        if (result.mArtists.size() > 0) {
                            segments.add(new Segment.Builder(result.mArtists)
                                    .headerLayout(R.layout.single_line_list_header)
                                    .headerString(R.string.artists)
                                    .build());
                        }
                        if (result.mAlbums.size() > 0) {
                            segments.add(new Segment.Builder(result.mAlbums)
                                    .headerLayout(R.layout.single_line_list_header)
                                    .headerString(R.string.albums)
                                    .build());
                        }
                        if (result.mTracks.size() > 0) {
                            List<Track> tracks = new ArrayList<>();
                            for (Pair<Track, String> pair : result.mTracks) {
                                tracks.add(pair.first);
                                mSongIds.put(pair.first, pair.second);
                            }
                            segments.add(new Segment.Builder(tracks)
                                    .headerLayout(R.layout.single_line_list_header)
                                    .headerString(R.string.songs)
                                    .build());
                        }
                        if (result.mGenres.size() > 0) {
                            List<ListItemString> genres = new ArrayList<>();
                            for (String genre : result.mGenres) {
                                genres.add(new ListItemString(genre));
                            }
                            segments.add(new Segment.Builder(genres)
                                    .headerLayout(R.layout.single_line_list_header)
                                    .headerString(R.string.genres)
                                    .build());
                        }
                        if (result.mMoods.size() > 0) {
                            List<ListItemString> moods = new ArrayList<>();
                            for (String mood : result.mMoods) {
                                moods.add(new ListItemString(mood));
                            }
                            segments.add(new Segment.Builder(moods)
                                    .headerLayout(R.layout.single_line_list_header)
                                    .headerString(R.string.moods)
                                    .build());
                        }

                        if (mAdapter == null) {
                            mAdapter = new TomahawkListAdapter(
                                    (TomahawkMainActivity) getActivity(),
                                    LayoutInflater.from(getContext()), segments, mListView,
                                    mClickListener);
                        } else {
                            mAdapter.setSegments(segments, mListView);
                        }
                        mListView.setAdapter(mAdapter);
                    }
                });
    }

    @Override
    protected void onNegativeAction() {
        dismiss();
    }
}
