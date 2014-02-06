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
import org.tomahawk.libtomahawk.collection.Playlist;
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
import org.tomahawk.tomahawk_android.utils.ContentViewer;
import org.tomahawk.tomahawk_android.utils.FakeContextMenu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * The base class for {@link AlbumsFragment}, {@link TracksFragment}, {@link ArtistsFragment},
 * {@link UserPlaylistsFragment} and {@link SearchableFragment}. Provides all sorts of functionality
 * to those classes, related to displaying {@link org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}s
 * in whichever needed way.
 */
public class TomahawkFragment extends TomahawkListFragment
        implements LoaderManager.LoaderCallbacks<Collection>, FakeContextMenu,
        AdapterView.OnItemLongClickListener, AbsListView.OnScrollListener {

    public static final String TOMAHAWK_ALBUM_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_album_id";

    public static final String TOMAHAWK_ARTIST_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_artist_id";

    public static final String TOMAHAWK_USER_PLAYLIST_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_user_playlist_id";

    public static final String TOMAHAWK_HUB_ID = "org.tomahawk.tomahawk_android.tomahawk_hub_id";

    public static final String TOMAHAWK_LIST_ITEM_IS_LOCAL
            = "org.tomahawk.tomahawk_list_item_is_local";

    private static final int PIPELINE_RESULT_REPORTER_MSG = 1337;

    private static final long PIPELINE_RESULT_REPORTER_DELAY = 500;

    private TomahawkBaseFragmentReceiver mTomahawkBaseFragmentReceiver;

    protected HashSet<String> mCurrentRequestIds = new HashSet<String>();

    protected InfoSystem mInfoSystem;

    protected PipeLine mPipeline;

    protected HashSet<String> mCorrespondingQueryIds = new HashSet<String>();

    protected ArrayList<Query> mShownQueries = new ArrayList<Query>();

    protected ArrayList<Album> mShownAlbums = new ArrayList<Album>();

    protected ArrayList<Artist> mShownArtists = new ArrayList<Artist>();

    protected int mCorrespondingHubId;

    protected Album mAlbum;

    protected Artist mArtist;

    protected UserPlaylist mUserPlaylist;

    protected boolean mIsLocal = false;

    private ArrayList<String> mQueryKeysToReport = new ArrayList<String>();

    // Handler which reports the PipeLine's results
    private final Handler mPipeLineResultReporter = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            removeMessages(msg.what);
            ArrayList<String> queryKeys;
            synchronized (TomahawkFragment.this) {
                queryKeys = new ArrayList<String>(mQueryKeysToReport);
                mQueryKeysToReport.clear();
            }
            onPipeLineResultsReported(queryKeys);
        }
    };

    /**
     * Handles incoming {@link Collection} updated broadcasts.
     */
    private class TomahawkBaseFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Collection.COLLECTION_UPDATED.equals(intent.getAction())) {
                onCollectionUpdated();
            } else if (PipeLine.PIPELINE_RESULTSREPORTED.equals(intent.getAction())) {
                String queryKey = intent.getStringExtra(PipeLine.PIPELINE_RESULTSREPORTED_QUERYKEY);
                synchronized (TomahawkFragment.this) {
                    mQueryKeysToReport.add(queryKey);
                }
                if (!mPipeLineResultReporter.hasMessages(PIPELINE_RESULT_REPORTER_MSG)) {
                    mPipeLineResultReporter.sendEmptyMessageDelayed(PIPELINE_RESULT_REPORTER_MSG,
                            PIPELINE_RESULT_REPORTER_DELAY);
                }
            } else if (InfoSystem.INFOSYSTEM_RESULTSREPORTED.equals(intent.getAction())) {
                String requestId = intent.getStringExtra(
                        InfoSystem.INFOSYSTEM_RESULTSREPORTED_REQUESTID);
                onInfoSystemResultsReported(requestId);
            } else if (TomahawkMainActivity.PLAYBACKSERVICE_READY.equals(intent.getAction())) {
                onPlaybackServiceReady();
            } else if (PlaybackService.BROADCAST_NEWTRACK.equals(intent.getAction())) {
                onTrackChanged();
                mTomahawkMainActivity.startLoadingAnimation();
            } else if (PlaybackService.BROADCAST_PLAYLISTCHANGED.equals(intent.getAction())) {
                onPlaylistChanged();
            } else if (PlaybackService.BROADCAST_PLAYSTATECHANGED.equals(intent.getAction())) {
                onPlaystateChanged();
            }
        }
    }

    /**
     * Basic initializations. Get corresponding hub id through getArguments(), if not null
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                if (!mAlbum.isResolvedByInfoSystem()) {
                    mCurrentRequestIds.add(mInfoSystem.resolve(mAlbum));
                }
                mTomahawkMainActivity.startLoadingAnimation();
            }
            if (getArguments().containsKey(TOMAHAWK_USER_PLAYLIST_KEY) && !TextUtils.isEmpty(
                    getArguments().getString(TOMAHAWK_USER_PLAYLIST_KEY))) {
                mUserPlaylist = mTomahawkMainActivity.getUserCollection()
                        .getUserPlaylistById(getArguments().getString(TOMAHAWK_USER_PLAYLIST_KEY));
                if (mUserPlaylist.getContentHeaderArtists().size() == 0) {
                    final HashMap<Artist, Integer> countMap = new HashMap<Artist, Integer>();
                    for (Query query : mUserPlaylist.getQueries()) {
                        Artist artist = query.getArtist();
                        if (countMap.containsKey(artist)) {
                            countMap.put(artist, countMap.get(artist) + 1);
                        } else {
                            countMap.put(artist, 1);
                        }
                    }
                    TreeMap<Artist, Integer> sortedCountMap = new TreeMap<Artist, Integer>(
                            new Comparator<Artist>() {
                                @Override
                                public int compare(Artist lhs, Artist rhs) {
                                    return countMap.get(lhs) >= countMap.get(rhs) ? -1 : 1;
                                }
                            }
                    );
                    sortedCountMap.putAll(countMap);
                    for (Artist artist : sortedCountMap.keySet()) {
                        mUserPlaylist.addContentHeaderArtists(artist);
                        if (!artist.isResolvedByInfoSystem()) {
                            ArrayList<String> requestIds = mInfoSystem.resolve(artist, true);
                            for (String requestId : requestIds) {
                                mCurrentRequestIds.add(requestId);
                            }
                        }
                        if (mUserPlaylist.getContentHeaderArtists().size() == 10) {
                            break;
                        }
                    }
                }
            }
            if (getArguments().containsKey(TOMAHAWK_ARTIST_KEY) && !TextUtils
                    .isEmpty(getArguments().getString(TOMAHAWK_ARTIST_KEY))) {
                mArtist = Artist.getArtistByKey(getArguments().getString(TOMAHAWK_ARTIST_KEY));
                if (!mArtist.isResolvedByInfoSystem()) {
                    ArrayList<String> requestIds = mInfoSystem.resolve(mArtist, false);
                    for (String requestId : requestIds) {
                        mCurrentRequestIds.add(requestId);
                    }
                }
                mTomahawkMainActivity.startLoadingAnimation();
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

        mTomahawkMainActivity.getSupportLoaderManager().destroyLoader(getId());
        mTomahawkMainActivity.getSupportLoaderManager().initLoader(getId(), null, this);

        // Initialize and register Receiver
        if (mTomahawkBaseFragmentReceiver == null) {
            mTomahawkBaseFragmentReceiver = new TomahawkBaseFragmentReceiver();
            IntentFilter intentFilter = new IntentFilter(Collection.COLLECTION_UPDATED);
            mTomahawkMainActivity.registerReceiver(mTomahawkBaseFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(PipeLine.PIPELINE_RESULTSREPORTED);
            mTomahawkMainActivity.registerReceiver(mTomahawkBaseFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(InfoSystem.INFOSYSTEM_RESULTSREPORTED);
            mTomahawkMainActivity.registerReceiver(mTomahawkBaseFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(PlaybackService.BROADCAST_NEWTRACK);
            mTomahawkMainActivity.registerReceiver(mTomahawkBaseFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(PlaybackService.BROADCAST_PLAYLISTCHANGED);
            mTomahawkMainActivity.registerReceiver(mTomahawkBaseFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(PlaybackService.BROADCAST_PLAYSTATECHANGED);
            mTomahawkMainActivity.registerReceiver(mTomahawkBaseFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(TomahawkMainActivity.PLAYBACKSERVICE_READY);
            mTomahawkMainActivity.registerReceiver(mTomahawkBaseFragmentReceiver, intentFilter);
        }
        StickyListHeadersListView list = getListView();
        if (list != null) {
            list.setOnItemLongClickListener(this);
            list.setOnScrollListener(this);
        }
        GridView grid = getGridView();
        if (grid != null) {
            grid.setOnItemLongClickListener(this);
            grid.setOnScrollListener(this);
        }

        onPlaylistChanged();
    }

    @Override
    public void onPause() {
        super.onPause();

        mPipeLineResultReporter.removeCallbacksAndMessages(null);

        if (mTomahawkBaseFragmentReceiver != null) {
            mTomahawkMainActivity.unregisterReceiver(mTomahawkBaseFragmentReceiver);
            mTomahawkBaseFragmentReceiver = null;
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
        if (tomahawkListItem instanceof UserPlaylist) {
            if (((UserPlaylist) tomahawkListItem).isHatchetPlaylist()) {
                menuItemTitles = getResources().getStringArray(
                        R.array.fake_context_menu_items_without_addplaylist_and_delete);
            } else {
                menuItemTitles = getResources().getStringArray(
                        R.array.fake_context_menu_items_without_addplaylist);
            }
        } else if (tomahawkListItem instanceof Query && (this instanceof PlaybackFragment
                || (mUserPlaylist != null && !mUserPlaylist.isHatchetPlaylist()))) {
            menuItemTitles = getResources().getStringArray(R.array.fake_context_menu_items);
        } else {
            menuItemTitles = getResources().getStringArray(
                    R.array.fake_context_menu_items_without_delete);
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
                mUserPlaylist = userCollection.getUserPlaylistById(mUserPlaylist.getId());
                updateAdapter();
            } else if (playbackService != null && this instanceof PlaybackFragment
                    && tomahawkListItem instanceof Query) {
                if (TomahawkUtils.getCacheKey(playbackService.getCurrentTrack())
                        .equals(TomahawkUtils.getCacheKey(tomahawkListItem))) {
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
                    if (TomahawkUtils.getCacheKey(
                            playbackService.getCurrentPlaylist().getCurrentQuery())
                            .equals(TomahawkUtils.getCacheKey(tomahawkListItem))) {
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
                    } else if (mArtist != null) {
                        if (mIsLocal) {
                            queries = mArtist.getLocalQueries();
                        } else {
                            queries = mArtist.getQueries();
                        }
                    } else if (mUserPlaylist != null) {
                        queries = mUserPlaylist.getQueries();
                    } else {
                        queries.add((Query) tomahawkListItem);
                    }
                    playlist = UserPlaylist
                            .fromQueryList(UserPlaylistsDataSource.CACHED_PLAYLIST_ID,
                                    UserPlaylistsDataSource.CACHED_PLAYLIST_NAME, queries,
                                    (Query) tomahawkListItem);
                } else if (tomahawkListItem instanceof UserPlaylist) {
                    playlist = (UserPlaylist) tomahawkListItem;
                } else if (tomahawkListItem instanceof Album) {
                    playlist = UserPlaylist
                            .fromQueryList(UserPlaylistsDataSource.CACHED_PLAYLIST_ID,
                                    UserPlaylistsDataSource.CACHED_PLAYLIST_NAME,
                                    ((Album) tomahawkListItem).getQueries());
                } else if (tomahawkListItem instanceof Artist) {
                    playlist = UserPlaylist
                            .fromQueryList(UserPlaylistsDataSource.CACHED_PLAYLIST_ID,
                                    UserPlaylistsDataSource.CACHED_PLAYLIST_NAME,
                                    ((Artist) tomahawkListItem).getQueries());
                }
                if (playbackService != null) {
                    playbackService.setCurrentPlaylist(playlist);
                    playbackService.start();
                }
                mTomahawkApp.getContentViewer().showHub(ContentViewer.HUB_ID_PLAYBACK);
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
            int screenLayout = getResources().getConfiguration().screenLayout;
            screenLayout &= Configuration.SCREENLAYOUT_SIZE_MASK;
            if (getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE) {
                if (screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE
                        || screenLayout == 4) {
                    getGridView().setNumColumns(4);
                } else {
                    getGridView().setNumColumns(3);
                }
            } else {
                if (screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE
                        || screenLayout == 4) {
                    getGridView().setNumColumns(3);
                } else {
                    getGridView().setNumColumns(2);
                }
            }
        }
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkBaseAdapter} content
     */
    protected void updateAdapter() {
    }

    /**
     * If the PlaybackService signals, that it is ready, this method is being called
     */
    protected void onPlaybackServiceReady() {
    }

    protected void onPipeLineResultsReported(ArrayList<String> queryKeys) {
    }

    protected void onInfoSystemResultsReported(String requestId) {
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver received a Broadcast indicating that the
     * playlist has changed inside our PlaybackService
     */
    protected void onPlaylistChanged() {
        updateShowPlaystate();
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver in PlaybackFragment received a Broadcast
     * indicating that the playState (playing or paused) has changed inside our PlaybackService
     */
    protected void onPlaystateChanged() {
        updateShowPlaystate();
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver received a Broadcast indicating that the
     * track has changed inside our PlaybackService
     */
    protected void onTrackChanged() {
        updateShowPlaystate();
    }

    public boolean shouldShowPlaystate() {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        if (playbackService != null) {
            Playlist playlist = playbackService.getCurrentPlaylist();
            if (playlist != null && playlist.getCount() == mShownQueries.size()) {
                for (int i = 0; i < playlist.getCount(); i++) {
                    if (!TomahawkUtils.getCacheKey(playlist.peekQueryAtPos(i))
                            .equals(TomahawkUtils.getCacheKey(mShownQueries.get(i)))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    protected void updateShowPlaystate() {
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        if (getListAdapter() instanceof TomahawkListAdapter) {
            TomahawkListAdapter tomahawkListAdapter = (TomahawkListAdapter) getListAdapter();
            if (shouldShowPlaystate() && playbackService != null) {
                tomahawkListAdapter.setShowPlaystate(true);
                tomahawkListAdapter.setHighlightedItem(
                        playbackService.getCurrentPlaylist().getCurrentQueryIndex()
                                + mShownAlbums.size() + mShownArtists.size());
                tomahawkListAdapter.setHighlightedItemIsPlaying(playbackService.isPlaying());
            } else {
                tomahawkListAdapter.setShowPlaystate(false);
            }
            tomahawkListAdapter.notifyDataSetChanged();
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

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (!(this instanceof SearchableFragment)) {
            resolveQueriesFromTo(firstVisibleItem, firstVisibleItem + visibleItemCount + 2);
        }
    }

    protected void resolveQueriesFromTo(int start, int end) {
        ArrayList<Query> qs = new ArrayList<Query>();
        for (int i = start; i < end; i++) {
            if (i >= 0 && i < mShownQueries.size()) {
                Query q = mShownQueries.get(i);
                if (!q.isSolved() && !mCorrespondingQueryIds
                        .contains(TomahawkUtils.getCacheKey(q))) {
                    qs.add(q);
                }
            }
        }
        if (!qs.isEmpty()) {
            HashSet<String> qids = mPipeline.resolve(qs);
            mCorrespondingQueryIds.addAll(qids);
            mTomahawkMainActivity.startLoadingAnimation();
        }
    }
}
