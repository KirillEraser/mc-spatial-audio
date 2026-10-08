package dev.kirilleraser.mcspatial;

import java.util.LinkedHashMap;
import java.util.Map;

/** Merge ALC key/value lists without discarding other mods' EFX attributes. */
public final class ContextAttributes {
    private ContextAttributes() {}

    public static int[] replace(int[] original, int[] overrides) {
        Map<Integer, Integer> attributes = new LinkedHashMap<>();
        read(attributes, original);
        read(attributes, overrides);
        int[] result = new int[attributes.size() * 2 + 1];
        int offset = 0;
        for (Map.Entry<Integer, Integer> entry : attributes.entrySet()) {
            result[offset++] = entry.getKey();
            result[offset++] = entry.getValue();
        }
        // Java initializes the final terminator to zero.
        return result;
    }

    private static void read(Map<Integer, Integer> attributes, int[] list) {
        if (list == null) return;
        for (int i = 0; i < list.length && list[i] != 0; i += 2) {
            if (i + 1 >= list.length) throw new IllegalArgumentException("Incomplete ALC attribute pair");
            attributes.put(list[i], list[i + 1]);
        }
    }
}
