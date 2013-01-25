/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
 *   Copyright 2012, Hugo Lindström <hugolm84@gmail.com>
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.libtomahawk.audio;

import java.io.IOException;
import java.util.ArrayList;

import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.Track;
import org.tomahawk.libtomahawk.UserCollection;
import org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceConnection;
import org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceConnection.PlaybackServiceConnectionListener;
import org.tomahawk.libtomahawk.playlist.AlbumPlaylist;
import org.tomahawk.libtomahawk.playlist.ArtistPlaylist;
import org.tomahawk.libtomahawk.playlist.CollectionPlaylist;
import org.tomahawk.libtomahawk.playlist.Playlist;
import org.tomahawk.tomahawk_android.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class PlaybackActivity extends TomahawkTabsActivity implements PlaybackServiceConnectionListener,
        Handler.Callback {

    private PlaybackFragment mPlaybackFragment;
    private PlaybackPlaylistFragment mPlaybackPlaylistFragment;
    private TabsAdapter mTabsAdapter;
    private PlaybackDirectionalViewPager mPlaybackDirectionalViewPager;

    private Drawable mProgressDrawable;
    private Handler mAnimationHandler = new Handler(this);
    private static final int MSG_UPDATE_ANIMATION = 0x20;

    private PlaybackService mPlaybackService;
    /** Allow communication to the PlaybackService. */
    private PlaybackServiceConnection mPlaybackServiceConnection = new PlaybackServiceConnection(this);

    private PlaybackServiceBroadcastReceiver mPlaybackServiceBroadcastReceiver;

    /** Identifier for passing a Track as an extra in an Intent. */
    public static final String PLAYLIST_EXTRA = "playlist_extra";

    public static final String PLAYLIST_ALBUM_ID = "playlist_album_id";

    public static final String PLAYLIST_ARTIST_ID = "playlist_artist_id";

    public static final String PLAYLIST_TRACK_ID = "playlist_track_id";

    public static final String PLAYLIST_COLLECTION_ID = "playlist_collection_id";

    public static final String PLAYLIST_PLAYLIST_ID = "playlist_playlist_id";

    public static final String PLAYBACK_ID_STOREDBACKSTACK = "playback_id_storedbackstack";

    private class PlaybackServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ensureFragmentReferences();
            if (intent.getAction().equals(PlaybackService.BROADCAST_NEWTRACK)) {
                if (mPlaybackFragment != null)
                    mPlaybackFragment.onTrackChanged();
                if (mPlaybackPlaylistFragment != null)
                    mPlaybackPlaylistFragment.onTrackChanged();
                startLoadingAnimation();
            }
            if (intent.getAction().equals(PlaybackService.BROADCAST_PLAYLISTCHANGED)) {
                if (mPlaybackFragment != null)
                    mPlaybackFragment.onPlaylistChanged();
                if (mPlaybackPlaylistFragment != null)
                    mPlaybackPlaylistFragment.onPlaylistChanged();
            }
            if (intent.getAction().equals(PlaybackService.BROADCAST_PLAYSTATECHANGED)) {
                if (mPlaybackFragment != null)
                    mPlaybackFragment.onPlaystateChanged();
                if (mPlaybackPlaylistFragment != null)
                    mPlaybackPlaylistFragment.onPlaystateChanged();
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
        setContentView(R.layout.playback_activity);

        final ActionBar bar = getSupportActionBar();
        bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        //Set up the pager
        mPlaybackDirectionalViewPager = (PlaybackDirectionalViewPager) findViewById(R.id.playbackdirectionalViewPager);
        mTabsAdapter = new TabsAdapter(this, getSupportFragmentManager(), mPlaybackDirectionalViewPager, false);

        if (savedInstanceState == null) {
            mTabsAdapter.addRootToTab(PlaybackFragment.class);
            mTabsAdapter.addRootToTab(PlaybackPlaylistFragment.class);
        } else {
            ArrayList<TabsAdapter.TabHolder> tabHolderStack = (ArrayList<TabsAdapter.TabHolder>) savedInstanceState.getSerializable(PLAYBACK_ID_STOREDBACKSTACK);
            if (tabHolderStack != null && tabHolderStack.size() > 0)
                mTabsAdapter.setBackStack(tabHolderStack);
            else {
                mTabsAdapter.addRootToTab(PlaybackFragment.class);
                mTabsAdapter.addRootToTab(PlaybackPlaylistFragment.class);
            }
        }
        mTabsAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putSerializable(PLAYBACK_ID_STOREDBACKSTACK, mTabsAdapter.getBackStack());
        super.onSaveInstanceState(bundle);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        mProgressDrawable = getResources().getDrawable(R.drawable.progress_indeterminate_tomahawk);

        Intent playbackIntent = new Intent(this, PlaybackService.class);
        startService(playbackIntent);
        bindService(playbackIntent, mPlaybackServiceConnection, Context.BIND_AUTO_CREATE);

        if (mPlaybackServiceBroadcastReceiver == null)
            mPlaybackServiceBroadcastReceiver = new PlaybackServiceBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(PlaybackService.BROADCAST_NEWTRACK);
        registerReceiver(mPlaybackServiceBroadcastReceiver, intentFilter);
        intentFilter = new IntentFilter(PlaybackService.BROADCAST_PLAYLISTCHANGED);
        registerReceiver(mPlaybackServiceBroadcastReceiver, intentFilter);
        intentFilter = new IntentFilter(PlaybackService.BROADCAST_PLAYSTATECHANGED);
        registerReceiver(mPlaybackServiceBroadcastReceiver, intentFilter);

        onTabsAdapterReady();
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

        if (mPlaybackServiceBroadcastReceiver != null) {
            unregisterReceiver(mPlaybackServiceBroadcastReceiver);
            mPlaybackServiceBroadcastReceiver = null;
        }
    }

    /*
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockActivity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.actionbarsherlock.app.SherlockActivity#onOptionsItemSelected(android
     * .view.MenuItem)
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

    public void setPlaybackService(PlaybackService ps) {
        mPlaybackService = ps;
    }

    /**
     * Called when the playback service is ready.
     */
    @Override
    public void onPlaybackServiceReady() {
        if (getIntent().hasExtra(PLAYLIST_EXTRA)) {

            Bundle playlistBundle = getIntent().getBundleExtra(PLAYLIST_EXTRA);
            getIntent().removeExtra(PLAYLIST_EXTRA);

            long trackid = playlistBundle.getLong(PLAYLIST_TRACK_ID);

            Playlist playlist = null;
            if (playlistBundle.containsKey(PLAYLIST_ALBUM_ID)) {
                long albumid = playlistBundle.getLong(PLAYLIST_ALBUM_ID);
                playlist = AlbumPlaylist.fromAlbum(Album.get(albumid), Track.get(trackid));
            } else if (playlistBundle.containsKey(PLAYLIST_COLLECTION_ID)) {
                int collid = playlistBundle.getInt(PLAYLIST_COLLECTION_ID);
                TomahawkApp app = (TomahawkApp) getApplication();
                playlist = CollectionPlaylist.fromCollection(app.getSourceList().getCollectionFromId(collid),
                        Track.get(trackid));
            } else if (playlistBundle.containsKey(PLAYLIST_ARTIST_ID)) {
                long artistid = playlistBundle.getLong(PLAYLIST_ARTIST_ID);
                playlist = ArtistPlaylist.fromArtist(Artist.get(artistid));
            } else if (playlistBundle.containsKey(PLAYLIST_PLAYLIST_ID)) {
                long playlistid = playlistBundle.getLong(PLAYLIST_PLAYLIST_ID);
                TomahawkApp app = (TomahawkApp) getApplication();
                playlist = app.getSourceList().getCollectionFromId(UserCollection.Id).getPlaylistById(playlistid);
                playlist.setCurrentTrack(Track.get(trackid));
            }
            try {
                mPlaybackService.setCurrentPlaylist(playlist);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        onTabsAdapterReady();
    }

    /**
     * Called when the play/pause button is clicked.
     *
     * @param view
     */
    public void onPlayPauseClicked(View view) {
        if (mPlaybackFragment != null)
            mPlaybackFragment.onPlayPauseClicked();
    }

    /**
     * Called when the next button is clicked.
     *
     * @param view
     */
    public void onNextClicked(View view) {
        if (mPlaybackFragment != null)
            mPlaybackFragment.onNextClicked();
    }

    /**
     * Called when the previous button is clicked.
     *
     * @param view
     */
    public void onPreviousClicked(View view) {
        if (mPlaybackFragment != null)
            mPlaybackFragment.onPreviousClicked();
    }

    /**
     * Called when the shuffle button is clicked.
     *
     * @param view
     */
    public void onShuffleClicked(View view) {
        if (mPlaybackFragment != null)
            mPlaybackFragment.onShuffleClicked();
    }

    /**
     * Called when the repeat button is clicked.
     *
     * @param view
     */
    public void onRepeatClicked(View view) {
        if (mPlaybackFragment != null)
            mPlaybackFragment.onRepeatClicked();
    }

    /**
     * Called when the showPlaylistBar is clicked inside the PlaybackFragment
     * @param view
     */
    public void onShowPlaylistClicked(View view) {
        mPlaybackDirectionalViewPager.setCurrentItem(1, true);
    }

    /**
     * Called when the showPlaylistBar is clicked inside the PlaybackPlaylistFragment
     * @param view
     */
    public void onShowPlaybackClicked(View view) {
        mPlaybackDirectionalViewPager.setCurrentItem(0, true);
    }

    /**
     * Called when the TabsAdapter has finished instantiating the fragments
     */
    @Override
    public void onTabsAdapterReady() {
        ensureFragmentReferences();
        if (mPlaybackService != null) {
            if (mPlaybackFragment != null) {
                mPlaybackFragment.setPlaybackService(mPlaybackService);
            }
            if (mPlaybackPlaylistFragment != null) {
                mPlaybackPlaylistFragment.setPlaybackService(mPlaybackService);
            }
        }
    }

    /**
     * Make sure to get all FragmentReferences from the TabsAdapter
     */
    public void ensureFragmentReferences() {
        if (mPlaybackFragment == null)
            mPlaybackFragment = (PlaybackFragment) mTabsAdapter.getFragmentOnTop(0);
        if (mPlaybackPlaylistFragment == null)
            mPlaybackPlaylistFragment = (PlaybackPlaylistFragment) mTabsAdapter.getFragmentOnTop(1);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        case MSG_UPDATE_ANIMATION:
            if (mPlaybackService.isPreparing()) {
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
        mAnimationHandler.sendEmptyMessageDelayed(MSG_UPDATE_ANIMATION, 200);
    }

    public void stopLoadingAnimation() {
        mAnimationHandler.removeMessages(MSG_UPDATE_ANIMATION);
        getSupportActionBar().setLogo(R.drawable.ic_launcher);
    }
}
