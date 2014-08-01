package org.tomahawk.libtomahawk.infosystem.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetSocialAction;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SocialActionsDeserializer extends JsonDeserializer<Map<String, HatchetSocialAction>> {

    @Override
    public Map<String, HatchetSocialAction> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        List<HatchetSocialAction> list = jp
                .readValueAs(new TypeReference<List<HatchetSocialAction>>() {
                });
        return DeserializerUtils.listToMap(list);
    }
}
