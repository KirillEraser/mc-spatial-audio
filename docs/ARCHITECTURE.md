# Architecture and honest boundaries

## Implemented source

```
Minecraft sound sources, buffers, listener and attenuation
                    |
Sound Physics Remastered EFX (if installed)
                    |
OpenAL Soft loopback: interleaved float, 48 kHz, 7.1
                    |
JNI passes the SAME OpenAL library's alcRenderSamplesSOFT pointer
                    |
Native AudioUnit input callback pulls PCM directly from OpenAL
                    |
Channel swizzle: OpenAL's rear/side order -> Apple's MPEG_7_1_C
                    |
AUSpatialMixer: ambience bed, headphone output, head-tracking request
                    |
AUConverter: stereo 48 kHz -> hardware sample rate
                    |
HALOutput -> macOS default output device
```

This is output routing, not a rewrite of source management or acoustics. It intentionally does not link Apple's deprecated OpenAL framework or ship a second copy of OpenAL Soft.

### Why a companion mod, not an upstream fork?

The new repository was empty. The prototype is a small companion mod so Sound Physics Remastered can remain separately installed and updated. Its 1.21.1 `LibraryMixin` modifies the ALC context's auxiliary-send count; our lower-priority context-argument modification preserves unknown attributes, including that count. No Sound Physics Remastered source or ray tracer was copied. Unit tests cover preservation of the send count, not actual two-mod runtime compatibility.

### Lifecycle

1. A macOS-only mixin intercepts Minecraft's OpenAL device opening. Non-macOS clients do not apply this mixin.
2. Disabled configuration uses the original `alcOpenDevice` path.
3. Enabled configuration checks loopback format support and prepares/starts the Apple graph with SILENCE. Failure here closes the candidate loopback device and opens the ordinary device.
4. Minecraft creates its OpenAL context. The mod merges the existing ALC attributes with 48 kHz float 7.1 and disables OpenAL HRTF to avoid a binaural mix being spatialized again.
5. At the end of successful library initialization, native code receives the loopback device. Only then can the audio callback render OpenAL PCM.
6. Before Minecraft destroys its contexts, the mod stops/disposes the native graph. The device-close hooks are an additional guard. A JVM shutdown hook stops the native graph but does not take over Minecraft's OpenAL ownership.

Preflight success is deliberately separate from Minecraft initialization success. The latter can still fail. No claim of automatic fallback after loopback adoption is made.

### Threading and real-time limits

The audio callback never invokes JNI/Java, writes logs, allocates bridge sample buffers, or intentionally acquires a bridge mutex. Its sample storage is allocated before output starts. Java/JNI lifecycle calls are serialized by `AudioRouter`. Native callback state uses atomics. Core Audio is stopped/disposed before callback state is freed.

OpenAL Soft itself can lock and do internal work during rendering; this is not a guarantee of hard real-time behavior. Rendering errors produce silence and counters, not automatic device recovery. Runtime performance and teardown race tests remain mandatory.

## What the Apple API results mean

The backend requests `kAudioUnitProperty_SpatialMixerEnableHeadTracking` and records the property readback. `1` means the mixer retained the request, **not that AirPods are connected, their pose is being consumed, or macOS offers Head Tracked controls for this process**.

The backend also inspects the current process's `com.apple.developer.coremotion.head-pose` entitlement for diagnostics. Absence is reported, not hidden or repaired. It does not add entitlements, present its own spatial-audio controls, or claim that a successful unsigned-probe run satisfies signing requirements.

Control Center/menu-bar eligibility is not implemented by a fake icon or a fabricated status flag. The actual acceptance criterion is a system control entry associated with the Minecraft audio process, plus independently observed head tracking. The separate probe has its own bundle identifier, so success there is necessary evidence about the native graph but insufficient evidence about Minecraft.

## Known constraints

- This is a 7.1 horizontal bed, not Apple's per-source/object renderer. Vertical positioning loses fidelity.
- All mixed audio, including stereo music and UI sounds, passes through one bed. Per-source exemptions are not implemented.
- The default macOS output at graph creation wins. Minecraft's OpenAL output-device setting does not select the native HAL output.
- Device changes and disconnections need a resource reload or restart. Diagnostics flag a changed default device; there is no native listener/rebuild loop.
- The probe is a standalone signal generator. No helper-process IPC transport is implemented.
- The packaged dylib is locally ad-hoc signed. That does not grant entitlements to Java, satisfy hardened-runtime library validation, or replace production signing/notarization.
- No automatic mutation of Java, launchers, system preferences, or user permissions.
- Mixin compatibility on Minecraft 1.21.1 and coexistence with other output mods require a real client launch. Compilation alone does not validate invocation injection points.

## Next stage if the JVM is the blocker

If the signed probe can track but the launcher/JVM cannot, do not keep patching the JVM blindly. Design a properly signed helper app with a bounded PCM IPC transport, device/permission management, process identity, and measured latency. Before building that, decide whether controls associated with the helper satisfy the user's requirement; they would not automatically become controls labelled Minecraft. That architecture is not part of this prototype.

## Primary references

- Apple's multichannel spatial audio sample: https://developer.apple.com/documentation/audiotoolbox/generating-spatial-audio-from-a-multichannel-audio-stream
- Spatial mixer head-tracking property: https://developer.apple.com/documentation/audiotoolbox/kaudiounitproperty_spatialmixerenableheadtracking
- Head Pose entitlement: https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.developer.coremotion.head-pose
- OpenAL loopback contract: https://openal-soft.org/openal-extensions/SOFT_loopback.txt
- Upstream EFX context integration: https://github.com/henkelmax/sound-physics-remastered/blob/1.21.1/common/src/main/java/com/sonicether/soundphysics/mixin/LibraryMixin.java
- LWJGL loopback bindings: https://github.com/LWJGL/lwjgl3/blob/3.3.3/modules/lwjgl/openal/src/generated/java/org/lwjgl/openal/SOFTLoopback.java
