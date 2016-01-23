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
package org.tomahawk.libtomahawk.resolver.plugins;

import org.tomahawk.libtomahawk.resolver.ScriptAccount;
import org.tomahawk.libtomahawk.resolver.ScriptObject;

import java.util.HashMap;

public abstract class ScriptPluginFactory<T> {

    private HashMap<String, T> mScriptPlugins = new HashMap<>();

    public void registerPlugin(ScriptObject object, ScriptAccount account) {
        if (!mScriptPlugins.containsKey(object.getId())) {
            T scriptPlugin = createPlugin(object, account);
            if (scriptPlugin != null) {
                mScriptPlugins.put(object.getId(), scriptPlugin);
            }
            addPlugin(scriptPlugin);
        }
    }

    public void unregisterPlugin(ScriptObject object) {
        T scriptPlugin = mScriptPlugins.get(object.getId());
        if (scriptPlugin != null) {
            removePlugin(scriptPlugin);
            mScriptPlugins.remove(object.getId());
        }
    }

    public abstract T createPlugin(ScriptObject object, ScriptAccount account);

    public void addAllPlugins() {
        for (T plugin : mScriptPlugins.values()) {
            addPlugin(plugin);
        }
    }

    public abstract void addPlugin(T scriptPlugin);

    public void removeAllPlugins() {
        for (T plugin : mScriptPlugins.values()) {
            removePlugin(plugin);
        }
    }

    public abstract void removePlugin(T scriptPlugin);

    public HashMap<String, T> getScriptPlugins() {
        return mScriptPlugins;
    }
}
