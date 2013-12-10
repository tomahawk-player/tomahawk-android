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
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.dialogs.ChooseUserPlaylistDialog;
import org.tomahawk.tomahawk_android.dialogs.FakeContextMenuDialog;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FakeContextMenu;
import org.tomahawk.tomahawk_android.views.TomahawkStickyListHeadersListView;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * The base class for {@link AlbumsFragment}, {@link TracksFragment}, {@link ArtistsFragment},
 * {@link UserPlaylistsFragment} and {@link SearchableFragment}. Provides all sorts of functionality
 * to those classes, related to displaying {@link org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}s
 * in whichever needed way.
 */
public class TomahawkFragment extends TomahawkListFragment
        implements LoaderManager.LoaderCallbacks<Collection>, FakeContextMenu,
        AdapterView.OnItemLongClickListener {

    public static final String TOMAHAWK_ALBUM_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_album_id";

    public static final String TOMAHAWK_ARTIST_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_artist_id";

    public static final String TOMAHAWK_PLAYLIST_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_playlist_id";

    public static final String TOMAHAWK_HUB_ID = "org.tomahawk.tomahawk_android.tomahawk_hub_id";

    public static final String TOMAHAWK_LIST_ITEM_IS_LOCAL
            = "org.tomahawk.tomahawk_list_item_is_local";

    protected TomahawkApp mTomahawkApp;

    private TomahawkFragmentReceiver mTomahawkFragmentReceiver;

    protected ArrayList<String> mCurrentRequestIds = new ArrayList<String>();

    protected InfoSystem mInfoSystem;

    protected PipeLine mPipeline;

    protected HashSet<String> mCorrespondingQueryIds = new HashSet<String>();

    protected int mCorrespondingHubId;

    protected Album mAlbum;

    protected Artist mArtist;

    protected UserPlaylist mUserPlaylist;

    protected boolean mIsLocal = false;

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
     * Basic initializations. Get corresponding hub id through getArguments(), if not null
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTomahawkApp = ((TomahawkApp) mTomahawkMainActivity.getApplication());
        mInfoSystem = mTomahawkApp.getInfoSystem();
        mPipeline = mTomahawkApp.getPipeLine();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getArguments() != null) {
            if (getArguments().containsKey(TOMAHAWK_ALBUM_KEY)
                    && !TextUtils.isEmpty(getArguments().getString(TOMAHAWK_ALBUM_KEY))) {
                mAlbum = Album.getAlbumByKey(getArguments().getString(TOMAHAWK_ALBUM_KEY));
            }
            if (getArguments().containsKey(TOMAHAWK_PLAYLIST_KEY) && !TextUtils.isEmpty(
                    getArguments().getString(TOMAHAWK_PLAYLIST_KEY))) {
                mUserPlaylist = mTomahawkMainActivity.getUserCollection()
                        .getUserPlaylistById(Long.valueOf(getArguments().getString(
                                TOMAHAWK_PLAYLIST_KEY)).longValue());
            }
            if (getArguments() != null && getArguments().containsKey(TOMAHAWK_ARTIST_KEY)
                    && !TextUtils.isEmpty(getArguments().getString(TOMAHAWK_ARTIST_KEY))) {
                mArtist = Artist.getArtistByKey(getArguments().getString(TOMAHAWK_ARTIST_KEY));
            }
            if (getArguments().containsKey(TOMAHAWK_HUB_ID)
                    && getArguments().getInt(TOMAHAWK_HUB_ID) > 0) {
                mCorrespondingHubId = getArguments().getInt(TOMAHAWK_HUB_ID);
            }
            if (getArguments().containsKey(TOMAHAWK_LIST_ITEM_IS_LOCAL)) {
                mIsLocal = getArguments().getBoolean(TOMAHAWK_LIST_ITEM_IS_LOCAL);
            }
        }

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
        TomahawkStickyListHeadersListView list = getListView();
        if (list != null) {
            list.setOnItemLongClickListener(this);
        }
        GridView grid = getGridView();
        if (grid != null) {
            grid.setOnItemLongClickListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mTomahawkFragmentReceiver != null) {
            getActivity().unregisterReceiver(mTomahawkFragmentReceiver);
            mTomahawkFragmentReceiver = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        adaptColumnCount();
    }

    /**
     * Insert our FakeContextMenuDialog initialization here
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        String[] menuItemTitles;
        TomahawkBaseAdapter.TomahawkListItem tomahawkListItem;
        position -= getListView().getHeaderViewsCount();
        Adapter adapter = isShowGridView() ? getGridAdapter() : getListAdapter();
        if (position >= 0) {
            tomahawkListItem = ((TomahawkBaseAdapter.TomahawkListItem) adapter
                    .getItem(position));
        } else {
            if (isShowGridView()) {
                return false;
            }
            tomahawkListItem = ((TomahawkListAdapter) adapter)
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
        new FakeContextMenuDialog(menuItemTitles, tomahawkListItem, this)
                .show(mTomahawkMainActivity.getSupportFragmentManager(), null);
        return true;
    }

    /**
     * If the user clicks on a fakeContextItem, handle what should be done here
     *
     * @param tomahawkMainActivity the {@link org.tomahawk.tomahawk_android.activities.TomahawkMainActivity}
     *                             to get the {@link org.tomahawk.libtomahawk.collection.UserCollection}
     *                             etc from
     * @param menuItemTitle        the menu item title of the clicked item
     * @param tomahawkListItem     the item that has been clicked
     */
    @Override
    public void onFakeContextItemSelected(TomahawkMainActivity tomahawkMainActivity,
            String menuItemTitle, TomahawkBaseAdapter.TomahawkListItem tomahawkListItem) {
        UserCollection userCollection = tomahawkMainActivity.getUserCollection();
        ArrayList<Query> queries = new ArrayList<Query>();
        PlaybackService playbackService = tomahawkMainActivity.getPlaybackService();
        if (menuItemTitle.equals(tomahawkMainActivity.getResources()
                .getString(R.string.fake_context_menu_delete))) {
            if (tomahawkListItem instanceof UserPlaylist) {
                ((TomahawkApp) tomahawkMainActivity.getApplication()).getUserPlaylistsDataSource()
                        .deleteUserPlaylist(((UserPlaylist) tomahawkListItem).getId());
                userCollection.updateUserPlaylists();
            } else if (tomahawkListItem instanceof Query && mUserPlaylist != null) {
                ((TomahawkApp) tomahawkMainActivity.getApplication()).getUserPlaylistsDataSource()
                        .deleteQueryInUserPlaylist(mUserPlaylist.getId(), (Query) tomahawkListItem);
                userCollection.updateUserPlaylists();
            } else if (playbackService != null && this instanceof PlaybackFragment
                    && tomahawkListItem instanceof Query) {
                if (TomahawkUtils.getCacheKey(playbackService.getCurrentTrack()) == TomahawkUtils
                        .getCacheKey(tomahawkListItem)) {
                    boolean wasPlaying = playbackService.isPlaying();
                    if (wasPlaying) {
                        playbackService.pause();
                    }
                    if (playbackService.getCurrentPlaylist().peekQueryAtPos(
                            playbackService.getCurrentPlaylist().getCurrentQueryIndex()
                                    + 1) != null) {
                        playbackService.setCurrentQuery(
                                playbackService.getCurrentPlaylist().getQueryAtPos(
                                        playbackService.getCurrentPlaylist()
                                                .getCurrentQueryIndex() + 1));
                        if (wasPlaying) {
                            playbackService.start();
                        }
                    } else if (playbackService.getCurrentPlaylist().peekQueryAtPos(
                            playbackService.getCurrentPlaylist().getCurrentQueryIndex()
                                    - 1) != null) {
                        playbackService.setCurrentQuery(
                                playbackService.getCurrentPlaylist().getQueryAtPos(
                                        playbackService.getCurrentPlaylist()
                                                .getCurrentQueryIndex() - 1));
                        if (wasPlaying) {
                            playbackService.start();
                        }
                    }
                }
                playbackService.deleteQuery((Query) tomahawkListItem);
            }
        } else if (menuItemTitle
                .equals(tomahawkMainActivity.getResources()
                        .getString(R.string.fake_context_menu_play))) {
            if (this instanceof PlaybackFragment) {
                if (playbackService != null && tomahawkListItem instanceof Query
                        && playbackService.getCurrentPlaylist().getCurrentQuery() != null) {
                    if (TomahawkUtils
                            .getCacheKey(playbackService.getCurrentPlaylist().getCurrentQuery())
                            == TomahawkUtils.getCacheKey(tomahawkListItem)) {
                        if (!playbackService.isPlaying()) {
                            playbackService.start();
                        }
                    } else {
                        playbackService.setCurrentQuery((Query) tomahawkListItem);
                        playbackService.getCurrentPlaylist()
                                .setCurrentQuery((Query) tomahawkListItem);
                        playbackService.start();
                    }
                }
            } else {
                UserPlaylist playlist = null;
                if (tomahawkListItem instanceof Query) {
                    if (mAlbum != null) {
                        if (mIsLocal) {
                            queries = mAlbum.getLocalQueries();
                        } else {
                            queries = mAlbum.getQueries();
                        }
                        playlist = UserPlaylist
                                .fromQueryList(
                                        UserPlaylistsDataSource.CACHED_PLAYLIST_NAME, queries,
                                        queries.indexOf(tomahawkListItem));
                    } else if (mArtist != null) {
                        if (mIsLocal) {
                            queries = mArtist.getLocalQueries();
                        } else {
                            queries = mArtist.getQueries();
                        }
                        playlist = UserPlaylist
                                .fromQueryList(
                                        UserPlaylistsDataSource.CACHED_PLAYLIST_NAME, queries,
                                        queries.indexOf(tomahawkListItem));
                    } else if (mUserPlaylist != null) {
                        queries = mUserPlaylist.getQueries();
                        playlist = UserPlaylist
                                .fromQueryList(
                                        UserPlaylistsDataSource.CACHED_PLAYLIST_NAME, queries,
                                        queries.indexOf(tomahawkListItem));
                    } else {
                        queries.add((Query) tomahawkListItem);
                        playlist = UserPlaylist
                                .fromQueryList(
                                        UserPlaylistsDataSource.CACHED_PLAYLIST_NAME, queries);
                    }
                    userCollection.setCachedUserPlaylist(playlist);
                } else if (tomahawkListItem instanceof UserPlaylist) {
                    playlist = (UserPlaylist) tomahawkListItem;
                } else if (tomahawkListItem instanceof Album) {
                    playlist = UserPlaylist.fromQueryList(
                            UserPlaylistsDataSource.CACHED_PLAYLIST_NAME,
                            ((Album) tomahawkListItem).getQueries());
                } else if (tomahawkListItem instanceof Artist) {
                    playlist = UserPlaylist.fromQueryList(
                            UserPlaylistsDataSource.CACHED_PLAYLIST_NAME,
                            ((Artist) tomahawkListItem).getQueries());
                }
                if (playbackService != null) {
                    playbackService.setCurrentPlaylist(playlist);
                    playbackService.start();
                }
                tomahawkMainActivity.getContentViewer()
                        .setCurrentHubId(TomahawkMainActivity.HUB_ID_PLAYBACK);
            }
        } else if (menuItemTitle.equals(tomahawkMainActivity.getResources()
                .getString(R.string.fake_context_menu_playaftercurrenttrack))) {
            if (tomahawkListItem instanceof Query) {
                queries.add((Query) tomahawkListItem);
            } else if (tomahawkListItem instanceof UserPlaylist) {
                queries = ((UserPlaylist) tomahawkListItem).getQueries();
            } else if (tomahawkListItem instanceof Album) {
                if (mIsLocal) {
                    queries = ((Album) tomahawkListItem).getLocalQueries();
                } else {
                    queries = ((Album) tomahawkListItem).getQueries();
                }
            } else if (tomahawkListItem instanceof Artist) {
                if (mIsLocal) {
                    queries = ((Artist) tomahawkListItem).getLocalQueries();
                } else {
                    queries = ((Artist) tomahawkListItem).getQueries();
                }
            }
            if (playbackService != null) {
                if (playbackService.getCurrentPlaylist() != null) {
                    playbackService.addTracksToCurrentPlaylist(
                            playbackService.getCurrentPlaylist().getCurrentQueryIndex() + 1,
                            queries);
                } else {
                    playbackService.addQueriesToCurrentPlaylist(queries);
                }
            }
        } else if (menuItemTitle.equals(tomahawkMainActivity.getResources()
                .getString(R.string.fake_context_menu_appendtoplaybacklist))) {
            if (tomahawkListItem instanceof Query) {
                queries.add((Query) tomahawkListItem);
            } else if (tomahawkListItem instanceof UserPlaylist) {
                queries = ((UserPlaylist) tomahawkListItem).getQueries();
            } else if (tomahawkListItem instanceof Album) {
                if (mIsLocal) {
                    queries = ((Album) tomahawkListItem).getLocalQueries();
                } else {
                    queries = ((Album) tomahawkListItem).getQueries();
                }
            } else if (tomahawkListItem instanceof Artist) {
                if (mIsLocal) {
                    queries = ((Artist) tomahawkListItem).getLocalQueries();
                } else {
                    queries = ((Artist) tomahawkListItem).getQueries();
                }
            }
            if (playbackService != null) {
                playbackService.addQueriesToCurrentPlaylist(queries);
            }
        } else if (menuItemTitle
                .equals(tomahawkMainActivity.getResources()
                        .getString(R.string.fake_context_menu_addtoplaylist))) {
            if (tomahawkListItem instanceof Query) {
                queries.add((Query) tomahawkListItem);
            } else if (tomahawkListItem instanceof UserPlaylist) {
                queries = ((UserPlaylist) tomahawkListItem).getQueries();
            } else if (tomahawkListItem instanceof Album) {
                if (mIsLocal) {
                    queries = ((Album) tomahawkListItem).getLocalQueries();
                } else {
                    queries = ((Album) tomahawkListItem).getQueries();
                }
            } else if (tomahawkListItem instanceof Artist) {
                if (mIsLocal) {
                    queries = ((Artist) tomahawkListItem).getLocalQueries();
                } else {
                    queries = ((Artist) tomahawkListItem).getQueries();
                }
            }
            new ChooseUserPlaylistDialog(userCollection, queries)
                    .show(tomahawkMainActivity.getSupportFragmentManager(),
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
        if (isShowGridView()) {
            getGridAdapter().notifyDataSetChanged();
        } else {
            ((TomahawkListAdapter) getListAdapter()).notifyDataSetChanged();
        }
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
