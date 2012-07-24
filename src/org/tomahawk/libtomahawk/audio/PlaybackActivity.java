package org.tomahawk.libtomahawk.audio;

import java.io.IOException;

import org.tomahawk.libtomahawk.Track;
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
    public static final String TRACK_EXTRA = "track";

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

        if (getIntent().hasExtra(TRACK_EXTRA))
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

            if (!getIntent().hasExtra(TRACK_EXTRA))
                return;

            Track track = (Track) getIntent().getSerializableExtra(TRACK_EXTRA);

            try {
                PlaybackService.get(this).setCurrentTrack(track);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Intent playbackIntent = new Intent(PlaybackActivity.this, PlaybackService.class);
            playbackIntent.putExtra(TRACK_EXTRA, getIntent().getSerializableExtra(TRACK_EXTRA));
            startService(playbackIntent);
        }

        getIntent().removeExtra(TRACK_EXTRA);
    }
}
