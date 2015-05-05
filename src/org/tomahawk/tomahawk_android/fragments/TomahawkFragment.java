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

import com.google.common.collect.Sets;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
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
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.MultiColumnClickListener;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * The base class for {@link AlbumsFragment}, {@link TracksFragment}, {@link ArtistsFragment},
 * {@link PlaylistsFragment} and {@link SearchPagerFragment}. Provides all sorts of functionality to
 * those classes, related to displaying {@link TomahawkListItem}s in whichever needed way.
 */
public abstract class TomahawkFragment extends TomahawkListFragment
        implements MultiColumnClickListener, AbsListView.OnScrollListener {

    public static final String ALBUM = "album";

    public static final String ALBUMARRAY = "albumarray";

    public static final String ARTIST = "artist";

    public static final String ARTISTARRAY = "artistarray";

    public static final String PLAYLIST = "playlist";

    public static final String USER = "user";

    public static final String USERARRAY = "userarray";

    public static final String SOCIALACTION = "socialaction";

    public static final String PLAYLISTENTRY = "playlistentry";

    public static final String QUERY = "query";

    public static final String QUERYARRAY = "queryarray";

    public static final String PREFERENCEID = "preferenceid";

    public static final String SHOWDELETE = "showdelete";

    public static final String TOMAHAWKLISTITEM = "tomahawklistitem";

    public static final String TOMAHAWKLISTITEM_TYPE = "tomahawklistitem_type";

    public static final String FROM_PLAYBACKFRAGMENT = "from_playbackfragment";

    public static final String USERNAME_STRING = "username_string";

    public static final String PASSWORD_STRING = "password_string";

    public static final String QUERY_STRING = "query_string";

    public static final String SHOW_MODE = "show_mode";

    public static final String COLLECTION_ID = "collection_id";

    public static final String LOG_DATA = "log_data";

    public static final String CONTENT_HEADER_MODE = "content_header_mode";

    public static final String CONTAINER_FRAGMENT_ID = "container_fragment_id";

    public static final String CONTAINER_FRAGMENT_PAGE = "container_fragment_page";

    public static final String CONTAINER_FRAGMENT_CLASSNAME = "container_fragment_classname";

    public static final String LIST_SCROLL_POSITION = "list_scroll_position";

    protected static final int RESOLVE_QUERIES_REPORTER_MSG = 1336;

    protected static final long RESOLVE_QUERIES_REPORTER_DELAY = 100;

    protected static final int ADAPTER_UPDATE_MSG = 1337;

    protected static final long ADAPTER_UPDATE_DELAY = 500;

    public static final int CREATE_PLAYLIST_BUTTON_ID = 8008135;

    private TomahawkListAdapter mTomahawkListAdapter;

    protected boolean mIsResumed;

    protected final Set<String> mCorrespondingRequestIds =
            Sets.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    protected final HashSet<TomahawkListItem> mResolvingItems = new HashSet<>();

    protected final Set<Query> mCorrespondingQueries
            = Sets.newSetFromMap(new ConcurrentHashMap<Query, Boolean>());

    protected ArrayList<Query> mShownQueries = new ArrayList<>();

    protected ArrayList<Query> mQueryArray;

    protected ArrayList<Album> mAlbumArray;

    protected ArrayList<Artist> mArtistArray;

    protected ArrayList<User> mUserArray;

    protected final ArrayList<PlaylistEntry> mShownPlaylistEntries = new ArrayList<>();

    protected Album mAlbum;

    protected Artist mArtist;

    protected Playlist mPlaylist;

    protected User mUser;

    protected Query mQuery;

    private int mFirstVisibleItemLastTime = -1;

    private int mVisibleItemCount = 0;

    protected int mShowMode;

    protected final Handler mResolveQueriesHandler = new ResolveQueriesHandler(this);

    private static class ResolveQueriesHandler extends WeakReferenceHandler<TomahawkFragment> {

        public ResolveQueriesHandler(TomahawkFragment referencedObject) {
            super(referencedObject);
        }

        @Override
        public void handleMessage(Message msg) {
            TomahawkFragment fragment = getReferencedObject();
            if (fragment != null) {
                removeMessages(msg.what);
                fragment.resolveVisibleItems();
            }
        }
    }

    // Handler which reports the PipeLine's and InfoSystem's results in intervals
    protected final Handler mAdapterUpdateHandler = new AdapterUpdateHandler(this);

    private static class AdapterUpdateHandler extends WeakReferenceHandler<TomahawkFragment> {

        public AdapterUpdateHandler(TomahawkFragment referencedObject) {
            super(referencedObject);
        }

        @Override
        public void handleMessage(Message msg) {
            TomahawkFragment fragment = getReferencedObject();
            if (fragment != null) {
                removeMessages(msg.what);
                fragment.updateAdapter();
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(PipeLine.ResultsEvent event) {
        if (mCorrespondingQueries.contains(event.mQuery)) {
            if (!mAdapterUpdateHandler.hasMessages(ADAPTER_UPDATE_MSG)) {
                mAdapterUpdateHandler.sendEmptyMessageDelayed(ADAPTER_UPDATE_MSG,
                        ADAPTER_UPDATE_DELAY);
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(InfoSystem.ResultsEvent event) {
        if (mCorrespondingRequestIds.contains(event.mInfoRequestData.getRequestId())) {
            if (!mAdapterUpdateHandler.hasMessages(ADAPTER_UPDATE_MSG)) {
                mAdapterUpdateHandler.sendEmptyMessageDelayed(ADAPTER_UPDATE_MSG,
                        ADAPTER_UPDATE_DELAY);
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PlaybackService.PlayingTrackChangedEvent event) {
        onTrackChanged();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PlaybackService.PlayStateChangedEvent event) {
        onPlaystateChanged();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PlaybackService.PlayingPlaylistChangedEvent event) {
        onPlaylistChanged();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CollectionManager.UpdatedEvent event) {
        if (event.mUpdatedItemIds != null) {
            if ((mPlaylist != null && event.mUpdatedItemIds.contains(mPlaylist.getId()))
                    || (mAlbum != null && event.mUpdatedItemIds.contains(mAlbum.getCacheKey()))
                    || (mArtist != null && event.mUpdatedItemIds.contains(mArtist.getCacheKey()))
                    || (mQuery != null && event.mUpdatedItemIds.contains(mQuery.getCacheKey()))) {
                if (!mAdapterUpdateHandler.hasMessages(ADAPTER_UPDATE_MSG)) {
                    mAdapterUpdateHandler.sendEmptyMessageDelayed(ADAPTER_UPDATE_MSG,
                            ADAPTER_UPDATE_DELAY);
                }
            }
        } else {
            if (!mAdapterUpdateHandler.hasMessages(ADAPTER_UPDATE_MSG)) {
                mAdapterUpdateHandler.sendEmptyMessageDelayed(ADAPTER_UPDATE_MSG,
                        ADAPTER_UPDATE_DELAY);
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PlaybackService.ReadyEvent event) {
        onPlaybackServiceReady();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PlaybackService.PlayPositionChangedEvent event) {
        if (mTomahawkListAdapter != null) {
            mTomahawkListAdapter.onPlayPositionChanged(event.duration, event.currentPosition);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getArguments() != null) {
            if (getArguments().containsKey(ALBUM)
                    && !TextUtils.isEmpty(getArguments().getString(ALBUM))) {
                mAlbum = Album.getAlbumByKey(getArguments().getString(ALBUM));
                if (mAlbum == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else {
                    mCorrespondingRequestIds.add(InfoSystem.getInstance().resolve(mAlbum));
                }
            }
            if (getArguments().containsKey(PLAYLIST)
                    && !TextUtils.isEmpty(getArguments().getString(PLAYLIST))) {
                String playlistId = getArguments().getString(TomahawkFragment.PLAYLIST);
                mPlaylist = DatabaseHelper.getInstance().getPlaylist(playlistId);
                if (mPlaylist == null) {
                    mPlaylist = Playlist.getPlaylistById(playlistId);
                    if (mPlaylist == null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                        return;
                    } else {
                        HatchetAuthenticatorUtils authenticatorUtils
                                = (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                        if (mUser != authenticatorUtils.getLoggedInUser()) {
                            mCorrespondingRequestIds
                                    .add(InfoSystem.getInstance().resolve(mPlaylist));
                        }
                    }
                }
            }
            if (getArguments().containsKey(ARTIST)
                    && !TextUtils.isEmpty(getArguments().getString(ARTIST))) {
                mArtist = Artist.getArtistByKey(getArguments().getString(ARTIST));
                if (mArtist == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else {
                    ArrayList<String> requestIds = InfoSystem.getInstance().resolve(mArtist, false);
                    for (String requestId : requestIds) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            }
            if (getArguments().containsKey(USER)
                    && !TextUtils.isEmpty(getArguments().getString(USER))) {
                mUser = User.getUserById(getArguments().getString(USER));
                if (mUser == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else if (mUser.getName() == null) {
                    mCorrespondingRequestIds.add(InfoSystem.getInstance().resolve(mUser));
                }
            }
            if (getArguments().containsKey(COLLECTION_ID)) {
                mCollection = CollectionManager.getInstance()
                        .getCollection(getArguments().getString(COLLECTION_ID));
            }
            if (getArguments().containsKey(QUERY)
                    && !TextUtils.isEmpty(getArguments().getString(QUERY))) {
                mQuery = Query.getQueryByKey(getArguments().getString(QUERY));
                if (mQuery == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else {
                    ArrayList<String> requestIds =
                            InfoSystem.getInstance().resolve(mQuery.getArtist(), false);
                    for (String requestId : requestIds) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            }
            if (getArguments().containsKey(USERARRAY)) {
                mUserArray = new ArrayList<>();
                for (String userId : getArguments().getStringArrayList(USERARRAY)) {
                    mUserArray.add(User.getUserById(userId));
                }
            }
            if (getArguments().containsKey(ARTISTARRAY)) {
                mArtistArray = new ArrayList<>();
                for (String userId : getArguments().getStringArrayList(ARTISTARRAY)) {
                    Artist artist = Artist.getArtistByKey(userId);
                    if (artist != null) {
                        mArtistArray.add(artist);
                    }
                }
            }
            if (getArguments().containsKey(ALBUMARRAY)) {
                mAlbumArray = new ArrayList<>();
                for (String userId : getArguments().getStringArrayList(ALBUMARRAY)) {
                    Album album = Album.getAlbumByKey(userId);
                    if (album != null) {
                        mAlbumArray.add(album);
                    }
                }
            }
            if (getArguments().containsKey(QUERYARRAY)) {
                mQueryArray = new ArrayList<>();
                for (String userId : getArguments().getStringArrayList(QUERYARRAY)) {
                    Query query = Query.getQueryByKey(userId);
                    if (query != null) {
                        mQueryArray.add(query);
                    }
                }
            }
        }

        StickyListHeadersListView list = getListView();
        if (list != null) {
            list.setOnScrollListener(this);
            if (mTomahawkListAdapter != null) {
                getListView().setOnItemClickListener(mTomahawkListAdapter);
                getListView().setOnItemLongClickListener(mTomahawkListAdapter);
            }
        }

        onPlaylistChanged();

        mIsResumed = true;
    }

    @Override
    public void onPause() {
        super.onPause();

        for (Query query : mCorrespondingQueries) {
            if (ThreadManager.getInstance().stop(query)) {
                mCorrespondingQueries.remove(query);
            }
        }

        mAdapterUpdateHandler.removeCallbacksAndMessages(null);

        mIsResumed = false;
    }

    @Override
    public abstract void onItemClick(View view, Object item);

    /**
     * Called every time an item inside a ListView or GridView is long-clicked
     *
     * @param item the Object which corresponds to the long-click
     */
    @Override
    public boolean onItemLongClick(View view, Object item) {
        String collectionId = mCollection != null ? mCollection.getId() : null;
        return FragmentUtils.showContextMenu((TomahawkMainActivity) getActivity(), item,
                collectionId, false);
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
        getListView().setOnItemClickListener(mTomahawkListAdapter);
        getListView().setOnItemLongClickListener(mTomahawkListAdapter);
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    protected abstract void updateAdapter();

    /**
     * This method _MUST_ be called at the end of updateAdapter (with the exception of
     * PlaybackFragment)
     */
    protected void onUpdateAdapterFinished() {
        updateShowPlaystate();
        forceAutoResolve();
        setupNonScrollableSpacer();
        setupScrollableSpacer();
        setupAnimations();
    }

    /**
     * If the PlaybackService signals, that it is ready, this method is being called
     */
    protected void onPlaybackServiceReady() {
        updateShowPlaystate();
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

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        super.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);

        mVisibleItemCount = visibleItemCount;
        if (mFirstVisibleItemLastTime != firstVisibleItem) {
            mFirstVisibleItemLastTime = firstVisibleItem;
            mResolveQueriesHandler.removeCallbacksAndMessages(null);
            mResolveQueriesHandler.sendEmptyMessageDelayed(RESOLVE_QUERIES_REPORTER_MSG,
                    RESOLVE_QUERIES_REPORTER_DELAY);
        }
    }

    protected void forceAutoResolve() {
        mResolveQueriesHandler.removeCallbacksAndMessages(null);
        mResolveQueriesHandler.sendEmptyMessageDelayed(RESOLVE_QUERIES_REPORTER_MSG,
                RESOLVE_QUERIES_REPORTER_DELAY);
    }

    private void resolveVisibleItems() {
        resolveQueriesFromTo(mFirstVisibleItemLastTime - 5,
                mFirstVisibleItemLastTime + mVisibleItemCount + 5);
        resolveItemsFromTo(mFirstVisibleItemLastTime,
                mFirstVisibleItemLastTime + mVisibleItemCount + 1);
    }

    private void resolveQueriesFromTo(final int start, final int end) {
        Set<Query> qs = new HashSet<>();
        for (int i = (start < 0 ? 0 : start); i < end && i < mShownQueries.size(); i++) {
            Query q = mShownQueries.get(i);
            if (!q.isSolved() && !mCorrespondingQueries.contains(q)) {
                qs.add(q);
            }
        }
        if (!qs.isEmpty()) {
            HashSet<Query> queries = PipeLine.getInstance().resolve(qs);
            mCorrespondingQueries.addAll(queries);
        }
    }

    private void resolveItemsFromTo(final int start, final int end) {
        if (mTomahawkListAdapter != null) {
            for (int i = (start < 0 ? 0 : start); i < end && i < mTomahawkListAdapter.getCount();
                    i++) {
                Object object = mTomahawkListAdapter.getItem(i);
                if (object instanceof List) {
                    for (Object item : (List) object) {
                        if (item instanceof TomahawkListItem) {
                            resolveItem((TomahawkListItem) item);
                        }
                    }
                } else if (object instanceof TomahawkListItem) {
                    resolveItem((TomahawkListItem) object);
                }
            }
        }
    }

    protected void resolveItem(TomahawkListItem item) {
        InfoSystem infoSystem = InfoSystem.getInstance();
        if (!mResolvingItems.contains(item)) {
            mResolvingItems.add(item);
            if (item instanceof SocialAction) {
                resolveItem(((SocialAction) item).getTargetObject());
                resolveItem(((SocialAction) item).getUser());
            } else if (item instanceof Album) {
                if (item.getImage() == null) {
                    mCorrespondingRequestIds.add(infoSystem.resolve((Album) item));
                }
            } else if (item instanceof Artist) {
                if (item.getImage() == null) {
                    mCorrespondingRequestIds.addAll(infoSystem.resolve((Artist) item, false));
                }
            } else if (item instanceof User) {
                if (item.getImage() == null) {
                    mCorrespondingRequestIds.add(infoSystem.resolve((User) item));
                }
            }
        }
    }
}

