/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
package org.tomahawk.tomahawk_android.activities;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionLoader;
import org.tomahawk.libtomahawk.collection.SourceList;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.TomahawkMenuAdapter;
import org.tomahawk.tomahawk_android.fragments.AlbumsFragment;
import org.tomahawk.tomahawk_android.fragments.ArtistsFragment;
import org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment;
import org.tomahawk.tomahawk_android.fragments.SearchableFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.fragments.TracksFragment;
import org.tomahawk.tomahawk_android.fragments.UserCollectionFragment;
import org.tomahawk.tomahawk_android.fragments.UserPlaylistsFragment;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.services.PlaybackService.PlaybackServiceConnection;
import org.tomahawk.tomahawk_android.services.PlaybackService.PlaybackServiceConnection.PlaybackServiceConnectionListener;
import org.tomahawk.tomahawk_android.services.TomahawkService;
import org.tomahawk.tomahawk_android.ui.widgets.SquareHeightRelativeLayout;
import org.tomahawk.tomahawk_android.utils.ContentViewer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The main Tomahawk activity
 */
public class TomahawkMainActivity extends ActionBarActivity
        implements PlaybackServiceConnectionListener,
        TomahawkService.TomahawkServiceConnection.TomahawkServiceConnectionListener,
        LoaderManager.LoaderCallbacks<Collection> {

    public static final int HUB_ID_HOME = 0;

    public static final int HUB_ID_SEARCH = 1;

    public static final int HUB_ID_COLLECTION = 2;

    public static final int HUB_ID_PLAYLISTS = 3;

    public static final int HUB_ID_STATIONS = 4;

    public static final int HUB_ID_FRIENDS = 5;

    public static final int HUB_ID_SETTINGS = 6;

    public static final String COLLECTION_ID_STOREDBACKSTACK = "collection_id_storedbackstack";

    public static final String COLLECTION_ID_STACKPOSITION = "collection_id_stackposition";

    public static final String TOMAHAWKSERVICE_READY = "tomahawkservice_ready";

    private CharSequence mTitle;

    private PlaybackServiceConnection mPlaybackServiceConnection = new PlaybackServiceConnection(
            this);

    private PlaybackService mPlaybackService;

    private TomahawkService.TomahawkServiceConnection mTomahawkServiceConnection
            = new TomahawkService.TomahawkServiceConnection(this);

    private TomahawkService mTomahawkService;

    private DrawerLayout mDrawerLayout;

    private ListView mDrawerList;

    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;

    private ContentViewer mContentViewer;

    private Collection mCollection;

    private CollectionUpdateReceiver mCollectionUpdatedReceiver;

    private View mNowPlayingView;

    private int mCurrentStackPosition = -1;

    /**
     * Handles incoming {@link Collection} updated broadcasts.
     */
    private class CollectionUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Collection.COLLECTION_UPDATED)) {
                onCollectionUpdated();
            }
        }
    }

    /**
     * Used to handle clicks on one of the breadcrumb items
     */
    private class BreadCrumbOnClickListener implements View.OnClickListener {

        String mSavedFragmentTag;

        public BreadCrumbOnClickListener(String savedFragmentTag) {
            mSavedFragmentTag = savedFragmentTag;
        }

        @Override
        public void onClick(View view) {
            getContentViewer()
                    .backToFragment(getContentViewer().getCurrentHubId(), mSavedFragmentTag,
                            true);
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        /**
         * Called every time an item inside the {@link android.widget.ListView} is clicked
         *
         * @param parent   The AdapterView where the click happened.
         * @param view     The view within the AdapterView that was clicked (this will be a view
         *                 provided by the adapter)
         * @param position The position of the view in the adapter.
         * @param id       The row id of the item that was clicked.
         */
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Show the correct hub, and if needed, display the search editText inside the ActionBar
            switch ((int) id) {
                case TomahawkMainActivity.HUB_ID_SEARCH:
                    mContentViewer
                            .setCurrentHubId(TomahawkMainActivity.HUB_ID_SEARCH);
                    setSearchEditTextVisibility(true);
                    break;
                case TomahawkMainActivity.HUB_ID_COLLECTION:
                    mContentViewer
                            .setCurrentHubId(TomahawkMainActivity.HUB_ID_COLLECTION);
                    setSearchEditTextVisibility(false);
                    break;
                case TomahawkMainActivity.HUB_ID_PLAYLISTS:
                    mContentViewer
                            .setCurrentHubId(TomahawkMainActivity.HUB_ID_PLAYLISTS);
                    setSearchEditTextVisibility(false);
                    break;
                case TomahawkMainActivity.HUB_ID_SETTINGS:
                    mContentViewer
                            .setCurrentHubId(TomahawkMainActivity.HUB_ID_SETTINGS);
                    setSearchEditTextVisibility(false);
                    break;
            }
            setTitle(getString(mContentViewer.getCurrentHubTitleResId()));
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tomahawk_main_activity);

        mTitle = mDrawerTitle = getTitle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Set up the TomahawkMenuAdapter. Give it its set of menu item texts and icons to display
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        TomahawkMenuAdapter slideMenuAdapter = new TomahawkMenuAdapter(this,
                getResources().getStringArray(R.array.slide_menu_items),
                getResources().obtainTypedArray(R.array.slide_menu_items_icons));
        mDrawerList.setAdapter(slideMenuAdapter);

        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // set customization variables on the ActionBar
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        View searchView = getLayoutInflater().inflate(R.layout.search_edittext, null);
        actionBar.setCustomView(searchView);
        actionBar.setDisplayShowCustomEnabled(false);

        // if not set yet, set our current default stack position to HUB_ID_COLLECTION
        if (mCurrentStackPosition == -1) {
            mCurrentStackPosition = HUB_ID_COLLECTION;
        }

        // initialize our ContentViewer, which will handle switching the fragments whenever an
        // entry in the slidingmenu is being clicked. Restore our saved state, if one exists.
        mContentViewer = new ContentViewer(this, getSupportFragmentManager(),
                R.id.content_viewer_frame);
        if (savedInstanceState == null) {
            mContentViewer.addRootToHub(HUB_ID_SEARCH, SearchableFragment.class);
            mContentViewer.addRootToHub(HUB_ID_COLLECTION, UserCollectionFragment.class);
            mContentViewer.addRootToHub(HUB_ID_PLAYLISTS, UserPlaylistsFragment.class);
            mContentViewer.addRootToHub(HUB_ID_SETTINGS, FakePreferenceFragment.class);
        } else {
            mCurrentStackPosition = savedInstanceState
                    .getInt(COLLECTION_ID_STACKPOSITION, HUB_ID_COLLECTION);
            ConcurrentHashMap<Integer, ArrayList<ContentViewer.FragmentStateHolder>> storedBackStack
                    = new ConcurrentHashMap<Integer, ArrayList<ContentViewer.FragmentStateHolder>>();
            if (savedInstanceState
                    .getSerializable(COLLECTION_ID_STOREDBACKSTACK) instanceof HashMap) {
                HashMap<Integer, ArrayList<ContentViewer.FragmentStateHolder>> temp
                        = (HashMap<Integer, ArrayList<ContentViewer.FragmentStateHolder>>) savedInstanceState
                        .getSerializable(COLLECTION_ID_STOREDBACKSTACK);
                storedBackStack.putAll(temp);
            } else if (savedInstanceState
                    .getSerializable(COLLECTION_ID_STOREDBACKSTACK) instanceof ConcurrentHashMap) {
                storedBackStack
                        = (ConcurrentHashMap<Integer, ArrayList<ContentViewer.FragmentStateHolder>>) savedInstanceState
                        .getSerializable(COLLECTION_ID_STOREDBACKSTACK);
            }

            if (storedBackStack != null && storedBackStack.size() > 0) {
                mContentViewer.setBackStack(storedBackStack);
            } else {
                mContentViewer.addRootToHub(HUB_ID_SEARCH, SearchableFragment.class);
                mContentViewer.addRootToHub(HUB_ID_COLLECTION, UserCollectionFragment.class);
                mContentViewer.addRootToHub(HUB_ID_PLAYLISTS, UserPlaylistsFragment.class);
                mContentViewer.addRootToHub(HUB_ID_SETTINGS, FakePreferenceFragment.class);
            }
        }

        //Setup our nowPlaying view at the top, if in landscape mode, otherwise at the bottom
        FrameLayout nowPlayingFrameTop = (FrameLayout) getSupportActionBar().getCustomView()
                .findViewById(R.id.now_playing_frame_top);
        FrameLayout nowPlayingFrameBottom = (FrameLayout) findViewById(
                R.id.now_playing_frame_bottom);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mNowPlayingView = getLayoutInflater().inflate(R.layout.now_playing_top, null);
            nowPlayingFrameTop.addView(mNowPlayingView);
            nowPlayingFrameTop.setVisibility(FrameLayout.VISIBLE);
            nowPlayingFrameBottom.setVisibility(FrameLayout.GONE);
        } else {
            mNowPlayingView = getLayoutInflater().inflate(R.layout.now_playing_bottom, null);
            nowPlayingFrameBottom.addView(mNowPlayingView);
            nowPlayingFrameTop.setVisibility(FrameLayout.GONE);
            nowPlayingFrameBottom.setVisibility(FrameLayout.VISIBLE);
        }
        mNowPlayingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent playbackIntent = TomahawkUtils
                        .getIntent(TomahawkMainActivity.this, PlaybackActivity.class);
                TomahawkMainActivity.this.startActivity(playbackIntent);
            }
        });
        if (mPlaybackService != null) {
            setNowPlayingInfo(mPlaybackService.getCurrentTrack());
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }


    @Override
    public void onStart() {
        super.onStart();

        //Setup our services
        Intent intent = new Intent(this, PlaybackService.class);
        startService(intent);
        bindService(intent, mPlaybackServiceConnection, Context.BIND_AUTO_CREATE);
        intent = new Intent(this, TomahawkService.class);
        startService(intent);
        bindService(intent, mTomahawkServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();

        SourceList sl = ((TomahawkApp) getApplication()).getSourceList();
        mCollection = sl.getCollectionFromId(sl.getLocalSource().getCollection().getId());
        if (mPlaybackService != null) {
            setNowPlayingInfo(mPlaybackService.getCurrentTrack());
        }

        getSupportLoaderManager().destroyLoader(0);
        getSupportLoaderManager().initLoader(0, null, this);

        IntentFilter intentFilter = new IntentFilter(Collection.COLLECTION_UPDATED);
        if (mCollectionUpdatedReceiver == null) {
            mCollectionUpdatedReceiver = new CollectionUpdateReceiver();
            registerReceiver(mCollectionUpdatedReceiver, intentFilter);
        }

        mContentViewer.setCurrentHubId(mCurrentStackPosition);
        // if we resume this activity with HUB_ID_SEARCH as the current stack position, make sure
        // that the searchEditText is being shown accordingly
        if (mCurrentStackPosition == HUB_ID_SEARCH) {
            setSearchEditTextVisibility(true);
        } else {
            setSearchEditTextVisibility(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mCurrentStackPosition = mContentViewer.getCurrentHubId();

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
        if (mTomahawkService != null) {
            unbindService(mTomahawkServiceConnection);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putSerializable(COLLECTION_ID_STOREDBACKSTACK, getContentViewer().getBackStack());
        bundle.putInt(COLLECTION_ID_STACKPOSITION, getContentViewer().getCurrentHubId());
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        setSearchEditTextVisibility(
                !drawerOpen && mContentViewer.getCurrentHubId() == HUB_ID_SEARCH);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * If the PlaybackService signals, that it is ready, this method is being called
     */
    @Override
    public void onPlaybackServiceReady() {
        setNowPlayingInfo(mPlaybackService.getCurrentTrack());
    }

    @Override
    public void setPlaybackService(PlaybackService ps) {
        mPlaybackService = ps;
    }

    @Override
    public void setTomahawkService(TomahawkService ps) {
        mTomahawkService = ps;
    }

    /**
     * If the TomahawkService signals, that it is ready, this method is being called
     */
    @Override
    public void onTomahawkServiceReady() {
        sendBroadcast(new Intent(TOMAHAWKSERVICE_READY));
    }

    public TomahawkService getTomahawkService() {
        return mTomahawkService;
    }

    /**
     * Whenever the back-button is pressed, go back in the ContentViewer, until the root tab is
     * reached. After that use the normal back-button functionality.
     */
    @Override
    public void onBackPressed() {
        if (!mContentViewer.back(mContentViewer.getCurrentHubId())) {
            super.onBackPressed();
        }
    }

    @Override
    public Loader<Collection> onCreateLoader(int id, Bundle args) {
        return new CollectionLoader(this,
                ((TomahawkApp) getApplication()).getSourceList().getLocalSource().getCollection());
    }

    @Override
    public void onLoaderReset(Loader<Collection> loader) {
    }

    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        mCollection = coll;
    }

    public PlaybackService getPlaybackService() {
        return mPlaybackService;
    }

    /**
     * Called when a {@link Collection} has been updated.
     */
    protected void onCollectionUpdated() {
        getSupportLoaderManager().restartLoader(0, null, this);
    }

    /**
     * Sets the playback information
     */
    public void setNowPlayingInfo(Track track) {
        if (mNowPlayingView == null) {
            supportInvalidateOptionsMenu();
        }
        if (mNowPlayingView != null) {
            mNowPlayingView.setClickable(false);
            ImageView nowPlayingInfoAlbumArt = (ImageView) mNowPlayingView
                    .findViewById(R.id.now_playing_album_art);
            TextView nowPlayingInfoArtist = (TextView) mNowPlayingView
                    .findViewById(R.id.now_playing_artist);
            TextView nowPlayingInfoTitle = (TextView) mNowPlayingView
                    .findViewById(R.id.now_playing_title);

            if (track != null) {
                if (nowPlayingInfoAlbumArt != null && nowPlayingInfoArtist != null
                        && nowPlayingInfoTitle != null) {
                    if (track.getAlbum() != null) {
                        track.getAlbum().loadBitmap(this, nowPlayingInfoAlbumArt);
                    } else {
                        nowPlayingInfoAlbumArt
                                .setImageResource(R.drawable.no_album_art_placeholder);
                    }
                    nowPlayingInfoArtist.setText(track.getArtist().toString());
                    nowPlayingInfoTitle.setText(track.getName());
                    nowPlayingInfoAlbumArt.setVisibility(View.VISIBLE);
                    nowPlayingInfoArtist.setVisibility(View.VISIBLE);
                    nowPlayingInfoTitle.setVisibility(View.VISIBLE);
                    mNowPlayingView.setClickable(true);
                }
            } else {
                nowPlayingInfoAlbumArt.setVisibility(View.GONE);
                nowPlayingInfoArtist.setVisibility(View.GONE);
                nowPlayingInfoTitle.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Set the search editText visibility in the top actionbar. If enabled is true, also display the
     * soft keyboard.
     */
    public void setSearchEditTextVisibility(boolean enabled) {
        if (enabled) {
            AutoCompleteTextView searchFrameTop = (AutoCompleteTextView) getSupportActionBar()
                    .getCustomView().findViewById(R.id.search_edittext);
            searchFrameTop.requestFocus();
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            findViewById(R.id.search_panel).setVisibility(LinearLayout.VISIBLE);

            InputMethodManager imm = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchFrameTop, 0);
        } else {
            AutoCompleteTextView searchFrameTop = (AutoCompleteTextView) getSupportActionBar()
                    .getCustomView().findViewById(R.id.search_edittext);
            getSupportActionBar().setDisplayShowCustomEnabled(false);
            findViewById(R.id.search_panel).setVisibility(LinearLayout.GONE);
        }
    }

    /**
     * Build the breadcumb navigation from the stack at the current tab position.
     */
    public void updateBreadCrumbNavigation() {
        ArrayList<ContentViewer.FragmentStateHolder> backStack = getContentViewer()
                .getBackStackAtPosition(getContentViewer().getCurrentHubId());
        LinearLayout breadCrumbFrame = (LinearLayout) findViewById(R.id.bread_crumb_frame);
        breadCrumbFrame.removeAllViews();
        if (breadCrumbFrame != null) {
            int validFragmentCount = 0;
            for (ContentViewer.FragmentStateHolder fpb : backStack) {
                if (fpb.clss == AlbumsFragment.class || fpb.clss == ArtistsFragment.class
                        || fpb.clss == TracksFragment.class
                        || fpb.clss == UserPlaylistsFragment.class
                        || fpb.clss == SearchableFragment.class
                        || fpb.clss == UserCollectionFragment.class) {
                    validFragmentCount++;
                }
            }
            for (ContentViewer.FragmentStateHolder fpb : backStack) {
                LinearLayout breadcrumbItem = (LinearLayout) getLayoutInflater()
                        .inflate(R.layout.breadcrumb_item, null);
                ImageView breadcrumbItemImageView = (ImageView) breadcrumbItem
                        .findViewById(R.id.fragmentLayout_icon_imageButton);
                SquareHeightRelativeLayout breadcrumbItemArrowLayout
                        = (SquareHeightRelativeLayout) breadcrumbItem
                        .findViewById(R.id.fragmentLayout_arrow_squareHeightRelativeLayout);
                SquareHeightRelativeLayout breadcrumbItemImageViewLayout
                        = (SquareHeightRelativeLayout) breadcrumbItem
                        .findViewById(R.id.fragmentLayout_icon_squareHeightRelativeLayout);
                TextView breadcrumbItemTextView = (TextView) breadcrumbItem
                        .findViewById(R.id.fragmentLayout_text_textView);
                if (fpb.clss == UserCollectionFragment.class) {
                    if (validFragmentCount == 1) {
                        breadcrumbItemTextView
                                .setText(getString(R.string.usercollectionfragment_title_string));
                    } else {
                        breadcrumbItemTextView.setVisibility(TextView.GONE);
                    }
                    breadcrumbItemImageView.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.ic_action_collection));
                    breadcrumbItemImageViewLayout.setVisibility(SquareHeightRelativeLayout.VISIBLE);
                    breadcrumbItemArrowLayout.setVisibility(SquareHeightRelativeLayout.GONE);
                    breadcrumbItem
                            .setOnClickListener(new BreadCrumbOnClickListener(fpb.fragmentTag));
                    breadCrumbFrame.addView(breadcrumbItem);
                } else if (fpb.clss == UserPlaylistsFragment.class) {
                    if (validFragmentCount == 1) {
                        breadcrumbItemTextView
                                .setText(getString(R.string.userplaylistsfragment_title_string));
                    } else {
                        breadcrumbItemTextView.setVisibility(TextView.GONE);
                    }
                    breadcrumbItemImageView.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.ic_action_playlist));
                    breadcrumbItemImageViewLayout.setVisibility(SquareHeightRelativeLayout.VISIBLE);
                    breadcrumbItemArrowLayout.setVisibility(SquareHeightRelativeLayout.GONE);
                    breadcrumbItem
                            .setOnClickListener(new BreadCrumbOnClickListener(fpb.fragmentTag));
                    breadCrumbFrame.addView(breadcrumbItem);
                } else if (fpb.clss == SearchableFragment.class) {
                    if (validFragmentCount == 1) {
                        breadcrumbItemTextView
                                .setText(getString(R.string.searchfragment_title_string));
                    } else {
                        breadcrumbItemTextView.setVisibility(TextView.GONE);
                    }
                    breadcrumbItemImageView.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.ic_action_search));
                    breadcrumbItemImageViewLayout.setVisibility(SquareHeightRelativeLayout.VISIBLE);
                    breadcrumbItemArrowLayout.setVisibility(SquareHeightRelativeLayout.GONE);
                    breadcrumbItem
                            .setOnClickListener(new BreadCrumbOnClickListener(fpb.fragmentTag));
                    breadCrumbFrame.addView(breadcrumbItem);
                } else if (fpb.clss == AlbumsFragment.class) {
                    Artist correspondingArtist = mCollection.getArtistById(fpb.tomahawkListItemId);
                    if (mCollection.getArtistById(fpb.tomahawkListItemId) != null) {
                        breadcrumbItemTextView.setText(correspondingArtist.getName());
                        breadcrumbItemImageViewLayout
                                .setVisibility(SquareHeightRelativeLayout.GONE);
                    } else {
                        if (validFragmentCount == 2) {
                            breadcrumbItemTextView
                                    .setText(getString(R.string.albumsfragment_title_string));
                        } else {
                            breadcrumbItemTextView.setVisibility(TextView.GONE);
                        }
                        breadcrumbItemImageView.setBackgroundDrawable(
                                getResources().getDrawable(R.drawable.ic_action_album));
                        breadcrumbItemImageViewLayout
                                .setVisibility(SquareHeightRelativeLayout.VISIBLE);
                    }
                    breadcrumbItem
                            .setOnClickListener(new BreadCrumbOnClickListener(fpb.fragmentTag));
                    breadCrumbFrame.addView(breadcrumbItem);
                } else if (fpb.clss == ArtistsFragment.class) {
                    if (validFragmentCount == 2) {
                        breadcrumbItemTextView
                                .setText(getString(R.string.artistsfragment_title_string));
                    } else {
                        breadcrumbItemTextView.setVisibility(TextView.GONE);
                    }
                    breadcrumbItemImageView.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.ic_action_artist));
                    breadcrumbItemImageViewLayout.setVisibility(SquareHeightRelativeLayout.VISIBLE);
                    breadcrumbItem
                            .setOnClickListener(new BreadCrumbOnClickListener(fpb.fragmentTag));
                    breadCrumbFrame.addView(breadcrumbItem);
                } else if (fpb.clss == TracksFragment.class) {
                    Album correspondingAlbum = mCollection.getAlbumById(fpb.tomahawkListItemId);
                    UserPlaylist correspondingUserPlaylist = mCollection
                            .getCustomPlaylistById(fpb.tomahawkListItemId);
                    if (fpb.tomahawkListItemType != null && fpb.tomahawkListItemType
                            .equals(TomahawkFragment.TOMAHAWK_ALBUM_ID)
                            && correspondingAlbum != null) {
                        breadcrumbItemTextView.setText(correspondingAlbum.getName());
                        breadcrumbItemImageViewLayout
                                .setVisibility(SquareHeightRelativeLayout.GONE);
                    } else if (fpb.tomahawkListItemType != null &&
                            fpb.tomahawkListItemType.equals(TomahawkFragment.TOMAHAWK_PLAYLIST_ID)
                            && correspondingUserPlaylist != null) {
                        breadcrumbItemTextView.setText(correspondingUserPlaylist.getName());
                        breadcrumbItemImageViewLayout
                                .setVisibility(SquareHeightRelativeLayout.GONE);
                    } else if (fpb.tomahawkListItemType != null && fpb.tomahawkListItemType
                            .equals(UserCollection.USERCOLLECTION_ALBUMCACHED)) {
                        breadcrumbItemTextView.setText(mCollection.getCachedAlbum().getName());
                        breadcrumbItemImageViewLayout
                                .setVisibility(SquareHeightRelativeLayout.GONE);
                    } else if (fpb.tomahawkListItemType != null && fpb.tomahawkListItemType
                            .equals(UserCollection.USERCOLLECTION_ARTISTCACHED)) {
                        breadcrumbItemTextView.setText(mCollection.getCachedArtist().getName());
                        breadcrumbItemImageViewLayout
                                .setVisibility(SquareHeightRelativeLayout.GONE);
                    } else {
                        if (validFragmentCount == 2) {
                            breadcrumbItemTextView
                                    .setText(getString(R.string.tracksfragment_title_string));
                        } else {
                            breadcrumbItemTextView.setVisibility(TextView.GONE);
                        }
                        breadcrumbItemImageView.setBackgroundDrawable(
                                getResources().getDrawable(R.drawable.ic_action_track));
                        breadcrumbItemImageViewLayout
                                .setVisibility(SquareHeightRelativeLayout.VISIBLE);
                    }
                    breadcrumbItem
                            .setOnClickListener(new BreadCrumbOnClickListener(fpb.fragmentTag));
                    breadCrumbFrame.addView(breadcrumbItem);
                }
            }
        }

    }

    /**
     * Set the visibilty of the breadcumb navigation view.
     *
     * @param showBreadcrumbs True, if breadcrumbs should be shown. False otherwise.
     */
    public void showBreadcrumbs(boolean showBreadcrumbs) {
        if (showBreadcrumbs) {
            findViewById(R.id.bread_crumb_container).setVisibility(FrameLayout.VISIBLE);
        } else {
            findViewById(R.id.bread_crumb_container).setVisibility(FrameLayout.GONE);
        }
    }

    /**
     * Returns this {@link Activity}s current {@link Collection}.
     *
     * @return the current {@link Collection} in this {@link Activity}.
     */
    public Collection getCollection() {
        return mCollection;
    }

    /**
     * @return the mContentViewer
     */
    public ContentViewer getContentViewer() {
        return mContentViewer;
    }

    /**
     * Whenever the backstack changes, update the breadcrumb navigation, so that it can represent
     * the correct stack.
     */
    public void onBackStackChanged() {
        updateBreadCrumbNavigation();
    }
}
