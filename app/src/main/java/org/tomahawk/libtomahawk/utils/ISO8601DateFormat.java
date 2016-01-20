package org.tomahawk.libtomahawk.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Date;

/**
 * Provide a fast thread-safe formatter/parser DateFormat for ISO8601 dates ONLY. It was mainly done
 * to be used with Jackson JSON Processor. <p/> Watch out for clone implementation that returns
 * itself. <p/> All other methods but parse and format and clone are undefined behavior.
 *
 * @see ISO8601Utils
 */
public class ISO8601DateFormat implements JsonDeserializer<Date>, JsonSerializer<Date> {

    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return ISO8601Utils.parse(json.getAsString());
    }

    @Override
    public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
        String value = ISO8601Utils.format(src);
        return new JsonPrimitive(value);
    }
}