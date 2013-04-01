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
package org.tomahawk.tomahawk_android;

import com.actionbarsherlock.app.SherlockFragment;

import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.CollectionLoader;
import org.tomahawk.libtomahawk.TomahawkBaseAdapter;
import org.tomahawk.libtomahawk.TomahawkGridAdapter;
import org.tomahawk.libtomahawk.TomahawkListAdapter;
import org.tomahawk.libtomahawk.TomahawkStickyListHeadersListView;
import org.tomahawk.libtomahawk.Track;
import org.tomahawk.libtomahawk.UserCollection;
import org.tomahawk.libtomahawk.audio.PlaybackActivity;
import org.tomahawk.libtomahawk.database.UserPlaylistsDataSource;
import org.tomahawk.libtomahawk.playlist.CustomPlaylist;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public abstract class TomahawkFragment extends SherlockFragment
        implements LoaderManager.LoaderCallbacks<Collection> {

    public static final String TOMAHAWK_ALBUM_ID = "tomahawk_album_id";

    public static final String TOMAHAWK_TRACK_ID = "tomahawk_track_id";

    public static final String TOMAHAWK_ARTIST_ID = "tomahawk_artist_id";

    public static final String TOMAHAWK_PLAYLIST_ID = "tomahawk_playlist_id";

    public static final String TOMAHAWK_LIST_SCROLL_POSITION = "tomahawk_list_scroll_position";

    private static IntentFilter sCollectionUpdateIntentFilter = new IntentFilter(
            Collection.COLLECTION_UPDATED);

    private CollectionUpdateReceiver mCollectionUpdatedReceiver;

    private UserPlaylistsDataSource mUserPlaylistsDataSource;

    protected TomahawkTabsActivity mActivity;

    static final int INTERNAL_EMPTY_ID = 0x00ff0001;

    static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;

    static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;

    TomahawkBaseAdapter mTomahawkBaseAdapter;

    TomahawkStickyListHeadersListView mList;

    GridView mGrid;

    private int mListScrollPosition = 0;

    protected Album mAlbum;

    protected Artist mArtist;

    protected CustomPlaylist mCustomPlaylist;

    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            ((mTomahawkBaseAdapter instanceof TomahawkGridAdapter) ? mGrid : mList)
                    .focusableViewAvailable(
                            ((mTomahawkBaseAdapter instanceof TomahawkGridAdapter) ? mGrid
                                    : mList));
        }
    };

    private boolean mBreadCrumbNavigationEnabled = true;

    /**
     * Handles incoming {@link Collection} updated broadcasts.
     */
    private class CollectionUpdateReceiver extends BroadcastReceiver {

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
        if (mActivity instanceof SearchableActivity) {
            setBreadCrumbNavigationEnabled(false);
        }
        if (getArguments() != null && getArguments().containsKey(TOMAHAWK_LIST_SCROLL_POSITION)
                && getArguments().getInt(TOMAHAWK_LIST_SCROLL_POSITION) > 0) {
            mListScrollPosition = getArguments().getInt(TOMAHAWK_LIST_SCROLL_POSITION);
        }
    }

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tomahawkfragment_layout, null, false);
    }

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.Fragment#onViewCreated(android.view.View, android.os.Bundle)
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureList();
        if (mBreadCrumbNavigationEnabled) {
            updateBreadCrumbNavigation();
        } else if (mActivity.findViewById(R.id.fragmentLayout_breadcrumbLayout_linearLayout)
                != null) {
            mActivity.findViewById(R.id.fragmentLayout_breadcrumbLayout_linearLayout)
                    .setVisibility(View.GONE);
        }
    }

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.Fragment#onDestroyView()
     */
    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus);
        mList = null;
        mGrid = null;
        super.onDestroyView();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        adaptColumnCount();

        getSherlockActivity().getSupportLoaderManager().destroyLoader(getId());
        getSherlockActivity().getSupportLoaderManager().initLoader(getId(), null, this);

        if (mCollectionUpdatedReceiver == null) {
            mCollectionUpdatedReceiver = new CollectionUpdateReceiver();
            getActivity()
                    .registerReceiver(mCollectionUpdatedReceiver, sCollectionUpdateIntentFilter);
        }

        if (mTomahawkBaseAdapter instanceof TomahawkGridAdapter) {
            getGridView().setSelection(mListScrollPosition);
        } else {
            getListView().setSelection(mListScrollPosition);
        }

        mUserPlaylistsDataSource = new UserPlaylistsDataSource(mActivity,
                ((TomahawkApp) mActivity.getApplication()).getPipeLine());
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

        if (mCollectionUpdatedReceiver != null) {
            getActivity().unregisterReceiver(mCollectionUpdatedReceiver);
            mCollectionUpdatedReceiver = null;
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
        mActivity = (TomahawkTabsActivity) activity;
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
        super.onCreateContextMenu(menu, v, menuInfo);
        android.view.MenuInflater inflater = mActivity.getMenuInflater();
        inflater.inflate(R.menu.popup_menu, menu);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        info.position -= mList.getHeaderViewsCount();
        TomahawkBaseAdapter.TomahawkListItem tomahawkListItem;
        if (info.position >= 0) {
            tomahawkListItem = ((TomahawkBaseAdapter.TomahawkListItem) mTomahawkBaseAdapter
                    .getItem(info.position));
        } else {
            tomahawkListItem = ((TomahawkListAdapter) mTomahawkBaseAdapter)
                    .getContentHeaderTomahawkListItem();
        }
        if (!(tomahawkListItem instanceof CustomPlaylist || (tomahawkListItem instanceof Track
                && mCustomPlaylist != null))) {
            menu.findItem(R.id.popupmenu_delete_item).setVisible(false);
        }
        if (tomahawkListItem instanceof CustomPlaylist) {
            menu.findItem(R.id.popupmenu_addtoplaylist_item).setVisible(false);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        UserCollection userCollection = ((UserCollection) ((TomahawkApp) mActivity.getApplication())
                .getSourceList().getCollectionFromId(UserCollection.Id));
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        info.position -= mList.getHeaderViewsCount();
        TomahawkBaseAdapter.TomahawkListItem tomahawkListItem;
        if (info.position >= 0) {
            tomahawkListItem = ((TomahawkBaseAdapter.TomahawkListItem) mTomahawkBaseAdapter
                    .getItem(info.position));
        } else {
            tomahawkListItem = ((TomahawkListAdapter) mTomahawkBaseAdapter)
                    .getContentHeaderTomahawkListItem();
        }
        Bundle bundle = new Bundle();
        ArrayList<Track> tracks = new ArrayList<Track>();
        switch (item.getItemId()) {
            case R.id.popupmenu_delete_item:
                if (tomahawkListItem instanceof CustomPlaylist) {
                    mUserPlaylistsDataSource
                            .deleteUserPlaylist(((CustomPlaylist) tomahawkListItem).getId());
                } else if (tomahawkListItem instanceof Track && mCustomPlaylist != null) {
                    mUserPlaylistsDataSource.deleteTrackInUserPlaylist(mCustomPlaylist.getId(),
                            ((Track) tomahawkListItem).getId());
                }
                userCollection.updateUserPlaylists();
                return true;
            case R.id.popupmenu_play_item:
                if (tomahawkListItem instanceof Track) {
                    CustomPlaylist playlist;
                    if (mAlbum != null) {
                        tracks = mAlbum.getTracks();
                        playlist = CustomPlaylist.fromTrackList("Last used playlist", tracks,
                                (Track) tomahawkListItem);
                        playlist.setCurrentTrackIndex(info.position);
                    } else if (mArtist != null) {
                        tracks = mArtist.getTracks();
                        playlist = CustomPlaylist.fromTrackList("Last used playlist", tracks,
                                (Track) tomahawkListItem);
                        playlist.setCurrentTrackIndex(info.position);
                    } else if (mCustomPlaylist != null) {
                        tracks = mCustomPlaylist.getTracks();
                        playlist = CustomPlaylist.fromTrackList("Last used playlist", tracks,
                                (Track) tomahawkListItem);
                        playlist.setCurrentTrackIndex(info.position);
                    } else {
                        tracks.add((Track) tomahawkListItem);
                        playlist = CustomPlaylist.fromTrackList("Last used playlist", tracks,
                                (Track) tomahawkListItem);
                        playlist.setCurrentTrackIndex(0);
                    }
                    userCollection.setCachedPlaylist(playlist);
                    bundle.putBoolean(UserCollection.USERCOLLECTION_PLAYLISTCACHED, true);
                    bundle.putLong(PlaybackActivity.PLAYLIST_TRACK_ID,
                            ((Track) tomahawkListItem).getId());
                } else if (tomahawkListItem instanceof CustomPlaylist) {
                    bundle.putLong(PlaybackActivity.PLAYLIST_PLAYLIST_ID,
                            ((CustomPlaylist) tomahawkListItem).getId());
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
                return true;
            case R.id.popupmenu_playaftercurrenttrack_item:
                if (tomahawkListItem instanceof Track) {
                    tracks.add((Track) tomahawkListItem);
                } else if (tomahawkListItem instanceof CustomPlaylist) {
                    tracks = ((CustomPlaylist) tomahawkListItem).getTracks();
                } else if (tomahawkListItem instanceof Album) {
                    tracks = ((Album) tomahawkListItem).getTracks();
                } else if (tomahawkListItem instanceof Artist) {
                    tracks = ((Artist) tomahawkListItem).getTracks();
                }
                if (mActivity.getPlaybackService().getCurrentPlaylist() != null) {
                    mActivity.getPlaybackService().addTracksToCurrentPlaylist(
                            mActivity.getPlaybackService().getCurrentPlaylist()
                                    .getCurrentTrackIndex() + 1, tracks);
                } else {
                    mActivity.getPlaybackService().addTracksToCurrentPlaylist(tracks);
                }
                return true;
            case R.id.popupmenu_appendtoplaybacklist_item:
                if (tomahawkListItem instanceof Track) {
                    tracks.add((Track) tomahawkListItem);
                } else if (tomahawkListItem instanceof CustomPlaylist) {
                    tracks = ((CustomPlaylist) tomahawkListItem).getTracks();
                } else if (tomahawkListItem instanceof Album) {
                    tracks = ((Album) tomahawkListItem).getTracks();
                } else if (tomahawkListItem instanceof Artist) {
                    tracks = ((Artist) tomahawkListItem).getTracks();
                }
                mActivity.getPlaybackService().addTracksToCurrentPlaylist(tracks);
                return true;
            case R.id.popupmenu_addtoplaylist_item:
                if (tomahawkListItem instanceof Track) {
                    tracks.add((Track) tomahawkListItem);
                } else if (tomahawkListItem instanceof CustomPlaylist) {
                    tracks = ((CustomPlaylist) tomahawkListItem).getTracks();
                } else if (tomahawkListItem instanceof Album) {
                    tracks = ((Album) tomahawkListItem).getTracks();
                } else if (tomahawkListItem instanceof Artist) {
                    tracks = ((Artist) tomahawkListItem).getTracks();
                }
                new ChoosePlaylistDialog(userCollection, tracks)
                        .show(mActivity.getSupportFragmentManager(), "ChoosePlaylistDialog");
                userCollection.updateUserPlaylists();
                return true;
            default:
                return super.onContextItemSelected(item);
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

    public void updateBreadCrumbNavigation() {
        ArrayList<TabsAdapter.FragmentStateHolder> backStack = ((CollectionActivity) mActivity)
                .getTabsAdapter().getBackStackAtPosition(TomahawkTabsActivity.TAB_ID_COLLECTION);
        LinearLayout navigationLayoutView = (LinearLayout) getActivity()
                .findViewById(R.id.fragmentLayout_breadcrumbLayout_linearLayout);
        if (navigationLayoutView != null) {
            int validFragmentCount = 0;
            for (TabsAdapter.FragmentStateHolder fpb : backStack) {
                if (fpb.clss == AlbumsFragment.class || fpb.clss == ArtistsFragment.class
                        || fpb.clss == TracksFragment.class
                        || fpb.clss == PlaylistsFragment.class) {
                    validFragmentCount++;
                }
            }
            Collection currentCollection = mActivity.getCollection();
            for (TabsAdapter.FragmentStateHolder fpb : backStack) {
                LinearLayout breadcrumbItem = (LinearLayout) getActivity().getLayoutInflater()
                        .inflate(R.layout.tomahawkfragment_layout_breadcrumb_item, null);
                ImageView breadcrumbItemImageView = (ImageView) breadcrumbItem
                        .findViewById(R.id.fragmentLayout_icon_imageButton);
                SquareHeightRelativeLayout breadcrumbItemImageViewLayout
                        = (SquareHeightRelativeLayout) breadcrumbItem
                        .findViewById(R.id.fragmentLayout_icon_squareHeightRelativeLayout);
                TextView breadcrumbItemTextView = (TextView) breadcrumbItem
                        .findViewById(R.id.fragmentLayout_text_textView);
                if (fpb.clss == AlbumsFragment.class) {
                    Artist correspondingArtist = currentCollection
                            .getArtistById(fpb.tomahawkListItemId);
                    if (currentCollection.getArtistById(fpb.tomahawkListItemId) != null) {
                        breadcrumbItemTextView.setText(correspondingArtist.getName());
                        breadcrumbItemImageViewLayout
                                .setVisibility(SquareHeightRelativeLayout.GONE);
                    } else {
                        if (validFragmentCount == 1) {
                            breadcrumbItemTextView
                                    .setText(getString(R.string.albumsfragment_title_string));
                        } else {
                            breadcrumbItemTextView.setVisibility(TextView.GONE);
                        }
                        breadcrumbItemImageView.setBackgroundDrawable(
                                getResources().getDrawable(R.drawable.ic_action_album));
                        breadcrumbItemImageViewLayout
                                .setVisibility(SquareHeightRelativeLayout.VISIBLE);
                    }
                    breadcrumbItem
                            .setOnClickListener(new BreadCrumbOnClickListener(fpb.fragmentTag));
                    navigationLayoutView.addView(breadcrumbItem);
                } else if (fpb.clss == ArtistsFragment.class) {
                    if (validFragmentCount == 1) {
                        breadcrumbItemTextView
                                .setText(getString(R.string.artistsfragment_title_string));
                    } else {
                        breadcrumbItemTextView.setVisibility(TextView.GONE);
                    }
                    breadcrumbItemImageView.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.ic_action_artist));
                    breadcrumbItemImageViewLayout.setVisibility(SquareHeightRelativeLayout.VISIBLE);
                    breadcrumbItem
                            .setOnClickListener(new BreadCrumbOnClickListener(fpb.fragmentTag));
                    navigationLayoutView.addView(breadcrumbItem);
                } else if (fpb.clss == TracksFragment.class) {
                    Album correspondingAlbum = currentCollection
                            .getAlbumById(fpb.tomahawkListItemId);
                    CustomPlaylist correspondingCustomPlaylist = currentCollection
                            .getCustomPlaylistById(fpb.tomahawkListItemId);
                    if (fpb.tomahawkListItemType == TOMAHAWK_ALBUM_ID
                            && correspondingAlbum != null) {
                        breadcrumbItemTextView.setText(correspondingAlbum.getName());
                        breadcrumbItemImageViewLayout
                                .setVisibility(SquareHeightRelativeLayout.GONE);
                    } else if (fpb.tomahawkListItemType == TOMAHAWK_PLAYLIST_ID
                            && correspondingCustomPlaylist != null) {
                        breadcrumbItemTextView.setText(correspondingCustomPlaylist.getName());
                        breadcrumbItemImageViewLayout
                                .setVisibility(SquareHeightRelativeLayout.GONE);
                    } else {
                        if (validFragmentCount == 1) {
                            breadcrumbItemTextView
                                    .setText(getString(R.string.tracksfragment_title_string));
                        } else {
                            breadcrumbItemTextView.setVisibility(TextView.GONE);
                        }
                        breadcrumbItemImageView.setBackgroundDrawable(
                                getResources().getDrawable(R.drawable.ic_action_track));
                        breadcrumbItemImageViewLayout
                                .setVisibility(SquareHeightRelativeLayout.VISIBLE);
                    }
                    breadcrumbItem
                            .setOnClickListener(new BreadCrumbOnClickListener(fpb.fragmentTag));
                    navigationLayoutView.addView(breadcrumbItem);
                } else if (fpb.clss == PlaylistsFragment.class) {
                    if (validFragmentCount == 1) {
                        breadcrumbItemTextView
                                .setText(getString(R.string.playlistsfragment_title_string));
                    } else {
                        breadcrumbItemTextView.setVisibility(TextView.GONE);
                    }
                    breadcrumbItemImageView.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.ic_action_playlist));
                    breadcrumbItemImageViewLayout.setVisibility(SquareHeightRelativeLayout.VISIBLE);
                    breadcrumbItem
                            .setOnClickListener(new BreadCrumbOnClickListener(fpb.fragmentTag));
                    navigationLayoutView.addView(breadcrumbItem);
                }
            }
        }
    }

    public class BreadCrumbOnClickListener implements View.OnClickListener {

        String mSavedFragmentTag;

        public BreadCrumbOnClickListener(String savedFragmentTag) {
            mSavedFragmentTag = savedFragmentTag;
        }

        @Override
        public void onClick(View view) {
            ((CollectionActivity) mActivity).getTabsAdapter()
                    .backToFragment(TomahawkTabsActivity.TAB_ID_COLLECTION, mSavedFragmentTag);
        }
    }

    /**
     * Get the activity's list view widget.
     */
    public TomahawkStickyListHeadersListView getListView() {
        ensureList();
        return mList;
    }

    /**
     * Get the activity's list view widget.
     */
    public GridView getGridView() {
        ensureList();
        return mGrid;
    }

    /**
     * Get the ListAdapter associated with this activity's ListView.
     */
    public ListAdapter getListAdapter() {
        return mTomahawkBaseAdapter;
    }

    private void ensureList() {
        if (((mTomahawkBaseAdapter instanceof TomahawkGridAdapter) ? mGrid : mList) != null) {
            return;
        }
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }
        if (root instanceof TomahawkStickyListHeadersListView) {
            mList = (TomahawkStickyListHeadersListView) root;
        } else if (root instanceof GridView) {
            mGrid = (GridView) root;
        } else {
            if (!(mTomahawkBaseAdapter instanceof TomahawkGridAdapter)) {
                View rawListView = root.findViewById(R.id.listview);
                if (!(rawListView instanceof TomahawkStickyListHeadersListView)) {
                    if (rawListView == null) {
                        throw new RuntimeException(
                                "Your content must have a TomahawkStickyListHeadersListView whose id attribute is "
                                        + "'R.id.listview'");
                    }
                    throw new RuntimeException("Content has view with id attribute 'R.id.listview' "
                            + "that is not a TomahawkStickyListHeadersListView class");
                }
                mList = (TomahawkStickyListHeadersListView) rawListView;
                registerForContextMenu(mList);
            } else {
                View rawListView = root.findViewById(R.id.gridview);
                if (!(rawListView instanceof GridView)) {
                    if (rawListView == null) {
                        throw new RuntimeException(
                                "Your content must have a GridView whose id attribute is "
                                        + "'R.id.gridview'");
                    }
                    throw new RuntimeException("Content has view with id attribute 'R.id.gridview' "
                            + "that is not a GridView class");
                }
                mGrid = (GridView) rawListView;
                registerForContextMenu(mGrid);
            }
        }
        if (mTomahawkBaseAdapter != null) {
            TomahawkBaseAdapter adapter = mTomahawkBaseAdapter;
            mTomahawkBaseAdapter = null;
            setListAdapter(adapter);
        }
        mHandler.post(mRequestFocus);
    }

    /**
     * Provide the cursor for the list view.
     */
    public void setListAdapter(TomahawkBaseAdapter adapter) {
        mTomahawkBaseAdapter = adapter;
        if (((mTomahawkBaseAdapter instanceof TomahawkGridAdapter) ? mGrid : mList) != null) {
            if (mTomahawkBaseAdapter instanceof TomahawkGridAdapter) {
                mGrid.setAdapter(adapter);
            } else {
                mList.setAdapter(adapter);
            }
        }
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

    /**
     * @return the current scrolling position of the list- or gridView
     */
    public int getListScrollPosition() {
        if (mTomahawkBaseAdapter instanceof TomahawkGridAdapter) {
            return getGridView().getFirstVisiblePosition();
        }
        return mListScrollPosition = getListView().getFirstVisiblePosition();
    }

    /**
     * Set wether or not the breadcrumbNavigationView should be updated
     */
    public void setBreadCrumbNavigationEnabled(boolean breadCrumbNavigationEnabled) {
        this.mBreadCrumbNavigationEnabled = breadCrumbNavigationEnabled;
    }
}
