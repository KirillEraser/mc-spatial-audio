# Backend

Minecraft still owns sound sources, listener pose, volumes, distance attenuation and playback. Sound Physics Remastered, when installed, still owns its acoustic simulation/EFX. This mod changes the final output route, not those systems.

## Render graph

1. Open Minecraft's existing OpenAL Soft library in loopback mode: 48 kHz, interleaved float, 7.1.
2. Preserve Minecraft/other mods' context attributes; add the explicit loopback format and disable OpenAL HRTF.
3. Pass the existing library's `alcRenderSamplesSOFT` pointer to JNI. Do not load a second OpenAL implementation.
4. Apple's input callback pulls PCM, converts channel order, and feeds one ambience bed to `AUSpatialMixer` with headphone output and `UseOutputType` spatialization.
5. `AUConverter` matches the output device's sample rate; `HALOutput` sends stereo to the macOS default device.

OpenAL channel order is **L R C LFE rear-L rear-R side-L side-R**. Apple's `MPEG_7_1_C` is **L R C LFE side-L side-R rear-L rear-R**. Only channels 4↔6 and 5↔7 are exchanged. Tests cover each channel, polarity, isolation, frame boundaries and unchanged amplitude/energy. No per-channel boost, loudness normalization or double headphone spatialization is added by the bridge. Apple's filters can legitimately affect perceived loudness by direction; impulse-routing tests are not a perceptual calibration.

## Head tracking and recentering

The mixer requests `kAudioUnitProperty_SpatialMixerEnableHeadTracking`. Apple owns the pose/reference-frame behavior. The mod neither integrates raw head motion nor applies its own orientation offset or recenter timeout. Exact stationary-head recentering must be compared on the real headset/OS; no duration is promised. Manual audio reload rebuilds the graph and can reset renderer state, so it must not be used as a recurring recenter mechanism.

The process Head Pose entitlement is diagnostic only, not a gate. The user reported working tracking with a readback of 1 and entitlement diagnostic of 0. Neither value is exposed as proof of observed tracking or macOS system-control eligibility.

## Lifecycle and settings

Native preflight starts with silence; the loopback device is attached only after Minecraft's context exists. Vanilla `setHrtf` is skipped only for our loopback device, preventing its playback-only reset from conflicting with the required loopback format. Ordinary OpenAL devices keep vanilla behavior.

Before context/device destruction, native output is stopped/disposed. Audio callbacks do not call Java, log, write files or allocate bridge buffers. OpenAL itself is not guaranteed hard-real-time. Settings retain the alpha properties format, save atomically, and apply via explicit client-thread audio reload. Preflight failure falls back; later render failures need explicit reload/disable. Device hot-switching is not seamless.

## References

- [Apple multichannel spatial audio sample](https://developer.apple.com/documentation/audiotoolbox/generating-spatial-audio-from-a-multichannel-audio-stream)
- [OpenAL loopback contract](https://openal-soft.org/openal-extensions/SOFT_loopback.txt)
- [OpenAL channel ordering](https://github.com/kcat/openal-soft/blob/master/alc/backends/base.cpp)
- Apple SDK `CoreAudioBaseTypes.h`: `kAudioChannelLayoutTag_MPEG_7_1_C`.
- [Sound Physics Remastered 1.21.1](https://github.com/henkelmax/sound-physics-remastered/tree/1.21.1)
