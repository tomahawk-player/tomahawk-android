/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
 *   Copyright 2012, Hugo Lindström <hugolm84@gmail.com>
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
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class PlaybackActivity extends SherlockActivity implements
        Handler.Callback, SeekBar.OnSeekBarChangeListener, OnTouchListener {

    private static final String TAG = PlaybackActivity.class.getName();

    private PlaybackService mPlaybackService;
    private NewTrackReceiver mNewTrackReceiver;
    private GestureDetector mGestureDetector;

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

    /**
     * Detects motion gestures and handles them
     */
    private class PlaybackGestureDetector extends SimpleOnGestureListener {
        private final ViewConfiguration vc = ViewConfiguration.get(getApplicationContext());
        private DisplayMetrics dm = getResources().getDisplayMetrics();

        private final int RELATIVE_SWIPE_MIN_DISTANCE = (int)(vc.getScaledTouchSlop() * dm.densityDpi / 160.0f);
        private final int RELATIVE_SWIPE_MAX_OFF_PATH = (int)(vc.getScaledTouchSlop() * dm.densityDpi / 160.0f);
        private final int RELATIVE_SWIPE_THRESHOLD_VELOCITY = (int)(vc.getScaledMinimumFlingVelocity() * dm.densityDpi / 160.0f);

        /*
         * (non-Javadoc)
         * @see android.view.GestureDetector.SimpleOnGestureListener#onFling(android.view.MotionEvent, android.view.MotionEvent, float, float)
         */
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(e1.getY() - e2.getY()) > RELATIVE_SWIPE_MAX_OFF_PATH) {
                return false;
            }

            ImageView albumCover = (ImageView)findViewById(R.id.imageButton_cover);

            if(e1.getX() - e2.getX() > RELATIVE_SWIPE_MIN_DISTANCE && Math.abs(velocityX) > RELATIVE_SWIPE_THRESHOLD_VELOCITY) {
                onNextSwipe();
                final Animation slideOutToLeft = AnimationUtils.makeOutAnimation(getApplicationContext(), false);
                albumCover.startAnimation(slideOutToLeft);
                
            }  else if (e2.getX() - e1.getX() > RELATIVE_SWIPE_MIN_DISTANCE && Math.abs(velocityX) > RELATIVE_SWIPE_THRESHOLD_VELOCITY) {
                onPreviousSwipe();
                final Animation slideOutToRight = AnimationUtils.makeOutAnimation(getApplicationContext(), true);
                albumCover.startAnimation(slideOutToRight);
            }
            albumCover = null;
            return false;
        }

        /**
         * Gets next track on swipe/flick to right
         */
        private void onNextSwipe() {
            mPlaybackService.next();
            final ImageButton button = (ImageButton) findViewById(R.id.imageButton_playpause);
            button.setImageDrawable(getResources()
                    .getDrawable(R.drawable.ic_player_pause));
        }

        /**
         * Gets previous track on swipe/flick to left
         */
        private void onPreviousSwipe() {
            mPlaybackService.previous();
            final ImageButton button = (ImageButton) findViewById(R.id.imageButton_playpause);
            button.setImageDrawable(getResources()
                    .getDrawable(R.drawable.ic_player_pause));
        }

        /*
         * (non-Javadoc)
         * @see android.view.GestureDetector.SimpleOnGestureListener#onDown(android.view.MotionEvent)
         * Must return true to enable onFlick for View ( Metadata area )
         */
        @Override
        public boolean onDown(MotionEvent e) {
                return true;
        }

        /*
         * (non-Javadoc)
         * @see android.view.GestureDetector.OnGestureListener#onLongPress(android.view.MotionEvent)
         */
        @Override
        public void onLongPress(MotionEvent e) {
        }

        /*
         * (non-Javadoc)
         * @see android.view.GestureDetector.OnGestureListener#onScroll(android.view.MotionEvent, android.view.MotionEvent, float, float)
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                float distanceY) {
            return false;
        }

        /*
         * (non-Javadoc)
         * @see android.view.GestureDetector.OnGestureListener#onShowPress(android.view.MotionEvent)
         */
        @Override
        public void onShowPress(MotionEvent e) {
        }

        /*
         * (non-Javadoc)
         * @see android.view.GestureDetector.OnGestureListener#onSingleTapUp(android.view.MotionEvent)
         */
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
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
        View view = getLayoutInflater()
                .inflate(R.layout.playback_activity, null);
        setContentView(view);

        final ActionBar bar = getSupportActionBar();
        bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayHomeAsUpEnabled(true);        

        mTextViewCompletionTime = (TextView) findViewById(R.id.textView_completionTime);
        mTextViewCurrentTime = (TextView) findViewById(R.id.textView_currentTime);
        mSeekBar = (SeekBar) findViewById(R.id.seekBar_track);
        mSeekBar.setOnSeekBarChangeListener(this);

        mGestureDetector = new GestureDetector(this.getApplicationContext(),new PlaybackGestureDetector(), mUiHandler );
        view.setOnTouchListener(this);
        final ImageButton button = (ImageButton) findViewById(R.id.imageButton_cover);
        button.setOnTouchListener(this);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Intent playbackIntent = new Intent(this, PlaybackService.class);
        getApplicationContext().startService(playbackIntent);
    }

    /*
     * (non-Javadoc)
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @Override
    public boolean onTouch(View v, MotionEvent e) {
        if (mGestureDetector.onTouchEvent(e)) {
            return true;
        }
        return false;
    }

    /**
     * Called when user is seeking in the seekbar.
     * Will seek to progress when stopped
     */
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
     * @see android.widget.SeekBar.OnSeekBarChangeListener#onStartTrackingTouch(android.widget.SeekBar)
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mIsSeeking = true;
    }

    /* (non-Javadoc)
     * @see android.widget.SeekBar.OnSeekBarChangeListener#onProgressChanged(android.widget.SeekBar, int, boolean)
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        mTextViewCurrentTime.setText(String.format("%02d", progress / 60000)
                + ":" + String.format("%02d", (int)((progress/ 1000) % 60 )));
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

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onStart()
     */
    @Override
    public void onStart() {
        super.onStart();

        if (mNewTrackReceiver == null)
            mNewTrackReceiver = new NewTrackReceiver();
        IntentFilter intentFilter = new IntentFilter(PlaybackService.BROADCAST_NEWTRACK);
        registerReceiver(mNewTrackReceiver, intentFilter);

        Intent playbackIntent = new Intent(this, PlaybackService.class);
        bindService(playbackIntent, mPlaybackServiceConnection, Context.BIND_ABOVE_CLIENT);
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
        if (mPlaybackService != null)
            refreshActivityTrackInfo(mPlaybackService.getCurrentTrack());
        else
            refreshActivityTrackInfo(null);
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
        unbindService(mPlaybackServiceConnection);
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
        mPlaybackService.getCurrentPlaylist().setShuffed(
                !mPlaybackService.getCurrentPlaylist().isShuffled());
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
            int mPosition = mPlaybackService.getPosition();
            mSeekBar.setProgress(mPosition);
            mTextViewCurrentTime.setText(String.format("%02d", mPosition / 60000)
                    + ":" + String.format("%02d", (int)((mPosition/ 1000) % 60 )));
        }
        mUiHandler.removeMessages(MSG_UPDATE_PROGRESS);
        mUiHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 10);
    }

    /**
     * Called when the PlaybackService signals the current Track has changed.
     */
    protected void onTrackChanged() {
        refreshActivityTrackInfo(mPlaybackService.getCurrentTrack());
    }

    /**
     * Refresh the information in this activity to reflect that of the current
     * Track.
     */
    private void refreshActivityTrackInfo(Track track) {
        final ImageButton button = (ImageButton) findViewById(R.id.imageButton_cover);
        if (track != null) {
            final TextView artistTextView = (TextView) findViewById(R.id.textView_artist);
            final TextView albumTextView = (TextView) findViewById(R.id.textView_album);
            final TextView titleTextView = (TextView) findViewById(R.id.textView_title);
            Bitmap albumArt = track.getAlbum().getAlbumArt();
            if (albumArt != null)
                button.setImageBitmap(albumArt);
            else
                button.setImageDrawable((getResources()
                        .getDrawable(R.drawable.no_album_art_placeholder)));
            artistTextView.setText(track.getArtist().toString());
            albumTextView.setText(track.getAlbum().toString());
            titleTextView.setText(track.getTitle().toString());
            findViewById(R.id.imageButton_playpause).setClickable(true);
            findViewById(R.id.imageButton_next).setClickable(true);
            findViewById(R.id.imageButton_previous).setClickable(true);
            findViewById(R.id.imageButton_shuffle).setClickable(true);
            findViewById(R.id.imageButton_repeat).setClickable(true);
            int duration = (int) mPlaybackService.getCurrentTrack()
                    .getDuration();
            mSeekBar.setProgress(0);
            mSeekBar.setMax(duration);
            mTextViewCompletionTime.setText(String.format("%02d", duration / 60000)
                    + ":" + String.format("%02d", (int)((duration/ 1000) % 60 )));
            int mPosition = mPlaybackService.getPosition();
            mTextViewCurrentTime.setText(String.format("%02d", mPosition / 60000)
                    + ":" + String.format("%02d", (int)((mPosition/ 1000) % 60 )));
            // Update the progressbar the next second
            mUiHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 1000);
        } else {
            button.setImageDrawable((getResources()
                    .getDrawable(R.drawable.no_album_art_placeholder)));
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

}
