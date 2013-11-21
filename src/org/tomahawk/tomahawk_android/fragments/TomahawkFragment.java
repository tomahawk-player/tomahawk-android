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
import org.tomahawk.tomahawk_android.activities.PlaybackActivity;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.dialogs.ChooseUserPlaylistDialog;
import org.tomahawk.tomahawk_android.dialogs.FakeContextMenuDialog;
import org.tomahawk.tomahawk_android.utils.FakeContextMenu;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

    public static final String TOMAHAWK_ALBUM_ID = "tomahawk_album_id";

    public static final String TOMAHAWK_TRACK_ID = "tomahawk_track_id";

    public static final String TOMAHAWK_ARTIST_ID = "tomahawk_artist_id";

    public static final String TOMAHAWK_PLAYLIST_ID = "tomahawk_playlist_id";

    public static final String TOMAHAWK_HUB_ID = "tomahawk_hub_id";

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

    private Drawable mProgressDrawable;

    // Used to display an animated progress drawable, as long as the PipeLine is resolving something
    private Handler mAnimationHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_ANIMATION:
                    if (mPipeline.isResolving()) {
                        mProgressDrawable.setLevel(mProgressDrawable.getLevel() + 500);
                        mTomahawkMainActivity.getSupportActionBar().setLogo(mProgressDrawable);
                        mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
                        mAnimationHandler.sendEmptyMessageDelayed(MSG_UPDATE_ANIMATION, 50);
                    } else {
                        stopLoadingAnimation();
                    }
                    break;
            }
            return true;
        }
    });

    private static final int MSG_UPDATE_ANIMATION = 0x20;

    /**
     * Handles incoming {@link Collection} updated broadcasts.
     */
    private class TomahawkFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Collection.COLLECTION_UPDATED)) {
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

        mProgressDrawable = getResources().getDrawable(R.drawable.progress_indeterminate_tomahawk);

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
                .show(getFragmentManager(), null);
    }

    /**
     * If the user clicks on a fakeContextItem, handle what should be done here
     */
    @Override
    public void onFakeContextItemSelected(String menuItemTitle, int position) {
        UserCollection userCollection = ((UserCollection) mTomahawkApp.getSourceList()
                .getCollectionFromId(UserCollection.Id));
        TomahawkBaseAdapter.TomahawkListItem tomahawkListItem;
        position -= getListView().getHeaderViewsCount();
        if (position >= 0) {
            tomahawkListItem = ((TomahawkBaseAdapter.TomahawkListItem) getListAdapter()
                    .getItem(position));
        } else {
            tomahawkListItem = ((TomahawkListAdapter) getListAdapter())
                    .getContentHeaderTomahawkListItem();
        }
        Bundle bundle = new Bundle();
        ArrayList<Track> tracks = new ArrayList<Track>();
        if (menuItemTitle.equals(getResources().getString(R.string.fake_context_menu_delete))) {
            if (tomahawkListItem instanceof UserPlaylist) {
                mUserPlaylistsDataSource
                        .deleteUserPlaylist(((UserPlaylist) tomahawkListItem).getId());
            } else if (tomahawkListItem instanceof Track && mUserPlaylist != null) {
                mUserPlaylistsDataSource.deleteTrackInUserPlaylist(mUserPlaylist.getId(),
                        ((Track) tomahawkListItem).getId());
            }
            userCollection.updateUserPlaylists();
        } else if (menuItemTitle
                .equals(getResources().getString(R.string.fake_context_menu_play))) {
            if (tomahawkListItem instanceof Track) {
                UserPlaylist playlist;
                if (mAlbum != null) {
                    tracks = mAlbum.getTracks();
                    playlist = UserPlaylist
                            .fromTrackList("Last used playlist", tracks, (Track) tomahawkListItem);
                    playlist.setCurrentTrackIndex(position);
                } else if (mArtist != null) {
                    tracks = mArtist.getTracks();
                    playlist = UserPlaylist
                            .fromTrackList("Last used playlist", tracks, (Track) tomahawkListItem);
                    playlist.setCurrentTrackIndex(position);
                } else if (mUserPlaylist != null) {
                    tracks = mUserPlaylist.getTracks();
                    playlist = UserPlaylist
                            .fromTrackList("Last used playlist", tracks, (Track) tomahawkListItem);
                    playlist.setCurrentTrackIndex(position);
                } else {
                    tracks.add((Track) tomahawkListItem);
                    playlist = UserPlaylist
                            .fromTrackList("Last used playlist", tracks, (Track) tomahawkListItem);
                    playlist.setCurrentTrackIndex(0);
                }
                userCollection.setCachedPlaylist(playlist);
                bundle.putBoolean(UserCollection.USERCOLLECTION_PLAYLISTCACHED, true);
                bundle.putLong(PlaybackActivity.PLAYLIST_TRACK_ID,
                        ((Track) tomahawkListItem).getId());
            } else if (tomahawkListItem instanceof UserPlaylist) {
                bundle.putLong(PlaybackActivity.PLAYLIST_PLAYLIST_ID,
                        ((UserPlaylist) tomahawkListItem).getId());
            } else if (tomahawkListItem instanceof Album) {
                bundle.putLong(PlaybackActivity.PLAYLIST_ALBUM_ID,
                        ((Album) tomahawkListItem).getId());
                bundle.putLong(PlaybackActivity.PLAYLIST_TRACK_ID,
                        ((Album) tomahawkListItem).getTracks().get(0).getId());
            } else if (tomahawkListItem instanceof Artist) {
                bundle.putLong(PlaybackActivity.PLAYLIST_ARTIST_ID,
                        ((Artist) tomahawkListItem).getId());
            }
            Intent playbackIntent = new Intent(getActivity(), PlaybackActivity.class);
            playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, bundle);
            startActivity(playbackIntent);
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
            if (mTomahawkMainActivity.getPlaybackService().getCurrentPlaylist() != null) {
                mTomahawkMainActivity.getPlaybackService().addTracksToCurrentPlaylist(
                        mTomahawkMainActivity.getPlaybackService().getCurrentPlaylist()
                                .getCurrentTrackIndex() + 1, tracks);
            } else {
                mTomahawkMainActivity.getPlaybackService().addTracksToCurrentPlaylist(tracks);
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
            mTomahawkMainActivity.getPlaybackService().addTracksToCurrentPlaylist(tracks);
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
        getActivity().getSupportLoaderManager().restartLoader(getId(), null, this);
    }

    @Override
    public Loader<Collection> onCreateLoader(int id, Bundle args) {
        return new CollectionLoader(getActivity(), getCurrentCollection());
    }

    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
    }

    @Override
    public void onLoaderReset(Loader<Collection> loader) {
    }

    /**
     * @return the current Collection
     */
    public Collection getCurrentCollection() {
        if (mTomahawkMainActivity != null) {
            return mTomahawkMainActivity.getCollection();
        }
        return null;
    }

    /**
     * Start the loading animation. Called when beginning login process.
     */
    public void startLoadingAnimation() {
        mAnimationHandler.sendEmptyMessageDelayed(MSG_UPDATE_ANIMATION, 50);
    }

    /**
     * Stop the loading animation. Called when login/logout process has finished.
     */
    public void stopLoadingAnimation() {
        mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
        mTomahawkMainActivity.getSupportActionBar().setLogo(R.drawable.ic_launcher);
    }
}
