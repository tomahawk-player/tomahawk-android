package org.tomahawk.libtomahawk.infosystem.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetArtistInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ArtistsDeserializer extends JsonDeserializer<Map<String, HatchetArtistInfo>> {

    @Override
    public Map<String, HatchetArtistInfo> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        List<HatchetArtistInfo> list = jp.readValueAs(new TypeReference<List<HatchetArtistInfo>>() {
        });
        return DeserializerUtils.listToMap(list);
    }
}
