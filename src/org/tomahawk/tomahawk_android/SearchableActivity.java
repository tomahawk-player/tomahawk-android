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
import org.tomahawk.libtomahawk.CollectionLoader;
import org.tomahawk.libtomahawk.TomahawkListAdapter;
import org.tomahawk.libtomahawk.TomahawkListAdapter.TomahawkListItem;
import org.tomahawk.libtomahawk.Track;
import org.tomahawk.libtomahawk.audio.PlaybackActivity;

import android.app.Fragment;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 *
 */
public class SearchableActivity extends SherlockFragmentActivity implements OnItemClickListener, TextWatcher,
        OnEditorActionListener, LoaderManager.LoaderCallbacks<Collection> {

    private static IntentFilter sCollectionUpdateIntentFilter = new IntentFilter(Collection.COLLECTION_UPDATED);

    private CollectionUpdateReceiver mCollectionUpdatedReceiver;

    private EditText mSearchEditText = null;
    private String mSearchString = null;
    private TomahawkListAdapter mTomahawkListAdapter;
    ListView mList;
    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            mList.focusableViewAvailable(mList);
        }
    };

    /**
     * Handles incoming {@link Collection} updated broadcasts.
     */
    private class CollectionUpdateReceiver extends BroadcastReceiver {

        /*
         * (non-Javadoc)
         * 
         * @see
         * android.content.BroadcastReceiver#onReceive(android.content.Context,
         * android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Collection.COLLECTION_UPDATED))
                onCollectionUpdated();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = getLayoutInflater().inflate(R.layout.search_activity, null);
        setContentView(view);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        View searchView = getLayoutInflater().inflate(R.layout.collapsible_edittext, null);
        actionBar.setCustomView(searchView);
        ensureList();
        setSearchText((EditText) searchView.findViewById(R.id.search_edittext));

        mList.setOnItemClickListener(this);
    }

    /* 
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        getSupportLoaderManager().destroyLoader(0);
        getSupportLoaderManager().initLoader(0, null, this);

        if (mCollectionUpdatedReceiver == null) {
            mCollectionUpdatedReceiver = new CollectionUpdateReceiver();
            registerReceiver(mCollectionUpdatedReceiver, sCollectionUpdateIntentFilter);
        }
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

        if (mCollectionUpdatedReceiver != null) {
            unregisterReceiver(mCollectionUpdatedReceiver);
            mCollectionUpdatedReceiver = null;
        }
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        mHandler.removeCallbacks(mRequestFocus);
        mList = null;
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null) {
            if (item.getItemId() == android.R.id.home) {
                super.onBackPressed();
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        showFragment(idx);
    }

    private void ensureList() {
        if (mList != null)
            return;
        View rawListView = findViewById(R.id.listview);
        if (!(rawListView instanceof ListView)) {
            if (rawListView == null) {
                throw new RuntimeException("Your content must have a ListView whose id attribute is "
                        + "'R.id.listview'");
            }
            throw new RuntimeException("Content has view with id attribute 'R.id.listview' "
                    + "that is not a ListView class");
        }
        mList = (ListView) rawListView;

        if (mTomahawkListAdapter != null) {
            TomahawkListAdapter adapter = mTomahawkListAdapter;
            mTomahawkListAdapter = null;
            setListAdapter(adapter);
        }
        mHandler.post(mRequestFocus);
    }

    /**
     * Provide the cursor for the {@link ListView}.
     */
    public void setListAdapter(TomahawkListAdapter adapter) {
        mTomahawkListAdapter = adapter;
        if (mList != null) {
            adapter.setFiltered(true);
            mList.setAdapter(adapter);
        }
    }

    /** Show the corresponding {@link Fragment} (depends of which instance the item at position idx is of)
     * @param idx the position of the item inside the shown {@link ListView} */
    public void showFragment(int idx) {
        Object item = mTomahawkListAdapter.getItem(idx);
        if (item instanceof TomahawkListItem) {
            if (item instanceof Album) {
                Intent intent = getIntent(this, CollectionActivity.class);
                intent.putExtra(CollectionActivity.COLLECTION_ID_ALBUM, ((Album) item).getId());
                startActivity(intent);
                finish();
            }
            if (item instanceof Artist) {
                Intent intent = getIntent(this, CollectionActivity.class);
                intent.putExtra(CollectionActivity.COLLECTION_ID_ARTIST, ((Artist) item).getId());
                startActivity(intent);
                finish();
            }
            if (item instanceof Track) {
                Bundle bundle = new Bundle();
                bundle.putInt(PlaybackActivity.PLAYLIST_COLLECTION_ID,
                        ((TomahawkApp) getApplication()).getSourceList().getLocalSource().getCollection().getId());
                bundle.putLong(PlaybackActivity.PLAYLIST_TRACK_ID, ((Track) item).getId());
                Intent playbackIntent = getIntent(this, PlaybackActivity.class);
                playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, bundle);
                startActivity(playbackIntent);
                finish();
            }
        }
    }

    /**
     * Return the {@link Intent} defined by the given parameters
     * 
     * @param context
     * @param cls
     * @return
     */
    private static Intent getIntent(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    /**
     * Called when a {@link Collection} has been updated.
     */
    protected void onCollectionUpdated() {
        getSupportLoaderManager().restartLoader(0, null, this);
        if (mTomahawkListAdapter != null)
            mTomahawkListAdapter.getFilter().filter(mSearchString);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int,
     * android.os.Bundle)
     */
    @Override
    public Loader<Collection> onCreateLoader(int id, Bundle args) {
        return new CollectionLoader(this,
                ((TomahawkApp) getApplication()).getSourceList().getLocalSource().getCollection());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android
     * .support.v4.content.Loader)
     */
    @Override
    public void onLoaderReset(Loader<Collection> loader) {
    }

    /* 
     * (non-Javadoc)
     * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android.support.v4.content.Loader, java.lang.Object)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
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

        mTomahawkListAdapter = new TomahawkListAdapter(this, R.layout.single_line_list_header,
                R.id.single_line_list_header_textview, R.layout.single_line_list_item, R.id.single_line_list_textview,
                listArray, headerArray);
        setListAdapter(mTomahawkListAdapter);
        mTomahawkListAdapter.getFilter().filter(mSearchString);
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            mTomahawkListAdapter.getFilter().filter(query);
        }
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
        mSearchString = s.toString();
        mTomahawkListAdapter.getFilter().filter(s.toString());
    }

    /** Set the reference to the searchText, that is used to filter the custom {@link ListView}
     *  @param searchText */
    public void setSearchText(EditText searchText) {
        mSearchEditText = searchText;
        if (mSearchEditText != null) {
            mSearchEditText.addTextChangedListener(this);
            mSearchEditText.setOnEditorActionListener(this);
            mSearchEditText.setImeActionLabel("Go", KeyEvent.KEYCODE_ENTER);
        }
    }

    /**
     * @return the mSearchString
     */
    public String getSearchString() {
        return mSearchString;
    }

    /* 
     * (non-Javadoc)
     * @see android.widget.TextView.OnEditorActionListener#onEditorAction(android.widget.TextView, int, android.view.KeyEvent)
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (event == null || actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
                || event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
            return true;
        }
        return false;
    }
}
