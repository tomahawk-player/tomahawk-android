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
package org.tomahawk.libtomahawk.resolver;

import com.google.gson.JsonObject;

import org.tomahawk.libtomahawk.utils.GsonHelper;

import java.lang.ref.WeakReference;

public class ScriptObject {

    public static final String TYPE_RESOLVER = "resolver";

    public static final String TYPE_INFOPLUGIN = "infoPlugin";

    public static final String TYPE_COLLECTION = "collection";

    public static final String TYPE_CHARTSPROVIDER = "chartsProvider";

    public static final String TYPE_PLAYLISTGENERATOR = "playlistGenerator";

    private String mId;

    private ScriptAccount mScriptAccount;

    private WeakReference<ScriptPlugin> mScriptPlugin;

    public ScriptObject(String id, ScriptAccount account) {
        mId = id;
        mScriptAccount = account;
    }

    public String getId() {
        return mId;
    }

    public ScriptAccount getScriptAccount() {
        return mScriptAccount;
    }

    public ScriptPlugin getScriptPlugin() {
        return mScriptPlugin.get();
    }

    public void setScriptPlugin(ScriptPlugin scriptPlugin) {
        mScriptPlugin = new WeakReference<>(scriptPlugin);
    }

    public String toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("id", mId);
        return GsonHelper.get().toJson(object);
    }

}
