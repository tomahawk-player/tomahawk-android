/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.fragments;

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.collection.ListItemDrawable;
import org.tomahawk.libtomahawk.collection.StationPlaylist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.dialogs.CreateStationDialog;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class StationsFragment extends TomahawkFragment {

    private static final String TAG = StationsFragment.class.getSimpleName();

    @SuppressWarnings("unused")
    public void onEventAsync(DatabaseHelper.PlaylistsUpdatedEvent event) {
        scheduleUpdateAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playlistsfragment_layout, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mContainerFragmentClass == null) {
            getActivity().setTitle(getString(R.string.drawer_title_stations).toUpperCase());
            if (getView() != null) {
                View newButton = getView().findViewById(R.id.create_new_button);
                newButton.setVisibility(View.VISIBLE);
                newButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCreateDialog();
                    }
                });
            }
        }
        updateAdapter();
    }

    public void showCreateDialog() {
        CreateStationDialog dialog = new CreateStationDialog();
        dialog.show(getFragmentManager(), null);
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param view the clicked view
     * @param item the Object which corresponds to the click
     */
    @Override
    public void onItemClick(View view, Object item) {
        if (getMediaController() == null) {
            Log.e(TAG, "onItemClick failed because getMediaController() is null");
            return;
        }
        if (item instanceof StationPlaylist) {
            if (item != getPlaybackManager().getPlaylist()) {
                getPlaybackManager().setPlaylist((StationPlaylist) item);
                getMediaController().getTransportControls().play();
            }
        } else if (item instanceof ListItemDrawable) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("http://the.echonest.com/"));
            startActivity(i);
        }
    }

    /**
     * Called every time an item inside a ListView or GridView is long-clicked
     *
     * @param item the Object which corresponds to the long-click
     */
    @Override
    public boolean onItemLongClick(View view, Object item) {
        return FragmentUtils.showContextMenu((TomahawkMainActivity) getActivity(), item, null,
                false, mHideRemoveButton);
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    @Override
    protected void updateAdapter() {
        if (!mIsResumed) {
            return;
        }

        User.getSelf().done(new DoneCallback<User>() {
            @Override
            public void onDone(User user) {
                final List<Segment> segments = new ArrayList<>();
                List myStations = new ArrayList();
                myStations.addAll(DatabaseHelper.get().getStations());
                segments.add(new Segment.Builder(myStations)
                        .showAsGrid(R.integer.grid_column_count, R.dimen.padding_superlarge,
                                R.dimen.padding_superlarge)
                        .headerLayout(R.layout.single_line_list_header)
                        .headerString(R.string.my_stations)
                        .build());
                segments.add(new Segment.Builder(getSuggestedStations())
                        .showAsGrid(R.integer.grid_column_count, R.dimen.padding_superlarge,
                                R.dimen.padding_superlarge)
                        .headerLayout(R.layout.single_line_list_header)
                        .headerString(R.string.suggested_stations)
                        .build());
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        fillAdapter(segments);
                        showContentHeader(R.drawable.stations_header);
                        if (getView() != null) {
                            View newButton = getView().findViewById(R.id.create_new_button);
                            int y = mHeaderNonscrollableHeight
                                    - getResources().getDimensionPixelSize(
                                    R.dimen.row_height_medium)
                                    - getResources().getDimensionPixelSize(R.dimen.padding_small);
                            newButton.setY(y);
                        }
                    }
                });
            }
        });
    }

    private List getSuggestedStations() {
        List suggestedStations = new ArrayList();
        String[] suggestedGenres = new String[]{"acoustic", "alternative", "club", "dance",
                "drum-and-bass", "dubstep", "electronic", "funk", "goth", "grunge", "happy",
                "hard-rock", "heavy-metal", "hip-hop", "house", "indie", "j-pop", "jazz", "k-pop",
                "metal", "party", "pop", "punk-rock", "r-n-b", "reggae", "rock-n-roll", "romance",
                "singer-songwriter", "soul", "techno", "trance", "trip-hop", "world-music"};
        for (String suggestedGenre : suggestedGenres) {
            ArrayList<String> genres = new ArrayList<>();
            genres.add(suggestedGenre);
            suggestedStations.add(StationPlaylist.get(null, null, genres));
        }
        return suggestedStations;
    }

}
