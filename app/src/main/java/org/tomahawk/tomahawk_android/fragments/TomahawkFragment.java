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

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.StationPlaylist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.listeners.MultiColumnClickListener;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.PreferenceUtils;
import org.tomahawk.tomahawk_android.utils.ProgressBarUpdater;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;
import org.tomahawk.tomahawk_android.utils.WeakReferenceHandler;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * The base class for every {@link android.support.v4.app.Fragment} that displays a collection
 * object
 */
public abstract class TomahawkFragment extends TomahawkListFragment
        implements MultiColumnClickListener, AbsListView.OnScrollListener {

    private static final String TAG = TomahawkFragment.class.getSimpleName();

    public static final String ALBUM = "album";

    public static final String ALBUMARRAY = "albumarray";

    public static final String ARTIST = "artist";

    public static final String ARTISTARRAY = "artistarray";

    public static final String PLAYLIST = "playlist";

    public static final String STATION = "station";

    public static final String USER = "user";

    public static final String USERARRAY = "userarray";

    public static final String SOCIALACTION = "socialaction";

    public static final String PLAYLISTENTRY = "playlistentry";

    public static final String QUERY = "query";

    public static final String QUERYARRAY = "queryarray";

    public static final String PREFERENCEID = "preferenceid";

    public static final String TOMAHAWKLISTITEM = "tomahawklistitem";

    public static final String TOMAHAWKLISTITEM_TYPE = "tomahawklistitem_type";

    public static final String FROM_PLAYBACKFRAGMENT = "from_playbackfragment";

    public static final String HIDE_REMOVE_BUTTON = "hide_remove_button";

    public static final String QUERY_STRING = "query_string";

    public static final String SHOW_MODE = "show_mode";

    public static final String CONTAINER_FRAGMENT_CLASSNAME = "container_fragment_classname";

    public static final String LIST_SCROLL_POSITION = "list_scroll_position";

    public static final String MESSAGE = "message";

    protected static final int RESOLVE_QUERIES_REPORTER_MSG = 1336;

    protected static final long RESOLVE_QUERIES_REPORTER_DELAY = 100;

    protected static final int ADAPTER_UPDATE_MSG = 1337;

    protected static final long ADAPTER_UPDATE_DELAY = 500;

    private TomahawkListAdapter mTomahawkListAdapter;

    private ProgressBarUpdater mProgressBarUpdater = new ProgressBarUpdater(
            new ProgressBarUpdater.UpdateProgressRunnable() {
                @Override
                public void updateProgress(PlaybackStateCompat playbackState, long duration) {
                    if (playbackState != null && mTomahawkListAdapter != null
                            && mTomahawkListAdapter.getProgressBar() != null) {
                        long currentPosition = playbackState.getPosition();
                        if (playbackState.getState() != PlaybackStateCompat.STATE_PAUSED) {
                            // Calculate the elapsed time between the last position update and now
                            // and unless paused, we can assume (delta * speed) + current position
                            // is approximately the latest position. This ensure that we do not
                            // repeatedly call the getPlaybackState() on MediaControllerCompat.
                            long timeDelta = SystemClock.elapsedRealtime() -
                                    playbackState.getLastPositionUpdateTime();
                            currentPosition += (int) timeDelta * playbackState.getPlaybackSpeed();
                        }
                        mTomahawkListAdapter.getProgressBar().setProgress(
                                (int) ((float) currentPosition / duration
                                        * mTomahawkListAdapter.getProgressBar().getMax()));
                    }
                }
            });

    protected boolean mIsResumed;

    protected final Set<String> mCorrespondingRequestIds =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    protected final HashSet<Object> mResolvingItems = new HashSet<>();

    protected final Set<Query> mCorrespondingQueries =
            Collections.newSetFromMap(new ConcurrentHashMap<Query, Boolean>());

    protected ArrayList<Query> mQueryArray;

    protected ArrayList<Album> mAlbumArray;

    protected ArrayList<Artist> mArtistArray;

    protected ArrayList<User> mUserArray;

    protected Album mAlbum;

    protected Artist mArtist;

    protected Playlist mPlaylist;

    protected User mUser;

    protected Query mQuery;

    private int mFirstVisibleItemLastTime = -1;

    private int mVisibleItemCount = 0;

    protected int mShowMode = -1;

    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            Log.d(TAG, "onPlaybackstate changed" + state);
            if (mTomahawkListAdapter != null) {
                boolean isPlaying = state.getState() == PlaybackStateCompat.STATE_PLAYING;
                mTomahawkListAdapter.setHighlightedItemIsPlaying(isPlaying);
                mTomahawkListAdapter.notifyDataSetChanged();
                mProgressBarUpdater.setPlaybackState(state);
                if (isPlaying) {
                    mProgressBarUpdater.scheduleSeekbarUpdate();
                } else {
                    mProgressBarUpdater.stopSeekbarUpdate();
                }
            }
            TomahawkFragment.this.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            Log.d(TAG, "onMetadataChanged changed" + metadata);
            if (mTomahawkListAdapter != null && metadata != null) {
                if (getPlaybackManager().getCurrentEntry() != null) {
                    mProgressBarUpdater.setCurrentDuration(
                            metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
                    mTomahawkListAdapter
                            .setHighlightedEntry(getPlaybackManager().getCurrentEntry());
                    mTomahawkListAdapter
                            .setHighlightedQuery(getPlaybackManager().getCurrentQuery());
                    mTomahawkListAdapter.notifyDataSetChanged();
                }
            }
            TomahawkFragment.this.onMetadataChanged(metadata);
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            Log.d(TAG, "onQueueChanged changed queue.size()= " + queue.size());
            TomahawkFragment.this.onQueueChanged(queue);
        }
    };

    private final Handler mResolveQueriesHandler = new ResolveQueriesHandler(this);

    private static class ResolveQueriesHandler extends WeakReferenceHandler<TomahawkFragment> {

        public ResolveQueriesHandler(TomahawkFragment referencedObject) {
            super(referencedObject);
        }

        @Override
        public void handleMessage(Message msg) {
            TomahawkFragment fragment = getReferencedObject();
            if (fragment != null && getReferencedObject().shouldAutoResolve()) {
                Log.d(TAG, "Auto resolving ...");
                removeMessages(msg.what);
                getReferencedObject().resolveItemsFromTo(
                        getReferencedObject().mFirstVisibleItemLastTime - 2,
                        getReferencedObject().mFirstVisibleItemLastTime
                                + getReferencedObject().mVisibleItemCount + 2);
            }
        }
    }

    // Handler which reports the PipeLine's and InfoSystem's results in intervals
    private final Handler mAdapterUpdateHandler = new AdapterUpdateHandler(this);

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
    public void onEvent(PipeLine.ResolversChangedEvent event) {
        forceResolveVisibleItems(event.mManuallyAdded);
    }

    @SuppressWarnings("unused")
    public void onEvent(PipeLine.ResultsEvent event) {
        if (mCorrespondingQueries.contains(event.mQuery)) {
            scheduleUpdateAdapter();
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(InfoSystem.ResultsEvent event) {
        if (mCorrespondingRequestIds.contains(event.mInfoRequestData.getRequestId())) {
            scheduleUpdateAdapter();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CollectionManager.UpdatedEvent event) {
        if (event.mUpdatedItemIds != null) {
            if ((mPlaylist != null && event.mUpdatedItemIds.contains(mPlaylist.getId()))
                    || (mAlbum != null && event.mUpdatedItemIds.contains(mAlbum.getCacheKey()))
                    || (mArtist != null && event.mUpdatedItemIds.contains(mArtist.getCacheKey()))
                    || (mQuery != null && event.mUpdatedItemIds.contains(mQuery.getCacheKey()))) {
                scheduleUpdateAdapter();
            }
        } else {
            scheduleUpdateAdapter();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getArguments() != null) {
            if (getArguments().containsKey(ALBUM)
                    && !TextUtils.isEmpty(getArguments().getString(ALBUM))) {
                mAlbum = Album.getByKey(getArguments().getString(ALBUM));
                if (mAlbum == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else {
                    String requestId = InfoSystem.get().resolve(mAlbum);
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            }
            if (getArguments().containsKey(PLAYLIST)
                    && !TextUtils.isEmpty(getArguments().getString(PLAYLIST))) {
                mPlaylist = Playlist.getByKey(getArguments().getString(TomahawkFragment.PLAYLIST));
                if (mPlaylist == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else {
                    User.getSelf().done(new DoneCallback<User>() {
                        @Override
                        public void onDone(User user) {
                            if (mUser != user) {
                                String requestId = InfoSystem.get().resolve(mPlaylist);
                                if (requestId != null) {
                                    mCorrespondingRequestIds.add(requestId);
                                }
                            }
                        }
                    });
                }
            }
            if (getArguments().containsKey(ARTIST)
                    && !TextUtils.isEmpty(getArguments().getString(ARTIST))) {
                mArtist = Artist.getByKey(getArguments().getString(ARTIST));
                if (mArtist == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else {
                    String requestId = InfoSystem.get().resolve(mArtist, true);
                    if (requestId != null) {
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
                    String requestId = InfoSystem.get().resolve(mUser);
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            }
            if (getArguments().containsKey(QUERY)
                    && !TextUtils.isEmpty(getArguments().getString(QUERY))) {
                mQuery = Query.getByKey(getArguments().getString(QUERY));
                if (mQuery == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else {
                    String requestId = InfoSystem.get().resolve(mQuery.getArtist(), false);
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            }
            ArrayList<String> argList = getArguments().getStringArrayList(USERARRAY);
            if (argList != null) {
                mUserArray = new ArrayList<>();
                for (String userId : argList) {
                    mUserArray.add(User.getUserById(userId));
                }
            }
            argList = getArguments().getStringArrayList(ARTISTARRAY);
            if (argList != null) {
                mArtistArray = new ArrayList<>();
                for (String artistKey : argList) {
                    Artist artist = Artist.getByKey(artistKey);
                    if (artist != null) {
                        mArtistArray.add(artist);
                    }
                }
            }
            argList = getArguments().getStringArrayList(ALBUMARRAY);
            if (argList != null) {
                mAlbumArray = new ArrayList<>();
                for (String albumKey : argList) {
                    Album album = Album.getByKey(albumKey);
                    if (album != null) {
                        mAlbumArray.add(album);
                    }
                }
            }
            argList = getArguments().getStringArrayList(QUERYARRAY);
            if (argList != null) {
                mQueryArray = new ArrayList<>();
                for (String queryKey : argList) {
                    Query query = Query.getByKey(queryKey);
                    if (query != null) {
                        mQueryArray.add(query);
                    }
                }
            }
            if (getArguments().containsKey(SHOW_MODE)) {
                mShowMode = getArguments().getInt(SHOW_MODE);
            }
        }

        StickyListHeadersListView list = getListView();
        if (list != null) {
            list.setOnScrollListener(this);
        }

        mIsResumed = true;
    }

    @Override
    public void onPause() {
        super.onPause();

        for (Query query : mCorrespondingQueries) {
            if (ThreadManager.get().stop(query)) {
                mCorrespondingQueries.remove(query);
            }
        }

        mAdapterUpdateHandler.removeCallbacksAndMessages(null);

        mIsResumed = false;

        if (mTomahawkListAdapter != null) {
            mTomahawkListAdapter.closeSegments(null);
            mTomahawkListAdapter = null;
        }

        mProgressBarUpdater.stopSeekbarUpdate();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        onMediaControllerConnected();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
        if (getMediaController() != null) {
            getMediaController().unregisterCallback(mCallback);
        }
    }

    @Override
    public void onMediaControllerConnected() {
        super.onMediaControllerConnected();
        Log.d(TAG, "onMediaControllerConnected()");
        if (getMediaController() != null) {
            onPlaybackStateChanged(getMediaController().getPlaybackState());
            onMetadataChanged(getMediaController().getMetadata());
            getMediaController().registerCallback(mCallback);
        } else {
            Log.e(TAG, "Couldn't get MediaController object!");
        }
    }

    @Override
    public abstract void onItemClick(View view, Object item, Segment segment);

    /**
     * Called every time an item inside a ListView or GridView is long-clicked
     *
     * @param item the Object which corresponds to the long-click
     * @param segment
     */
    @Override
    public boolean onItemLongClick(View view, Object item, Segment segment) {
        return FragmentUtils.showContextMenu((TomahawkMainActivity) getActivity(), item,
                mCollection.getId(), false, mHideRemoveButton);
    }

    protected void fillAdapter(Segment segment, Collection collection) {
        List<Segment> segments = new ArrayList<>();
        segments.add(segment);
        fillAdapter(segments, null, collection);
    }

    protected void fillAdapter(Segment segment) {
        List<Segment> segments = new ArrayList<>();
        segments.add(segment);
        fillAdapter(segments, null, null);
    }

    protected void fillAdapter(List<Segment> segments) {
        fillAdapter(segments, null, null);
    }

    protected void fillAdapter(List<Segment> segments, Collection collection) {
        fillAdapter(segments, null, collection);
    }

    protected void fillAdapter(final List<Segment> segments, final View headerSpacerForwardView,
            final Collection collection) {
        final TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (activity != null && getListView() != null) {
                    if (mTomahawkListAdapter == null) {
                        LayoutInflater inflater = activity.getLayoutInflater();
                        TomahawkListAdapter adapter = new TomahawkListAdapter(activity,
                                inflater,
                                segments, collection, getListView(), TomahawkFragment.this);
                        TomahawkFragment.super.setListAdapter(adapter);
                        mTomahawkListAdapter = adapter;
                    } else {
                        mTomahawkListAdapter.setSegments(segments, getListView());
                    }
                    forceResolveVisibleItems(false);
                    setupNonScrollableSpacer(getListView());
                    setupScrollableSpacer(getListAdapter(), getListView(),
                            headerSpacerForwardView);
                    if (headerSpacerForwardView == null) {
                        setupAnimations();
                    }
                } else {
                    Log.e(TAG, "fillAdapter - getActivity() or getListView() returned null!");
                }
            }
        });
    }

    /**
     * Get the {@link TomahawkListAdapter} associated with this activity's ListView.
     */
    public TomahawkListAdapter getListAdapter() {
        return (TomahawkListAdapter) super.getListAdapter();
    }

    protected void setAreHeadersSticky(final boolean areHeadersSticky) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (getListView() != null) {
                    getListView().setAreHeadersSticky(areHeadersSticky);
                } else {
                    Log.e(TAG, "setAreHeadersSticky - getListView() returned null!");
                }
            }
        });
    }

    protected void scheduleUpdateAdapter() {
        if (!mAdapterUpdateHandler.hasMessages(ADAPTER_UPDATE_MSG)) {
            mAdapterUpdateHandler.sendEmptyMessageDelayed(ADAPTER_UPDATE_MSG,
                    ADAPTER_UPDATE_DELAY);
        }
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    protected abstract void updateAdapter();

    protected void onPlaybackStateChanged(PlaybackStateCompat playbackState) {
    }

    protected void onMetadataChanged(MediaMetadataCompat metadata) {
    }

    protected void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
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

    protected void forceResolveVisibleItems(boolean reresolve) {
        if (reresolve) {
            mCorrespondingQueries.clear();
        }
        mResolveQueriesHandler.removeCallbacksAndMessages(null);
        mResolveQueriesHandler.sendEmptyMessageDelayed(RESOLVE_QUERIES_REPORTER_MSG,
                RESOLVE_QUERIES_REPORTER_DELAY);
    }

    private void resolveItemsFromTo(int start, int end) {
        if (mTomahawkListAdapter != null) {
            start = Math.max(start, 0);
            end = Math.min(end, mTomahawkListAdapter.getCount());
            for (int i = start; i < end; i++) {
                Object object = mTomahawkListAdapter.getItem(i);
                if (object instanceof List) {
                    for (Object item : (List) object) {
                        resolveItem(item);
                    }
                } else {
                    resolveItem(object);
                }
            }
        }
    }

    private void resolveItem(final Object object) {
        if (object instanceof PlaylistEntry || object instanceof Query) {
            Query query;
            if (object instanceof PlaylistEntry) {
                PlaylistEntry entry = (PlaylistEntry) object;
                query = entry.getQuery();
            } else {
                query = (Query) object;
            }
            if (!mCorrespondingQueries.contains(query)) {
                mCorrespondingQueries.add(PipeLine.get().resolve(query));
            }
        } else if (object instanceof StationPlaylist) {
            resolveItem((StationPlaylist) object);
        } else if (object instanceof Playlist) {
            resolveItem((Playlist) object);
        } else if (object instanceof SocialAction) {
            resolveItem((SocialAction) object);
        } else if (object instanceof Album) {
            resolveItem((Album) object);
        } else if (object instanceof Artist) {
            resolveItem((Artist) object);
        } else if (object instanceof User) {
            resolveItem((User) object);
        }
    }

    private void resolveItem(StationPlaylist stationPlaylist) {
        if (mResolvingItems.add(stationPlaylist)) {
            if (stationPlaylist.getArtists() != null) {
                for (Pair<Artist, String> pair : stationPlaylist.getArtists()) {
                    resolveItem(pair.first);
                }
            }
            if (stationPlaylist.getTracks() != null) {
                for (Pair<Track, String> pair : stationPlaylist.getTracks()) {
                    resolveItem(pair.first.getArtist());
                }
            }
        }
    }

    private void resolveItem(final Playlist playlist) {
        User.getSelf().done(new DoneCallback<User>() {
            @Override
            public void onDone(User user) {
                if (mUser == null || mUser == user) {
                    TomahawkRunnable r = new TomahawkRunnable(
                            TomahawkRunnable.PRIORITY_IS_DATABASEACTION) {
                        @Override
                        public void run() {
                            if (mResolvingItems.add(playlist)) {
                                Playlist pl = playlist;
                                if (pl.size() == 0) {
                                    pl = DatabaseHelper.get().getPlaylist(pl.getId());
                                }
                                if (pl != null && pl.size() > 0) {
                                    boolean isFavorites = mUser != null
                                            && pl == mUser.getFavorites();
                                    pl.updateTopArtistNames(isFavorites);
                                    DatabaseHelper.get().updatePlaylist(pl);
                                    if (pl.getTopArtistNames() != null) {
                                        for (int i = 0; i < pl.getTopArtistNames().length && i < 5;
                                                i++) {
                                            resolveItem(Artist.get(pl.getTopArtistNames()[i]));
                                        }
                                    }
                                } else {
                                    mResolvingItems.remove(pl);
                                }
                            }
                        }
                    };
                    ThreadManager.get().execute(r);
                }
            }
        });
    }

    private void resolveItem(SocialAction socialAction) {
        if (mResolvingItems.add(socialAction)) {
            if (socialAction.getTargetObject() != null) {
                resolveItem(socialAction.getTargetObject());
            }
            resolveItem(socialAction.getUser());
        }
    }

    private void resolveItem(Album album) {
        if (mResolvingItems.add(album)) {
            if (album.getImage() == null) {
                String requestId = InfoSystem.get().resolve(album);
                if (requestId != null) {
                    mCorrespondingRequestIds.add(requestId);
                }
            }
        }
        resolveItem(album.getArtist());
    }

    private void resolveItem(Artist artist) {
        if (mResolvingItems.add(artist)) {
            if (artist.getImage() == null) {
                String requestId = InfoSystem.get().resolve(artist, false);
                if (requestId != null) {
                    mCorrespondingRequestIds.add(requestId);
                }
            }
        }
    }

    private void resolveItem(User user) {
        if (mResolvingItems.add(user)) {
            if (user.getImage() == null) {
                String requestId = InfoSystem.get().resolve(user);
                if (requestId != null) {
                    mCorrespondingRequestIds.add(requestId);
                }
            }
        }
    }

    protected AdapterView.OnItemSelectedListener constructDropdownListener(final String prefKey) {
        return new AdapterView.OnItemSelectedListener() {
            @SuppressLint("CommitPrefEdits")
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (getDropdownPos(prefKey) != position) {
                    PreferenceUtils.edit().putInt(prefKey, position).commit();
                    updateAdapter();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
    }

    protected int getDropdownPos(String prefKey) {
        return PreferenceUtils.getInt(prefKey, 0);
    }

    private boolean shouldAutoResolve() {
        return mContainerFragmentClass == null
                || !mContainerFragmentClass.equals(SearchPagerFragment.class.getName())
                && (mCollection == null
                || mCollection.getId().equals(TomahawkApp.PLUGINNAME_HATCHET)
                || mCollection.getId().equals(TomahawkApp.PLUGINNAME_USERCOLLECTION));
    }
}

