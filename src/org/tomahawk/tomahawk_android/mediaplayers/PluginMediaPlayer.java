/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.tomahawk.aidl.IPluginService;
import org.tomahawk.aidl.IPluginServiceCallback;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.PlaybackService;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public abstract class PluginMediaPlayer extends IPluginServiceCallback.Stub {

    private static final String TAG = PluginMediaPlayer.class.getSimpleName();

    private ScriptResolver mScriptResolver;

    private IPluginService mService;

    private List<ServiceCall> mWaitingServiceCalls = new ArrayList<>();

    public interface ServiceCall {

        void call(IPluginService pluginService);
    }

    public ScriptResolver getScriptResolver() {
        if (mScriptResolver != null) {
            return mScriptResolver;
        } else {
            String pluginName = null;
            if (this instanceof SpotifyMediaPlayer) {
                pluginName = TomahawkApp.PLUGINNAME_SPOTIFY;
            }/* else if (this instanceof DeezerMediaPlayer) {
                pluginName = TomahawkApp.PLUGINNAME_DEEZER;
            } else if (this instanceof RdioMediaPlayer) {
                pluginName = TomahawkApp.PLUGINNAME_RDIO;
            }*/
            mScriptResolver = (ScriptResolver) PipeLine.getInstance().getResolver(pluginName);
        }
        if (mScriptResolver == null) {
            Log.e(TAG, "getScriptResolver - Couldn't find associated ScriptResolver!");
        }
        return mScriptResolver;
    }

    public void setService(IPluginService service) {
        mService = service;
        if (mService != null) {
            for (int i = 0; i < mWaitingServiceCalls.size(); i++) {
                mWaitingServiceCalls.remove(i).call(mService);
            }
        }
    }

    /**
     * Call the {@link IPluginService} that is associated with this {@link PluginMediaPlayer}. If it
     * is not available, a binding will be requested. The given {@link ServiceCall} will be cached
     * in that case and called after a successful binding to the {@link IPluginService}.
     *
     * @param serviceCall the actual {@link ServiceCall} that should be made to the {@link
     *                    IPluginService}.
     */
    protected void callService(ServiceCall serviceCall) {
        callService(serviceCall, true);
    }

    /**
     * Call the {@link IPluginService} that is associated with this {@link PluginMediaPlayer}. If it
     * is not available, a binding will be requested.
     *
     * @param serviceCall the actual {@link ServiceCall} that should be made to the {@link
     *                    IPluginService}.
     * @param cacheCall   Whether or not the given {@link ServiceCall} should be cached if the
     *                    {@link IPluginService} is unavailable and called after a successful
     *                    binding to the {@link IPluginService}.
     */
    protected void callService(ServiceCall serviceCall, boolean cacheCall) {
        if (mService != null) {
            serviceCall.call(mService);
        } else {
            if (cacheCall) {
                mWaitingServiceCalls.add(serviceCall);
            }
            requestService();
        }
    }

    /**
     * Construct and send off a {@link PlaybackService.RequestServiceBindingEvent} to the {@link
     * PlaybackService}. As soon as the {@link PlaybackService} has been successfully bound to the
     * {@link IPluginService} {@link #setService} will be called.
     */
    private void requestService() {
        String packageName = null;
        if (this instanceof SpotifyMediaPlayer) {
            packageName = "org.tomahawk.spotifyplugin";
        }/* else if (this instanceof DeezerMediaPlayer) {
            packageName = "org.tomahawk.deezerplugin";
        } else if (this instanceof RdioMediaPlayer){
            packageName="org.tomahawk.rdioplugin";
        }*/ else {
            Log.e(TAG, "requestService - PluginMediaPlayer type not supported!");
        }
        PlaybackService.RequestServiceBindingEvent event =
                new PlaybackService.RequestServiceBindingEvent(this, packageName);
        EventBus.getDefault().post(event);
    }

}
