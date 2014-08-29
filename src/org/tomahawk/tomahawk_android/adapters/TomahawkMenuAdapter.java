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
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.utils.AdapterUtils;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;

import android.app.Activity;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * This class populates the listview inside the navigation drawer
 */
public class TomahawkMenuAdapter extends StickyBaseAdapter {

    private final Activity mActivity;

    private LayoutInflater mLayoutInflater;

    private List<ResourceHolder> mItems = new ArrayList<ResourceHolder>();

    private User mUser;

    private boolean mShowHatchetMenu;

    private static class ResourceHolder {

        String title;

        int icon;

        int color;
    }

    /**
     * Constructs a new {@link TomahawkMenuAdapter}
     *
     * @param activity    reference to whatever {@link Activity}
     * @param stringArray Array of {@link String}s containing every menu entry text
     * @param iconArray   {@link TypedArray} containing an array of resource ids to be used to show
     *                    an icon left to every menu entry text
     * @param colorArray  {@link TypedArray} containing an array of resource ids to be used to show
     *                    the appropriately colored background (fancy stuff :>)
     */
    public TomahawkMenuAdapter(Activity activity, String[] stringArray, TypedArray iconArray,
            TypedArray colorArray) {
        mActivity = activity;
        mLayoutInflater = activity.getLayoutInflater();
        for (int i = 0; i < stringArray.length; i++) {
            ResourceHolder holder = new ResourceHolder();
            holder.title = stringArray[i];
            holder.icon = iconArray.getResourceId(i, 0);
            holder.color = mActivity.getResources().getColor(colorArray.getResourceId(i, 0));
            mItems.add(holder);
        }
    }

    public void setUser(User user) {
        mUser = user;
    }

    public void setShowHatchetMenu(boolean showHatchetMenu) {
        mShowHatchetMenu = showHatchetMenu;
    }

    /**
     * @return the count of every item to display
     */
    @Override
    public int getCount() {
        int correction = 0;
        if (mShowHatchetMenu) {
            if (mUser != null) {
                correction = 1;
            } else {
                correction = -1;
            }
        }
        return mItems.size() + correction;
    }

    /**
     * @return item for the given position
     */
    @Override
    public Object getItem(int position) {
        if (mShowHatchetMenu) {
            if (mUser != null) {
                position--;
            } else {
                position++;
            }
        }
        if (position < 0) {
            return mUser;
        } else {
            return mItems.get(position);
        }
    }

    /**
     * Get the id of the item for the given position. (Id is equal to given position)
     */
    @Override
    public long getItemId(int position) {
        Object item = getItem(position);
        if (item == mUser) {
            return FragmentUtils.HUB_ID_HOME;
        } else if (item instanceof ResourceHolder) {
            if (item.equals(mItems.get(0))) {
                return FragmentUtils.HUB_ID_DASHBOARD;
            } else if (item.equals(mItems.get(1))) {
                return FragmentUtils.HUB_ID_COLLECTION;
            } else if (item.equals(mItems.get(2))) {
                return FragmentUtils.HUB_ID_LOVEDTRACKS;
            } else if (item.equals(mItems.get(3))) {
                return FragmentUtils.HUB_ID_PLAYLISTS;
            }
        }
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
        Object item = getItem(position);
        if (item instanceof User) {
            View contentHeaderView = mLayoutInflater.inflate(R.layout.content_header_user_navdrawer,
                    null);
            ViewHolder viewHolder = new ViewHolder(contentHeaderView,
                    R.layout.content_header_user_navdrawer);
            AdapterUtils.fillContentHeaderSmall(mActivity, viewHolder, mUser);
            return contentHeaderView;
        } else if (item instanceof ResourceHolder) {
            View view = mLayoutInflater.inflate(R.layout.single_line_list_menu, parent, false);
            TextView textView = (TextView) view.findViewById(R.id.single_line_list_menu_textview);
            ImageView imageView = (ImageView) view.findViewById(R.id.icon_menu_imageview);
            ResourceHolder holder = (ResourceHolder) item;
            textView.setText(holder.title);
            TomahawkUtils.loadDrawableIntoImageView(mActivity, imageView, holder.icon);
            imageView.setBackgroundColor(holder.color);
            return view;
        } else {
            return new View(null);
        }
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

    @Override
    public void setShowContentHeaderSpacer(boolean showContentHeaderSpacer) {

    }
}
