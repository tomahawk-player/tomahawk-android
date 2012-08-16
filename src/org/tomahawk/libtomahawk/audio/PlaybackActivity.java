/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
 *   Copyright 2012, Hugo Lindström <hugolm84@gmail.com>
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
package org.tomahawk.libtomahawk.audio;

import java.io.IOException;

import org.tomahawk.libtomahawk.Track;
import org.tomahawk.libtomahawk.playlist.Playlist;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.CollectionActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class PlaybackActivity extends SherlockActivity {

    private static final String TAG = PlaybackActivity.class.getName();

    private PlaybackService mPlaybackService;
    private PlaybackServiceBroadcastReceiver mPlaybackServiceBroadcastReceiver;

    private AlbumArtViewPager mAlbumArtViewPager;

    private PlaybackSeekBar mPlaybackSeekBar;

    /** Identifier for passing a Track as an extra in an Intent. */
    public static final String PLAYLIST_EXTRA = "playlist";

    private class PlaybackServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackService.BROADCAST_NEWTRACK))
                onTrackChanged();
            if (intent.getAction().equals(PlaybackService.BROADCAST_PLAYLISTCHANGED))
                onPlaylistChanged();
            if (intent.getAction().equals(PlaybackService.BROADCAST_PLAYSTATECHANGED))
                onPlaystateChanged();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        View view = getLayoutInflater().inflate(R.layout.playback_activity, null);
        setContentView(view);

        mPlaybackService = ((TomahawkApp) getApplication()).getPlaybackService();
        mAlbumArtViewPager = (AlbumArtViewPager) findViewById(R.id.album_art_view_pager);
        mAlbumArtViewPager.setPlaybackService(mPlaybackService);

        final ActionBar bar = getSupportActionBar();
        bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayHomeAsUpEnabled(true);

        mPlaybackSeekBar = (PlaybackSeekBar) findViewById(R.id.seekBar_track);
        mPlaybackSeekBar.setPlaybackService(mPlaybackService);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        mPlaybackSeekBar.setTextViewCurrentTime((TextView) findViewById(R.id.textView_currentTime));
        mPlaybackSeekBar.setTextViewCompletionTime((TextView) findViewById(R.id.textView_completionTime));
        if (mPlaybackServiceBroadcastReceiver == null)
            mPlaybackServiceBroadcastReceiver = new PlaybackServiceBroadcastReceiver();

        IntentFilter intentFilter = new IntentFilter(PlaybackService.BROADCAST_NEWTRACK);
        registerReceiver(mPlaybackServiceBroadcastReceiver, intentFilter);
        intentFilter = new IntentFilter(PlaybackService.BROADCAST_PLAYLISTCHANGED);
        registerReceiver(mPlaybackServiceBroadcastReceiver, intentFilter);
        intentFilter = new IntentFilter(PlaybackService.BROADCAST_PLAYSTATECHANGED);
        registerReceiver(mPlaybackServiceBroadcastReceiver, intentFilter);

        if (getIntent().hasExtra(PLAYLIST_EXTRA)) {
            try {
                mPlaybackService.setCurrentPlaylist((Playlist) getIntent().getSerializableExtra(
                        PLAYLIST_EXTRA));
            } catch (IOException e) {
                e.printStackTrace();
            }
            getIntent().removeExtra(PLAYLIST_EXTRA);
        }
        refreshButtonStates();
        refreshActivityTrackInfo();
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.actionbarsherlock.app.SherlockActivity#onCreateOptionsMenu(android
     * .view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

         menu.add(0, CollectionActivity.SEARCH_OPTION_ID, 0, "Search")
         .setIcon(R.drawable.ic_action_search)
         .setActionView(R.layout.collapsible_edittext)
         .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
         | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        return super.onCreateOptionsMenu(menu);
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
        switch (item.getItemId()) {
        case android.R.id.home:
            super.onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Called when the play/pause button is clicked.
     * 
     * @param view
     */
    public void onPlayPauseClicked(View view) {
        Log.d(TAG, "onPlayPauseClicked");
        mPlaybackService.playPause();
        refreshButtonStates();
        mPlaybackSeekBar.updateSeekBarPosition();
    }

    /**
     * Called when the next button is clicked.
     * 
     * @param view
     */
    public void onNextClicked(View view) {
        nextTrack();
    }

    /** play the next track and set the playbutton to pause icon */
    public void nextTrack() {
        mPlaybackService.next();
    }

    /**
     * Called when the previous button is clicked.
     * 
     * @param view
     */
    public void onPreviousClicked(View view) {
        previousTrack();
    }

    /** play the previous track and set the playbutton to pause icon */
    public void previousTrack() {
        mPlaybackService.previous();
    }

    /**
     * Called when the shuffle button is clicked.
     * 
     * @param view
     */
    public void onShuffleClicked(View view) {
        mPlaybackService.getCurrentPlaylist().setShuffled(!mPlaybackService.getCurrentPlaylist().isShuffled());
        onPlaylistChanged();
    }

    /**
     * Called when the repeat button is clicked.
     * 
     * @param view
     */
    public void onRepeatClicked(View view) {
        mPlaybackService.getCurrentPlaylist().setRepeating(
                !mPlaybackService.getCurrentPlaylist().isRepeating());
    }

    /** Called when the PlaybackService signals the current Track has changed. */
    protected void onTrackChanged() {
        refreshActivityTrackInfo();
    }

    /**
     * Called when the PlaybackService signals the current Playlist has changed.
     */
    protected void onPlaylistChanged() {
        mAlbumArtViewPager.updatePlaylist(mPlaybackService.getCurrentPlaylist());
    }

    /**
     * Called when the PlaybackService signals the current Playstate has
     * changed.
     */
    protected void onPlaystateChanged() {
        refreshButtonStates();
    }

    /**
     * Refresh the information in this activity to reflect that of the current
     * Track, if possible (meaning mPlaybackService is not null).
     */
    private void refreshActivityTrackInfo() {
        if (mPlaybackService != null)
            refreshActivityTrackInfo(mPlaybackService.getCurrentTrack());
        else
            refreshActivityTrackInfo(null);
    }

    /**
     * Refresh the information in this activity to reflect that of the given
     * Track.
     */
    private void refreshActivityTrackInfo(Track track) {
        if (track != null) {
            if (mAlbumArtViewPager.isPlaylistNull())
                onPlaylistChanged();
            if (!mAlbumArtViewPager.isSwiped()) {
                mAlbumArtViewPager.setByUser(false);
                mAlbumArtViewPager.setCurrentItem(mPlaybackService.getCurrentPlaylist().getPosition(), false);
                mAlbumArtViewPager.setByUser(true);
            }
            mAlbumArtViewPager.setSwiped(false);
            final TextView artistTextView = (TextView) findViewById(R.id.textView_artist);
            final TextView albumTextView = (TextView) findViewById(R.id.textView_album);
            final TextView titleTextView = (TextView) findViewById(R.id.textView_title);
            artistTextView.setText(track.getArtist().toString());
            albumTextView.setText(track.getAlbum().toString());
            titleTextView.setText(track.getTitle().toString());
            findViewById(R.id.imageButton_playpause).setClickable(true);
            findViewById(R.id.imageButton_next).setClickable(true);
            findViewById(R.id.imageButton_previous).setClickable(true);
            findViewById(R.id.imageButton_shuffle).setClickable(true);
            findViewById(R.id.imageButton_repeat).setClickable(true);
            mPlaybackSeekBar.setMax((int) mPlaybackService.getCurrentTrack().getDuration());
            mPlaybackSeekBar.updateSeekBarPosition();
            mPlaybackSeekBar.updateTextViewCompleteTime();
            mPlaybackSeekBar.updateTextViewCurrentTime();
            // Update the progressbar the next second
            mPlaybackSeekBar.getUiHandler().sendEmptyMessageDelayed(PlaybackSeekBar.getMsgUpdateProgress(),
                    1000);
        } else {
            findViewById(R.id.imageButton_playpause).setClickable(false);
            findViewById(R.id.imageButton_next).setClickable(false);
            findViewById(R.id.imageButton_previous).setClickable(false);
            findViewById(R.id.imageButton_shuffle).setClickable(false);
            findViewById(R.id.imageButton_repeat).setClickable(false);
        }
    }

    /**
     * Refresh the information in this activity to reflect that of the current
     * buttonstate.
     */
    private void refreshButtonStates() {
        final ImageButton button = (ImageButton) findViewById(R.id.imageButton_playpause);
        if (mPlaybackService != null && mPlaybackService.isPlaying())
            button.setImageDrawable(getResources().getDrawable(R.drawable.ic_player_pause));
        else
            button.setImageDrawable(getResources().getDrawable(R.drawable.ic_player_play));
    }
}
