package org.tomahawk.libtomahawk.infosystem.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaybackItemResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PlaybackItemsDeserializer
        extends JsonDeserializer<Map<String, HatchetPlaybackItemResponse>> {

    @Override
    public Map<String, HatchetPlaybackItemResponse> deserialize(JsonParser jp,
            DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        List<HatchetPlaybackItemResponse> list =
                jp.readValueAs(new TypeReference<List<HatchetPlaybackItemResponse>>() {
                });
        return DeserializerUtils.listToMap(list);
    }
}
