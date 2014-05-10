/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.utils;

import org.tomahawk.libtomahawk.resolver.Query;

import android.content.Context;
import android.media.MediaPlayer;

public interface MediaPlayerInterface {

    public void setVolume(float leftVolume, float rightVolume);

    public void start();

    public void pause();

    public void seekTo(int msec);

    public MediaPlayerInterface prepare(Context context, Query query,
            MediaPlayer.OnPreparedListener onPreparedListener,
            MediaPlayer.OnCompletionListener onCompletionListener,
            MediaPlayer.OnErrorListener onErrorListener);

    public void release();

    public int getPosition();

    public boolean isPlaying(Query query);

    public boolean isPreparing(Query query);

    public boolean isPrepared(Query query);

    public void onPrepared(MediaPlayer mp);

    public void onCompletion(MediaPlayer mp);

    public boolean onError(MediaPlayer mp, int what, int extra);

}