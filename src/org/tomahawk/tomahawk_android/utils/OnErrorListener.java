package org.tomahawk.tomahawk_android.utils;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 23.06.13
 */
public interface OnErrorListener {

    public abstract boolean onError(TomahawkMediaPlayer tmp, int what, int extra);
}