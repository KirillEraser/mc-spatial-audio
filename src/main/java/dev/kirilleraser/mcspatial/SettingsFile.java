package dev.kirilleraser.mcspatial;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** Pure Java persistence, independently testable without starting Minecraft. */
public final class SettingsFile {
    private SettingsFile() {}

    public static SpatialSettings read(Path path) throws IOException {
        Properties properties = load(path);
        return new SpatialSettings(
            bool(properties, "enabled", SpatialSettings.DEFAULTS.enabled()),
            bool(properties, "headTrackingRequested", SpatialSettings.DEFAULTS.headTrackingRequested()));
    }

    private static boolean bool(Properties properties, String key, boolean fallback) throws IOException {
        String value = properties.getProperty(key);
        if (value == null) return fallback;
        if (value.trim().equalsIgnoreCase("true")) return true;
        if (value.trim().equalsIgnoreCase("false")) return false;
        throw new IOException("Invalid boolean setting: " + key);
    }

    private static Properties load(Path path) throws IOException {
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IllegalArgumentException malformed) {
                throw new IOException("Malformed settings file", malformed);
            }
        }
        return properties;
    }

    public static void write(Path path, SpatialSettings settings) throws IOException {
        Path target = path.toAbsolutePath();
        if (Files.exists(target)) target = target.toRealPath(); // Preserve a user's existing config symlink.
        Properties properties = load(target);
        properties.setProperty("enabled", Boolean.toString(settings.enabled()));
        properties.setProperty("headTrackingRequested", Boolean.toString(settings.headTrackingRequested()));
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "mcspatial-settings-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                properties.store(writer, "MC Spatial Audio — edit through Mods > MC Spatial Audio > Config");
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unavailable) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
