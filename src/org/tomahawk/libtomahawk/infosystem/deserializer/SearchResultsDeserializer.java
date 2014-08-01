package org.tomahawk.libtomahawk.infosystem.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetSearchItem;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SearchResultsDeserializer extends JsonDeserializer<Map<String, HatchetSearchItem>> {

    @Override
    public Map<String, HatchetSearchItem> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        List<HatchetSearchItem> list = jp.readValueAs(new TypeReference<List<HatchetSearchItem>>() {
        });
        return DeserializerUtils.listToMap(list);
    }
}
