# Install MC Spatial Audio

Requires Minecraft 1.21.1, NeoForge 21.1.256+ within 21.1, macOS 13+, and supported headphones for head tracking. Java 21 is the recommended baseline.

1. Download the **JAR asset**, not GitHub's source ZIP. No build tools are needed; the native library is included for Apple Silicon and Intel.
2. Remove older copies and put the JAR in your test instance's `mods` folder.
3. Launch and open **Mods → MC Spatial Audio → Config**.
4. Select your headphones as the macOS sound output. Enable spatial audio and press **Apply & reload**.

Head tracking On requests Apple's tracking; Off keeps fixed spatialization. Apple manages the reference frame; this mod does not force a recenter timer. The old `config/mc-spatial-audio.properties` is retained, so alpha settings survive upgrades. New installs start disabled.

Use **Copy diagnostics** or `/mcspatial status` for troubleshooting. `/mcspatial reload` rebuilds output after changing devices. If sound fails, disable spatial audio and apply/reload, or remove the JAR and restart. Send relevant latest.log lines and your version/platform details, not credentials or personal launcher paths.

Sound Physics Remastered's 1.21.1 NeoForge build is optional. Test the bridge alone first, then add it for acoustic effects.

The optional probe ZIP is a separate signal generator, not a Minecraft dependency. At low system volume, run its executable with `--fixed --seconds 32`, then `--seconds 32`, to compare fixed/tracked behavior.

Binaries are ad-hoc signed, not Developer ID signed/notarized. Do not disable Gatekeeper/SIP or modify Java/launcher signatures. Automated tests verify builds/routing, not real-world recenter timing, perceived balance or macOS menu-bar eligibility.
