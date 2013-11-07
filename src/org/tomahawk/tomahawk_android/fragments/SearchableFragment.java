/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.fragments;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.PlaybackActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment which represents the "Tracks" tabview.
 */
public class SearchableFragment extends TomahawkFragment
        implements OnItemClickListener, CompoundButton.OnCheckedChangeListener,
        TextView.OnEditorActionListener {

    public static final String SEARCHABLEFRAGMENT_QUERY_STRING
            = "org.tomahawk.tomahawk_android.SEARCHABLEFRAGMENT_QUERRY_ID";

    private SearchableFragment mSearchableFragment = this;

    private ArrayList<Track> mCurrentShownTracks;

    private ArrayList<Album> mCurrentShownAlbums;

    private ArrayList<Artist> mCurrentShownArtists;

    private String mCurrentQueryString;

    private SearchableBroadcastReceiver mSearchableBroadcastReceiver;

    private Collection mCollection;

    private EditText mSearchEditText = null;

    private class SearchableBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PipeLine.PIPELINE_RESULTSREPORTED_FULLTEXTQUERY)) {
                String queryId = intent.getStringExtra(PipeLine.PIPELINE_RESULTSREPORTED_QID);
                mActivity.getContentViewer().getBackStackAtPosition(mCorrespondingStackId)
                        .get(0).queryString = mCurrentQueryString;
                showQueryResults(queryId);
            }
        }
    }

    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);

        if (inState != null && inState.containsKey(SEARCHABLEFRAGMENT_QUERY_STRING)
                && inState.getString(SEARCHABLEFRAGMENT_QUERY_STRING) != null) {
            mCurrentQueryString = inState.getString(SEARCHABLEFRAGMENT_QUERY_STRING);
        }
        if (getArguments() != null && getArguments().containsKey(SEARCHABLEFRAGMENT_QUERY_STRING)
                && getArguments().getString(SEARCHABLEFRAGMENT_QUERY_STRING) != null) {
            mCurrentQueryString = getArguments().getString(SEARCHABLEFRAGMENT_QUERY_STRING);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        setSearchText((EditText) mActivity.getSupportActionBar().getCustomView()
                .findViewById(R.id.search_edittext));
        // Sets the background colour to grey so that the text is visible
        AutoCompleteTextView textView = (AutoCompleteTextView) mActivity.getSupportActionBar()
                .getCustomView().findViewById(R.id.search_edittext);
        textView.setDropDownBackgroundResource(R.drawable.menu_dropdown_panel_tomahawk);
        setupAutoComplete();

        CheckBox onlineSourcesCheckBox = (CheckBox) mActivity
                .findViewById(R.id.search_onlinesources_checkbox);
        onlineSourcesCheckBox.setOnCheckedChangeListener(this);

        if (mSearchableBroadcastReceiver == null) {
            mSearchableBroadcastReceiver = new SearchableBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter(
                    PipeLine.PIPELINE_RESULTSREPORTED_FULLTEXTQUERY);
            mActivity.registerReceiver(mSearchableBroadcastReceiver, intentFilter);
        }

        if (mCurrentQueryString != null) {
            resolveFullTextQuery(mCurrentQueryString);
            mSearchEditText.setText(mCurrentQueryString);
            mSearchEditText.setSelection(mCurrentQueryString.length());
        }
    }

    /*
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

        InputMethodManager imm = (InputMethodManager) mActivity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);

        if (mSearchableBroadcastReceiver != null) {
            mActivity.unregisterReceiver(mSearchableBroadcastReceiver);
            mSearchableBroadcastReceiver = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putString(SEARCHABLEFRAGMENT_QUERY_STRING, mCurrentQueryString);
        super.onSaveInstanceState(out);
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        idx -= getListView().getHeaderViewsCount();
        if (idx >= 0) {
            if (getListAdapter().getItem(idx) instanceof Track) {
                ((UserCollection) mActivity.getCollection()).setCachedPlaylist(UserPlaylist
                        .fromTrackList(mCurrentQueryString, mCurrentShownTracks,
                                (Track) getListAdapter().getItem(idx)));
                Bundle bundle = new Bundle();
                bundle.putBoolean(UserCollection.USERCOLLECTION_PLAYLISTCACHED, true);
                bundle.putLong(PlaybackActivity.PLAYLIST_TRACK_ID,
                        ((Track) getListAdapter().getItem(idx)).getId());

                Intent playbackIntent = getIntent(mActivity, PlaybackActivity.class);
                playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, bundle);
                startActivity(playbackIntent);
            } else if (getListAdapter().getItem(idx) instanceof Album) {
                mCollection.setCachedAlbum((Album) getListAdapter().getItem(idx));
                mActivity.getContentViewer().
                        replace(mCorrespondingStackId, TracksFragment.class, -1,
                                UserCollection.USERCOLLECTION_ALBUMCACHED, false);
            } else if (getListAdapter().getItem(idx) instanceof Artist) {
                mCollection.setCachedArtist((Artist) getListAdapter().getItem(idx));
                mActivity.getContentViewer().
                        replace(mCorrespondingStackId, AlbumsFragment.class, -1,
                                UserCollection.USERCOLLECTION_ARTISTCACHED, false);
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        resolveFullTextQuery(mCurrentQueryString);
    }

    /* (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#onLoadFinished(android.support.v4.content.Loader, org.tomahawk.libtomahawk.Collection)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        super.onLoadFinished(loader, coll);

        mCollection = coll;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.widget.TextView.OnEditorActionListener#onEditorAction(android
     * .widget.TextView, int, android.view.KeyEvent)
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (event == null || actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == EditorInfo.IME_ACTION_DONE
                || event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            InputMethodManager imm = (InputMethodManager) mActivity
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
            String searchText = v.getText().toString();
            if (searchText != null && !TextUtils.isEmpty(searchText)) {
                addToAutoCompleteArray(searchText);
                setupAutoComplete();
                resolveFullTextQuery(searchText);
                return true;
            }
        }
        return false;
    }

    public void showQueryResults(String qid) {
        Query query = mPipeline.getQuery(qid);
        mCurrentQueryString = query.getFullTextQuery();
        List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
        ArrayList<TomahawkBaseAdapter.TomahawkListItem> trackResultList
                = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        mCurrentShownTracks = query.getTrackResults();
        trackResultList.addAll(mCurrentShownTracks);
        listArray.add(trackResultList);
        ArrayList<TomahawkBaseAdapter.TomahawkListItem> artistResultList
                = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        mCurrentShownArtists = query.getArtistResults();
        artistResultList.addAll(mCurrentShownArtists);
        listArray.add(artistResultList);
        ArrayList<TomahawkBaseAdapter.TomahawkListItem> albumResultList
                = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        mCurrentShownAlbums = query.getAlbumResults();
        albumResultList.addAll(mCurrentShownAlbums);
        listArray.add(albumResultList);
        if (getListAdapter() == null) {
            TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(mActivity, listArray);
            tomahawkListAdapter.setShowCategoryHeaders(true);
            tomahawkListAdapter.setShowResolvedBy(true);
            setListAdapter(tomahawkListAdapter);
        } else {
            ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
        }
        getListView().setOnItemClickListener(mSearchableFragment);
    }

    /**
     * Return the {@link Intent} defined by the given parameters
     *
     * @param context the context with which the intent will be created
     * @param cls     the class which contains the activity to launch
     * @return the created intent
     */
    private static Intent getIntent(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    private void setupAutoComplete() {
        // Autocomplete code
        AutoCompleteTextView textView = (AutoCompleteTextView) mActivity.getSupportActionBar()
                .getCustomView().findViewById(R.id.search_edittext);
        ArrayList<String> autoCompleteSuggestions = getAutoCompleteArray();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mActivity,
                android.R.layout.simple_list_item_1, autoCompleteSuggestions);
        textView.setAdapter(adapter);
    }

    /**
     * Set the reference to the searchText, that is used to filter the custom {@link
     * android.widget.ListView}
     *
     * @param searchText the EditText object which the listener is connected to
     */
    public void setSearchText(EditText searchText) {
        mSearchEditText = searchText;
        if (mSearchEditText != null) {
            mSearchEditText.setOnEditorActionListener(this);
            mSearchEditText.setImeActionLabel("Go", KeyEvent.KEYCODE_ENTER);
            mSearchEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        }
    }

    public void resolveFullTextQuery(String fullTextQuery) {
        mActivity.getContentViewer().backToRoot(mCorrespondingStackId, false);
        mCurrentQueryString = fullTextQuery;
        CheckBox onlineSourcesCheckBox = (CheckBox) mActivity
                .findViewById(R.id.search_onlinesources_checkbox);
        String queryId = mPipeline.resolve(fullTextQuery, !onlineSourcesCheckBox.isChecked());
        if (queryId != null) {
            mCorrespondingQueryIds.put(queryId, new Track());
            startLoadingAnimation();
        }
    }

    public void addToAutoCompleteArray(String newString) {
        ArrayList<String> myArrayList = getAutoCompleteArray();
        int highestIndex = myArrayList.size();

        for (String aMyArrayList : myArrayList) {
            if (newString != null && newString.equals(aMyArrayList)) {
                return;
            }
        }

        myArrayList.add(newString);

        SharedPreferences sPrefs = PreferenceManager
                .getDefaultSharedPreferences(mActivity.getBaseContext());
        SharedPreferences.Editor sEdit = sPrefs.edit();

        sEdit.putString("autocomplete_" + highestIndex, myArrayList.get(highestIndex));
        sEdit.putInt("autocomplete_size", myArrayList.size());
        sEdit.commit();
    }

    public ArrayList<String> getAutoCompleteArray() {
        SharedPreferences sPrefs = PreferenceManager
                .getDefaultSharedPreferences(mActivity.getBaseContext());
        ArrayList<String> myAList = new ArrayList<String>();
        int size = sPrefs.getInt("autocomplete_size", 0);

        for (int j = 0; j < size; j++) {
            myAList.add(sPrefs.getString("autocomplete_" + j, null));
        }
        return myAList;
    }
}
