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

import org.tomahawk.libtomahawk.TomahawkListArrayAdapter.TomahawkListItem;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 */
public class TomahawkListArrayAdapter extends ArrayAdapter<TomahawkListItem>
        implements Filterable {

    public static final int FILTER_BY_ALBUM = 0;
    public static final int FILTER_BY_ARTIST = 1;

    private List<TomahawkListItem> mAllItemsArray;
    private List<TomahawkListItem> mFilteredItemsArray;
    private TomahawkListFilter mTomahawkListFilter;
    private LayoutInflater inflator;
    private int mTextViewResourceIdFirstLine;
    private int mTextViewResourceIdSecondLine;
    private int mResource;
    private int mFilterMethod = FILTER_BY_ARTIST;

    public interface TomahawkListItem {

        /** @return the corresponding name/title */
        public String getName();

        /** @return the corresponding artist */
        public Artist getArtist();

        /** @return the corresponding album */
        public Album getAlbum();
    }

    /**
     * Constructs a new TArrayAdapter
     * 
     * @param context
     * @param resource
     * @param textViewResourceIdSecondLine
     * @param objects
     */
    public TomahawkListArrayAdapter(Activity activity, int resource, int textViewResourceIdFirstLine,
            int textViewResourceIdSecondLine, List<TomahawkListItem> objects, int filterMethod) {
        super(activity, resource, textViewResourceIdFirstLine, objects);

        this.mTextViewResourceIdFirstLine = textViewResourceIdFirstLine;
        this.mTextViewResourceIdSecondLine = textViewResourceIdSecondLine;
        this.mResource = resource;
        this.mFilterMethod = filterMethod;
        this.mAllItemsArray = new ArrayList<TomahawkListItem>();
        mAllItemsArray.addAll(objects);
        this.mFilteredItemsArray = new ArrayList<TomahawkListItem>();
        mFilteredItemsArray.addAll(mAllItemsArray);
        inflator = activity.getLayoutInflater();
    }

    public TomahawkListArrayAdapter(Activity activity, int resource, int textViewResourceIdFirstLine,
            List<TomahawkListItem> objects, int filterMethod) {
        super(activity, resource, textViewResourceIdFirstLine, objects);

        this.mTextViewResourceIdFirstLine = textViewResourceIdFirstLine;
        this.mResource = resource;
        this.mFilterMethod = filterMethod;
        this.mAllItemsArray = new ArrayList<TomahawkListItem>();
        mAllItemsArray.addAll(objects);
        this.mFilteredItemsArray = new ArrayList<TomahawkListItem>();
        mFilteredItemsArray.addAll(mAllItemsArray);
        inflator = activity.getLayoutInflater();
    }

    /* 
     * (non-Javadoc)
     * @see android.widget.Filterable#getFilter()
     */
    @Override
    public Filter getFilter() {
        if (mTomahawkListFilter == null) {
            mTomahawkListFilter = new TomahawkListFilter();
        }
        return mTomahawkListFilter;
    }

    static class ViewHolder {
        protected TextView textFirstLine;
        protected TextView textSecondLine;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;

        view = inflator.inflate(mResource, null);
        TomahawkListItem item = mFilteredItemsArray.get(position);
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            viewHolder.textFirstLine = (TextView) view.findViewById(mTextViewResourceIdFirstLine);
            viewHolder.textSecondLine = (TextView) view.findViewById(mTextViewResourceIdSecondLine);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = ((ViewHolder) view.getTag());
        }
        if (viewHolder.textFirstLine != null)
            viewHolder.textFirstLine.setText(item.getName());
        if (viewHolder.textSecondLine != null)
            viewHolder.textSecondLine.setText(item.getArtist().getName());

        return view;
    }

    private class TomahawkListFilter extends Filter {
        /* 
                * (non-Javadoc)
                * @see android.widget.Filter#performFiltering(java.lang.CharSequence)
                */
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults result = new FilterResults();
            if (constraint != null && constraint.toString().length() > 0) {
                ArrayList<TomahawkListItem> filteredItems = new ArrayList<TomahawkListItem>();

                for (int i = 0, l = mAllItemsArray.size(); i < l; i++) {
                    TomahawkListItem item = mAllItemsArray.get(i);
                    if (mFilterMethod == FILTER_BY_ARTIST && item.getArtist().getName().contentEquals(
                            constraint))
                        filteredItems.add(item);
                    if (mFilterMethod == FILTER_BY_ALBUM && item.getAlbum().getName().contentEquals(
                            constraint))
                        filteredItems.add(item);
                }
                result.count = filteredItems.size();
                result.values = filteredItems;
            } else {
                synchronized (this) {
                    result.values = mAllItemsArray;
                    result.count = mAllItemsArray.size();
                }
            }
            return result;
        }

        /* 
         * (non-Javadoc)
         * @see android.widget.Filter#publishResults(java.lang.CharSequence, android.widget.Filter.FilterResults)
         */
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mFilteredItemsArray = (ArrayList<TomahawkListItem>) results.values;
            notifyDataSetChanged();
            clear();
            for (int i = 0, l = mFilteredItemsArray.size(); i < l; i++)
                add(mFilteredItemsArray.get(i));
            notifyDataSetInvalidated();
        }

    }
}
