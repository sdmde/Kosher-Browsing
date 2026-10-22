# Kosher Browsing

An independent Android browser built with Mozilla GeckoView and a packaged protection WebExtension. Current version: **2.7 (version code 14)**. Package: `org.shield.browser`. Developer: sdmde.

## Features

- Chrome-style native toolbar, tab grid, light/dark themes and settings. The supplied blue logo is retained; this is not a repackaged Chrome app.
- Persistent Gecko profile for cookies and website sign-ins; saved tabs, browsing history, bookmarks, folders and bookmark import/export.
- Fixed DuckDuckGo address-bar search and mandatory Safe Search parameters on supported search routes.
- On-device image processing: configured skin-color pixels receive grayscale and strong blur with a 15-pixel surrounding mask. A 40×40 region becomes a 70×70 mask. Images stay hidden until verification succeeds; decoding, size, request and filtering failures stay blocked. Recognized CAPTCHA providers have scoped challenge exceptions.
- Unsupported image surfaces are hidden conservatively. This can hide website icons, charts and previews. Color detection is heuristic and is not a guarantee that every unsuitable image is identified.
- Bundled adult-domain/query restrictions, social-site blocks, advertising rules, YouTube Shorts blocking and expanded random stranger-chat restrictions. Listed hosts include subdomains; recognizable Omegle-style brand variants are checked across common domain suffixes. Lists are bundled snapshots, not an automatically updated category service. New names, proxies and custom rendering paths are not guaranteed covered.
- Downloads opens Android's file manager/Downloads view. **Download transfers** retains in-app pause/resume controls. Downloads pause when the browser is backgrounded; no foreground-service permissions are requested.
- APK installation packages are blocked by URL, name, MIME and completed-file checks. Downloads are staged in private cache before allowed files are exported. This is not a device-wide installation restriction or a universal classifier for encrypted/nested archives.
- Main interface languages: English, Hebrew, Arabic, Spanish and French; RTL for Hebrew/Arabic. Some technical text falls back to English. Ordered website-language preferences are separate from app language. Gecko translation controls depend on supported devices/language pairs and may download models from Mozilla.

## Build

Requires JDK 17+, Android SDK platform 37, build tools 36.0.0 and network access to Gradle/Maven dependencies. The wrapper uses Gradle 9.3.1 with Android Gradle Plugin 9.1.0; AGP prints an advisory because compileSdk 37 is newer than its tested range.

```sh
./gradlew assembleDebug lintRelease
```

On Windows, use `gradlew.bat`. Debug APKs are produced under `app/build/outputs/apk/debug/` for ARM64, ARMv7, x86_64 and universal installation. Debug keys differ from Google Play app-signing keys: update a Play installation through its Play testing/release track to preserve data.

For a signed Play bundle, supply your own keystore through the `KOSHER_UPLOAD_KEYSTORE`, `KOSHER_UPLOAD_STORE_PASSWORD`, `KOSHER_UPLOAD_KEY_PASSWORD` and optional `KOSHER_UPLOAD_KEY_ALIAS` environment variables, then run `./gradlew bundleRelease`. The repository contains no signing keys or passwords. `build-release.ps1` supports an external private signing directory. See [release guidance](PLAY-RELEASE.md).

## Tests

```sh
npm install --prefix tests
npm test --prefix tests
```

Set `CHROMIUM_PATH` when using an existing desktop Chromium browser. Tests use synthetic images and mocked requests; they do not prove live compatibility with every website. JVM download-policy tests are in `tests/downloads/DownloadPolicyTest.java`. They can be compiled with `app/src/main/java/org/shield/browser/DownloadPolicy.java` and run as `org.shield.browser.DownloadPolicyTest`.

Validation has included compilation, Android lint, desktop policy/image checks, JVM package checks, bundle/signature checks and native-library alignment checks. No emulator or physical-device testing has been performed. Phone layout, file-manager integrations, CAPTCHA flows and translation-model behavior remain for device testing.

## Privacy and license

Browser data is primarily stored in the app's local profile. Websites, search engines and engine services still receive their ordinary network requests. See [privacy policy](PRIVACY-POLICY.md), [third-party notices](THIRD-PARTY.md) and [GPL-3.0 license](LICENSE).

Mandatory protections have no off switch. This app cannot prevent uninstalling it, using another browser or every possible bypass; it is not a device-owner parental-control system.
