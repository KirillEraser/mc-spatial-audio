import dev.kirilleraser.mcspatial.NativeBridge;
import java.nio.file.Path;

/** Load the compiled JNI library without opening an audio device or pretending to test AirPods. */
public final class NativeAbiSmoke {
    public static void main(String[] arguments) {
        if (arguments.length != 1) throw new IllegalArgumentException("Provide the native library path");
        System.load(Path.of(arguments[0]).toAbsolutePath().toString());
        int actual = NativeBridge.abiVersion();
        if (actual != NativeBridge.ABI_VERSION) {
            throw new AssertionError("JNI ABI mismatch: expected " + NativeBridge.ABI_VERSION + ", got " + actual);
        }
        String status = NativeBridge.diagnostics(0);
        if (status == null || status.isBlank()) throw new AssertionError("JNI diagnostic string is missing");
        System.out.println("JNI library loaded; ABI " + actual + "; " + status);
        System.out.println("No audio device, head tracking, or macOS controls were tested by this smoke check.");
    }
}
