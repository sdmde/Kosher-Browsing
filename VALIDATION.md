# Validation for 2.9

- Signed release AAB, debug APKs and full release lint completed successfully; 0 lint errors, 24 warnings.
- 705 chat-site policy checks cover listed hosts, subdomains, recognizable brand/suffix variants and unrelated-site controls.
- Existing network image fail-closed, request policy, diagnostic redirects and background image verification tests passed.
- Main language-label and palette-contrast checks passed.
- Bundle payload signatures are verified against the existing upload certificate, and packaged protection/language assets are compared with source during packaging.
- Existing 64-bit native libraries are checked for 16KB-compatible ELF load segments; the bundle requests PAGE_ALIGNMENT_16K.
- APK download policy and existing browser safeguards remain; no foreground-service permissions or broad storage permissions are added.

Android API 30 x86_64 emulator tests passed for the random-chat cases described below. No physical-device tests have been performed. Android file-manager activity availability, OEM picker behavior, device translation support and phone UI behavior require user device testing. A debug-signed APK does not upgrade a Play-signed installation; use the Play track for that update path.

Private image verification tests confirm cookie omission while ordinary image requests retain credentials. Incognito cookie isolation and device UI interactions still require phone testing.

Emulator checks on Android 11/API 30 x86_64: ordinary page allowed; safety article allowed; Meetgle blocked; random-video-chat.invalid blocked; HTTP redirect to Meetgle blocked; dynamically injected unknown chat UI blocked; private ordinary page allowed despite normal detected-host restriction; private chat clone blocked; subsequent private requests to that detected host blocked. No physical-device, camera-call, translation, payment or account sign-in tests were performed.

705 policy assertions and the detected-site request/WebSocket/private-scope regression test passed.
