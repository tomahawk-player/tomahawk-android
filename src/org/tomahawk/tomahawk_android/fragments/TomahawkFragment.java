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
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.dialogs.FakeContextMenuDialog;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.SparseIntArray;
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
 * to those classes, related to displaying {@link TomahawkListItem}s in whichever needed way.
 */
public class TomahawkFragment extends TomahawkListFragment
        implements AdapterView.OnItemLongClickListener, AbsListView.OnScrollListener,
        View.OnLongClickListener {

    public static final String TOMAHAWK_ALBUM_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_album_id";

    public static final String TOMAHAWK_ARTIST_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_artist_id";

    public static final String TOMAHAWK_USERPLAYLIST_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_userplaylist_id";

    public static final String TOMAHAWK_USER_ID
            = "org.tomahawk.tomahawk_android.tomahawk_user_id";

    public static final String TOMAHAWK_SOCIALACTION_ID
            = "org.tomahawk.tomahawk_android.tomahawk_socialaction_id";

    public static final String TOMAHAWK_QUERY_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_query_id";

    public static final String TOMAHAWK_QUERYKEYSARRAY_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_querykeysarray_id";

    public static final String TOMAHAWK_AUTHENTICATORID_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_authenticatorid_id";

    public static final String TOMAHAWK_SHOWDELETE_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_showdelete_key";

    public static final String TOMAHAWK_TOMAHAWKLISTITEM_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_tomahawklistitem_id";

    public static final String TOMAHAWK_TOMAHAWKLISTITEM_TYPE
            = "org.tomahawk.tomahawk_android.tomahawk_tomahawklistitem_type";

    public static final String TOMAHAWK_FROMPLAYBACKFRAGMENT
            = "org.tomahawk.tomahawk_android.tomahawk_fromplaybackfragment";

    public static final String TOMAHAWK_LIST_ITEM_IS_LOCAL
            = "org.tomahawk.tomahawk_list_item_is_local";

    public static final String TOMAHAWK_LIST_ITEM_POSITION
            = "org.tomahawk.tomahawk_android.tomahawk_list_item_position";

    private static final int RESOLVE_QUERIES_REPORTER_MSG = 1336;

    private static final long RESOLVE_QUERIES_REPORTER_DELAY = 100;

    private static final int PIPELINE_RESULT_REPORTER_MSG = 1337;

    private static final long PIPELINE_RESULT_REPORTER_DELAY = 1000;

    private TomahawkFragmentReceiver mTomahawkFragmentReceiver;

    protected boolean isResumed;

    protected HashSet<String> mCurrentRequestIds = new HashSet<String>();

    protected HashSet<String> mCorrespondingQueryIds = new HashSet<String>();

    protected SparseIntArray mQueryPositions = new SparseIntArray();

    protected ArrayList<Query> mShownQueries = new ArrayList<Query>();

    protected ArrayList<Album> mShownAlbums = new ArrayList<Album>();

    protected ArrayList<Artist> mShownArtists = new ArrayList<Artist>();

    protected ArrayList<User> mShownUsers = new ArrayList<User>();

    protected Album mAlbum;

    protected Artist mArtist;

    protected UserPlaylist mUserPlaylist;

    protected User mUser;

    protected boolean mIsLocal = false;

    private int mFirstVisibleItemLastTime = 0;

    private int mVisibleItemCount = 0;

    private final Handler mResolveQueriesHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            removeMessages(msg.what);
            resolveVisibleQueries();
        }
    };

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
     * Handles incoming broadcasts.
     */
    private class TomahawkFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (UserCollection.COLLECTION_UPDATED.equals(intent.getAction())) {
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
            } else if (PlaybackService.BROADCAST_CURRENTTRACKCHANGED.equals(intent.getAction())) {
                onTrackChanged();
            } else if (PlaybackService.BROADCAST_PLAYLISTCHANGED.equals(intent.getAction())) {
                onPlaylistChanged();
            } else if (PlaybackService.BROADCAST_PLAYSTATECHANGED.equals(intent.getAction())) {
                onPlaystateChanged();
            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                boolean noConnectivity =
                        intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (!noConnectivity && !(TomahawkFragment.this instanceof SearchableFragment)) {
                    mCorrespondingQueryIds.clear();
                    resolveVisibleQueries();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        if (getArguments() != null) {
            if (getArguments().containsKey(TOMAHAWK_ALBUM_KEY)
                    && !TextUtils.isEmpty(getArguments().getString(TOMAHAWK_ALBUM_KEY))) {
                mAlbum = Album.getAlbumByKey(getArguments().getString(TOMAHAWK_ALBUM_KEY));
                if (mAlbum == null) {
                    getActivity().getSupportFragmentManager().beginTransaction().remove(this)
                            .commit();
                }
                mCurrentRequestIds.add(InfoSystem.getInstance().resolve(mAlbum));
            }
            if (getArguments().containsKey(TOMAHAWK_USERPLAYLIST_KEY) && !TextUtils.isEmpty(
                    getArguments().getString(TOMAHAWK_USERPLAYLIST_KEY))) {
                mUserPlaylist = UserPlaylist
                        .getUserPlaylistById(getArguments().getString(TOMAHAWK_USERPLAYLIST_KEY));
                if (mUserPlaylist == null) {
                    getActivity().getSupportFragmentManager().beginTransaction().remove(this)
                            .commit();
                } else if (mUserPlaylist.getContentHeaderArtists().size() == 0) {
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
                        ArrayList<String> requestIds = InfoSystem.getInstance().resolve(artist,
                                true);
                        for (String requestId : requestIds) {
                            mCurrentRequestIds.add(requestId);
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
                if (mArtist == null) {
                    getActivity().getSupportFragmentManager().beginTransaction().remove(this)
                            .commit();
                }
                ArrayList<String> requestIds = InfoSystem.getInstance().resolve(mArtist, false);
                for (String requestId : requestIds) {
                    mCurrentRequestIds.add(requestId);
                }
            }
            if (getArguments().containsKey(TOMAHAWK_USER_ID) && !TextUtils
                    .isEmpty(getArguments().getString(TOMAHAWK_USER_ID))) {
                mUser = User.getUserById(getArguments().getString(TOMAHAWK_USER_ID));
                if (mUser == null) {
                    getActivity().getSupportFragmentManager().beginTransaction().remove(this)
                            .commit();
                }
                mCurrentRequestIds.add(InfoSystem.getInstance().resolve(mUser));
            }
            if (getArguments().containsKey(TOMAHAWK_LIST_ITEM_IS_LOCAL)) {
                mIsLocal = getArguments().getBoolean(TOMAHAWK_LIST_ITEM_IS_LOCAL);
            }
        }

        // Adapt to current orientation. Show different count of columns in the GridView
        adaptColumnCount();

        // Initialize and register Receiver
        if (mTomahawkFragmentReceiver == null) {
            mTomahawkFragmentReceiver = new TomahawkFragmentReceiver();
            IntentFilter intentFilter = new IntentFilter(UserCollection.COLLECTION_UPDATED);
            activity.registerReceiver(mTomahawkFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(PipeLine.PIPELINE_RESULTSREPORTED);
            activity.registerReceiver(mTomahawkFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(InfoSystem.INFOSYSTEM_RESULTSREPORTED);
            activity.registerReceiver(mTomahawkFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(PlaybackService.BROADCAST_CURRENTTRACKCHANGED);
            activity.registerReceiver(mTomahawkFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(PlaybackService.BROADCAST_PLAYLISTCHANGED);
            activity.registerReceiver(mTomahawkFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(PlaybackService.BROADCAST_PLAYSTATECHANGED);
            activity.registerReceiver(mTomahawkFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(TomahawkMainActivity.PLAYBACKSERVICE_READY);
            activity.registerReceiver(mTomahawkFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            activity.registerReceiver(mTomahawkFragmentReceiver, intentFilter);
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
        View contentHeaderFrame = getView().findViewById(R.id.content_header_image_frame);
        if (contentHeaderFrame != null) {
            contentHeaderFrame.setOnLongClickListener(this);
        }

        onPlaylistChanged();

        isResumed = true;
    }

    @Override
    public void onPause() {
        super.onPause();

        mPipeLineResultReporter.removeCallbacksAndMessages(null);

        if (mTomahawkFragmentReceiver != null) {
            getActivity().unregisterReceiver(mTomahawkFragmentReceiver);
            mTomahawkFragmentReceiver = null;
        }

        isResumed = false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        adaptColumnCount();
    }

    @Override
    public boolean onLongClick(View view) {
        if (view == getView().findViewById(R.id.content_header_image_frame)) {
            return onItemLongClick(null, view, -1, 0);
        } else { //assume click on PlaybackFragment's albumart viewpager
            Query query = ((TomahawkMainActivity) getActivity()).getPlaybackService()
                    .getCurrentQuery();
            FakeContextMenuDialog dialog = new FakeContextMenuDialog();
            Bundle args = new Bundle();
            args.putBoolean(TOMAHAWK_SHOWDELETE_KEY, false);
            args.putBoolean(TOMAHAWK_LIST_ITEM_IS_LOCAL, mIsLocal);
            args.putBoolean(TOMAHAWK_FROMPLAYBACKFRAGMENT, this instanceof PlaybackFragment);
            if (mAlbum != null) {
                args.putString(TOMAHAWK_ALBUM_KEY, TomahawkUtils.getCacheKey(mAlbum));
            } else if (mUserPlaylist != null) {
                args.putString(TOMAHAWK_USERPLAYLIST_KEY, mUserPlaylist.getId());
            } else if (mArtist != null) {
                args.putString(TOMAHAWK_ARTIST_KEY, TomahawkUtils.getCacheKey(mArtist));
            }
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_KEY, TomahawkUtils.getCacheKey(query));
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_TYPE, TOMAHAWK_QUERY_KEY);
            dialog.setArguments(args);
            dialog.show(getFragmentManager(), null);
        }
        return true;
    }

    /**
     * Insert our FakeContextMenuDialog initialization here
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        TomahawkListItem tomahawkListItem;
        position -= getListView().getHeaderViewsCount();
        Adapter adapter = isShowGridView() ? getGridAdapter() : getListAdapter();
        if (position >= 0) {
            tomahawkListItem = ((TomahawkListItem) adapter.getItem(position));
        } else {
            if (isShowGridView()) {
                return false;
            }
            tomahawkListItem = ((TomahawkListAdapter) adapter).getContentHeaderTomahawkListItem();
        }
        boolean showDelete = true;
        if ((tomahawkListItem instanceof SocialAction
                && ((SocialAction) tomahawkListItem).getTargetObject() instanceof User)
                || tomahawkListItem instanceof User) {
            return false;
        }
        if (tomahawkListItem instanceof UserPlaylist
                && !((UserPlaylist) tomahawkListItem).isHatchetPlaylist()) {
            showDelete = false;
        } else if (!(this instanceof PlaybackFragment)) {
            if (!(mUserPlaylist == null || mUserPlaylist.isHatchetPlaylist()
                    || DatabaseHelper.LOVEDITEMS_PLAYLIST_ID
                    .equals(mUserPlaylist.getId()))) {
                showDelete = false;
            }
        } else if (tomahawkListItem instanceof Query
                && !((Query) tomahawkListItem).isCurrentlyPlaying()) {
            showDelete = false;
        }
        FakeContextMenuDialog dialog = new FakeContextMenuDialog();
        Bundle args = new Bundle();
        args.putBoolean(TOMAHAWK_SHOWDELETE_KEY, showDelete);
        args.putBoolean(TOMAHAWK_LIST_ITEM_IS_LOCAL, mIsLocal);
        if (position >= 0) {
            args.putInt(TOMAHAWK_LIST_ITEM_POSITION, position);
        }
        args.putBoolean(TOMAHAWK_FROMPLAYBACKFRAGMENT, this instanceof PlaybackFragment);
        if (mAlbum != null) {
            args.putString(TOMAHAWK_ALBUM_KEY, TomahawkUtils.getCacheKey(mAlbum));
        } else if (mUserPlaylist != null) {
            args.putString(TOMAHAWK_USERPLAYLIST_KEY, mUserPlaylist.getId());
        } else if (mArtist != null) {
            args.putString(TOMAHAWK_ARTIST_KEY, TomahawkUtils.getCacheKey(mArtist));
        }
        if (tomahawkListItem instanceof Query) {
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_KEY,
                    TomahawkUtils.getCacheKey(tomahawkListItem));
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_TYPE, TOMAHAWK_QUERY_KEY);
        } else if (tomahawkListItem instanceof Album) {
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_KEY,
                    TomahawkUtils.getCacheKey(tomahawkListItem));
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_TYPE, TOMAHAWK_ALBUM_KEY);
        } else if (tomahawkListItem instanceof Artist) {
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_KEY,
                    TomahawkUtils.getCacheKey(tomahawkListItem));
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_TYPE, TOMAHAWK_ARTIST_KEY);
        } else if (tomahawkListItem instanceof UserPlaylist) {
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_KEY,
                    ((UserPlaylist) tomahawkListItem).getId());
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_TYPE, TOMAHAWK_USERPLAYLIST_KEY);
        } else if (tomahawkListItem instanceof SocialAction) {
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_KEY,
                    ((SocialAction) tomahawkListItem).getId());
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_TYPE, TOMAHAWK_SOCIALACTION_ID);
        }
        dialog.setArguments(args);
        dialog.show(getFragmentManager(), null);
        return true;
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
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    protected void updateAdapter() {
    }

    /**
     * If the PlaybackService signals, that it is ready, this method is being called
     */
    protected void onPlaybackServiceReady() {
        updateShowPlaystate();
    }

    protected void onPipeLineResultsReported(ArrayList<String> queryKeys) {
        for (String key : queryKeys) {
            if (mCorrespondingQueryIds.contains(key)) {
                updateAdapter();
                break;
            }
        }
    }

    protected void onInfoSystemResultsReported(String requestId) {
        if (mCurrentRequestIds.contains(requestId)) {
            updateAdapter();
            resolveVisibleQueries();
        }
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
        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
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
        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (getListAdapter() instanceof TomahawkListAdapter) {
            TomahawkListAdapter tomahawkListAdapter = (TomahawkListAdapter) getListAdapter();
            if (shouldShowPlaystate() && playbackService != null
                    && playbackService.getCurrentPlaylist() != null) {
                tomahawkListAdapter.setShowPlaystate(true);
                tomahawkListAdapter.setHighlightedItem(playbackService.isPlaying(), mQueryPositions
                        .get(playbackService.getCurrentPlaylist().getCurrentQueryIndex()));
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
        updateAdapter();
        resolveVisibleQueries();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        mVisibleItemCount = visibleItemCount;
        if (mFirstVisibleItemLastTime != firstVisibleItem
                && !(this instanceof SearchableFragment)) {
            mFirstVisibleItemLastTime = firstVisibleItem;
            mResolveQueriesHandler.removeCallbacksAndMessages(null);
            mResolveQueriesHandler.sendEmptyMessageDelayed(RESOLVE_QUERIES_REPORTER_MSG,
                    RESOLVE_QUERIES_REPORTER_DELAY);
        }
    }

    protected void resolveVisibleQueries() {
        resolveQueriesFromTo(mFirstVisibleItemLastTime - 5,
                mFirstVisibleItemLastTime + mVisibleItemCount + 5);
    }

    private void resolveQueriesFromTo(final int start, final int end) {
        ArrayList<Query> qs = new ArrayList<Query>();
        for (int i = (start < 0 ? 0 : start); i < end && i < mShownQueries.size(); i++) {
            Query q = mShownQueries.get(i);
            if (!q.isSolved() && !mCorrespondingQueryIds
                    .contains(TomahawkUtils.getCacheKey(q))) {
                qs.add(q);
            }
        }
        if (!qs.isEmpty()) {
            HashSet<String> qids = PipeLine.getInstance().resolve(qs);
            mCorrespondingQueryIds.addAll(qids);
        }
    }

    /**
     * Remove or add a lovedItem-query from the LovedItems-UserPlaylist, depending on whether or not
     * it is already a lovedItem
     */
    protected void toggleLovedItem(Query query) {
        UserCollection.getInstance().toggleLovedItem(query);
        onTrackChanged();
    }
}
