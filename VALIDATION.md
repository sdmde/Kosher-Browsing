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

Version 2.10: settings-only update. Release build and Android lint passed (0 errors, 25 warnings); bundletool validation, AAB JAR signature and ARM64 APK signature checks passed. API 30 emulator checks covered root categories, protection details, appearance navigation, light/dark switching and language settings. No physical-device testing. Existing protection asset hashes matched the built AAB. Earlier policy/native checks apply to unchanged components and were not rerun for this UI update.

Version 2.11: compilation and release lint passed (0 errors, 26 warnings). API 30 emulator checks cover tab search, normal group restoration, multi-selection, closing all tabs to the empty state, opening a new tab afterward, the bottom group strip, closing a grouped current tab, and excluding incognito groups from saved sessions. The AAB and APK signatures are verified; bundletool validates the bundle. All bundled protection assets match the source. No physical-device testing. Protection logic and Gecko dependencies are unchanged; prior policy/native checks were not rerun for this UI update.

Version 2.12 is an icon-only update. Release compilation and Android lint passed; APK/AAB signatures and bundletool validation checked. The PNG pixels packaged in both the APK and AAB match the supplied source image. Browsing-protection assets are unchanged. No emulator or physical-device testing was repeated for this artwork change.

Version 2.13: shared closure logic returns to normal mode after the last private tab closes, including the empty, bulk-close and group-strip paths. Release compilation and lint completed (0 errors, 26 warnings). APK manifest has no debuggable/test-only flag; installed release rejected run-as. API 30 emulator checks completed for in-place update with saved normal tabs/groups, closure paths and normal relaunch. No physical-device testing was performed. APK signing certificate matches previous GitHub builds, while the AAB uses the Play upload key.
