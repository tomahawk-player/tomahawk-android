package org.tomahawk.libtomahawk.infosystem.deserializer;

import org.tomahawk.libtomahawk.infosystem.hatchet.Mappable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeserializerUtils {

    public static <T extends Mappable> Map<String, T> listToMap(List<T> list) {
        Map<String, T> map = new HashMap<String, T>();
        for (T item : list) {
            map.put(item.id, item);
        }
        return map;
    }
}
