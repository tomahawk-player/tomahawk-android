/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Christopher Reichert <creichert07@gmail.com>
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
package org.tomahawk.tomahawk_android.fragments;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionLoader;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.database.UserPlaylistsDataSource;
import org.tomahawk.libtomahawk.hatchet.InfoSystem;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.dialogs.ChooseUserPlaylistDialog;
import org.tomahawk.tomahawk_android.dialogs.FakeContextMenuDialog;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FakeContextMenu;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The base class for {@link AlbumsFragment}, {@link TracksFragment}, {@link ArtistsFragment},
 * {@link UserPlaylistsFragment} and {@link SearchableFragment}. Provides all sorts of functionality
 * to those classes, related to displaying {@link org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}s
 * in whichever needed way.
 */
public class TomahawkFragment extends TomahawkListFragment
        implements LoaderManager.LoaderCallbacks<Collection>, FakeContextMenu {

    public static final String TOMAHAWK_ALBUM_ID
            = "org.tomahawk.tomahawk_android.tomahawk_album_id";

    public static final String TOMAHAWK_TRACK_ID
            = "org.tomahawk.tomahawk_android.tomahawk_track_id";

    public static final String TOMAHAWK_ARTIST_ID
            = "org.tomahawk.tomahawk_android.tomahawk_artist_id";

    public static final String TOMAHAWK_PLAYLIST_ID
            = "org.tomahawk.tomahawk_android.tomahawk_playlist_id";

    public static final String TOMAHAWK_HUB_ID = "org.tomahawk.tomahawk_android.tomahawk_hub_id";

    protected TomahawkApp mTomahawkApp;

    private TomahawkFragmentReceiver mTomahawkFragmentReceiver;

    protected ArrayList<String> mCurrentRequestIds = new ArrayList<String>();

    protected InfoSystem mInfoSystem;

    protected PipeLine mPipeline;

    protected ConcurrentHashMap<String, Track> mCorrespondingQueryIds
            = new ConcurrentHashMap<String, Track>();

    private UserPlaylistsDataSource mUserPlaylistsDataSource;

    protected TomahawkMainActivity mTomahawkMainActivity;

    protected int mCorrespondingHubId;

    protected Album mAlbum;

    protected Artist mArtist;

    protected UserPlaylist mUserPlaylist;

    /**
     * Handles incoming {@link Collection} updated broadcasts.
     */
    private class TomahawkFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Collection.COLLECTION_UPDATED.equals(intent.getAction())) {
                onCollectionUpdated();
            }
        }
    }

    /**
     * Store the reference to the {@link Activity}, in which this {@link UserCollectionFragment} has
     * been created
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof TomahawkMainActivity) {
            mTomahawkMainActivity = (TomahawkMainActivity) activity;
        }
    }

    /**
     * Basic initializations. Get corresponding hub id through getArguments(), if not null
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            if (getArguments().containsKey(TOMAHAWK_HUB_ID)
                    && getArguments().getInt(TOMAHAWK_HUB_ID) > 0) {
                mCorrespondingHubId = getArguments().getInt(TOMAHAWK_HUB_ID);
            }
        }
        mTomahawkApp = ((TomahawkApp) mTomahawkMainActivity.getApplication());
        mInfoSystem = mTomahawkApp.getInfoSystem();
        mPipeline = mTomahawkApp.getPipeLine();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Adapt to current orientation. Show different count of columns in the GridView
        adaptColumnCount();

        getActivity().getSupportLoaderManager().destroyLoader(getId());
        getActivity().getSupportLoaderManager().initLoader(getId(), null, this);

        // Initialize and register Receiver
        if (mTomahawkFragmentReceiver == null) {
            mTomahawkFragmentReceiver = new TomahawkFragmentReceiver();
            IntentFilter intentFilter = new IntentFilter(Collection.COLLECTION_UPDATED);
            getActivity().registerReceiver(mTomahawkFragmentReceiver, intentFilter);
        }

        // Initialize UserPlaylistsDataSource, which makes it possible to retrieve persisted
        // UserPlaylists
        mUserPlaylistsDataSource = new UserPlaylistsDataSource(mTomahawkMainActivity,
                mTomahawkApp.getPipeLine());
        mUserPlaylistsDataSource.open();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mTomahawkFragmentReceiver != null) {
            getActivity().unregisterReceiver(mTomahawkFragmentReceiver);
            mTomahawkFragmentReceiver = null;
        }
        if (mUserPlaylistsDataSource != null) {
            mUserPlaylistsDataSource.close();
        }
    }

    /**
     * Null the reference to this {@link FakePreferenceFragment}'s {@link Activity}
     */
    @Override
    public void onDetach() {
        super.onDetach();

        mTomahawkMainActivity = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        adaptColumnCount();
    }

    /**
     * Insert our FakeContextMenuDialog initialization here, don't call overriden super method
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        int position = info.position;
        String[] menuItemTitles;
        TomahawkBaseAdapter.TomahawkListItem tomahawkListItem;
        position -= getListView().getHeaderViewsCount();
        if (position >= 0) {
            tomahawkListItem = ((TomahawkBaseAdapter.TomahawkListItem) getListAdapter()
                    .getItem(position));
        } else {
            tomahawkListItem = ((TomahawkListAdapter) getListAdapter())
                    .getContentHeaderTomahawkListItem();
        }
        if (!(tomahawkListItem instanceof UserPlaylist || (tomahawkListItem instanceof Track
                && mUserPlaylist != null))) {
            menuItemTitles = getResources()
                    .getStringArray(R.array.fake_context_menu_items_without_delete);
        } else if (tomahawkListItem instanceof UserPlaylist) {
            menuItemTitles = getResources()
                    .getStringArray(R.array.fake_context_menu_items_without_addplaylist);
        } else {
            menuItemTitles = getResources().getStringArray(R.array.fake_context_menu_items);
        }
        new FakeContextMenuDialog(menuItemTitles, info.position, this)
                .show(mTomahawkMainActivity.getSupportFragmentManager(), null);
    }

    /**
     * If the user clicks on a fakeContextItem, handle what should be done here
     */
    @Override
    public void onFakeContextItemSelected(String menuItemTitle, int position) {
        UserCollection userCollection = mTomahawkMainActivity.getUserCollection();
        TomahawkBaseAdapter.TomahawkListItem tomahawkListItem;
        position -= getListView().getHeaderViewsCount();
        if (position >= 0) {
            tomahawkListItem = ((TomahawkBaseAdapter.TomahawkListItem) getListAdapter()
                    .getItem(position));
        } else {
            tomahawkListItem = ((TomahawkListAdapter) getListAdapter())
                    .getContentHeaderTomahawkListItem();
        }
        ArrayList<Track> tracks = new ArrayList<Track>();
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        if (menuItemTitle.equals(getResources().getString(R.string.fake_context_menu_delete))) {
            if (tomahawkListItem instanceof UserPlaylist) {
                mUserPlaylistsDataSource
                        .deleteUserPlaylist(((UserPlaylist) tomahawkListItem).getId());
                userCollection.updateUserPlaylists();
            } else if (tomahawkListItem instanceof Track && mUserPlaylist != null) {
                mUserPlaylistsDataSource.deleteTrackInUserPlaylist(mUserPlaylist.getId(),
                        ((Track) tomahawkListItem).getId());
                userCollection.updateUserPlaylists();
            } else if (playbackService != null && this instanceof PlaybackFragment
                    && tomahawkListItem instanceof Track) {
                if (playbackService.getCurrentPlaylist().getCurrentTrackIndex() == position) {
                    boolean wasPlaying = playbackService.isPlaying();
                    if (wasPlaying) {
                        playbackService.pause();
                    }
                    if (playbackService.getCurrentPlaylist().peekTrackAtPos(
                            playbackService.getCurrentPlaylist().getCurrentTrackIndex()
                                    + 1) != null) {
                        playbackService.setCurrentTrack(
                                playbackService.getCurrentPlaylist().getTrackAtPos(
                                        playbackService.getCurrentPlaylist()
                                                .getCurrentTrackIndex() + 1));
                        if (wasPlaying) {
                            playbackService.start();
                        }
                    } else if (playbackService.getCurrentPlaylist().peekTrackAtPos(
                            playbackService.getCurrentPlaylist().getCurrentTrackIndex()
                                    - 1) != null) {
                        playbackService.setCurrentTrack(
                                playbackService.getCurrentPlaylist().getTrackAtPos(
                                        playbackService.getCurrentPlaylist()
                                                .getCurrentTrackIndex() - 1));
                        if (wasPlaying) {
                            playbackService.start();
                        }
                    }
                }
                playbackService.deleteTrackAtPos(position);
            }
        } else if (menuItemTitle
                .equals(getResources().getString(R.string.fake_context_menu_play))) {
            if (this instanceof PlaybackFragment) {
                if (playbackService != null && tomahawkListItem instanceof Track) {
                    if (playbackService.getCurrentPlaylist().getCurrentTrackIndex() == position) {
                        if (!playbackService.isPlaying()) {
                            playbackService.start();
                        }
                    } else {
                        playbackService.setCurrentTrack(
                                playbackService.getCurrentPlaylist().peekTrackAtPos(position));
                        playbackService.start();
                    }
                }
            } else {
                UserPlaylist playlist = null;
                if (tomahawkListItem instanceof Track) {
                    if (mAlbum != null) {
                        tracks = mAlbum.getTracks();
                        playlist = UserPlaylist
                                .fromTrackList(UserPlaylist.LAST_USED_PLAYLIST_NAME, tracks,
                                        (Track) tomahawkListItem);
                    } else if (mArtist != null) {
                        tracks = mArtist.getTracks();
                        playlist = UserPlaylist
                                .fromTrackList(UserPlaylist.LAST_USED_PLAYLIST_NAME, tracks,
                                        (Track) tomahawkListItem);
                    } else if (mUserPlaylist != null) {
                        tracks = mUserPlaylist.getTracks();
                        playlist = UserPlaylist
                                .fromTrackList(UserPlaylist.LAST_USED_PLAYLIST_NAME, tracks,
                                        (Track) tomahawkListItem);
                    } else {
                        tracks.add((Track) tomahawkListItem);
                        playlist = UserPlaylist
                                .fromTrackList(UserPlaylist.LAST_USED_PLAYLIST_NAME, tracks, 0);
                    }
                    userCollection.setCachedPlaylist(playlist);
                } else if (tomahawkListItem instanceof UserPlaylist) {
                    playlist = (UserPlaylist) tomahawkListItem;
                } else if (tomahawkListItem instanceof Album) {
                    playlist = UserPlaylist.fromTrackList(UserPlaylist.LAST_USED_PLAYLIST_NAME,
                            ((Album) tomahawkListItem).getTracks(), 0);
                } else if (tomahawkListItem instanceof Artist) {
                    playlist = UserPlaylist.fromTrackList(UserPlaylist.LAST_USED_PLAYLIST_NAME,
                            ((Artist) tomahawkListItem).getTracks(), 0);
                }
                if (playbackService != null) {
                    playbackService.setCurrentPlaylist(playlist);
                    playbackService.start();
                }
                mTomahawkMainActivity.getContentViewer()
                        .setCurrentHubId(TomahawkMainActivity.HUB_ID_PLAYBACK);
            }
        } else if (menuItemTitle.equals(getResources()
                .getString(R.string.fake_context_menu_playaftercurrenttrack))) {
            if (tomahawkListItem instanceof Track) {
                tracks.add((Track) tomahawkListItem);
            } else if (tomahawkListItem instanceof UserPlaylist) {
                tracks = ((UserPlaylist) tomahawkListItem).getTracks();
            } else if (tomahawkListItem instanceof Album) {
                tracks = ((Album) tomahawkListItem).getTracks();
            } else if (tomahawkListItem instanceof Artist) {
                tracks = ((Artist) tomahawkListItem).getTracks();
            }
            if (playbackService != null) {
                if (playbackService.getCurrentPlaylist() != null) {
                    playbackService.addTracksToCurrentPlaylist(
                            playbackService.getCurrentPlaylist().getCurrentTrackIndex() + 1,
                            tracks);
                } else {
                    playbackService.addTracksToCurrentPlaylist(tracks);
                }
            }
        } else if (menuItemTitle.equals(getResources()
                .getString(R.string.fake_context_menu_appendtoplaybacklist))) {
            if (tomahawkListItem instanceof Track) {
                tracks.add((Track) tomahawkListItem);
            } else if (tomahawkListItem instanceof UserPlaylist) {
                tracks = ((UserPlaylist) tomahawkListItem).getTracks();
            } else if (tomahawkListItem instanceof Album) {
                tracks = ((Album) tomahawkListItem).getTracks();
            } else if (tomahawkListItem instanceof Artist) {
                tracks = ((Artist) tomahawkListItem).getTracks();
            }
            if (playbackService != null) {
                playbackService.addTracksToCurrentPlaylist(tracks);
            }
        } else if (menuItemTitle
                .equals(getResources().getString(R.string.fake_context_menu_addtoplaylist))) {
            if (tomahawkListItem instanceof Track) {
                tracks.add((Track) tomahawkListItem);
            } else if (tomahawkListItem instanceof UserPlaylist) {
                tracks = ((UserPlaylist) tomahawkListItem).getTracks();
            } else if (tomahawkListItem instanceof Album) {
                tracks = ((Album) tomahawkListItem).getTracks();
            } else if (tomahawkListItem instanceof Artist) {
                tracks = ((Artist) tomahawkListItem).getTracks();
            }
            new ChooseUserPlaylistDialog(userCollection, tracks)
                    .show(mTomahawkMainActivity.getSupportFragmentManager(),
                            "ChooseUserPlaylistDialog");
            userCollection.updateUserPlaylists();
        }
    }

    /**
     * Adjust the column count so it fits to the current screen configuration
     */
    public void adaptColumnCount() {
        if (getGridView() != null) {
            if (getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE) {
                getGridView().setNumColumns(4);
            } else {
                getGridView().setNumColumns(2);
            }
        }
    }

    /**
     * Called when a Collection has been updated.
     */
    protected void onCollectionUpdated() {
        mTomahawkMainActivity.getSupportLoaderManager().restartLoader(getId(), null, this);
    }

    @Override
    public Loader<Collection> onCreateLoader(int id, Bundle args) {
        return new CollectionLoader(getActivity(), mTomahawkMainActivity.getUserCollection());
    }

    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
    }

    @Override
    public void onLoaderReset(Loader<Collection> loader) {
    }
}
