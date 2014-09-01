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

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.ListItemString;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.utils.AdapterUtils;
import org.tomahawk.tomahawk_android.utils.MultiColumnClickListener;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to populate a {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}.
 */
public class TomahawkListAdapter extends StickyBaseAdapter {

    private TomahawkMainActivity mActivity;

    private Map<View, List<ViewHolder>> mViewHolderMap = new HashMap<View, List<ViewHolder>>();

    private List<Segment> mSegments;

    private int mRowCount;

    private MultiColumnClickListener mClickListener;

    private LayoutInflater mLayoutInflater;

    private boolean mShowPlaystate = false;

    private PlaylistEntry mHighlightedPlaylistEntry;

    private Query mHighlightedQuery;

    private boolean mHighlightedItemIsPlaying = false;

    private boolean mShowResolvedBy = false;

    private boolean mShowArtistAsSingleLine = false;

    private boolean mShowContentHeaderSpacer = false;

    /**
     * Constructs a new {@link TomahawkListAdapter}.
     */
    public TomahawkListAdapter(TomahawkMainActivity activity, LayoutInflater layoutInflater,
            List<Segment> segments, MultiColumnClickListener clickListener) {
        mActivity = activity;
        mLayoutInflater = layoutInflater;
        mClickListener = clickListener;
        setSegments(segments);
    }

    /**
     * Constructs a new {@link TomahawkListAdapter}.
     */
    public TomahawkListAdapter(TomahawkMainActivity activity, LayoutInflater layoutInflater,
            Segment segment, MultiColumnClickListener clickListener) {
        mActivity = activity;
        mLayoutInflater = layoutInflater;
        mClickListener = clickListener;
        setSegments(segment);
    }

    /**
     * Set the complete list of {@link Segment}
     */
    public void setSegments(List<Segment> segments) {
        mSegments = segments;
        mRowCount = 0;
        for (Segment segment : mSegments) {
            mRowCount += segment.size();
        }
        notifyDataSetChanged();
    }

    /**
     * Set the complete list of {@link Segment}
     */
    public void setSegments(Segment segment) {
        ArrayList<Segment> segments = new ArrayList<Segment>();
        segments.add(segment);
        mSegments = segments;
        mRowCount = segment.size();
        notifyDataSetChanged();
    }

    public void setShowContentHeaderSpacer(boolean showContentHeaderSpacer) {
        mShowContentHeaderSpacer = showContentHeaderSpacer;
    }

    /**
     * Set whether or not to highlight the currently playing {@link TomahawkListItem} and show the
     * play/pause state
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
     * Set whether or not to show by which {@link org.tomahawk.libtomahawk.resolver.Resolver} the
     * {@link TomahawkListItem} has been resolved
     */
    public void setShowResolvedBy(boolean showResolvedBy) {
        this.mShowResolvedBy = showResolvedBy;
    }

