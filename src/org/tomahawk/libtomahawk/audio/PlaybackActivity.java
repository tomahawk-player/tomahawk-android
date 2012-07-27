/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class PlaybackActivity extends SherlockActivity implements
        Handler.Callback {

    private static final String TAG = PlaybackActivity.class.getName();

    private NewTrackReceiver mNewTrackReceiver;

    private Looper mLooper;
    private Handler mHandler;

    /**
     * Identifier for passing a Track as an extra in an Intent.
     */
    public static final String PLAYLIST_EXTRA = "playlist";

    /**
     * Handles incoming new Track broadcasts from the PlaybackService.
     */
    private class NewTrackReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackService.BROADCAST_NEWTRACK))
                onTrackChanged();
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        View view = getLayoutInflater()
                .inflate(R.layout.playback_activity, null);
        setContentView(view);

        final ActionBar bar = getSupportActionBar();
        bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayHomeAsUpEnabled(true);

        final ImageButton button = (ImageButton) findViewById(R.id.imageButton_cover);
        Display display = getWindowManager().getDefaultDisplay();
        button.setMinimumHeight(display.getWidth());

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        HandlerThread thread = new HandlerThread(getClass().getName(), Process.THREAD_PRIORITY_LOWEST);
        thread.start();

        mLooper = thread.getLooper();
        mHandler = new Handler(mLooper, this);
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockActivity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.tomahawk_main_activity, menu);
        menu.add("Search")
                .setIcon(R.drawable.ic_action_search)
                .setActionView(R.layout.collapsible_edittext)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS
                        | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        return true;
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockActivity#onOptionsItemSelected(android.view.MenuItem)
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

    /* (non-Javadoc)
     * @see android.app.Activity#onStart()
     */
    @Override
    public void onStart() {
        super.onStart();

        if (mNewTrackReceiver == null)
            mNewTrackReceiver = new NewTrackReceiver();
        IntentFilter intentFilter = new IntentFilter(PlaybackService.BROADCAST_NEWTRACK);
        registerReceiver(mNewTrackReceiver, intentFilter);

        if (getIntent().hasExtra(PLAYLIST_EXTRA))
            onServiceReady();
        else
            refreshActivityTrackInfo(PlaybackService.get(this).getCurrentTrack());
    }

    /* (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockActivity#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

        if (mNewTrackReceiver != null)
            unregisterReceiver(mNewTrackReceiver);
    }
    
    /* (non-Javadoc)
     * @see android.os.Handler.Callback#handleMessage(android.os.Message)
     */
    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    /**
     * Called when the service is ready and requested.
     */
    private void onServiceReady() {

        if (PlaybackService.hasInstance()) {

            if (!getIntent().hasExtra(PLAYLIST_EXTRA))
                return;

            Playlist playlist = (Playlist) getIntent()
                    .getSerializableExtra(PLAYLIST_EXTRA);

            try {
                PlaybackService.get(this).setCurrentPlaylist(playlist);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Intent playbackIntent = new Intent(PlaybackActivity.this, PlaybackService.class);
            playbackIntent.putExtra(PLAYLIST_EXTRA, getIntent()
                    .getSerializableExtra(PLAYLIST_EXTRA));
            startService(playbackIntent);
        }

        getIntent().removeExtra(PLAYLIST_EXTRA);
    }

    /**
     * Called when the play/pause button is clicked.
     * 
     * @param view
     */
    public void onPlayPauseClicked(View view) {
        Log.d(TAG,"onPlayPauseClicked");
        PlaybackService.get(this).playPause();
        refreshButtonStates();
    }

    /**
     * Called when the next button is clicked.
     * 
     * @param view
     */
    public void onNextClicked(View view) {
        PlaybackService.get(this).next();
        final ImageButton button = (ImageButton) findViewById(R.id.imageButton_playpause);
        button.setImageDrawable(getResources()
                .getDrawable(R.drawable.ic_action_pause));
    }

    /**
     * Called when the previous button is clicked.
     * 
     * @param view
     */
    public void onPreviousClicked(View view) {
        PlaybackService.get(this).previous();
        final ImageButton button = (ImageButton) findViewById(R.id.imageButton_playpause);
        button.setImageDrawable(getResources()
                .getDrawable(R.drawable.ic_action_pause));
    }

    /**
     * Called when the shuffle button is clicked.
     * 
     * @param view
     */
    public void onShuffleClicked(View view) {
    }

    /**
     * Called when the repeat button is clicked.
     * 
     * @param view
     */
    public void onRepeatClicked(View view) {
    }

    /**
     * Called when the PlaybackService signals the current Track has changed.
     */
    protected void onTrackChanged() {
        refreshActivityTrackInfo(PlaybackService.get(this).getCurrentTrack());
    }

    /**
     * Refresh the information in this activity to reflect that of the current
     * Track.
     */
    private void refreshActivityTrackInfo(Track track) {

        if (track != null) {
            final ImageButton button = (ImageButton) findViewById(R.id.imageButton_cover);
            final TextView artistTextView = (TextView) findViewById(R.id.textView_artist);
            final TextView albumTextView = (TextView) findViewById(R.id.textView_album);
            final TextView titleTextView = (TextView) findViewById(R.id.textView_title);
            button.setImageBitmap(track.getAlbum().getAlbumArt());
            artistTextView.setText(track.getArtist().toString());
            albumTextView.setText(track.getAlbum().toString());
            titleTextView.setText(track.getTitle().toString());
            
            findViewById(R.id.imageButton_playpause).setClickable(true);
            findViewById(R.id.imageButton_next).setClickable(true);
            findViewById(R.id.imageButton_previous).setClickable(true);
            findViewById(R.id.imageButton_shuffle).setClickable(true);
            findViewById(R.id.imageButton_repeat).setClickable(true);
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
        if (PlaybackService.get(this).isPlaying())
            button.setImageDrawable(getResources()
                    .getDrawable(R.drawable.ic_action_pause));
        else
            button.setImageDrawable(getResources()
                    .getDrawable(R.drawable.ic_action_play));
    }
    
}
