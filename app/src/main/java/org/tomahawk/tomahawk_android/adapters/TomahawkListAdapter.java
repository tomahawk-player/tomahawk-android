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

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.ListItemDrawable;
import org.tomahawk.libtomahawk.collection.ListItemString;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.StationPlaylist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.utils.ViewUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.listeners.MultiColumnClickListener;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.PreferenceUtils;
import org.tomahawk.tomahawk_android.views.BiDirectionalFrame;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * This class is used to populate a {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}.
 */
public class TomahawkListAdapter extends StickyBaseAdapter implements
        SwipeItemMangerInterface, SwipeAdapterInterface {

    private static final String TAG = TomahawkListAdapter.class.getSimpleName();

    private final TomahawkMainActivity mActivity;

    private List<Segment> mSegments;

    private int mRowCount;

    private Collection mCollection;

    private final MultiColumnClickListener mClickListener;

    private final LayoutInflater mLayoutInflater;

    private PlaylistEntry mHighlightedPlaylistEntry;

    private Query mHighlightedQuery;

    private boolean mHighlightedItemIsPlaying = false;

    private int mHeaderSpacerHeight = 0;

    private View mHeaderSpacerForwardView;

    private int mFooterSpacerHeight = 0;

    private ProgressBar mProgressBar;

    private final SwipeItemAdapterMangerImpl mItemManager = new SwipeItemAdapterMangerImpl(this);

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
        closeSegments(segments);
        mSegments = segments;
        mRowCount = 0;
        for (Segment segment : mSegments) {
            mRowCount += segment.getRowCount();
        }
    }

    public void closeSegments(List<Segment> newSegments) {
        if (mSegments != null) {
            for (Segment segment : mSegments) {
                if (newSegments == null || !newSegments.contains(segment)) {
                    segment.close();
                }
            }
        }
    }

    public void setShowContentHeaderSpacer(int headerSpacerHeight,
            StickyListHeadersListView listView, View headerSpacerForwardView) {
        mHeaderSpacerHeight = headerSpacerHeight;
        mHeaderSpacerForwardView = headerSpacerForwardView;
        updateFooterSpacerHeight(listView);
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
            shouldBeHighlighted = mHighlightedQuery != null
                    && ((SocialAction) o).getQuery() == mHighlightedQuery;
        } else if (o instanceof PlaylistEntry) {
            shouldBeHighlighted = mHighlightedPlaylistEntry != null
                    && o == mHighlightedPlaylistEntry;
        } else if (o instanceof Query) {
            shouldBeHighlighted = mHighlightedQuery != null
                    && o == mHighlightedQuery;
        }

        List<ViewHolder> viewHolders = new ArrayList<>();
        if (convertView != null) {
            viewHolders = (List<ViewHolder>) convertView.getTag();
            view = convertView;
        }
        int viewType = getViewType(o, position, mHeaderSpacerHeight > 0 && position == 0,
                position == getCount() - 1);
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
                || viewHolders.size() == 0 || viewHolders.get(0).mLayoutId != viewType) {
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
                    if (!PreferenceUtils.getBoolean(
                            PreferenceUtils.COACHMARK_SWIPELAYOUT_ENQUEUE_DISABLED)) {
                        ((SwipeLayout) view).addSwipeListener(new SwipeLayout.SwipeListener() {
                            @Override
                            public void onStartOpen(SwipeLayout swipeLayout) {
                            }

                            @Override
                            public void onOpen(SwipeLayout swipeLayout) {
                                if (!PreferenceUtils.getBoolean(
                                        PreferenceUtils.COACHMARK_SWIPELAYOUT_ENQUEUE_DISABLED)) {
                                    final View coachMark = ViewUtils.ensureInflation(
                                            swipeLayout, R.id.swipelayout_enqueue_coachmark_stub,
                                            R.id.swipelayout_enqueue_coachmark);
                                    coachMark.setVisibility(View.VISIBLE);
                                    View closeButton = coachMark.findViewById(R.id.close_button);
                                    closeButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            PreferenceUtils.edit().putBoolean(
                                                    PreferenceUtils.COACHMARK_SWIPELAYOUT_ENQUEUE_DISABLED,
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
            Object item = getItem(position);
            if (item instanceof List) {
                item = ((List) item).get(i);
            }
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
                Object targetItem = item;
                // Don't display the socialAction item directly, but rather the item that is its target
                if (item instanceof SocialAction
                        && ((SocialAction) item).getTargetObject() != null) {
                    targetItem = ((SocialAction) item).getTargetObject();
                }
                if (viewHolder.mLayoutId == R.layout.grid_item_artist
                        || viewHolder.mLayoutId == R.layout.list_item_artist) {
                    String numerationString = null;
                    if (getSegment(position).isShowNumeration()) {
                        int pos = getPosInSegment(position) * viewHolders.size() + i;
                        numerationString = "" + getSegment(position).getNumeration(pos);
                    }
                    viewHolder.fillView((Artist) targetItem, numerationString);
                } else if (viewHolder.mLayoutId == R.layout.grid_item_album
                        || viewHolder.mLayoutId == R.layout.list_item_album) {
                    String numerationString = null;
                    if (getSegment(position).isShowNumeration()) {
                        int pos = getPosInSegment(position) * viewHolders.size() + i;
                        numerationString = "" + getSegment(position).getNumeration(pos);
                    }
                    viewHolder.fillView((Album) targetItem, mCollection, numerationString);
                } else if (viewHolder.mLayoutId == R.layout.grid_item_resolver) {
                    viewHolder.fillView((Resolver) targetItem);
                } else if (viewHolder.mLayoutId == R.layout.grid_item_playlist
                        || viewHolder.mLayoutId == R.layout.grid_item_station) {
                    if (targetItem instanceof StationPlaylist) {
                        viewHolder.fillView((StationPlaylist) targetItem);
                    } else {
                        viewHolder.fillView((Playlist) targetItem);
                    }
                } else if (viewHolder.mLayoutId == R.layout.grid_item_user
                        || viewHolder.mLayoutId == R.layout.list_item_user) {
                    viewHolder.fillView((User) targetItem);
                } else if (viewHolder.mLayoutId == R.layout.single_line_list_item) {
                    viewHolder.fillView(((Playlist) targetItem).getName());
                } else if (viewHolder.mLayoutId == R.layout.list_item_text
                        || viewHolder.mLayoutId == R.layout.list_item_text_highlighted) {
                    viewHolder.fillView(((ListItemString) targetItem).getText());
                } else if (viewHolder.mLayoutId == R.layout.list_item_image) {
                    viewHolder.fillView((ListItemDrawable) targetItem);
                } else if (viewHolder.mLayoutId == R.layout.list_item_track_artist
                        || viewType == R.layout.list_item_numeration_track_artist
                        || viewType == R.layout.list_item_numeration_track_duration
                        || viewType == R.layout.list_item_track_artist_queued) {
                    if (targetItem instanceof Track) {
                        viewHolder.fillView((Track) targetItem);
                    } else {
                        View coachMark =
                                viewHolder.findViewById(R.id.swipelayout_enqueue_coachmark);
                        if (coachMark != null) {
                            coachMark.setVisibility(View.GONE);
                        }
                        String numerationString = null;
                        boolean isShowAsQueued =
                                getSegment(position).isShowAsQueued(getPosInSegment(position));
                        if (!isShowAsQueued && getSegment(position).isShowNumeration()) {
                            int pos = getPosInSegment(position) * viewHolders.size() + i;
                            numerationString = String.format("%02d",
                                    getSegment(position).getNumeration(pos));
                        }
                        final Query query;
                        final PlaylistEntry entry;
                        if (targetItem instanceof PlaylistEntry) {
                            query = ((PlaylistEntry) targetItem).getQuery();
                            entry = (PlaylistEntry) targetItem;
                        } else {
                            query = (Query) targetItem;
                            entry = null;
                        }

                        View.OnClickListener dequeueListener = null;
                        if (mHighlightedPlaylistEntry != entry) {
                            dequeueListener = new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Bundle extras = new Bundle();
                                    extras.putString(TomahawkFragment.PLAYLISTENTRY,
                                            entry.getCacheKey());
                                    mActivity.getSupportMediaController().getTransportControls()
                                            .sendCustomAction(
                                                    PlaybackService.ACTION_DELETE_ENTRY_IN_QUEUE,
                                                    extras);
                                }
                            };
                        }
                        viewHolder.fillView(query, numerationString,
                                mHighlightedItemIsPlaying && shouldBeHighlighted, isShowAsQueued,
                                dequeueListener, getSegment(position).isShowResolverIcon());

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
                }

                //Set up the click listeners
                viewHolder.setMainClickListener(item, getSegment(position), mClickListener);
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
            counter += segment.getRowCount();
            if (position < counter) {
                return segment.get(correctedPos);
            } else {
                correctedPos -= segment.getRowCount();
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
            counter += segment.getRowCount();
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
            counter += segment.getRowCount();
            if (position < counter) {
                return correctedPos;
            } else {
                correctedPos -= segment.getRowCount();
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
                viewHolder.fillHeaderView(socialAction, segment.getCount());
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

    private int getViewType(Object item, int position, boolean isContentHeaderItem,
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
                } else if (firstItem instanceof StationPlaylist) {
                    return R.layout.grid_item_station;
                } else if (firstItem instanceof Playlist) {
                    return R.layout.grid_item_playlist;
                } else {
                    Log.e(TAG, "getViewType - Couldn't find appropriate viewType!");
                    return 0;
                }
            }
        }
        if (item instanceof SocialAction && ((SocialAction) item).getTargetObject() != null) {
            item = ((SocialAction) item).getTargetObject();
        }
        Segment segment = getSegment(position);
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
        } else if (item instanceof ListItemDrawable) {
            return R.layout.list_item_image;
        } else if (item instanceof Album) {
            return R.layout.list_item_album;
        } else if (item instanceof Artist) {
            return R.layout.list_item_artist;
        } else if (item instanceof User) {
            return R.layout.list_item_user;
        } else if (segment != null && segment.isHideArtistName()) {
            return R.layout.list_item_numeration_track_duration;
        } else if (segment != null && segment.isShowAsQueued(getPosInSegment(position))) {
            return R.layout.list_item_track_artist_queued;
        } else if (segment != null && segment.isShowNumeration()) {
            return R.layout.list_item_numeration_track_artist;
        } else if (segment != null && segment.isShowResolverIcon()) {
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
            ViewUtils.afterViewGlobalLayout(new ViewUtils.ViewRunnable(listView) {
                @Override
                public void run() {
                    mFooterSpacerHeight = calculateFooterSpacerHeight(listView);
                    notifyDataSetChanged();
                }
            });
        }
    }

    private int calculateFooterSpacerHeight(StickyListHeadersListView listView) {
        if (getCount() > 10) {
            return 0;
        }
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
    public int getSwipeLayoutResourceId(int position) {
        return 0;
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

    public ProgressBar getProgressBar() {
        return mProgressBar;
    }
}
