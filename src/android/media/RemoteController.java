package android.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.KeyEvent;

/**
 * This class is just an empty shell, so that RemoteControllerService doesn't crash on API18
 * devices
 */
public class RemoteController {

    private static final String TAG = "RemoteControllerShell";

    /**
     * Default playback position synchronization mode where the RemoteControlClient is not asked
     * regularly for its playback position to see if it has drifted from the estimated position.
     */
    public static final int POSITION_SYNCHRONIZATION_NONE = 0;

    /**
     * The playback position synchronization mode where the RemoteControlClient instances which
     * expose their playback position to the framework, will be regularly polled to check whether
     * any drift has been noticed between their estimated position and the one they report. Note
     * that this mode should only ever be used when needing to display very accurate playback
     * position, as regularly polling a RemoteControlClient for its position may have an impact on
     * battery life (if applicable) when this query will trigger network transactions in the case of
     * remote playback.
     */
    public static final int POSITION_SYNCHRONIZATION_CHECK = 1;

    byte[] engineDigest() {
        return new byte[0];
    }

    void engineReset() {
    }

    void engineUpdate(byte[] input, int offset, int len) {
    }

    public RemoteController(Context context, OnClientUpdateListener l) {
        Log.d(TAG, "Remote Controller Shell instantiated");
    }

    public boolean setArtworkConfiguration(boolean wantBitmap, int width, int height) {
        return false;
    }

    public boolean sendMediaKeyEvent(KeyEvent keyEvent) {
        return false;
    }

    public boolean setArtworkConfiguration(int width, int height) throws IllegalArgumentException {
        return setArtworkConfiguration(true, width, height);
    }

    public class MetadataEditor {

        public synchronized String getString(int key, String defaultValue) {
            return "";
        }

        public synchronized long getLong(int key, long defaultValue) {
            long i = 0;
            return i;
        }

        public synchronized Bitmap getBitmap(int key, Bitmap defaultValue) {
            Bitmap b = null;
            return b;
        }
    }

    public interface OnClientUpdateListener {

        public void onClientChange(boolean clearing);

        public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor);

        public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs,
                long currentPosMs, float speed);

        public void onClientPlaybackStateUpdate(int state);

        public void onClientTransportControlUpdate(int transportControlFlags);

    }
}