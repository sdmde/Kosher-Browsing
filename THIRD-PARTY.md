# Bundled sources

Launcher logo: user-supplied IMG-20261019-WA7263.jpg, packaged unchanged as kosher_logo.jpg. Adaptive-icon XML supplies placement and background.

EasyList authors, https://easylist.to/ and https://github.com/easylist/easylist. Ad servers and general block sources retrieved 2026-10-19. Distributed under GPL-3.0-or-later (one of EasyList's offered licenses): see COPYING-GPL-3.0.txt and https://easylist.to/pages/licence.html. Unconditional hostname rules are extracted; only simple unconditional URL-path fragments are imported. This is a subset, not a complete AdBlock syntax engine. Original extracted source files are in third-party/.

The Block List Project, https://github.com/blocklistproject/Lists, porn.txt retrieved 2026-10-19; list header last modified 2026-07-18. Repository license is Unlicense (third-party/blocklistproject-LICENSE); the list header labels itself MIT. Original domain data is preserved in adult-hosts.js, sorted for binary search. Domains include subdomains automatically. Snapshot only; no automatic updates.

The user supplied Nudity Filter 3.10 from their local Brave installation. Its RGB predicate was used as a behavioral specification; original scripts, artwork, jQuery and manifest are not redistributed. The new filtering implementation uses the same threshold: R>95, G>40, B>20, max-min>15, abs(R-G)>15, R>G, R>B.

GeckoView is Mozilla software, distributed as an unmodified Maven dependency. Gecko source and license information: https://firefox-source-docs.mozilla.org/ and https://www.mozilla.org/MPL/2.0/ .

The newly written browser source and extension are provided under GPL-3.0-or-later. The APK includes the license and these notices in its assets.

Interface reference for version 2.1: user-supplied decompiled Firefox for Android 158.0 APK (org.mozilla.firefox), specifically its res/xml preference organization, Nova color roles and toolbar/menu dimensions. The app uses original native Java UI and geometric Canvas icons; it does not import Fenix smali, Compose binaries, Firefox logos, accounts or Sync services. See UI-ADAPTATION.md for mapping. Firefox source is Mozilla software under MPL-2.0; GeckoView remains an unmodified Maven dependency.

Version 2.6 interface reference: user-supplied Chrome Android 155.0.8059.41 APK. The toolbar, menu, tab-grid and language preferences are implemented as original native Java controls; common translated interface labels are adapted from the supplied resource tables. No Chrome smali, native engine, proprietary services, Google account integration, or Chrome branding is bundled. Gecko translation uses Mozilla's existing runtime APIs and downloadable models where supported.

Version 2.10 settings reference: user-supplied Brave Android 1.89.145 preference XML and theme color roles. Layout, spacing and settings organization were adapted using original native Java controls and geometric icons. Brave smali, branding, native libraries and account services are not bundled. See UI-ADAPTATION.md.
