package dev.kirilleraser.mcspatial;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.SOFTHRTF;
import org.lwjgl.openal.SOFTLoopback;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/** Singleton matching Minecraft's one client sound library. Native handles are serialized here. */
public final class AudioRouter {
    public static final int SAMPLE_RATE = 48_000;
    private static long loopbackDevice;
    private static long nativeHandle;
    private static boolean failedForSession;
    private static boolean shutdownHookInstalled;
    private static String lastStatus = "Native route not started (disabled by default).";
    private AudioRouter() {}

    public static synchronized long openDevice(CharSequence requestedName) {
        SpatialSettings settings = SpatialSettings.current();
        if (!settings.enabled() || failedForSession) return ALC10.alcOpenDevice(requestedName);
        if (loopbackDevice != 0) {
            throw new IllegalStateException("Multiple concurrent Minecraft sound libraries are unsupported");
        }
        long candidate = 0;
        long candidateHandle = 0;
        try {
            // Global capabilities remain valid independently of a previously closed device.
            ALCCapabilities global = ALC.createCapabilities(0);
            if (!global.ALC_SOFT_loopback) throw new IllegalStateException("OpenAL lacks ALC_SOFT_loopback");
            candidate = SOFTLoopback.alcLoopbackOpenDeviceSOFT((ByteBuffer) null);
            if (candidate == 0) throw new IllegalStateException("Could not open OpenAL loopback device");
            ALCCapabilities capabilities = ALC.createCapabilities(candidate);
            if (!capabilities.ALC_SOFT_loopback || capabilities.alcRenderSamplesSOFT == 0) {
                throw new IllegalStateException("Loopback rendering entry point unavailable");
            }
            if (!SOFTLoopback.alcIsRenderFormatSupportedSOFT(candidate, SAMPLE_RATE,
                    SOFTLoopback.ALC_7POINT1_SOFT, SOFTLoopback.ALC_FLOAT_SOFT)) {
                throw new IllegalStateException("OpenAL cannot render 48 kHz float 7.1");
            }
            NativeLibrary.load();
            candidateHandle = NativeBridge.create(capabilities.alcRenderSamplesSOFT, settings.headTrackingRequested());
            if (candidateHandle == 0) throw new IllegalStateException(NativeBridge.lastError());
            if (!shutdownHookInstalled) {
                Runtime.getRuntime().addShutdownHook(new Thread(AudioRouter::stopNative, "mcspatial-shutdown"));
                shutdownHookInstalled = true;
            }
            loopbackDevice = candidate;
            nativeHandle = candidateHandle;
            lastStatus = "Native output preflight succeeded; awaiting Minecraft's context. Tracking/control UI unverified.";
            SpatialAudioMod.LOG.info("{} {}", lastStatus, NativeBridge.diagnostics(nativeHandle));
            return candidate;
        } catch (Exception | LinkageError failure) {
            if (candidateHandle != 0) NativeBridge.destroy(candidateHandle);
            if (candidate != 0) ALC10.alcCloseDevice(candidate);
            ALC.setCapabilities(null);
            failedForSession = true;
            lastStatus = "Preflight failed; ordinary OpenAL output retained: " + failure.getMessage();
            SpatialAudioMod.LOG.warn(lastStatus, failure);
            return ALC10.alcOpenDevice(requestedName);
        }
    }

    public static synchronized boolean isLoopback(long device) {
        return device != 0 && device == loopbackDevice;
    }

    public static synchronized IntBuffer contextAttributes(long device, IntBuffer original) {
        if (!isLoopback(device)) return original;
        int[] input = null;
        if (original != null) {
            IntBuffer copy = original.duplicate();
            input = new int[copy.remaining()];
            copy.get(input);
        }
        int[] merged = ContextAttributes.replace(input, new int[] {
            ALC10.ALC_FREQUENCY, SAMPLE_RATE,
            SOFTLoopback.ALC_FORMAT_CHANNELS_SOFT, SOFTLoopback.ALC_7POINT1_SOFT,
            SOFTLoopback.ALC_FORMAT_TYPE_SOFT, SOFTLoopback.ALC_FLOAT_SOFT,
            SOFTHRTF.ALC_HRTF_SOFT, ALC10.ALC_FALSE, 0
        });
        // GC-managed direct storage: do not return memory from a popped MemoryStack.
        return BufferUtils.createIntBuffer(merged.length).put(merged).flip();
    }

    public static synchronized void onLibraryInitialized() {
        if (nativeHandle == 0 || loopbackDevice == 0) return;
        long context = ALC10.alcGetCurrentContext();
        if (context == 0 || ALC10.alcGetContextsDevice(context) != loopbackDevice) {
            stopNative();
            lastStatus = "No matching current OpenAL context. Disable the prototype and restart.";
            SpatialAudioMod.LOG.error(lastStatus);
            return;
        }
        NativeBridge.attach(nativeHandle, loopbackDevice);
        lastStatus = "7.1 native route attached. Requested head tracking is NOT proof of active tracking or macOS controls.";
        SpatialAudioMod.LOG.info("{} {}", lastStatus, NativeBridge.diagnostics(nativeHandle));
    }

    /** Called before Minecraft destroys contexts, and again safely when the device closes. */
    public static synchronized void stopNative() {
        if (nativeHandle != 0) {
            NativeBridge.destroy(nativeHandle);
            nativeHandle = 0;
        }
    }

    public static synchronized boolean closeDevice(long device) {
        if (isLoopback(device)) {
            stopNative();
            loopbackDevice = 0;
            lastStatus = "Native route stopped; sound-library reload may reopen it.";
        }
        return ALC10.alcCloseDevice(device);
    }

    public static synchronized String status() {
        return "enabled=" + SpatialSettings.current().enabled() + "; " + lastStatus
            + (nativeHandle == 0 ? "" : "\n" + NativeBridge.diagnostics(nativeHandle));
    }
}
