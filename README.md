# iDrop X

**Advanced offline AirDrop-style sharing for Android — MIT licensed, no accounts, no ads, no payments.**

iDrop X is a brand-new project (not a LocalSend fork). It is built from the ground up around one simple flow:

1. Two phones come close.
2. A popup appears on both phones.
3. Tap **Connect** — no router, no manual hotspot, no Bluetooth pairing.
4. Files move directly over an encrypted local link.

## Status

Phase 1 — core foundation:

- Kotlin + Jetpack Compose UI
- Aurora Glass design system
- Nearby discovery with the free Android Nearby Connections API
- Automatic encrypted peer-to-peer transfer
- 5 MB test-file transfer + in-app diagnostics/logs
- GitHub Actions debug builds

Proximity RSSI popup, premium transfer animations, history, settings persistence, QR pairing, and release signing are the next milestones.

## Requirements

- Android 8.0+ (API 26)
- Wi-Fi and Bluetooth radios enabled (the app turns the link on automatically; you never connect anything manually)
- No internet, no account, no server

## Build

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

A prebuilt debug APK is attached to every successful GitHub Actions run.

## License

MIT — see [LICENSE](LICENSE). Third-party notices are in [NOTICE](NOTICE).

Built with love by BhagattJii.
