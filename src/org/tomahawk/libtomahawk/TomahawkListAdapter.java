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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 *
 */
public class TomahawkListAdapter extends TomahawkBaseAdapter {

    private LayoutInflater mInflater;

    private int mResourceListItem;
    private int mImageViewResourceListItemId;
    private int mTextViewResourceListItemId1;
    private int mTextViewResourceListItemId2;

    private boolean mShowCategoryHeaders = false;
    private List<TomahawkMenuItem> mCategoryHeaderArray;
    private int mResourceCategoryHeader;
    private int mTextViewResourceCategoryHeaderId;
    private int mImageViewResourceCategoryHeaderId;

    private boolean mShowContentHeader = false;
    private TomahawkListItem mCorrespondingTomahawkListItem;
    private int mResourceContentHeader;
    private int mImageViewResourceContentHeaderId;
    private int mTextViewResourceContentHeaderId1;
    private int mTextViewResourceContentHeaderId2;

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
        mInflater = activity.getLayoutInflater();
        mResourceListItem = resourceListItem;
        mTextViewResourceListItemId1 = textViewResourceListItemId1;
        mListArray = new ArrayList<List<TomahawkListItem>>();
        mListArray.add(list);
        fillHeaderArray();
    }

    public TomahawkListAdapter(Activity activity, int resourceCategoryHeader, int imageViewResourceCategoryHeaderId,
            int textViewResourceCategoryHeaderId, int resourceListItem, int textViewResourceListItemId1,
            List<List<TomahawkListItem>> listArray, List<TomahawkMenuItem> categoryHeaderArray) {
        mActivity = activity;
        mInflater = activity.getLayoutInflater();
        mResourceListItem = resourceListItem;
        mTextViewResourceListItemId1 = textViewResourceListItemId1;
        mListArray = listArray;
        setShowCategoryHeaders(categoryHeaderArray, resourceCategoryHeader, imageViewResourceCategoryHeaderId,
                textViewResourceCategoryHeaderId);
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
        mInflater = activity.getLayoutInflater();
        mResourceListItem = resourceListItem;
        mTextViewResourceListItemId1 = textViewResourceListItemId1;
        mTextViewResourceListItemId2 = textViewResourceListItemId2;
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
        mInflater = activity.getLayoutInflater();
        mResourceListItem = resourceListItem;
        mImageViewResourceListItemId = imageViewResourcesListItemId;
        mTextViewResourceListItemId1 = textViewResourceListItemId1;
        mTextViewResourceListItemId2 = textViewResourceListItemId2;
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

    public void setShowCategoryHeaders(List<TomahawkMenuItem> categoryHeaderArray, int resourceCategoryHeader, int imageViewResourceCategoryHeaderId, int textViewResourceCategoryHeaderId) {
        mCategoryHeaderArray = categoryHeaderArray;
        mResourceCategoryHeader = resourceCategoryHeader;
        mImageViewResourceCategoryHeaderId = imageViewResourceCategoryHeaderId;
        mTextViewResourceCategoryHeaderId = textViewResourceCategoryHeaderId;
        mShowCategoryHeaders = true;
    }

    public void setShowContentHeader(TomahawkListItem correspondingTomahawkListItem, int resourceContentHeader, int imageViewResourceContentHeaderId, int textViewResourceContentHeaderId1, int textViewResourceContentHeaderId2) {
        mCorrespondingTomahawkListItem = correspondingTomahawkListItem;
        mResourceContentHeader = resourceContentHeader;
        mImageViewResourceContentHeaderId = imageViewResourceContentHeaderId;
        mTextViewResourceContentHeaderId1 = textViewResourceContentHeaderId1;
        mTextViewResourceContentHeaderId2 = textViewResourceContentHeaderId2;
        mShowContentHeader = true;
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
            ViewHolder viewHolder = new ViewHolder();

            if ((item == mCorrespondingTomahawkListItem && position == 0 && convertView == null)
                    || (item == mCorrespondingTomahawkListItem && position == 0 && ((ViewHolder) convertView.getTag()).viewType != R.id.tomahawklistadapter_viewtype_contentheader)) {
                view = mInflater.inflate(mResourceContentHeader, null);
                viewHolder.viewType = R.id.tomahawklistadapter_viewtype_contentheader;
                viewHolder.imageView = (ImageView) view.findViewById(mImageViewResourceContentHeaderId);
                viewHolder.textFirstLine = (TextView) view.findViewById(mTextViewResourceContentHeaderId1);
                viewHolder.textSecondLine = (TextView) view.findViewById(mTextViewResourceContentHeaderId2);
                view.setTag(viewHolder);
            } else if ((item instanceof TomahawkListItem && item != mCorrespondingTomahawkListItem && convertView == null)
                    || (item instanceof TomahawkListItem && item != mCorrespondingTomahawkListItem && ((ViewHolder) convertView.getTag()).viewType != R.id.tomahawklistadapter_viewtype_listitem)) {
                view = mInflater.inflate(mResourceListItem, null);
                viewHolder.viewType = R.id.tomahawklistadapter_viewtype_listitem;
                viewHolder.imageView = (ImageView) view.findViewById(mImageViewResourceListItemId);
                viewHolder.textFirstLine = (TextView) view.findViewById(mTextViewResourceListItemId1);
                viewHolder.textSecondLine = (TextView) view.findViewById(mTextViewResourceListItemId2);
                view.setTag(viewHolder);
            } else if ((item instanceof TomahawkMenuItem && convertView == null)
                    || (item instanceof TomahawkMenuItem && ((ViewHolder) convertView.getTag()).viewType != R.id.tomahawklistadapter_viewtype_categoryheader)) {
                view = mInflater.inflate(mResourceCategoryHeader, null);
                viewHolder.viewType = R.id.tomahawklistadapter_viewtype_categoryheader;
                viewHolder.imageView = (ImageView) view.findViewById(mImageViewResourceCategoryHeaderId);
                viewHolder.textFirstLine = (TextView) view.findViewById(mTextViewResourceCategoryHeaderId);
                view.setTag(viewHolder);
            } else {
                view = convertView;
                viewHolder = (ViewHolder) view.getTag();
            }
            if (viewHolder.textFirstLine != null) {
                if (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_categoryheader)
                    viewHolder.textFirstLine.setText(((TomahawkMenuItem) item).mMenuItemString);
                else
                    viewHolder.textFirstLine.setText(((TomahawkListItem) item).getName());
            }
            if (viewHolder.textSecondLine != null) {
                if (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_listitem
                        || (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_contentheader && item instanceof Album))
                    viewHolder.textSecondLine.setText(((TomahawkListItem) item).getArtist().getName());
            }
            if (viewHolder.imageView != null) {
                String albumArtPath = null;
                if (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_listitem
                        || (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_contentheader && item instanceof Album))
                    albumArtPath = ((TomahawkListItem) item).getAlbum().getAlbumArtPath();
                if (albumArtPath != null)
                    loadBitmap(albumArtPath, viewHolder.imageView);
                else {
                    if (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_categoryheader)
                        viewHolder.imageView.setImageResource(((TomahawkMenuItem) item).mMenuItemIconResource);
                    else if (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_contentheader
                            && item instanceof Artist)
                        viewHolder.imageView.setImageResource(R.drawable.no_artist_placeholder);
                    else
                        viewHolder.imageView.setImageResource(R.drawable.no_album_art_placeholder);
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
}
