package org.tomahawk.libtomahawk.infosystem.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetChart;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ChartsDeserializer extends JsonDeserializer<Map<String, HatchetChart>> {

    @Override
    public Map<String, HatchetChart> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        List<HatchetChart> list = jp.readValueAs(new TypeReference<List<HatchetChart>>() {
        });
        return DeserializerUtils.listToMap(list);
    }
}
