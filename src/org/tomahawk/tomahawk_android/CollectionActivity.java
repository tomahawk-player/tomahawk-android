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
package org.tomahawk.tomahawk_android;

import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.SourceList;
import org.tomahawk.libtomahawk.Track;
import org.tomahawk.libtomahawk.audio.PlaybackActivity;
import org.tomahawk.libtomahawk.audio.PlaybackService;
import org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceConnection;
import org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceConnection.PlaybackServiceConnectionListener;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;

public class CollectionActivity extends SherlockFragmentActivity implements PlaybackServiceConnectionListener,
        OnActionExpandListener {

    public static final String COLLECTION_ID_EXTRA = "collection_id";

    private PlaybackService mPlaybackService;
    private TabsAdapter mTabsAdapter;
    private FragmentManager mFragmentManager;
    private Menu mMenu;
    private Collection mCollection;
    private boolean mSearchModeEnabled = false;
    private boolean mSoftKeyboardShown = false;
    private SearchFragment mSearchFragment;

    private PlaybackServiceConnection mPlaybackServiceConnection = new PlaybackServiceConnection(this);
    private NewTrackBroadcastReceiver mNewTrackBroadcastReceiver;

    private class NewTrackBroadcastReceiver extends BroadcastReceiver {

        /* 
         * (non-Javadoc)
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackService.BROADCAST_NEWTRACK)) {
                setNowPlayingInfo(mPlaybackService.getCurrentTrack());
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

        View view = getLayoutInflater().inflate(R.layout.tomahawk_main_activity, null);
        setContentView(view);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.removeAllTabs();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(false);
        View actionBarPlaybackTop = getLayoutInflater().inflate(R.layout.now_playing_top, null);
        actionBar.setCustomView(actionBarPlaybackTop);

        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        mFragmentManager = getSupportFragmentManager();
        mTabsAdapter = new TabsAdapter(this, mFragmentManager, viewPager);
        mTabsAdapter.addTab(actionBar.newTab().setText(R.string.localcollectionactivity_title_string),
                new LocalCollectionFragment());
        mTabsAdapter.addTab(actionBar.newTab().setText(R.string.remotecollectionactivity_title_string),
                new RemoteCollectionFragment());
        mTabsAdapter.addTab(actionBar.newTab().setText(R.string.globalcollectionfragment_title_string),
                new GlobalCollectionFragment());
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
        mCollection = sl.getCollectionFromId(getIntent().getIntExtra(COLLECTION_ID_EXTRA, 0));

        if (mNewTrackBroadcastReceiver == null)
            mNewTrackBroadcastReceiver = new NewTrackBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(PlaybackService.BROADCAST_NEWTRACK);
        registerReceiver(mNewTrackBroadcastReceiver, intentFilter);

        Intent playbackIntent = new Intent(this, PlaybackService.class);
        bindService(playbackIntent, mPlaybackServiceConnection, Context.BIND_WAIVE_PRIORITY);
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceConnection.PlaybackServiceConnectionListener#setPlaybackService(org.tomahawk.libtomahawk.audio.PlaybackService)
     */
    @Override
    public void setPlaybackService(PlaybackService ps) {
        mPlaybackService = ps;
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
     * 
     * @see com.actionbarsherlock.app.SherlockActivity#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

        if (mPlaybackService != null)
            unbindService(mPlaybackServiceConnection);

        if (mNewTrackBroadcastReceiver != null) {
            unregisterReceiver(mNewTrackBroadcastReceiver);
            mNewTrackBroadcastReceiver = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.actionbarsherlock.app.SherlockFragmentActivity#onPrepareOptionsMenu
     * (android.view.Menu)
     */
    @SuppressLint("NewApi")
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        ImageButton overflowMenuButton = (ImageButton) findViewById(R.id.imageButton_overflowmenu);
        MenuItem searchActionItem = menu.add(0, R.id.collectionactivity_search_menu_button, 0, "Search");
        searchActionItem.setIcon(R.drawable.ic_action_search).setActionView(R.layout.collapsible_edittext).setOnActionExpandListener(
                this).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            searchActionItem.setVisible(true);

        if (mSearchModeEnabled) {
            searchActionItem.setVisible(true);
            if (!searchActionItem.isActionViewExpanded())
                searchActionItem.expandActionView();
            View actionView = searchActionItem.getActionView();
            EditText searchEditText = (EditText) actionView.findViewById(R.id.search_edittext);
            if (searchEditText != null)
                searchEditText.requestFocus();
            if (!mSoftKeyboardShown) {
                mSoftKeyboardShown = true;
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(searchEditText, 0);
            }
            if (mSearchFragment != null && searchEditText != null) {
                mSearchFragment.setSearchText(searchEditText);
                searchEditText.setText(mSearchFragment.getSearchString());
                searchEditText.setSelection(searchEditText.getText().length());
            }
        } else {
            mSoftKeyboardShown = false;
            if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                getSupportActionBar().setDisplayShowCustomEnabled(false);
                searchActionItem.setVisible(false);
            }
            searchActionItem.collapseActionView();
        }

        if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH && ViewConfiguration.get(
                getApplicationContext()).hasPermanentMenuKey())
                || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            overflowMenuButton.setVisibility(ImageButton.GONE);
            overflowMenuButton.setClickable(false);
            overflowMenuButton.setLayoutParams(new LayoutParams(0, 0));
        }
        if (mPlaybackService != null)
            setNowPlayingInfo(mPlaybackService.getCurrentTrack());
        refreshNowPlayingBarVisibility();

        mMenu = menu;

        return super.onPrepareOptionsMenu(menu);
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null) {
            if (item.getItemId() == R.id.collectionactivity_search_menu_button) {
                onSearchButtonClicked();
                return true;
            } else if (item.getItemId() == android.R.id.home) {
                super.onBackPressed();
                return true;
            }
        }
        return false;
    }

    /**
     * Called when the search button is pressed
     * @param view */
    public void onSearchButtonClicked(View view) {
        onSearchButtonClicked();
    }

    public void onSearchButtonClicked() {
        toggleSearchMode(mMenu.findItem(R.id.collectionactivity_search_menu_button));
    }

    public void toggleSearchMode(MenuItem item) {
        if (item.getItemId() == R.id.collectionactivity_search_menu_button) {
            if (!mSearchModeEnabled) {
                mSearchModeEnabled = true;
                mSearchFragment = new SearchFragment();
                mTabsAdapter.replace(mSearchFragment, false);
                supportInvalidateOptionsMenu();
            } else {
                mSearchModeEnabled = false;
                supportInvalidateOptionsMenu();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.actionbarsherlock.app.SherlockFragmentActivity#onConfigurationChanged
     * (android.content.res.Configuration)
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        supportInvalidateOptionsMenu();
    }

    /**
     * Called when the nowPlayingInfo is clicked
     * 
     */
    public void onNowPlayingClicked(View view) {
        Intent playbackIntent = getIntent(this, PlaybackActivity.class);
        this.startActivity(playbackIntent);
    }

    /**
     * Return the intent defined by the given parameters
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
     * Sets the playback information
     * 
     * @param track
     */
    public void setNowPlayingInfo(Track track) {
        RelativeLayout nowPlayingInfoTop = (RelativeLayout) findViewById(R.id.now_playing_top);
        LinearLayout nowPlayingBottom = (LinearLayout) findViewById(R.id.now_playing_bottom);
        if (nowPlayingInfoTop != null)
            nowPlayingInfoTop.setClickable(false);
        if (nowPlayingBottom != null)
            nowPlayingBottom.setClickable(false);

        if (track == null)
            return;

        ImageView nowPlayingInfoAlbumArtTop = (ImageView) findViewById(R.id.now_playing_album_art_top);
        TextView nowPlayingInfoArtistTop = (TextView) findViewById(R.id.now_playing_artist_top);
        TextView nowPlayingInfoTitleTop = (TextView) findViewById(R.id.now_playing_title_top);
        ImageView nowPlayingInfoAlbumArtBottom = (ImageView) findViewById(R.id.now_playing_album_art_bottom);
        TextView nowPlayingInfoArtistBottom = (TextView) findViewById(R.id.now_playing_artist_bottom);
        TextView nowPlayingInfoTitleBottom = (TextView) findViewById(R.id.now_playing_title_bottom);
        Bitmap albumArt = null;
        if (track.getAlbum() != null)
            albumArt = track.getAlbum().getAlbumArt();
        if (nowPlayingInfoAlbumArtTop != null && nowPlayingInfoArtistTop != null && nowPlayingInfoTitleTop != null) {
            if (albumArt != null)
                nowPlayingInfoAlbumArtTop.setImageBitmap(albumArt);
            else
                nowPlayingInfoAlbumArtTop.setImageDrawable(getResources().getDrawable(
                        R.drawable.no_album_art_placeholder));
            nowPlayingInfoArtistTop.setText(track.getArtist().toString());
            nowPlayingInfoTitleTop.setText(track.getName());
            nowPlayingInfoTop.setClickable(true);
        }
        if (nowPlayingInfoAlbumArtBottom != null && nowPlayingInfoArtistBottom != null
                && nowPlayingInfoTitleBottom != null) {
            if (albumArt != null)
                nowPlayingInfoAlbumArtBottom.setImageBitmap(albumArt);
            else
                nowPlayingInfoAlbumArtBottom.setImageDrawable(getResources().getDrawable(
                        R.drawable.no_album_art_placeholder));
            nowPlayingInfoArtistBottom.setText(track.getArtist().toString());
            nowPlayingInfoTitleBottom.setText(track.getName());
            nowPlayingBottom.setClickable(true);
        }
    }

    /**
     * Make the playback info panel invisible, if device is in landscape-mode.
     * Otherwise make it visible
     * 
     */
    public void refreshNowPlayingBarVisibility() {
        LinearLayout nowPlayingInfoBottom = (LinearLayout) findViewById(R.id.now_playing_bottom_bar);
        final ActionBar actionBar = getSupportActionBar();
        if (nowPlayingInfoBottom != null) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                nowPlayingInfoBottom.setVisibility(LinearLayout.GONE);
                actionBar.setDisplayShowCustomEnabled(true);
            } else if (mSearchModeEnabled) {
                nowPlayingInfoBottom.setVisibility(LinearLayout.GONE);
            } else {
                nowPlayingInfoBottom.setVisibility(LinearLayout.VISIBLE);
                actionBar.setDisplayShowCustomEnabled(false);
            }
        }
    }

    /**
     * Returns this Activities current Collection.
     * 
     * @return the current Collection in this Activity.
     */
    public Collection getCollection() {
        return mCollection;
    }

    /**
     * @return the mTabsAdapter
     */
    public TabsAdapter getTabsAdapter() {
        return mTabsAdapter;
    }

    /* 
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        if (mSearchModeEnabled)
            onSearchButtonClicked();
        if (!mTabsAdapter.back()) {
            super.onBackPressed();
        }
    }

    /**
     * Called when the back button is pressed
     * @param view */
    public void onBackPressed(View view) {
        this.onBackPressed();
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.view.MenuItem.OnActionExpandListener#onMenuItemActionExpand(com.actionbarsherlock.view.MenuItem)
     */
    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.view.MenuItem.OnActionExpandListener#onMenuItemActionCollapse(com.actionbarsherlock.view.MenuItem)
     */
    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        if (mSearchModeEnabled)
            onBackPressed();
        return true;
    }
}
