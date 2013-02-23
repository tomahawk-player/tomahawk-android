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
package org.tomahawk.libtomahawk;

import com.emilsjolander.components.stickylistheaders.StickyListHeadersAdapter;

import org.tomahawk.libtomahawk.playlist.CustomPlaylist;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.SquareWidthRelativeLayout;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 */
public class TomahawkListAdapter extends TomahawkBaseAdapter implements StickyListHeadersAdapter {

    private LayoutInflater mLayoutInflater;

    private ResourceHolder mSingleLineListItemResourceHolder;

    private ResourceHolder mDoubleLineListItemResourceHolder;

    private ResourceHolder mCategoryHeaderResourceHolder;

    private boolean mShowCategoryHeaders = false;

    private boolean mShowPlaylistHeader = false;

    private boolean mShowContentHeader = false;

    private boolean mShowHighlightingAndPlaystate = false;

    private boolean mShowResolvedBy = false;

    /**
     * Constructs a new {@link TomahawkListAdapter} to display list items with a single line of text
     * and categoryHeaders
     */
    public TomahawkListAdapter(Activity activity, List<List<TomahawkListItem>> listArray) {
        mActivity = activity;
        mLayoutInflater = mActivity.getLayoutInflater();
        mListArray = listArray;
        mSingleLineListItemResourceHolder = new ResourceHolder(R.layout.single_line_list_item, -1,
                -1, R.id.single_line_list_textview, -1);
        mDoubleLineListItemResourceHolder = new ResourceHolder(R.layout.double_line_list_item,
                R.id.double_line_list_imageview, R.id.double_line_list_imageview2,
                R.id.double_line_list_textview, R.id.double_line_list_textview2);
    }

    public void setShowCategoryHeaders(boolean showCategoryHeaders) {
        mCategoryHeaderResourceHolder = new ResourceHolder(R.layout.single_line_list_header,
                R.id.single_line_list_header_icon_imageview, -1,
                R.id.single_line_list_header_textview, -1);
        mShowCategoryHeaders = showCategoryHeaders;
    }

    public void setShowPlaylistHeader(boolean showPlaylistHeader) {
        mCategoryHeaderResourceHolder = new ResourceHolder(R.layout.show_playlist_header, -1, -1,
                -1, -1);
        mShowPlaylistHeader = showPlaylistHeader;
    }

    public void setShowContentHeader(boolean showContentHeader, ListView list,
            TomahawkBaseAdapter.TomahawkListItem contentHeaderTomahawkListItem) {
        if (list.getHeaderViewsCount() == 0) {
            View contentHeaderView = mLayoutInflater.inflate(R.layout.content_header, null);
            if (contentHeaderView != null) {
                loadBitmap(contentHeaderTomahawkListItem,
                        (ImageView) contentHeaderView.findViewById(R.id.content_header_image));
                ((TextView) contentHeaderView.findViewById(R.id.content_header_textview))
                        .setText(contentHeaderTomahawkListItem.getName());
                if (contentHeaderTomahawkListItem.getArtist() != null
                        && contentHeaderTomahawkListItem instanceof Album) {
                    ((TextView) contentHeaderView.findViewById(R.id.content_header_textview2))
                            .setText(contentHeaderTomahawkListItem.getArtist().getName());
                }
            }
            list.addHeaderView(contentHeaderView);
            mShowContentHeader = showContentHeader;
        }
    }

    public void setShowHighlightingAndPlaystate(boolean showHighlightingAndPlaystate) {
        this.mShowHighlightingAndPlaystate = showHighlightingAndPlaystate;
    }

