/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
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
import org.tomahawk.libtomahawk.collection.AlphaComparator;
import org.tomahawk.libtomahawk.collection.ArtistAlphaComparator;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.CollectionUtils;
import org.tomahawk.libtomahawk.collection.LastModifiedComparator;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.QueryComparator;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.views.FancyDropDown;

import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * {@link TomahawkFragment} which shows a set of {@link Track}s inside its {@link
 * se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class TracksFragment extends TomahawkFragment {

    public static final String COLLECTION_TRACKS_SPINNER_POSITION
            = "org.tomahawk.tomahawk_android.collection_tracks_spinner_position";

    @SuppressWarnings("unused")
    public void onEventMainThread(CollectionManager.UpdatedEvent event) {
        super.onEventMainThread(event);

        if (event.mUpdatedItemIds != null && event.mUpdatedItemIds.contains(mAlbum.getCacheKey())) {
            showAlbumFancyDropDown();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mContainerFragmentClass == null) {
            getActivity().setTitle("");
        }
        updateAdapter();
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param view the clicked view
     * @param item the Object which corresponds to the click
     */
    @Override
    public void onItemClick(View view, Object item) {
        if (item instanceof Query) {
            PlaylistEntry entry = getListAdapter().getPlaylistEntry(item);
            if (entry.getQuery().isPlayable()) {
                TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
                PlaybackService playbackService = activity.getPlaybackService();
                if (playbackService != null) {
                    if (playbackService.getCurrentEntry() == entry) {
                        playbackService.playPause();
                    } else {
                        playbackService.setPlaylist(getListAdapter().getPlaylist(), entry);
                        playbackService.start();
                    }
                }
            }
        }
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter}
     * content
     */
    @Override
    protected void updateAdapter() {
        if (!mIsResumed) {
            return;
        }

        if (mAlbum != null) {
            showContentHeader(mAlbum);
            showAlbumFancyDropDown();
            mCollection.getAlbumTracks(mAlbum).done(new DoneCallback<List<Query>>() {
                @Override
                public void onDone(List<Query> queries) {
                    Collections.sort(queries,
                            new QueryComparator(QueryComparator.COMPARE_ALBUMPOS));
                    Segment segment = new Segment(mAlbum.getArtist().getPrettyName(), queries);
                    if (CollectionUtils.allFromOneArtist(queries)) {
                        segment.setHideArtistName(true);
                        segment.setShowDuration(true);
                    }
                    segment.setShowNumeration(true, 1);
                    fillAdapter(segment);
                }
            });
        } else if (mQuery != null) {
            ArrayList<Object> queries = new ArrayList<>();
            queries.add(mQuery);
            Segment segment = new Segment(queries);
            segment.setShowDuration(true);
            fillAdapter(segment);
            showContentHeader(mQuery);
            showFancyDropDown(0, mQuery.getName(), null, null);
        } else if (mQueryArray != null) {
            Segment segment = new Segment(mQueryArray);
            segment.setShowDuration(true);
            fillAdapter(segment);
        } else {
            mCollection.getQueries().done(new DoneCallback<Set<Query>>() {
                @Override
                public void onDone(Set<Query> queries) {
                    fillAdapter(new Segment(getDropdownPos(COLLECTION_TRACKS_SPINNER_POSITION),
                            constructDropdownItems(),
                            constructDropdownListener(COLLECTION_TRACKS_SPINNER_POSITION),
                            sortQueries(queries)));
                }
            });
        }
    }

    private List<Integer> constructDropdownItems() {
        List<Integer> dropDownItems = new ArrayList<>();
        dropDownItems.add(R.string.collection_dropdown_recently_added);
        dropDownItems.add(R.string.collection_dropdown_alpha);
        dropDownItems.add(R.string.collection_dropdown_alpha_artists);
        return dropDownItems;
    }

    private List<Query> sortQueries(java.util.Collection<Query> queries) {
        List<Query> sortedQueries;
        if (queries instanceof List) {
            sortedQueries = (List<Query>) queries;
        } else {
            sortedQueries = new ArrayList<>(queries);
        }
        switch (getDropdownPos(COLLECTION_TRACKS_SPINNER_POSITION)) {
            case 0:
                UserCollection userColl = (UserCollection) CollectionManager.getInstance()
                        .getCollection(TomahawkApp.PLUGINNAME_USERCOLLECTION);
                Collections.sort(sortedQueries,
                        new LastModifiedComparator<>(userColl.getQueryTimeStamps()));
                break;
            case 1:
                Collections.sort(sortedQueries, new AlphaComparator());
                break;
            case 2:
                Collections.sort(sortedQueries, new ArtistAlphaComparator());
                break;
        }
        return sortedQueries;
    }

    private void showAlbumFancyDropDown() {
        if (mAlbum != null) {
            CollectionManager.getInstance().getAvailableCollections(mAlbum).done(
                    new DoneCallback<List<Collection>>() {
                        @Override
                        public void onDone(final List<Collection> result) {
                            int initialSelection = 0;
                            for (int i = 0; i < result.size(); i++) {
                                if (result.get(i) == mCollection) {
                                    initialSelection = i;
                                    break;
                                }
                            }
                            showFancyDropDown(mAlbum.getName(), initialSelection,
                                    FancyDropDown.convertToDropDownItemInfo(result),
                                    new FancyDropDown.DropDownListener() {
                                        @Override
                                        public void onDropDownItemSelected(int position) {
                                            mCollection = result.get(position);
                                            updateAdapter();
                                        }

                                        @Override
                                        public void onCancel() {
                                        }
                                    });
                        }
                    });
        }
    }
}
