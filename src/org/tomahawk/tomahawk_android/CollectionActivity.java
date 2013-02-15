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

import java.util.ArrayList;

import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.SourceList;
import org.tomahawk.libtomahawk.Track;
import org.tomahawk.libtomahawk.audio.PlaybackActivity;
import org.tomahawk.libtomahawk.audio.PlaybackService;
import org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceConnection;
import org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceConnection.PlaybackServiceConnectionListener;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class CollectionActivity extends SherlockFragmentActivity implements PlaybackServiceConnectionListener {

    public static final String COLLECTION_ID_EXTRA = "collection_id";
    public static final String COLLECTION_ID_ALBUM = "collection_album_id";
    public static final String COLLECTION_ID_ARTIST = "collection_artist_id";
    public static final String COLLECTION_ID_STOREDBACKSTACK = "collection_id_storedbackstack";

    protected static final int LOCAL_COLLECTION_TAB_POSITION = 0;

    private PlaybackService mPlaybackService;
    private TabsAdapter mTabsAdapter;
    private Collection mCollection;
    private View mNowPlayingView;

    private PlaybackServiceConnection mPlaybackServiceConnection = new PlaybackServiceConnection(this);
    private CollectionActivityBroadcastReceiver mCollectionActivityBroadcastReceiver;

    private class CollectionActivityBroadcastReceiver extends BroadcastReceiver {

        /* 
         * (non-Javadoc)
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackService.BROADCAST_NEWTRACK)) {
                if (mPlaybackService != null)
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

        View view = getLayoutInflater().inflate(R.layout.collection_activity, null);
        setContentView(view);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.removeAllTabs();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);

        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        mTabsAdapter = new TabsAdapter(this, getSupportFragmentManager(), viewPager, true);
        mTabsAdapter.addTab(actionBar.newTab().setText(R.string.localcollectionactivity_title_string));
        if (savedInstanceState == null) {
            mTabsAdapter.addRootToTab(LocalCollectionFragment.class);
        } else {
            ArrayList<TabsAdapter.TabHolder> fragmentStateHolderStack = (ArrayList<TabsAdapter.TabHolder>) savedInstanceState.getSerializable(COLLECTION_ID_STOREDBACKSTACK);
            if (fragmentStateHolderStack != null && fragmentStateHolderStack.size() > 0)
                mTabsAdapter.setBackStack(fragmentStateHolderStack);
            else {
                mTabsAdapter.addRootToTab(LocalCollectionFragment.class);
            }
        }
        Intent intent = getIntent();
        if (intent.hasExtra(COLLECTION_ID_ALBUM)) {
            Long albumId = intent.getLongExtra(COLLECTION_ID_ALBUM, 0);
            intent.removeExtra(COLLECTION_ID_ALBUM);
            getTabsAdapter().addFragmentToBackStack(LOCAL_COLLECTION_TAB_POSITION, TracksFragment.class, albumId,
                    TomahawkFragment.TOMAHAWK_ALBUM_ID);
        } else if (intent.hasExtra(COLLECTION_ID_ARTIST)) {
            Long artistId = intent.getLongExtra(COLLECTION_ID_ARTIST, 0);
            intent.removeExtra(COLLECTION_ID_ARTIST);
            getTabsAdapter().addFragmentToBackStack(LOCAL_COLLECTION_TAB_POSITION, AlbumsFragment.class, artistId,
                    TomahawkFragment.TOMAHAWK_ARTIST_ID);
        }
        mTabsAdapter.notifyDataSetChanged();
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

        SourceList sl = ((TomahawkApp) getApplication()).getSourceList();
        Intent intent = getIntent();
        mCollection = sl.getCollectionFromId(intent.getIntExtra(COLLECTION_ID_EXTRA, 0));
        if (mPlaybackService != null)
            setNowPlayingInfo(mPlaybackService.getCurrentTrack());

        if (mCollectionActivityBroadcastReceiver == null)
            mCollectionActivityBroadcastReceiver = new CollectionActivityBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(PlaybackService.BROADCAST_NEWTRACK);
        registerReceiver(mCollectionActivityBroadcastReceiver, intentFilter);

        if (intent.hasExtra(COLLECTION_ID_ALBUM)) {
            Long albumId = intent.getLongExtra(COLLECTION_ID_ALBUM, 0);
            intent.removeExtra(COLLECTION_ID_ALBUM);
            getTabsAdapter().addFragmentToBackStack(LOCAL_COLLECTION_TAB_POSITION, TracksFragment.class, albumId,
                    TomahawkFragment.TOMAHAWK_ALBUM_ID);
        } else if (intent.hasExtra(COLLECTION_ID_ARTIST)) {
            Long artistId = intent.getLongExtra(COLLECTION_ID_ARTIST, 0);
            intent.removeExtra(COLLECTION_ID_ARTIST);
            getTabsAdapter().addFragmentToBackStack(LOCAL_COLLECTION_TAB_POSITION, AlbumsFragment.class, artistId,
                    TomahawkFragment.TOMAHAWK_ARTIST_ID);
        }
        mTabsAdapter.notifyDataSetChanged();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.actionbarsherlock.app.SherlockActivity#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

        if (mCollectionActivityBroadcastReceiver != null) {
            unregisterReceiver(mCollectionActivityBroadcastReceiver);
            mCollectionActivityBroadcastReceiver = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mPlaybackService != null)
            unbindService(mPlaybackServiceConnection);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putSerializable(COLLECTION_ID_STOREDBACKSTACK, getTabsAdapter().getBackStack());
        super.onSaveInstanceState(bundle);
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
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        RelativeLayout relativeLayout = (RelativeLayout) menu.findItem(R.id.now_playing_layout_item).getActionView();
        mNowPlayingView = getLayoutInflater().inflate(R.layout.now_playing, null);
        relativeLayout.addView(mNowPlayingView);
        if (mPlaybackService != null)
            setNowPlayingInfo(mPlaybackService.getCurrentTrack());

        return true;
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null) {
            if (item.getItemId() == R.id.action_search_item) {
                Intent searchIntent = getIntent(this, SearchableActivity.class);
                startActivity(searchIntent);
                return true;
            } else if (item.getItemId() == R.id.action_settings_item) {
                Intent searchIntent = getIntent(this, SettingsActivity.class);
                startActivity(searchIntent);
                return true;
            } else if (item.getItemId() == android.R.id.home) {
                super.onBackPressed();
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

    /* 
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentActivity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
        if (!mTabsAdapter.back()) {
            super.onBackPressed();
        }
    }

    /**
     * Called when the back {@link Button} is pressed
     * @param view */
    public void onBackPressed(View view) {
        onBackPressed();
    }

    public void onBackToRootPressed(View view) {
        getTabsAdapter().backToRoot(mTabsAdapter.getCurrentPosition());
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
     * Sets the playback information
     * 
     * @param track
     */
    public void setNowPlayingInfo(Track track) {
        if (mNowPlayingView == null)
            supportInvalidateOptionsMenu();
        if (mNowPlayingView != null) {
            mNowPlayingView.setClickable(false);

            if (track != null) {
                ImageView nowPlayingInfoAlbumArt = (ImageView) mNowPlayingView.findViewById(R.id.now_playing_album_art);
                TextView nowPlayingInfoArtist = (TextView) mNowPlayingView.findViewById(R.id.now_playing_artist);
                TextView nowPlayingInfoTitle = (TextView) mNowPlayingView.findViewById(R.id.now_playing_title);
                Bitmap albumArt = null;
                if (track.getAlbum() != null)
                    albumArt = track.getAlbum().getAlbumArt();
                if (nowPlayingInfoAlbumArt != null && nowPlayingInfoArtist != null && nowPlayingInfoTitle != null) {
                    if (albumArt != null)
                        nowPlayingInfoAlbumArt.setImageBitmap(albumArt);
                    else
                        nowPlayingInfoAlbumArt.setImageDrawable(getResources().getDrawable(
                                R.drawable.no_album_art_placeholder));
                    nowPlayingInfoArtist.setText(track.getArtist().toString());
                    nowPlayingInfoTitle.setText(track.getName());
                    mNowPlayingView.setClickable(true);
                }
            }
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
     * @return the mTabsAdapter
     */
    public TabsAdapter getTabsAdapter() {
        return mTabsAdapter;
    }
}
