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
import android.widget.Filterable;
import android.widget.TextView;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 */
public class TomahawkListArrayAdapter extends ArrayAdapter<TomahawkListItem>
        implements Filterable {

    private List<TomahawkListItem> mItemsArray;
    private LayoutInflater mInflator;
    private int mTextViewResourceIdFirstLine;
    private int mTextViewResourceIdSecondLine;
    private int mResource;

    /**
     * This interface represents an item displayed in our Collections list.
     */
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
            int textViewResourceIdSecondLine, List<TomahawkListItem> objects) {
        super(activity, resource, textViewResourceIdFirstLine, objects);

        mTextViewResourceIdFirstLine = textViewResourceIdFirstLine;
        mTextViewResourceIdSecondLine = textViewResourceIdSecondLine;
        mResource = resource;
        mItemsArray = new ArrayList<TomahawkListItem>(objects);
        mInflator = activity.getLayoutInflater();
    }

    public TomahawkListArrayAdapter(Activity activity, int resource, int textViewResourceIdFirstLine,
            List<TomahawkListItem> objects) {
        super(activity, resource, textViewResourceIdFirstLine, objects);

        mTextViewResourceIdFirstLine = textViewResourceIdFirstLine;
        mResource = resource;
        mItemsArray = new ArrayList<TomahawkListItem>(objects);
        mInflator = activity.getLayoutInflater();
    }

    static class ViewHolder {
        protected TextView textFirstLine;
        protected TextView textSecondLine;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;

        view = mInflator.inflate(mResource, null);
        TomahawkListItem item = mItemsArray.get(position);
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
}
