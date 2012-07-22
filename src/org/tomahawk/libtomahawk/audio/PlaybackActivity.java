package org.tomahawk.libtomahawk.audio;

import java.io.IOException;

import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.SourceList;
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

    private static final String TAG = PlaybackActivity.class.getName();

    private PlaybackService mPlaybackService;

    private Looper mLooper;
    private Handler mHandler;

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

        if (PlaybackService.hasInstance()) {

            if (mPlaybackService == null) {
                mPlaybackService = PlaybackService.get(this);

                Collection coll = SourceList.instance().getLocalSource().getCollection();
                Track track = coll.getTracks().get(0);

                try {
                    mPlaybackService.setCurrentTrack(track);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            mPlaybackService.playPause();
        }
        else
            startService(new Intent(this, PlaybackService.class));

        return false;
    }
}
