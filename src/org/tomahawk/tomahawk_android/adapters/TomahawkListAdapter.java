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
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.ui.widgets.SquareHeightRelativeLayout;
import org.tomahawk.tomahawk_android.utils.AdapterUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * This class is used to populate a {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}.
 */
public class TomahawkListAdapter extends TomahawkBaseAdapter implements StickyListHeadersAdapter {

    private LayoutInflater mLayoutInflater;

    private boolean mShowCategoryHeaders = false;

    private boolean mShowQueriesAsTopHits = false;

    private TomahawkListItem mContentHeaderTomahawkListItem;

    private boolean mShowPlaystate = false;

    private boolean mShowResolvedBy = false;

    private boolean mShowArtistAsSingleLine = false;

    /**
     * Constructs a new {@link TomahawkListAdapter}.
     *
     * @param activity  reference to whatever {@link Activity}
     * @param listArray complete set of lists containing all content which the listview should be
     *                  populated with
     */
    public TomahawkListAdapter(Activity activity, List<List<TomahawkListItem>> listArray) {
        mActivity = activity;
        mLayoutInflater = mActivity.getLayoutInflater();
        mListArray = listArray;
    }

    /**
     * Set whether or not a header should be shown above each "category". Like "Albums", "Tracks"
     * etc.
     */
    public void setShowCategoryHeaders(boolean showCategoryHeaders, boolean showQueriesAsTopHits) {
        mShowCategoryHeaders = showCategoryHeaders;
        mShowQueriesAsTopHits = showQueriesAsTopHits;
    }

    /**
     * Show a content header. A content header provides information about the current {@link
     * TomahawkListItem} that the user has navigated to. Like an AlbumArt image with the {@link
     * Album}s name, which is shown at the top of the listview, if the user browses to a particular
     * {@link Album} in his {@link org.tomahawk.libtomahawk.collection.UserCollection}.
     *
     * @param list        a reference to the list, so we can set its header view
     * @param listItem    the {@link TomahawkListItem}'s information to show in the header view
     * @param isOnlyLocal whether or not the given listItem was given in a local context. This will
     *                    determine whether to show the total track count, or just the count of
     *                    local tracks in the contentHeader's textview.
     */
    public void showContentHeader(StickyListHeadersListView list, TomahawkListItem listItem,
            boolean isOnlyLocal) {
        mContentHeaderTomahawkListItem = listItem;
        View contentHeaderView;
        boolean landscapeMode = mActivity.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        if (!landscapeMode && list.getHeaderViewsCount() == 0) {
            if (listItem instanceof User) {
                contentHeaderView = mLayoutInflater.inflate(R.layout.content_header_user, null);
            } else {
                contentHeaderView = mLayoutInflater.inflate(R.layout.content_header, null);
            }
            list.addHeaderView(contentHeaderView);
        }
        updateContentHeader(listItem, isOnlyLocal);
    }

    public void updateContentHeader(TomahawkListItem listItem, boolean isOnlyLocal) {
        SquareHeightRelativeLayout frame = (SquareHeightRelativeLayout)
                mActivity.findViewById(R.id.content_header_image_frame);
        if (frame != null) {
            frame.setVisibility(SquareHeightRelativeLayout.VISIBLE);
        }
        ViewHolder viewHolder = new ViewHolder(
                mActivity.getWindow().getDecorView().findViewById(android.R.id.content),
                R.id.tomahawklistadapter_viewtype_contentheader);
        if (listItem instanceof Album) {
            AdapterUtils.fillContentHeader(mActivity, viewHolder, (Album) listItem, isOnlyLocal);
        } else if (listItem instanceof Artist) {
            AdapterUtils.fillContentHeader(mActivity, viewHolder, (Artist) listItem, isOnlyLocal);
        } else if (listItem instanceof UserPlaylist) {
            AdapterUtils.fillContentHeader(mActivity, viewHolder, (UserPlaylist) listItem,
                    isOnlyLocal);
        } else if (listItem instanceof User) {
            AdapterUtils.fillContentHeader(mActivity, viewHolder, (User) listItem);
        }
    }

