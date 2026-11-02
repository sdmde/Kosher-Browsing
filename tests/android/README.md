# Android interface checks

Use a dedicated Android API 30 x86_64 emulator with direct adbd on localhost:5557 and the debug APK installed. Start from a new test profile (do not run against a phone containing browser data). Launch org.shield.browser/.MainActivity and wait for the native homepage, then run from the project root:

    node tests/android/test-screenshot-ui.cjs
    node tests/android/test-group-privacy-ui.cjs

Set KOSHER_ADB_PORT for a different emulator adbd port. These tests create and close temporary tabs/groups, restart the browser, and read its private preferences via debug-only run-as to verify that private groups are not saved. Screenshots and results go into the ignored work/ directory. No device-owner provisioning is used.
