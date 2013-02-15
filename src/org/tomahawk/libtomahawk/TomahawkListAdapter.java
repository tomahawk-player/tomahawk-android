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

import java.util.ArrayList;
import java.util.List;

import org.tomahawk.tomahawk_android.R;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.emilsjolander.components.stickylistheaders.StickyListHeadersAdapter;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 *
 */
public class TomahawkListAdapter extends TomahawkBaseAdapter implements StickyListHeadersAdapter {

    private LayoutInflater mLayoutInflater;

    private ResourceHolder mSingleLineListItemResourceHolder;
    private ResourceHolder mDoubleLineListItemResourceHolder;
    private ResourceHolder mDoubleLineImageListItemResourceHolder;
    private ResourceHolder mCategoryHeaderResourceHolder;
    private ResourceHolder mContentHeaderResourceHolder;

    private boolean mShowCategoryHeaders = false;
    private List<TomahawkMenuItem> mCategoryHeaderArray;

    private boolean mShowContentHeader = false;
    private TomahawkListItem mCorrespondingTomahawkListItem;

    private boolean mShowHighlightingAndPlaystate = false;
    private boolean mShowResolvedBy = false;

    /**
     * Constructs a new {@link TomahawkListAdapter} to display list items with a single line of text
     *
     * @param activity the activity, which uses the {@link TomahawkListAdapter}. Used to get the {@link LayoutInflater}
     * @param resourceListItem the resource id for the view, that represents a listItem
     * @param textViewResourceListItemId1 the resource id for the textView inside resourceListItem that displays the
     * first line of text
     * @param list contains a list of TomahawkListItems.
     */
    public TomahawkListAdapter(Activity activity, int resourceListItem, int textViewResourceListItemId1,
            List<TomahawkListItem> list) {
        mActivity = activity;
        mLayoutInflater = mActivity.getLayoutInflater();
        setSingleLineListItemResources(resourceListItem, textViewResourceListItemId1);
        mListArray = new ArrayList<List<TomahawkListItem>>();
        mListArray.add(list);
        fillHeaderArray();
    }

    /**
     * Constructs a new {@link TomahawkListAdapter} to display list items with a single line of text and categoryHeaders
     * 
     * @param activity
     * @param resourceCategoryHeader
     * @param imageViewResourceCategoryHeaderId
     * @param textViewResourceCategoryHeaderId
     * @param resourceListItem
     * @param textViewResourceListItemId1
     * @param listArray
     * @param categoryHeaderArray
     */
    public TomahawkListAdapter(Activity activity, int resourceCategoryHeader, int imageViewResourceCategoryHeaderId,
            int textViewResourceCategoryHeaderId, int resourceListItem, int textViewResourceListItemId1,
            List<List<TomahawkListItem>> listArray, List<TomahawkMenuItem> categoryHeaderArray) {
        mActivity = activity;
        mLayoutInflater = mActivity.getLayoutInflater();
        setShowCategoryHeaders(categoryHeaderArray, resourceCategoryHeader, imageViewResourceCategoryHeaderId,
                textViewResourceCategoryHeaderId);
        setSingleLineListItemResources(resourceListItem, textViewResourceListItemId1);
        mListArray = listArray;
        fillHeaderArray();

    }

    /**
     * Constructs a new {@link TomahawkListAdapter} to display list items with two lines of text
     *
     * @param activity the activity, which uses the {@link TomahawkListAdapter}. Used to get the {@link LayoutInflater}
     * @param resourceListItem the resource id for the view, that represents a listItem
     * @param textViewResourceListItemId1 the resource id for the textView inside resourceListItem that displays the
     * first line of text
     * @param textViewResourceListItemId2 the resource id for the textView inside resourceListItem that displays the
     * second line of text
     * @param list contains a list of TomahawkListItems.
     */
    public TomahawkListAdapter(Activity activity, int resourceListItem, int textViewResourceListItemId1,
            int textViewResourceListItemId2, List<TomahawkListItem> list) {
        mActivity = activity;
        mLayoutInflater = mActivity.getLayoutInflater();
        setDoubleLineListItemResources(resourceListItem, textViewResourceListItemId1, textViewResourceListItemId2);
        mListArray = new ArrayList<List<TomahawkListItem>>();
        mListArray.add(list);
        fillHeaderArray();
    }

