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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class PlaybackActivity extends Activity implements Handler.Callback, OnTouchListener {

    private Looper mLooper;
    private Handler mHandler;

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
        View view = getLayoutInflater().inflate(R.layout.playback_activity, null);
        setContentView(view);
        view.setOnTouchListener(this);

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
            PlaybackService.get(this).playPause();

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

        } else {
            Intent playbackIntent = new Intent(PlaybackActivity.this, PlaybackService.class);
            playbackIntent.putExtra(PLAYLIST_EXTRA, getIntent().getSerializableExtra(PLAYLIST_EXTRA));
            startService(playbackIntent);
        }

        getIntent().removeExtra(PLAYLIST_EXTRA);
    }
}
