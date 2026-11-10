# LuckyTool Reborn v0.7 browser follow-up

Version: `1.3.5_reborn.7`, code `23308`, application ID `com.luckyzyx.luckytool`.
This is a browser-only follow-up to v0.6. The user confirmed the v0.6 Market
change works; no Market code or runtime-diagnostic behavior is changed here.

## Changes and identified causes

- Custom homepage: the per-fragment once gate missed the actual web Home
  callbacks and reused native Home subpage transitions. Valid URL mode now
  navigates directly, without the manual launch button. Explicit Home clicks
  are distinguished from native Back paths, which share a lower-level method.
  Stale queued work, tab changes, repeated clicks, direct load failure and
  permission/cancel interruptions are guarded. Back or a failed departure
  restores interactive native Home rather than reopening forever, leaving a
  dead white page, clearing history or forcing the Activity to finish.
- Search icons: module-created models had neither usable bitmap data nor the
  light/dark URLs consumed by the result-tab binder. Icons are fetched
  asynchronously from the configured engine's HTTPS origin, decoded within
  size limits, cached, and supplied to the native image pipeline. Failed or
  unsupported fetches retain a stable letter icon. Search queries, paths,
  credentials and cookies are not sent to an icon service. No third-party
  favicon aggregator is used. Native engine selection and search routing are
  unchanged. The settings dropdown is natively text-only and is not replaced.
- Cash reward button: the real account view is a subclass of
  `HeyTapAccountInfoView`, while the old exact-class check rejected it and
  subclass-only reflection missed inherited controls. The cleanup now checks
  the class hierarchy and reads the declared base controls inside MyProfile.
  It preserves account, login, avatar, VIP and other tools.

Detailed method evidence and failure handling are in
`docs/browser-home-route-v7.md` and `docs/browser-search-icons.md`.

## Validation

- Integrated `:app:assembleDebug`: passed.
- Full `:app:testDebugUnitTest`: 392 tests in 57 suites passed, with zero
  failures, errors or skipped tests. This includes 103 diagnostic tests,
  nine unchanged Market tests, 26 homepage/mapping tests, eight profile tests and
  13 icon tests.
- Icon decoding runs with Robolectric native graphics, using synthetic
  malformed/PNG/WebP/ICO inputs and actual Bing, Google and DuckDuckGo ICO
  fixtures. It checks pixel data and rejects bad inputs; a letter fallback
  is not counted as a successful downloaded-icon decode.
- Independent static review and `git diff --check`: passed. No tests were
  disabled and no lint suppressions or baselines were added.
- Full `:app:lintDebug`: failed with 406 errors, 660 warnings and 28 hints.
  All 406 errors are `MissingTranslation` entries in unchanged
  `app/src/main/res/values/strings.xml`. This is not a clean lint result.
- APK package/version/API/ABI, ZIP CRC, v2 signature, Xposed metadata,
  launcher alias, 16 KiB ZIP alignment and native-library LOAD alignment:
  passed on the pre-publication build. The final published source revision
  is rebuilt into the APK and these artifact checks are repeated.

Both original browser APKs were freshly inspected. The user's current build
is 40.10.21.9.6 (code 10210906), 272305359 bytes, SHA-256
`caeda453881561b60e2342004688b86c830468dcb8e42a06231286581d88b6e1`.
The reference is 40.10.25.2 (code 10250200), SHA-256
`e60a6712963c9601d4cb7b756efd3d66c16e5b5bc4576fac92a3ba557d8f3a2a`.
The current BaseTab Home/page-change methods are `g1`/`g0`; the reference uses
`h1`/`h0`. This difference was found in the actual APK and corrected before
release. Version name, version code, fragment creation method and complete
method signatures select a verified pair. Unknown URL-routing versions retain
the native Home rather than installing an unsupported replacement layer.
The current cash view hierarchy/inherited fields and all icon hook bindings
were also cross-checked. APK signatures verified successfully; both originals
have matching signing certificates for their respective Android signing ranges.

No OPPO phone or Vector/LSPosed runtime is attached. Remaining device checks
include cold start, direct Home from native/web/search pages, Back/Forward,
repeated Home and multiple tabs, cancellation of browser permission flows,
icons under light/dark mode and failed networking, and account-card refresh.
Neither automated tests nor hook callback counts prove the final visible effect.

## Signing and installation

The signing certificate is unchanged from the delivered v0.6 APK:
`b0240d829cdb969d2a5fa90cc9d376984eefaa3cfb4d58656a41bbaf68e67be5`.
The higher version code permits an in-place update of that v0.6 installation;
no uninstall is required for this update. Earlier differently signed builds
and official LuckyTool are not signature-compatible. Preserve a settings
backup, install the APK, and restart the affected browser process/device so
the new hooks are loaded. This is a debug-signed test release, not an official
LuckyTool signature.

GitHub Actions remain paused. No workflow edits or manual runs are part of
this work, and final source publication uses `[skip ci]`.
