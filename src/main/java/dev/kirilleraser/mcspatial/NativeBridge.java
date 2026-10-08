package dev.kirilleraser.mcspatial;

/** JNI entry points. Never called from the native audio render thread. */
public final class NativeBridge {
    public static final int ABI_VERSION = 1;
    private NativeBridge() {}

    public static native int abiVersion();
    /** Start the output graph with silence; attach OpenAL only after its context is initialized. */
    public static native long create(long renderFunction, boolean headTrackingRequested);
    public static native void attach(long handle, long loopbackDevice);
    /** Synchronously stops/disposes audio units before returning. */
    public static native void destroy(long handle);
    public static native String lastError();
    public static native String diagnostics(long handle);
}
