#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
REPO_USER="${1:-errortoken}"
REPO_NAME="${2:-lineman-auto-update-template}"
TARGET_FILE="$HOME/lineman-auto-update-template/app/build.gradle.kts"

# 1) set version
"$HOME/lineman-auto-update-template/scripts/set_version.sh" "$TARGET_FILE"

# 2) git add/commit/push
cd "$HOME/lineman-auto-update-template"
pkg update -y >/dev/null 2>&1 || true
pkg install -y git gh >/dev/null 2>&1 || true
[ -n "$(git config --global --get user.name || true)" ] || git config --global user.name "$REPO_USER"
[ -n "$(git config --global --get user.email || true)" ] || git config --global user.email "${REPO_USER}@users.noreply.github.com"

[ -d .git ] || git init
git branch -M main
git add -A
git commit -m "chore: bump version for release" || true

if ! gh auth status >/dev/null 2>&1; then gh auth login --web --scopes repo; fi
if ! gh repo view "$REPO_USER/$REPO_NAME" >/dev/null 2>&1; then gh repo create "$REPO_USER/$REPO_NAME" --public -y; fi
git remote remove origin 2>/dev/null || true
git remote add origin "https://github.com/$REPO_USER/$REPO_NAME.git"
git fetch origin main >/dev/null 2>&1 || true
git pull --rebase --allow-unrelated-histories origin main || true
git push -u origin main || git push -u origin main --force-with-lease

echo "🚀 Pushed. Check Actions/Release for v<versionCode>."
