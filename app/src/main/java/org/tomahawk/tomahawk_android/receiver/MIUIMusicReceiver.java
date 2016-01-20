package org.tomahawk.tomahawk_android.receiver;

import android.content.Context;
import android.os.Bundle;

public class MIUIMusicReceiver extends BuiltInMusicAppReceiver {

    static final String APP_PACKAGE = "com.miui.player";

    static final String ACTION_MIUI_STOP = "com.miui.player.playbackcomplete";

    public MIUIMusicReceiver() {
        super(ACTION_MIUI_STOP, APP_PACKAGE, "MIUI Music Player");
    }

    @Override
    protected void parseIntent(Context ctx, String action, Bundle bundle)
            throws IllegalArgumentException {
        super.parseIntent(ctx, action, bundle);
    }

}
