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

import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.utils.AdapterUtils;

import android.app.Activity;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * This class populates the listview inside the navigation drawer
 */
public class TomahawkMenuAdapter extends BaseAdapter implements StickyListHeadersAdapter {

    private final Activity mActivity;

    private LayoutInflater mLayoutInflater;

    private List<String> mStringArray = new ArrayList<String>();

    private List<Integer> mIconArray = new ArrayList<Integer>();

    /**
     * Constructs a new {@link TomahawkMenuAdapter}
     *
     * @param activity    reference to whatever {@link Activity}
     * @param stringArray Array of {@link String}s containing every menu entry text
     * @param iconArray   {@link TypedArray} containing an array of resource ids to be used to show
     *                    an icon left to every menu entry text
     */
    public TomahawkMenuAdapter(Activity activity, String[] stringArray, TypedArray iconArray) {
        mActivity = activity;
        mLayoutInflater = activity.getLayoutInflater();
        Collections.addAll(mStringArray, stringArray);
        for (int i = 0; i < iconArray.length(); i++) {
            mIconArray.add(iconArray.getResourceId(i, 0));
        }
    }

    /**
     * Show a content header. A content header provides information about the current {@link
     * org.tomahawk.tomahawk_android.utils.TomahawkListItem} that the user has navigated to. Like an
     * AlbumArt image with the {@link org.tomahawk.libtomahawk.collection.Album}s name, which is
     * shown at the top of the listview, if the user browses to a particular {@link
     * org.tomahawk.libtomahawk.collection.Album} in his {@link org.tomahawk.libtomahawk.collection.UserCollection}.
     *
     * @param list a reference to the list, so we can set its header view
     * @param user the {@link User} object to show in the header view
     */
    public void showContentHeader(ListView list, User user) {
        View contentHeaderView;
        if (list.getHeaderViewsCount() == 0) {
            contentHeaderView = mLayoutInflater.inflate(R.layout.content_header_user_navdrawer,
                    null);
            list.addHeaderView(contentHeaderView);
        }
        if (user != null) {
            updateContentHeader(list, user);
        }
    }

    public void updateContentHeader(ListView list, User user) {
        ViewHolder viewHolder = new ViewHolder(list,
                R.id.tomahawklistadapter_viewtype_contentheader);
        AdapterUtils.fillContentHeaderSmall(mActivity, viewHolder, user);
    }

    /**
     * @return the count of every item to display
     */
    @Override
    public int getCount() {
        return mStringArray.size();
    }

    /**
     * @return item for the given position
     */
    @Override
    public Object getItem(int position) {
        return mStringArray.get(position);
    }

    /**
     * Get the id of the item for the given position. (Id is equal to given position)
     */
    @Override
    public long getItemId(int position) {
        return position;
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
        View view = mLayoutInflater.inflate(R.layout.single_line_list_menu, null);
        TextView textView = (TextView) view.findViewById(R.id.single_line_list_menu_textview);
        ImageView imageView = (ImageView) view.findViewById(R.id.icon_menu_imageview);
        String string = mStringArray.get(position);
        Integer icon = mIconArray.get(position);
        if (string != null) {
            textView.setText(string);
        }
        if (icon != null) {
            imageView.setBackgroundResource(icon);
        }
        return view;
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
        return new View(mActivity);
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
        return 0;
    }
}
