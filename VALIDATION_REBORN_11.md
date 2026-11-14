# Reborn v0.11 browser fixes: local validation

Version `1.3.5_reborn.11` / `23312`, application ID
`io.github.zechariah0825.luckytoolreborn`. Validated locally on 2026-11-14.

## Changes

- Reward cleanup removes the scoped profile card's remote/third-party reward
  inputs before native binding, independent of caption. Native login, avatar,
  account and VIP actions are preserved; refresh and attachment are covered.
- Custom Home root Back returns through native Home activation to Dock Search+
  (Home child type 7), not `MultiSearchPage`. Captured request/page identity,
  permission generations, active-tab checks, cancellation and failed-selection
  retry prevent unrelated web history or later navigation from being consumed.
- Exact native mappings were checked against supplied Browser `40.10.21.9.6`
  (`10210906`) and reference `40.10.25.2` (`10250200`). Unknown/mismatched Search
  mappings fail closed. See [Search/Home evidence](docs/browser-search-home-v11.md)
  and [profile/reward evidence](docs/browser-profile-rewards-v11.md).
- Independent identity, migration/import support, community updater, removed
  diagnostics, colored Google icon and existing Market changes are retained.

## Fresh checks

| Stage | Result |
| --- | --- |
| `:app:compileDebugKotlin` | Passed, 18 tasks executed |
| `:app:testDebugUnitTest` without filters | Passed: 367 tests / 60 suites; 0 failures, errors or skipped |
| Focused cases within the full run | 26 Search/Home + 16 reward/profile = 42 |
| `:app:lintDebug` | Failed: 406 `MissingTranslation` errors, 710 warnings, 34 hints |
| Original-key `:app:assembleDebug` | Passed |
| APK signature and identity | Passed; original certificate, expected package/version |
| APK ZIP CRC and 16 KiB ZIP alignment | Passed |
| Both ARM64 native libraries, 16 KiB ELF alignment | Passed |
| Modern Xposed entry and browser/Market scopes | Present; expected implementation entry |

All lint errors are existing missing translations in unchanged resource files.
No other error category occurred, and no rule was disabled or baseline added.
Warnings/hints remain and are not represented as a clean lint pass.

The 973-file build-input snapshot was checked before and after the full pipeline
without changes. Subsequent edits only update release/validation documentation.
The local constrained-memory commands are documented in [BUILD_REBORN.md](BUILD_REBORN.md).
Private signing material is not included in the source or release assets.

## Artifact

- File: `LuckyTool-Reborn-v0.11.apk`
- Bytes: `23479839`
- SHA-256: `2d2a65869df9d9cfbe4216bcdcdd1889ccf75afaa167d45e58ccb7aa31b1209a`
- Certificate SHA-256: `723b385232dba362e1822702a38d86d486e71c5d73bb104ca4e62093d834ac4c`
- APK signature verification: passed (v2); same signing identity as v0.8/v0.9/v0.10.
- Minimum SDK 30; target/compile SDK 37; `arm64-v8a`.
- Enabled launcher alias: `io.github.zechariah0825.luckytoolreborn.Hide`.
- Modern entry: `com.luckyzyx.luckytool.hook.Entry`.
- Application implementation remains `com.luckyzyx.luckytool.ui.application.MyApplication`;
  own provider authorities and receiver permission use the independent app ID.

## Installation and verification limits

v0.10 users can install this APK over v0.10 with the same identity and certificate;
no uninstall is required. Users of the old package should follow
[COMMUNITY_MIGRATION.md](COMMUNITY_MIGRATION.md) and must not enable both modules
for the same targets.

No OPPO/ColorOS 17/Vector phone installation, launch or Hook runtime test was
performed. Static DEX/resource analysis, unit tests and APK checks do not prove
on-device compatibility. After updating/restarting, verify logged-in reward
removal, logged-out login, custom Home root Back to Search+, ordinary web Back,
repeated Home/Back, permission interruption and tab switching on the phone.

GitHub Actions remain deferred by user instruction; these are actual local
build results, not CI results. No claim of a successful Actions run is made.
