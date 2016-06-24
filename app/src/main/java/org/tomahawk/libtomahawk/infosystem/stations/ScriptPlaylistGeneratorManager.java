/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.libtomahawk.infosystem.stations;

import org.tomahawk.tomahawk_android.TomahawkApp;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;

public class ScriptPlaylistGeneratorManager {

    public static final String TAG = ScriptPlaylistGeneratorManager.class.getSimpleName();

    private static class Holder {

        private static final ScriptPlaylistGeneratorManager
                instance = new ScriptPlaylistGeneratorManager();

    }

    public class GeneratorAddedEvent {

    }

    private Map<String, ScriptPlaylistGenerator> mPlaylistGeneratorMap = new HashMap<>();

    private ScriptPlaylistGeneratorManager() {

    }

    public static ScriptPlaylistGeneratorManager get() {
        return Holder.instance;
    }

    public void addPlaylistGenerator(ScriptPlaylistGenerator generator) {
        mPlaylistGeneratorMap.put(generator.getScriptAccount().getName(), generator);
        EventBus.getDefault().post(new GeneratorAddedEvent());
    }

    public void removePlaylistGenerator(ScriptPlaylistGenerator generator) {
        mPlaylistGeneratorMap.remove(generator.getScriptAccount().getName());
    }

    public Map<String, ScriptPlaylistGenerator> getAllPlaylistGenerator() {
        return mPlaylistGeneratorMap;
    }

    public ScriptPlaylistGenerator getPlaylistGenerator(String id) {
        return mPlaylistGeneratorMap.get(id);
    }

    public ScriptPlaylistGenerator getDefaultPlaylistGenerator() {
        return mPlaylistGeneratorMap.get(getDefaultPlaylistGeneratorId());
    }

    public String getDefaultPlaylistGeneratorId() {
        return TomahawkApp.PLUGINNAME_SPOTIFY;
    }

}
