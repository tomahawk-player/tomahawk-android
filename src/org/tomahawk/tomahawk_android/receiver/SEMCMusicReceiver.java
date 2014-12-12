package org.tomahawk.tomahawk_android.receiver;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public class SEMCMusicReceiver extends BuiltInMusicAppReceiver {

    static final String APP_PACKAGE = "com.sonyericsson.music";

    static final String ACTION_SEMC_STOP_LEGACY
            = "com.sonyericsson.music.playbackcontrol.ACTION_PLAYBACK_PAUSE";

    static final String ACTION_SEMC_STOP = "com.sonyericsson.music.playbackcontrol.ACTION_PAUSED";

    private static final String TAG = SEMCMusicReceiver.class.getSimpleName();

    public SEMCMusicReceiver() {
        super(ACTION_SEMC_STOP, APP_PACKAGE, "Sony Ericsson Music Player");
    }

    @Override
    /**
     * Checks that the action received is either the one used in the
     * newer Sony Xperia devices or that of the previous versions
     * of the app.
     *
     * @param action    the received action
     * @return true when the received action is a stop action, false otherwise
     */
    protected boolean isStopAction(String action) {
        return action.equals(ACTION_SEMC_STOP) || action.equals(ACTION_SEMC_STOP_LEGACY);
    }

    @Override
    protected void parseIntent(Context ctx, String action, Bundle bundle)
            throws IllegalArgumentException {
        Log.d(TAG, "Will read data from SEMC intent");

        setTimestamp(System.currentTimeMillis());

        CharSequence ar = bundle.getCharSequence("ARTIST_NAME");
        CharSequence al = bundle.getCharSequence("ALBUM_NAME");
        CharSequence tr = bundle.getCharSequence("TRACK_NAME");

        if (ar == null || tr == null) {
            throw new IllegalArgumentException("null track values");
        }

        Artist artist = Artist.get(ar.toString());
        Album album = null;
        if (al != null) {
            album = Album.get(al.toString(), artist);
        }
        Track track = Track.get(tr.toString(), album, artist);
        setTrack(track);
    }
}
