package org.tomahawk.libtomahawk.infosystem.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetUserInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class UsersDeserializer extends JsonDeserializer<Map<String, HatchetUserInfo>> {

    @Override
    public Map<String, HatchetUserInfo> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        List<HatchetUserInfo> list = jp.readValueAs(new TypeReference<List<HatchetUserInfo>>() {
        });
        return DeserializerUtils.listToMap(list);
    }
}
