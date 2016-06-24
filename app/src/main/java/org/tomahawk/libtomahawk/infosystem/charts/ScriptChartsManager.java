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
package org.tomahawk.libtomahawk.infosystem.charts;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;

public class ScriptChartsManager {

    public static final String TAG = ScriptChartsManager.class.getSimpleName();

    private static class Holder {

        private static final ScriptChartsManager instance = new ScriptChartsManager();

    }

    public class ProviderAddedEvent {

    }

    private Map<String, ScriptChartsProvider> mScriptChartsProviderMap = new HashMap<>();

    private ScriptChartsManager() {

    }

    public static ScriptChartsManager get() {
        return Holder.instance;
    }

    public void addScriptChartsProvider(ScriptChartsProvider provider) {
        mScriptChartsProviderMap.put(provider.getScriptAccount().getName(), provider);
        EventBus.getDefault().post(new ProviderAddedEvent());
    }

    public void removeScriptChartsProvider(ScriptChartsProvider provider) {
        mScriptChartsProviderMap.remove(provider.getScriptAccount().getName());
    }

    public Map<String, ScriptChartsProvider> getAllScriptChartsProvider() {
        return mScriptChartsProviderMap;
    }

    public ScriptChartsProvider getScriptChartsProvider(String chartsProviderId) {
        return mScriptChartsProviderMap.get(chartsProviderId);
    }

}
