/*   
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tomahawk.tomahawk_android.utils;

import org.tomahawk.tomahawk_android.services.PlaybackService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Receives broadcasted intents. In particular, we are interested in the
 * android.media.AUDIO_BECOMING_NOISY and android.intent.action.MEDIA_BUTTON intents, which is
 * broadcast, for example, when the user disconnects the headphones. This class works because we are
 * declaring it in a &lt;receiver&gt; tag in AndroidManifest.xml.
 */
public class MediaButtonReceiver extends BroadcastReceiver {

    private static String TAG = MediaButtonReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
            Log.d(TAG, "Mediabutton pressed ... keyCode: " + keyEvent.getKeyCode());
            if (keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
                return;
            }

            switch (keyEvent.getKeyCode()) {
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    context.startService(new Intent(PlaybackService.ACTION_PLAYPAUSE));
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    context.startService(new Intent(PlaybackService.ACTION_PLAY));
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    context.startService(new Intent(PlaybackService.ACTION_PAUSE));
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    context.startService(new Intent(PlaybackService.ACTION_PAUSE));
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    context.startService(new Intent(PlaybackService.ACTION_NEXT));
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    context.startService(new Intent(PlaybackService.ACTION_PREVIOUS));
                    break;
            }
        }
    }
}
