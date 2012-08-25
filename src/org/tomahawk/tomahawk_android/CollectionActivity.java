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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.LayoutParams;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class CollectionActivity extends SherlockFragmentActivity {

    public static final String COLLECTION_ID_EXTRA = "collection_id";
    public static final int SEARCH_OPTION_ID = 0;

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private Collection mCollection;

    private NewTrackBroadcastReceiver mNewTrackBroadcastReceiver;

    private class NewTrackBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackService.BROADCAST_NEWTRACK)) {
                setPlaybackInfo(((TomahawkApp) getApplication()).getPlaybackService().getCurrentTrack());
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
        mViewPager = (ViewPager) findViewById(R.id.view_pager);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.removeAllTabs();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(false);
        View actionBarPlaybackTop = getLayoutInflater().inflate(R.layout.playback_info_top, null);
        actionBar.setCustomView(actionBarPlaybackTop);

        mTabsAdapter = new TabsAdapter(this, getSupportFragmentManager(), mViewPager);
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
        if (((TomahawkApp) getApplication()).getPlaybackService() != null)
            setPlaybackInfo(((TomahawkApp) getApplication()).getPlaybackService().getCurrentTrack());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.actionbarsherlock.app.SherlockActivity#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

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
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            menu.add(0, SEARCH_OPTION_ID, 0, "Search").setIcon(R.drawable.ic_action_search).setActionView(
                    R.layout.collapsible_edittext).setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH && ViewConfiguration.get(
                getApplicationContext()).hasPermanentMenuKey()) || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            overflowMenuButton.setVisibility(ImageButton.GONE);
            overflowMenuButton.setClickable(false);
            overflowMenuButton.setLayoutParams(new LayoutParams(0, 0));
        }
        refreshPlaybackInfoVisibility();
        if (((TomahawkApp) getApplication()).getPlaybackService() != null)
            setPlaybackInfo(((TomahawkApp) getApplication()).getPlaybackService().getCurrentTrack());

        return super.onPrepareOptionsMenu(menu);
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

        invalidateOptionsMenu();
    }

    /**
     * Called when the playbackInfo is clicked
     * 
     */
    public void onPlaybackInfoClicked(View view) {
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
    public void setPlaybackInfo(Track track) {
        RelativeLayout playbackInfoTop = (RelativeLayout) findViewById(R.id.playback_info_top);
        LinearLayout playbackInfoBottom = (LinearLayout) findViewById(R.id.playback_info_bottom);
        if (playbackInfoTop != null)
            playbackInfoTop.setClickable(false);
        if (playbackInfoBottom != null)
            playbackInfoBottom.setClickable(false);

        if (track == null)
            return;

        ImageView playbackInfoAlbumArtTop = (ImageView) findViewById(R.id.playback_info_album_art_top);
        TextView playbackInfoArtistTop = (TextView) findViewById(R.id.playback_info_artist_top);
        TextView playbackInfoTitleTop = (TextView) findViewById(R.id.playback_info_title_top);
        ImageView playbackInfoAlbumArtBottom = (ImageView) findViewById(R.id.playback_info_album_art_bottom);
        TextView playbackInfoArtistBottom = (TextView) findViewById(R.id.playback_info_artist_bottom);
        TextView playbackInfoTitleBottom = (TextView) findViewById(R.id.playback_info_title_bottom);
        Bitmap albumArt = null;
        if (track.getAlbum() != null)
            albumArt = track.getAlbum().getAlbumArt();
        if (playbackInfoAlbumArtTop != null && playbackInfoArtistTop != null && playbackInfoTitleTop != null) {
            if (albumArt != null)
                playbackInfoAlbumArtTop.setImageBitmap(albumArt);
            else
                playbackInfoAlbumArtTop.setImageDrawable(getResources().getDrawable(R.drawable.no_album_art_placeholder));
            playbackInfoArtistTop.setText(track.getArtist().toString());
            playbackInfoTitleTop.setText(track.getName());
            playbackInfoTop.setClickable(true);
        }
        if (playbackInfoAlbumArtBottom != null && playbackInfoArtistBottom != null
                && playbackInfoTitleBottom != null) {
            if (albumArt != null)
                playbackInfoAlbumArtBottom.setImageBitmap(albumArt);
            else
                playbackInfoAlbumArtBottom.setImageDrawable(getResources().getDrawable(R.drawable.no_album_art_placeholder));
            playbackInfoArtistBottom.setText(track.getArtist().toString());
            playbackInfoTitleBottom.setText(track.getName());
            playbackInfoBottom.setClickable(true);
        }
    }

    /**
     * Make the playback info panel invisible, if device is in landscape-mode.
     * Otherwise make it visible
     * 
     */
    public void refreshPlaybackInfoVisibility() {
        LinearLayout fakeSplitActionBar = (LinearLayout) findViewById(R.id.fake_split_action_bar);
        final ActionBar actionBar = getSupportActionBar();
        if (fakeSplitActionBar != null) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                fakeSplitActionBar.setVisibility(LinearLayout.GONE);
                actionBar.setDisplayShowCustomEnabled(true);
            } else {
                fakeSplitActionBar.setVisibility(LinearLayout.VISIBLE);
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
        if (!mTabsAdapter.back()) {
            super.onBackPressed();
        }
    }
    
    public void onBackPressed(View view){
        this.onBackPressed();
    }

}
