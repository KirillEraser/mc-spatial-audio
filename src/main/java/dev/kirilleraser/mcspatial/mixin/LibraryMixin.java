package dev.kirilleraser.mcspatial.mixin;

import com.mojang.blaze3d.audio.Library;
import dev.kirilleraser.mcspatial.AudioRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import java.nio.IntBuffer;

/** Runs after Sound Physics Remastered's default priority, preserving its EFX context attributes. */
@Mixin(value = Library.class, priority = 900)
public abstract class LibraryMixin {
    @Shadow private long currentDevice;

    @Redirect(method = "tryOpenDevice", at = @At(value = "INVOKE",
        target = "Lorg/lwjgl/openal/ALC10;alcOpenDevice(Ljava/lang/CharSequence;)J"))
    private static long mcspatial$open(CharSequence deviceName) {
        return AudioRouter.openDevice(deviceName);
    }

    @Inject(method = "setHrtf(Z)V", at = @At("HEAD"), cancellable = true)
    private void mcspatial$nativeHeadphones(boolean directionalAudio, CallbackInfo callback) {
        // Vanilla resets the device with playback/HRTF-only attributes. A loopback device needs
        // its explicit render format; Apple, not OpenAL, performs our headphone spatialization.
        // Leave the vanilla path entirely unchanged for ordinary playback devices.
        if (AudioRouter.isLoopback(currentDevice)) callback.cancel();
    }

    @ModifyArgs(method = "init", at = @At(value = "INVOKE",
        target = "Lorg/lwjgl/openal/ALC10;alcCreateContext(JLjava/nio/IntBuffer;)J"))
    private void mcspatial$format(Args args) {
        long device = args.get(0);
        if (AudioRouter.isLoopback(device)) {
            args.set(1, AudioRouter.contextAttributes(device, (IntBuffer) args.get(1)));
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void mcspatial$attach(CallbackInfo callback) { AudioRouter.onLibraryInitialized(); }

    @Inject(method = "cleanup", at = @At("HEAD"))
    private void mcspatial$stop(CallbackInfo callback) { AudioRouter.stopNative(); }

    @Redirect(method = "cleanup", at = @At(value = "INVOKE",
        target = "Lorg/lwjgl/openal/ALC10;alcCloseDevice(J)Z"))
    private boolean mcspatial$close(long device) { return AudioRouter.closeDevice(device); }

    @Redirect(method = "tryOpenDevice", require = 0, at = @At(value = "INVOKE",
        target = "Lorg/lwjgl/openal/ALC10;alcCloseDevice(J)Z"))
    private static boolean mcspatial$closeRejected(long device) { return AudioRouter.closeDevice(device); }
}
