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
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.ListItemString;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetInfoPlugin;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.fragments.PlaylistsFragment;
import org.tomahawk.tomahawk_android.utils.MultiColumnClickListener;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
        SwipeItemMangerInterface,
        SwipeAdapterInterface {

    private TomahawkMainActivity mActivity;

    private List<Segment> mSegments;

    private int mRowCount;

    private MultiColumnClickListener mClickListener;

    private LayoutInflater mLayoutInflater;

    private boolean mShowPlaystate = false;

    private PlaylistEntry mHighlightedPlaylistEntry;

    private Query mHighlightedQuery;

    private boolean mHighlightedItemIsPlaying = false;

    private int mHeaderSpacerHeight = 0;

    private int mFooterSpacerHeight = 0;

    private ProgressBar mProgressBar;

    private SwipeItemAdapterMangerImpl mItemManager = new SwipeItemAdapterMangerImpl(this);

    private Handler mProgressHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (mProgressBar != null) {
                float pos = mActivity.getPlaybackService().getPosition();
                float duration = mActivity.getPlaybackService().getCurrentTrack().getDuration();
                mProgressBar.setProgress((int) (pos / duration * mProgressBar.getMax()));
                mProgressHandler.sendEmptyMessageDelayed(0, 500);
                return false;
            } else {
                mProgressHandler.removeCallbacksAndMessages(null);
                return true;
            }
        }
    });

    /**
     * Constructs a new {@link TomahawkListAdapter}.
     */
    public TomahawkListAdapter(TomahawkMainActivity activity, LayoutInflater layoutInflater,
            List<Segment> segments, StickyListHeadersListView listView,
            MultiColumnClickListener clickListener) {
        mActivity = activity;
        mLayoutInflater = layoutInflater;
        mClickListener = clickListener;
        mSegments = segments;
        mRowCount = 0;
        for (Segment segment : mSegments) {
            mRowCount += segment.size();
        }
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
        mSegments = new ArrayList<Segment>();
        mSegments.add(segment);
        mRowCount = segment.size();
        updateFooterSpacerHeight(listView);
        mItemManager.setMode(Attributes.Mode.Single);
    }

    /**
     * Set the complete list of {@link Segment}
     */
    public void setSegments(List<Segment> segments, StickyListHeadersListView listView) {
        mSegments = segments;
        mRowCount = 0;
        for (Segment segment : mSegments) {
            mRowCount += segment.size();
        }
        updateFooterSpacerHeight(listView);
        notifyDataSetChanged();
    }

    /**
     * Set the complete list of {@link Segment}
     */
    public void setSegments(Segment segment, StickyListHeadersListView listView) {
        ArrayList<Segment> segments = new ArrayList<Segment>();
        segments.add(segment);
        setSegments(segments, listView);
    }

    public void setShowContentHeaderSpacer(int headerSpacerHeight,
            StickyListHeadersListView listView) {
        mHeaderSpacerHeight = headerSpacerHeight;
        updateFooterSpacerHeight(listView);
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

        List<ViewHolder> viewHolders = new ArrayList<ViewHolder>();
        if (convertView != null) {
            viewHolders = (List<ViewHolder>) convertView.getTag();
            view = convertView;
        }
        int viewType = getViewType(o, mHeaderSpacerHeight > 0 && position == 0,
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
                || viewHolders.get(0).mLayoutId != viewType) {
            // If the viewHolder is null or the old viewType is different than the new one,
            // we need to inflate a new view and construct a new viewHolder,
            // which we set as the view's tag
            viewHolders = new ArrayList<ViewHolder>();
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
                if (viewType == R.layout.list_item_track) {
                    // We have a SwipeLayout
                    mItemManager.initialize(view, position);
                }
            }
            // Set extra padding
            if (getSegment(position) != null && getSegment(position).getLeftExtraPadding() > 0) {
                if (viewType == R.layout.list_item_track) {
                    // if it's a list_item_track, we have to set the padding on the foreground
                    // layout in the SwipeLayout instead
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
            if (viewType == R.layout.list_item_track) {
                // We have a SwipeLayout
                mItemManager.updateConvertView(view, position);
            }
            if (viewType == R.layout.list_item_track
                    || viewType == R.layout.grid_item
                    || viewType == R.layout.list_item_artistalbum
                    || viewType == R.layout.grid_item_user
                    || viewType == R.layout.list_item_user
                    || viewType == R.layout.grid_item_resolver
                    || viewType == R.layout.grid_item_playlist) {
                for (ViewHolder viewHolder : viewHolders) {
                    if (viewType == R.layout.list_item_track) {
                        viewHolder.mImageView1.setVisibility(View.GONE);
                        viewHolder.mTextView1.setVisibility(View.GONE);
                        viewHolder.mTextView3.setVisibility(View.GONE);
                        viewHolder.mTextView4.setVisibility(View.GONE);
                        viewHolder.mProgressBarContainer.removeAllViews();
                    } else if (viewType == R.layout.grid_item_resolver) {
                        viewHolder.mImageView1.clearColorFilter();
                    } else if (viewType == R.layout.grid_item_playlist) {
                        viewHolder.mMainClickArea.setBackgroundResource(0);
                        viewHolder.mImageView1.setVisibility(View.GONE);
                        viewHolder.mImageView2.setVisibility(View.VISIBLE);
                        viewHolder.mImageView3.setVisibility(View.GONE);
                        viewHolder.mAddIcon.setVisibility(View.GONE);
                    } else {
                        viewHolder.mTextView2.setVisibility(View.GONE);
                        viewHolder.mTextView3.setVisibility(View.GONE);
                    }
                }
            }
        }

        // After we've setup the correct view and viewHolder, we now can fill the View's
        // components with the correct data
        for (int i = 0; i < viewHolders.size(); i++) {
            ViewHolder viewHolder = viewHolders.get(i);
            Object item = o instanceof List ? ((List) o).get(i) : o;
            // Don't display the socialAction item directly, but rather the item that is its target
            if (item instanceof SocialAction && ((SocialAction) item).getTargetObject() != null) {
                item = ((SocialAction) item).getTargetObject();
            }
            if (viewHolder.mLayoutId == R.layout.content_footer_spacer) {
                view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        mFooterSpacerHeight));
            } else if (viewHolder.mLayoutId == R.layout.content_header_spacer) {
                view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        mHeaderSpacerHeight));
            } else if (viewHolder.mLayoutId == R.layout.grid_item
                    || viewHolder.mLayoutId == R.layout.list_item_artistalbum) {
                if (item instanceof Album) {
                    viewHolder.fillView((Album) item);
                } else if (item instanceof Artist) {
                    viewHolder.fillView((Artist) item);
                }
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
                viewHolder.fillView(((TomahawkListItem) item).getName());
            } else if (viewHolder.mLayoutId == R.layout.list_item_text) {
                viewHolder.fillView(((TomahawkListItem) item).getName());
            } else if (viewHolder.mLayoutId == R.layout.list_item_track) {
                if (item instanceof Query || item instanceof PlaylistEntry) {
                    String numerationString = null;
                    if (!getSegment(position).isShowAsQueued() && getSegment(position)
                            .isShowNumeration()) {
                        numerationString = String.format("%02d", getPosInSegment(position)
                                + getSegment(position).getNumerationCorrection());
                    }
                    final Query query;
                    if (item instanceof PlaylistEntry) {
                        query = ((PlaylistEntry) item).getQuery();
                    } else {
                        query = (Query) item;
                    }
                    viewHolder.fillView(query, numerationString,
                            mHighlightedItemIsPlaying && shouldBeHighlighted,
                            getSegment(position).isShowNumeration(),
                            getSegment(position).isHideArtistName(), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mActivity.getPlaybackService().addQueryToQueue(query);
                                    closeAllItems();
                                }
                            }, getSegment(position).isShowAsQueued());
                }
                if (mHighlightedItemIsPlaying && shouldBeHighlighted) {
                    if (mProgressBar == null) {
                        mProgressBar = (ProgressBar) mLayoutInflater.inflate(R.layout.progressbar,
                                viewHolder.mProgressBarContainer, false);
                    }
                    if (mProgressBar.getParent() instanceof FrameLayout) {
                        ((FrameLayout) mProgressBar.getParent()).removeView(mProgressBar);
                    }
                    viewHolder.mProgressBarContainer.addView(mProgressBar);
                    mProgressHandler.sendEmptyMessage(0);
                }
            }

            //Set up the click listeners
            if (item != null && viewHolder.mMainClickArea != null) {
                viewHolder.setMainClickListener(new ClickListener(item, mClickListener));
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
        if (segment != null && (segment.getHeaderString() != null
                || segment.getFirstSegmentItem() instanceof SocialAction)) {
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
                ArrayList<CharSequence> list = new ArrayList<CharSequence>();
                for (String headerString : segment.getHeaderStrings()) {
                    list.add(headerString.toUpperCase());
                }
                ArrayAdapter<CharSequence> adapter =
                        new ArrayAdapter<CharSequence>(TomahawkApp.getContext(),
                                R.layout.dropdown_header_textview, list);
                adapter.setDropDownViewResource(R.layout.dropdown_header_dropdown_textview);
                viewHolder.mSpinner1.setAdapter(adapter);
                viewHolder.mSpinner1.setSelection(segment.getInitialPos());
                viewHolder.mSpinner1.setOnItemSelectedListener(segment.getSpinnerClickListener());
            } else if (layoutId == R.layout.single_line_list_header) {
                viewHolder.mTextView1.setText(segment.getHeaderString().toUpperCase());
            } else if (layoutId == R.layout.list_header_socialaction) {
                SocialAction socialAction = (SocialAction) segment.getFirstSegmentItem();
                TomahawkUtils.loadUserImageIntoImageView(TomahawkApp.getContext(),
                        viewHolder.mUserImageView1, socialAction.getUser(),
                        Image.getSmallImageSize(), viewHolder.mUserTextView1);
                TomahawkListItem targetObject = socialAction.getTargetObject();
                Resources resources = view.getResources();
                String userName = socialAction.getUser().getName();
                String phrase = "!FIXME! type: " + socialAction.getType()
                        + ", action: " + socialAction.getAction() + ", user: " + userName;
                if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LOVE
                        .equals(socialAction.getType())) {
                    if (targetObject instanceof Query) {
                        phrase = segment.segmentSize() > 1 ?
                                resources.getString(R.string.socialaction_type_love_track_multiple,
                                        userName, segment.segmentSize())
                                : resources.getString(R.string.socialaction_type_love_track_single,
                                        userName);
                    } else if (targetObject instanceof Album) {
                        phrase = segment.segmentSize() > 1 ?
                                resources.getString(
                                        R.string.socialaction_type_collected_album_multiple,
                                        userName, segment.segmentSize())
                                : resources.getString(
                                        R.string.socialaction_type_collected_album_single,
                                        userName);
                    } else if (targetObject instanceof Artist) {
                        phrase = segment.segmentSize() > 1 ?
                                resources.getString(
                                        R.string.socialaction_type_collected_artist_multiple,
                                        userName, segment.segmentSize())
                                : resources.getString(
                                        R.string.socialaction_type_collected_artist_single,
                                        userName);
                    }
                } else if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_FOLLOW
                        .equals(socialAction.getType())) {
                    phrase = resources.getString(R.string.socialaction_type_follow, userName);
                } else if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_CREATEPLAYLIST
                        .equals(socialAction.getType())) {
                    phrase = segment.segmentSize() > 1 ?
                            resources.getString(R.string.socialaction_type_createplaylist_multiple,
                                    userName, segment.segmentSize())
                            : resources.getString(R.string.socialaction_type_createplaylist_single,
                                    userName);
                } else if (HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LATCHON
                        .equals(socialAction.getType())) {
                    phrase = segment.segmentSize() > 1 ?
                            resources.getString(R.string.socialaction_type_latchon_multiple,
                                    userName, segment.segmentSize())
                            : resources.getString(
                                    R.string.socialaction_type_latchon_single, userName);
                }
                viewHolder.mTextView1.setText(phrase + ":");
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

    private int getViewType(Object item, boolean isContentHeaderItem, boolean isFooter) {
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
                } else if (firstItem instanceof Playlist) {
                    return R.layout.grid_item_playlist;
                } else if (firstItem instanceof Integer) {
                    switch ((Integer) firstItem) {
                        case PlaylistsFragment.CREATE_PLAYLIST_BUTTON_ID:
                            return R.layout.grid_item_playlist;
                    }
                } else {
                    return R.layout.grid_item;
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
            return R.layout.list_item_text;
        } else if (item instanceof Album || item instanceof Artist) {
            return R.layout.list_item_artistalbum;
        } else if (item instanceof User) {
            return R.layout.list_item_user;
        } else {
            return R.layout.list_item_track;
        }
    }

    private int getHeaderViewType(Segment segment) {
        if (segment.isSpinnerSegment()) {
            return R.layout.dropdown_header;
        } else if (segment.getFirstSegmentItem() instanceof SocialAction) {
            return R.layout.list_header_socialaction;
        } else {
            return R.layout.single_line_list_header;
        }
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
}
