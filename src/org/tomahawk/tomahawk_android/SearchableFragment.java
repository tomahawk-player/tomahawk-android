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
package org.tomahawk.tomahawk_android;

import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.TomahawkBaseAdapter;
import org.tomahawk.libtomahawk.TomahawkListAdapter;
import org.tomahawk.libtomahawk.Track;
import org.tomahawk.libtomahawk.UserCollection;
import org.tomahawk.libtomahawk.audio.PlaybackActivity;
import org.tomahawk.libtomahawk.playlist.CustomPlaylist;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private static final String SEARCHABLEFRAGMENT_QUERY_STRING
            = "org.tomahawk.tomahawk_android.SEARCHABLEFRAGMENT_QUERY_STRING";

    private SearchableFragment mSearchableFragment = this;

    private PipeLine mPipeline;

    private ArrayList<Track> mCurrentShownTracks;

    private ArrayList<Album> mCurrentShownAlbums;

    private ArrayList<Artist> mCurrentShownArtists;

    private String mCurrentQueryString;

    private String mCurrentQueryId;

    private SearchableBroadcastReceiver mSearchableBroadcastReceiver;

    private Collection mCollection;

    private EditText mSearchEditText = null;

    private Handler mAnimationHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_ANIMATION:
                    if (mPipeline.isResolving()) {
                        mProgressDrawable.setLevel(mProgressDrawable.getLevel() + 500);
                        mActivity.getSupportActionBar().setLogo(mProgressDrawable);
                        mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
                        mAnimationHandler.sendEmptyMessageDelayed(MSG_UPDATE_ANIMATION, 50);
                    } else {
                        stopLoadingAnimation();
                    }
                    break;
            }
            return true;
        }
    });

    private static final int MSG_UPDATE_ANIMATION = 0x20;

    private Drawable mProgressDrawable;

    private class SearchableBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PipeLine.PIPELINE_RESULTSREPORTED)) {
                mCurrentQueryId = intent.getStringExtra(PipeLine.PIPELINE_RESULTSREPORTED_QID);
                showQueryResults(mCurrentQueryId);
            } else if (intent.getAction()
                    .equals(CollectionActivity.COLLECTION_ACTIONBAR_EXPANDED)) {
                onActionBarExpanded();
            }
        }
    }

    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);
        if (inState != null && inState.containsKey(SEARCHABLEFRAGMENT_QUERY_STRING)) {
            mCurrentQueryString = inState.getString(SEARCHABLEFRAGMENT_QUERY_STRING);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.searchablefragment_layout, null, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        CheckBox onlineSourcesCheckBox = (CheckBox) getView()
                .findViewById(R.id.searchactivity_onlinesources_checkbox);
        onlineSourcesCheckBox.setOnCheckedChangeListener(this);

        mProgressDrawable = getResources().getDrawable(R.drawable.progress_indeterminate_tomahawk);

        mPipeline = ((TomahawkApp) mActivity.getApplication()).getPipeLine();
        if (mSearchableBroadcastReceiver == null) {
            mSearchableBroadcastReceiver = new SearchableBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter(PipeLine.PIPELINE_RESULTSREPORTED);
            mActivity.registerReceiver(mSearchableBroadcastReceiver, intentFilter);
            intentFilter = new IntentFilter(CollectionActivity.COLLECTION_ACTIONBAR_EXPANDED);
            mActivity.registerReceiver(mSearchableBroadcastReceiver, intentFilter);
        }

        if (mCurrentQueryString != null) {
            resolveFullTextQuery(mCurrentQueryString);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

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
        idx -= mList.getHeaderViewsCount();
        if (idx >= 0) {
            if (getListAdapter().getItem(idx) instanceof Track) {
                ((UserCollection) mActivity.getCollection()).setCachedPlaylist(CustomPlaylist
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
                        replace(TomahawkTabsActivity.TAB_ID_SEARCH, TracksFragment.class, -1,
                                UserCollection.USERCOLLECTION_ALBUMCACHED, false);
            } else if (getListAdapter().getItem(idx) instanceof Artist) {
                mCollection.setCachedArtist((Artist) getListAdapter().getItem(idx));
                mActivity.getContentViewer().
                        replace(TomahawkTabsActivity.TAB_ID_SEARCH, TracksFragment.class, -1,
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

    public void onActionBarExpanded() {
        setSearchText((EditText) mActivity.getSupportActionBar().getCustomView()
                .findViewById(R.id.search_edittext));
        // Sets the background colour to grey so that the text is visible
        AutoCompleteTextView textView = (AutoCompleteTextView) mActivity.getSupportActionBar()
                .getCustomView().findViewById(R.id.search_edittext);
        textView.setDropDownBackgroundResource(R.drawable.menu_dropdown_panel_tomahawk);
        setupAutoComplete();
    }

    public void showQueryResults(String qid) {
        PipeLine pipeLine = ((TomahawkApp) mActivity.getApplication()).getPipeLine();
        Query query = pipeLine.getQuery(qid);
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
        if (mTomahawkBaseAdapter == null) {
            TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(mActivity, listArray);
            tomahawkListAdapter.setShowCategoryHeaders(true);
            tomahawkListAdapter.setShowResolvedBy(true);
            setListAdapter(tomahawkListAdapter);
        } else {
            mTomahawkBaseAdapter.setListArray(listArray);
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
        mActivity.getContentViewer().backToRoot(TomahawkTabsActivity.TAB_ID_SEARCH);
        CheckBox onlineSourcesCheckBox = (CheckBox) getView()
                .findViewById(R.id.searchactivity_onlinesources_checkbox);
        PipeLine pipeLine = ((TomahawkApp) mActivity.getApplication()).getPipeLine();
        pipeLine.resolve(fullTextQuery, !onlineSourcesCheckBox.isChecked());
        InputMethodManager imm = (InputMethodManager) mActivity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
        startLoadingAnimation();
    }

    public void addToAutoCompleteArray(String newString) {
        ArrayList<String> myArrayList = getAutoCompleteArray();
        int highestIndex = myArrayList.size();

        for (int i = 0; i < highestIndex; i++) {
            if (newString.equals(myArrayList.get(i))) {
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

    public void startLoadingAnimation() {
        mAnimationHandler.sendEmptyMessageDelayed(MSG_UPDATE_ANIMATION, 50);
    }

    public void stopLoadingAnimation() {
        mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
        mActivity.getSupportActionBar().setLogo(R.drawable.ic_action_slidemenu);
    }
}
