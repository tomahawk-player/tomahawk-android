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
package org.tomahawk.libtomahawk.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;

public class GsonHelper {

    private static Gson mGson;

    private static class CollectionAdapter implements JsonSerializer<Collection<?>> {

        @Override
        public JsonElement serialize(Collection<?> src, Type typeOfSrc,
                JsonSerializationContext context) {
            if (src == null || src.isEmpty()) // exclusion is made here
            {
                return null;
            }

            JsonArray array = new JsonArray();

            for (Object child : src) {
                JsonElement element = context.serialize(child);
                array.add(element);
            }

            return array;
        }
    }

    public static com.google.gson.Gson get() {
        if (mGson == null) {
            mGson = new GsonBuilder()
                    .registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter())
                    .registerTypeAdapter(Date.class, new ISO8601DateFormat())
                    .create();
        }
        return mGson;
    }

}
