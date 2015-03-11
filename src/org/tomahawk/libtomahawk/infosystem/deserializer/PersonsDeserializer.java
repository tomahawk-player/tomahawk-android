package org.tomahawk.libtomahawk.infosystem.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPersonInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PersonsDeserializer extends JsonDeserializer<Map<String, HatchetPersonInfo>> {

    @Override
    public Map<String, HatchetPersonInfo> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        List<HatchetPersonInfo> list =
                jp.readValueAs(new TypeReference<List<HatchetPersonInfo>>() {
                });
        return DeserializerUtils.listToMap(list);
    }
}