    /**
     * Constructs a new {@link TomahawkListAdapter} to display list items with two lines of text and an imageView
     *
     * @param activity the activity, which uses the {@link TomahawkListAdapter}. Used to get the {@link LayoutInflater}
     * @param resourceListItem the resource id for the view, that represents a listItem
     * @param imageViewResourcesListItemId the resource id for the imageView
     * @param textViewResourceListItemId1 the resource id for the textView inside resourceListItem that displays the
     * first line of text
     * @param textViewResourceListItemId2 the resource id for the textView inside resourceListItem that displays the
     * second line of text
     * @param list contains a list of TomahawkListItems.
     */
    public TomahawkListAdapter(Activity activity, int resourceListItem, int imageViewResourcesListItemId,
            int textViewResourceListItemId1, int textViewResourceListItemId2, List<TomahawkListItem> list) {
        mActivity = activity;
        mLayoutInflater = mActivity.getLayoutInflater();
        setDoubleLineImageListItemResources(resourceListItem, imageViewResourcesListItemId,
                textViewResourceListItemId1, textViewResourceListItemId2);
        mListArray = new ArrayList<List<TomahawkListItem>>();
        mListArray.add(list);
        fillHeaderArray();
    }

    /**  fill mCategoryHeaderArray with "No header" strings  */
    public void fillHeaderArray() {
        if (mCategoryHeaderArray == null) {
            mShowCategoryHeaders = false;
            mCategoryHeaderArray = new ArrayList<TomahawkMenuItem>();
            for (int i = 0; i < mListArray.size(); i++) {
                mCategoryHeaderArray.add(new TomahawkMenuItem("No header", null));
            }
        }
        while (mCategoryHeaderArray.size() < mListArray.size()) {
            mCategoryHeaderArray.add(new TomahawkMenuItem("No header", null));
        }
    }

    public void setSingleLineListItemResources(int resourceListItem, int textViewResourceListItemId1) {
        mSingleLineListItemResourceHolder = new ResourceHolder(resourceListItem, -1, textViewResourceListItemId1, -1);
    }

    public void setDoubleLineListItemResources(int resourceListItem, int textViewResourceListItemId1, int textViewResourceListItemId2) {
        mDoubleLineListItemResourceHolder = new ResourceHolder(resourceListItem, -1, textViewResourceListItemId1,
                textViewResourceListItemId2);
    }

    public void setDoubleLineImageListItemResources(int resourceListItem, int imageViewResourcesListItemId, int textViewResourceListItemId1, int textViewResourceListItemId2) {
        mDoubleLineImageListItemResourceHolder = new ResourceHolder(resourceListItem, imageViewResourcesListItemId,
                textViewResourceListItemId1, textViewResourceListItemId2);
    }

    public void setShowCategoryHeaders(List<TomahawkMenuItem> categoryHeaderArray, int resourceCategoryHeader, int imageViewResourceCategoryHeaderId, int textViewResourceCategoryHeaderId) {
        mCategoryHeaderArray = categoryHeaderArray;
        mCategoryHeaderResourceHolder = new ResourceHolder(resourceCategoryHeader, imageViewResourceCategoryHeaderId,
                textViewResourceCategoryHeaderId, -1);
        mShowCategoryHeaders = true;
    }

