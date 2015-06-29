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
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.AlphaComparator;
import org.tomahawk.libtomahawk.collection.ArtistAlphaComparator;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.HatchetCollection;
import org.tomahawk.libtomahawk.collection.LastModifiedComparator;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.QueryComparator;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;

import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * {@link TomahawkFragment} which shows a set of {@link Album}s inside its {@link
 * se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class AlbumsFragment extends TomahawkFragment {

    public static final String COLLECTION_ALBUMS_SPINNER_POSITION
            = "org.tomahawk.tomahawk_android.collection_albums_spinner_position";

    @Override
    public void onResume() {
        super.onResume();

        if (getArguments() != null) {
            if (getArguments().containsKey(SHOW_MODE)) {
                mShowMode = getArguments().getInt(SHOW_MODE);
            }
        }
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
    public void onItemClick(View view, final Object item) {
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        if (item instanceof Query) {
            PlaylistEntry entry = getListAdapter().getPlaylistEntry(item);
            if (entry.getQuery().isPlayable()) {
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
        } else if (item instanceof Album) {
            mCollection.hasAlbumTracks((Album) item).done(new DoneCallback<Boolean>() {
                @Override
                public void onDone(Boolean result) {
                    Bundle bundle = new Bundle();
                    bundle.putString(TomahawkFragment.ALBUM, ((Album) item).getCacheKey());
                    if (result) {
                        bundle.putString(TomahawkFragment.COLLECTION_ID, mCollection.getId());
                    } else {
                        bundle.putString(TomahawkFragment.COLLECTION_ID,
                                TomahawkApp.PLUGINNAME_HATCHET);
                    }
                    bundle.putInt(CONTENT_HEADER_MODE,
                            ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                    FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                            TracksFragment.class, bundle);
                }
            });
        }
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    @Override
    protected void updateAdapter() {
        if (!mIsResumed) {
            return;
        }

        if (mArtist != null) {
            if (!TomahawkApp.PLUGINNAME_HATCHET.equals(mCollection.getId())) {
                mCollection.getArtistAlbums(mArtist).done(new DoneCallback<List<Album>>() {
                    @Override
                    public void onDone(List<Album> result) {
                        fillAdapter(new Segment(
                                        mCollection.getName() + " " + getString(R.string.albums),
                                        sortAlbums(result), R.integer.grid_column_count,
                                        R.dimen.padding_superlarge, R.dimen.padding_superlarge),
                                mCollection);
                    }
                });
            } else {
                mCollection.getArtistAlbums(mArtist).done(new DoneCallback<List<Album>>() {
                    @Override
                    public void onDone(List<Album> result) {
                        final List<Segment> segments = new ArrayList<>();
                        Segment segment = new Segment(R.string.top_albums, sortAlbums(result),
                                R.integer.grid_column_count, R.dimen.padding_superlarge,
                                R.dimen.padding_superlarge);
                        segments.add(segment);
                        fillAdapter(segment);
                        ((HatchetCollection) mCollection).getArtistTopHits(mArtist).done(
                                new DoneCallback<List<Query>>() {
                                    @Override
                                    public void onDone(List<Query> result) {
                                        Collections.sort(result, new QueryComparator(
                                                QueryComparator.COMPARE_ALBUMPOS));
                                        Segment segment = new Segment(R.string.top_hits, result);
                                        segment.setShowNumeration(true, 1);
                                        segment.setHideArtistName(true);
                                        segment.setShowDuration(true);
                                        segments.add(segment);
                                        fillAdapter(segments);
                                    }
                                });
                    }
                });
            }
        } else if (mAlbumArray != null) {
            fillAdapter(new Segment(mAlbumArray));
        } else if (mUser != null) {
            fillAdapter(new Segment(getDropdownPos(COLLECTION_ALBUMS_SPINNER_POSITION),
                    constructDropdownItems(),
                    constructDropdownListener(COLLECTION_ALBUMS_SPINNER_POSITION),
                    sortAlbums(mUser.getStarredAlbums()), R.integer.grid_column_count,
                    R.dimen.padding_superlarge, R.dimen.padding_superlarge));
        } else {
            final List<Album> starredAlbums;
            if (mCollection.getId().equals(TomahawkApp.PLUGINNAME_USERCOLLECTION)) {
                starredAlbums = DatabaseHelper.getInstance().getStarredAlbums();
            } else {
                starredAlbums = null;
            }
            mCollection.getAlbums().done(new DoneCallback<Set<Album>>() {
                @Override
                public void onDone(Set<Album> result) {
                    if (starredAlbums != null) {
                        result.addAll(starredAlbums);
                    }
                    fillAdapter(new Segment(getDropdownPos(COLLECTION_ALBUMS_SPINNER_POSITION),
                            constructDropdownItems(),
                            constructDropdownListener(COLLECTION_ALBUMS_SPINNER_POSITION),
                            sortAlbums(result), R.integer.grid_column_count,
                            R.dimen.padding_superlarge, R.dimen.padding_superlarge), mCollection);
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

    private List<Album> sortAlbums(java.util.Collection<Album> albums) {
        List<Album> sortedAlbums;
        if (albums instanceof List) {
            sortedAlbums = (List<Album>) albums;
        } else {
            sortedAlbums = new ArrayList<>(albums);
        }
        switch (getDropdownPos(COLLECTION_ALBUMS_SPINNER_POSITION)) {
            case 0:
                UserCollection userColl = (UserCollection) CollectionManager.getInstance()
                        .getCollection(TomahawkApp.PLUGINNAME_USERCOLLECTION);
                Collections.sort(sortedAlbums,
                        new LastModifiedComparator<>(userColl.getAlbumTimeStamps()));
                break;
            case 1:
                Collections.sort(sortedAlbums, new AlphaComparator());
                break;
            case 2:
                Collections.sort(sortedAlbums, new ArtistAlphaComparator());
                break;
        }
        return sortedAlbums;
    }
}
