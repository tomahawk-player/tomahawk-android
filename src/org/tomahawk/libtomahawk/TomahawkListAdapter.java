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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.tomahawk.tomahawk_android.R;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 *
 */
public class TomahawkListAdapter extends BaseAdapter {

    private Activity mActivity;
    private LayoutInflater mInflater;

    private boolean mShowAsGrid = false;
    private boolean mShowHeaders;
    private boolean mFiltered;

    private List<List<TomahawkListItem>> mListArray;
    private List<List<TomahawkListItem>> mFilteredListArray;
    private List<String> mHeaderArray;
    private int mResourceListItem;
    private int mTextViewResourceListItemId1;
    private int mTextViewResourceListItemId2;
    private int mResourceGridItem;
    private int mImageViewResourceGridItem;
    private int mTextViewResourceGridItemId1;
    private int mTextViewResourceGridItemId2;
    private int mResourceListHeader;
    private int mTextViewResourceListHeaderId;
    private Bitmap mPlaceHolderBitmap;

    /**
     * This interface represents an item displayed in our {@link Collection} list.
     */
    public interface TomahawkListItem {

        /** @return the corresponding name/title */
        public String getName();

        /** @return the corresponding {@link Artist} */
        public Artist getArtist();

        /** @return the corresponding {@link Album} */
        public Album getAlbum();
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return (BitmapWorkerTask) bitmapWorkerTaskReference.get();
        }
    }

    private class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private String data;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            data = params[0];
            return BitmapFactory.decodeFile(data);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = (ImageView) imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    /**
     * Constructs a new {@link TomahawkListAdapter}
     * 
     * @param activity the activity, which uses the {@link TomahawkListAdapter}. Used to get the {@link LayoutInflater}
     * @param resourceListItem the resource id for the view, that represents a listItem
     * @param imageViewResourceGridItem the resource id for the view, that displays the album art image
     * @param textViewResourceListItemId1 the resource id for the textView inside resourceListItem that displays the
     * first line of text
     * @param textViewResourceListItemId2 the resource id for the textView inside resourceListItem that displays the
     * second line of text
     * @param list contains a list of TomahawkListItems.
     */
    public TomahawkListAdapter(Activity activity, int resourceGridItem, int imageViewResourceGridItem,
            int textViewResourceGridItemId1, int textViewResourceGridItemId2, List<TomahawkListItem> list) {
        mActivity = activity;
        mInflater = activity.getLayoutInflater();
        mResourceGridItem = resourceGridItem;
        mImageViewResourceGridItem = imageViewResourceGridItem;
        mTextViewResourceGridItemId1 = textViewResourceGridItemId1;
        mTextViewResourceGridItemId2 = textViewResourceGridItemId2;
        mListArray = new ArrayList<List<TomahawkListItem>>();
        mListArray.add(list);
        mShowAsGrid = true;
        mShowHeaders = false;
        setFiltered(false);
        fillHeaderArray();
    }

    /**
     * Constructs a new {@link TomahawkListAdapter}
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
        mShowHeaders = false;
        setFiltered(false);
        fillHeaderArray();
    }

    /**
     * Constructs a new {@link TomahawkListAdapter}
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
        mShowHeaders = false;
        setFiltered(false);
        fillHeaderArray();
    }

    /**
     * Constructs a new {@link TomahawkListAdapter}
     * 
     * @param activity the activity, which uses the {@link TomahawkListAdapter}. Used to get the {@link LayoutInflater}
     * @param resourceListHeader the resource id for the view, that represents a listHeader
     * @param textViewResourceListHeaderId the resource id for the textView inside resourceListHeader that displays the
     * text
     * @param resourceListItem the resource id for the view, that represents a listItem
     * @param textViewResourceListItemId1 the resource id for the textView inside resourceListItem that displays the
     * first line of text
     * @param textViewResourceListItemId2 the resource id for the textView inside resourceListItem that displays the
     * second line of text
     * @param listArray contains a list of lists of TomahawkListItems. Every list within the list can be shown in
     * different categories with headers specified in headerArray
     * @param headerArray contains the headers of the lists. if no headerArray is given, the headers won't be shown
     */
    public TomahawkListAdapter(Activity activity, int resourceListHeader, int textViewResourceListHeaderId,
            int resourceListItem, int textViewResourceListItemId1, int textViewResourceListItemId2,
            List<List<TomahawkListItem>> listArray, List<String> headerArray) {
        mActivity = activity;
        mInflater = activity.getLayoutInflater();
        mResourceListHeader = resourceListHeader;
        mTextViewResourceListHeaderId = textViewResourceListHeaderId;
        mResourceListItem = resourceListItem;
        mTextViewResourceListItemId1 = textViewResourceListItemId1;
        mTextViewResourceListItemId2 = textViewResourceListItemId2;
        mListArray = listArray;
        mHeaderArray = headerArray;
        mShowHeaders = true;
        setFiltered(true);
        fillHeaderArray();

    }

    /**
     * Constructs a new {@link TomahawkListAdapter}
     * 
     * @param activity the activity, which uses the {@link TomahawkListAdapter}. Used to get the {@link LayoutInflater}
     * @param resourceListHeader the resource id for the view, that represents a listHeader
     * @param textViewResourceListHeaderId the resource id for the textView inside resourceListHeader that displays the
     * text
     * @param resourceListItem the resource id for the view, that represents a listItem
     * @param textViewResourceListItemId1 the resource id for the textView inside resourceListItem that displays the
     * first line of text
     * @param listArray contains a list of lists of TomahawkListItems. Every list within the list can be shown in
     * different categories with headers specified in headerArray
     * @param headerArray contains the headers of the lists. if no headerArray is given, the headers won't be shown
     */
    public TomahawkListAdapter(Activity activity, int resourceListHeader, int textViewResourceListHeaderId,
            int resourceListItem, int textViewResourceListItemId1, List<List<TomahawkListItem>> listArray,
            List<String> headerArray) {
        mActivity = activity;
        mInflater = activity.getLayoutInflater();
        mResourceListHeader = resourceListHeader;
        mTextViewResourceListHeaderId = textViewResourceListHeaderId;
        mResourceListItem = resourceListItem;
        mTextViewResourceListItemId1 = textViewResourceListItemId1;
        mListArray = listArray;
        mHeaderArray = headerArray;
        mShowHeaders = true;
        setFiltered(true);
        fillHeaderArray();
    }

    /**  fill mHeaderArray with "No header" strings  */
    public void fillHeaderArray() {
        if (mHeaderArray == null) {
            mShowHeaders = false;
            mHeaderArray = new ArrayList<String>();
            for (int i = 0; i < mListArray.size(); i++) {
                mHeaderArray.add("No header");
            }
        }
        while (mHeaderArray.size() < mListArray.size()) {
            mHeaderArray.add("No header");
        }
    }

    /** Add a list to the {@link TomahawkListAdapter}.
     *  @param title the title of the list, which will be displayed as a header, if the list is not empty
     * @return the index of the just added list*/
    public int addList(String title) {
        mListArray.add(new ArrayList<TomahawkListItem>());
        mHeaderArray.add(title);
        return mListArray.size() - 1;
    }

    /** Add an item to the list with the given index
     *  @param index the index which specifies which list the item should be added to
     *  @param item the item to add
     *  @return true if successful, otherwise false*/
    public boolean addItemToList(int index, TomahawkListItem item) {
        if (hasListWithIndex(index)) {
            mListArray.get(index).add(item);
            return true;
        }
        return false;
    }

    /** Add an item to the list with the given title
     *  @param title the title, which specifies which list the item should be added to
     *  @param item the item to add
     *  @return true if successful, otherwise false*/
    public boolean addItemToList(String title, TomahawkListItem item) {
        int i = hasListWithTitle(title);
        if (i >= 0) {
            mListArray.get(i).add(item);
            return true;
        }
        return false;
    }

    /** test if the list with the given index exists
     *  @param index
     *  @return true if list exists, false otherwise*/
    public boolean hasListWithIndex(int index) {
        if (mListArray.get(index) != null) {
            return true;
        }
        return false;
    }

    /** test if the list with the given title exists
     *  @param title
     *  @return if the list was found, the method returns the index of the list, otherwise -1*/
    public int hasListWithTitle(String title) {
        for (int i = 0; i < mHeaderArray.size(); i++)
            if (mHeaderArray.get(i) == title) {
                return i;
            }
        return -1;
    }

    /** Removes every element from every list there is  */
    public void clearAllLists() {
        for (int i = 0; i < mHeaderArray.size(); i++)
            mListArray.get(i).clear();
    }

    /** @return the {@link Filter}, which allows to filter the items inside the custom {@link ListView} fed by {@link TomahawkListAdapter}*/
    public Filter getFilter() {
        return new Filter() {
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mFilteredListArray = (List<List<TomahawkListItem>>) results.values;
                TomahawkListAdapter.this.notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                constraint = constraint.toString().toLowerCase();
                constraint = constraint.toString().trim();
                List<List<TomahawkListItem>> filteredResults = (List<List<TomahawkListItem>>) getFilteredResults(constraint);

                FilterResults results = new FilterResults();
                synchronized (this) {
                    results.values = filteredResults;
                }

                return results;
            }

            protected List<List<TomahawkListItem>> getFilteredResults(CharSequence constraint) {
                List<List<TomahawkListItem>> filteredResults = new ArrayList<List<TomahawkListItem>>();
                if (constraint == null || constraint.toString().length() <= 1)
                    return filteredResults;

                for (int i = 0; i < mListArray.size(); i++) {
                    filteredResults.add(new ArrayList<TomahawkListItem>());
                    for (int j = 0; j < mListArray.get(i).size(); j++) {
                        TomahawkListItem item = mListArray.get(i).get(j);
                        if (item.getName().toLowerCase().contains(constraint))
                            filteredResults.get(i).add(item);
                    }
                }
                return filteredResults;
            }
        };
    }

    /**
     * @param filtered true if the list is being filtered, else false
     */
    public void setFiltered(boolean filtered) {
        this.mFiltered = filtered;
    }

    /**
     * @return true if data is shown as a gridView, else false
     */
    public boolean isShowAsGrid() {
        return mShowAsGrid;
    }

    /**
     * @param set wether or not the data should be shown in a gridView
     */
    public void setShowAsGrid(boolean mShowAsGrid) {
        this.mShowAsGrid = mShowAsGrid;
        this.mShowHeaders = false;
    }

    /** 
     * Load a {@link Bitmap} asynchronously
     * @param pathToBitmap the file path to the {@link Bitmap} to load
     * @param imageView the {@link ImageView}, which will be used to show the {@link Bitmap}*/
    public void loadBitmap(String pathToBitmap, ImageView imageView) {
        if (cancelPotentialWork(pathToBitmap, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            if (mPlaceHolderBitmap == null)
                mPlaceHolderBitmap = BitmapFactory.decodeResource(mActivity.getResources(),
                        R.drawable.no_album_art_placeholder);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(mActivity.getResources(), mPlaceHolderBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(pathToBitmap);
        }
    }

    /** 
     * Checks if another running task is already associated with the {@link ImageView}
     * @param data
     * @param imageView
     * @return */
    public static boolean cancelPotentialWork(String data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.data;
            if (bitmapData != data) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    /** 
     * Used to get the {@link BitmapWorkerTask}, which is used to asynchronously load a {@link Bitmap} into to {@link ImageView}
     * @param imageView
     * @return */
    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    /**
     * This {@link ViewHolder} holds the data to an entry in the grid/listView
     */
    static class ViewHolder {
        protected int viewType;
        protected ImageView albumArt;
        protected TextView textFirstLine;
        protected TextView textSecondLine;
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

            if ((item instanceof TomahawkListItem && !mShowAsGrid && convertView == null)
                    || (item instanceof TomahawkListItem && convertView != null && !mShowAsGrid && ((ViewHolder) convertView.getTag()).viewType != R.id.tomahawklistadapter_viewtype_listitem)) {
                view = mInflater.inflate(mResourceListItem, null);
                viewHolder.viewType = R.id.tomahawklistadapter_viewtype_listitem;
                viewHolder.textFirstLine = (TextView) view.findViewById(mTextViewResourceListItemId1);
                viewHolder.textSecondLine = (TextView) view.findViewById(mTextViewResourceListItemId2);
                view.setTag(viewHolder);
            } else if ((item instanceof TomahawkListItem && mShowAsGrid && convertView == null)
                    || (item instanceof TomahawkListItem && convertView != null && mShowAsGrid && ((ViewHolder) convertView.getTag()).viewType != R.id.tomahawklistadapter_viewtype_griditem)) {
                view = mInflater.inflate(mResourceGridItem, null);
                viewHolder.viewType = R.id.tomahawklistadapter_viewtype_griditem;
                viewHolder.albumArt = (ImageView) view.findViewById(mImageViewResourceGridItem);
                viewHolder.textFirstLine = (TextView) view.findViewById(mTextViewResourceGridItemId1);
                viewHolder.textSecondLine = (TextView) view.findViewById(mTextViewResourceGridItemId2);
                view.setTag(viewHolder);
            } else if ((item instanceof String && convertView == null)
                    || (item instanceof String && convertView != null && ((ViewHolder) convertView.getTag()).viewType != R.id.tomahawklistadapter_viewtype_header)) {
                view = mInflater.inflate(mResourceListHeader, null);
                viewHolder.viewType = R.id.tomahawklistadapter_viewtype_header;
                viewHolder.textFirstLine = (TextView) view.findViewById(mTextViewResourceListHeaderId);
                view.setTag(viewHolder);
            } else {
                view = convertView;
                viewHolder = (ViewHolder) view.getTag();
            }
            if (viewHolder.albumArt != null) {
                String albumArtPath = ((TomahawkListItem) item).getAlbum().getAlbumArtPath();
                if (albumArtPath != null)
                    //                    viewHolder.albumArt.setImageBitmap(albumArtBitmap);
                    loadBitmap(albumArtPath, viewHolder.albumArt);
                else
                    viewHolder.albumArt.setImageResource(R.drawable.no_album_art_placeholder);
            }
            if (viewHolder.textFirstLine != null) {
                if (item instanceof String)
                    viewHolder.textFirstLine.setText((String) item);
                else if (item instanceof TomahawkListItem)
                    viewHolder.textFirstLine.setText(((TomahawkListItem) item).getName());
            }
            if (viewHolder.textSecondLine != null)
                viewHolder.textSecondLine.setText(((TomahawkListItem) item).getArtist().getName());

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
        for (List<TomahawkListItem> list : (mFiltered ? mFilteredListArray : mListArray)) {
            displayedListArrayItemsCount += list.size();
            if (list.size() > 0 && mShowHeaders)
                displayedHeaderArrayItemsCount++;
        }
        return displayedListArrayItemsCount + displayedHeaderArrayItemsCount;
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
            if (mShowHeaders) {
                if (!list.isEmpty())
                    offsetCounter++;
                if (position - offsetCounter == -1) {
                    item = mHeaderArray.get(i);
                    break;
                }
            }
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
}