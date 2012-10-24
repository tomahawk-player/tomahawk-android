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

import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.TomahawkListAdapter;
import org.tomahawk.libtomahawk.TomahawkListAdapter.TomahawkListItem;
import org.tomahawk.libtomahawk.Track;
import org.tomahawk.libtomahawk.audio.PlaybackActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 *
 */
public class SearchFragment extends TomahawkListFragment implements OnItemClickListener, TextWatcher {

    private EditText mSearchText = null;
    private String mSearchString = null;
    private TomahawkListAdapter mTomahawkListAdapter;

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setFastScrollEnabled(true);
        getListView().setOnItemClickListener(this);
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_fragment_layout, null);
        return view;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onDestroyOptionsMenu()
     */
    @Override
    public void onDestroyOptionsMenu() {
        super.onDestroyOptionsMenu();

        if (mSearchText != null)
            mSearchText.removeTextChangedListener(this);
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        showFragment(idx);
    }

    /** Show the corresponding fragment (depends of which instance the item at position idx is of)
     * @param idx the position of the item inside the shown list*/
    public void showFragment(int idx) {
        Object item = mTomahawkListAdapter.getItem(idx);
        if (item instanceof TomahawkListItem) {
            if (item instanceof Album)
                mCollectionActivity.getTabsAdapter().replace(new TracksFragment((Album) item), false);
            if (item instanceof Artist)
                mCollectionActivity.getTabsAdapter().replace(new AlbumsFragment((Artist) item), false);
            if (item instanceof Track) {
                Bundle bundle = new Bundle();
                bundle.putInt(PlaybackActivity.PLAYLIST_COLLECTION_ID, getCurrentCollection().getId());

                bundle.putLong(PlaybackActivity.PLAYLIST_TRACK_ID, ((Track) item).getId());

                Intent playbackIntent = new Intent(getActivity(), PlaybackActivity.class);
                playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, bundle);
                startActivity(playbackIntent);
            }
        }
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#onLoadFinished(android.support.v4.content.Loader, org.tomahawk.libtomahawk.Collection)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        super.onLoadFinished(loader, coll);

        List<List<TomahawkListItem>> listArray = new ArrayList<List<TomahawkListItem>>();
        List<String> headerArray = new ArrayList<String>();
        String trackListTitle = getResources().getString(R.string.tracksfragment_title_string);
        String artistListTitle = getResources().getString(R.string.artistsfragment_title_string);
        String albumListTitle = getResources().getString(R.string.albumsfragment_title_string);

        listArray.add(new ArrayList<TomahawkListItem>());
        headerArray.add(trackListTitle);
        listArray.add(new ArrayList<TomahawkListItem>());
        headerArray.add(artistListTitle);
        listArray.add(new ArrayList<TomahawkListItem>());
        headerArray.add(albumListTitle);
        listArray.get(0).addAll(coll.getTracks());
        listArray.get(1).addAll(coll.getArtists());
        listArray.get(2).addAll(coll.getAlbums());

        mTomahawkListAdapter = new TomahawkListAdapter(getActivity(), R.layout.single_line_list_header,
                R.id.single_line_list_header_textview, R.layout.single_line_list_item, R.id.single_line_list_textview,
                listArray, headerArray);
        getListView().setAdapter(mTomahawkListAdapter);

        getSherlockActivity().supportInvalidateOptionsMenu();
    }

    /* 
     * (non-Javadoc)
     * @see android.text.TextWatcher#afterTextChanged(android.text.Editable)
     */
    @Override
    public void afterTextChanged(Editable s) {
    }

    /* 
     * (non-Javadoc)
     * @see android.text.TextWatcher#beforeTextChanged(java.lang.CharSequence, int, int, int)
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    /* 
     * (non-Javadoc)
     * @see android.text.TextWatcher#onTextChanged(java.lang.CharSequence, int, int, int)
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (getView() != null) {
            mSearchString = s.toString();
            if (s.length() <= 0)
                getListView().setVisibility(View.INVISIBLE);
            else
                getListView().setVisibility(View.VISIBLE);
            mTomahawkListAdapter.getFilter().filter(s.toString());
        }
    }

    /** Set the reference to the searchText, that is used to filter the custom listView
     *  @param searchText */
    public void setSearchText(EditText searchText) {
        mSearchText = searchText;
        if (mSearchText != null)
            mSearchText.addTextChangedListener(this);
    }

    /**
     * @return the mSearchString
     */
    public String getSearchString() {
        return mSearchString;
    }
}
