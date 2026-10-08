package dev.kirilleraser.mcspatial;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class NativeLibrary {
    private static boolean loaded;
    private NativeLibrary() {}

    public static synchronized void load() throws IOException {
        if (loaded) return;
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")) {
            throw new IOException("Apple audio backend is macOS-only");
        }
        String override = System.getProperty("mcspatial.nativeLibrary");
        Path library;
        if (override != null && !override.isBlank()) {
            library = Path.of(override).toRealPath();
        } else {
            String resource = "/natives/macos/universal/libmcspatial.dylib";
            try (InputStream input = NativeLibrary.class.getResourceAsStream(resource)) {
                if (input == null) throw new IOException("Native library missing. Build/stage the macOS backend first.");
                Path directory = Files.createTempDirectory("mcspatial-");
                library = directory.resolve("libmcspatial.dylib");
                Files.copy(input, library, StandardCopyOption.REPLACE_EXISTING);
                // deleteOnExit runs in reverse registration order.
                directory.toFile().deleteOnExit();
                library.toFile().deleteOnExit();
            }
        }
        System.load(library.toAbsolutePath().toString());
        if (NativeBridge.abiVersion() != NativeBridge.ABI_VERSION) {
            throw new IOException("Native bridge ABI mismatch");
        }
        loaded = true;
    }
}
