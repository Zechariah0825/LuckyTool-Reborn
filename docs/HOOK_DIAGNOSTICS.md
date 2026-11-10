# Hook diagnostics: work in progress

This is an unpublished development branch on released base `6d7b5f4098359d4f3ba4250ee2ca020b9065adc4`. Do not treat it as a finished APK or a validated release. The working environment was reset during development; source has been reconstructed and must be compiled and tested again. Earlier test results do not validate the reconstructed tree.

## Evidence, not a green light

- Off is saved configuration only. A running target may still need a restart.
- No response is unknown, not failure. The app may not be running, scoped, initialized, or reachable.
- A response proves that the module loaded in a process, not that any feature works.
- Registration success requires a real receipt. YukiHook 1.5.0's onHooked/onHookingFailure are empty methods, and the DSL can swallow registration failures. No generic wrapper marks registration successful.
- Callback entry is counted only in the relevant target path where a shared method needs filtering. Counts are per process session and do not prove final user-visible behavior.
- Mutations are shown only when explicitly measured. The calendar holiday pilot counts actual URL rewrites. Other enrolled features say the final modification/effect is unverified, not zero.
- Direct setup/callback failures retain their original propagation behavior. Only exception class names are reported, not messages or stacks.
- Old module/configuration evidence is rejected or clearly labeled. Recent reports expire after 15 seconds; no claim that a recently responding process is still alive.

## Scope and limits

105 unique setting keys are enrolled: 104 shared UI switches and one string mode, across 107 Hook files and 24 scope directories. The static UI inventory has 386 literal switches, so 282 remain uninstrumented. Generated keys and custom-rendered controls require separate auditing. Run `python scripts/audit-hook-diagnostics.py` for the checked exact list.

Browser/Market repair code is untouched. Compound/global FeatureConfig, SystemProperties, multi-setting layouts, async tasks and direct field writes require precise key/dependency attribution or readback; generic callbacks must not claim every enabled feature is effective. Enabling a switch is never evidence.

## Transport and lifecycle

The existing Yuki data channel targets the host/module packages. A random 128-bit challenge rejects stale/mismatched responses; schema, package, digest, name, counter and byte limits are validated. This is diagnostics, not security attestation against a compromised host. No new exported components or permissions are added.

Settings and loaded APK paths are typed SHA-256 fingerprints. No raw URLs, searches, browsing data or exception messages cross the channel. Build identity includes version and install path; unusual in-place replacement is not a cryptographic code-identity guarantee.

Targets store bounded observations in memory only (128 features). No disk/Binder/network work runs in feature callbacks. The app polls only while its settings page is STARTED, every three seconds. Replies are rate limited, retain at most 16 processes per package, and replace restarted-session counters.

## Validation required

Compile, full unit tests, lint outcome, APK generation and on-device checks must be reported separately. Device acceptance must cover no module/scope, closed target, actual feature invocation, missing target class, toggling without restart, module reinstall with old target process, target restart, expired responses, Material/Miuix layouts, search jump/highlight, conditional rows and navigation. System-server/shared-UID delivery requires actual ROM/LSPosed testing.
