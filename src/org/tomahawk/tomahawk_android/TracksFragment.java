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

/**
 * Fragment which represents the "Tracks" tabview.
 */
public class TracksFragment extends TomahawkFragment implements OnItemClickListener {

    private Album mAlbum;

    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);
        if (mCollectionActivity.getCollection() != null && getArguments() != null
                && getArguments().containsKey(TOMAHAWK_ITEM_ID) && getArguments().getLong(TOMAHAWK_ITEM_ID) > 0)
            mAlbum = mCollectionActivity.getCollection().getAlbumById(getArguments().getLong(TOMAHAWK_ITEM_ID));
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        if (getListAdapter().getItem(idx) instanceof Track) {
            Bundle bundle = new Bundle();
            if (mAlbum != null)
                bundle.putLong(PlaybackActivity.PLAYLIST_ALBUM_ID, mAlbum.getId());
            else
                bundle.putInt(PlaybackActivity.PLAYLIST_COLLECTION_ID, getCurrentCollection().getId());

            bundle.putLong(PlaybackActivity.PLAYLIST_TRACK_ID, ((Track) getListAdapter().getItem(idx)).getId());

            Intent playbackIntent = new Intent(getActivity(), PlaybackActivity.class);
            playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, bundle);
            startActivity(playbackIntent);
        }
    }

    /* (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#onLoadFinished(android.support.v4.content.Loader, org.tomahawk.libtomahawk.Collection)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        super.onLoadFinished(loader, coll);

        List<TomahawkBaseAdapter.TomahawkListItem> tracks = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        TomahawkListAdapter tomahawkListAdapter;
        if (mAlbum != null) {
            tracks.addAll(mAlbum.getTracks());
            List<TomahawkBaseAdapter.TomahawkMenuItem> headerArray = new ArrayList<TomahawkBaseAdapter.TomahawkMenuItem>();
            String trackListTitle = getResources().getString(R.string.tracksfragment_title_string);
            headerArray.add(new TomahawkBaseAdapter.TomahawkMenuItem(trackListTitle, R.drawable.ic_action_album));
            tomahawkListAdapter = new TomahawkListAdapter(getActivity(), R.layout.double_line_list_item,
                    R.id.double_line_list_textview, R.id.double_line_list_textview2, tracks);
            tomahawkListAdapter.setShowContentHeader(mAlbum, R.layout.content_header, R.id.content_header_image,
                    R.id.content_header_textview, R.id.content_header_textview2);
            tomahawkListAdapter.setShowCategoryHeaders(headerArray, R.layout.single_line_list_header,
                    R.id.single_line_list_header_icon_imageview, R.id.single_line_list_header_textview);
        } else {
            tracks.addAll(coll.getTracks());
            tomahawkListAdapter = new TomahawkListAdapter(getActivity(), R.layout.double_line_list_item,
                    R.id.double_line_list_textview, R.id.double_line_list_textview2, tracks);
        }

        setListAdapter(tomahawkListAdapter);
        getListView().setOnItemClickListener(this);
    }

    public Album getAlbum() {
        return mAlbum;
    }
}
