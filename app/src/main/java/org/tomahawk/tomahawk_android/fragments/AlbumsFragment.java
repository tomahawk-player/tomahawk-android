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

import org.jdeferred.AlwaysCallback;
import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;
import org.jdeferred.android.AndroidDeferredManager;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.AlphaComparator;
import org.tomahawk.libtomahawk.collection.ArtistAlphaComparator;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionCursor;
import org.tomahawk.libtomahawk.collection.HatchetCollection;
import org.tomahawk.libtomahawk.collection.LastModifiedComparator;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.ScriptResolverCollection;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;

import android.os.Bundle;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link TomahawkFragment} which shows a set of {@link Album}s inside its {@link
 * se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class AlbumsFragment extends TomahawkFragment {

    private static final String TAG = AlbumsFragment.class.getSimpleName();

    public static final String COLLECTION_ALBUMS_SPINNER_POSITION
            = "org.tomahawk.tomahawk_android.collection_albums_spinner_position_";

    @Override
    public void onResume() {
        super.onResume();

        mHideRemoveButton = true;
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
    public void onItemClick(View view, final Object item, Segment segment) {
        if (getMediaController() == null) {
            Log.e(TAG, "onItemClick failed because getMediaController() is null");
            return;
        }
        if (item instanceof PlaylistEntry) {
            final PlaylistEntry entry = (PlaylistEntry) item;
            if (entry.getQuery().isPlayable()) {
                if (getPlaybackManager().getCurrentEntry() == entry) {
                    // if the user clicked on an already playing track
                    int playState = getMediaController().getPlaybackState().getState();
                    if (playState == PlaybackStateCompat.STATE_PLAYING) {
                        getMediaController().getTransportControls().pause();
                    } else if (playState == PlaybackStateCompat.STATE_PAUSED) {
                        getMediaController().getTransportControls().play();
                    }
                } else {
                    if (!TomahawkApp.PLUGINNAME_HATCHET.equals(mCollection.getId())) {
                        mCollection.getArtistTracks(mArtist).done(new DoneCallback<Playlist>() {
                            @Override
                            public void onDone(Playlist topHits) {
                                getPlaybackManager().setPlaylist(topHits, entry);
                                getMediaController().getTransportControls().play();
                            }
                        });
                    } else {
                        HatchetCollection collection = (HatchetCollection) mCollection;
                        collection.getArtistTopHits(mArtist).done(new DoneCallback<Playlist>() {
                            @Override
                            public void onDone(Playlist topHits) {
                                getPlaybackManager().setPlaylist(topHits, entry);
                                getMediaController().getTransportControls().play();
                            }
                        });
                    }
                }
            }
        } else if (item instanceof Album) {
            Album album = (Album) item;
            mCollection.getAlbumTracks(album).done(new DoneCallback<Playlist>() {
                @Override
                public void onDone(Playlist playlist) {
                    Bundle bundle = new Bundle();
                    bundle.putString(TomahawkFragment.ALBUM, ((Album) item).getCacheKey());
                    if (playlist != null) {
                        bundle.putString(TomahawkFragment.COLLECTION_ID, mCollection.getId());
                    } else {
                        bundle.putString(
                                TomahawkFragment.COLLECTION_ID, TomahawkApp.PLUGINNAME_HATCHET);
                    }
                    bundle.putInt(CONTENT_HEADER_MODE,
                            ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                    FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                            PlaylistEntriesFragment.class, bundle);
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
                final List<Segment> segments = new ArrayList<>();
                List<Promise> promises = new ArrayList<>();
                promises.add(mCollection.getArtistTracks(mArtist));
                promises.add(mCollection.getArtistAlbums(mArtist));
                AndroidDeferredManager deferredManager = new AndroidDeferredManager();
                deferredManager.when(promises.toArray(new Promise[promises.size()])).always(
                        new AlwaysCallback<MultipleResults, OneReject>() {
                            @Override
                            public void onAlways(Promise.State state, MultipleResults resolved,
                                    OneReject rejected) {
                                Playlist artistTracks = (Playlist) resolved.get(0).getResult();
                                Segment segment = new Segment.Builder(artistTracks)
                                        .headerLayout(R.layout.single_line_list_header)
                                        .headerString(mCollection.getName() + " "
                                                + TomahawkApp.getContext().getString(
                                                R.string.tracks))
                                        .showNumeration(true, 1)
                                        .hideArtistName(true)
                                        .showDuration(true)
                                        .build();
                                segments.add(0, segment);

                                CollectionCursor<Album> cursor =
                                        (CollectionCursor<Album>) resolved.get(1).getResult();
                                segment = new Segment.Builder(cursor)
                                        .headerLayout(R.layout.single_line_list_header)
                                        .headerString(mCollection.getName() + " "
                                                + TomahawkApp.getContext().getString(
                                                R.string.albums))
                                        .showAsGrid(R.integer.grid_column_count,
                                                R.dimen.padding_superlarge,
                                                R.dimen.padding_superlarge)
                                        .build();
                                segments.add(segment);
                                fillAdapter(segments, mCollection);
                            }
                        });
            } else {
                HatchetCollection collection = (HatchetCollection) mCollection;
                final List<Segment> segments = new ArrayList<>();
                List<Promise> promises = new ArrayList<>();
                promises.add(collection.getArtistTopHits(mArtist));
                promises.add(collection.getArtistAlbums(mArtist));
                AndroidDeferredManager deferredManager = new AndroidDeferredManager();
                deferredManager.when(promises.toArray(new Promise[promises.size()])).always(
                        new AlwaysCallback<MultipleResults, OneReject>() {
                            @Override
                            public void onAlways(Promise.State state, MultipleResults resolved,
                                    OneReject rejected) {
                                Playlist artistTophits = (Playlist) resolved.get(0).getResult();
                                Segment segment = new Segment.Builder(artistTophits)
                                        .headerLayout(R.layout.single_line_list_header)
                                        .headerString(R.string.top_hits)
                                        .showNumeration(true, 1)
                                        .hideArtistName(true)
                                        .showDuration(true)
                                        .build();
                                segments.add(0, segment);

                                CollectionCursor<Album> cursor =
                                        (CollectionCursor<Album>) resolved.get(1).getResult();
                                List<Album> albumsAndEps = new ArrayList<>();
                                /* Remove this for now since all "other releases" albums returned by
                                   Hatchet are empty
                                List<Album> others = new ArrayList<>();
                                */
                                if (cursor != null) {
                                    for (int i = 0; i < cursor.size(); i++) {
                                        Album album = cursor.get(i);
                                        if (album.getReleaseType() != null
                                                && (Album.RELEASETYPE_ALBUM.equals(
                                                album.getReleaseType())
                                                || Album.RELEASETYPE_EPS.equals(
                                                album.getReleaseType()))) {
                                            albumsAndEps.add(album);
                                        }
                                        /* Remove this for now since all "other releases" albums returned by
                                           Hatchet are empty
                                        else {
                                            others.add(album);
                                        }
                                        */
                                    }
                                }
                                segment = new Segment.Builder(albumsAndEps)
                                        .headerLayout(R.layout.single_line_list_header)
                                        .headerString(R.string.albums_and_eps)
                                        .showAsGrid(R.integer.grid_column_count,
                                                R.dimen.padding_superlarge,
                                                R.dimen.padding_superlarge)
                                        .build();
                                segments.add(segment);
                                /* Remove this for now since all "other releases" albums returned by
                                   Hatchet are empty
                                segment = new Segment.Builder(others)
                                        .headerLayout(R.layout.single_line_list_header)
                                        .headerString(R.string.other_releases)
                                        .showAsGrid(R.integer.grid_column_count,
                                                R.dimen.padding_superlarge,
                                                R.dimen.padding_superlarge)
                                        .build();
                                segments.add(segment);
                                */
                                fillAdapter(segments);
                            }
                        });
            }
        } else if (mAlbumArray != null) {
            Segment.Builder builder = new Segment.Builder(mAlbumArray);
            if (mContainerFragmentClass != null
                    && mContainerFragmentClass.equals(ChartsPagerFragment.class.getName())) {
                builder.showAsGrid(R.integer.grid_column_count,
                        R.dimen.padding_superlarge,
                        R.dimen.padding_superlarge)
                        .showNumeration(true, 1);
            }
            Segment segment = builder.build();
            fillAdapter(segment);
        } else if (mUser != null) {
            String id = mCollection.getId();
            Segment segment = new Segment.Builder(sortLovedAlbums(mUser, mUser.getStarredAlbums()))
                    .headerLayout(R.layout.dropdown_header)
                    .headerStrings(constructDropdownItems())
                    .spinner(constructDropdownListener(COLLECTION_ALBUMS_SPINNER_POSITION + id),
                            getDropdownPos(COLLECTION_ALBUMS_SPINNER_POSITION + id))
                    .showAsGrid(R.integer.grid_column_count,
                            R.dimen.padding_superlarge,
                            R.dimen.padding_superlarge)
                    .build();
            fillAdapter(segment);
        } else {
            mCollection.getAlbums(getSortMode()).done(new DoneCallback<CollectionCursor<Album>>() {
                @Override
                public void onDone(final CollectionCursor<Album> cursor) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String id = mCollection.getId();
                            Segment segment = new Segment.Builder(cursor)
                                    .headerLayout(R.layout.dropdown_header)
                                    .headerStrings(constructDropdownItems())
                                    .spinner(constructDropdownListener(
                                            COLLECTION_ALBUMS_SPINNER_POSITION + id),
                                            getDropdownPos(COLLECTION_ALBUMS_SPINNER_POSITION + id))
                                    .showAsGrid(R.integer.grid_column_count,
                                            R.dimen.padding_superlarge,
                                            R.dimen.padding_superlarge)
                                    .build();
                            fillAdapter(segment, mCollection);
                        }
                    }).start();
                }
            });
        }
    }

    private List<Integer> constructDropdownItems() {
        List<Integer> dropDownItems = new ArrayList<>();
        if (!(mCollection instanceof ScriptResolverCollection)) {
            dropDownItems.add(R.string.collection_dropdown_recently_added);
        }
        dropDownItems.add(R.string.collection_dropdown_alpha);
        dropDownItems.add(R.string.collection_dropdown_alpha_artists);
        return dropDownItems;
    }

    private int getSortMode() {
        String id = mCollection.getId();
        int pos = getDropdownPos(COLLECTION_ALBUMS_SPINNER_POSITION + id);
        if (!(mCollection instanceof ScriptResolverCollection)) {
            switch (pos) {
                case 0:
                    return Collection.SORT_LAST_MODIFIED;
                case 1:
                    return Collection.SORT_ALPHA;
                case 2:
                    return Collection.SORT_ARTIST_ALPHA;
                default:
                    return Collection.SORT_NOT;
            }
        } else {
            switch (pos) {
                case 0:
                    return Collection.SORT_ALPHA;
                case 1:
                    return Collection.SORT_ARTIST_ALPHA;
                default:
                    return Collection.SORT_NOT;
            }
        }
    }

    private List<Album> sortLovedAlbums(User user, List<Album> albums) {
        String id = mCollection.getId();
        switch (getDropdownPos(COLLECTION_ALBUMS_SPINNER_POSITION + id)) {
            case 0:
                Map<Album, Long> timestamps = new HashMap<>();
                for (Album album : albums) {
                    timestamps.put(album, user.getRelationship(album).getDate().getTime());
                }
                Collections.sort(albums, new LastModifiedComparator<>(timestamps));
                break;
            case 1:
                Collections.sort(albums, new AlphaComparator());
                break;
            case 2:
                Collections.sort(albums, new ArtistAlphaComparator());
                break;
        }
        return albums;
    }
}
