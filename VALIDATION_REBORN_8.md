# LuckyTool Reborn v0.8

Version: `1.3.5_reborn.8`, code `23309`, application ID `com.luckyzyx.luckytool`.

## Changes

- The custom browser homepage becomes a logical Back boundary. Native web
  history is consumed first; at the module-created first web page, native
  exit/background handling takes over instead of exposing the original news
  anchor. Ownership follows the exact native load request and page identity,
  not a matching URL. Other tabs and pages keep their original Back behavior.
- Browser empty-input search landing retains the native search history card
  and its clear-history controls. Advertising, hot rankings, clipboard and
  recommendation cards are removed from this landing only. Typed suggestions,
  actual results, input controls and search-engine selection are unchanged.
- Market search landing retains history and the native “大家正在搜” word card.
  Other recommendation cards are filtered at the dedicated search presenter,
  including network, cache and pagination batches. The previously accepted
  Market homepage implementation is unchanged.
- Market search hint rotation and suggested prefilled hints are disabled using
  the action bar's hint-only interfaces. Actual user text, cursor, history
  selection and query submission remain separate.

The three new search switches default to on and can be disabled independently.
Restart the affected browser/Market process after changing module settings.
Browser/Market repair switches remain outside the diagnostic enrollment; an
enabled switch or a module response does not establish visible success.

## Original APK evidence

- Current browser 40.10.21.9.6, code 10210906, 272305359 bytes:
  `caeda453881561b60e2342004688b86c830468dcb8e42a06231286581d88b6e1`.
- Reference browser 40.10.25.2, code 10250200, 223247642 bytes:
  `e60a6712963c9601d4cb7b756efd3d66c16e5b5bc4576fac92a3ba557d8f3a2a`.
- Market 26.10.2_CN, 93531819 bytes:
  `6a43dedbb4d74e45e53c4be5f438d87c101cf6eaf79714799e4ad886da5b2c56`.

The original files were restored and their hashes rechecked. Native data and
navigation methods were inspected, rather than inferring structure from
screenshots. The browser Back change is mapped for both listed versions;
search landing cleanup is inspected against the current browser. Exact
class/resource/signature contracts fail open on mismatches. OEM APKs and user
search contents are not included in this repository.

Detailed evidence and limits: `docs/browser-home-back-v8.md`,
`docs/browser-search-landing-v8.md`, `docs/market-search-landing-v8.md`.

## Validation

- Full `:app:testDebugUnitTest`: passed, 424 tests in 61 suites, zero failures,
  errors or skipped tests. This includes 38 Home/mapping tests, 11 browser
  search-landing tests, 18 Market tests and 103 diagnostic tests. Production
  Kotlin/Java and test compilation completed in the same run.
- Independent static review and `git diff --check`: passed. Review found and
  corrected accidental removal of native incognito notices before this run.
  No tests were disabled, no lint baseline or blanket suppression was added.
- The diagnostic audit still has 105 enrolled setting keys. There are now
  389 literal UI switches, with 104 enrolled switches and 285 unenrolled ones.
- `:app:assembleDebug`: passed with the explicitly selected new signing key.
- Full `:app:lintDebug`: failed with 406 errors, 678 warnings and 29 hints.
  All 406 errors are `MissingTranslation` in unchanged
  `app/src/main/res/values/strings.xml`. This is not a clean lint result.
- APK package/version/API/ABI, ZIP CRC, signing certificate and v2 signature,
  signature verification for supported API 30–37, Xposed entry/properties/scopes,
  launcher alias and 16 KiB ZIP/native ELF LOAD alignment: passed on the
  pre-publication build. The published source revision is rebuilt into the
  final APK and these checks are repeated before delivery. An embedded commit
  alone does not prove a clean checkout; the final local and remote trees are
  compared separately.

No OPPO phone or Vector/LSPosed runtime is attached. Device acceptance still
needs cold start, Home/Back with and without web history, native exit prompts,
multiple tabs, repeated Home, delayed/canceled browser permissions, private
mode, search text entry/clear, history selection/deletion, delayed content,
pagination and repeated opening. Compilation, JVM/Robolectric tests and static
APK inspection are not on-device verification.

## Signing and installation

The earlier v0.7 signing key was not recoverable from the known build and
backup locations. v0.8 uses a new explicitly configured private signing key:

`723b385232dba362e1822702a38d86d486e71c5d73bb104ca4e62093d834ac4c`

It cannot update the delivered v0.7 in place, despite its higher version code.
First export and confirm a usable module settings backup, then uninstall the
old module, install v0.8, restore settings and recheck enabled scopes. Restart
the affected target processes/device to load the new module. This is an
experimental debug-signed release, not the official LuckyTool signature.

With the user's permission, the new key and recovery materials were backed up
privately. The uploaded copy was downloaded again, checked byte-for-byte by
SHA-256, restored and used to sign/verify a random message; its certificate
matched. Only the owner has access. No private key, password, backup link or
recovery file is committed or published with the release.

GitHub Actions remain paused. No workflow edits or manual runs are part of
this work; source publication uses `[skip ci]`.
