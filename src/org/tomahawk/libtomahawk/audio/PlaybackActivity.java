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

import org.tomahawk.libtomahawk.playlist.Playlist;
import org.tomahawk.tomahawk_android.R;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ImageView;

public class PlaybackActivity extends Activity implements Handler.Callback, OnTouchListener {

    private Looper mLooper;
    private Handler mHandler;
    private View mView;
    private ImageButton mPlayButton;
    private SeekBar mSeekbar;
    /**
     * Identifier for passing a Track as an extra in an Intent.
     */
    public static final String PLAYLIST_EXTRA = "playlist";
    
    /**
     * Create this activity.
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mView = getLayoutInflater().inflate(R.layout.player_fragment, null);
        setContentView(mView);
        
        mPlayButton = (ImageButton) mView.findViewById(R.id.imageButton_playpause );
        mPlayButton.setImageResource(R.drawable.ic_action_pause);
        
        mView.setOnTouchListener(this);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        HandlerThread thread = new HandlerThread(getClass().getName(),
                Process.THREAD_PRIORITY_LOWEST);
        thread.start();

        mLooper = thread.getLooper();
        mHandler = new Handler(mLooper, this);	
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getIntent().hasExtra(PLAYLIST_EXTRA))
            onServiceReady();

    }

    /**
     * Handle Handler messages.
     */
    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    /**
     * Handle screen touches for this activity.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            PlaybackService.get(this).playPause();
            Intent playbackIntent = new Intent(PlaybackActivity.this, PlaybackService.class);
            playbackIntent.putExtra(PLAYLIST_EXTRA, getIntent().getSerializableExtra(PLAYLIST_EXTRA));
            populateMetadata( (Playlist) getIntent().getSerializableExtra(PLAYLIST_EXTRA) );
        }
        return false;
    }

    /**
     * onPlayPauseClick
     * @param v
     */
    public void onPlayPauseClick(View v)
    {

		PlaybackService.get(this).playPause();
		
		if( PlaybackService.get(this).isPlaying() )
			mPlayButton.setImageResource(R.drawable.ic_action_pause);
		else 
			mPlayButton.setImageResource(R.drawable.ic_action_play);
    }
    

    /**
     * Called when the service is ready and requested.
     */
    private void onServiceReady() {

        if (PlaybackService.hasInstance()) {

            if (!getIntent().hasExtra(PLAYLIST_EXTRA))
                return;
            
            Playlist playlist = (Playlist) getIntent().getSerializableExtra(PLAYLIST_EXTRA);
            
            try {
                PlaybackService.get(this).setCurrentPlaylist(playlist);
                populateMetadata(playlist);
                startPlayProgressUpdater();
                
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {

            Intent playbackIntent = new Intent(PlaybackActivity.this, PlaybackService.class);
            Playlist playlist = (Playlist) getIntent().getSerializableExtra(PLAYLIST_EXTRA);
            playbackIntent.putExtra(PLAYLIST_EXTRA, playlist);
            
            populateMetadata(playlist);
            startService(playbackIntent);
            startPlayProgressUpdater();
        }

        getIntent().removeExtra(PLAYLIST_EXTRA);
    }
    
    /**
     * startPlayProgressUpdater
     * will update the seek bar
     * note: kinda sucky
     */
    public void startPlayProgressUpdater() {
    	
    	if( PlaybackService.hasInstance() ) {	
    		if( PlaybackService.get(this).isPlaying() ) {
    			mSeekbar.setProgress( PlaybackService.get(this).getCurrentPosition() );
    		}
    	} else {
    		mSeekbar.setProgress(0);
    	}
    	// Not the smoothest seek transition
    	
		Runnable notify = new Runnable() {
		    public void run() {
		    	startPlayProgressUpdater();
		    }
		};
		mHandler.postDelayed(notify,1000);
}

    // This is event handler thumb moving event
    private void seekChange(View v) {
    	
		if(PlaybackService.get(this).isPlaying()){
			SeekBar seekbar = (SeekBar)v;
			PlaybackService.get(this).seekTo(seekbar.getProgress());
		}
    }
    /**
     * Called when service is ready to populate playerMetadata
     * Also sets the seekbar max and listener
     */
     private void populateMetadata(Playlist playlist) {
    	 
    	 TextView meta = (TextView) mView.findViewById(R.id.textView_track);
         meta.setText(playlist.getCurrentTrack().getTitle() );
         
         meta = (TextView) mView.findViewById(R.id.textView_artist);
         meta.setText(playlist.getCurrentTrack().getArtist().getName() );
         
         meta = (TextView) mView.findViewById(R.id.textView_album);
         meta.setText(playlist.getCurrentTrack().getAlbum().getName() );

         ImageView image = (ImageView) findViewById(R.id.imageView_cover);
         image.setImageURI( Uri.parse( playlist.getCurrentTrack().getAlbum().getAlbumArt() ) );
         
         mSeekbar = (SeekBar) findViewById(R.id.seekBar_track);
         
         mSeekbar.setMax((int)playlist.getCurrentTrack().getDuration());
         mSeekbar.setOnTouchListener(new OnTouchListener() {
        	 @Override 
        	 public boolean onTouch(View v, MotionEvent event) {
        		 seekChange(v);
        		 return false; 
        	 }
 		});
     }
}
