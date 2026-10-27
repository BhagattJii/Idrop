# Contributing to iDrop

iDrop is an offline, AirDrop-style sharing app for Android: no accounts, no ads, no payments, no server. Contributions are welcome.

## Report a bug

Use the bug report template and attach the in-app diagnostics output (Settings → Diagnostics → copy event log). Include both device models — radio behaviour differs a lot between vendors.

## Build from source

```bash
./gradlew assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

For a signed release build, pass the signing material through environment variables (never commit it):

```bash
export IDROP_KEYSTORE_PATH=/absolute/path/to/release.jks
export IDROP_KEYSTORE_PASSWORD=...
export IDROP_KEY_ALIAS=...
export IDROP_KEY_PASSWORD=...
./gradlew assembleRelease
```

## Before you open a pull request

1. `./gradlew assembleDebug` passes.
2. Keep formatting consistent with the surrounding Kotlin (the project follows the official Android/Kotlin style).
3. One logical change per PR.
4. Fill in the PR template — state how you verified it, and on how many physical devices.

## Rules that are not negotiable

- **No accounts, no login, no payments, no ads, no analytics, no tracking.**
- **No server round-trip.** Files move directly between devices over the local encrypted link.
- **Never commit keystores, `*.jks`, `*.keystore` or signing passwords.** They live only in GitHub Actions secrets.
- **Keep the no-Play-services path alive.** Devices without Google Play services must still show the UI and the self-test instead of crashing.
- Do not weaken the banned-file-type rules or the proximity threshold without explaining why in the PR.

## Commit messages

```
fix: reuse the Nearby Connections client after a failed session
feat: add a transfer-speed self-test
chore: bump Nearby Connections to 19.5.1
```

## License

By contributing you agree that your contributions are licensed under the MIT license of this repository.
