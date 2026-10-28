# Version 2.6 interface adaptation

Reference: user-supplied Chrome Android 155.0.8059.41 APK, package com.android.chrome.

The language reference is res/xml/APKTOOL_DUMMYVAL_0x7f180032.xml: app_language_section, content_languages_section and translation_settings_section. The referenced string group 0x7f140b28 through 0x7f140b43 identifies Languages, Preferred languages and translation actions. Common localized labels were adapted from the corresponding English, Hebrew, Arabic, Spanish and French string tables; additional app-specific text is original. Chrome UI code, smali, native libraries, account services and branding are not imported.

BrowserUi implements the top toolbar, three-dot dropdown, centered new-tab layout and tab grid. BrowserSettings supplies native Basics/Advanced preferences and language rows. BrowserLanguages handles app locale context, dictionary fallback, RTL, ordered website locales and Gecko translation APIs. The app remains Gecko-based with its packaged protections.

The JSON language dictionary is bundled in base assets and language splits are disabled. No language-pack download is required to change the app interface. Translation models are a separate Gecko capability and may require downloading. Main screens and language settings are localized; technical messages can fall back to English.

## Version 2.10 settings adaptation

Reference: user-supplied Brave Android 1.89.145 APK. The brave_main_preferences.xml and brave_appearance_preferences.xml tables supply the category organization and preference layout. Native settings now use leading geometric icons, compact typography, flat rows and dividers, plus Brave-style purple light/dark accent roles. Features, General and Display sections map to functioning Kosher Browsing controls. Brave account, Wallet, Rewards, VPN and News integrations are not added. Settings icons and Java code are original. English, Hebrew, Arabic, Spanish and French category labels are included.
