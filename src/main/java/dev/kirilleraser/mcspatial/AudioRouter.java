package dev.kirilleraser.mcspatial;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.SOFTHRTF;
import org.lwjgl.openal.SOFTLoopback;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/** One native output graph for Minecraft's one client sound library. */
public final class AudioRouter {
    public static final int SAMPLE_RATE = 48_000;
    private enum Phase { IDLE, PREPARED, ACTIVE, FALLBACK }
    private static Phase phase = Phase.IDLE;
    private static long loopbackDevice;
    private static long nativeHandle;
    private static boolean failedForSession;
    private static boolean shutdownHookInstalled;
    private static String failureReason = "";
    private AudioRouter() {}

    public static synchronized long openDevice(CharSequence requestedName) {
        SpatialSettings settings = SpatialSettings.current();
        if (!settings.enabled() || failedForSession) return ALC10.alcOpenDevice(requestedName);
        if (loopbackDevice != 0) throw new IllegalStateException("Concurrent sound libraries are unsupported");
        long candidate = 0;
        long candidateHandle = 0;
        try {
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
            phase = Phase.PREPARED;
            failureReason = "";
            SpatialAudioMod.LOG.info("Prepared native 7.1 output at {} Hz", SAMPLE_RATE);
            return candidate;
        } catch (Exception | LinkageError failure) {
            if (candidateHandle != 0) NativeBridge.destroy(candidateHandle);
            if (candidate != 0) ALC10.alcCloseDevice(candidate);
            loopbackDevice = 0;
            nativeHandle = 0;
            ALC.setCapabilities(null);
            failedForSession = true;
            phase = Phase.FALLBACK;
            failureReason = String.valueOf(failure.getMessage());
            SpatialAudioMod.LOG.warn("Native output unavailable; using ordinary OpenAL: {}", failureReason, failure);
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
        return BufferUtils.createIntBuffer(merged.length).put(merged).flip();
    }

    public static synchronized void onLibraryInitialized() {
        if (nativeHandle == 0 || loopbackDevice == 0) return;
        long context = ALC10.alcGetCurrentContext();
        if (context == 0 || ALC10.alcGetContextsDevice(context) != loopbackDevice) {
            stopNative();
            phase = Phase.FALLBACK;
            failedForSession = true;
            failureReason = "No matching current OpenAL context; reload audio or disable native output";
            SpatialAudioMod.LOG.error(failureReason);
            return;
        }
        NativeBridge.attach(nativeHandle, loopbackDevice);
        phase = Phase.ACTIVE;
        SpatialAudioMod.LOG.info("Native 7.1 output attached; Apple head tracking requested={}", SpatialSettings.current().headTrackingRequested());
        if (SpatialAudioMod.LOG.isDebugEnabled()) SpatialAudioMod.LOG.debug("{}", NativeBridge.diagnostics(nativeHandle));
    }

    public static synchronized void stopNative() {
        if (nativeHandle != 0) {
            NativeBridge.destroy(nativeHandle);
            nativeHandle = 0;
        }
        if (phase != Phase.FALLBACK) phase = Phase.IDLE;
    }

    public static synchronized boolean closeDevice(long device) {
        if (isLoopback(device)) {
            stopNative();
            loopbackDevice = 0;
        }
        return ALC10.alcCloseDevice(device);
    }

    /** Call before an explicit client-thread reload, never while holding the lock through SoundManager.reload. */
    public static synchronized void prepareForReload() {
        failedForSession = false;
        failureReason = "";
    }

    public static synchronized String summary() {
        if (!SpatialAudioMod.isMac()) return "macOS only; ordinary audio output";
        if (!SpatialSettings.current().enabled() && nativeHandle == 0) return "Disabled; ordinary audio output";
        return switch (phase) {
            case IDLE -> "Native audio not started";
            case PREPARED -> "Starting native audio";
            case ACTIVE -> SpatialSettings.current().headTrackingRequested()
                ? "Native 7.1 output; head tracking requested" : "Native 7.1 output; fixed spatialization";
            case FALLBACK -> "Native output unavailable; see diagnostics";
        };
    }

    public static synchronized String status() {
        SpatialSettings settings = SpatialSettings.current();
        return summary() + "\nenabled=" + settings.enabled() + "; headTrackingRequested=" + settings.headTrackingRequested()
            + "; phase=" + phase + (failureReason.isBlank() ? "" : "; reason=" + failureReason)
            + (nativeHandle == 0 ? "" : "\n" + NativeBridge.diagnostics(nativeHandle));
    }
}