    public void setShowContentHeader(TomahawkListItem correspondingTomahawkListItem, int resourceContentHeader, int imageViewResourceContentHeaderId, int textViewResourceContentHeaderId1, int textViewResourceContentHeaderId2) {
        mCorrespondingTomahawkListItem = correspondingTomahawkListItem;
        mContentHeaderResourceHolder = new ResourceHolder(resourceContentHeader, imageViewResourceContentHeaderId,
                textViewResourceContentHeaderId1, textViewResourceContentHeaderId2);
        mShowContentHeader = true;
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
            if ((item == mCorrespondingTomahawkListItem && position == 0 && convertView == null)
                    || (item == mCorrespondingTomahawkListItem && position == 0 && ((ViewHolder) convertView.getTag()).viewType != R.id.tomahawklistadapter_viewtype_contentheader)) {
                view = mLayoutInflater.inflate(mContentHeaderResourceHolder.resourceId, null);
                viewHolder = new ViewHolder(R.id.tomahawklistadapter_viewtype_contentheader,
                        (ImageView) view.findViewById(mContentHeaderResourceHolder.imageViewId),
                        (TextView) view.findViewById(mContentHeaderResourceHolder.textViewId1),
                        (TextView) view.findViewById(mContentHeaderResourceHolder.textViewId2));
                view.setTag(viewHolder);
            } else if ((item instanceof TomahawkMenuItem && convertView == null)
                    || (item instanceof TomahawkMenuItem && ((ViewHolder) convertView.getTag()).viewType != R.id.tomahawklistadapter_viewtype_categoryheader)) {
                view = mLayoutInflater.inflate(mCategoryHeaderResourceHolder.resourceId, null);
                viewHolder = new ViewHolder(R.id.tomahawklistadapter_viewtype_categoryheader,
                        (ImageView) view.findViewById(mCategoryHeaderResourceHolder.imageViewId),
                        (TextView) view.findViewById(mCategoryHeaderResourceHolder.textViewId1));
                view.setTag(viewHolder);
            } else if ((item instanceof Artist && item != mCorrespondingTomahawkListItem && convertView == null)
                    || (item instanceof Artist && item != mCorrespondingTomahawkListItem && ((ViewHolder) convertView.getTag()).viewType != R.id.tomahawklistadapter_viewtype_singlelinelistitem)) {
                view = mLayoutInflater.inflate(mSingleLineListItemResourceHolder.resourceId, null);
                viewHolder = new ViewHolder(R.id.tomahawklistadapter_viewtype_singlelinelistitem,
                        (TextView) view.findViewById(mSingleLineListItemResourceHolder.textViewId1));
                view.setTag(viewHolder);
            } else if (!mShowHighlightingAndPlaystate
                    && !mShowResolvedBy
                    && ((item instanceof Track && item != mCorrespondingTomahawkListItem && convertView == null) || (item instanceof Track
                            && item != mCorrespondingTomahawkListItem && ((ViewHolder) convertView.getTag()).viewType != R.id.tomahawklistadapter_viewtype_doublelinelistitem))) {
                view = mLayoutInflater.inflate(mDoubleLineListItemResourceHolder.resourceId, null);
                viewHolder = new ViewHolder(R.id.tomahawklistadapter_viewtype_doublelinelistitem,
                        (TextView) view.findViewById(mDoubleLineListItemResourceHolder.textViewId1),
                        (TextView) view.findViewById(mDoubleLineListItemResourceHolder.textViewId2));
                view.setTag(viewHolder);
            } else if ((mShowHighlightingAndPlaystate || mShowResolvedBy)
                    && ((item instanceof Track && item != mCorrespondingTomahawkListItem && convertView == null) || (item instanceof Track
                            && item != mCorrespondingTomahawkListItem && ((ViewHolder) convertView.getTag()).viewType != R.id.tomahawklistadapter_viewtype_doublelineplaystateimagelistitem))) {
                view = mLayoutInflater.inflate(mDoubleLineImageListItemResourceHolder.resourceId, null);
                viewHolder = new ViewHolder(R.id.tomahawklistadapter_viewtype_doublelineplaystateimagelistitem,
                        (ImageView) view.findViewById(mDoubleLineImageListItemResourceHolder.imageViewId),
                        (TextView) view.findViewById(mDoubleLineImageListItemResourceHolder.textViewId1),
                        (TextView) view.findViewById(mDoubleLineImageListItemResourceHolder.textViewId2));
                view.setTag(viewHolder);
            } else if ((item instanceof Album && item != mCorrespondingTomahawkListItem && convertView == null)
                    || (item instanceof Album && item != mCorrespondingTomahawkListItem && ((ViewHolder) convertView.getTag()).viewType != R.id.tomahawklistadapter_viewtype_doublelineimagelistitem)) {
                view = mLayoutInflater.inflate(mDoubleLineImageListItemResourceHolder.resourceId, null);
                viewHolder = new ViewHolder(R.id.tomahawklistadapter_viewtype_doublelineimagelistitem,
                        (ImageView) view.findViewById(mDoubleLineImageListItemResourceHolder.imageViewId),
                        (TextView) view.findViewById(mDoubleLineImageListItemResourceHolder.textViewId1),
                        (TextView) view.findViewById(mDoubleLineImageListItemResourceHolder.textViewId2));
                view.setTag(viewHolder);
            } else {
                view = convertView;
                viewHolder = (ViewHolder) view.getTag();
            }

            if (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_contentheader) {
                viewHolder.textFirstLine.setText(((TomahawkListItem) item).getName());
                viewHolder.textSecondLine.setText(((TomahawkListItem) item).getArtist().getName());
                loadBitmap((TomahawkListItem) item, viewHolder.imageView);
            } else if (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_categoryheader) {
                viewHolder.imageView.setImageResource(((TomahawkMenuItem) item).mMenuItemIconResource);
                viewHolder.textFirstLine.setText(((TomahawkMenuItem) item).mMenuItemString);
            } else if (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_singlelinelistitem) {
                viewHolder.textFirstLine.setText(((Artist) item).getName());
            } else if (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_doublelinelistitem) {
                viewHolder.textFirstLine.setText(((Track) item).getName());
                viewHolder.textSecondLine.setText(((Track) item).getArtist().getName());
            } else if (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_doublelineimagelistitem) {
                if (item instanceof Album) {
                    viewHolder.textFirstLine.setText(((Album) item).getName());
                    if (((Album) item).getArtist() != null)
                        viewHolder.textSecondLine.setText(((Album) item).getArtist().getName());
                    loadBitmap((Album) item, viewHolder.imageView);
                }
            } else if (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_doublelineplaystateimagelistitem) {
                if (item instanceof Track) {
                    viewHolder.textFirstLine.setText(((Track) item).getName());
                    viewHolder.textSecondLine.setText(((Track) item).getArtist().getName());
                    if (mShowHighlightingAndPlaystate) {
                        if (position == mHighlightedItemPosition) {
                            view.setBackgroundResource(R.color.pressed_tomahawk);
                            if (mHighlightedItemIsPlaying) {
                                viewHolder.imageView.setVisibility(ImageView.VISIBLE);
                                viewHolder.imageView.setImageResource(R.drawable.ic_playlist_is_playing);
                            } else
                                viewHolder.imageView.setVisibility(ImageView.GONE);
                        } else {
                            view.setBackgroundResource(R.drawable.selectable_background_tomahawk);
                            viewHolder.imageView.setVisibility(ImageView.GONE);
                        }
                    } else if (mShowResolvedBy) {
                        Drawable resolverIcon = null;
                        if (((Track) item).getResolver() != null)
                            resolverIcon = ((Track) item).getResolver().getIcon();
                        if (resolverIcon != null)
                            viewHolder.imageView.setImageDrawable(((Track) item).getResolver().getIcon());
                        else if (((Track) item).isLocal())
                            viewHolder.imageView.setImageResource(R.drawable.ic_action_collection);
                        else
                            viewHolder.imageView.setImageResource(R.drawable.ic_resolver_default);
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
        if ((mFiltered ? mFilteredListArray : mListArray) == null)
            return 0;
        int displayedListArrayItemsCount = 0;
        int displayedHeaderArrayItemsCount = 0;
        int displayedContentHeaderCount = 0;
        for (List<TomahawkListItem> list : (mFiltered ? mFilteredListArray : mListArray)) {
            displayedListArrayItemsCount += list.size();
            if (list.size() > 0 && mShowCategoryHeaders)
                displayedHeaderArrayItemsCount++;
        }
        if (mShowContentHeader)
            displayedContentHeaderCount = 1;
        return displayedListArrayItemsCount + displayedHeaderArrayItemsCount + displayedContentHeaderCount;
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getItem(int)
     */
    @Override
    public Object getItem(int position) {
        Object item = null;
        int offsetCounter = 0;
        if (mShowContentHeader)
            offsetCounter++;
        if (mShowContentHeader && position == 0)
            item = mCorrespondingTomahawkListItem;
        else {
            for (int i = 0; i < (mFiltered ? mFilteredListArray : mListArray).size(); i++) {
                List<TomahawkListItem> list = (mFiltered ? mFilteredListArray : mListArray).get(i);
                if (mShowCategoryHeaders) {
                    if (!list.isEmpty())
                        offsetCounter++;
                    if (position - offsetCounter == -1) {
                        item = mCategoryHeaderArray.get(i);
                        break;
                    }
                }
                if (position - offsetCounter < list.size()) {
                    item = list.get(position - offsetCounter);
                    break;
                }
                offsetCounter += list.size();
            }
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
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.show_playlist_header, parent, false);
        }
        return convertView;
    }

    //remember that these have to be static, postion=1 should walys return the same Id that is.
    @Override
    public long getHeaderId(int position) {
        //return the first character of the country as ID because this is what headers are based upon
        return 0;
    }

}
