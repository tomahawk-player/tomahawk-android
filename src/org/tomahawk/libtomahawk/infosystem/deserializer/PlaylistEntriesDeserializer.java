package org.tomahawk.libtomahawk.infosystem.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistEntryInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PlaylistEntriesDeserializer
        extends JsonDeserializer<Map<String, HatchetPlaylistEntryInfo>> {

    @Override
    public Map<String, HatchetPlaylistEntryInfo> deserialize(JsonParser jp,
            DeserializationContext ctxt)
            throws IOException {
        List<HatchetPlaylistEntryInfo> list =
                jp.readValueAs(new TypeReference<List<HatchetPlaylistEntryInfo>>() {
                });
        return DeserializerUtils.listToMap(list);
    }
}
