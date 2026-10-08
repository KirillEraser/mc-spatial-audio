# Verification

## Observed alpha results

On October 8, 2026, the user confirmed physical-head tracking and ordinary distance attenuation. Reported environment: M4 Pro, macOS 26.7.1, Oracle Java 26.0.2.1, Minecraft 1.21.1, NeoForge 21.1.256. The alpha log showed 48 kHz hardware output, accepted head-tracking request and no initial render error. Entitlement readback was 0; it did not prevent the reported native tracking.

The bell attenuation report was withdrawn as normal Minecraft behavior; no attenuation workaround was applied. System-control visibility, Intel hardware, Sound Physics Remastered coexistence and the new settings UI are not established by that alpha report.

## Automated gates

- Java compilation, config persistence/legacy compatibility, ALC attribute merging and EFX preservation.
- Actual mapped Minecraft bytecode checks for device/context/cleanup, `setHrtf` and public audio reload targets.
- Native channel permutation, isolated-channel amplitude/polarity, energy preservation and buffer boundaries.
- Universal native compilation, packaged dylib/architectures/signature, and JNI loading/ABI smoke test.

GitHub publication requires these checks. They do not run a real Minecraft window, sense head pose or measure perceived headphone balance.

## Candidate acceptance on a Mac

1. Upgrade with only one copy of the mod. Confirm the existing alpha enabled/head-tracking settings survive.
2. Open **Mods → MC Spatial Audio → Config**. Check layout/tooltips, enable/disable, fixed/tracked toggle, defaults, diagnostics copy, and apply without restarting. Unsaved changes must not affect audio.
3. Confirm `Failed to reset device: Invalid Value` no longer appears for native output. Disabled output should retain normal Minecraft behavior.
4. Listen to stationary world sounds while moving the camera, player and physical head independently. Confirm ordinary attenuation still works.
5. With camera and audio scene fixed, turn the head and hold still. Observe whether Apple's reference frame settles/recenters as expected for that OS/headset, then compare the standalone probe. Record the observed behavior rather than assuming a timeout.
6. At low system volume, run the probe's repeated channel sequence. Compare left/right pairs and side/rear direction. LFE is intentionally 80 Hz; others are 440 Hz. Direction-dependent Apple HRTF coloration is not itself a routing imbalance.
7. Repeat with Sound Physics Remastered 1.21.1 NeoForge. Check occlusion/reverb and avoid double attenuation.
8. Apply/reload at least ten times during playback; quit during streaming sound. Change/disconnect the output and confirm explicit reload/disable recovers. Check render-error counters.

Mark a stable release validated only after the relevant candidate checks. Include version, exact Mac/OS/Java/headset, mod list, diagnostics and results. Do not publish account identifiers, tokens or launcher paths.

## Optional probe

```sh
native/build/MCSpatialProbe.app/Contents/MacOS/MCSpatialProbe --fixed --seconds 32
native/build/MCSpatialProbe.app/Contents/MacOS/MCSpatialProbe --seconds 32
```

No probe process is needed alongside Minecraft. It has a different app identity; results there do not prove Minecraft's system-control eligibility. Do not disable security protections or re-sign Java/your launcher to force a pass.
