# Target APK samples — Git LFS

The requested target files are OPPO Browser (~223 MB) and OPPO App Market (~93 MB), supplied by the device owner for testing LuckyTool Reborn hooks.

The root .gitattributes configures these paths for genuine Git LFS uploads. **This commit contains only the LFS configuration and uploader script, NOT either APK binary or LFS pointer.** Check `git lfs ls-files` after uploading.

```bash
git clone https://github.com/Zechariah0825/LuckyTool-Reborn.git
cd LuckyTool-Reborn
git lfs install
bash scripts/push-target-apks-lfs.sh ~/Downloads/OPPO-Browser.apk ~/Downloads/OPPO-Market.apk
```

WARNING: this repository is public. Git LFS uploads will also be accessible publicly, and the OEM APKs may be proprietary. Only upload with the right to redistribute these files, or use a private repository for target binary fixtures.
