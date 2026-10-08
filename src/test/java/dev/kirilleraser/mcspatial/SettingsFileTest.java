package dev.kirilleraser.mcspatial;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class SettingsFileTest {
    @TempDir Path directory;

    @Test void missingFileUsesSafeDefaults() throws IOException {
        assertEquals(SpatialSettings.DEFAULTS, SettingsFile.read(directory.resolve("settings.properties")));
    }

    @Test void readsExistingAlphaConfigurationWithoutMigration() throws IOException {
        Path path = directory.resolve("settings.properties");
        Files.writeString(path, "# Existing user config\nenabled=true\nheadTrackingRequested=true\n");
        assertEquals(new SpatialSettings(true, true), SettingsFile.read(path));
    }

    @Test void roundTripsAndPreservesUnknownKeys() throws IOException {
        Path path = directory.resolve("settings.properties");
        Files.writeString(path, "futureSetting=keep-me\nenabled=false\nheadTrackingRequested=true\n");
        SettingsFile.write(path, new SpatialSettings(true, false));
        assertEquals(new SpatialSettings(true, false), SettingsFile.read(path));
        assertTrue(Files.readString(path).contains("futureSetting=keep-me"));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count(), "Temporary files must not be left behind");
        }
    }

    @Test void createsMissingParentDirectories() throws IOException {
        Path path = directory.resolve("config/nested/settings.properties");
        SettingsFile.write(path, new SpatialSettings(false, false));
        assertEquals(new SpatialSettings(false, false), SettingsFile.read(path));
    }

    @Test void rejectsMalformedBooleanRatherThanSilentlyEnabling() throws IOException {
        Path path = directory.resolve("settings.properties");
        Files.writeString(path, "enabled=maybe\n");
        assertThrows(IOException.class, () -> SettingsFile.read(path));
    }

    @Test void whitespaceAndCaseAreAccepted() throws IOException {
        Path path = directory.resolve("settings.properties");
        Files.writeString(path, "enabled= TRUE \nheadTrackingRequested= False \n");
        assertEquals(new SpatialSettings(true, false), SettingsFile.read(path));
    }
}
