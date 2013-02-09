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
import java.util.List;

import org.tomahawk.libtomahawk.*;
import org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceConnection;
import org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceConnection.PlaybackServiceConnectionListener;
import org.tomahawk.libtomahawk.playlist.AlbumPlaylist;
import org.tomahawk.libtomahawk.playlist.ArtistPlaylist;
import org.tomahawk.libtomahawk.playlist.CollectionPlaylist;
import org.tomahawk.libtomahawk.playlist.Playlist;
import org.tomahawk.libtomahawk.resolver.TomahawkUtils;
import org.tomahawk.tomahawk_android.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.FrameLayout;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.emilsjolander.components.stickylistheaders.StickyListHeadersListView;

public class PlaybackActivity extends SherlockFragmentActivity implements PlaybackServiceConnectionListener,
        Handler.Callback, AdapterView.OnItemClickListener, StickyListHeadersListView.OnHeaderClickListener,
        ViewTreeObserver.OnGlobalLayoutListener {

    public static final String TAG = PlaybackActivity.class.getName();

    private int mFragmentLayoutHeight;

    private PlaybackFragment mPlaybackFragment;
    private Playlist mPlaylist;
    private TomahawkListAdapter mTomahawkListAdapter;
    private TomahawkStickyListHeadersListView mList;

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

    private class PlaybackServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackService.BROADCAST_NEWTRACK)) {
                if (mPlaybackFragment != null)
                    mPlaybackFragment.onTrackChanged();
                onTrackChanged();
                startLoadingAnimation();
            }
            if (intent.getAction().equals(PlaybackService.BROADCAST_PLAYLISTCHANGED)) {
                if (mPlaybackFragment != null)
                    mPlaybackFragment.onPlaylistChanged();
                onPlaylistChanged();
            }
            if (intent.getAction().equals(PlaybackService.BROADCAST_PLAYSTATECHANGED)) {
                if (mPlaybackFragment != null)
                    mPlaybackFragment.onPlaystateChanged();
                onPlaystateChanged();
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
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(this);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
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
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        initAdapter();

        mProgressDrawable = getResources().getDrawable(R.drawable.progress_indeterminate_tomahawk);

        if (mPlaybackServiceBroadcastReceiver == null)
            mPlaybackServiceBroadcastReceiver = new PlaybackServiceBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(PlaybackService.BROADCAST_NEWTRACK);
        registerReceiver(mPlaybackServiceBroadcastReceiver, intentFilter);
        intentFilter = new IntentFilter(PlaybackService.BROADCAST_PLAYLISTCHANGED);
        registerReceiver(mPlaybackServiceBroadcastReceiver, intentFilter);
        intentFilter = new IntentFilter(PlaybackService.BROADCAST_PLAYSTATECHANGED);
        registerReceiver(mPlaybackServiceBroadcastReceiver, intentFilter);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.actionbarsherlock.app.SherlockActivity#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

        if (mPlaybackServiceBroadcastReceiver != null) {
            unregisterReceiver(mPlaybackServiceBroadcastReceiver);
            mPlaybackServiceBroadcastReceiver = null;
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
        super.onSaveInstanceState(bundle);
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
                Intent collectionIntent = getIntent(this, CollectionActivity.class);
                startActivity(collectionIntent);
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        Object obj = mTomahawkListAdapter.getItem(idx - 1);
        if (obj instanceof Track) {
            if (mPlaylist.getPosition() == idx - 1)
                mPlaybackService.playPause();
            else {
                try {
                    mPlaybackService.setCurrentTrack(mPlaylist.getTrackAtPos(idx - 1));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onHeaderClick(StickyListHeadersListView l, View header, int itemPosition, long headerId, boolean currentlySticky) {
        ensureList();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mList.smoothScrollToPositionFromTop(0, 0, 200);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            int firstVisible = mList.getFirstVisiblePosition();
            int lastVisible = mList.getLastVisiblePosition();
            if (0 < firstVisible)
                mList.smoothScrollToPosition(0);
            else
                mList.smoothScrollToPosition(0 + lastVisible - firstVisible - 2);
        } else {
            mList.setSelectionFromTop(0, 0);
        }
    }

    @Override
    public void onGlobalLayout() {
        final View activityRootView = getWindow().getDecorView().findViewById(android.R.id.content);
        mFragmentLayoutHeight = activityRootView.getHeight() - (int) TomahawkUtils.convertDpToPixel(32f, this);
        mPlaybackFragment = (PlaybackFragment) getSupportFragmentManager().findFragmentById(R.id.playbackFragment);
        if (mPlaybackFragment != null && mPlaybackFragment.getView() != null)
            mPlaybackFragment.getView().setLayoutParams(
                    new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, mFragmentLayoutHeight));
        //is softkeyboard shown hack
        int heightdiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
        if (heightdiff < 200)
            getWindow().getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
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

    @Override
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
                if (playlist != null)
                    playlist.setCurrentTrack(Track.get(trackid));
            }
            if (playlist != null) {
                try {
                    mPlaybackService.setCurrentPlaylist(playlist);
                    mPlaylist = mPlaybackService.getCurrentPlaylist();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (mPlaybackFragment != null)
            mPlaybackFragment.setPlaybackService(mPlaybackService);
        onPlaylistChanged();
    }

    private void initAdapter() {
        if (mPlaybackService != null)
            mPlaylist = mPlaybackService.getCurrentPlaylist();
        if (mPlaylist != null) {
            List<TomahawkBaseAdapter.TomahawkListItem> tracks = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
            tracks.addAll(mPlaylist.getTracks());
            mTomahawkListAdapter = new TomahawkListAdapter(this, R.layout.double_line_list_item_with_playstate_image,
                    R.id.double_line_list_imageview, R.id.double_line_list_textview, R.id.double_line_list_textview2,
                    tracks);
            mTomahawkListAdapter.setShowHighlightingAndPlaystate(true);
            mTomahawkListAdapter.setHighlightedItem(mPlaylist.getPosition());
            mTomahawkListAdapter.setHighlightedItemIsPlaying(mPlaybackService.isPlaying());
            ensureList();
            mList.setOnItemClickListener(this);
            mList.setOnHeaderClickListener(this);
            if (mList.getHeaderViewsCount() == 0) {
                mPlaybackFragment = (PlaybackFragment) getSupportFragmentManager().findFragmentById(
                        R.id.playbackFragment);
                View headerView;
                if (mPlaybackFragment == null || (mPlaybackFragment != null && mPlaybackFragment.getView() == null)) {
                    headerView = getLayoutInflater().inflate(R.layout.fragment_container_list_item, null);
                    mPlaybackFragment = (PlaybackFragment) getSupportFragmentManager().findFragmentById(
                            R.id.playbackFragment);
                } else {
                    headerView = (View) mPlaybackFragment.getView().getParent();
                }
                mPlaybackFragment.getView().setLayoutParams(
                        new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, mFragmentLayoutHeight));
                mPlaybackFragment.setPlaybackService(mPlaybackService);
                mList.addHeaderView(headerView);
            }
            mPlaybackFragment.init();
            mList.setAdapter(mTomahawkListAdapter);
        }
    }

    private void ensureList() {
        View rawListView = findViewById(R.id.listview);
        if (!(rawListView instanceof TomahawkStickyListHeadersListView)) {
            if (rawListView == null) {
                throw new RuntimeException("Your content must have a ListView whose id attribute is "
                        + "'R.id.listview'");
            }
            throw new RuntimeException("Content has view with id attribute 'R.id.listview' "
                    + "that is not a ListView class");
        }
        mList = (TomahawkStickyListHeadersListView) rawListView;
    }

    /**
     * Called when the track has Changed inside our PlaybackService
     */
    public void onTrackChanged() {
        if (mPlaylist != null) {
            mTomahawkListAdapter.setHighlightedItem(mPlaylist.getPosition());
            mTomahawkListAdapter.setHighlightedItemIsPlaying(mPlaybackService.isPlaying());
            mTomahawkListAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Called when the playState (playing or paused) has Changed inside our PlaybackService
     */
    public void onPlaystateChanged() {
        if (mPlaylist != null) {
            mTomahawkListAdapter.setHighlightedItem(mPlaylist.getPosition());
            mTomahawkListAdapter.setHighlightedItemIsPlaying(mPlaybackService.isPlaying());
            mTomahawkListAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Called when the playlist has Changed inside our PlaybackService
     */
    public void onPlaylistChanged() {
        if (mTomahawkListAdapter != null) {
            mPlaylist = mPlaybackService.getCurrentPlaylist();
            if (mPlaylist != null) {
                ArrayList<TomahawkBaseAdapter.TomahawkListItem> tracks = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
                tracks.addAll(mPlaylist.getTracks());
                mTomahawkListAdapter.setListWithIndex(0, tracks);
                mTomahawkListAdapter.setHighlightedItem(mPlaylist.getPosition());
                mTomahawkListAdapter.setHighlightedItemIsPlaying(mPlaybackService.isPlaying());
                mTomahawkListAdapter.notifyDataSetChanged();
            }
        } else {
            initAdapter();
        }
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
}
