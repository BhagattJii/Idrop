# Security Policy

## Supported versions

| Version | Supported |
|---|---|
| Latest release (`v*`) | Yes |
| `main` | Yes |
| Older versions | No |

## Reporting a vulnerability

**Do not open a public issue.** Report it privately through GitHub Security Advisories:

https://github.com/BhagattJii/Idrop/security/advisories/new

Include the affected version, device setup, reproduction steps and impact. Expect an acknowledgement within 72 hours.

## Threat model in one paragraph

iDrop transfers files directly between two phones over an encrypted local link (Google Nearby Connections). There is no account, no server and no cloud copy of your data. The sensitive assets are the files in transit and the app's local storage. Areas of interest are therefore: weaknesses in the transport/session handling, plaintext of transferred files being exposed on disk, path traversal in the download-to-Downloads step, an attacker being able to connect to a device that was not explicitly approved, and permission overreach.

## What to report

- Files readable by other apps after a transfer completes
- Path traversal or filename injection when saving to `Downloads/iDrop`
- A remote peer connecting without the user tapping **Connect**
- Plaintext file content written to logs, caches or crash reports
- A new permission requested without a stated need
