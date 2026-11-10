#!/usr/bin/env bash
# Commit and push the exact two owner-provided OPPO APKs as real Git LFS objects.
set -euo pipefail
if [ "$#" -ne 2 ]; then
  echo "Usage: bash scripts/push-target-apks-lfs.sh /path/to/OPPO-Browser.apk /path/to/OPPO-Market.apk" >&2
  exit 2
fi
if ! command -v git >/dev/null 2>&1 || ! git lfs version >/dev/null 2>&1; then
  echo "Git and git-lfs must be installed" >&2
  exit 1
fi
BROWSER=$(cd "$(dirname "$1")" && pwd)/$(basename "$1")
MARKET=$(cd "$(dirname "$2")" && pwd)/$(basename "$2")
test -f "$BROWSER" || { echo "Missing $BROWSER" >&2; exit 1; }
test -f "$MARKET" || { echo "Missing $MARKET" >&2; exit 1; }
ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT"
git lfs install --local
mkdir -p target-apks
cp "$BROWSER" target-apks/OPPO-Browser.apk
cp "$MARKET" target-apks/OPPO-Market.apk
for file in target-apks/OPPO-Browser.apk target-apks/OPPO-Market.apk; do
  case "$(git check-attr filter -- "$file")" in
    *"filter: lfs") : ;;
    *) echo "LFS is not configured for $file" >&2; exit 1 ;;
  esac
  echo "Prepared: $file"
  if command -v shasum >/dev/null 2>&1; then shasum -a 256 "$file"; fi
done
git add .gitattributes target-apks/OPPO-Browser.apk target-apks/OPPO-Market.apk
if ! git diff --cached --quiet; then
  git commit -m "test-assets: add OPPO browser and market APKs using Git LFS"
fi
# The Git LFS pre-push hook uploads the actual binary objects.
git push origin HEAD
git lfs ls-files
echo "Finished. Verify both files appear above."
