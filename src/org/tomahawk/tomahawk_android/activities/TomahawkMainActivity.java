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

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;

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
import org.tomahawk.tomahawk_android.fragments.AlbumsFragment;
import org.tomahawk.tomahawk_android.fragments.ArtistsFragment;
import org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment;
import org.tomahawk.tomahawk_android.fragments.SearchableFragment;
import org.tomahawk.tomahawk_android.fragments.SlideMenuFragment;
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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The main Tomahawk activity
 */
public class TomahawkMainActivity extends SlidingFragmentActivity
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

    private PlaybackServiceConnection mPlaybackServiceConnection = new PlaybackServiceConnection(
            this);

    private PlaybackService mPlaybackService;

    private TomahawkService.TomahawkServiceConnection mTomahawkServiceConnection
            = new TomahawkService.TomahawkServiceConnection(this);

    private TomahawkService mTomahawkService;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tomahawk_main_activity);
        // check if the content frame contains the menu frame
        if (findViewById(R.id.slide_menu_frame) == null) {
            setBehindContentView(R.layout.slide_menu_layout);
            getSlidingMenu().setSlidingEnabled(true);
            getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
            final ActionBar actionBar = getSupportActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setLogo(R.drawable.ic_action_slidemenu);
        } else {
            View v = new View(this);
            setBehindContentView(v);
            getSlidingMenu().setSlidingEnabled(false);
            getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
            final ActionBar actionBar = getSupportActionBar();
            actionBar.setHomeButtonEnabled(false);
            actionBar.setLogo(R.drawable.ic_launcher);
        }

        // customize the SlidingMenu
        SlidingMenu sm = getSlidingMenu();
        sm.setFadeDegree(0.35f);
        sm.setShadowWidthRes(R.dimen.shadow_width);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            sm.setBehindOffsetRes(R.dimen.slidingmenu_offset_landscape);
        } else {
            sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        }

        // set the Behind View Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.slide_menu_frame, new SlideMenuFragment()).commit();

        // set customization variables on the ActionBar
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        View searchView = getLayoutInflater().inflate(R.layout.search_edittext, null);
        actionBar.setCustomView(searchView);
        actionBar.setDisplayShowCustomEnabled(true);

        // if not set yet, set our current default stack position to HUB_ID_COLLECTION
        if (mCurrentStackPosition == -1) {
            mCurrentStackPosition = HUB_ID_COLLECTION;
        }

        // initialize our ContentViewer, which will handle switching the fragments whenever an
        // entry in the slidingmenu is being clicked. Restore our saved state, if one exists.
        mContentViewer = new ContentViewer(this, getSupportFragmentManager(), R.id.content_frame);
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
            showSearchEditText();
        } else {
            hideSearchEditText();
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null) {
            // if the user clicks on the app icon in the top left corner, toggle the SlidingMenu
            if (item.getItemId() == android.R.id.home) {
                toggle();
                return true;
            }
        }
        return false;
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
     * Display the search editText in the top actionbar. Also display the soft keyboard.
     */
    public void showSearchEditText() {
        AutoCompleteTextView searchFrameTop = (AutoCompleteTextView) getSupportActionBar()
                .getCustomView().findViewById(R.id.search_edittext);
        searchFrameTop.setVisibility(AutoCompleteTextView.VISIBLE);
        findViewById(R.id.search_panel).setVisibility(LinearLayout.VISIBLE);

        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
    }

    /**
     * Hide the search editText in the top actionbar.
     */
    public void hideSearchEditText() {
        AutoCompleteTextView searchFrameTop = (AutoCompleteTextView) getSupportActionBar()
                .getCustomView().findViewById(R.id.search_edittext);
        searchFrameTop.setVisibility(AutoCompleteTextView.GONE);
        findViewById(R.id.search_panel).setVisibility(LinearLayout.GONE);
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
                                .setText(getString(R.string.localcollectionactivity_title_string));
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
                                .setText(getString(R.string.playlistsfragment_title_string));
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
