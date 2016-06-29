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
package org.tomahawk.tomahawk_android.mediaplayers;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.tomahawk_android.utils.ThreadManager;

public abstract class TomahawkMediaPlayer {

    public abstract void play();

    public abstract void pause();

    public abstract void seekTo(long msec);

    public abstract void prepare(Query query, TomahawkMediaPlayerCallback callback);

    public abstract void release();

    public abstract long getPosition();

    public abstract void setBitrate(int mode);

    public abstract boolean isPlaying(Query query);

    public abstract boolean isPreparing(Query query);

    public abstract boolean isPrepared(Query query);

    public Promise<String, Throwable, Void> getStreamUrl(Result result) {
        final DeferredObject<String, Throwable, Void> deferred = new DeferredObject<>();
        if (result.getResolvedBy() instanceof ScriptResolver) {
            ScriptResolver resolver = (ScriptResolver) result.getResolvedBy();
            resolver.getStreamUrl(result)
                    .done(new DoneCallback<String>() {
                        @Override
                        public void onDone(final String url) {
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    deferred.resolve(url);
                                }
                            };
                            ThreadManager.get().executePlayback(TomahawkMediaPlayer.this, r);
                        }
                    })
                    .fail(new FailCallback<Throwable>() {
                        @Override
                        public void onFail(final Throwable result) {
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    deferred.reject(result);
                                }
                            };
                            ThreadManager.get().executePlayback(TomahawkMediaPlayer.this, r);
                        }
                    });
        } else {
            deferred.resolve(result.getPath());
        }
        return deferred;
    }

}