package org.tomahawk.libtomahawk.infosystem.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetAlbumInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AlbumsDeserializer extends JsonDeserializer<Map<String,HatchetAlbumInfo>> {

    @Override
    public Map<String, HatchetAlbumInfo> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        List<HatchetAlbumInfo> list = jp.readValueAs(new TypeReference<List<HatchetAlbumInfo>>() {
        });
        return DeserializerUtils.listToMap(list);
    }
}
