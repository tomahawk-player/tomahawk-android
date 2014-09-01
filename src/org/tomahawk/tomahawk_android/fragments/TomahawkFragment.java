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
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.dialogs.FakeContextMenuDialog;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.MultiColumnClickListener;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * The base class for {@link AlbumsFragment}, {@link TracksFragment}, {@link ArtistsFragment},
 * {@link PlaylistsFragment} and {@link SearchableFragment}. Provides all sorts of functionality to
 * those classes, related to displaying {@link TomahawkListItem}s in whichever needed way.
 */
public abstract class TomahawkFragment extends TomahawkListFragment
        implements MultiColumnClickListener {

    public static final String TOMAHAWK_ALBUM_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_album_id";

    public static final String TOMAHAWK_ARTIST_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_artist_id";

    public static final String TOMAHAWK_PLAYLIST_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_playlist_id";

    public static final String TOMAHAWK_USER_ID
            = "org.tomahawk.tomahawk_android.tomahawk_user_id";

    public static final String TOMAHAWK_SOCIALACTION_ID
            = "org.tomahawk.tomahawk_android.tomahawk_socialaction_id";

    public static final String TOMAHAWK_PLAYLISTENTRY_ID
            = "org.tomahawk.tomahawk_android.tomahawk_playlistentry_id";

    public static final String TOMAHAWK_QUERY_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_query_id";

    public static final String TOMAHAWK_QUERYKEYSARRAY_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_querykeysarray_id";

    public static final String TOMAHAWK_PREFERENCEID_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_preferenceid_key";

    public static final String TOMAHAWK_SHOWDELETE_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_showdelete_key";

    public static final String TOMAHAWK_TOMAHAWKLISTITEM_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_tomahawklistitem_id";

    public static final String TOMAHAWK_TOMAHAWKLISTITEM_TYPE
            = "org.tomahawk.tomahawk_android.tomahawk_tomahawklistitem_type";

    public static final String TOMAHAWK_FROMPLAYBACKFRAGMENT
            = "org.tomahawk.tomahawk_android.tomahawk_fromplaybackfragment";

    public static final String TOMAHAWK_USERNAME_STRING
            = "org.tomahawk.tomahawk_android.tomahawk_username_string";

    public static final String TOMAHAWK_PASSWORD_STRING
            = "org.tomahawk.tomahawk_android.tomahawk_password_string";

    public static final String SHOW_MODE
            = "org.tomahawk.tomahawk_android.show_mode";

    public static final String CONTAINER_FRAGMENT_PAGE
            = "org.tomahawk.tomahawk_android.container_fragment_page";

    public static final String CONTAINER_FRAGMENT_NAME
            = "org.tomahawk.tomahawk_android.container_fragment_name";

    protected static final int RESOLVE_QUERIES_REPORTER_MSG = 1336;

    protected static final long RESOLVE_QUERIES_REPORTER_DELAY = 100;

    protected static final int PIPELINE_RESULT_REPORTER_MSG = 1337;

    protected static final long PIPELINE_RESULT_REPORTER_DELAY = 1000;

    private TomahawkListAdapter mTomahawkListAdapter;

    private TomahawkFragmentReceiver mTomahawkFragmentReceiver;

    protected boolean mIsResumed;

    protected HashSet<String> mCurrentRequestIds = new HashSet<String>();

    protected ConcurrentSkipListSet<String> mCorrespondingQueryIds
            = new ConcurrentSkipListSet<String>();

    protected ArrayList<Query> mShownQueries = new ArrayList<Query>();

    protected ArrayList<Album> mShownAlbums = new ArrayList<Album>();

    protected ArrayList<Artist> mShownArtists = new ArrayList<Artist>();

    protected ArrayList<User> mShownUsers = new ArrayList<User>();

    protected ArrayList<PlaylistEntry> mShownPlaylistEntries = new ArrayList<PlaylistEntry>();

    protected Collection mCollection;

    protected Album mAlbum;

    protected Artist mArtist;

    protected Playlist mPlaylist;

    protected User mUser;

    protected Query mQuery;

    private int mFirstVisibleItemLastTime = 0;

    private int mVisibleItemCount = 0;

    protected int mShowMode;

    protected Class mContainerFragmentClass;

    protected MultiColumnClickListener mStarLoveButtonListener = new MultiColumnClickListener() {
        @Override
        public void onItemClick(View view, TomahawkListItem item) {
            if (item instanceof Artist) {
                CollectionManager.getInstance().toggleLovedItem((Artist) item);
            } else if (item instanceof Album) {
                CollectionManager.getInstance().toggleLovedItem((Album) item);
            } else if (item instanceof Query) {
                CollectionManager.getInstance().toggleLovedItem((Query) item);
            }
        }

        @Override
        public boolean onItemLongClick(View view, TomahawkListItem item) {
            return false;
        }
    };

    protected final Handler mResolveQueriesHandler = new Handler() {
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
            if (CollectionManager.COLLECTION_UPDATED.equals(intent.getAction())) {
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
                    mResolveQueriesHandler.removeCallbacksAndMessages(null);
                    mResolveQueriesHandler.sendEmptyMessage(RESOLVE_QUERIES_REPORTER_MSG);
                }
            } else if (DatabaseHelper.PLAYLISTSDATASOURCE_RESULTSREPORTED
                    .equals(intent.getAction())) {
                if (mPlaylist != null && mPlaylist.getId().equals(intent.getStringExtra(
                        DatabaseHelper.PLAYLISTSDATASOURCE_RESULTSREPORTED_PLAYLISTID))) {
                    refreshCurrentPlaylist();
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
                    getActivity().getSupportFragmentManager().popBackStack();
                } else {
                    mCurrentRequestIds.add(InfoSystem.getInstance().resolve(mAlbum));
                }
            }
            if (getArguments().containsKey(TOMAHAWK_PLAYLIST_KEY) && !TextUtils.isEmpty(
                    getArguments().getString(TOMAHAWK_PLAYLIST_KEY))) {
                mPlaylist = Playlist
                        .getPlaylistById(getArguments().getString(TOMAHAWK_PLAYLIST_KEY));
                if (mPlaylist == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                } else {
                    refreshCurrentPlaylist();
                }
            }
            if (getArguments().containsKey(TOMAHAWK_ARTIST_KEY) && !TextUtils
                    .isEmpty(getArguments().getString(TOMAHAWK_ARTIST_KEY))) {
                mArtist = Artist.getArtistByKey(getArguments().getString(TOMAHAWK_ARTIST_KEY));
                if (mArtist == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                } else {
                    ArrayList<String> requestIds = InfoSystem.getInstance().resolve(mArtist, false);
                    for (String requestId : requestIds) {
                        mCurrentRequestIds.add(requestId);
                    }
                }
            }
            if (getArguments().containsKey(TOMAHAWK_USER_ID) && !TextUtils
                    .isEmpty(getArguments().getString(TOMAHAWK_USER_ID))) {
                mUser = User.getUserById(getArguments().getString(TOMAHAWK_USER_ID));
                if (mUser == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                } else {
                    mCurrentRequestIds.add(InfoSystem.getInstance().resolve(mUser));
                }
            }
            if (getArguments().containsKey(CollectionManager.COLLECTION_ID)) {
                mCollection = CollectionManager.getInstance()
                        .getCollection(getArguments().getString(CollectionManager.COLLECTION_ID));
            }
            if (getArguments().containsKey(TOMAHAWK_QUERY_KEY) && !TextUtils
                    .isEmpty(getArguments().getString(TOMAHAWK_QUERY_KEY))) {
                mQuery = Query.getQueryByKey(getArguments().getString(TOMAHAWK_QUERY_KEY));
                if (mQuery == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                } else {
                    ArrayList<String> requestIds =
                            InfoSystem.getInstance().resolve(mQuery.getArtist(), true);
                    for (String requestId : requestIds) {
                        mCurrentRequestIds.add(requestId);
                    }
                }
            }
            if (getArguments().containsKey(CONTAINER_FRAGMENT_NAME)) {
                String fragmentName = getArguments().getString(CONTAINER_FRAGMENT_NAME);
                if (fragmentName.equals(FavoritesFragment.class.getName())) {
                    mContainerFragmentClass = FavoritesFragment.class;
                } else if (fragmentName.equals(OldCollectionFragment.class.getName())) {
                    mContainerFragmentClass = OldCollectionFragment.class;
                }
            }
            if (getArguments().containsKey(CONTAINER_FRAGMENT_PAGE)) {
                getListView().setTag(getArguments().getInt(CONTAINER_FRAGMENT_PAGE));
            }
        }

        // Initialize and register Receiver
        if (mTomahawkFragmentReceiver == null) {
            mTomahawkFragmentReceiver = new TomahawkFragmentReceiver();
            IntentFilter intentFilter = new IntentFilter(CollectionManager.COLLECTION_UPDATED);
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
            intentFilter = new IntentFilter(DatabaseHelper.PLAYLISTSDATASOURCE_RESULTSREPORTED);
            activity.registerReceiver(mTomahawkFragmentReceiver, intentFilter);
        }

        onPlaylistChanged();

        mIsResumed = true;
    }

    @Override
    public void onPause() {
        super.onPause();

        for (String queryKey : mCorrespondingQueryIds) {
            if (ThreadManager.getInstance().stop(Query.getQueryByKey(queryKey))) {
                mCorrespondingQueryIds.remove(queryKey);
            }
        }

        mPipeLineResultReporter.removeCallbacksAndMessages(null);

        if (mTomahawkFragmentReceiver != null) {
            getActivity().unregisterReceiver(mTomahawkFragmentReceiver);
            mTomahawkFragmentReceiver = null;
        }

        mIsResumed = false;
    }

    @Override
    public abstract void onItemClick(View view, TomahawkListItem item);

    /**
     * Called every time an item inside a ListView or GridView is long-clicked
     *
     * @param item the TomahawkListItem which corresponds to the long-click
     */
    @Override
    public boolean onItemLongClick(View view, TomahawkListItem item) {
        if ((item instanceof SocialAction
                && (((SocialAction) item).getTargetObject() instanceof User
                || ((SocialAction) item).getTargetObject() instanceof Playlist))
                || item instanceof User) {
            return false;
        }
        boolean showDelete = false;
        if (item instanceof Playlist) {
            showDelete = true;
        } else if (!(this instanceof PlaybackFragment)) {
            if (!(mPlaylist == null
                    || DatabaseHelper.LOVEDITEMS_PLAYLIST_ID.equals(mPlaylist.getId()))) {
                showDelete = true;
            }
        } else if (item instanceof Query && item !=
                ((TomahawkMainActivity) getActivity()).getPlaybackService().getCurrentQuery()) {
            showDelete = true;
        }
        FakeContextMenuDialog dialog = new FakeContextMenuDialog();
        Bundle args = new Bundle();
        args.putBoolean(TOMAHAWK_SHOWDELETE_KEY, showDelete);
        args.putBoolean(TOMAHAWK_FROMPLAYBACKFRAGMENT, this instanceof PlaybackFragment);
        if (mAlbum != null) {
            args.putString(TOMAHAWK_ALBUM_KEY, mAlbum.getCacheKey());
        } else if (mPlaylist != null) {
            args.putString(TOMAHAWK_PLAYLIST_KEY, mPlaylist.getId());
        } else if (mArtist != null) {
            args.putString(TOMAHAWK_ARTIST_KEY, mArtist.getCacheKey());
        }
        if (mCollection != null) {
            args.putString(CollectionManager.COLLECTION_ID, mCollection.getId());
        }
        if (item instanceof Query) {
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_KEY, item.getCacheKey());
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_TYPE, TOMAHAWK_QUERY_KEY);
        } else if (item instanceof Album) {
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_KEY, item.getCacheKey());
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_TYPE, TOMAHAWK_ALBUM_KEY);
        } else if (item instanceof Artist) {
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_KEY, item.getCacheKey());
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_TYPE, TOMAHAWK_ARTIST_KEY);
        } else if (item instanceof Playlist) {
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_KEY,
                    ((Playlist) item).getId());
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_TYPE, TOMAHAWK_PLAYLIST_KEY);
        } else if (item instanceof SocialAction) {
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_KEY,
                    ((SocialAction) item).getId());
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_TYPE, TOMAHAWK_SOCIALACTION_ID);
        } else if (item instanceof PlaylistEntry) {
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_KEY, item.getCacheKey());
            args.putString(TOMAHAWK_TOMAHAWKLISTITEM_TYPE, TOMAHAWK_PLAYLISTENTRY_ID);
        }
        dialog.setArguments(args);
        dialog.show(getFragmentManager(), null);
        return true;
    }

    /**
     * Get the {@link BaseAdapter} associated with this activity's ListView.
     */
    public TomahawkListAdapter getListAdapter() {
        return mTomahawkListAdapter;
    }

    /**
     * Set the {@link BaseAdapter} associated with this activity's ListView.
     */
    public void setListAdapter(TomahawkListAdapter adapter) {
        super.setListAdapter(adapter);
        mTomahawkListAdapter = adapter;
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    protected abstract void updateAdapter();

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
            mResolveQueriesHandler.removeCallbacksAndMessages(null);
            mResolveQueriesHandler.sendEmptyMessage(RESOLVE_QUERIES_REPORTER_MSG);
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

    protected void updateShowPlaystate() {
        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (getListAdapter() != null) {
            if (playbackService != null) {
                getListAdapter().setShowPlaystate(true);
                getListAdapter().setHighlightedItemIsPlaying(playbackService.isPlaying());
                getListAdapter().setHighlightedEntry(playbackService.getCurrentEntry());
                getListAdapter().setHighlightedQuery(playbackService.getCurrentQuery());
            } else {
                getListAdapter().setShowPlaystate(false);
            }
            getListAdapter().notifyDataSetChanged();
        }
    }

    /**
     * Called when a Collection has been updated.
     */
    protected void onCollectionUpdated() {
        updateAdapter();
        mResolveQueriesHandler.removeCallbacksAndMessages(null);
        mResolveQueriesHandler.sendEmptyMessage(RESOLVE_QUERIES_REPORTER_MSG);
    }

    private void resolveVisibleQueries() {
        resolveQueriesFromTo(mFirstVisibleItemLastTime - 5,
                mFirstVisibleItemLastTime + mVisibleItemCount + 5);
    }

    private void resolveQueriesFromTo(final int start, final int end) {
        ArrayList<Query> qs = new ArrayList<Query>();
        for (int i = (start < 0 ? 0 : start); i < end && i < mShownQueries.size(); i++) {
            Query q = mShownQueries.get(i);
            if (!q.isSolved() && !mCorrespondingQueryIds.contains(q.getCacheKey())) {
                qs.add(q);
            }
        }
        if (!qs.isEmpty()) {
            HashSet<String> qids = PipeLine.getInstance().resolve(qs);
            mCorrespondingQueryIds.addAll(qids);
        }
    }

    protected void refreshCurrentPlaylist() {
        ThreadManager.getInstance().execute(
                new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_VERYHIGH) {
                    @Override
                    public void run() {
                        Playlist playlist =
                                DatabaseHelper.getInstance().getPlaylist(mPlaylist.getId());
                        if (playlist != null) {
                            mPlaylist = playlist;
                        }
                        TomahawkApp.getContext().sendBroadcast(
                                new Intent(CollectionManager.COLLECTION_UPDATED));
                    }
                }
        );
    }
}