    public void setShowArtistAsSingleLine(boolean showArtistAsSingleLine) {
        mShowArtistAsSingleLine = showArtistAsSingleLine;
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
        final Object o = getItem(position);

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

        List<ViewHolder> viewHolders = new ArrayList<ViewHolder>();
        if (convertView != null) {
            viewHolders = mViewHolderMap.get(convertView);
            view = convertView;
        }
        int viewType = getViewType(o, shouldBeHighlighted,
                mShowContentHeaderSpacer && position == 0);
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
                || viewHolders.get(0).getLayoutId() != viewType) {
            // If the viewHolder is null or the old viewType is different than the new one,
            // we need to inflate a new view and construct a new viewHolder,
            // which we set as the view's tag
            viewHolders = new ArrayList<ViewHolder>();
            if (o instanceof List) {
                LinearLayout rowContainer = (LinearLayout) mLayoutInflater
                        .inflate(R.layout.row_container, parent, false);
                int horizontalPadding = getSegment(position).getHorizontalPadding() / 2;
                rowContainer.setPadding(0, 0, 0, horizontalPadding);
                for (int i = 0; i < ((List) o).size(); i++) {
                    if (((List) o).get(i) != null) {
                        View gridItem = mLayoutInflater.inflate(viewType, rowContainer, false);
                        int verticalPadding = getSegment(position).getVerticalPadding();
                        int leftPadding = verticalPadding / 2;
                        if (i == 0) {
                            leftPadding = verticalPadding;
                        }
                        int rightPadding = verticalPadding / 2;
                        if (i == ((List) o).size() - 1) {
                            rightPadding = verticalPadding;
                        }
                        gridItem.setPadding(leftPadding, verticalPadding / 2, rightPadding,
                                verticalPadding / 2);
                        ViewHolder viewHolder = new ViewHolder(gridItem, viewType);
                        rowContainer.addView(gridItem);
                        viewHolders.add(viewHolder);
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
            }
            mViewHolderMap.put(view, viewHolders);
        } else if (viewType == R.layout.list_item
                || viewType == R.layout.list_item_highlighted) {
            for (ViewHolder viewHolder : viewHolders) {
                viewHolder.getImageView1().setVisibility(View.GONE);
                viewHolder.getImageView2().setVisibility(View.GONE);
                viewHolder.getTextView2().setVisibility(View.GONE);
                viewHolder.getTextView3().setVisibility(View.GONE);
                viewHolder.getTextView4().setVisibility(View.GONE);
                viewHolder.getTextView5().setVisibility(View.GONE);
            }
        }

        // After we've setup the correct view and viewHolder, we now can fill the View's
        // components with the correct data
        for (int i = 0; i < viewHolders.size(); i++) {
            ViewHolder viewHolder = viewHolders.get(i);
            TomahawkListItem item = o instanceof List ? (TomahawkListItem) ((List) o).get(i)
                    : (TomahawkListItem) o;
            if (viewHolder.getLayoutId() == R.layout.album_art_grid_item) {
                viewHolder.getTextView1().setText(item.getName());
                viewHolder.getTextView2().setVisibility(View.VISIBLE);
                viewHolder.getTextView2().setText(item.getArtist()
                        .getName());
                if (item instanceof Album || item instanceof Artist) {
                    TomahawkUtils.loadImageIntoImageView(mActivity,
                            viewHolder.getImageView1(),
                            item.getImage(),
                            Image.getSmallImageSize());
                }
            }
            if (viewHolder.getLayoutId() == R.layout.single_line_list_item) {
                viewHolder.getTextView1().setText(item.getName());
            } else if (viewHolder.getLayoutId() == R.layout.list_item_text) {
                AdapterUtils.fillView(mActivity, viewHolder, (ListItemString) item);
            } else if (viewHolder.getLayoutId() == R.layout.list_item
                    || viewHolder.getLayoutId() == R.layout.list_item_highlighted) {
                if (item instanceof Query) {
                    AdapterUtils.fillView(mActivity, viewHolder, (Query) item,
                            mHighlightedItemIsPlaying && shouldBeHighlighted, mShowResolvedBy);
                } else if (item instanceof PlaylistEntry) {
                    AdapterUtils
                            .fillView(mActivity, viewHolder,
                                    ((PlaylistEntry) item).getQuery(),
                                    mHighlightedItemIsPlaying && shouldBeHighlighted,
                                    mShowResolvedBy);
                } else if (item instanceof Album) {
                    AdapterUtils.fillView(mActivity, viewHolder, (Album) item);
                } else if (item instanceof Artist) {
                    AdapterUtils.fillView(mActivity, viewHolder, (Artist) item);
                } else if (item instanceof User) {
                    AdapterUtils.fillView(mActivity, viewHolder, (User) item);
                } else if (item instanceof SocialAction) {
                    AdapterUtils.fillView(mActivity, viewHolder, (SocialAction) item,
                            mHighlightedItemIsPlaying && shouldBeHighlighted, mShowResolvedBy);
                }
            }

            //Set up the click listeners
            if (viewHolder.getLayoutId() == R.layout.list_item
                    || viewHolder.getLayoutId() == R.layout.list_item_highlighted) {
                if (item instanceof SocialAction || item instanceof User) {
                    User user;
                    if (item instanceof SocialAction) {
                        user = ((SocialAction) item).getUser();
                    } else {
                        user = (User) item;
                    }
                    viewHolder.getClickArea1().setOnClickListener(
                            new ClickListener(user, mClickListener));
                    viewHolder.getClickArea1().setOnLongClickListener(
                            new ClickListener(user, mClickListener));
                }
            }
            viewHolder.getMainClickArea().setOnClickListener(
                    new ClickListener(item, mClickListener));
            viewHolder.getMainClickArea().setOnLongClickListener(
                    new ClickListener(item, mClickListener));
        }
        return view;
    }

    /**
     * @return the count of every item to display
     */
    @Override
    public int getCount() {
        return mRowCount + (mShowContentHeaderSpacer ? 1 : 0);
    }

    /**
     * @return item for the given position
     */
    @Override
    public Object getItem(int position) {
        if (mShowContentHeaderSpacer) {
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
                correctedPos -= counter;
            }
        }
        return null;
    }

    public Segment getSegment(int position) {
        if (mShowContentHeaderSpacer) {
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
        if (segment != null && segment.getHeaderStringResId() > 0) {
            View view = null;
            ViewHolder viewHolder = null;
            if (convertView != null) {
                viewHolder = (ViewHolder) convertView.getTag();
                view = convertView;
            }
            int layoutId = getHeaderViewType(segment);
            if (viewHolder == null || viewHolder.getLayoutId() != layoutId) {
                view = mLayoutInflater.inflate(layoutId, null);
                viewHolder = new ViewHolder(view, layoutId);
                view.setTag(viewHolder);
            }

            if (layoutId == R.layout.dropdown_header) {
                ArrayList<CharSequence> list = new ArrayList<CharSequence>();
                for (int resId : segment.getHeaderStringResIds()) {
                    list.add(TomahawkApp.getContext().getString(resId));
                }
                ArrayAdapter<CharSequence> adapter =
                        new ArrayAdapter<CharSequence>(TomahawkApp.getContext(),
                                R.layout.dropdown_header_textview, list);
                adapter.setDropDownViewResource(R.layout.dropdown_header_dropdown_textview);
                viewHolder.getSpinner().setAdapter(adapter);
                viewHolder.getSpinner().setSelection(segment.getInitialPos());
                viewHolder.getSpinner()
                        .setOnItemSelectedListener(segment.getSpinnerClickListener());
            } else if (layoutId == R.layout.single_line_list_header) {
                viewHolder.getTextView1().setText(segment.getHeaderStringResId());
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
            return 0;
        }
    }

    private int getViewType(Object item, boolean isHighlighted,
            boolean isContentHeaderItem) {
        if (item instanceof List) {
            //We have a grid item
            return R.layout.album_art_grid_item;
        }
        if (isContentHeaderItem) {
            return R.layout.content_header_spacer;
        } else if (item instanceof Playlist || (item instanceof Artist
                && mShowArtistAsSingleLine)) {
            return R.layout.single_line_list_item;
        } else if (isHighlighted) {
            return R.layout.list_item_highlighted;
        } else if (item instanceof ListItemString) {
            return R.layout.list_item_text;
        } else {
            return R.layout.list_item;
        }
    }

    private int getHeaderViewType(Segment segment) {
        if (segment.isSpinnerSegment()) {
            return R.layout.dropdown_header;
        } else {
            return R.layout.single_line_list_header;
        }
    }
}
