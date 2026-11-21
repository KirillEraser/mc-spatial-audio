# Changelog

## 1.0.0

- First stable release.
- Add mod-list icon.

## 1.0.0-rc.1

- Fix NeoForge config-screen registration that broke compilation.
- Rewrite README for public release.

- Add in-game configuration and explicit audio reload without a game restart.
- Preserve alpha configuration, atomically save settings, and retain unknown keys.
- Bypass vanilla HRTF reset only on the native loopback route.
- Strengthen channel-order/gain and Minecraft hook regression tests.
- Simplify startup logging, mod description and documentation.
- Retain Apple-managed head tracking/reference frames and ordinary distance attenuation.

## 0.1.0-alpha.1

- Initial universal macOS JNI/OpenAL loopback bridge and native test app.
- GitHub build/test/release pipeline.
- User-confirmed head tracking and ordinary attenuation on an M4 Pro Mac.
