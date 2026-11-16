# Reborn v0.12 Market update: local validation

Version `1.3.5_reborn.12` / `23313`, independent application ID
`io.github.zechariah0825.luckytoolreborn`. Local checks dated 2026-11-16.
Baseline: public v0.11 commit `15d34b4883e2ce79960a354f4ab1473e4cb01f65`.
The recovered source was byte-compared with that exact public source archive;
remote main and v0.11 both still pointed to it when checked.

## Changes and evidence

- Adapted home-page filtering to Market `26.11.3_CN` / `261103`. Its new
  FragmentManager field shifts the old index fields; the old home-index name
  now refers to a Fragment. Both known typed schemas are supported, with
  missing/ambiguous layouts rejected before mutation.
- Adapted the dedicated search recommendation helper's renamed classes and
  methods, validating complete method signatures. Stable hint APIs install
  independently; initial hint cleanup follows constructor completion instead
  of a renamed private initialization method.
- Kept native search history and 大家正在搜. The landing insertion method and
  four retained card codes were directly confirmed unchanged in the new DEX.
- Preserved the existing home-tab scope and real App Moment page identity,
  normal typed searches, browser behavior and all existing module settings.
- New and old APKs were directly inspected. Home static checks: 11/11;
  search static checks: 7/7. Independent source/DEX review found no blocking
  regression. These are not phone runtime checks.

Evidence details: [home mapping](docs/market-compatibility-v12.md),
[search mapping](docs/market-search-landing-v12.md), [inputs](TARGET_APKS.md).

## Integrated checks

| Check | Result |
| --- | --- |
| `:app:compileDebugKotlin` | Passed |
| Full `:app:testDebugUnitTest` | Passed: 375 tests / 62 suites; 0 failures, errors, skips |
| New compatibility cases | 8 passed, included in full run |
| Existing Market rule cases | 18 passed, included in full run |
| `:app:lintDebug` | Failed: 406 MissingTranslation errors, 715 warnings, 33 hints |
| Changed-file whitespace check | Passed |

All lint errors point to `app/src/main/res/values/strings.xml`, which is byte-
identical to public v0.11. No lint rules were disabled and no baseline was added.
Warnings/hints remain; this is not a clean lint pass.

Environment recovery required a full JDK21 rather than the preinstalled JRE.
The first test attempt could not initialize Robolectric because its home-directory
lock was read-only. Its failure records were retained. A workspace-local test
home fixed the environment, then the complete unfiltered suite passed. No tests
or assertions were removed. SDK 37.0 / Build Tools 36.0.0 and the checked-in
Gradle 9.8.2 wrapper were used. GitHub Actions remain deferred.

## Device boundary

No OPPO/ColorOS 17/Vector phone installation, launch or Hook runtime test was
performed. After installing the module update, restart the target app or reboot
and check cold launch, the actual App Moment content, switching tabs and Back,
refresh/rebind, search history/keyword clicks, typed searches and keyboard
submission, returning from results, waiting for hint rotation, and toggling the
settings independently. Future Market versions are not automatically certified.

## APK verification

- `:app:assembleDebug`: passed with the original private signing key.
- APK: `LuckyTool-Reborn-v0.12.apk`, 23,479,839 bytes.
- SHA-256: `932e4b6b081cf005a526c9a0b75a1349df116e2155df4e6434438c9e0e4d4a76`.
- Signature v2 verification: passed.
- Certificate SHA-256: `723b385232dba362e1822702a38d86d486e71c5d73bb104ca4e62093d834ac4c`.
- Identity/version verified: `io.github.zechariah0825.luckytoolreborn`,
  `23313` / `1.3.5_reborn.12`; minimum SDK 30, target SDK 37; ARM64.
- ZIP CRC, 16 KiB ZIP alignment, and both native-library ELF load alignments passed.
- Expected modern Xposed entry, Browser and Market scopes present.

This artifact has the same package/certificate as delivered v0.10/v0.11 and can
update them in place; no uninstall or settings migration is required for those
versions. Older package-name users must follow COMMUNITY_MIGRATION.md.
Signing material, OEM APKs and machine-specific build configuration are not
included in the source archive. No public publication or CI run is claimed by
this local validation record.
