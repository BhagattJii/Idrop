<div align="center">

# iDrop

**Advanced offline AirDrop-style sharing for Android — MIT licensed, no accounts, no ads, no payments.**

[![Release](https://img.shields.io/badge/release-v0.4.0-brightgreen?style=flat-square)](https://github.com/BhagattJii/Idrop/releases/latest)
[![CI](https://github.com/BhagattJii/Idrop/actions/workflows/android.yml/badge.svg)](https://github.com/BhagattJii/Idrop/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android)](https://developer.android.com/)

</div>

---

iDrop is a brand-new project (not a LocalSend fork), built from the ground up around one simple flow:

1. Two phones come close.
2. A popup appears automatically.
3. Tap **Connect** — no router, no manual hotspot, no Bluetooth pairing.
4. Files and text move directly over an encrypted local link.

## Download

Grab the signed APK from the [latest release](https://github.com/BhagattJii/Idrop/releases/latest).

- Package: `com.idrop.x`
- Minimum Android: 8.0 (API 26)
- Size: about 12 MB (signed release)
- Signed release APK — installable on any compatible Android phone
- Debug builds are also attached to every successful GitHub Actions run

> Android may ask you to allow installs from your browser or file manager the first time. That is the normal sideload prompt; iDrop has no Play Store dependency and no payment of any kind.

## Features (v0.4.0)

- **Realtime close-range popup** — a small BLE beacon layer estimates distance from RSSI, and the popup appears when another iDrop phone is within roughly **2.5 m**. A discovery fallback keeps things working on phones where BLE advertising is unavailable.
- **Automatic discovery** — both phones advertise and scan at the same time; no manual pairing, hotspot, or router.
- **Encrypted direct transfer** — Google Nearby Connections handles the authenticated, encrypted peer-to-peer link. Files never touch a server.
- **Real file sharing** — send any file type or photos/videos with the system picker, including multiple files at once.
- **Text messages** — send notes and links instantly; received text can be copied with one tap.
- **Downloads/iDrop** — received files are saved to `Downloads/iDrop` on Android 10+ (with automatic unique names); older versions use the same public Downloads folder.
- **Transfer history** — direction, peer, size, progress, status, saved path, and time for every transfer.
- **Live progress** — byte-level progress for outgoing and incoming transfers.
- **One-tap self-test** — checks Android version, Bluetooth LE, Wi-Fi, location services, Google Play services, permissions, app storage, Downloads write access, and live peer discovery. The report can be shared or copied.
- **Diagnostics** — BLE proximity status, nearest estimated distance, event log, and connection state.
- **Editable device name** — choose what nearby phones see.
- **Aurora Glass UI** — dark Material 3 interface with animated radar, proximity dialog, and transfer sheets.
- **No payments** — no billing code, no subscriptions, no ads, no owner fees, no credit card anywhere.

## How to use

1. Install iDrop on two Android phones.
2. Open the app on both phones and allow nearby access.
3. Keep both screens on and bring the phones close (about 1–2.5 m).
4. When the popup appears, tap **Connect**.
5. Tap **Send** on the connected device card, then choose Files, Photos & videos, Text message, or the 5 MB speed test.
6. Received files are saved under `Downloads/iDrop`; received text appears in a dialog.

No Wi-Fi network, hotspot, or Bluetooth settings screen is needed. The radios must simply be switched on.

## Requirements and honest limitations

- Android 8.0+ with Bluetooth LE and Wi-Fi.
- **Google Play services is required for Nearby Connections.** Phones without it (for example, many Huawei models) can still show the app UI but cannot complete Nearby transfers.
- Bluetooth and Wi-Fi must be enabled; the app does not ask you to join a network.
- Android 11 and older also require Location to be on for nearby scanning. Android 12+ does not.
- BLE distance is an estimate. Walls, bodies, phone cases, and radio noise affect RSSI, so the popup threshold is intentionally forgiving; the device list always remains available.
- Physical radio behavior can only be fully validated on two real phones. Every release is compiled and signature-verified in CI, and the in-app self-test is included so a two-phone check takes one tap per device.
- This project uses the free Google Nearby Connections API. It is **not** the Google "Quick Share" app and contains no Google billing or paid service.

## Build from source

```bash
./gradlew assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

For a signed release build, provide the signing material through environment variables (never commit it):

```bash
export IDROP_KEYSTORE_PATH=/absolute/path/to/release.jks
export IDROP_KEYSTORE_PASSWORD=...
export IDROP_KEY_ALIAS=...
export IDROP_KEY_PASSWORD=...
./gradlew assembleRelease
```

Release APK: `app/build/outputs/apk/release/app-release.apk`

The CI workflow builds debug and signed release APKs, verifies the release signature with `apksigner`, and publishes the signed APK automatically when a `v*` tag is pushed. Signing secrets live only in GitHub Actions secrets.

## Tech stack

- Kotlin 2.0 + Jetpack Compose + Material 3
- Google Nearby Connections 19.5.1 (free, no billing)
- Android BLE advertising/scanning for RSSI proximity only
- Kotlin Coroutines + StateFlow
- AndroidX MediaStore for scoped-storage downloads
- GitHub Actions CI, MIT license

## Privacy

- No account, no cloud, no analytics, no tracking, no ads.
- Transfers go directly between the two phones over the local encrypted link.
- The app stores only the device name you choose and the in-memory transfer list; received files go to `Downloads/iDrop`.

## Contributing

Pull requests are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md) for the build steps, the rules that keep iDrop account-free and server-free, and the PR checklist.

## Security

Found a vulnerability? Report it privately via [GitHub Security Advisories](https://github.com/BhagattJii/Idrop/security/advisories/new) — see [SECURITY.md](SECURITY.md).

## License

MIT — see [LICENSE](LICENSE). Third-party notices are in [NOTICE](NOTICE).

Built by [BhagattJii](https://github.com/BhagattJii).
