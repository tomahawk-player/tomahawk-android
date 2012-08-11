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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
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
        actionBar.setDisplayShowCustomEnabled(true);
        View actionBarPlaybackTop = getLayoutInflater().inflate(R.layout.playback_info_top, null);
        actionBar.setCustomView(actionBarPlaybackTop);

        mTabsAdapter = new TabsAdapter(this, mViewPager);
        mTabsAdapter.addTab(actionBar.newTab().setText(R.string.title_browse_fragment),
                ArtistFragment.class, null);
        mTabsAdapter.addTab(actionBar.newTab().setText(R.string.title_mymusic_fragment),
                AlbumFragment.class, null);
        mTabsAdapter.addTab(actionBar.newTab().setText(R.string.title_friends_fragment),
                TrackFragment.class, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.FragmentActivity#onStart()
     */
    @Override
    public void onStart() {
        super.onStart();

        refreshPlaybackInfoVisibility();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.FragmentActivity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        if (getIntent().hasExtra(COLLECTION_ID_EXTRA)) {
            SourceList sl = ((TomahawkApp) getApplication()).getSourceList();
            mCollection = sl.getCollectionFromId(getIntent().getIntExtra(COLLECTION_ID_EXTRA, 0));
        }
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
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        final ActionBar mActionBar = getSupportActionBar();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            menu.add(0, SEARCH_OPTION_ID, 0, "Search").setIcon(R.drawable.ic_action_search).setActionView(
                    R.layout.collapsible_edittext).setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            mActionBar.setDisplayShowCustomEnabled(true);
        } else
            mActionBar.setDisplayShowCustomEnabled(false);

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
        if (((TomahawkApp) getApplication()).getPlaybackService() != null)
            setPlaybackInfo(((TomahawkApp) getApplication()).getPlaybackService().getCurrentTrack());
        refreshPlaybackInfoVisibility();

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
     * @param mTrack
     */
    public void setPlaybackInfo(Track mTrack) {
        RelativeLayout playbackInfoTop = (RelativeLayout) findViewById(R.id.playback_info_top);
        LinearLayout playbackInfoBottom = (LinearLayout) findViewById(R.id.playback_info_bottom);
        if (playbackInfoTop != null && playbackInfoBottom != null) {
            playbackInfoTop.setClickable(false);
            playbackInfoBottom.setClickable(false);
        }
        if (mTrack != null) {
            ImageView playbackInfoAlbumArtTop = (ImageView) findViewById(R.id.playback_info_album_art_top);
            TextView playbackInfoArtistTop = (TextView) findViewById(R.id.playback_info_artist_top);
            TextView playbackInfoTitleTop = (TextView) findViewById(R.id.playback_info_title_top);
            ImageView playbackInfoAlbumArtBottom = (ImageView) findViewById(R.id.playback_info_album_art_bottom);
            TextView playbackInfoArtistBottom = (TextView) findViewById(R.id.playback_info_artist_bottom);
            TextView playbackInfoTitleBottom = (TextView) findViewById(R.id.playback_info_title_bottom);
            Bitmap albumArt = null;
            if (mTrack.getAlbum() != null)
                albumArt = mTrack.getAlbum().getAlbumArt();
            if (playbackInfoAlbumArtTop != null && playbackInfoArtistTop != null && playbackInfoTitleTop != null) {
                if (albumArt != null)
                    playbackInfoAlbumArtTop.setImageBitmap(albumArt);
                else
                    playbackInfoAlbumArtTop.setImageDrawable(getResources().getDrawable(
                            R.drawable.no_album_art_placeholder));
                playbackInfoArtistTop.setText(mTrack.getArtist().toString());
                playbackInfoTitleTop.setText(mTrack.getTitle());
                playbackInfoTop.setClickable(true);
            }
            if (playbackInfoAlbumArtBottom != null && playbackInfoArtistBottom != null && playbackInfoTitleBottom != null) {
                if (albumArt != null)
                    playbackInfoAlbumArtBottom.setImageBitmap(albumArt);
                else
                    playbackInfoAlbumArtBottom.setImageDrawable(getResources().getDrawable(
                            R.drawable.no_album_art_placeholder));
                playbackInfoArtistBottom.setText(mTrack.getArtist().toString());
                playbackInfoTitleBottom.setText(mTrack.getTitle());
                playbackInfoBottom.setClickable(true);
            }
        } else
            return;
    }

    /**
     * Make the playback info panel invisible, if device is in landscape-mode. Otherwise make it visible
     *
     */
    public void refreshPlaybackInfoVisibility() {
        LinearLayout fakeSplitActionBar = (LinearLayout) findViewById(R.id.fake_split_action_bar);
        if (fakeSplitActionBar != null) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                fakeSplitActionBar.setVisibility(LinearLayout.GONE);
            else
                fakeSplitActionBar.setVisibility(LinearLayout.VISIBLE);
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
}