    public void setShowResolvedBy(boolean showResolvedBy) {
        this.mShowResolvedBy = showResolvedBy;
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        Object item = getItem(position);

        if (item != null) {
            ViewHolder viewHolder;
            if (((item instanceof Artist || item instanceof CustomPlaylist) && convertView == null)
                    || ((item instanceof Artist || item instanceof CustomPlaylist)
                    && ((ViewHolder) convertView.getTag()).viewType
                    != R.id.tomahawklistadapter_viewtype_singlelinelistitem)) {
                view = mLayoutInflater.inflate(mSingleLineListItemResourceHolder.resourceId, null);
                viewHolder = new ViewHolder(R.id.tomahawklistadapter_viewtype_singlelinelistitem,
                        (TextView) view
                                .findViewById(mSingleLineListItemResourceHolder.textViewId1));
                view.setTag(viewHolder);
            } else if (!mShowHighlightingAndPlaystate && !mShowResolvedBy && (
                    (item instanceof Track && convertView == null) || (item instanceof Track
                            && ((ViewHolder) convertView.getTag()).viewType
                            != R.id.tomahawklistadapter_viewtype_doublelinelistitem))) {
                view = mLayoutInflater.inflate(mDoubleLineListItemResourceHolder.resourceId, null);
                viewHolder = new ViewHolder(R.id.tomahawklistadapter_viewtype_doublelinelistitem,
                        (TextView) view.findViewById(mDoubleLineListItemResourceHolder.textViewId1),
                        (TextView) view
                                .findViewById(mDoubleLineListItemResourceHolder.textViewId2));
                view.setTag(viewHolder);
            } else if ((mShowResolvedBy || mShowHighlightingAndPlaystate) && (
                    (item instanceof Track && convertView == null) || (item instanceof Track
                            && ((ViewHolder) convertView.getTag()).viewType
                            != R.id.tomahawklistadapter_viewtype_doublelineplaystateimagelistitem))) {
                view = mLayoutInflater.inflate(mDoubleLineListItemResourceHolder.resourceId, null);
                viewHolder = new ViewHolder(
                        R.id.tomahawklistadapter_viewtype_doublelineplaystateimagelistitem,
                        (ImageView) view
                                .findViewById(mDoubleLineListItemResourceHolder.imageViewId),
                        (ImageView) view
                                .findViewById(mDoubleLineListItemResourceHolder.imageViewId2),
                        (TextView) view.findViewById(mDoubleLineListItemResourceHolder.textViewId1),
                        (TextView) view
                                .findViewById(mDoubleLineListItemResourceHolder.textViewId2));
                view.setTag(viewHolder);
            } else if ((item instanceof Album && convertView == null) || (item instanceof Album
                    && ((ViewHolder) convertView.getTag()).viewType
                    != R.id.tomahawklistadapter_viewtype_doublelineimagelistitem)) {
                view = mLayoutInflater.inflate(mDoubleLineListItemResourceHolder.resourceId, null);
                viewHolder = new ViewHolder(
                        R.id.tomahawklistadapter_viewtype_doublelineimagelistitem, (ImageView) view
                        .findViewById(mDoubleLineListItemResourceHolder.imageViewId),
                        (TextView) view.findViewById(mDoubleLineListItemResourceHolder.textViewId1),
                        (TextView) view
                                .findViewById(mDoubleLineListItemResourceHolder.textViewId2));
                view.setTag(viewHolder);
            } else {
                view = convertView;
                viewHolder = (ViewHolder) view.getTag();
            }

            if (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_singlelinelistitem) {
                if (item instanceof Artist) {
                    viewHolder.textFirstLine.setText(((Artist) item).getName());
                } else if (item instanceof CustomPlaylist) {
                    viewHolder.textFirstLine.setText(((CustomPlaylist) item).getName());
                }
            } else if (viewHolder.viewType
                    == R.id.tomahawklistadapter_viewtype_doublelinelistitem) {
                viewHolder.textFirstLine.setText(((Track) item).getName());
                viewHolder.textSecondLine.setText(((Track) item).getArtist().getName());
            } else if (viewHolder.viewType
                    == R.id.tomahawklistadapter_viewtype_doublelineimagelistitem) {
                if (item instanceof Album) {
                    viewHolder.textFirstLine.setText(((Album) item).getName());
                    if (((Album) item).getArtist() != null) {
                        viewHolder.textSecondLine.setText(((Album) item).getArtist().getName());
                    }
                    ((SquareWidthRelativeLayout) viewHolder.imageViewLeft.getParent())
                            .setVisibility(SquareWidthRelativeLayout.VISIBLE);
                    loadBitmap((Album) item, viewHolder.imageViewLeft);
                }
            } else if (viewHolder.viewType
                    == R.id.tomahawklistadapter_viewtype_doublelineplaystateimagelistitem) {
                if (item instanceof Track) {
                    viewHolder.textFirstLine.setText(((Track) item).getName());
                    viewHolder.textSecondLine.setText(((Track) item).getArtist().getName());
                    if (mShowHighlightingAndPlaystate) {
                        if (position == mHighlightedItemPosition) {
                            view.setBackgroundResource(R.color.pressed_tomahawk);
                            if (mHighlightedItemIsPlaying) {
                                ((SquareWidthRelativeLayout) viewHolder.imageViewLeft.getParent())
                                        .setVisibility(SquareWidthRelativeLayout.VISIBLE);
                                viewHolder.imageViewLeft
                                        .setImageResource(R.drawable.ic_playlist_is_playing);
                            } else {
                                ((SquareWidthRelativeLayout) viewHolder.imageViewLeft.getParent())
                                        .setVisibility(SquareWidthRelativeLayout.GONE);
                            }
                        } else {
                            ((SquareWidthRelativeLayout) viewHolder.imageViewLeft.getParent())
                                    .setVisibility(SquareWidthRelativeLayout.GONE);
                            view.setBackgroundResource(R.drawable.selectable_background_tomahawk);
                        }
                    }
                    if (mShowResolvedBy) {
                        ((SquareWidthRelativeLayout) viewHolder.imageViewRight.getParent())
                                .setVisibility(SquareWidthRelativeLayout.VISIBLE);
                        Drawable resolverIcon = null;
                        if (((Track) item).getResolver() != null) {
                            resolverIcon = ((Track) item).getResolver().getIcon();
                        }
                        if (resolverIcon != null) {
                            viewHolder.imageViewRight
                                    .setImageDrawable(((Track) item).getResolver().getIcon());
                        } else if (((Track) item).isLocal()) {
                            viewHolder.imageViewRight
                                    .setImageResource(R.drawable.ic_action_collection);
                        } else {
                            viewHolder.imageViewRight
                                    .setImageResource(R.drawable.ic_resolver_default);
                        }
                    }
                }
            }
        }
        return view;
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getCount()
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
        if (mShowContentHeader) {
            displayedContentHeaderCount = 1;
        }
        return displayedListArrayItemsCount + displayedContentHeaderCount;
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getItem(int)
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

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        if (mShowPlaylistHeader || mShowCategoryHeaders) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = mLayoutInflater
                        .inflate(mCategoryHeaderResourceHolder.resourceId, null);
                viewHolder = new ViewHolder(R.id.tomahawklistadapter_viewtype_header,
                        (ImageView) convertView
                                .findViewById(mCategoryHeaderResourceHolder.imageViewId),
                        (TextView) convertView
                                .findViewById(mCategoryHeaderResourceHolder.textViewId1));
                convertView.setTag(viewHolder);
            }
            viewHolder = (ViewHolder) convertView.getTag();
            if (mShowCategoryHeaders && getItem(position) != null) {
                if (getItem(position) instanceof Track) {
                    viewHolder.imageViewLeft.setImageResource(R.drawable.ic_action_track);
                    viewHolder.textFirstLine.setText(R.string.tracksfragment_title_string);
                } else if (getItem(position) instanceof Artist) {
                    viewHolder.imageViewLeft.setImageResource(R.drawable.ic_action_artist);
                    viewHolder.textFirstLine.setText(R.string.artistsfragment_title_string);
                } else if (getItem(position) instanceof Album) {
                    viewHolder.imageViewLeft.setImageResource(R.drawable.ic_action_album);
                    viewHolder.textFirstLine.setText(R.string.albumsfragment_title_string);
                }
            }
            return convertView;
        } else {
            return new View(mActivity);
        }
    }

    //remember that these have to be static, position=1 should always return the same Id that is.
    @Override
    public long getHeaderId(int position) {
        //return the first character of the country as ID because this is what headers are based upon
        long result = 0;
        int sizeSum = 0;
        if (mShowContentHeader) {
            sizeSum += 1;
        }
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
}
