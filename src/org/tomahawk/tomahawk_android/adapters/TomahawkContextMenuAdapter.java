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
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Since we can't customize the appearance of the official context menu dialog, we have to create
 * our own {@link org.tomahawk.tomahawk_android.fragments.ContextMenuFragment} with this {@link
 * TomahawkContextMenuAdapter} to be used to populate it.
 */
public class TomahawkContextMenuAdapter extends BaseAdapter {

    private Context mContext;

    private LayoutInflater mLayoutInflater;

    private List<String> mStringArray = new ArrayList<String>();

    /**
     * Constructs a new {@link TomahawkContextMenuAdapter}
     */
    public TomahawkContextMenuAdapter(Context context, LayoutInflater layoutInflater,
            List<String> stringArray) {
        mContext = context;
        mLayoutInflater = layoutInflater;
        mStringArray = stringArray;
    }

    /**
     * Constructs a new {@link TomahawkContextMenuAdapter}
     */
    public TomahawkContextMenuAdapter(Context context, LayoutInflater layoutInflater,
            TomahawkListItem item, boolean showDelete) {
        mContext = context;
        mLayoutInflater = layoutInflater;
        mStringArray = getMenuItems(item, showDelete);
    }

    /**
     * @return length of the array of {@link String}s containing the context menu entry texts
     */
    @Override
    public int getCount() {
        return mStringArray.size();
    }

    /**
     * @return {@link String} for the given position
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
        View view = mLayoutInflater.inflate(R.layout.single_line_list_context_menu, parent, false);
        TextView textView = (TextView) view
                .findViewById(R.id.single_line_list_context_menu_textview);
        String string = mStringArray.get(position);
        if (string != null) {
            textView.setText(string);
        }
        return view;
    }

    private List<String> getMenuItems(TomahawkListItem item, boolean showDelete) {
        LinkedList<String> menuItems = new LinkedList<String>();
        if (item instanceof SocialAction) {
            item = ((SocialAction) item).getTargetObject();
            showDelete = false;
        } else if (item instanceof PlaylistEntry) {
            item = ((PlaylistEntry) item).getQuery();
        }

        if (item instanceof Playlist) {
            menuItems.add(mContext.getString(R.string.fake_context_menu_play));
            menuItems.add(mContext.getString(R.string.fake_context_menu_addtoqueue));
            menuItems.add(mContext.getString(R.string.fake_context_menu_addtoplaylist));
            if (showDelete) {
                menuItems.add(mContext.getString(R.string.fake_context_menu_delete));
            }
        } else if (item instanceof Query) {
            Query query = ((Query) item);
            if (query.isPlayable()) {
                menuItems.add(mContext.getString(R.string.fake_context_menu_play));
                menuItems
                        .add(mContext.getString(R.string.fake_context_menu_addtoqueue));
                menuItems.add(mContext.getString(R.string.fake_context_menu_addtoplaylist));
            }
            menuItems.add(mContext.getString(R.string.menu_item_share));
            menuItems.add(mContext.getString(R.string.menu_item_go_to_artist));
            menuItems.add(mContext.getString(R.string.menu_item_go_to_album));
            if (DatabaseHelper.getInstance().isItemLoved(query)) {
                menuItems.add(mContext.getString(R.string.fake_context_menu_unlove_track));
            } else {
                menuItems.add(mContext.getString(R.string.fake_context_menu_love_track));
            }
            if (showDelete) {
                menuItems.add(mContext.getString(R.string.fake_context_menu_delete));
            }
        } else if (item instanceof Artist) {
            menuItems.add(mContext.getString(R.string.fake_context_menu_play));
            menuItems.add(mContext.getString(R.string.fake_context_menu_addtoqueue));
            menuItems.add(mContext.getString(R.string.fake_context_menu_addtoplaylist));
            menuItems.add(mContext.getString(R.string.menu_item_share));
        } else if (item instanceof Album) {
            menuItems.add(mContext.getString(R.string.fake_context_menu_play));
            menuItems.add(mContext.getString(R.string.fake_context_menu_addtoqueue));
            menuItems.add(mContext.getString(R.string.fake_context_menu_addtoplaylist));
            menuItems.add(mContext.getString(R.string.menu_item_share));
            menuItems.add(mContext.getString(R.string.menu_item_go_to_artist));
        }
        return menuItems;
    }

}
