#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
GH_USER=${1:-errortoken}
REPO=${2:-lineman-auto-update-template}
cd "$HOME/lineman-auto-update-template"
pkg update -y >/dev/null 2>&1 || true
pkg install -y git gh unzip openjdk-17 >/dev/null 2>&1 || true
[ -n "$(git config --global --get user.name || true)" ] || git config --global user.name "$GH_USER"
[ -n "$(git config --global --get user.email || true)" ] || git config --global user.email "${GH_USER}@users.noreply.github.com"
[ -d .git ] || git init
git branch -M main
git add -A
git commit -m "init: lineman-auto-update-template (v1)" || true
if ! gh auth status >/dev/null 2>&1; then gh auth login --web --scopes repo; fi
if ! gh repo view "$GH_USER/$REPO" >/dev/null 2>&1; then gh repo create "$GH_USER/$REPO" --public -y; fi
git remote remove origin 2>/dev/null || true
git remote add origin "https://github.com/$GH_USER/$REPO.git"
git fetch origin main >/dev/null 2>&1 || true
git pull --rebase --allow-unrelated-histories origin main || true
git push -u origin main || git push -u origin main --force-with-lease
echo "✅ Done. Actions will build & create release on next version bump."
