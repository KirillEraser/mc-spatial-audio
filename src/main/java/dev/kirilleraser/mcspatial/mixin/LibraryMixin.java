package dev.kirilleraser.mcspatial.mixin;

import com.mojang.blaze3d.audio.Library;
import dev.kirilleraser.mcspatial.AudioRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import java.nio.IntBuffer;

/** Lower priority than Sound Physics Remastered's default 1000: merge its EFX attributes. */
@Mixin(value = Library.class, priority = 900)
public abstract class LibraryMixin {
    @Redirect(method = "tryOpenDevice", at = @At(value = "INVOKE",
        target = "Lorg/lwjgl/openal/ALC10;alcOpenDevice(Ljava/lang/CharSequence;)J"))
    private static long mcspatial$open(CharSequence deviceName) {
        return AudioRouter.openDevice(deviceName);
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
    private void mcspatial$attach(String deviceName, boolean directionalAudio, CallbackInfo callback) {
        AudioRouter.onLibraryInitialized();
    }

    @Inject(method = "cleanup", at = @At("HEAD"))
    private void mcspatial$stop(CallbackInfo callback) {
        AudioRouter.stopNative();
    }

    @Redirect(method = "cleanup", at = @At(value = "INVOKE",
        target = "Lorg/lwjgl/openal/ALC10;alcCloseDevice(J)Z"))
    private boolean mcspatial$close(long device) { return AudioRouter.closeDevice(device); }

    // Vanilla can close a device when its initial ALC error check fails. This path is optional.
    @Redirect(method = "tryOpenDevice", require = 0, at = @At(value = "INVOKE",
        target = "Lorg/lwjgl/openal/ALC10;alcCloseDevice(J)Z"))
    private static boolean mcspatial$closeRejected(long device) { return AudioRouter.closeDevice(device); }
}
