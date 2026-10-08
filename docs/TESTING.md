# Build verification and hardware acceptance

Do not call this mod working based only on CI, property readback, or an audible test tone. Record each level independently.

## Current verification record

- Source committed: yes.
- Java build against NeoForge 1.21.1: not verified by the authoring assistant; CI configured.
- Native macOS compile: not verified by the authoring assistant; CI configured.
- Portable unit tests: provided; a checked CI result is needed to mark them passed.
- Minecraft client mixin application: not verified.
- AirPods playback / real head tracking / system controls: not verified.

Replace these entries only with observed test results and identify the tested commit. No hardware results are pre-filled.

## A. Build and portable checks

On macOS with JDK 21, CMake, Xcode tools and Gradle 8.14.3:

```sh
bash scripts/build-native.sh
gradle clean build
```

On Linux or Windows, native channel-order tests can run without Apple headers:

```sh
cmake -S native -B native/build -DMCSPATIAL_BUILD_MAC_BACKEND=OFF -DBUILD_TESTING=ON
cmake --build native/build
ctest --test-dir native/build --output-on-failure
```

Java tests cover attribute merging, zero-valued attributes, duplicate normalization, incomplete pairs and EFX auxiliary sends. Native tests check the exact 7.1 channel permutation, multiple frames, sentinels around the buffer, the inverse permutation and zero-frame input. Neither validates Core Audio.

In Actions, use the `experimental-macos-mod` artifact for the intended native test. The Java-only artifact intentionally lacks a staged backend and is NOT a working macOS audio artifact.

## B. Isolated Apple renderer

Select AirPods Pro as the default output and lower system volume before starting. The probe plays a quiet tone for two seconds in each OpenAL channel, in order: front left, front right, center, LFE, rear left, rear right, side left, side right; the cycle repeats. LFE uses an 80 Hz tone; other channels use 440 Hz.

Run a fixed spatialized baseline first from a terminal to preserve diagnostics:

```sh
native/build/MCSpatialProbe.app/Contents/MacOS/MCSpatialProbe --fixed --seconds 32
```

Then request tracking:

```sh
native/build/MCSpatialProbe.app/Contents/MacOS/MCSpatialProbe --seconds 32
```

Also test launch through Launch Services, because terminal and application launch identity can differ:

```sh
open -n native/build/MCSpatialProbe.app --args --seconds 60
```

The `.app` does not create any menu-bar UI. Inspect the real macOS output controls while audio is active. The head-tracking property may be accepted even if tracking is unavailable. Record the difference between requested/read-back state and actual behavior.

### Signing investigation — isolated app only

The entitlements plist is an input for controlled developer tests, not a way to bypass OS policy. Merely attaching an entitlement to an ad-hoc signature does not prove it is authorized or effective. If you have an appropriate Apple development signing identity/provisioning setup, sign the test app according to Apple's requirements; the project does not automate provisioning.

For an explicit local ad-hoc diagnostic experiment ONLY:

```sh
codesign --force --sign - --entitlements native/probe/head-pose.entitlements.plist native/build/MCSpatialProbe.app
codesign --display --entitlements :- native/build/MCSpatialProbe.app
```

A displayed entitlement is not proof of authorization. Re-run the probe and inspect its process-entitlement diagnostic, any OS permissions, tracking behavior, and controls. Do NOT apply these commands to your installed Java binary, launcher, or Minecraft installation. Do not disable SIP, Gatekeeper, or library validation to make this prototype appear to work.

## C. Minecraft without Sound Physics Remastered

1. Create a disposable NeoForge 1.21.1 instance with the mod and matching Java 21 architecture.
2. Launch with the default disabled config; confirm normal audio and no native route.
3. Quit, set `enabled=true`, relaunch. Check for mixin application errors before interpreting playback.
4. Confirm `framesRendered` increases in `/mcspatial status`, `renderErrors=0`, and the correct default output.
5. Inspect system audio controls while Minecraft plays a continuous stationary sound. Record which application/process they identify.
6. Keep the camera/mouse fixed and turn your physical head. Determine whether the apparent direction changes relative to your ears as a stable virtual scene should. Recenter, and distinguish tracking from ordinary left/right panning.
7. Separately turn the in-game camera with your physical head still; ordinary Minecraft panning must still work.
8. Compare fixed versus head-tracking-requested configuration after a full restart. A difference in logs alone is not a result.
9. Disable the mod and confirm baseline audio is restored.

Final requirement: **real Head Tracked system controls for the Minecraft audio process AND observed head tracking**. Controls for `MCSpatialProbe` alone do not pass this test.

## D. Minecraft with Sound Physics Remastered

Repeat section C with the upstream **1.21.1 NeoForge** mod installed. Check reverb in an enclosed room, attenuation through walls, source movement, and water transitions. Confirm the native route still renders and the EFX send count survives. This stage validates coexistence; portable attribute tests alone do not.

## E. Failure and lifecycle tests

- Backend missing or unsupported hardware/API: preflight must retain ordinary OpenAL output.
- Native ABI mismatch / library validation rejection: logs must explain fallback without a fabricated tracking status.
- Repeat F3+T resource reload at least ten times while sound plays; observe crashes, duplicate output, resource leaks and render errors.
- Quit while streaming sound plays. Output must stop before native callback state/OpenAL device destruction.
- Disconnect AirPods and change the default output. This prototype may stop producing audio; record diagnostics, then reload/restart. Seamless recovery is NOT a success criterion for this version.
- Change sample rates where supported and verify the converter, playback speed and pitch.
- Test Apple Silicon and Intel if distributing the universal artifact.

## Report template

```
Commit:
Build job/run:
Mac model / chip:
macOS exact version:
AirPods Pro generation / firmware:
Launcher / version:
Java vendor / version / architecture:
NeoForge version:
Sound Physics Remastered version (or absent):
Launch method (terminal, Launch Services, Minecraft launcher):
Signing / processHeadPoseEntitlement:
Requested / read-back property:
framesRendered / renderErrors / lastRenderOSStatus:
System controls shown / identified process:
Actual physical-head-turn behavior:
Camera-only behavior:
Reload / disconnect / shutdown results:
Relevant latest.log excerpt:
```
