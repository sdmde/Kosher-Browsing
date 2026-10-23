# Validation for 2.8

- Signed release AAB, debug APKs and full release lint completed successfully; 0 lint errors, 24 warnings.
- 268 chat-site policy checks cover listed hosts, subdomains, recognizable brand/suffix variants and unrelated-site controls.
- Existing network image fail-closed, request policy, diagnostic redirects and background image verification tests passed.
- Main language-label and palette-contrast checks passed.
- Bundle payload signatures are verified against the existing upload certificate, and packaged protection/language assets are compared with source during packaging.
- Existing 64-bit native libraries are checked for 16KB-compatible ELF load segments; the bundle requests PAGE_ALIGNMENT_16K.
- APK download policy and existing browser safeguards remain; no foreground-service permissions or broad storage permissions are added.

No emulator or physical-device tests have been performed. Android file-manager activity availability, OEM picker behavior, device translation support and phone UI behavior require user device testing. A debug-signed APK does not upgrade a Play-signed installation; use the Play track for that update path.

Private image verification tests confirm cookie omission while ordinary image requests retain credentials. Incognito cookie isolation and device UI interactions still require phone testing.