    /**
     * Set whether or not to highlight the currently playing {@link TomahawkListItem} and show the
     * play/pause state
     */
    public void setShowPlaystate(boolean showPlaystate) {
        this.mShowPlaystate = showPlaystate;
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
     * Set whether or not to show an AddButton, so that the user can add {@link UserPlaylist}s to
     * the database
     *
     * @param list       a reference to the list, so we can set its footer view
     * @param buttonText {@link String} containing the button's text to show
     */
    public void setShowAddButton(StickyListHeadersListView list, String buttonText) {
        View addButtonFooterView = mLayoutInflater.inflate(R.layout.add_button_layout, null);
        if (addButtonFooterView != null && list.getFooterViewsCount() == 0) {
            ((TextView) addButtonFooterView.findViewById(R.id.add_button_textview))
                    .setText(buttonText);
            list.addFooterView(addButtonFooterView);
        }
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
        TomahawkListItem item = (TomahawkListItem) getItem(position);

        if (item != null) {
            ViewHolder viewHolder = null;
            if (convertView != null) {
                viewHolder = (ViewHolder) convertView.getTag();
                view = convertView;
            }
            int viewType = getViewType(item);
            if (viewHolder == null || viewHolder.getViewType() != viewType) {
                // If the viewHolder is null or the old viewType is different than the new one,
                // we need to inflate a new view and construct a new viewHolder,
                // which we set as the view's tag
                if (viewType == R.id.tomahawklistadapter_viewtype_singlelinelistitem) {
                    view = mLayoutInflater.inflate(R.layout.single_line_list_item, null);
                    viewHolder = new ViewHolder(view, viewType);
                    view.setTag(viewHolder);
                } else if (viewType == R.id.tomahawklistadapter_viewtype_doublelinelistitem) {
                    view = mLayoutInflater.inflate(R.layout.double_line_list_item, null);
                    viewHolder = new ViewHolder(view, viewType);
                    view.setTag(viewHolder);
                }
            } else if (viewType == R.id.tomahawklistadapter_viewtype_doublelinelistitem) {
                viewHolder.getImageView1().setVisibility(View.GONE);
                viewHolder.getImageView2().setVisibility(View.GONE);
                viewHolder.getTextSecondLine().setVisibility(View.GONE);
                viewHolder.getTextThirdLine().setVisibility(View.GONE);
                viewHolder.getTextFourthLine().setVisibility(View.GONE);
                viewHolder.getTextFifthLine().setVisibility(View.GONE);
            }

            // After we've setup the correct view and viewHolder, we now can fill the View's
            // components with the correct data
            if (viewHolder.getViewType() == R.id.tomahawklistadapter_viewtype_singlelinelistitem) {
                viewHolder.getTextFirstLine().setText(item.getName());
            } else if (viewHolder.getViewType()
                    == R.id.tomahawklistadapter_viewtype_doublelinelistitem) {
                if (item instanceof Query) {
                    AdapterUtils.fillView(mActivity, viewHolder, view, (Query) item,
                            mShowPlaystate && position == mHighlightedItemPosition,
                            mHighlightedItemIsPlaying, mShowResolvedBy);
                } else if (item instanceof Album) {
                    AdapterUtils.fillView(mActivity, viewHolder, (Album) item);
                } else if (item instanceof Artist) {
                    AdapterUtils.fillView(mActivity, viewHolder, (Artist) item);
                } else if (item instanceof User) {
                    AdapterUtils.fillView(mActivity, viewHolder, (User) item);
                } else if (item instanceof SocialAction) {
                    AdapterUtils.fillView(mActivity, viewHolder, (SocialAction) item);
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
        if ((mFiltered ? mFilteredListArray : mListArray) == null) {
            return 0;
        }
        int displayedListArrayItemsCount = 0;
        int displayedContentHeaderCount = 0;
        for (List<TomahawkListItem> list : (mFiltered ? mFilteredListArray : mListArray)) {
            displayedListArrayItemsCount += list.size();
        }
        return displayedListArrayItemsCount + displayedContentHeaderCount;
    }

    /**
     * @return item for the given position
     */
    @Override
    public Object getItem(int position) {
        Object item = null;
        int offsetCounter = 0;
        for (int i = 0; i < (mFiltered ? mFilteredListArray : mListArray).size(); i++) {
            List<TomahawkListItem> list = (mFiltered ? mFilteredListArray : mListArray).get(i);
            if (position - offsetCounter < list.size()) {
                item = list.get(position - offsetCounter);
                break;
            }
            offsetCounter += list.size();
        }
        return item;
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
        TomahawkListItem item = (TomahawkListItem) getItem(position);
        if (mShowCategoryHeaders && item != null) {
            View view;
            ViewHolder viewHolder;
            if (convertView != null) {
                viewHolder = (ViewHolder) convertView.getTag();
                view = convertView;
            } else {
                view = mLayoutInflater.inflate(R.layout.single_line_list_header, null);
                viewHolder = new ViewHolder(view, R.id.tomahawklistadapter_viewtype_header);
                view.setTag(viewHolder);
            }

            if (item instanceof Track || item instanceof Query) {
                if (mShowQueriesAsTopHits) {
                    TomahawkUtils.loadDrawableIntoImageView(mActivity,
                            viewHolder.getImageView1(), R.drawable.ic_action_tophits);
                    viewHolder.getTextFirstLine().setText(R.string.tophits_categoryheaders_string);
                } else {
                    TomahawkUtils.loadDrawableIntoImageView(mActivity,
                            viewHolder.getImageView1(), R.drawable.ic_action_track);
                    viewHolder.getTextFirstLine().setText(R.string.tracksfragment_title_string);
                }
            } else if (item instanceof Artist) {
                TomahawkUtils.loadDrawableIntoImageView(mActivity, viewHolder.getImageView1(),
                        R.drawable.ic_action_artist);
                viewHolder.getTextFirstLine().setText(R.string.artistsfragment_title_string);
            } else if (item instanceof Album) {
                TomahawkUtils.loadDrawableIntoImageView(mActivity, viewHolder.getImageView1(),
                        R.drawable.ic_action_album);
                viewHolder.getTextFirstLine().setText(R.string.albumsfragment_title_string);
            } else if (item instanceof UserPlaylist) {
                TomahawkUtils.loadDrawableIntoImageView(mActivity, viewHolder.getImageView1(),
                        R.drawable.ic_action_playlist);
                if (((UserPlaylist) item).isHatchetPlaylist()) {
                    viewHolder.getTextFirstLine()
                            .setText(R.string.hatchet_userplaylists_categoryheaders_string);
                } else {
                    viewHolder.getTextFirstLine()
                            .setText(R.string.userplaylists_categoryheaders_string);
                }
            } else if (item instanceof User) {
                TomahawkUtils.loadDrawableIntoImageView(mActivity, viewHolder.getImageView1(),
                        R.drawable.ic_action_friends);
                viewHolder.getTextFirstLine().setText(R.string.userfragment_title_string);
            } else if (item instanceof SocialAction) {
                TomahawkUtils.loadDrawableIntoImageView(mActivity, viewHolder.getImageView1(),
                        R.drawable.ic_action_trending);
                viewHolder.getTextFirstLine().setText(R.string.content_header_activityfeed);
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
        long result = 0;
        int sizeSum = 0;
        for (List<TomahawkListItem> list : mFiltered ? mFilteredListArray : mListArray) {
            sizeSum += list.size();
            if (position < sizeSum) {
                break;
            } else {
                result++;
            }
        }
        return result;
    }

    /**
     * @return the {@link TomahawkListItem} shown in the content header
     */
    public TomahawkListItem getContentHeaderTomahawkListItem() {
        return mContentHeaderTomahawkListItem;
    }

    private int getViewType(TomahawkListItem item) {
        if (item instanceof UserPlaylist || (item instanceof Artist && mShowArtistAsSingleLine)) {
            return R.id.tomahawklistadapter_viewtype_singlelinelistitem;
        } else {
            return R.id.tomahawklistadapter_viewtype_doublelinelistitem;
        }
    }
}
