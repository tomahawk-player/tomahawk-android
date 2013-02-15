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
package org.tomahawk.tomahawk_android;

import java.util.ArrayList;
import java.util.List;

import org.tomahawk.libtomahawk.*;
import org.tomahawk.libtomahawk.audio.PlaybackActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

/**
 * Fragment which represents the "Album" tabview.
 */
public class AlbumsFragment extends TomahawkFragment implements OnItemClickListener, OnItemLongClickListener {

    private Artist mArtist;

    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);
        if (mActivity.getCollection() != null && getArguments() != null
                && getArguments().containsKey(TOMAHAWK_ARTIST_ID) && getArguments().getLong(TOMAHAWK_ARTIST_ID) > 0)
            mArtist = mActivity.getCollection().getArtistById(getArguments().getLong(TOMAHAWK_ARTIST_ID));
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        if (getListAdapter().getItem(idx) instanceof Album) {
            Bundle bundle = new Bundle();
            bundle.putLong(TOMAHAWK_ALBUM_ID, ((Album) getListAdapter().getItem(idx)).getId());
            if (mActivity instanceof CollectionActivity)
                ((CollectionActivity) mActivity).getTabsAdapter().replace(
                        CollectionActivity.LOCAL_COLLECTION_TAB_POSITION, TracksFragment.class,
                        ((Album) getListAdapter().getItem(idx)).getId(), TOMAHAWK_ALBUM_ID, false);
        }
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.internal.widget.IcsAdapterView.OnItemLongClickListener#onItemLongClick(com.actionbarsherlock.internal.widget.IcsAdapterView, android.view.View, int, long)
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        if (getListAdapter().getItem(position) instanceof Album) {
            Bundle bundle = new Bundle();
            bundle.putLong(PlaybackActivity.PLAYLIST_ALBUM_ID, ((Album) getListAdapter().getItem(position)).getId());
            bundle.putLong(PlaybackActivity.PLAYLIST_TRACK_ID,
                    ((Album) getListAdapter().getItem(position)).getTracks().get(0).getId());

            Intent playbackIntent = new Intent(getActivity(), PlaybackActivity.class);
            playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, bundle);
            startActivity(playbackIntent);
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#onLoadFinished(android.support.v4.content.Loader, org.tomahawk.libtomahawk.Collection)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        super.onLoadFinished(loader, coll);

        List<TomahawkBaseAdapter.TomahawkListItem> albums = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        if (mArtist != null) {
            albums.addAll(mArtist.getAlbums());
            List<TomahawkBaseAdapter.TomahawkMenuItem> headerArray = new ArrayList<TomahawkBaseAdapter.TomahawkMenuItem>();
            String albumListTitle = getResources().getString(R.string.albumsfragment_title_string);
            headerArray.add(new TomahawkBaseAdapter.TomahawkMenuItem(albumListTitle, R.drawable.ic_action_album));
            TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(getActivity(),
                    R.layout.double_line_list_item_with_image, R.id.double_line_list_imageview,
                    R.id.double_line_list_textview, R.id.double_line_list_textview2, albums);
            tomahawkListAdapter.setShowContentHeader(mArtist, R.layout.content_header, R.id.content_header_image,
                    R.id.content_header_textview, R.id.content_header_textview2);
            tomahawkListAdapter.setShowCategoryHeaders(headerArray, R.layout.single_line_list_header,
                    R.id.single_line_list_header_icon_imageview, R.id.single_line_list_header_textview);
            setListAdapter(tomahawkListAdapter);
            getListView().setOnItemClickListener(this);
            getListView().setOnItemLongClickListener(this);
        } else {
            albums.addAll(coll.getAlbums());
            setListAdapter(new TomahawkGridAdapter(getActivity(), R.layout.album_art_grid_item,
                    R.id.album_art_grid_image, R.id.album_art_grid_textView1, R.id.album_art_grid_textView2, albums));
            getGridView().setOnItemClickListener(this);
            getGridView().setOnItemLongClickListener(this);
            adaptColumnCount();
        }
    }

    public Artist getArtist() {
        return mArtist;
    }

}
