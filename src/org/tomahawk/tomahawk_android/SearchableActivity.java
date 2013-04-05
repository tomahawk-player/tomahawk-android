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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;

import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.CollectionLoader;
import org.tomahawk.libtomahawk.audio.PlaybackService;
import org.tomahawk.libtomahawk.resolver.PipeLine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.util.ArrayList;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com> This class contains the UI code to search for
 *         music
 */
public class SearchableActivity extends TomahawkTabsActivity
        implements OnEditorActionListener, LoaderManager.LoaderCallbacks<Collection>,
        Handler.Callback,
        PlaybackService.PlaybackServiceConnection.PlaybackServiceConnectionListener {


    public TabsAdapter getTabsAdapter(){
        return null;
    }

    private PipeLine mPipeline;

    private Collection mCollection;

    private CollectionUpdateReceiver mCollectionUpdatedReceiver;

    private PlaybackService mPlaybackService;

    private PlaybackService.PlaybackServiceConnection mPlaybackServiceConnection
            = new PlaybackService.PlaybackServiceConnection(this);

    private EditText mSearchEditText = null;

    private String mSearchString = null;

    private Handler mAnimationHandler = new Handler(this);

    private static final int MSG_UPDATE_ANIMATION = 0x20;

    private Drawable mProgressDrawable;

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
            if (intent.getAction().equals(Collection.COLLECTION_UPDATED)) {
                onCollectionUpdated();
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
        setSearchText((EditText) searchView.findViewById(R.id.search_edittext));

        // Sets the background colour to grey so that the text is visible
        AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.search_edittext);
        textView.setDropDownBackgroundResource(R.drawable.menu_dropdown_panel_tomahawk);

        mPipeline = ((TomahawkApp) getApplication()).getPipeLine();

        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.searchactivity_fragmentcontainer_framelayout,
                    Fragment.instantiate(this, SearchableFragment.class.getName()));
            ft.commit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Intent playbackIntent = new Intent(this, PlaybackService.class);
        startService(playbackIntent);
        bindService(playbackIntent, mPlaybackServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        setupAutoComplete();

        getSupportLoaderManager().destroyLoader(0);
        getSupportLoaderManager().initLoader(0, null, this);

        IntentFilter intentFilter = new IntentFilter(Collection.COLLECTION_UPDATED);
        if (mCollectionUpdatedReceiver == null) {
            mCollectionUpdatedReceiver = new CollectionUpdateReceiver();
            registerReceiver(mCollectionUpdatedReceiver, intentFilter);
        }

        mProgressDrawable = getResources().getDrawable(R.drawable.progress_indeterminate_tomahawk);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mCollectionUpdatedReceiver != null) {
            unregisterReceiver(mCollectionUpdatedReceiver);
            mCollectionUpdatedReceiver = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mPlaybackService != null) {
            unbindService(mPlaybackServiceConnection);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.actionbarsherlock.app.SherlockFragmentActivity#onOptionsItemSelected
     * (android.view.MenuItem)
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
     *
     * @see
     * android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android
     * .support.v4.content.Loader, java.lang.Object)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        mCollection = coll;
    }

    private void setupAutoComplete() {
        // Autocomplete code
        AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.search_edittext);
        ArrayList<String> autoCompleteSuggestions = getAutoCompleteArray();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, autoCompleteSuggestions);
        textView.setAdapter(adapter);
    }

    /**
     * Set the reference to the searchText, that is used to filter the custom {@link ListView}
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

    /**
     * @return the mSearchString
     */
    public String getSearchString() {
        return mSearchString;
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

    public void resolveFullTextQuery(String fullTextQuery) {
        getSupportFragmentManager().popBackStack();
        CheckBox onlineSourcesCheckBox = (CheckBox) findViewById(
                R.id.searchactivity_onlinesources_checkbox);
        PipeLine pipeLine = ((TomahawkApp) getApplication()).getPipeLine();
        pipeLine.resolve(fullTextQuery, !onlineSourcesCheckBox.isChecked());
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
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

        SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Editor sEdit = sPrefs.edit();

        sEdit.putString("autocomplete_" + highestIndex, myArrayList.get(highestIndex));
        sEdit.putInt("autocomplete_size", myArrayList.size());
        sEdit.commit();
    }

    public ArrayList<String> getAutoCompleteArray() {
        SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        ArrayList<String> myAList = new ArrayList<String>();
        int size = sPrefs.getInt("autocomplete_size", 0);

        for (int j = 0; j < size; j++) {
            myAList.add(sPrefs.getString("autocomplete_" + j, null));
        }
        return myAList;
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
                    stopLoadingAnimation();
                }
                break;
        }
        return true;
    }

    public void startLoadingAnimation() {
        mAnimationHandler.sendEmptyMessageDelayed(MSG_UPDATE_ANIMATION, 50);
    }

    public void stopLoadingAnimation() {
        mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
        getSupportActionBar().setLogo(R.drawable.ic_launcher);
    }

    @Override
    public Collection getCollection() {
        return mCollection;
    }

    /*
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceConnection.PlaybackServiceConnectionListener#onPlaybackServiceReady()
     */
    @Override
    public void onPlaybackServiceReady() {
    }

    /*
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceConnection.PlaybackServiceConnectionListener#setPlaybackService(org.tomahawk.libtomahawk.audio.PlaybackService)
     */
    @Override
    public void setPlaybackService(PlaybackService ps) {
        mPlaybackService = ps;
    }

    @Override
    public PlaybackService getPlaybackService() {
        return mPlaybackService;
    }
}
