package org.tomahawk.libtomahawk.infosystem.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetSearches;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SearchesDeserializer extends JsonDeserializer<Map<String, HatchetSearches>> {

    @Override
    public Map<String, HatchetSearches> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        List<HatchetSearches> list = jp.readValueAs(new TypeReference<List<HatchetSearches>>() {
        });
        return DeserializerUtils.listToMap(list);
    }
}
