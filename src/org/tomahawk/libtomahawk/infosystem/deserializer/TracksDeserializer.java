package org.tomahawk.libtomahawk.infosystem.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetTrackInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TracksDeserializer extends JsonDeserializer<Map<String, HatchetTrackInfo>> {

    @Override
    public Map<String, HatchetTrackInfo> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        List<HatchetTrackInfo> list = jp.readValueAs(new TypeReference<List<HatchetTrackInfo>>() {
        });
        return DeserializerUtils.listToMap(list);
    }
}
