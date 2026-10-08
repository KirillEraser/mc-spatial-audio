package dev.kirilleraser.mcspatial;

import net.neoforged.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persisted user settings; native output is rebuilt only when the user applies/reloads audio. */
public record SpatialSettings(boolean enabled, boolean headTrackingRequested) {
    public static final SpatialSettings DEFAULTS = new SpatialSettings(false, true);
    private static SpatialSettings stored;

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("mc-spatial-audio.properties");
    }

    public static synchronized SpatialSettings current() {
        if (stored == null) {
            try {
                stored = SettingsFile.read(path());
                if (!Files.exists(path())) SettingsFile.write(path(), stored);
            } catch (IOException failure) {
                SpatialAudioMod.LOG.warn("Could not read settings; native routing remains disabled", failure);
                stored = DEFAULTS;
            }
        }
        String override = System.getProperty("mcspatial.enabled");
        return override == null ? stored : new SpatialSettings(Boolean.parseBoolean(override), stored.headTrackingRequested());
    }

    public static boolean enabledOverridden() {
        return System.getProperty("mcspatial.enabled") != null;
    }

    public static synchronized void save(SpatialSettings settings) throws IOException {
        SettingsFile.write(path(), settings);
        stored = settings; // Do not update runtime settings if persistence failed.
    }
}
