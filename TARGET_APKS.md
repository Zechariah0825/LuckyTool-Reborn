# LuckyTool Reborn target app references

Inspected on 2026-11-10. These OEM application files were provided by the device owner for local compatibility work. None of their binaries are included in the public source tree or deliverable archive.

## Current Google Drive samples

- `Browser-Browser.apk`: `com.heytap.browser`, version `40.10.21.9.6`, code `10210906`, 272,305,359 bytes, 21 DEX files. SHA-256: `caeda453881561b60e2342004688b86c830468dcb8e42a06231286581d88b6e1`.
- `Browser-split_live_video_bundle.apk`: same package/version/code, 55,919,162 bytes. SHA-256: `38ffafc7755bf28f35d552d969b03fe2eebc909d05dc5da895480306a5fc0ae8`.
- `OPPO-Market.apk`: `com.heytap.market`, version `26.10.2_CN`, code `261002`, 93,531,819 bytes. SHA-256: `6a43dedbb4d74e45e53c4be5f438d87c101cf6eaf79714799e4ad886da5b2c56`.

## Earlier uploaded browser reference

- `OPPO-Browser.apk`: version `40.10.25.2`, 223,247,642 bytes. SHA-256: `e60a6712963c9601d4cb7b756efd3d66c16e5b5bc4576fac92a3ba557d8f3a2a`.

The current Drive browser is a different build from the earlier screenshot/reference. A signature observed in one must not be assumed to match the other. Inspection of DEX confirms candidate APIs and control flow, not successful Xposed hook execution on the phone.

The full source imports from 2026-11-09 are now present in the repository. See `VALIDATION_REBORN_4.md` for this iteration's actual build/test evidence and limits, and `VALIDATION.md` for the historical v0.3 base build. GitHub LFS scaffolding does not imply that any OEM APK was uploaded.
