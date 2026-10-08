# MC Spatial Audio

Experimental **Minecraft 1.21.1 / NeoForge** client mod for routing Minecraft's existing OpenAL mix through Apple's native spatial mixer on macOS. No custom menu-bar icon and no replacement of Sound Physics Remastered's acoustics.

## Status — prototype, not a verified head-tracking release

The source implements an OpenAL Soft loopback output bridge, a JNI macOS backend, and an independent native test app. **A successful build, a successful head-tracking property write, and audible spatialized sound do not prove that macOS exposes its Head Tracked controls for Minecraft.** That is a separate hardware acceptance test. The prototype is disabled by default.

This repository has not yet been built against Minecraft or tested with AirPods by the authoring assistant. CI is provided to perform Java and native builds. See [the test plan](docs/TESTING.md) and [architecture and constraints](docs/ARCHITECTURE.md).

## Requirements

- Minecraft **1.21.1**, NeoForge **21.1.256 or later within 21.1**, Java **21**.
- macOS **13 or newer** for this prototype; an SDK exposing the spatial-mixer head-tracking property.
- For compiling the native component: Xcode command-line tools, CMake 3.24+, and JDK 21.
- AirPods Pro selected as the macOS output device for the intended hardware tests.
- Sound Physics Remastered's **1.21.1 NeoForge** build is optional and recommended. Install it separately; its source is not copied or redistributed here. Compatibility is a design goal, not yet a tested claim.

## Build

Install Gradle **8.14.3** and Java 21, then run from the checkout:

```sh
# On a Mac; builds both architectures and stages the dylib into the mod resources.
bash scripts/build-native.sh
# Tests and packages the client mod, including the staged dylib.
gradle clean build
```

The mod JAR is in `build/libs/`. Java-only builds on Linux/Windows are useful for checks but do not contain a usable native backend unless a dylib was staged first. There is deliberately no incomplete Gradle wrapper: with Gradle installed, `gradle wrapper --gradle-version 8.14.3` can generate a complete wrapper locally.

GitHub Actions also builds the Java project and the macOS JNI backend and uploads artifacts. A green CI build still does not test AirPods or the macOS controls.

## First run

1. Back up your Minecraft instance. Put the built mod JAR in the instance's `mods` directory.
2. Launch once. The mod creates `config/mc-spatial-audio.properties` with `enabled=false`.
3. Quit Minecraft, set `enabled=true`, and relaunch. Leave `headTrackingRequested=true` for the intended path.
4. Check `logs/latest.log` for `MC Spatial Audio`. Use `/mcspatial status` in a world for diagnostics.
5. Inspect macOS's real audio controls while a sound is playing and perform the listening tests. **The mod never creates, labels, or imitates those controls.**

If preflight fails before Minecraft adopts the loopback device, the mod leaves ordinary OpenAL output in place. Failures later in initialization, device changes, and runtime rendering failures are not fully recovered automatically in this prototype. To restore ordinary audio, quit, set `enabled=false`, and restart.

The prototype uses the **macOS default output device**, not Minecraft's selected OpenAL device name. Choose AirPods in macOS before launching. After changing devices, use Minecraft's resource reload (F3+T), or restart; seamless hot-switching is not implemented.

For local development, `-Dmcspatial.nativeLibrary=/absolute/path/to/libmcspatial.dylib` selects an external dylib instead of the packaged one. `-Dmcspatial.enabled=true` overrides the config's enabled flag. These are JVM arguments, not Minecraft command-line arguments.

## Important signing / entitlement limitation

Apple documents the Head Pose entitlement for its head-tracking APIs. A mod JAR or loaded dylib cannot add entitlements to the host Java executable. The backend reports the running process's entitlement and requests the public spatial-mixer property; it does **not** interpret either result as proof of active tracking. Whether permissions, signing, launcher identity, and OS policy permit the required experience must be established on the actual Mac.

A small `MCSpatialProbe.app` is built alongside the dylib to separate Apple-renderer testing from JVM testing. It is not a production audio-helper process and Minecraft does not route sound to it. The project does not re-sign, alter, or replace your launcher or Java runtime. See `docs/TESTING.md` before experimenting with signing.

## Rendering trade-offs

This is a **7.1 surround-bed bridge**, not an object-audio renderer. OpenAL continues to position sources and apply EFX; Apple's mixer performs the final headphone rendering. Elevation detail is limited by the horizontal bed, and UI/music sounds share that bed. Per-source spatial metadata, selective head tracking, and a signed helper-process transport are future work.

## Sources

- [Apple: generating spatial audio from a multichannel audio stream](https://developer.apple.com/documentation/audiotoolbox/generating-spatial-audio-from-a-multichannel-audio-stream)
- [Apple: kAudioUnitProperty_SpatialMixerEnableHeadTracking](https://developer.apple.com/documentation/audiotoolbox/kaudiounitproperty_spatialmixerenableheadtracking)
- [Apple: Head Pose entitlement](https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.developer.coremotion.head-pose)
- [OpenAL Soft: SOFT_loopback extension](https://openal-soft.org/openal-extensions/SOFT_loopback.txt)
- [LWJGL 3.3.3 OpenAL bindings](https://github.com/LWJGL/lwjgl3/tree/3.3.3/modules/lwjgl/openal)
- [Sound Physics Remastered, 1.21.1 branch](https://github.com/henkelmax/sound-physics-remastered/tree/1.21.1)
- [NeoForge 1.21.1 ModDevGradle template](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle)

License: MIT for the original code in this repository. Upstream Minecraft, NeoForge, LWJGL, and Sound Physics Remastered retain their own licenses.
