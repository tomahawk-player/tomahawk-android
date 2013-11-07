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
import org.tomahawk.tomahawk_android.dialogs.ChoosePlaylistDialog;
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

public abstract class TomahawkFragment extends TomahawkListFragment
        implements LoaderManager.LoaderCallbacks<Collection>, FakeContextMenu {

    public static final String TOMAHAWK_ALBUM_ID = "tomahawk_album_id";

    public static final String TOMAHAWK_TRACK_ID = "tomahawk_track_id";

    public static final String TOMAHAWK_ARTIST_ID = "tomahawk_artist_id";

    public static final String TOMAHAWK_PLAYLIST_ID = "tomahawk_playlist_id";

    public static final String TOMAHAWK_TAB_ID = "tomahawk_tab_id";

    protected TomahawkApp mTomahawkApp;

    private TomahawkFragmentReceiver mTomahawkFragmentReceiver;

    protected ArrayList<String> mCurrentRequestIds = new ArrayList<String>();

    protected InfoSystem mInfoSystem;

    protected PipeLine mPipeline;

    protected ConcurrentHashMap<String, Track> mCorrespondingQueryIds
            = new ConcurrentHashMap<String, Track>();

    private UserPlaylistsDataSource mUserPlaylistsDataSource;

    protected TomahawkMainActivity mActivity;

    protected int mCorrespondingStackId;

    protected Album mAlbum;

    protected Artist mArtist;

    protected UserPlaylist mUserPlaylist;

    private Drawable mProgressDrawable;

    private Handler mAnimationHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_ANIMATION:
                    if (mPipeline.isResolving()) {
                        mProgressDrawable.setLevel(mProgressDrawable.getLevel() + 500);
                        mActivity.getSupportActionBar().setLogo(mProgressDrawable);
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

        /*
         * (non-Javadoc)
         * 
         * @see
         * android.content.BroadcastReceiver#onReceive(android.content.Context,
         * android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Collection.COLLECTION_UPDATED)) {
                onCollectionUpdated();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (getArguments().containsKey(TOMAHAWK_TAB_ID)
                    && getArguments().getInt(TOMAHAWK_TAB_ID) > 0) {
                mCorrespondingStackId = getArguments().getInt(TOMAHAWK_TAB_ID);
            }
        }
        mTomahawkApp = ((TomahawkApp) mActivity.getApplication());
        mInfoSystem = mTomahawkApp.getInfoSystem();
        mPipeline = mTomahawkApp.getPipeLine();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        mProgressDrawable = getResources().getDrawable(R.drawable.progress_indeterminate_tomahawk);

        adaptColumnCount();

        getSherlockActivity().getSupportLoaderManager().destroyLoader(getId());
        getSherlockActivity().getSupportLoaderManager().initLoader(getId(), null, this);

        if (mTomahawkFragmentReceiver == null) {
            mTomahawkFragmentReceiver = new TomahawkFragmentReceiver();
            IntentFilter intentFilter = new IntentFilter(Collection.COLLECTION_UPDATED);
            getActivity().registerReceiver(mTomahawkFragmentReceiver, intentFilter);
        }

        mUserPlaylistsDataSource = new UserPlaylistsDataSource(mActivity,
                mTomahawkApp.getPipeLine());
        mUserPlaylistsDataSource.open();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.Fragment#onPause()
     */
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

    /*
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockListFragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity) {
        mActivity = (TomahawkMainActivity) activity;
        super.onAttach(activity);
    }

    /*
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockListFragment#onDetach()
     */
    @Override
    public void onDetach() {
        super.onDetach();
    }

    /* 
     * (non-Javadoc)
     * @see android.support.v4.app.Fragment#onConfigurationChanged(android.content.res.Configuration)
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        adaptColumnCount();
    }

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
            if (mActivity.getPlaybackService().getCurrentPlaylist() != null) {
                mActivity.getPlaybackService().addTracksToCurrentPlaylist(
                        mActivity.getPlaybackService().getCurrentPlaylist().getCurrentTrackIndex()
                                + 1, tracks);
            } else {
                mActivity.getPlaybackService().addTracksToCurrentPlaylist(tracks);
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
            mActivity.getPlaybackService().addTracksToCurrentPlaylist(tracks);
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
            new ChoosePlaylistDialog(userCollection, tracks)
                    .show(mActivity.getSupportFragmentManager(), "ChoosePlaylistDialog");
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
        getSherlockActivity().getSupportLoaderManager().restartLoader(getId(), null, this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int,
     * android.os.Bundle)
     */
    @Override
    public Loader<Collection> onCreateLoader(int id, Bundle args) {
        return new CollectionLoader(getActivity(), getCurrentCollection());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android
     * .support.v4.content.Loader, java.lang.Object)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android
     * .support.v4.content.Loader)
     */
    @Override
    public void onLoaderReset(Loader<Collection> loader) {
    }

    /**
     * @return the current Collection
     */
    public Collection getCurrentCollection() {
        if (mActivity != null) {
            return mActivity.getCollection();
        }
        return null;
    }

    public void startLoadingAnimation() {
        mAnimationHandler.sendEmptyMessageDelayed(MSG_UPDATE_ANIMATION, 50);
    }

    public void stopLoadingAnimation() {
        mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
        mActivity.getSupportActionBar().setLogo(R.drawable.ic_action_slidemenu);
    }
}
