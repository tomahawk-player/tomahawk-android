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

import android.app.Activity;
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
import android.view.View;
import android.widget.ImageButton;

public class PlaybackActivity extends Activity implements Handler.Callback {

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

    /**
     * Create this activity.
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        View view = getLayoutInflater().inflate(R.layout.playback_activity, null);
        setContentView(view);

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

        if (mNewTrackReceiver == null)
            mNewTrackReceiver = new NewTrackReceiver();
        IntentFilter intentFilter = new IntentFilter(PlaybackService.BROADCAST_NEWTRACK);
        registerReceiver(mNewTrackReceiver, intentFilter);

        if (getIntent().hasExtra(PLAYLIST_EXTRA))
            onServiceReady();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mNewTrackReceiver != null)
            unregisterReceiver(mNewTrackReceiver);
    }

    /**
     * Handle Handler messages.
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

            Playlist playlist = (Playlist) getIntent().getSerializableExtra(PLAYLIST_EXTRA);

            try {
                PlaybackService.get(this).setCurrentPlaylist(playlist);
            } catch (IOException e) {
                e.printStackTrace();
            }

            refreshTrackInfo();

        } else {
            Intent playbackIntent = new Intent(PlaybackActivity.this, PlaybackService.class);
            playbackIntent.putExtra(PLAYLIST_EXTRA, getIntent().getSerializableExtra(PLAYLIST_EXTRA));
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
        PlaybackService.get(this).playPause();
    }

    /**
     * Called when the next button is clicked.
     * 
     * @param view
     */
    public void onNextClicked(View view) {
        PlaybackService.get(this).next();
    }

    /**
     * Called when the previous button is clicked.
     * 
     * @param view
     */
    public void onPreviousClicked(View view) {
        PlaybackService.get(this).previous();
    }

    /**
     * Called when the PlaybackService signals the current Track has changed.
     */
    protected void onTrackChanged() {

        refreshTrackInfo();
        stockMusicBroadcast(PlaybackService.get(this).getCurrentTrack());
    }

    /**
     * Send a broadcast emulating that of the stock music player.
     * 
     * Borrow from Vanilla Music Player. Thanks!
     */
    private void stockMusicBroadcast(Track track) {

        Intent intent = new Intent("com.android.music.playstatechanged");
        intent.putExtra("playing", 1);
        if (track != null) {
            intent.putExtra("track", track.getTitle());
            intent.putExtra("album", track.getAlbum().getName());
            intent.putExtra("artist", track.getArtist().getName());
            intent.putExtra("songid", track.getId());
            intent.putExtra("albumid", track.getAlbum().getId());
        }
        sendBroadcast(intent);
    }

    /**
     * Refresh the information in this activity to reflect that of the current
     * Track.
     */
    private void refreshTrackInfo() {
        final ImageButton button = (ImageButton) findViewById(R.id.albumImageButton);

        Track track = PlaybackService.get(this).getCurrentTrack();

        button.setImageDrawable(track.getAlbum().getAlbumArt());
    }
}
