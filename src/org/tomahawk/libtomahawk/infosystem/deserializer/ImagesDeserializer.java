package org.tomahawk.libtomahawk.infosystem.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetImage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ImagesDeserializer extends JsonDeserializer<Map<String, HatchetImage>> {

    @Override
    public Map<String, HatchetImage> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        List<HatchetImage> list = jp.readValueAs(new TypeReference<List<HatchetImage>>() {
        });
        return DeserializerUtils.listToMap(list);
    }
}
