package org.tomahawk.libtomahawk.infosystem.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PlaylistsDeserializer extends JsonDeserializer<Map<String, HatchetPlaylistInfo>> {

    @Override
    public Map<String, HatchetPlaylistInfo> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        List<HatchetPlaylistInfo> list =
                jp.readValueAs(new TypeReference<List<HatchetPlaylistInfo>>() {
                });
        return DeserializerUtils.listToMap(list);
    }
}
