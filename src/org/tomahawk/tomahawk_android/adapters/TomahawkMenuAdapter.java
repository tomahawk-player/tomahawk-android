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

import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.ScriptResolverCollection;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.app.Activity;
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

    private final LayoutInflater mLayoutInflater;

    private List<ResourceHolder> mResourceHolders = new ArrayList<>();

    public static class ResourceHolder {

        public String id;

        public String title;

        public int iconResId;

        public ScriptResolverCollection collection;

        public Image image;

        public User user;

        public boolean isCloudCollection;
    }

    /**
     * Constructs a new {@link TomahawkMenuAdapter}
     *
     * @param activity reference to whatever {@link Activity}
     */
    public TomahawkMenuAdapter(Activity activity, ArrayList<ResourceHolder> resourceHolders) {
        mActivity = activity;
        mLayoutInflater = activity.getLayoutInflater();
        mResourceHolders = resourceHolders;
    }

    /**
     * @return the count of every item to display
     */
    @Override
    public int getCount() {
        return mResourceHolders.size();
    }

    /**
     * @return item for the given position
     */
    @Override
    public Object getItem(int position) {
        return mResourceHolders.get(position);
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
        Object item = getItem(position);
        ResourceHolder holder = (ResourceHolder) item;
        if (((ResourceHolder) item).user != null) {
            View contentHeaderView = mLayoutInflater.inflate(
                    R.layout.content_header_user_navdrawer, parent, false);
            TextView textView = (TextView) contentHeaderView.findViewById(R.id.textview1);
            textView.setText(holder.title.toUpperCase());
            TextView userTextView = (TextView) contentHeaderView.findViewById(R.id.usertextview1);
            ImageView userImageView =
                    (ImageView) contentHeaderView.findViewById(R.id.userimageview1);
            TomahawkUtils.loadUserImageIntoImageView(TomahawkApp.getContext(), userImageView,
                    holder.user, Image.getSmallImageSize(), userTextView);
            userImageView.setVisibility(View.VISIBLE);
            return contentHeaderView;
        } else {
            View view = mLayoutInflater.inflate(R.layout.single_line_list_menu, parent, false);
            TextView textView = (TextView) view
                    .findViewById(R.id.single_line_list_menu_textview);
            ImageView imageView = (ImageView) view.findViewById(R.id.icon_menu_imageview);
            textView.setText(holder.title.toUpperCase());
            if (holder.collection != null) {
                holder.collection.loadIcon(imageView, false);
            } else {
                TomahawkUtils.loadDrawableIntoImageView(mActivity, imageView, holder.iconResId);
            }
            return view;
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
        if (mResourceHolders.get(position).isCloudCollection) {
            View headerView =
                    mLayoutInflater.inflate(R.layout.menu_header_cloudcollection, parent, false);
            TextView textView = (TextView) headerView.findViewById(R.id.textview1);
            textView.setText(TomahawkApp.getContext().getString(
                    R.string.drawer_header_cloudcollections).toUpperCase());
            return headerView;
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
        if (mResourceHolders.get(position).isCloudCollection) {
            return 1;
        }
        return 0;
    }
}
