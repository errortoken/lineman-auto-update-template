#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
TARGET_FILE="${1:-$HOME/lineman-auto-update-template/app/build.gradle.kts}"
[ -f "$TARGET_FILE" ] || { echo "❌ Not found: $TARGET_FILE"; exit 1; }
read -p "📌 New versionCode: " new_code
read -p "📌 New versionName (e.g. ${new_code}.0): " new_name
sed -i "s/versionCode[[:space:]]*=[[:space:]]*[0-9]\+/versionCode = ${new_code}/" "$TARGET_FILE"
sed -i "s/versionName[[:space:]]*=[[:space:]]*\"[0-9.]\+\"/versionName = \"${new_name}\"/" "$TARGET_FILE"
echo "✅ Updated: versionCode=${new_code}, versionName=${new_name}"
