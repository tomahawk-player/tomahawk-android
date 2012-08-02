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
import org.tomahawk.libtomahawk.audio.PlaybackService.PlaybackServiceBinder;
import org.tomahawk.libtomahawk.playlist.Playlist;
import org.tomahawk.tomahawk_android.R;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class PlaybackActivity extends SherlockActivity implements
        Handler.Callback, SeekBar.OnSeekBarChangeListener, OnTouchListener, OnPageChangeListener {

    private static final String TAG = PlaybackActivity.class.getName();

    private PlaybackService mPlaybackService;
    private NewTrackReceiver mNewTrackReceiver;    
    private PlaylistChangedReceiver mPlaylistChangedReceiver;

    private ViewPager mAlbumArtViewPager;
    private AlbumArtSwipeAdapter mAlbumArtSwipeAdapter;
    int currentViewPage=0;

    /**
     * Ui thread handler.
     */
    protected final Handler mUiHandler = new Handler(this);
    private SeekBar mSeekBar;
    private TextView mTextViewCompletionTime;
    private TextView mTextViewCurrentTime;
    boolean mIsSeeking = false;
    private static final int MSG_UPDATE_PROGRESS = 0x1;

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
    
    /**
     * Handles incoming changed playlist broadcasts from the PlaybackService.
     */
    private class PlaylistChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackService.BROADCAST_PLAYLISTCHANGED))
                onPlaylistChanged();
        }
    }

    /**
     * Allow communication to the PlaybackService.
     */
    private ServiceConnection mPlaybackServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            PlaybackServiceBinder binder = (PlaybackServiceBinder) service;
            mPlaybackService = binder.getService();
            onServiceReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mPlaybackService = null;
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        View view = getLayoutInflater()
                .inflate(R.layout.playback_activity, null);
        setContentView(view);
        
        mAlbumArtViewPager= (ViewPager) findViewById(R.id.album_art_view_pager);
        mAlbumArtViewPager.setOnPageChangeListener(this);
        mAlbumArtSwipeAdapter=new AlbumArtSwipeAdapter(this, null);
        mAlbumArtViewPager.setAdapter(mAlbumArtSwipeAdapter);
        mAlbumArtViewPager.setCurrentItem(0,false);
        
        final ActionBar bar = getSupportActionBar();
        bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayHomeAsUpEnabled(true);        

        mTextViewCompletionTime = (TextView) findViewById(R.id.textView_completionTime);
        mTextViewCurrentTime = (TextView) findViewById(R.id.textView_currentTime);
        mSeekBar = (SeekBar) findViewById(R.id.seekBar_track);
        mSeekBar.setOnSeekBarChangeListener(this);
        
        view.setOnTouchListener(this);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Intent playbackIntent = new Intent(this, PlaybackService.class);
        getApplicationContext().startService(playbackIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
        
        if (mNewTrackReceiver == null)
            mNewTrackReceiver = new NewTrackReceiver();
        if (mPlaylistChangedReceiver == null)
            mPlaylistChangedReceiver = new PlaylistChangedReceiver();
        IntentFilter intentFilter = new IntentFilter(PlaybackService.BROADCAST_NEWTRACK);
        registerReceiver(mNewTrackReceiver, intentFilter);
        intentFilter = new IntentFilter(PlaybackService.BROADCAST_PLAYLISTCHANGED);
        registerReceiver(mPlaylistChangedReceiver, intentFilter);

        Intent playbackIntent = new Intent(this, PlaybackService.class);
        bindService(playbackIntent, mPlaybackServiceConnection, Context.BIND_ABOVE_CLIENT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.actionbarsherlock.app.SherlockActivity#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

        if (mNewTrackReceiver != null)
            unregisterReceiver(mNewTrackReceiver);
        if (mPlaylistChangedReceiver != null)
            unregisterReceiver(mPlaylistChangedReceiver);
        unbindService(mPlaybackServiceConnection);
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
        getSupportMenuInflater().inflate(R.menu.tomahawk_main_activity, menu);
        menu.add("Search")
                .setIcon(R.drawable.ic_action_search)
                .setActionView(R.layout.collapsible_edittext)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS
                        | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
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
        switch (item.getItemId()) {
        case android.R.id.home:
            super.onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /* (non-Javadoc)
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    /* (non-Javadoc)
     * @see android.widget.SeekBar.OnSeekBarChangeListener#onStartTrackingTouch(android.widget.SeekBar)
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mIsSeeking = true;
    }
    
    /* (non-Javadoc)
     * @see android.widget.SeekBar.OnSeekBarChangeListener#onStopTrackingTouch(android.widget.SeekBar)
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mIsSeeking = false;
        mPlaybackService.seekTo(seekBar.getProgress());
        updateSeekBarPosition();
    }

    /* (non-Javadoc)
     * @see android.widget.SeekBar.OnSeekBarChangeListener#onProgressChanged(android.widget.SeekBar, int, boolean)
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        updateTextViewCurrentTime();
    }

    /**
     * Called when the PlaybackService is ready.
     */
    public void onServiceReady() {
        if (getIntent().hasExtra(PLAYLIST_EXTRA)) {
            try {
                mPlaybackService.setCurrentPlaylist((Playlist) getIntent()
                        .getSerializableExtra(PLAYLIST_EXTRA));
            } catch (IOException e) {
                e.printStackTrace();
            }
            getIntent().removeExtra(PLAYLIST_EXTRA);
        }
        refreshActivityTrackInfo();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.os.Handler.Callback#handleMessage(android.os.Message)
     */
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        case MSG_UPDATE_PROGRESS:
            updateSeekBarPosition();
            break;
        }
        return true;
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
        updateSeekBarPosition();
    }

    /**
     * Called when the next button is clicked.
     * 
     * @param view
     */
    public void onNextClicked(View view) {
        mAlbumArtViewPager.setCurrentItem(mPlaybackService.getCurrentPlaylist().getPosition()+1,false);
    }
    
    /**
     * play the next track and set the playbutton to pause icon
     *
     */
    public void nextTrack(){
        mPlaybackService.next();
        final ImageButton button = (ImageButton) findViewById(R.id.imageButton_playpause);
        button.setImageDrawable(getResources()
                .getDrawable(R.drawable.ic_player_pause));
    }

    /**
     * Called when the previous button is clicked.
     * 
     * @param view
     */
    public void onPreviousClicked(View view) {
        mAlbumArtViewPager.setCurrentItem(mPlaybackService.getCurrentPlaylist().getPosition()-1,false);
    }
    
    /**
     * play the previous track and set the playbutton to pause icon
     *
     */
    public void previousTrack(){
        mPlaybackService.previous();
        final ImageButton button = (ImageButton) findViewById(R.id.imageButton_playpause);
        button.setImageDrawable(getResources()
                .getDrawable(R.drawable.ic_player_pause));
    }

    /**
     * Called when the shuffle button is clicked.
     * 
     * @param view
     */
    public void onShuffleClicked(View view) {
        mPlaybackService.getCurrentPlaylist().setShuffled(
                !mPlaybackService.getCurrentPlaylist().isShuffled());
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

    /**
     * Updates the position on seekbar and the related textviews
     */
    private void updateSeekBarPosition() {
        if (!mPlaybackService.isPlaying() && !mIsSeeking)
            return;
        if (!mIsSeeking) {
            mSeekBar.setProgress(mPlaybackService.getPosition());
            updateTextViewCurrentTime();
        }
        mUiHandler.removeMessages(MSG_UPDATE_PROGRESS);
        mUiHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 10);
    }
    
    /**
     * Updates the textview that shows the current time the track is at
     *
     */
    private void updateTextViewCurrentTime(){
        mTextViewCurrentTime.setText(String.format("%02d", mPlaybackService.getPosition() / 60000)
                + ":" + String.format("%02d", (int)((mPlaybackService.getPosition()/ 1000) % 60 )));
    }
    
    /**
     * Updates the textview that shows the duration of the current track
     *
     */
    private void updateTextViewCompleteTime(){
        mTextViewCompletionTime.setText(String.format("%02d", mPlaybackService.getCurrentTrack().getDuration() / 60000)
                + ":" + String.format("%02d", (int)((mPlaybackService.getCurrentTrack().getDuration()/ 1000) % 60 )));
    }

    /**
     * Called when the PlaybackService signals the current Track has changed.
     */
    protected void onTrackChanged() {
        refreshActivityTrackInfo();
    }
    
    /**
     * Called when the PlaybackService signals the current Playlist has changed.
     */
    protected void onPlaylistChanged() {
        mAlbumArtSwipeAdapter=new AlbumArtSwipeAdapter(this, mPlaybackService.getCurrentPlaylist());
        mAlbumArtViewPager.setAdapter(mAlbumArtSwipeAdapter);
        mAlbumArtViewPager.setCurrentItem(mPlaybackService.getCurrentPlaylist().getPosition(),false);        
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
            if (mAlbumArtSwipeAdapter.isPlaylistNull())
                onPlaylistChanged();
            mAlbumArtViewPager.setCurrentItem(mPlaybackService.getCurrentPlaylist().getPosition(),false);
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
            mSeekBar.setMax((int)mPlaybackService.getCurrentTrack().getDuration());
            updateSeekBarPosition();
            updateTextViewCompleteTime();
            updateTextViewCurrentTime();
            // Update the progressbar the next second
            mUiHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 1000);
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
        if (mPlaybackService.isPlaying())
            button.setImageDrawable(getResources()
                    .getDrawable(R.drawable.ic_player_pause));
        else
            button.setImageDrawable(getResources()
                    .getDrawable(R.drawable.ic_player_play));
    }

    /* (non-Javadoc)
     * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrollStateChanged(int)
     */
    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    /* (non-Javadoc)
     * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrolled(int, float, int)
     */
    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    /* (non-Javadoc)
     * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageSelected(int)
     */
    @Override
    public void onPageSelected(int arg0) {
        if (arg0==currentViewPage-1){
            previousTrack();
        }
        else if (arg0==currentViewPage+1){
            nextTrack();
        }
        currentViewPage=mPlaybackService.getCurrentPlaylist().getPosition();
    }

}
