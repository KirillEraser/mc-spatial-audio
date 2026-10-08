package dev.kirilleraser.mcspatial;

import net.neoforged.fml.loading.FMLPaths;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Read once per JVM: output reconfiguration during playback is intentionally unsupported. */
public record SpatialSettings(boolean enabled, boolean headTrackingRequested) {
    private static SpatialSettings cached;

    public static synchronized SpatialSettings current() {
        if (cached != null) return cached;
        Properties properties = new Properties();
        Path path = FMLPaths.CONFIGDIR.get().resolve("mc-spatial-audio.properties");
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                Files.writeString(path, "# Experimental; quit Minecraft before editing.\n"
                    + "# Set enabled=true only after reading the hardware test plan.\n"
                    + "enabled=false\nheadTrackingRequested=true\n", StandardCharsets.UTF_8);
            }
            try (InputStream input = Files.newInputStream(path)) { properties.load(input); }
            String enabled = System.getProperty("mcspatial.enabled", properties.getProperty("enabled", "false"));
            cached = new SpatialSettings(Boolean.parseBoolean(enabled),
                Boolean.parseBoolean(properties.getProperty("headTrackingRequested", "true")));
        } catch (IOException | IllegalArgumentException failure) {
            SpatialAudioMod.LOG.warn("Could not read settings; native routing remains disabled", failure);
            cached = new SpatialSettings(false, true);
        }
        return cached;
    }
}
