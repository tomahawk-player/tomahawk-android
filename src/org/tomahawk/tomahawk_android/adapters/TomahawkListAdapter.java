/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.adapters;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.implments.SwipeItemAdapterMangerImpl;
import com.daimajia.swipe.interfaces.SwipeAdapterInterface;
import com.daimajia.swipe.interfaces.SwipeItemMangerInterface;
import com.daimajia.swipe.util.Attributes;

import org.jdeferred.DonePipe;
import org.jdeferred.Promise;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.ListItemString;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.utils.ADeferredObject;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.fragments.PlaylistsFragment;
import org.tomahawk.tomahawk_android.utils.MultiColumnClickListener;
import org.tomahawk.tomahawk_android.views.BiDirectionalFrame;

import android.content.SharedPreferences;
import android.database.StaleDataException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * This class is used to populate a {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}.
 */
public class TomahawkListAdapter extends StickyBaseAdapter implements
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
        SwipeItemMangerInterface,
        SwipeAdapterInterface {

    private static final String TAG = TomahawkListAdapter.class.getSimpleName();

    public static final String TEMP_PLAYLIST_NAME = "Temporary playlist";

    private final TomahawkMainActivity mActivity;

    private List<Segment> mSegments;

    private boolean mSegmentsClosed;

    private int mRowCount;

    private Collection mCollection;

    private final MultiColumnClickListener mClickListener;

    private final LayoutInflater mLayoutInflater;

    private boolean mShowPlaystate = false;

    private PlaylistEntry mHighlightedPlaylistEntry;

    private Query mHighlightedQuery;

    private boolean mHighlightedItemIsPlaying = false;

    private int mHeaderSpacerHeight = 0;

    private View mHeaderSpacerForwardView;

    private int mFooterSpacerHeight = 0;

    private ProgressBar mProgressBar;

    private final SwipeItemAdapterMangerImpl mItemManager = new SwipeItemAdapterMangerImpl(this);

    private ADeferredObject<Playlist, Throwable, Void> mGetPlaylistPromise;

    private final Map<Object, PlaylistEntry> mPlaylistEntryMap = new HashMap<>();

    /**
     * Constructs a new {@link TomahawkListAdapter}.
     */
    public TomahawkListAdapter(TomahawkMainActivity activity, LayoutInflater layoutInflater,
            List<Segment> segments, Collection collection, StickyListHeadersListView listView,
            MultiColumnClickListener clickListener) {
        mActivity = activity;
        mLayoutInflater = layoutInflater;
        mClickListener = clickListener;
        setSegments(segments);
        updateFooterSpacerHeight(listView);
        mItemManager.setMode(Attributes.Mode.Single);
        mCollection = collection;
    }

    /**
     * Constructs a new {@link TomahawkListAdapter}.
     */
    public TomahawkListAdapter(TomahawkMainActivity activity, LayoutInflater layoutInflater,
            List<Segment> segments, StickyListHeadersListView listView,
            MultiColumnClickListener clickListener) {
        mActivity = activity;
        mLayoutInflater = layoutInflater;
        mClickListener = clickListener;
        setSegments(segments);
        updateFooterSpacerHeight(listView);
        mItemManager.setMode(Attributes.Mode.Single);
    }

    /**
     * Constructs a new {@link TomahawkListAdapter}.
     */
    public TomahawkListAdapter(TomahawkMainActivity activity, LayoutInflater layoutInflater,
            Segment segment, StickyListHeadersListView listView,
            MultiColumnClickListener clickListener) {
        mActivity = activity;
        mLayoutInflater = layoutInflater;
        mClickListener = clickListener;
        List<Segment> segments = new ArrayList<>();
        segments.add(segment);
        setSegments(segments);
        updateFooterSpacerHeight(listView);
        mItemManager.setMode(Attributes.Mode.Single);
    }

    /**
     * Set the complete list of {@link Segment}
     */
    public void setSegments(List<Segment> segments, StickyListHeadersListView listView) {
        setSegments(segments);

        updateFooterSpacerHeight(listView);
        notifyDataSetChanged();
    }

    private void setSegments(List<Segment> segments) {
        closeSegments();
        mSegments = segments;
        mSegmentsClosed = false;
        mRowCount = 0;
        for (Segment segment : mSegments) {
            mRowCount += segment.size();
        }

        synchronized (this) {
            mGetPlaylistPromise = null;
        }
    }

    public void closeSegments() {
        if (mSegments != null) {
            mSegmentsClosed = true;
            for (Segment segment : mSegments) {
                segment.close();
            }
        }
    }

    private void extractPlaylistEntry(Playlist playlist, Object item) {
        Query q = null;
        if (item instanceof SocialAction
                && ((SocialAction) item).getTargetObject() instanceof Query) {
            item = ((SocialAction) item).getTargetObject();
            q = (Query) item;
        } else if (item instanceof Query) {
            q = (Query) item;
        } else if (item instanceof PlaylistEntry) {
            q = ((PlaylistEntry) item).getQuery();
        }
        if (q != null) {
            PlaylistEntry entry = playlist.addQuery(q);
            mPlaylistEntryMap.put(item, entry);
        }
    }

    public synchronized Promise<Playlist, Throwable, Void> getPlaylist() {
        if (mGetPlaylistPromise == null) {
            final ADeferredObject<Playlist, Throwable, Void> promise = new ADeferredObject<>();
            mGetPlaylistPromise = promise;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Playlist playlist = Playlist.fromEntriesList(
                            TEMP_PLAYLIST_NAME, "", new ArrayList<PlaylistEntry>());
                    mPlaylistEntryMap.clear();
                    for (int i = 0; i < getCount(); i++) {
                        synchronized (TomahawkListAdapter.this) {
                            if (mGetPlaylistPromise != promise || mSegmentsClosed) {
                                promise.reject(null);
                                return;
                            }
                        }
                        try {
                            Object object = getItem(i);
                            if (object instanceof List) {
                                for (Object item : (List) object) {
                                    extractPlaylistEntry(playlist, item);
                                }
                            } else {
                                extractPlaylistEntry(playlist, object);
                            }
                        } catch (StaleDataException e) {
                            Log.d(TAG, "getPlaylist - Cursor closed. Aborting ...");
                            promise.reject(null);
                            return;
                        }
                    }
                    synchronized (TomahawkListAdapter.this) {
                        if (mGetPlaylistPromise != promise) {
                            promise.reject(null);
                        } else {
                            promise.resolve(playlist);
                        }
                    }
                }
            }).start();
        }
        return mGetPlaylistPromise;
    }

    public Promise<PlaylistEntry, Throwable, Void> getPlaylistEntry(final Object item) {
        return getPlaylist().then(new DonePipe<Playlist, PlaylistEntry, Throwable, Void>() {
            @Override
            public Promise<PlaylistEntry, Throwable, Void> pipeDone(Playlist result) {
                ADeferredObject<PlaylistEntry, Throwable, Void> deferred = new ADeferredObject<>();
                return deferred.resolve(mPlaylistEntryMap.get(item));
            }
        });
    }

    public void setShowContentHeaderSpacer(int headerSpacerHeight,
            StickyListHeadersListView listView, View headerSpacerForwardView) {
        mHeaderSpacerHeight = headerSpacerHeight;
        mHeaderSpacerForwardView = headerSpacerForwardView;
        updateFooterSpacerHeight(listView);
    }

    /**
     * Set whether or not to highlight the currently playing {@link Query} and show the play/pause
     * state
     */
    public void setShowPlaystate(boolean showPlaystate) {
        this.mShowPlaystate = showPlaystate;
    }

    public void setHighlightedItemIsPlaying(boolean highlightedItemIsPlaying) {
        mHighlightedItemIsPlaying = highlightedItemIsPlaying;
    }

    /**
     * set the position of the item, which should be highlighted
     */
    public void setHighlightedQuery(Query query) {
        mHighlightedQuery = query;
    }

    /**
     * set the position of the item, which should be highlighted
     */
    public void setHighlightedEntry(PlaylistEntry entry) {
        mHighlightedPlaylistEntry = entry;
    }

    /**
     * Get the correct {@link View} for the given position.
     *
     * @param position    The position for which to get the correct {@link View}
     * @param convertView The old {@link View}, which we might be able to recycle
     * @param parent      parental {@link ViewGroup}
     * @return the correct {@link View} for the given position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        Object o = getItem(position);

        // Don't display the socialAction item directly, but rather the item that is its target
        if (o instanceof SocialAction && ((SocialAction) o).getTargetObject() != null) {
            o = ((SocialAction) o).getTargetObject();
        }

        boolean shouldBeHighlighted = false;
        if (o instanceof SocialAction) {
            shouldBeHighlighted = mShowPlaystate && mHighlightedQuery != null
                    && ((SocialAction) o).getQuery() == mHighlightedQuery;
        } else if (o instanceof PlaylistEntry) {
            shouldBeHighlighted = mShowPlaystate && mHighlightedPlaylistEntry != null
                    && o == mHighlightedPlaylistEntry;
        } else if (o instanceof Query) {
            shouldBeHighlighted = mShowPlaystate && mHighlightedQuery != null
                    && o == mHighlightedQuery;
        }

        List<ViewHolder> viewHolders = new ArrayList<>();
        if (convertView != null) {
            viewHolders = (List<ViewHolder>) convertView.getTag();
            view = convertView;
        }
        int viewType = getViewType(o, getSegment(position),
                mHeaderSpacerHeight > 0 && position == 0, position == getCount() - 1);
        int expectedViewHoldersCount = 1;
        if (o instanceof List) {
            expectedViewHoldersCount = 0;
            for (Object object : (List) o) {
                if (object != null) {
                    expectedViewHoldersCount++;
                }
            }
        }
        if (viewHolders.size() != expectedViewHoldersCount
                || viewHolders.get(0).mLayoutId != viewType) {
            // If the viewHolder is null or the old viewType is different than the new one,
            // we need to inflate a new view and construct a new viewHolder,
            // which we set as the view's tag
            viewHolders = new ArrayList<>();
            if (o instanceof List) {
                LinearLayout rowContainer = (LinearLayout) mLayoutInflater
                        .inflate(R.layout.row_container, parent, false);
                rowContainer.setPadding(rowContainer.getPaddingLeft(),
                        getSegment(position).getVerticalPadding(), rowContainer.getPaddingRight(),
                        0);
                for (int i = 0; i < ((List) o).size(); i++) {
                    if (((List) o).get(i) != null) {
                        View gridItem = mLayoutInflater.inflate(viewType, rowContainer, false);
                        ViewHolder viewHolder = new ViewHolder(gridItem, viewType);
                        rowContainer.addView(gridItem);
                        viewHolders.add(viewHolder);
                        if (i < ((List) o).size() - 1) {
                            View spacer = new View(mLayoutInflater.getContext());
                            spacer.setLayoutParams(new ViewGroup.LayoutParams(
                                    getSegment(position).getHorizontalPadding(),
                                    ViewGroup.LayoutParams.MATCH_PARENT));
                            rowContainer.addView(spacer);
                        }
                    } else {
                        rowContainer.addView(mLayoutInflater
                                .inflate(R.layout.row_container_spacer, rowContainer, false));
                    }
                }
                view = rowContainer;
            } else {
                view = mLayoutInflater.inflate(viewType, parent, false);
                ViewHolder viewHolder = new ViewHolder(view, viewType);
                viewHolders.add(viewHolder);
                if (view instanceof SwipeLayout) {
                    mItemManager.initialize(view, position);
                    final SharedPreferences preferences =
                            PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext());
                    if (!preferences.getBoolean(
                            TomahawkMainActivity.COACHMARK_SWIPELAYOUT_ENQUEUE_DISABLED, false)) {
                        ((SwipeLayout) view).addSwipeListener(new SwipeLayout.SwipeListener() {
                            @Override
                            public void onStartOpen(SwipeLayout swipeLayout) {
                            }

                            @Override
                            public void onOpen(SwipeLayout swipeLayout) {
                                if (!preferences.getBoolean(
                                        TomahawkMainActivity.COACHMARK_SWIPELAYOUT_ENQUEUE_DISABLED,
                                        false)) {
                                    final View coachMark = TomahawkUtils.ensureInflation(
                                            swipeLayout, R.id.swipelayout_enqueue_coachmark_stub,
                                            R.id.swipelayout_enqueue_coachmark);
                                    coachMark.setVisibility(View.VISIBLE);
                                    View closeButton = coachMark.findViewById(R.id.close_button);
                                    closeButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            preferences.edit().putBoolean(
                                                    TomahawkMainActivity.COACHMARK_SWIPELAYOUT_ENQUEUE_DISABLED,
                                                    true).apply();
                                            coachMark.setVisibility(View.GONE);
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onStartClose(SwipeLayout swipeLayout) {
                            }

                            @Override
                            public void onClose(SwipeLayout swipeLayout) {
                                View coachMark = swipeLayout.findViewById(
                                        R.id.swipelayout_enqueue_coachmark);
                                if (coachMark != null) {
                                    coachMark.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onUpdate(SwipeLayout swipeLayout, int i, int i1) {
                            }

                            @Override
                            public void onHandRelease(SwipeLayout swipeLayout, float v, float v1) {
                            }
                        });
                    }
                }
            }
            // Set extra padding
            if (getSegment(position) != null && getSegment(position).getLeftExtraPadding() > 0) {
                if (view instanceof SwipeLayout) {
                    // if it's a SwipeLayout, we have to set the padding on the foreground
                    // layout within the SwipeLayout instead
                    View foreground = ((ViewGroup) view).getChildAt(1);
                    foreground.setPadding(foreground.getPaddingLeft() + getSegment(position)
                                    .getLeftExtraPadding(),
                            foreground.getPaddingTop(), foreground.getPaddingRight(),
                            foreground.getPaddingBottom());
                } else {
                    view.setPadding(
                            view.getPaddingLeft() + getSegment(position).getLeftExtraPadding(),
                            view.getPaddingTop(),
                            view.getPaddingRight(), view.getPaddingBottom());
                }
            }
            view.setTag(viewHolders);
        } else {
            if (view instanceof SwipeLayout) {
                // We have a SwipeLayout
                mItemManager.updateConvertView(view, position);
            }
        }

        // After we've setup the correct view and viewHolder, we now can fill the View's
        // components with the correct data
        for (int i = 0; i < viewHolders.size(); i++) {
            ViewHolder viewHolder = viewHolders.get(i);
            Object item = o instanceof List ? ((List) o).get(i) : o;
            if (item == null) {
                if (viewHolder.mLayoutId == R.layout.content_footer_spacer) {
                    view.setLayoutParams(
                            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    mFooterSpacerHeight));
                } else if (viewHolder.mLayoutId == R.layout.content_header_spacer) {
                    view.setLayoutParams(
                            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    mHeaderSpacerHeight));
                    if (mHeaderSpacerForwardView != null) {
                        BiDirectionalFrame biDirectionalFrame = (BiDirectionalFrame) view;
                        biDirectionalFrame.setForwardView(mHeaderSpacerForwardView);
                    }
                }
            } else {
                // Don't display the socialAction item directly, but rather the item that is its target
                if (item instanceof SocialAction
                        && ((SocialAction) item).getTargetObject() != null) {
                    item = ((SocialAction) item).getTargetObject();
                }
                if (viewHolder.mLayoutId == R.layout.grid_item_artist
                        || viewHolder.mLayoutId == R.layout.list_item_artist) {
                    viewHolder.fillView((Artist) item);
                } else if (viewHolder.mLayoutId == R.layout.grid_item_album
                        || viewHolder.mLayoutId == R.layout.list_item_album) {
                    viewHolder.fillView((Album) item, mCollection);
                } else if (viewHolder.mLayoutId == R.layout.grid_item_resolver) {
                    viewHolder.fillView((Resolver) item);
                } else if (viewHolder.mLayoutId == R.layout.grid_item_playlist) {
                    if (item instanceof Playlist) {
                        viewHolder.fillView((Playlist) item);
                    } else if (item instanceof Integer) {
                        viewHolder.fillView((int) item);
                    }
                } else if (viewHolder.mLayoutId == R.layout.grid_item_user
                        || viewHolder.mLayoutId == R.layout.list_item_user) {
                    viewHolder.fillView((User) item);
                } else if (viewHolder.mLayoutId == R.layout.single_line_list_item) {
                    viewHolder.fillView(((Playlist) item).getName());
                } else if (viewHolder.mLayoutId == R.layout.list_item_text
                        || viewHolder.mLayoutId == R.layout.list_item_text_highlighted) {
                    viewHolder.fillView(((ListItemString) item).getText());
                } else if (viewHolder.mLayoutId == R.layout.list_item_track_artist
                        || viewType == R.layout.list_item_numeration_track_artist
                        || viewType == R.layout.list_item_numeration_track_duration) {
                    View coachMark = viewHolder.findViewById(R.id.swipelayout_enqueue_coachmark);
                    if (coachMark != null) {
                        coachMark.setVisibility(View.GONE);
                    }
                    String numerationString = null;
                    if (!getSegment(position).isShowAsQueued() && getSegment(position)
                            .isShowNumeration()) {
                        numerationString = String.format("%02d", getPosInSegment(position)
                                + getSegment(position).getNumerationCorrection());
                    }
                    final Query query;
                    final PlaylistEntry entry;
                    if (item instanceof PlaylistEntry) {
                        query = ((PlaylistEntry) item).getQuery();
                        entry = (PlaylistEntry) item;
                    } else {
                        query = (Query) item;
                        entry = null;
                    }
                    View.OnClickListener swipeButtonListener;
                    boolean isShowAsQueued = getSegment(position).isShowAsQueued();
                    if (isShowAsQueued) {
                        swipeButtonListener = new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mActivity.getPlaybackService().deleteQueryInQueue(entry);
                                closeAllItems();
                            }
                        };
                    } else {
                        swipeButtonListener = new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mActivity.getPlaybackService().addQueryToQueue(query);
                                SharedPreferences preferences = PreferenceManager
                                        .getDefaultSharedPreferences(TomahawkApp.getContext());
                                preferences.edit().putBoolean(
                                        TomahawkMainActivity.COACHMARK_SWIPELAYOUT_ENQUEUE_DISABLED,
                                        true).apply();
                                closeAllItems();
                            }
                        };
                    }
                    viewHolder.fillView(query, numerationString,
                            mHighlightedItemIsPlaying && shouldBeHighlighted, swipeButtonListener,
                            isShowAsQueued);

                    FrameLayout progressBarContainer = (FrameLayout) viewHolder
                            .findViewById(R.id.progressbar_container);
                    if (mHighlightedItemIsPlaying && shouldBeHighlighted) {
                        if (mProgressBar == null) {
                            mProgressBar = (ProgressBar) mLayoutInflater
                                    .inflate(R.layout.progressbar,
                                            progressBarContainer, false);
                        }
                        if (mProgressBar.getParent() instanceof FrameLayout) {
                            ((FrameLayout) mProgressBar.getParent()).removeView(mProgressBar);
                        }
                        progressBarContainer.addView(mProgressBar);
                    } else {
                        progressBarContainer.removeAllViews();
                    }
                }

                //Set up the click listeners
                if (viewHolder.findViewById(R.id.mainclickarea) != null) {
                    viewHolder.setMainClickListener(new ClickListener(item, mClickListener));
                }
            }
        }
        return view;
    }

    /**
     * @return the count of every item to display
     */
    @Override
    public int getCount() {
        return mRowCount + (mHeaderSpacerHeight > 0 ? 1 : 0) + 1;
    }

    /**
     * @return item for the given position
     */
    @Override
    public Object getItem(int position) {
        if (mHeaderSpacerHeight > 0) {
            if (position == 0) {
                return null;
            } else {
                position--;
            }
        }
        int counter = 0;
        int correctedPos = position;
        for (Segment segment : mSegments) {
            counter += segment.size();
            if (position < counter) {
                return segment.get(correctedPos);
            } else {
                correctedPos -= segment.size();
            }
        }
        return null;
    }

    public Segment getSegment(int position) {
        if (mHeaderSpacerHeight > 0) {
            if (position == 0) {
                return null;
            } else {
                position--;
            }
        }
        int counter = 0;
        for (Segment segment : mSegments) {
            counter += segment.size();
            if (position < counter) {
                return segment;
            }
        }
        return null;
    }

    public int getPosInSegment(int position) {
        if (mHeaderSpacerHeight > 0) {
            if (position == 0) {
                return 0;
            } else {
                position--;
            }
        }
        int counter = 0;
        int correctedPos = position;
        for (Segment segment : mSegments) {
            counter += segment.size();
            if (position < counter) {
                return correctedPos;
            } else {
                correctedPos -= segment.size();
            }
        }
        return 0;
    }

    /**
     * Get the id of the item for the given position. (Id is equal to given position)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * This method is being called by the StickyListHeaders library. Get the correct header {@link
     * View} for the given position.
     *
     * @param position    The position for which to get the correct {@link View}
     * @param convertView The old {@link View}, which we might be able to recycle
     * @param parent      parental {@link ViewGroup}
     * @return the correct header {@link View} for the given position.
     */
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        Segment segment = getSegment(position);
        if (segment != null && segment.getHeaderLayoutId() > 0) {
            View view = null;
            ViewHolder viewHolder = null;
            if (convertView != null) {
                viewHolder = (ViewHolder) convertView.getTag();
                view = convertView;
            }
            int layoutId = getHeaderViewType(segment);
            if (viewHolder == null || viewHolder.mLayoutId != layoutId) {
                view = mLayoutInflater.inflate(layoutId, null);
                viewHolder = new ViewHolder(view, layoutId);
                view.setTag(viewHolder);
            }

            if (layoutId == R.layout.dropdown_header) {
                ArrayList<CharSequence> spinnerItems = new ArrayList<>();
                for (String headerString : segment.getHeaderStrings()) {
                    spinnerItems.add(headerString.toUpperCase());
                }
                viewHolder.fillHeaderView(spinnerItems, segment.getInitialPos(),
                        segment.getSpinnerClickListener());
            } else if (layoutId == R.layout.single_line_list_header) {
                viewHolder.fillHeaderView(segment.getHeaderString().toUpperCase());
            } else if (layoutId == R.layout.list_header_socialaction_fake) {
                viewHolder.fillHeaderView(segment.getHeaderString());
            } else if (layoutId == R.layout.list_header_socialaction) {
                SocialAction socialAction = (SocialAction) segment.getFirstSegmentItem();
                viewHolder.fillHeaderView(socialAction, segment.size());
            }
            return view;
        } else {
            return new View(mActivity);
        }
    }

    /**
     * This method is being called by the StickyListHeaders library. Returns the same value for each
     * item that should be grouped under the same header.
     *
     * @param position the position of the item for which to get the header id
     * @return the same value for each item that should be grouped under the same header.
     */
    @Override
    public long getHeaderId(int position) {
        Segment segment = getSegment(position);
        if (segment != null) {
            return mSegments.indexOf(segment);
        } else {
            return -1;
        }
    }

    private int getViewType(Object item, Segment segment, boolean isContentHeaderItem,
            boolean isFooter) {
        if (item instanceof List) {
            // We have a grid item
            // Don't display the socialAction item directly, but rather the item that is its target
            if (!((List) item).isEmpty()) {
                Object firstItem = ((List) item).get(0);
                if (firstItem instanceof SocialAction
                        && ((SocialAction) firstItem).getTargetObject() != null) {
                    firstItem = ((SocialAction) firstItem).getTargetObject();
                }
                if (firstItem instanceof User) {
                    return R.layout.grid_item_user;
                } else if (firstItem instanceof Resolver) {
                    return R.layout.grid_item_resolver;
                } else if (firstItem instanceof Artist) {
                    return R.layout.grid_item_artist;
                } else if (firstItem instanceof Album) {
                    return R.layout.grid_item_album;
                } else if (firstItem instanceof Playlist) {
                    return R.layout.grid_item_playlist;
                } else if (firstItem instanceof Integer) {
                    switch ((Integer) firstItem) {
                        case PlaylistsFragment.CREATE_PLAYLIST_BUTTON_ID:
                            return R.layout.grid_item_playlist;
                    }
                } else {
                    Log.e(TAG, "getViewType - Couldn't find appropriate viewType!");
                    return 0;
                }
            }
        }
        if (item instanceof SocialAction && ((SocialAction) item).getTargetObject() != null) {
            item = ((SocialAction) item).getTargetObject();
        }
        if (isContentHeaderItem) {
            return R.layout.content_header_spacer;
        } else if (isFooter) {
            return R.layout.content_footer_spacer;
        } else if (item instanceof Playlist) {
            return R.layout.single_line_list_item;
        } else if (item instanceof ListItemString) {
            if (((ListItemString) item).isHighlighted()) {
                return R.layout.list_item_text_highlighted;
            } else {
                return R.layout.list_item_text;
            }
        } else if (item instanceof Album) {
            return R.layout.list_item_album;
        } else if (item instanceof Artist) {
            return R.layout.list_item_artist;
        } else if (item instanceof User) {
            return R.layout.list_item_user;
        } else if (segment.isHideArtistName()) {
            return R.layout.list_item_numeration_track_duration;
        } else if (segment.isShowNumeration() || segment.isShowAsQueued()) {
            return R.layout.list_item_numeration_track_artist;
        } else {
            return R.layout.list_item_track_artist;
        }
    }

    private int getHeaderViewType(Segment segment) {
        return segment.getHeaderLayoutId();
    }

    private void updateFooterSpacerHeight(final StickyListHeadersListView listView) {
        if (mHeaderSpacerHeight > 0) {
            TomahawkUtils.afterViewGlobalLayout(new TomahawkUtils.ViewRunnable(listView) {
                @Override
                public void run() {
                    mFooterSpacerHeight = calculateFooterSpacerHeight(listView);
                    notifyDataSetChanged();
                }
            });
        }
    }

    private int calculateFooterSpacerHeight(StickyListHeadersListView listView) {
        int footerSpacerHeight = listView.getWrappedList().getHeight();
        long headerId = getHeaderId(0);
        for (int i = 1; i < getCount(); i++) {
            View view = getView(i, null, listView.getWrappedList());
            if (view != null) {
                view.measure(View.MeasureSpec.makeMeasureSpec(0,
                                View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0,
                                View.MeasureSpec.UNSPECIFIED));
                footerSpacerHeight -= view.getMeasuredHeight();
            }
            if (headerId != getHeaderId(i)) {
                headerId = getHeaderId(i);
                View headerView = getHeaderView(i, null, listView.getWrappedList());
                if (headerView != null) {
                    headerView.measure(View.MeasureSpec.makeMeasureSpec(0,
                                    View.MeasureSpec.UNSPECIFIED),
                            View.MeasureSpec.makeMeasureSpec(0,
                                    View.MeasureSpec.UNSPECIFIED));
                    footerSpacerHeight -= headerView.getMeasuredHeight();
                }
            }
            if (footerSpacerHeight <= 0) {
                footerSpacerHeight = 0;
                break;
            }
        }
        return footerSpacerHeight;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object o = getItem(position);
        if (!(o instanceof List)) {
            // Don't display the socialAction item directly, but rather the item that is its target
            if (o instanceof SocialAction && ((SocialAction) o).getTargetObject() != null) {
                o = ((SocialAction) o).getTargetObject();
            }
            mClickListener.onItemClick(view, o);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Object o = getItem(position);
        if (!(o instanceof List)) {
            // Don't display the socialAction item directly, but rather the item that is its target
            if (o instanceof SocialAction && ((SocialAction) o).getTargetObject() != null) {
                o = ((SocialAction) o).getTargetObject();
            }
            return mClickListener.onItemLongClick(view, o);
        }
        return false;
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipelayout;
    }

    @Override
    public void openItem(int position) {
        mItemManager.openItem(position);
    }

    @Override
    public void closeItem(int position) {
        mItemManager.closeItem(position);
    }

    @Override
    public void closeAllExcept(SwipeLayout layout) {
        mItemManager.closeAllExcept(layout);
    }

    @Override
    public void closeAllItems() {
        mItemManager.closeAllItems();
    }

    @Override
    public List<Integer> getOpenItems() {
        return mItemManager.getOpenItems();
    }

    @Override
    public List<SwipeLayout> getOpenLayouts() {
        return mItemManager.getOpenLayouts();
    }

    @Override
    public void removeShownLayouts(SwipeLayout layout) {
        mItemManager.removeShownLayouts(layout);
    }

    @Override
    public boolean isOpen(int position) {
        return mItemManager.isOpen(position);
    }

    @Override
    public Attributes.Mode getMode() {
        return mItemManager.getMode();
    }

    @Override
    public void setMode(Attributes.Mode mode) {
        mItemManager.setMode(mode);
    }

    public void onPlayPositionChanged(long duration, int currentPosition) {
        if (mProgressBar != null) {
            mProgressBar.setProgress(
                    (int) ((float) currentPosition / duration * mProgressBar.getMax()));
        }
    }
}
