# LuckyTool Reborn build

## Requirements

- JDK 21. The Gradle daemon accepts an installed JDK 21 without requiring a particular vendor.
- Android SDK platform 37.0 (`platforms;android-37.0`) and Build Tools 36.0.0, the default required by Android Gradle Plugin 9.5.0.
- Network access to the Gradle distribution, Google Maven, Maven Central, and the repositories listed in `settings.gradle.kts`.
- Android SDK licenses accepted. For local builds, set `ANDROID_HOME` or an untracked `local.properties` with `sdk.dir`.

The project uses its checked-in Gradle Wrapper and dependency catalog. Do not substitute an arbitrary installed Gradle version.

## Local validation

```sh
bash ./gradlew --version
bash ./gradlew --no-daemon --stacktrace :app:assembleDebug :app:testDebugUnitTest
# Run independently; current source has the lint errors documented below.
bash ./gradlew --no-daemon --stacktrace :app:lintDebug
```

The APK and its metadata are written to `app/build/outputs/apk/debug/`. Tests and lint reports are written under `app/build/reports/`; JUnit XML is under `app/build/test-results/`.

Version codes are read without modifying `app/version.properties`. Optional overrides are explicit:

```sh
bash ./gradlew :app:assembleDebug -PrebornVersionCode=23304 -PrebornVersionName=1.3.5_reborn.3
```

## Constrained-memory local validation

The 2026-11-10 run passed with one Gradle worker and in-process Kotlin. On a constrained machine, run the stages separately so compiler/lint state does not accumulate alongside Robolectric forks:

```sh
COMMON=(--no-daemon --max-workers=1 \
  -Pkotlin.compiler.execution.strategy=in-process \
  '-Dorg.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8')
bash ./gradlew "${COMMON[@]}" :app:assembleDebug
bash ./gradlew "${COMMON[@]}" -I scripts/low-memory-tests.init.gradle :app:testDebugUnitTest
bash ./gradlew "${COMMON[@]}" :app:lintDebug
```

The optional init script limits the test JVM to 1 GiB and restarts it every eight test classes; it does not omit or ignore tests. Keep the full lint outcome separate from compilation/test success. Machine-specific network/proxy configuration is not part of the source. Use the normal trusted repository setup for your environment.

## GitHub Actions

The workflow `.github/workflows/build-reborn.yml` runs on pushes and pull requests to `main`, or manually through **Actions > Build LuckyTool Reborn APK > Run workflow**.

It checks out the selected commit, sets up Temurin JDK 21 on Ubuntu 24.04, installs Android SDK platform 37.0 and Build Tools 36.0.0, builds the debug APK and runs required unit tests. A separate job runs full lint as a non-blocking check, preserving its actual outcome in the job summary and warning annotations. Third-party actions are pinned to verified commit SHAs. Workflow permissions are read-only and checkout credentials are not persisted. No source is cloned from upstream or pushed from CI.

A successful APK build uploads `LuckyTool-Reborn-debug-<commit>` containing the APK, `output-metadata.json`, `build-info.txt`, and `SHA256SUMS`. The APK is retained even if a later unit-test step fails, but unit-test failure still fails the build job. Unit-test reports are uploaded separately even after failure, if produced. Full lint reports are uploaded in `LuckyTool-Reborn-lint-<commit>`; lint errors remain visible but do not block APK/unit-test success. A green workflow must not be described as a clean lint result. Artifacts are retained for 14 days.

A checked-in workflow is not evidence of a successful run. Verify the run's exact commit, required unit-test step, build result, and uploaded artifact before calling an APK build successful. Report the separate lint outcome explicitly. A run that fails before runner assignment does not test the source.

## Signing and installation

Debug builds use the Android Gradle Plugin's normal local debug key; no private signing material is committed. This key is not the official LuckyTool key. A new machine or CI runner may create a different debug key, so artifacts from separate runs are not guaranteed to support in-place upgrades.

The application ID remains `com.luckyzyx.luckytool`. Do not assume a debug build can replace the official app. Back up settings first and test on a suitable device. Keep any intended long-term signing key private and use it consistently for updates.

Custom release signing is optional through untracked `keystore/keystore.properties`. Consult `build.gradle.kts` and `app/build.gradle.kts` for the accepted keys. Do not commit signing keys, passwords, `local.properties`, Gradle caches, or generated APKs.

## Verification boundary

Compilation, unit tests, and lint do not establish Android 17 / ColorOS 17 runtime compatibility. Device tests must cover the actual ROM, LSPosed version, enabled scopes, reboot/recovery behavior, and individual hooks.

## Historical v0.3 local result

For the current browser/Market implementation and results, see `VALIDATION_REBORN_4.md`. The following records the earlier base build.

The modified project produced a debug APK locally. All 196 unit tests across 34 suites passed. Full lint failed with 414 errors, 559 warnings, and 8 informational findings. The six error-bearing files are unchanged from the supplied source archive; no blanket suppression or baseline was added. See `VALIDATION.md` for categories, commands, artifact hashes, and verification limits. These results do not imply a successful GitHub Actions run or a device test.
