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
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.collection.SourceList;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.hatchet.InfoSystem;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.TomahawkMenuAdapter;
import org.tomahawk.tomahawk_android.fragments.AlbumsFragment;
import org.tomahawk.tomahawk_android.fragments.ArtistsFragment;
import org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment;
import org.tomahawk.tomahawk_android.fragments.PlaybackFragment;
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

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

    public static final int HUB_ID_HOME = -1;

    public static final int HUB_ID_SEARCH = 0;

    public static final int HUB_ID_COLLECTION = 1;

    public static final int HUB_ID_PLAYLISTS = 2;

    public static final int HUB_ID_STATIONS = -2;

    public static final int HUB_ID_FRIENDS = -3;

    public static final int HUB_ID_SETTINGS = 3;

    public static final int HUB_ID_PLAYBACK = 4;

    public static final String COLLECTION_ID_STOREDBACKSTACK
            = "org.tomahawk.tomahawk_android.collection_id_storedbackstack";

    public static final String COLLECTION_ID_STACKPOSITION
            = "org.tomahawk.tomahawk_android.collection_id_stackposition";

    public static final String TOMAHAWKSERVICE_READY
            = "org.tomahawk.tomahawk_android.tomahawkservice_ready";

    public static final String PLAYBACKSERVICE_READY
            = "org.tomahawk.tomahawk_android.playbackservice_ready";

    public static final String SHOW_PLAYBACKFRAGMENT_ON_STARTUP
            = "org.tomahawk.tomahawk_android.show_playbackfragment_on_startup";

    private TomahawkApp mTomahawkApp;

    private PipeLine mPipeLine;

    private InfoSystem mInfoSystem;

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

    private UserCollection mUserCollection;

    private TomahawkMainReceiver mTomahawkMainReceiver;

    private View mNowPlayingFrame;

    private int mCurrentStackPosition = -1;

    private Drawable mProgressDrawable;

    private static final int MSG_UPDATE_ANIMATION = 0x20;

    // Used to display an animated progress drawable, as long as the PipeLine is resolving something
    private Handler mAnimationHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_ANIMATION:
                    if ((mPipeLine != null && mPipeLine.isResolving()) ||
                            (mPlaybackService != null && mPlaybackService.isPreparing()) ||
                            (mInfoSystem != null && mInfoSystem.isResolving())) {
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
    });

    /**
     * Handles incoming broadcasts.
     */
    private class TomahawkMainReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Collection.COLLECTION_UPDATED.equals(intent.getAction())) {
                onCollectionUpdated();
            }
            if (PlaybackService.BROADCAST_NEWTRACK.equals(intent.getAction())) {
                if (mPlaybackService != null) {
                    setNowPlayingInfo();
                }
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
                    break;
                case TomahawkMainActivity.HUB_ID_COLLECTION:
                    mContentViewer
                            .setCurrentHubId(TomahawkMainActivity.HUB_ID_COLLECTION);
                    break;
                case TomahawkMainActivity.HUB_ID_PLAYLISTS:
                    mContentViewer
                            .setCurrentHubId(TomahawkMainActivity.HUB_ID_PLAYLISTS);
                    break;
                case TomahawkMainActivity.HUB_ID_SETTINGS:
                    mContentViewer
                            .setCurrentHubId(TomahawkMainActivity.HUB_ID_SETTINGS);
                    break;
            }
            if (mDrawerLayout != null) {
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tomahawk_main_activity);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mTomahawkApp = ((TomahawkApp) getApplication());
        mPipeLine = mTomahawkApp.getPipeLine();
        mInfoSystem = mTomahawkApp.getInfoSystem();

        mProgressDrawable = getResources().getDrawable(R.drawable.progress_indeterminate_tomahawk);

        mTitle = mDrawerTitle = getTitle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (mDrawerLayout != null) {
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer,
                    R.string.drawer_open, R.string.drawer_close) {

                /** Called when a drawer has settled in a completely closed state. */
                public void onDrawerClosed(View view) {
                    getSupportActionBar().setTitle(mTitle);
                    supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }

                /** Called when a drawer has settled in a completely open state. */
                public void onDrawerOpened(View drawerView) {
                    getSupportActionBar().setTitle(mDrawerTitle);
                    supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }
            };
            // Set the drawer toggle as the DrawerListener
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }

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
        View customView = getLayoutInflater()
                .inflate(R.layout.search_custom_view, null);
        actionBar.setCustomView(customView);

        // set our default stack position to HUB_ID_COLLECTION
        mCurrentStackPosition = HUB_ID_COLLECTION;

        // initialize our ContentViewer, which will handle switching the fragments whenever an
        // entry in the slidingmenu is being clicked. Restore our saved state, if one exists.
        mContentViewer = new ContentViewer(this, getSupportFragmentManager(),
                R.id.content_viewer_frame);
        if (savedInstanceState == null) {
            mContentViewer.addRootToHub(HUB_ID_SEARCH, SearchableFragment.class);
            mContentViewer.addRootToHub(HUB_ID_COLLECTION, UserCollectionFragment.class);
            mContentViewer.addRootToHub(HUB_ID_PLAYLISTS, UserPlaylistsFragment.class);
            mContentViewer.addRootToHub(HUB_ID_SETTINGS, FakePreferenceFragment.class);
            mContentViewer.addRootToHub(HUB_ID_PLAYBACK, PlaybackFragment.class);
        } else {
            mCurrentStackPosition = savedInstanceState
                    .getInt(COLLECTION_ID_STACKPOSITION, HUB_ID_COLLECTION);
            ConcurrentHashMap<Integer, ArrayList<ContentViewer.FragmentStateHolder>>
                    storedBackStack
                    = new ConcurrentHashMap<Integer, ArrayList<ContentViewer.FragmentStateHolder>>();
            if (savedInstanceState
                    .getSerializable(COLLECTION_ID_STOREDBACKSTACK) instanceof HashMap) {
                HashMap<Integer, ArrayList<ContentViewer.FragmentStateHolder>> temp
                        = (HashMap<Integer, ArrayList<ContentViewer.FragmentStateHolder>>) savedInstanceState
                        .getSerializable(COLLECTION_ID_STOREDBACKSTACK);
                storedBackStack.putAll(temp);
            } else if (savedInstanceState
                    .getSerializable(
                            COLLECTION_ID_STOREDBACKSTACK) instanceof ConcurrentHashMap) {
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
                mContentViewer.addRootToHub(HUB_ID_PLAYBACK, PlaybackFragment.class);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (SHOW_PLAYBACKFRAGMENT_ON_STARTUP.equals(getIntent().getAction())) {
            // if this Activity is being shown after the user clicked the notification
            mCurrentStackPosition = HUB_ID_PLAYBACK;
        }
        if (getIntent().hasExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)) {
            mCurrentStackPosition = HUB_ID_SETTINGS;
            ContentViewer.FragmentStateHolder fragmentStateHolder =
                    mContentViewer.getBackStackAtPosition(mCurrentStackPosition).get(0);
            fragmentStateHolder.tomahawkListItemType = TomahawkService.AUTHENTICATOR_ID;
            fragmentStateHolder.tomahawkListItemKey = String.valueOf(
                    getIntent().getIntExtra(TomahawkService.AUTHENTICATOR_ID, -1));
        }

        SourceList sl = ((TomahawkApp) getApplication()).getSourceList();
        mUserCollection = (UserCollection) sl
                .getCollectionFromId(sl.getLocalSource().getCollection().getId());
        if (mPlaybackService != null) {
            setNowPlayingInfo();
        }

        getSupportLoaderManager().destroyLoader(0);
        getSupportLoaderManager().initLoader(0, null, this);

        if (mTomahawkMainReceiver == null) {
            mTomahawkMainReceiver = new TomahawkMainReceiver();
        }
        // Register intents that the BroadcastReceiver should listen to
        IntentFilter intentFilter = new IntentFilter(Collection.COLLECTION_UPDATED);
        registerReceiver(mTomahawkMainReceiver, intentFilter);
        intentFilter = new IntentFilter(PlaybackService.BROADCAST_NEWTRACK);
        registerReceiver(mTomahawkMainReceiver, intentFilter);

        mNowPlayingFrame = findViewById(R.id.now_playing_frame);
        mNowPlayingFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContentViewer.setCurrentHubId(HUB_ID_PLAYBACK);
            }
        });
        if (mPlaybackService != null) {
            setNowPlayingInfo();
        }

        mContentViewer.setCurrentHubId(mCurrentStackPosition);
    }

    @Override
    public void onPause() {
        super.onPause();

        mCurrentStackPosition = mContentViewer.getCurrentHubId();

        if (mTomahawkMainReceiver != null) {
            unregisterReceiver(mTomahawkMainReceiver);
            mTomahawkMainReceiver = null;
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
        bundle.putSerializable(COLLECTION_ID_STOREDBACKSTACK,
                getContentViewer().getBackStack());
        bundle.putInt(COLLECTION_ID_STACKPOSITION, getContentViewer().getCurrentHubId());
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        if (mDrawerLayout != null) {
            boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
            getSupportActionBar()
                    .setDisplayShowCustomEnabled(
                            !drawerOpen && mCurrentStackPosition == HUB_ID_SEARCH);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item) ||
                super.onOptionsItemSelected(item);
    }

    /**
     * If the PlaybackService signals, that it is ready, this method is being called
     */
    @Override
    public void onPlaybackServiceReady() {
        updateViewVisibility();
        setNowPlayingInfo();
        sendBroadcast(new Intent(PLAYBACKSERVICE_READY));
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
     * Whenever the back-button is pressed, go back in the ContentViewer, until the root fragment is
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
        mUserCollection = (UserCollection) coll;
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
    public void setNowPlayingInfo() {
        Track track = null;
        if (mPlaybackService != null) {
            track = mPlaybackService.getCurrentTrack();
        }
        if (mNowPlayingFrame != null) {
            ImageView nowPlayingInfoAlbumArt = (ImageView) mNowPlayingFrame
                    .findViewById(R.id.now_playing_album_art);
            TextView nowPlayingInfoArtist = (TextView) mNowPlayingFrame
                    .findViewById(R.id.now_playing_artist);
            TextView nowPlayingInfoTitle = (TextView) mNowPlayingFrame
                    .findViewById(R.id.now_playing_title);

            if (track != null) {
                if (nowPlayingInfoAlbumArt != null && nowPlayingInfoArtist != null
                        && nowPlayingInfoTitle != null) {
                    if (track.getAlbum() != null) {
                        TomahawkUtils.loadImageIntoImageView(this, nowPlayingInfoAlbumArt,
                                track.getAlbum());
                    }
                    nowPlayingInfoArtist.setText(track.getArtist().toString());
                    nowPlayingInfoTitle.setText(track.getName());
                }
            }
        }
    }

    public void updateViewVisibility() {
        if (mCurrentStackPosition
                == TomahawkMainActivity.HUB_ID_SEARCH
                || mCurrentStackPosition
                == TomahawkMainActivity.HUB_ID_SETTINGS
                || mCurrentStackPosition
                == TomahawkMainActivity.HUB_ID_PLAYBACK) {
            setBreadcrumbsVisibility(false);
        } else {
            setBreadcrumbsVisibility(true);
        }
        if (mCurrentStackPosition == TomahawkMainActivity.HUB_ID_SEARCH) {
            setSearchEditTextVisibility(true);
            showSoftKeyboard();
        } else {
            setSearchEditTextVisibility(false);
        }
        if (mCurrentStackPosition == TomahawkMainActivity.HUB_ID_PLAYBACK
                || mPlaybackService == null || mPlaybackService.getCurrentQuery() == null) {
            setNowPlayingInfoVisibility(false);
        } else {
            setNowPlayingInfoVisibility(true);
        }
    }

    public void showSoftKeyboard() {
        AutoCompleteTextView searchFrameTop = (AutoCompleteTextView) getSupportActionBar()
                .getCustomView().findViewById(R.id.search_edittext);
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchFrameTop, 0);
    }

    public void setNowPlayingInfoVisibility(boolean enabled) {
        if (enabled) {
            mNowPlayingFrame.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            mNowPlayingFrame.setVisibility(View.VISIBLE);
            if (mPlaybackService != null) {
                setNowPlayingInfo();
            }
        } else {
            mNowPlayingFrame.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
            mNowPlayingFrame.setVisibility(View.GONE);
        }
    }

    /**
     * Set the search editText visibility in the top actionbar. If enabled is true, also display the
     * soft keyboard.
     */
    public void setSearchEditTextVisibility(boolean enabled) {
        if (enabled) {
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            AutoCompleteTextView searchEditText = (AutoCompleteTextView) getSupportActionBar()
                    .getCustomView().findViewById(R.id.search_edittext);
            searchEditText.requestFocus();
            findViewById(R.id.search_panel).setVisibility(LinearLayout.VISIBLE);
        } else {
            getSupportActionBar().setDisplayShowCustomEnabled(false);
            findViewById(R.id.search_panel).setVisibility(LinearLayout.GONE);
        }
    }

    /**
     * Set the visibilty of the breadcumb navigation view.
     *
     * @param enabled True, if breadcrumbs should be shown. False otherwise.
     */
    public void setBreadcrumbsVisibility(boolean enabled) {
        if (enabled) {
            findViewById(R.id.bread_crumb_container).setVisibility(FrameLayout.VISIBLE);
        } else {
            findViewById(R.id.bread_crumb_container).setVisibility(FrameLayout.GONE);
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
                    breadcrumbItemTextView.setVisibility(TextView.GONE);
                    breadcrumbItemImageView.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.ic_action_collection));
                    breadcrumbItemImageViewLayout.setVisibility(SquareHeightRelativeLayout.VISIBLE);
                    breadcrumbItemArrowLayout.setVisibility(SquareHeightRelativeLayout.GONE);
                    breadcrumbItem
                            .setOnClickListener(new BreadCrumbOnClickListener(fpb.fragmentTag));
                    breadCrumbFrame.addView(breadcrumbItem);
                } else if (fpb.clss == UserPlaylistsFragment.class) {
                    breadcrumbItemTextView.setVisibility(TextView.GONE);
                    breadcrumbItemImageView.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.ic_action_playlist));
                    breadcrumbItemImageViewLayout.setVisibility(SquareHeightRelativeLayout.VISIBLE);
                    breadcrumbItemArrowLayout.setVisibility(SquareHeightRelativeLayout.GONE);
                    breadcrumbItem
                            .setOnClickListener(new BreadCrumbOnClickListener(fpb.fragmentTag));
                    breadCrumbFrame.addView(breadcrumbItem);
                } else if (fpb.clss == SearchableFragment.class) {
                    breadcrumbItemTextView.setVisibility(TextView.GONE);
                    breadcrumbItemImageView.setBackgroundDrawable(
                            getResources().getDrawable(R.drawable.ic_action_search));
                    breadcrumbItemImageViewLayout.setVisibility(SquareHeightRelativeLayout.VISIBLE);
                    breadcrumbItemArrowLayout.setVisibility(SquareHeightRelativeLayout.GONE);
                    breadcrumbItem
                            .setOnClickListener(new BreadCrumbOnClickListener(fpb.fragmentTag));
                    breadCrumbFrame.addView(breadcrumbItem);
                } else if (fpb.clss == AlbumsFragment.class) {
                    Artist correspondingArtist = Artist.getArtistByKey(fpb.tomahawkListItemKey);
                    if (Artist.getArtistByKey(fpb.tomahawkListItemKey) != null) {
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
                    Album correspondingAlbum = Album.getAlbumByKey(fpb.tomahawkListItemKey);
                    if (fpb.tomahawkListItemType != null && fpb.tomahawkListItemType
                            .equals(TomahawkFragment.TOMAHAWK_ALBUM_KEY)
                            && correspondingAlbum != null) {
                        breadcrumbItemTextView.setText(correspondingAlbum.getName());
                        breadcrumbItemImageViewLayout
                                .setVisibility(SquareHeightRelativeLayout.GONE);
                    } else if (fpb.tomahawkListItemType != null && fpb.tomahawkListItemType
                            .equals(TomahawkFragment.TOMAHAWK_USER_PLAYLIST_KEY)) {
                        UserPlaylist correspondingUserPlaylist = mUserCollection
                                .getUserPlaylistById(fpb.tomahawkListItemKey);
                        breadcrumbItemTextView.setText(correspondingUserPlaylist.getName());
                        breadcrumbItemImageViewLayout
                                .setVisibility(SquareHeightRelativeLayout.GONE);
                    } else if (fpb.tomahawkListItemType != null && fpb.tomahawkListItemType
                            .equals(TomahawkFragment.TOMAHAWK_HATCHET_USER_PLAYLIST_KEY)) {
                        UserPlaylist correspondingUserPlaylist = mUserCollection
                                .getUserPlaylistById(fpb.tomahawkListItemKey);
                        breadcrumbItemTextView.setText(correspondingUserPlaylist.getName());
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
     * Returns this {@link Activity}s current {@link org.tomahawk.libtomahawk.collection.UserCollection}.
     *
     * @return the current {@link org.tomahawk.libtomahawk.collection.UserCollection} in this {@link
     * Activity}.
     */
    public UserCollection getUserCollection() {
        return mUserCollection;
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
        mCurrentStackPosition = mContentViewer.getCurrentHubId();
        updateBreadCrumbNavigation();
    }

    /**
     * Start the loading animation. Called when beginning login process.
     */
    public void startLoadingAnimation() {
        mAnimationHandler.sendEmptyMessageDelayed(MSG_UPDATE_ANIMATION, 50);
    }

    /**
     * Stop the loading animation. Called when login/logout process has finished.
     */
    public void stopLoadingAnimation() {
        mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
        getSupportActionBar().setLogo(R.drawable.ic_launcher);
    }
}
