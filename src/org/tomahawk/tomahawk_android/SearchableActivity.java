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

import android.util.Log;
import org.tomahawk.libtomahawk.*;
import org.tomahawk.libtomahawk.audio.PlaybackActivity;
import org.tomahawk.libtomahawk.playlist.CustomPlaylist;
import org.tomahawk.libtomahawk.resolver.PipeLine;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
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
public class SearchableActivity extends SherlockFragmentActivity implements OnItemClickListener,
        OnEditorActionListener, LoaderManager.LoaderCallbacks<Collection>, Handler.Callback {

    private Activity mActivity = this;
    private PipeLine mPipeline;

    private Collection mCollection;

    private CollectionUpdateReceiver mCollectionUpdatedReceiver;
    private PipelineBroadcastReceiver mPipelineBroadcastReceiver;

    private ArrayList<Track> mCurrentShownTracks;
    private String mCurrentQueryString;
    private EditText mSearchEditText = null;
    private String mSearchString = null;
    private TomahawkListAdapter mTomahawkListAdapter;
    private ListView mList;
    final private Handler mHandler = new Handler();

    private Handler mAnimationHandler = new Handler(this);
    private static final int MSG_UPDATE_ANIMATION = 0x20;
    private Drawable mProgressDrawable;

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

    private class PipelineBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PipeLine.PIPELINE_RESULTSREPORTED)) {
                PipeLine pipeLine = ((TomahawkApp) getApplication()).getPipeLine();
                String qid = intent.getStringExtra(PipeLine.PIPELINE_RESULTSREPORTED_QID);
                mCurrentQueryString = pipeLine.getQuery(qid).getFullTextQuery();
                ArrayList<TomahawkBaseAdapter.TomahawkListItem> tomahawkListItems = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
                mCurrentShownTracks = (pipeLine.getQuery(qid).getTracks());
                tomahawkListItems.addAll(mCurrentShownTracks);
                if (mTomahawkListAdapter == null)
                    mTomahawkListAdapter = new TomahawkListAdapter(mActivity,
                            R.layout.double_line_list_item_with_playstate_image, R.id.double_line_list_imageview,
                            R.id.double_line_list_textview, R.id.double_line_list_textview2, tomahawkListItems);
                else
                    mTomahawkListAdapter.setListWithIndex(0, tomahawkListItems);
                mTomahawkListAdapter.setShowResolvedBy(true);
                setListAdapter(mTomahawkListAdapter);
            }
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

        mPipeline = ((TomahawkApp) getApplication()).getPipeLine();
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

        IntentFilter intentFilter = new IntentFilter(Collection.COLLECTION_UPDATED);
        if (mCollectionUpdatedReceiver == null) {
            mCollectionUpdatedReceiver = new CollectionUpdateReceiver();
            registerReceiver(mCollectionUpdatedReceiver, intentFilter);
        }
        intentFilter = new IntentFilter(PipeLine.PIPELINE_RESULTSREPORTED);
        if (mPipelineBroadcastReceiver == null) {
            mPipelineBroadcastReceiver = new PipelineBroadcastReceiver();
            registerReceiver(mPipelineBroadcastReceiver, intentFilter);
        }
        mSearchEditText.setOnEditorActionListener(this);
        mProgressDrawable = getResources().getDrawable(R.drawable.progress_indeterminate_tomahawk);
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
        if (mPipelineBroadcastReceiver != null) {
            unregisterReceiver(mPipelineBroadcastReceiver);
            mPipelineBroadcastReceiver = null;
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
        if (mTomahawkListAdapter.getItem(idx) instanceof Track) {
            long playlistId = mCollection.addPlaylist(CustomPlaylist.fromTrackList(mCurrentQueryString,
                    mCurrentShownTracks, (Track) mTomahawkListAdapter.getItem(idx)));
            Bundle bundle = new Bundle();
            bundle.putLong(PlaybackActivity.PLAYLIST_PLAYLIST_ID, playlistId);
            bundle.putLong(PlaybackActivity.PLAYLIST_TRACK_ID, ((Track) mTomahawkListAdapter.getItem(idx)).getId());

            Intent playbackIntent = getIntent(this, PlaybackActivity.class);
            playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, bundle);
            startActivity(playbackIntent);
        }
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
            mList.setAdapter(adapter);
        }
    }

    /** Show the corresponding {@link Fragment} (depends of which instance the item at position idx is of)
     * @param idx the position of the item inside the shown {@link ListView} */
    public void showFragment(int idx) {
        if (mTomahawkListAdapter != null) {
            Object item = mTomahawkListAdapter.getItem(idx);
            if (item instanceof TomahawkBaseAdapter.TomahawkListItem) {
                if (item instanceof Album) {
                    Intent intent = new Intent(this, CollectionActivity.class);
                    intent.putExtra(CollectionActivity.COLLECTION_ID_ALBUM, ((Album) item).getId());
                    startActivity(intent);
                    finish();
                }
                if (item instanceof Artist) {
                    Intent intent = new Intent(this, CollectionActivity.class);
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
    }

    /**
     * Return the {@link Intent} defined by the given parameters
     * 
     * @param context the context with which the intent will be created
     * @param cls the class which contains the activity to launch
     * @return the created intent
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
        mCollection = coll;
    }

    /** Set the reference to the searchText, that is used to filter the custom {@link ListView}
     *  @param searchText the EditText object which the listener is connected to
     */
    public void setSearchText(EditText searchText) {
        mSearchEditText = searchText;
        if (mSearchEditText != null) {
            mSearchEditText.setOnEditorActionListener(this);
            mSearchEditText.setImeActionLabel("Go", KeyEvent.KEYCODE_ENTER);
            mSearchEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
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
            if (v.getText().toString() != null && !TextUtils.isEmpty(v.getText().toString())) {
                ((TomahawkApp) getApplication()).getPipeLine().resolve(v.getText().toString());
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
                startLoadingAnimation();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        case MSG_UPDATE_ANIMATION:
            if (mPipeline.isResolving()) {
                mProgressDrawable.setLevel(mProgressDrawable.getLevel() + 500);
                getSupportActionBar().setLogo(mProgressDrawable);
                mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
                mAnimationHandler.sendEmptyMessageDelayed(MSG_UPDATE_ANIMATION, 50);
            } else {
                long time = System.currentTimeMillis() - testTime;
                Log.d("org.tomahawk","mPipeline stopped resolving, stopping animation after "+time+"ms");
                stopLoadingAnimation();
            }
            break;
        }
        return true;
    }

    long testTime;

    public void startLoadingAnimation() {
        testTime = System.currentTimeMillis();
        Log.d("org.tomahawk","starting animation");
        mAnimationHandler.sendEmptyMessageDelayed(MSG_UPDATE_ANIMATION, 50);
    }

    public void stopLoadingAnimation() {
        long time = System.currentTimeMillis() - testTime;
        Log.d("org.tomahawk","stopping animation after "+time+"ms");
        mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
        getSupportActionBar().setLogo(R.drawable.ic_launcher);
    }
}
