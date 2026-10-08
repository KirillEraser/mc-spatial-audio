# Experimental macOS spatial-audio prototype

Prebuilt Minecraft 1.21.1 / NeoForge client mod with a universal macOS native backend. **This is a prerelease, not a verified AirPods head-tracking product.**

## Download

- `mc-spatial-audio-<version>.jar`: install this in your test instance's `mods` directory. Includes the native library; no local compilation required.
- `MCSpatialProbe-macos-universal.zip`: optional standalone audio test app.
- `INSTALL.md`: setup, activation, diagnostics and rollback instructions.
- `SHA256SUMS.txt`: download checksums.

GitHub's automatic source ZIP/tarball downloads are NOT the compiled mod.

## Automated release gates

Publication is conditional on portable channel-order tests, Java compilation/tests (including checks of mapped Minecraft hook targets), universal native compilation, verification of the packaged dylib, and a JNI loading/ABI smoke test on the build runner. Tests are not skipped to manufacture a release.

These checks do not launch a real Minecraft client, connect AirPods, verify live head tracking, or verify macOS menu-bar/Control Center behavior.

## Requirements and limitations

- Minecraft 1.21.1, NeoForge 21.1.256 or a compatible later 21.1 build, Java 21, macOS 13+.
- Disabled by default. Launch once, quit, then edit `config/mc-spatial-audio.properties` to enable.
- macOS default output device is used. Device hot-switching/reconnection is not seamless.
- 7.1 surround-bed rendering, not per-source/object audio; elevation and UI/music treatment have limitations.
- Sound Physics Remastered 1.21.1 NeoForge is optional; coexistence remains a runtime test, not a build result.
- Native files are ad-hoc signed, not Developer ID signed/notarized. A JAR/dylib cannot grant the host JVM a Head Pose entitlement.
- There is no custom menu-bar UI. Head-tracking property acceptance is not proof of system-control eligibility or active tracking.

Use a disposable test instance and keep system volume low during probe tests. If audio fails, disable the mod and restart. Do not disable macOS security protections or re-sign your launcher/Java runtime to force success.
