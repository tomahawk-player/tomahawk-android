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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionLoader;
import org.tomahawk.libtomahawk.collection.CustomPlaylist;
import org.tomahawk.libtomahawk.collection.SourceList;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.AlbumsFragment;
import org.tomahawk.tomahawk_android.fragments.ArtistsFragment;
import org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment;
import org.tomahawk.tomahawk_android.fragments.LocalCollectionFragment;
import org.tomahawk.tomahawk_android.fragments.PlaylistsFragment;
import org.tomahawk.tomahawk_android.fragments.SearchableFragment;
import org.tomahawk.tomahawk_android.fragments.SlideMenuFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.fragments.TracksFragment;
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
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionActivity extends TomahawkTabsActivity
        implements PlaybackServiceConnectionListener,
        TomahawkService.TomahawkServiceConnection.TomahawkServiceConnectionListener,
        LoaderManager.LoaderCallbacks<Collection> {

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

    private CollectionActivityBroadcastReceiver mCollectionActivityBroadcastReceiver;

    private int mCurrentStackPosition = -1;

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

    private class CollectionActivityBroadcastReceiver extends BroadcastReceiver {

        /* 
         * (non-Javadoc)
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackService.BROADCAST_NEWTRACK)) {
                if (mPlaybackService != null) {
                    setNowPlayingInfo(mPlaybackService.getCurrentTrack());
                }
            }
        }
    }

    private class BreadCrumbOnClickListener implements View.OnClickListener {

        String mSavedFragmentTag;

        public BreadCrumbOnClickListener(String savedFragmentTag) {
            mSavedFragmentTag = savedFragmentTag;
        }

        @Override
        public void onClick(View view) {
            getContentViewer()
                    .backToFragment(getContentViewer().getCurrentStackId(), mSavedFragmentTag,
                            true);
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

        setContentView(R.layout.collection_activity);
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

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        View searchView = getLayoutInflater().inflate(R.layout.search_edittext, null);
        actionBar.setCustomView(searchView);
        actionBar.setDisplayShowCustomEnabled(true);

        if (mCurrentStackPosition == -1) {
            mCurrentStackPosition = TomahawkTabsActivity.TAB_ID_COLLECTION;
        }

        mContentViewer = new ContentViewer(this, getSupportFragmentManager(), R.id.content_frame);
        if (savedInstanceState == null) {
            mContentViewer
                    .addRootToTab(TomahawkTabsActivity.TAB_ID_SEARCH, SearchableFragment.class);
            mContentViewer.addRootToTab(TomahawkTabsActivity.TAB_ID_COLLECTION,
                    LocalCollectionFragment.class);
            mContentViewer
                    .addRootToTab(TomahawkTabsActivity.TAB_ID_PLAYLISTS, PlaylistsFragment.class);
            mContentViewer.addRootToTab(TomahawkTabsActivity.TAB_ID_SETTINGS,
                    FakePreferenceFragment.class);
        } else {
            mCurrentStackPosition = savedInstanceState
                    .getInt(COLLECTION_ID_STACKPOSITION, TomahawkTabsActivity.TAB_ID_COLLECTION);
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
                mContentViewer
                        .addRootToTab(TomahawkTabsActivity.TAB_ID_SEARCH, SearchableFragment.class);
                mContentViewer.addRootToTab(TomahawkTabsActivity.TAB_ID_COLLECTION,
                        LocalCollectionFragment.class);
                mContentViewer.addRootToTab(TomahawkTabsActivity.TAB_ID_PLAYLISTS,
                        PlaylistsFragment.class);
                mContentViewer.addRootToTab(TomahawkTabsActivity.TAB_ID_SETTINGS,
                        FakePreferenceFragment.class);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Intent intent = new Intent(this, PlaybackService.class);
        startService(intent);
        bindService(intent, mPlaybackServiceConnection, Context.BIND_AUTO_CREATE);
        intent = new Intent(this, TomahawkService.class);
        startService(intent);
        bindService(intent, mTomahawkServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
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

        mContentViewer.setCurrentStackId(mCurrentStackPosition);
        if (mCurrentStackPosition == TomahawkTabsActivity.TAB_ID_SEARCH) {
            showSearchEditText();
        } else {
            hideSearchEditText();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.actionbarsherlock.app.SherlockActivity#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

        mCurrentStackPosition = mContentViewer.getCurrentStackId();

        if (mCollectionActivityBroadcastReceiver != null) {
            unregisterReceiver(mCollectionActivityBroadcastReceiver);
            mCollectionActivityBroadcastReceiver = null;
        }
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
        bundle.putInt(COLLECTION_ID_STACKPOSITION, getContentViewer().getCurrentStackId());
        super.onSaveInstanceState(bundle);
    }

    /* 
     * (non-Javadoc)
     * @see android.app.Activity#onNewIntent(android.content.Intent)
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
        if (mPlaybackService != null) {
            setNowPlayingInfo(mPlaybackService.getCurrentTrack());
        }

        return true;
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null) {
            if (item.getItemId() == android.R.id.home) {
                toggle();
                return true;
            }
        }
        return false;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceConnection.PlaybackServiceConnectionListener#onPlaybackServiceReady()
     */
    @Override
    public void onPlaybackServiceReady() {
        setNowPlayingInfo(mPlaybackService.getCurrentTrack());
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

    @Override
    public void setTomahawkService(TomahawkService ps) {
        mTomahawkService = ps;
    }

    @Override
    public void onTomahawkServiceReady() {
        sendBroadcast(new Intent(TOMAHAWKSERVICE_READY));
    }

    public TomahawkService getTomahawkService() {
        return mTomahawkService;
    }

    /* 
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        if (!mContentViewer.back(mContentViewer.getCurrentStackId())) {
            super.onBackPressed();
        }
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

    /**
     * Called when the back {@link Button} is pressed
     */
    public void onBackPressed(View view) {
        onBackPressed();
    }

    /**
     * Called when the nowPlayingInfo is clicked
     */
    public void onNowPlayingClicked(View view) {
        Intent playbackIntent = getIntent(this, PlaybackActivity.class);
        this.startActivity(playbackIntent);
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

    public void showSearchEditText() {
        AutoCompleteTextView searchFrameTop = (AutoCompleteTextView) getSupportActionBar()
                .getCustomView().findViewById(R.id.search_edittext);
        searchFrameTop.setVisibility(AutoCompleteTextView.VISIBLE);
        findViewById(R.id.search_panel).setVisibility(LinearLayout.VISIBLE);

        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
    }

    public void hideSearchEditText() {
        AutoCompleteTextView searchFrameTop = (AutoCompleteTextView) getSupportActionBar()
                .getCustomView().findViewById(R.id.search_edittext);
        searchFrameTop.setVisibility(AutoCompleteTextView.GONE);
        findViewById(R.id.search_panel).setVisibility(LinearLayout.GONE);
    }

    public void updateBreadCrumbNavigation() {
        ArrayList<ContentViewer.FragmentStateHolder> backStack = getContentViewer()
                .getBackStackAtPosition(getContentViewer().getCurrentStackId());
        LinearLayout breadCrumbFrame = (LinearLayout) findViewById(R.id.bread_crumb_frame);
        breadCrumbFrame.removeAllViews();
        if (breadCrumbFrame != null) {
            int validFragmentCount = 0;
            for (ContentViewer.FragmentStateHolder fpb : backStack) {
                if (fpb.clss == AlbumsFragment.class || fpb.clss == ArtistsFragment.class
                        || fpb.clss == TracksFragment.class || fpb.clss == PlaylistsFragment.class
                        || fpb.clss == SearchableFragment.class
                        || fpb.clss == LocalCollectionFragment.class) {
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
                if (fpb.clss == LocalCollectionFragment.class) {
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
                } else if (fpb.clss == PlaylistsFragment.class) {
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
                    CustomPlaylist correspondingCustomPlaylist = mCollection
                            .getCustomPlaylistById(fpb.tomahawkListItemId);
                    if (fpb.tomahawkListItemType != null && fpb.tomahawkListItemType
                            .equals(TomahawkFragment.TOMAHAWK_ALBUM_ID)
                            && correspondingAlbum != null) {
                        breadcrumbItemTextView.setText(correspondingAlbum.getName());
                        breadcrumbItemImageViewLayout
                                .setVisibility(SquareHeightRelativeLayout.GONE);
                    } else if (fpb.tomahawkListItemType != null &&
                            fpb.tomahawkListItemType.equals(TomahawkFragment.TOMAHAWK_PLAYLIST_ID)
                            && correspondingCustomPlaylist != null) {
                        breadcrumbItemTextView.setText(correspondingCustomPlaylist.getName());
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

    @Override
    public void onBackStackChanged() {
        updateBreadCrumbNavigation();
    }
}
